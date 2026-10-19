/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps the plugin ID change from creating a second, empty authentication database. */
public final class PluginDataDirectory {

    private PluginDataDirectory() {
    }

    public static Path resolve(Path preferred, String... legacyNames) {
        if (hasConfiguration(preferred)) return preferred;

        for (String name : legacyNames) {
            var legacy = preferred.resolveSibling(name);
            if (hasConfiguration(legacy)) return legacy;
        }
        for (String name : legacyNames) {
            var legacy = preferred.resolveSibling(name);
            if (Files.isDirectory(legacy)) return legacy;
        }
        // New installations use librelogin-fork. Existing files are never moved,
        // merged or overwritten just because the plugin's internal name changed.
        return preferred;
    }

    private static boolean hasConfiguration(Path folder) {
        return Files.isRegularFile(folder.resolve("config.yml"))
                || Files.isRegularFile(folder.resolve("config.conf"));
    }
}
