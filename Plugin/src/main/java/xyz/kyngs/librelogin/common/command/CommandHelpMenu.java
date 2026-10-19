/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Renders a permission-aware, paginated view of the registered commands. */
public final class CommandHelpMenu {

    public static final int PAGE_SIZE = 6;
    private static final String WIKI_URL = "https://github.com/mitsuba7891/LibreLogin-Fork/wiki";

    private CommandHelpMenu() {
    }

    public record Entry(String command, String syntax, String description, List<String> aliases,
                        Set<String> permissions) {
        public Entry {
            aliases = List.copyOf(aliases);
            permissions = Set.copyOf(permissions);
        }
    }

    public static List<Entry> visibleEntries(Collection<Entry> entries, Predicate<String> hasPermission) {
        return entries.stream()
                .filter(entry -> entry.permissions().stream().allMatch(hasPermission))
                .sorted(Comparator.comparing((Entry entry) -> !entry.command().startsWith("/librelogin "))
                        .thenComparing(Entry::command))
                .toList();
    }

    public static int pageCount(int entryCount) {
        return Math.max(1, Math.ceilDiv(entryCount, PAGE_SIZE));
    }

    public static List<Component> render(List<Entry> entries, String requestedPage, String version, Component status) {
        int pages = pageCount(entries.size());
        int page;
        try {
            page = requestedPage == null ? 1 : Integer.parseInt(requestedPage);
        } catch (NumberFormatException ignored) {
            page = 0;
        }
        if (page < 1 || page > pages) {
            return List.of(Component.text("Choose a help page between 1 and " + pages
                            + ": /librelogin <page>", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/librelogin 1"))
                    .hoverEvent(HoverEvent.showText(Component.text("Open help page 1", NamedTextColor.YELLOW))));
        }

        var messages = new ArrayList<Component>();
        messages.add(Component.text("━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("LibreLogin-Fork", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" v" + version, NamedTextColor.YELLOW))
                .append(Component.text(" ━━", NamedTextColor.DARK_GRAY))
                .hoverEvent(HoverEvent.showText(status)));
        messages.add(Component.text("Help • Page " + page + "/" + pages, NamedTextColor.GOLD)
                .append(Component.text(" • " + entries.size() + " commands available", NamedTextColor.GRAY)));
        messages.add(Component.text("Click to fill in a command. Hover for details and aliases.", NamedTextColor.DARK_GRAY));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for (var entry : entries.subList(start, end)) {
            messages.add(renderEntry(entry));
        }
        messages.add(navigation(page, pages));
        messages.add(Component.empty()
                .append(Component.text("Wiki", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(WIKI_URL))
                        .hoverEvent(HoverEvent.showText(Component.text("Open documentation", NamedTextColor.GRAY))))
                .append(Component.text(" • /librelogin <page> or /librelogin help <page>", NamedTextColor.DARK_GRAY)));
        return List.copyOf(messages);
    }

    private static Component renderEntry(Entry entry) {
        String description = entry.description().isBlank() ? "No description available." : entry.description();
        Component hover = Component.text(description, NamedTextColor.GRAY);
        if (!entry.aliases().isEmpty()) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Aliases: " + String.join(", ", entry.aliases()), NamedTextColor.YELLOW));
        }
        if (!entry.permissions().isEmpty()) {
            hover = hover.append(Component.newline())
                    .append(Component.text("Permission: " + String.join(", ", entry.permissions().stream().sorted().toList()),
                            NamedTextColor.DARK_GRAY));
        }
        Component usage = Component.text("  " + entry.command(), NamedTextColor.AQUA);
        if (!entry.syntax().isBlank()) {
            usage = usage.append(Component.space()).append(legacy(entry.syntax(), NamedTextColor.GRAY));
        }
        return Component.empty().append(usage)
                .append(Component.newline()).append(Component.text("    ").append(legacy(description, NamedTextColor.GRAY)))
                // Only navigation runs commands. Account/staff actions are always suggestions,
                // and placeholders are deliberately not inserted into the player's input.
                .clickEvent(ClickEvent.suggestCommand(entry.command() + (entry.syntax().isBlank() ? "" : " ")))
                .hoverEvent(HoverEvent.showText(hover));
    }

    private static Component legacy(String text, NamedTextColor color) {
        return LegacyComponentSerializer.legacySection().deserialize(text).colorIfAbsent(color);
    }

    private static Component navigation(int page, int pages) {
        return Component.empty()
                .append(pageButton("[« Previous]", page - 1, pages))
                .append(Component.text("  " + page + " / " + pages + "  ", NamedTextColor.GRAY))
                .append(pageButton("[Next »]", page + 1, pages));
    }

    private static Component pageButton(String label, int target, int pages) {
        if (target < 1 || target > pages) {
            return Component.text(label, NamedTextColor.DARK_GRAY);
        }
        return Component.text(label, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/librelogin " + target))
                .hoverEvent(HoverEvent.showText(Component.text("Open help page " + target + "/" + pages,
                        NamedTextColor.GRAY)));
    }
}
