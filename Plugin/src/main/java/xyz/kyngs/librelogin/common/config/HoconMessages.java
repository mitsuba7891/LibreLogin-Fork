/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.CommentedConfigurationNode;
import xyz.kyngs.librelogin.api.BiHolder;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.configuration.CorruptedConfigurationException;
import xyz.kyngs.librelogin.api.configuration.Messages;
import xyz.kyngs.librelogin.common.config.migrate.messages.FirstMessagesMigrator;
import xyz.kyngs.librelogin.common.config.migrate.messages.SecondMessagesMigrator;
import xyz.kyngs.librelogin.common.config.migrate.messages.ThirdMessagesMigrator;
import xyz.kyngs.librelogin.common.config.migrate.messages.FourthMessagesMigrator;
import xyz.kyngs.librelogin.common.util.GeneralUtil;
import xyz.kyngs.utils.legacymessage.LegacyMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static xyz.kyngs.librelogin.common.config.MessageKeys.PREFIX;

public class HoconMessages implements Messages {

    /** Default Minecraft chat width in pixels, used by the [center] marker. */
    private static final int CHAT_WIDTH = 320;

    private static final MiniMessage SERIALIZER = MiniMessage.builder()
            .build();
    private final Map<String, TextComponent> messages;
    private final Logger logger;
    private ConfigurateConfiguration rawMessages;

    public HoconMessages(Logger logger) {
        this.logger = logger;
        messages = new HashMap<>();
    }

    public Map<String, TextComponent> getMessages() {
        return messages;
    }

    @Override
    public TextComponent getMessage(String key, String... replacements) {

        var message = messages.get(key);

        if (message != null && shouldApplyPrefix(key)) {
            var prefix = messages.get(PREFIX.key());
            var prefixed = Component.text();
            if (prefix != null) prefixed.append(prefix);
            prefixed.append(message);
            message = prefixed.build();
        }

        if (replacements.length == 0) return message;

        var replaceMap = new HashMap<String, String>();

        String toReplace = null;

        for (int i = 0; i < replacements.length; i++) {
            if (i % 2 != 0) {
                replaceMap.put(toReplace, replacements[i]);
            } else {
                toReplace = replacements[i];
            }
        }

        return GeneralUtil.formatComponent(message, replaceMap);
    }

    @Override
    public void reload(LibreLoginPlugin<?, ?> plugin) throws IOException, CorruptedConfigurationException {
        var adept = new ConfigurateConfiguration(
                plugin.getDataFolder(),
                "messages.yml",
                Set.of(new BiHolder<>(MessageKeys.class, "")),
                """
                          !!THIS FILE IS WRITTEN IN YAML FORMAT!!
                          Messages support legacy colour codes and MiniMessage syntax.
                          YAML comments use '#'; '//' is not valid YAML comment syntax.
                          ----------------------------------------------------------------------------------------
                          LibreLogin Messages (fork)
                          ----------------------------------------------------------------------------------------
                          This file contains all of the messages used by the plugin, you are welcome to fit it to your needs.
                          The messages can be written both in the legacy format and in the MiniMessage format. For example, the following message is completely valid: <bold>&aReloaded!</bold>
                          ----------------------------------------------------------------------------------------
                          Message formatting shortcuts
                          ----------------------------------------------------------------------------------------
                          - Every message value is written between double quotes, for example: prefix: "LibreLogin"
                          - A message can also be a list of quoted strings; each entry becomes one line, for example:
                              prompt-login:
                                - "Line one"
                                - "[center]&e&lLine two"
                          - "\n" (backslash n) inside a value also creates a line break.
                          - "[center]" at the start of a line centers that line using pixel-based measurement.
                          - MiniMessage tags keep working: <bold>, <italic>, <gradient:red:blue>, <size:20>.
                          ----------------------------------------------------------------------------------------
                          CHANGES APPLIED TO THIS FILE (fork; reviewed and updated with AI assistance - GPT Luna 5.6):
                          - All scalar message values are emitted with double quotes.
                          - Messages may be written as lists of quoted strings (one line per entry).
                          - "\n" is accepted as an explicit line break.
                          - "[center]" centers an individual line.
                          - The global "prefix" key controls the chat prefix; leave it empty to disable it.
                          - Rate-limit and other quoted values are stored as quoted, single-line YAML scalars.
                          ----------------------------------------------------------------------------------------
                          You can find more information about LibreLogin on the github page:
                          https://github.com/kyngs/LibreLogin
                        """,
                logger,
                true,
                new FirstMessagesMigrator(),
                new SecondMessagesMigrator(),
                new ThirdMessagesMigrator(),
                new FourthMessagesMigrator()
        );

        messages.clear();
        extractKeys("", adept.getHelper().configuration());
        rawMessages = adept;
    }

