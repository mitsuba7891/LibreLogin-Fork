/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CommandHelpMenuTest {

    @Test
    void everyCommandIsReachableExactlyOnceAcrossPages() {
        var entries = IntStream.range(0, 31)
                .mapToObj(i -> entry("/librelogin user command" + i, "<name>", Set.of()))
                .toList();
        var seen = new HashSet<String>();
        int pages = CommandHelpMenu.pageCount(entries.size());

        for (int page = 1; page <= pages; page++) {
            var messages = render(entries, String.valueOf(page));
            var suggestions = suggestions(messages);
            assertEquals(Math.min(CommandHelpMenu.PAGE_SIZE, entries.size() - (page - 1) * CommandHelpMenu.PAGE_SIZE), suggestions.size());
            for (var suggestion : suggestions) {
                assertTrue(seen.add(suggestion.value().trim()), "Command repeated across pages");
                assertFalse(suggestion.value().contains("<name>"), "Do not insert placeholders into chat input");
            }
            assertTrue(text(messages.get(1)).contains("Page " + page + "/" + pages));
        }
        assertEquals(entries.stream().map(CommandHelpMenu.Entry::command).collect(java.util.stream.Collectors.toSet()), seen);
    }

    @Test
    void navigationRunsOnlyValidHelpPagesAndDisablesTheBoundaries() {
        var entries = IntStream.range(0, 13).mapToObj(i -> entry("/command" + i, "", Set.of())).toList();
        var first = render(entries, "1");
        assertNull(button(first, "[« Previous]").clickEvent());
        assertEquals(ClickEvent.runCommand("/librelogin 2"), button(first, "[Next »]").clickEvent());

        var middle = render(entries, "2");
        assertEquals(ClickEvent.runCommand("/librelogin 1"), button(middle, "[« Previous]").clickEvent());
        assertEquals(ClickEvent.runCommand("/librelogin 3"), button(middle, "[Next »]").clickEvent());

        var last = render(entries, "3");
        assertEquals(ClickEvent.runCommand("/librelogin 2"), button(last, "[« Previous]").clickEvent());
        assertNull(button(last, "[Next »]").clickEvent());
    }

    @Test
    void staffActionsAreSuggestionsNeverExecutedByClicking() {
        var entry = new CommandHelpMenu.Entry("/librelogin user delete", "<name>", "Delete an offline account.",
                List.of("/alias user delete"), Set.of("librepremium.user.delete"));
        var messages = render(List.of(entry), null);
        var command = messages.stream().filter(message -> message.clickEvent() != null).findFirst().orElseThrow();
        assertEquals(ClickEvent.suggestCommand("/librelogin user delete "), command.clickEvent());
        assertTrue(text(command).contains("<name>"));
        assertTrue(text(command).contains("Delete an offline account."));
        String hover = text((Component) command.hoverEvent().value());
        assertTrue(hover.contains("/alias user delete"));
        assertTrue(hover.contains("librepremium.user.delete"));
        assertTrue(messages.stream().flatMap(CommandHelpMenuTest::walk)
                .map(Component::clickEvent).filter(java.util.Objects::nonNull)
                .noneMatch(click -> click.action() == ClickEvent.Action.RUN_COMMAND));
    }

    @Test
    void invalidAndOverflowingPageNumbersReturnAUsefulRecoveryLink() {
        for (String page : List.of("0", "-1", "not-a-page", "2147483648", "999999999999999999999", "2", "")) {
            var messages = render(List.of(entry("/librelogin about", "", Set.of())), page);
            assertEquals(1, messages.size(), page);
            assertTrue(text(messages.getFirst()).contains("between 1 and 1"), page);
            assertEquals(ClickEvent.runCommand("/librelogin 1"), messages.getFirst().clickEvent(), page);
        }
    }

    @Test
    void defaultAndEmptyPagesAreSafeAndPageCountsHaveNoPhantomLastPage() {
        assertEquals(1, CommandHelpMenu.pageCount(0));
        assertEquals(1, CommandHelpMenu.pageCount(6));
        assertEquals(2, CommandHelpMenu.pageCount(7));
        assertEquals(2, CommandHelpMenu.pageCount(12));
        assertEquals(3, CommandHelpMenu.pageCount(13));
        assertEquals(render(List.of(), "1"), render(List.of(), null));
        var empty = render(List.of(), null);
        assertTrue(suggestions(empty).isEmpty());
        assertTrue(text(empty.get(1)).contains("0 commands available"));
        assertNull(button(empty, "[Next »]").clickEvent());
    }

    @Test
    void permissionsAreAppliedBeforePaginationWithoutChangingLegacyNodes() {
        var entries = List.of(
                entry("/login", "<password>", Set.of()),
                entry("/librelogin user delete", "<name>", Set.of("librepremium.user.delete")),
                entry("/librelogin about", "", Set.of()),
                entry("/librelogin user info", "<name>", Set.of("librepremium.user.info", "extra.permission")));
        var publicEntries = CommandHelpMenu.visibleEntries(entries, permission -> false);
        assertEquals(List.of("/librelogin about", "/login"), publicEntries.stream().map(CommandHelpMenu.Entry::command).toList());
        var partial = CommandHelpMenu.visibleEntries(entries, Set.of("librepremium.user.info")::contains);
        assertEquals(publicEntries, partial, "All required permissions must be present");
        var allowed = CommandHelpMenu.visibleEntries(entries, permission -> true);
        assertEquals(entries.size(), allowed.size());
    }

    @Test
    void entryMetadataCannotBeChangedAfterTheHelpSnapshotWasCreated() {
        var aliases = new ArrayList<>(List.of("/l"));
        var permissions = new HashSet<>(Set.of("example.permission"));
        var entry = new CommandHelpMenu.Entry("/login", "<password>", "Log in.", aliases, permissions);
        aliases.clear();
        permissions.clear();
        assertEquals(List.of("/l"), entry.aliases());
        assertEquals(Set.of("example.permission"), entry.permissions());
    }

    private static CommandHelpMenu.Entry entry(String command, String syntax, Set<String> permissions) {
        return new CommandHelpMenu.Entry(command, syntax, "Command description.", List.of(), permissions);
    }

    private static List<Component> render(List<CommandHelpMenu.Entry> entries, String page) {
        return CommandHelpMenu.render(entries, page, "0.25.0", Component.text("Platform: test"));
    }

    private static List<ClickEvent> suggestions(List<Component> messages) {
        return messages.stream().map(Component::clickEvent).filter(java.util.Objects::nonNull)
                .filter(click -> click.action() == ClickEvent.Action.SUGGEST_COMMAND).toList();
    }

    private static Component button(List<Component> messages, String label) {
        return messages.stream().flatMap(CommandHelpMenuTest::walk)
                .filter(component -> component instanceof TextComponent text && text.content().equals(label))
                .findFirst().orElseThrow();
    }

    private static Stream<Component> walk(Component component) {
        return Stream.concat(Stream.of(component), component.children().stream().flatMap(CommandHelpMenuTest::walk));
    }

    private static String text(Component component) {
        return (component instanceof TextComponent text ? text.content() : "")
                + component.children().stream().map(CommandHelpMenuTest::text).collect(java.util.stream.Collectors.joining());
    }
}
