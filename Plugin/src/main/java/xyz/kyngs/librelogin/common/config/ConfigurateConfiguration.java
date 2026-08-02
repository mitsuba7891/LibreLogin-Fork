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
import java.nio.file.Path;
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
    private final boolean quoteStringScalars;

    public ConfigurateConfiguration(File dataFolder, String name,
                                    Collection<BiHolder<Class<?>, String>> defaultKeys,
                                    String comment, Logger logger,
                                    ConfigurationMigrator... migrators)
            throws IOException, CorruptedConfigurationException {
        this(dataFolder, name, defaultKeys, comment, logger, false, migrators);
    }

    /**
     * @param quoteStringScalars when {@code true} every scalar message value
     *                           (including list entries) is written between
     *                           double quotes. Used for messages.yml; config.yml
     *                           keeps typed values unquoted.
     */
    public ConfigurateConfiguration(File dataFolder, String name,
                                    Collection<BiHolder<Class<?>, String>> defaultKeys,
                                    String comment, Logger logger,
                                    boolean quoteStringScalars,
                                    ConfigurationMigrator... migrators)
            throws IOException, CorruptedConfigurationException {
        this.quoteStringScalars = quoteStringScalars;
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
                saveAtomically(quoteStringScalars, legacyNode, file, commentsByPath);
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

    private static void saveAtomically(boolean quoteStringScalars,
                                       CommentedConfigurationNode node,
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

            var raw = Files.readString(temporary.toPath());
            var text = quoteStringScalars ? quoteYamlStringScalars(raw) : raw;
            Files.writeString(temporary.toPath(), injectComments(text, commentsByPath));

            // Parse the exact output before replacing the live file. On failure
            // keep a copy of both documents for diagnosis when the debug flag is
            // set (e.g. -Dlibrelogin.debug-yaml-dump=true); servers are never
            // polluted with build directories by default.
            try {
                temporaryLoader.load();
            } catch (ConfigurateException e) {
                if (Boolean.getBoolean("librelogin.debug-yaml-dump")) {
                    try {
                        var debugDir = Path.of("build").toAbsolutePath();
                        Files.createDirectories(debugDir);
                        Files.writeString(debugDir.resolve(target.getName() + ".raw-debug.yml"), raw);
                        Files.writeString(debugDir.resolve(target.getName() + ".quoted-debug.yml"), text);
                    } catch (IOException ignored) {
                        // Diagnostics are best-effort.
                    }
                }
                throw e;
            }
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
     * Converts scalar mapping values (plain, single-quoted, folded across lines
     * or block scalars) to single-line double-quoted scalars, so that
     * messages.yml contains only quoted strings. Values that are already
     * complete double-quoted scalars, flow collections and the revision number
     * are left untouched.
     */
    static String quoteYamlStringScalars(String yaml) {
        var lines = yaml.split("\n", -1);
        var out = new StringBuilder(yaml.length() + 512);

        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];
            var trimmed = line.stripLeading();
            var indent = line.length() - trimmed.length();

            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                out.append(line).append('\n');
                continue;
            }

            // Multi-line values emitted as block scalars (|- | > etc.) are folded
            // into one double-quoted scalar with escaped line breaks (folded
            // style > joins with a space, literal style | keeps the newlines).
            var marker = blockScalarMarker(trimmed);
            if (marker != null) {
                var isListItem = trimmed.startsWith("- ") || trimmed.equals("-");
                var prefix = trimmed.substring(0, trimmed.length() - marker.length()).strip();
                var content = new ArrayList<String>();
                var contentIndent = -1;
                int j = i + 1;
                for (; j < lines.length; j++) {
                    var l = lines[j];
                    if (l.isBlank()) {
                        content.add("");
                        continue;
                    }
                    var lt = l.stripLeading();
                    var li = l.length() - lt.length();
                    // Only lines strictly deeper than the block key belong to the
                    // block. This also keeps an empty block scalar from swallowing
                    // the next sibling key into its folded value.
                    if (li <= indent) break;
                    if (contentIndent < 0) contentIndent = li;
                    if (li < contentIndent) break;
                    content.add(lt);
                }
                while (!content.isEmpty() && content.get(content.size() - 1).isEmpty()) {
                    content.remove(content.size() - 1);
                }
                var separator = marker.contains(">") ? " " : "\n";
                var value = String.join(separator, content);
                if (isListItem) {
                    out.append(" ".repeat(indent)).append("- \"")
                            .append(escapeYamlDoubleQuoted(value)).append("\"\n");
                } else {
                    // `prefix` keeps its trailing colon (e.g. `info-user:`); do not
                    // append another one or the emitted line would be `info-user:: ""`.
                    var key = prefix.endsWith(":") ? prefix : prefix + ":";
                    out.append(" ".repeat(indent)).append(key).append(" \"")
                            .append(escapeYamlDoubleQuoted(value)).append("\"\n");
                }
                i = j - 1;
                continue;
            }

            var isListItem = trimmed.startsWith("- ") || trimmed.equals("-");
            String key;
            String value;
            if (isListItem) {
                key = null;
                value = (trimmed.equals("-") ? "" : trimmed.substring(2)).strip();
            } else {
                var colon = trimmed.indexOf(':');
                if (colon <= 0) {
                    out.append(line).append('\n');
                    continue;
                }
                key = trimmed.substring(0, colon).trim();
                value = trimmed.substring(colon + 1).strip();
                if (key.equals("revision")) {
                    out.append(line).append('\n');
                    continue;
                }
            }

            if (value.isEmpty() || isUnquotable(value)) {
                out.append(line).append('\n');
                continue;
            }
            // Already a complete, single-line double-quoted scalar: keep as-is.
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                    && !value.endsWith("\\\"")) {
                out.append(line).append('\n');
                continue;
            }

            // Plain / single-quoted scalars that SnakeYAML wrapped across lines
            // are folded back: YAML folds each single line break into a space.
            var folded = foldWrappedScalar(lines, i, indent, value);
            i = folded.index - 1;
            var quoted = "\"" + escapeYamlDoubleQuoted(folded.value) + "\"";
            if (isListItem) {
                out.append(" ".repeat(indent)).append("- ").append(quoted).append('\n');
            } else {
                out.append(" ".repeat(indent)).append(key).append(": ").append(quoted).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Joins the continuation lines of a plain or quoted scalar that SnakeYAML
     * wrapped at the preferred line width back into a single value. Continuation
     * lines are indented deeper than the mapping key; YAML folding turns each
     * single line break into a space.
     */
    private static WrappedScalar foldWrappedScalar(String[] lines, int start, int keyIndent, String first) {
        var parts = new ArrayList<String>();
        parts.add(first);
        int j = start + 1;
        for (; j < lines.length; j++) {
            var l = lines[j];
            if (l.isBlank()) break;
            var lt = l.stripLeading();
            var li = l.length() - lt.length();
            if (li <= keyIndent) break;
            parts.add(lt);
        }
        var joined = String.join(" ", parts);
        var value = joined;
        if (joined.startsWith("'") && joined.endsWith("'")) {
            value = joined.substring(1, joined.length() - 1).replace("''", "'");
        } else if (joined.startsWith("\"") && joined.endsWith("\"")) {
            value = decodeYamlDoubleQuoted(joined.substring(1, joined.length() - 1));
        }
        return new WrappedScalar(value, j);
    }

    private record WrappedScalar(String value, int index) { }

    private static String blockScalarMarker(String trimmed) {
        for (var marker : new String[]{"|-", "|+", ">-", ">+", "|", ">"}) {
            if (trimmed.endsWith(marker)) {
                var before = trimmed.substring(0, trimmed.length() - marker.length()).strip();
                if (before.endsWith(":")) return marker;
                if (before.equals("-")) return marker;
                if (before.startsWith("- ") && before.substring(2).strip().isEmpty()) return marker;
            }
        }
        return null;
    }

    private static boolean isUnquotable(String value) {
        return value.isEmpty()
                || value.startsWith("[") || value.startsWith("{")
                || value.equals("|") || value.equals("|-") || value.equals("|+")
                || value.equals(">") || value.equals(">-") || value.equals(">+");
    }

    /** Decodes the escapes of a YAML double-quoted scalar. */
    private static String decodeYamlDoubleQuoted(String value) {
        var out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            var next = value.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case '0' -> out.append('\0');
                case 'a' -> out.append('\u0007');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'v' -> out.append('\u000B');
                case 'e' -> out.append('\u001B');
                case ' ' -> out.append(' ');
                case 'N' -> out.append('\u0085');
                case '_' -> out.append('\u00A0');
                case 'L' -> out.append('\u2028');
                case 'P' -> out.append('\u2029');
                case 'x' -> {
                    if (i + 3 <= value.length()) {
                        out.append((char) Integer.parseInt(value.substring(i + 1, i + 3), 16));
                        i += 2;
                    }
                }
                case 'u' -> {
                    if (i + 5 <= value.length()) {
                        out.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                case 'U' -> {
                    if (i + 9 <= value.length()) {
                        out.appendCodePoint(Integer.parseInt(value.substring(i + 1, i + 9), 16));
                        i += 8;
                    }
                }
                default -> out.append('\\').append(next);
            }
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
            saveAtomically(quoteStringScalars, helper.configuration(), file, commentsByPath);
        } catch (ConfigurateException e) {
            throw new IOException("Could not save YAML configuration atomically", e);
        }
    }
}
