/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.kyngs.librelogin.api.BiHolder;
import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.common.config.migrate.ConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.EightConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.FifthConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.FirstConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.FourthConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.SecondConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.SeventhConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.SixthConfigurationMigrator;
import xyz.kyngs.librelogin.common.config.migrate.config.ThirdConfigurationMigrator;
import xyz.kyngs.librelogin.common.database.connector.AuthenticMySQLDatabaseConnector;
import xyz.kyngs.librelogin.common.database.connector.AuthenticPostgreSQLDatabaseConnector;
import xyz.kyngs.librelogin.common.database.connector.AuthenticSQLiteDatabaseConnector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDumpTest {

    @TempDir
    Path temporaryDirectory;

    private java.util.ArrayList<BiHolder<Class<?>, String>> testDefaults() {
        var defaults = new java.util.ArrayList<BiHolder<Class<?>, String>>();
        defaults.add(new BiHolder<>(ConfigurationKeys.class, ""));
        defaults.add(new BiHolder<>(AuthenticMySQLDatabaseConnector.Configuration.class, "database.properties.mysql."));
        defaults.add(new BiHolder<>(AuthenticMySQLDatabaseConnector.Configuration.class, "migration.old-database.mysql."));
        defaults.add(new BiHolder<>(AuthenticSQLiteDatabaseConnector.Configuration.class, "database.properties.sqlite."));
        defaults.add(new BiHolder<>(AuthenticSQLiteDatabaseConnector.Configuration.class, "migration.old-database.sqlite."));
        defaults.add(new BiHolder<>(AuthenticPostgreSQLDatabaseConnector.Configuration.class, "database.properties.postgresql."));
        defaults.add(new BiHolder<>(AuthenticPostgreSQLDatabaseConnector.Configuration.class, "migration.old-database.postgresql."));
        return defaults;
    }

    @Test
    void dumpMigrated() throws Exception {
        // Simulate a legacy HOCON config so the migration path (legacy .conf -> YAML)
        // is exercised, which is what existing servers hit when upgrading.
        // Note: 'kick-on-wrong-password' is intentionally omitted to prove that
        // migration survives legacy configs missing optional keys (Seventh
        // migrator must be null-safe).
        Files.writeString(temporaryDirectory.resolve("config.conf"), """
            database {
                type = librelogin-sqlite
                host = localhost
                port = 3306
                user = root
                password = ""
                database = librelogin
            }
            limbo = [limbo0, limbo1]
            allowed-commands-while-unauthorized = [login, register, 2fa]
            totp {
                enabled = true
                label = MyNetwork
                delay = 1000
            }
            revision = 4
            """.stripIndent());

        var migrators = migrators();
        var config = new ConfigurateConfiguration(
                temporaryDirectory.toFile(),
                "config.yml",
                testDefaults(),
                """
                          !!THIS FILE IS WRITTEN IN YAML FORMAT!!
                          LibreLogin migrated this file from HOCON when upgrading older installations.
                          Keep indentation intact and quote values that contain YAML syntax.
                        """,
                new NoOpLogger(),
                migrators
        );

        var yaml = Files.readString(temporaryDirectory.resolve("config.yml"));
        Files.createDirectories(Path.of("build"));
        Files.writeString(Path.of("build/dump-migrated-config.yml"), yaml);

        // Regression guardrails: the migrated file must be block style with
        // the guide comments, keep legacy values and bump the revision.
        assertFalse(yaml.matches("(?m)^\\S+\\s*:\\s*\\{.*"), "migrated output must not contain inline flow maps");
        assertTrue(yaml.contains("type: librelogin-sqlite"), "legacy database.type was not preserved");
        assertTrue(yaml.contains("label: MyNetwork"), "legacy totp.label was not preserved");
        assertTrue(yaml.contains("- limbo0"), "legacy limbo list was not preserved");
        assertTrue(yaml.contains("revision: " + migrators.length), "revision was not bumped to the current revision");
        assertTrue(yaml.contains("# The host of the database."), "per-key guide comments missing in migrated output");
        assertFalse(config.isNewlyCreated(), "migrated-from-legacy must not be flagged as newly created");

        System.out.println("=== MIGRATED CONFIG START ===");
        System.out.println(yaml);
        System.out.println("=== MIGRATED CONFIG END ===");
        System.out.println("newlyCreated=" + config.isNewlyCreated());
    }

    @Test
    void messagesUseQuotedScalarsAndGlobalPrefix() throws Exception {
        new ConfigurateConfiguration(
                temporaryDirectory.toFile(),
                "messages.yml",
                Set.of(new BiHolder<>(MessageKeys.class, "")),
                "LibreLogin messages",
                new NoOpLogger()
        );

        var yaml = Files.readString(temporaryDirectory.resolve("messages.yml"));
        assertTrue(yaml.contains("prefix:"), "global prefix is missing");
        assertTrue(yaml.contains("# Optional global prefix prepended literally to chat"), "prefix documentation is missing");
        assertFalse(yaml.contains("prefix: &"), "prefix must not use YAML anchors");

        // Administrators may use ordinary YAML double quotes around messages;
        // the same loader must read them back as the original scalar value.
        var quoted = yaml.replaceFirst("(?m)^prefix:.*$", "prefix: \\\"&6[Network] &r\\\"");
        Files.writeString(temporaryDirectory.resolve("messages.yml"), quoted);
        var loaded = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                .file(temporaryDirectory.resolve("messages.yml").toFile())
                .build().load();
        assertTrue("&6[Network] &r".equals(loaded.node("prefix").getString()), "quoted message values must load unchanged");

        var disabled = quoted.replaceFirst("(?m)^prefix:.*$", "prefix: \\\"\\\"");
        Files.writeString(temporaryDirectory.resolve("messages.yml"), disabled);
        var emptyPrefix = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                .file(temporaryDirectory.resolve("messages.yml").toFile())
                .build().load();
        assertTrue("".equals(emptyPrefix.node("prefix").getString()), "an empty prefix must remain disabled");
    }

    @Test
    void databasePropertiesUseReadableOrder() throws Exception {
        new ConfigurateConfiguration(
                temporaryDirectory.toFile(),
                "config.yml",
                testDefaults(),
                "LibreLogin configuration",
                new NoOpLogger(),
                migrators()
        );

        var yaml = Files.readString(temporaryDirectory.resolve("config.yml"));
        var mysqlStart = yaml.indexOf("mysql:");
        assertTrue(mysqlStart >= 0, "mysql properties are missing");
        var mysql = yaml.substring(mysqlStart);
        assertTrue(mysql.indexOf("database:") < mysql.indexOf("host:"), "database name should come before host");
        assertTrue(mysql.indexOf("host:") < mysql.indexOf("port:"), "host should come before port");
        assertTrue(mysql.indexOf("port:") < mysql.indexOf("user:"), "port should come before user");
        assertTrue(mysql.indexOf("user:") < mysql.indexOf("password:"), "user should come before password");
    }

    @Test
    void dump() throws Exception {
        var defaults = new ArrayList<BiHolder<Class<?>, String>>();
        defaults.add(new BiHolder<>(ConfigurationKeys.class, ""));
        defaults.add(new BiHolder<>(AuthenticMySQLDatabaseConnector.Configuration.class, "database.properties.mysql."));
        defaults.add(new BiHolder<>(AuthenticMySQLDatabaseConnector.Configuration.class, "migration.old-database.mysql."));
        defaults.add(new BiHolder<>(AuthenticSQLiteDatabaseConnector.Configuration.class, "database.properties.sqlite."));
        defaults.add(new BiHolder<>(AuthenticSQLiteDatabaseConnector.Configuration.class, "migration.old-database.sqlite."));
        defaults.add(new BiHolder<>(AuthenticPostgreSQLDatabaseConnector.Configuration.class, "database.properties.postgresql."));
        defaults.add(new BiHolder<>(AuthenticPostgreSQLDatabaseConnector.Configuration.class, "migration.old-database.postgresql."));

        var migrators = migrators();
        var config = new ConfigurateConfiguration(
                temporaryDirectory.toFile(),
                "config.yml",
                defaults,
                """
                          !!THIS FILE IS WRITTEN IN YAML FORMAT!!
                          LibreLogin migrated this file from HOCON when upgrading older installations.
                          Keep indentation intact and quote values that contain YAML syntax.
                          ----------------------------------------------------------------------------------------
                          LibreLogin Configuration
                          ----------------------------------------------------------------------------------------
                          This is the configuration file for LibreLogin.
                          You can find more information about LibreLogin on the github page:
                          https://github.com/kyngs/LibreLogin
                        """,
                new NoOpLogger(),
                migrators
        );

        var yaml = Files.readString(temporaryDirectory.resolve("config.yml"));
        Files.createDirectories(Path.of("build"));
        Files.writeString(Path.of("build/dump-config.yml"), yaml);

        // Regression guardrails: fresh configs must be block style, carry the
        // per-key guide comments (including the mail section) and the revision.
        assertFalse(yaml.matches("(?m)^\\S+\\s*:\\s*\\{.*"), "fresh config must not contain inline flow maps");
        assertTrue(yaml.contains("# The host of the database."), "per-key comment for database.properties.mysql.host missing");
        assertTrue(yaml.contains("# This section is used for configuring the email password recovery feature."), "mail section comment missing");
        assertTrue(yaml.contains("# The config revision number. !!DO NOT TOUCH THIS!!"), "revision comment missing");
        assertTrue(yaml.contains("revision: " + migrators.length), "revision key missing");
        assertTrue(config.isNewlyCreated(), "fresh config must be flagged as newly created");

        System.out.println("=== GENERATED CONFIG START ===");
        System.out.println(yaml);
        System.out.println("=== GENERATED CONFIG END ===");
        System.out.println("newlyCreated=" + config.isNewlyCreated());
    }

    private static ConfigurationMigrator[] migrators() {
        return new ConfigurationMigrator[]{
                new FirstConfigurationMigrator(),
                new SecondConfigurationMigrator(),
                new ThirdConfigurationMigrator(),
                new FourthConfigurationMigrator(),
                new FifthConfigurationMigrator(),
                new SixthConfigurationMigrator(),
                new SeventhConfigurationMigrator(),
                new EightConfigurationMigrator()
        };
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
