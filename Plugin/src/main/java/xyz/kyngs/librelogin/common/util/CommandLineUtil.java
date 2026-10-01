/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CommandLineUtil {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private CommandLineUtil() {
    }

    /**
     * Builds the /login alias list. {@code /log} is optional because other
     * plugins and mods (for example Carpet) may already own that command.
     */
    public static String loginAliases(boolean includeLogAlias) {
        return includeLogAlias ? "login|l|log" : "login|l";
    }

    public static String normalize(String commandLine) {
        var normalized = commandLine.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ");

        var separator = normalized.indexOf(' ');
        var root = separator < 0 ? normalized : normalized.substring(0, separator);
        var namespaceSeparator = root.indexOf(':');
        if (namespaceSeparator >= 0) {
            root = root.substring(namespaceSeparator + 1);
        }
        return separator < 0 ? root : root + normalized.substring(separator);
    }

    public static String root(String commandLine) {
        var normalized = normalize(commandLine);
        var separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }
}
