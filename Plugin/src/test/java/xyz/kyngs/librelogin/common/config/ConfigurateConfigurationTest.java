/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.kyngs.librelogin.api.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurateConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesHoconToYamlAndKeepsBackup() throws Exception {
        var legacy = temporaryDirectory.resolve("config.conf");
        Files.writeString(legacy,
                "database {\n" +
                "  properties {\n" +
                "    postgresql: {host: localhost, database: librelogin, port: 5432}\n" +
                "    sqlite: {path: user-data.db}\n" +
                "  }\n" +
                "  type = \"sqlite\"\n" +
                "}\n" +
                "limbo: [limbo0, limbo1]\n" +
                "revision = 0\n");

        var migrated = new ConfigurateConfiguration(
                temporaryDirectory.toFile(),
                "config.yml",
                List.of(),
                "test configuration",
                new NoOpLogger()
        );

        var yaml = temporaryDirectory.resolve("config.yml");
        var backup = temporaryDirectory.resolve("config.conf.pre-yaml.bak");

        assertFalse(migrated.isNewlyCreated());
        assertTrue(Files.exists(yaml));
        assertTrue(Files.exists(backup));
        assertEquals("sqlite", migrated.getHelper().getString("database.type"));
        assertEquals(Files.readString(legacy), Files.readString(backup));
        var yamlText = Files.readString(yaml);
        assertTrue(yamlText.contains("database:"));
        assertTrue(yamlText.contains("  properties:"), "Nested keys should be written on their own indented lines");
        assertTrue(yamlText.contains("    postgresql:"), "Nested maps loaded from flow style should expand to block style");
        assertFalse(yamlText.contains("{"), "YAML should be written in block style, not flow style");
        assertFalse(yamlText.contains("["), "Lists should be written in block style, not flow style");
        assertTrue(yamlText.contains("- limbo0"), "List items should be written on their own lines");
        var reloaded = YamlConfigurationLoader.builder().file(yaml.toFile()).build().load();
        assertEquals("sqlite", reloaded.node("database", "type").getString());
        assertEquals("limbo1", reloaded.node("limbo", 1).getString());
    }

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
}
