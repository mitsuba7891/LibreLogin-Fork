/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;
import xyz.kyngs.librelogin.api.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormattingTest {

    private static final class NoOpLogger implements Logger {
        @Override public void info(String message) { }
        @Override public void info(String message, Throwable throwable) { }
        @Override public void warn(String message) { }
        @Override public void warn(String message, Throwable throwable) { }
        @Override public void error(String message) { }
        @Override public void error(String message, Throwable throwable) { }
        @Override public void debug(String message) { }
        @Override public void debug(String message, Throwable throwable) { }
    }

    private static String plainText(Component component) {
        var out = new StringBuilder();
        collectText(component, out);
        return out.toString();
    }

    private static void collectText(Component component, StringBuilder out) {
        if (component instanceof TextComponent text) out.append(text.content());
        component.children().forEach(child -> collectText(child, out));
    }

    private static HoconMessages messagesWith(List<String> lines) {
        var messages = new HoconMessages(new NoOpLogger());
        var node = CommentedConfigurationNode.root();
        try {
            node.node("prompt-login").set(lines);
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            throw new RuntimeException(e);
        }
        messages.extractKeys("", node);
        return messages;
    }

    @Test
    void listValuesAreJoinedIntoLines() {
        var messages = messagesWith(List.of("Line one", "Line two"));
        var text = plainText(messages.getMessages().get("prompt-login"));
        assertTrue(text.contains("Line one"));
        assertTrue(text.contains("Line two"));
        assertTrue(text.contains("Line one\nLine two"), "list entries must be joined with a line break: " + text);
    }

    @Test
    void literalBackslashNBecomesLineBreak() {
        var processed = HoconMessages.process("Line one\\nLine two");
        assertTrue(processed.contains("Line one\nLine two"), "literal \\n must become a line break: " + processed);
    }

    @Test
    void centerTagPadsTheLine() {
        var processed = HoconMessages.process("[center]&e&lLibreLogin");
        assertTrue(processed.contains("LibreLogin"), "centered text must be preserved");
        assertFalse(processed.contains("[center]"), "the center marker must be removed");
        assertTrue(processed.startsWith(" "), "centered line must be padded with leading spaces: '" + processed + "'");
    }

    @Test
    void centeredListEntryRendersWithPadding() {
        var messages = messagesWith(List.of("Line one", "[center]&e&lLibreLogin"));
        var text = plainText(messages.getMessages().get("prompt-login"));
        assertTrue(text.contains("Line one\n"), "first line must be preserved");
        var centered = text.substring(text.indexOf('\n') + 1);
        assertTrue(centered.contains("LibreLogin"), "centered line must keep its text: " + text);
        assertTrue(centered.startsWith(" "), "centered line must be padded: '" + centered + "'");
        assertFalse(centered.contains("[center]"), "the center marker must not reach the client");
    }

    @Test
    void centerTagDoesNotAffectOtherLines() {
        var processed = HoconMessages.process("Plain line\n[center]Centered");
        var lines = processed.split("\n", -1);
        assertTrue(lines[0].equals("Plain line"), "lines without the marker must stay untouched");
        assertTrue(lines[1].startsWith(" "), "only the marked line is centered");
    }
}
