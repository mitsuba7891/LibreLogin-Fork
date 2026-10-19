/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginDataDirectoryTest {

    @TempDir
    Path plugins;

    @Test
    void freshInstallUsesTheForkNameWithoutCreatingOrMovingFiles() {
        var preferred = plugins.resolve("librelogin-fork");
        assertEquals(preferred, resolve());
        assertFalse(Files.exists(preferred));
    }

    @Test
    void existingVelocityDataAndDatabaseAreReusedWithoutRenaming() throws Exception {
        var legacy = Files.createDirectory(plugins.resolve("librelogin"));
        Files.writeString(legacy.resolve("config.yml"), "database: existing");
        Files.writeString(legacy.resolve("user-data.db"), "existing account data");
        assertEquals(legacy, resolve());
        assertEquals("existing account data", Files.readString(legacy.resolve("user-data.db")));
        assertFalse(Files.exists(plugins.resolve("librelogin-fork")));
    }

    @Test
    void existingPaperDataIsReused() throws Exception {
        var legacy = Files.createDirectory(plugins.resolve("LibreLogin"));
        Files.writeString(legacy.resolve("config.yml"), "database: existing");
        assertEquals(legacy, resolve());
    }

    @Test
    void configuredForkDirectoryTakesPrecedenceWithoutMergingOldAccounts() throws Exception {
        var preferred = Files.createDirectory(plugins.resolve("librelogin-fork"));
        var legacy = Files.createDirectory(plugins.resolve("librelogin"));
        Files.writeString(preferred.resolve("config.yml"), "database: current");
        Files.writeString(legacy.resolve("config.yml"), "database: old");
        assertEquals(preferred, resolve());
        assertEquals("database: old", Files.readString(legacy.resolve("config.yml")));
    }

    @Test
    void autoCreatedLibraryDirectoryDoesNotHideExistingConfiguration() throws Exception {
        var cache = Files.createDirectories(plugins.resolve("librelogin-fork/libs"));
        Files.writeString(cache.resolve("marker"), "cache");
        var legacy = Files.createDirectory(plugins.resolve("librelogin"));
        Files.writeString(legacy.resolve("config.yml"), "database: existing");
        assertEquals(legacy, resolve());
        assertEquals("cache", Files.readString(cache.resolve("marker")));
    }

    @Test
    void premiumAndHoconDirectoriesRemainUsable() throws Exception {
        var legacy = Files.createDirectory(plugins.resolve("LibrePremium"));
        Files.writeString(legacy.resolve("config.conf"), "database = existing");
        assertEquals(legacy, resolve());
        assertTrue(Files.exists(legacy.resolve("config.conf")));
        assertFalse(Files.exists(legacy.resolve("config.yml")));
    }

    private Path resolve() {
        return PluginDataDirectory.resolve(plugins.resolve("librelogin-fork"),
                "librelogin", "LibreLogin", "librepremium", "LibrePremium");
    }
}