    void extractKeys(String prefix, CommentedConfigurationNode node) {
        node.childrenMap().forEach((key, value) -> {
            if (!(key instanceof String str)) return;

            if (value.isList()) {
                // A message written as a list of quoted strings: one line per entry.
                try {
                    var lines = value.getList(String.class);
                    if (lines == null || lines.isEmpty()) return;
                    messages.put(prefix + str, deserialize(String.join("\n", lines)));
                } catch (org.spongepowered.configurate.serialize.SerializationException ignored) {
                    // Ignore malformed list values; the key stays absent.
                }
            } else if (value.childrenMap().isEmpty()) {
                var string = value.getString();

                if (string == null) return;

                messages.put(prefix + str, deserialize(string));
            } else {
                extractKeys(prefix + str + ".", value);
            }
        });
    }

    private static TextComponent deserialize(String raw) {
        return Component.empty().append(SERIALIZER.deserialize(LegacyMessage.fromLegacy(process(raw), "&")));
    }

    /**
     * Applies the fork message syntax before MiniMessage conversion:
     * <ul>
     *   <li>{@code \n} (literal backslash-n) becomes a real line break;</li>
     *   <li>{@code [center]} at the start of a line pads that line to center it in chat.</li>
     * </ul>
     */
    static String process(String raw) {
        raw = raw.replace("\\n", "\n");
        var lines = raw.split("\n", -1);
        var out = new StringBuilder(raw.length() + 32);
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (line.startsWith("[center]")) {
                line = center(line.substring("[center]".length()));
            }
            out.append(line);
            if (i < lines.length - 1) out.append('\n');
        }
        return out.toString();
    }

    private static String center(String line) {
        var width = pixelWidth(line);
        if (width >= CHAT_WIDTH) return line;
        var leftPx = Math.max((CHAT_WIDTH - width) / 2, 0);
        return " ".repeat(leftPx / 4) + line;
    }

    private static int pixelWidth(String line) {
        // Legacy colour/formatting codes do not consume visible width, and
        // MiniMessage tags with arguments (<color:red>, <gradient:red:blue>,
        // <#FF0000>, <bold>) are not rendered. Literal placeholders such as
        // <password> or <2fa> inside prompts must still be measured, so only
        // known MiniMessage tag names, tags with arguments and hex colours are
        // stripped.
        var visible = line.replaceAll("(?i)&[0-9a-fk-orx]", "")
                .replaceAll("(?i)<(?:(?:#[0-9a-fA-F]{6})|(?:[a-z]+:[^>]*)|(?:/?(?:bold|italic|underlined|strikethrough|obfuscated|reset|newline|br|white|black|gray|dark_gray|red|dark_red|gold|yellow|green|dark_green|aqua|dark_aqua|blue|dark_blue|light_purple|dark_purple|color|gradient|rainbow|size|font|click|hover|insertion|key|keybind|lang|translate|selector|score|nbt|transition|decorate|shadow)))>", "");
        var width = 0;
        for (int i = 0; i < visible.length(); i++) {
            var c = visible.charAt(i);
            if (c == 'i' || c == 'l') {
                width += 2;
            } else if (c == 'I' || c == 't' || c == '[' || c == ']' || c == '\'' || c == '.') {
                width += 4;
            } else if (Character.isUpperCase(c) || (c >= '0' && c <= '9')) {
                width += 6;
            } else if (c == '@') {
                width += 7;
            } else {
                width += 5;
            }
        }
        return width;
    }

    private boolean shouldApplyPrefix(String key) {
        return !key.equals(PREFIX.key())
                && !key.startsWith("title-")
                && !key.startsWith("sub-title-")
                && !key.startsWith("action-bar-")
                && !key.startsWith("email-");
    }

    public String getRawMessage(String key) {
        return rawMessages.getHelper().getString(key);
    }
}
