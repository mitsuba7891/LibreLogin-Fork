/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.log;

import java.util.Set;

import static xyz.kyngs.librelogin.common.util.CommandLineUtil.normalize;
import static xyz.kyngs.librelogin.common.util.CommandLineUtil.root;

public abstract class LogFilter {

    private static final Set<String> PROTECTED_ROOT_COMMANDS = Set.of(
            "login", "l", "log", "register", "reg", "premium", "autologin",
            "2faconfirm", "changepassword", "changepass", "passch", "passwd",
            "confirmpasswordreset", "setemail"
    );
    private static final Set<String> PROTECTED_STAFF_COMMANDS = Set.of(
            "librelogin user register", "librelogin user pass-change"
    );
    private static final String[] COMMAND_LOG_MARKERS = {
            "issued server command: /",
            "executed command /",
            "executed command: /"
    };

    protected boolean checkMessage(String message) {
        if (message == null) return true;

        var normalized = message.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("duplicate key name")) return false;

        for (String marker : COMMAND_LOG_MARKERS) {
            var markerIndex = normalized.indexOf(marker);
            if (markerIndex < 0) continue;

            var commandLine = normalize(normalized.substring(markerIndex + marker.length()));

            if (PROTECTED_ROOT_COMMANDS.contains(root(commandLine))) return false;
            for (String protectedCommand : PROTECTED_STAFF_COMMANDS) {
                if (commandLine.equals(protectedCommand) || commandLine.startsWith(protectedCommand + " ")) {
                    return false;
                }
            }
        }

        return true;
    }

    public abstract void inject();

}
