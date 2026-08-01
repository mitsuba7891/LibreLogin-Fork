/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.kyngs.librelogin.api.BiHolder;
import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.configuration.CorruptedConfigurationException;
import xyz.kyngs.librelogin.common.config.key.ConfigurationKey;
import xyz.kyngs.librelogin.common.config.migrate.ConfigurationMigrator;
import xyz.kyngs.librelogin.common.util.GeneralUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigurateConfiguration {

    private final ConfigurateHelper helper;
    private final boolean newlyCreated;
    private final File file;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private final Map<String, String> commentsByPath;

    public ConfigurateConfiguration(File dataFolder, String name,
                                    Collection<BiHolder<Class<?>, String>> defaultKeys,
                                    String comment, Logger logger,
                                    ConfigurationMigrator... migrators)
            throws IOException, CorruptedConfigurationException {
        var revision = migrators.length;
        this.file = new File(dataFolder, name);
        var legacyFile = legacyFile(file);
        var migratedFromLegacy = !file.exists() && legacyFile.exists();
        newlyCreated = !file.exists() && !migratedFromLegacy;

        if (migratedFromLegacy) {
            logger.info("Migrating " + legacyFile.getName() + " to " + file.getName() + " (YAML)");
        } else if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could not create configuration file!");
        }

        var refHelper = new ConfigurateHelper(CommentedConfigurationNode.root().comment(comment));
        var extractedKeys = defaultKeys.stream()
                .map(data -> new BiHolder<>(orderedKeys(GeneralUtil.extractKeys(data.key()), data.value()), data.value()))
                .toList();

        // Configurate 4.1.x does not preserve node comments when writing YAML.
        // They are injected as valid YAML '#' lines after serialization.
        var commentsByPath = new LinkedHashMap<String, String>();
        for (var key : extractedKeys) {
            for (ConfigurationKey<?> configurationKey : key.key()) {
                var path = key.value() + configurationKey.key();
                var keyComment = configurationKey.comment();
                if (keyComment != null && !keyComment.isBlank()) {
                    commentsByPath.put(path, keyComment);
                }
            }
        }
        commentsByPath.put("revision", "The config revision number. !!DO NOT TOUCH THIS!!");
        this.commentsByPath = commentsByPath;

        for (var key : extractedKeys) {
            for (ConfigurationKey<?> configurationKey : key.key()) {
                refHelper.setDefault(configurationKey, key.value());
            }
        }

        var ref = refHelper.configuration();
        loader = YamlConfigurationLoader.builder()
                .defaultOptions(ConfigurationOptions.defaults().header(ref.comment()))
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .file(file)
                .build();

        if (migratedFromLegacy) {
            try {
                var legacyNode = HoconConfigurationLoader.builder()
                        .file(legacyFile)
                        .build()
                        .load();
                var backup = new File(legacyFile.getParentFile(), legacyFile.getName() + ".pre-yaml.bak");
                if (!backup.exists()) {
                    Files.copy(legacyFile.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }
                saveAtomically(legacyNode, file, commentsByPath);
            } catch (IOException e) {
                Files.deleteIfExists(file.toPath());
                throw new CorruptedConfigurationException(e);
            }
        }

        try {
            helper = new ConfigurateHelper(loader.load().mergeFrom(ref));
        } catch (ConfigurateException e) {
            throw new CorruptedConfigurationException(e);
        }

        var presentRevision = helper.getInt("revision");
        if (presentRevision == null) presentRevision = newlyCreated ? revision : 0;
        if (presentRevision < revision) {
            for (int i = presentRevision; i < revision; i++) {
                migrators[i].migrate(helper, logger);
            }
        }

        helper.configuration().mergeFrom(ref);
        helper.configuration().node("revision")
                .set(revision)
                .comment("The config revision number. !!DO NOT TOUCH THIS!!");

        for (var key : extractedKeys) {
            for (ConfigurationKey<?> configurationKey : key.key()) {
                helper.setComment(configurationKey, key.value());
            }
        }
        save();
    }

    private static List<ConfigurationKey<?>> orderedKeys(List<ConfigurationKey<?>> keys, String prefix) {
        if (!prefix.contains("database.properties.") && !prefix.contains("migration.old-database.")) {
            return keys;
        }
        var order = Map.of(
                "database", 0,
                "host", 1,
                "port", 2,
                "user", 3,
                "password", 4
        );
        return keys.stream()
                .sorted(Comparator.comparingInt(key -> order.getOrDefault(key.key(), 100)))
                .toList();
    }

    private static File legacyFile(File yamlFile) {
        var name = yamlFile.getName();
        var yamlName = name.endsWith(".yml") ? name.substring(0, name.length() - 4) : name;
        return new File(yamlFile.getParentFile(), yamlName + ".conf");
    }

    public ConfigurateHelper getHelper() {
        return helper;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    private static void saveAtomically(CommentedConfigurationNode node,
                                       File target,
                                       Map<String, String> commentsByPath) throws IOException {
        var temporary = Files.createTempFile(target.getParentFile().toPath(), target.getName(), ".tmp").toFile();
        try {
            var temporaryLoader = YamlConfigurationLoader.builder()
                    .indent(2)
                    .nodeStyle(NodeStyle.BLOCK)
                    .file(temporary)
                    .build();
            temporaryLoader.save(node);

            var text = Files.readString(temporary.toPath());
            Files.writeString(temporary.toPath(), injectComments(text, commentsByPath));

            // Parse the exact output before replacing the live file.
            temporaryLoader.load();
            try {
                Files.move(temporary.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    /** Inserts compact, valid YAML comments above their documented keys. */
    private static String injectComments(String yaml, Map<String, String> commentsByPath) {
        if (commentsByPath.isEmpty()) return yaml;

        var lines = yaml.split("\n", -1);
        var path = new ArrayList<String>();
        var out = new StringBuilder(yaml.length() + 512);
        var blockIndent = -1;

        for (String line : lines) {
            var trimmed = line.stripLeading();
            var indent = line.length() - trimmed.length();

            if (blockIndent >= 0) {
                if (!trimmed.isEmpty() && indent <= blockIndent) {
                    blockIndent = -1;
                } else {
                    out.append(line).append('\n');
                    continue;
                }
            }

            var colon = trimmed.indexOf(':');
            if (colon > 0 && !trimmed.startsWith("#") && !trimmed.startsWith("- ")) {
                var depth = indent / 2;
                if (path.size() > depth) path.subList(depth, path.size()).clear();
                while (path.size() < depth) path.add("");
                path.add(trimmed.substring(0, colon).trim());

                var comment = commentsByPath.get(String.join(".", path));
                if (comment != null) {
                    for (var commentLine : comment.split("\n", -1)) {
                        var commentText = commentLine.strip();
                        if (!commentText.isBlank()) {
                            out.append(" ".repeat(indent)).append("# ").append(commentText).append('\n');
                        }
                    }
                }

                var value = trimmed.substring(colon + 1).trim();
                if (value.equals("|") || value.equals("|-") || value.equals("|+")
                        || value.equals(">") || value.equals(">-") || value.equals(">+")) {
                    blockIndent = indent;
                }
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /**
     * Converts scalar mapping values to double quotes while leaving lists and
     * block scalars intact. This keeps multiline email/message bodies valid.
     */
    private static String quoteYamlStringScalars(String yaml) {
        var lines = yaml.split("\n", -1);
        var out = new StringBuilder(yaml.length() + 512);
        var blockIndent = -1;

        for (String line : lines) {
            var trimmed = line.stripLeading();
            var indent = line.length() - trimmed.length();

            if (blockIndent >= 0) {
                if (!trimmed.isEmpty() && indent <= blockIndent) {
                    blockIndent = -1;
                } else {
                    out.append(line).append('\n');
                    continue;
                }
            }

            var colon = trimmed.indexOf(':');
            if (colon <= 0 || trimmed.startsWith("#") || trimmed.startsWith("- ")) {
                out.append(line).append('\n');
                continue;
            }

            var key = trimmed.substring(0, colon).trim();
            var value = trimmed.substring(colon + 1).trim();
            if (value.equals("|") || value.equals("|-") || value.equals("|+")
                    || value.equals(">") || value.equals(">-") || value.equals(">+")) {
                blockIndent = indent;
                out.append(line).append('\n');
                continue;
            }
            if (value.isEmpty() || value.startsWith("[") || value.startsWith("{")) {
                out.append(line).append('\n');
                continue;
            }

            // Configurate may emit a single-quoted scalar. Decode that style
            // before emitting a double-quoted scalar.
            if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1).replace("''", "'");
            } else if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                out.append(line).append('\n');
                continue;
            }

            out.append(" ".repeat(indent)).append(key).append(": \"")
                    .append(escapeYamlDoubleQuoted(value)).append("\"\n");
        }
        return out.toString();
    }

    private static String escapeYamlDoubleQuoted(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    public void save() throws IOException {
        try {
            saveAtomically(helper.configuration(), file, commentsByPath);
        } catch (ConfigurateException e) {
            throw new IOException("Could not save YAML configuration atomically", e);
        }
    }
}
