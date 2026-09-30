/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.database.connector;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.kyngs.librelogin.api.database.connector.MySQLDatabaseConnector;
import xyz.kyngs.librelogin.api.util.ThrowableFunction;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurateHelper;
import xyz.kyngs.librelogin.common.config.key.ConfigurationKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

public class AuthenticMySQLDatabaseConnector extends AuthenticDatabaseConnector<SQLException, Connection> implements MySQLDatabaseConnector {

    private final HikariConfig hikariConfig;
    private HikariDataSource dataSource;

    public AuthenticMySQLDatabaseConnector(AuthenticLibreLogin<?, ?> plugin, String prefix) {
        super(plugin, prefix);

        this.hikariConfig = new HikariConfig();

        hikariConfig.setPoolName("LibreLogin MySQL Pool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setUsername(get(Configuration.USER));
        hikariConfig.setPassword(get(Configuration.PASSWORD));

        var jdbcUrl = get(Configuration.JDBC_URL)
                .replace("%host%", get(Configuration.HOST))
                .replace("%port%", String.valueOf(get(Configuration.PORT)))
                .replace("%database%", get(Configuration.NAME));

        // Keep both JDBC implementations available. The URL scheme is the
        // explicit and backwards-compatible selector: jdbc:mariadb:// uses
        // MariaDB Connector/J, while jdbc:mysql:// uses MySQL Connector/J.
        hikariConfig.setDriverClassName(resolveDriverClassName(jdbcUrl));
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setMaxLifetime(get(Configuration.MAX_LIFE_TIME));

        if (!isLoopbackJdbcUrl(jdbcUrl) && !requestsEncryption(jdbcUrl)) {
            plugin.getLogger().warn("The database connection is not encrypted: '" + jdbcUrl + "' does not enable TLS.");
            plugin.getLogger().warn("Password hashes and the database credentials travel in clear text. Add sslMode=verify-full to the JDBC URL (or sslMode=VERIFY_IDENTITY with jdbc:mysql://).");
        }
    }

    /**
     * Reports whether the URL explicitly asks for an encrypted connection.
     * Relying on the driver default is not enough: MariaDB Connector/J 3.x
     * defaults to {@code sslMode=disable}, which sends password hashes and the
     * database credentials over the network unencrypted.
     */
    static boolean requestsEncryption(String jdbcUrl) {
        var queryStart = jdbcUrl.indexOf('?');
        if (queryStart < 0) {
            return false;
        }

        var query = jdbcUrl.substring(queryStart + 1).toLowerCase(java.util.Locale.ROOT);

        for (String parameter : query.split("&")) {
            var separator = parameter.indexOf('=');
            if (separator <= 0) continue;

            var key = parameter.substring(0, separator);
            var value = parameter.substring(separator + 1);

            switch (key) {
                case "sslmode", "ssl-mode" -> {
                    // MariaDB: disable|trust|verify-ca|verify-full
                    // MySQL:   DISABLE|PREFERRED|REQUIRED|VERIFY_CA|VERIFY_IDENTITY
                    if (!value.equals("disable")) return true;
                }
                case "usessl", "use-ssl", "requiresecuretransport" -> {
                    if (value.equals("true")) return true;
                }
                default -> {
                }
            }
        }

        return false;
    }

    /** Loopback connections never leave the host, so transport encryption is not required there. */
    static boolean isLoopbackJdbcUrl(String jdbcUrl) {
        var schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) return false;

        var authority = jdbcUrl.substring(schemeEnd + 3);
        var pathStart = authority.indexOf('/');
        if (pathStart >= 0) authority = authority.substring(0, pathStart);

        String host;
        if (authority.startsWith("[")) {
            var end = authority.indexOf(']');
            host = end < 0 ? authority.substring(1) : authority.substring(1, end);
        } else {
            var portStart = authority.indexOf(':');
            host = portStart < 0 ? authority : authority.substring(0, portStart);
        }

        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    static String resolveDriverClassName(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mariadb:")) {
            return "xyz.kyngs.librelogin.lib.mariadb.jdbc.Driver";
        }
        if (jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:mysql+srv:")) {
            return "xyz.kyngs.librelogin.lib.mysql.cj.jdbc.Driver";
        }
        throw new IllegalArgumentException(
                "Unsupported MySQL JDBC URL scheme. Use jdbc:mariadb:// or jdbc:mysql://"
        );
    }

    @Override
    public void connect() throws SQLException {
        dataSource = new HikariDataSource(hikariConfig);
        try (var ignored = dataSource.getConnection()) {
            connected = true;
        } catch (SQLException exception) {
            dataSource.close();
            throw exception;
        }
    }

    @Override
    public void disconnect() throws SQLException {
        connected = false;
        if (dataSource != null) dataSource.close();
    }

    @Override
    public Connection obtainInterface() throws SQLException, IllegalStateException {
        if (!connected()) throw new IllegalStateException("Not connected to the database!");
        return dataSource.getConnection();
    }

    @Override
    public <V> V runQuery(ThrowableFunction<Connection, V, SQLException> function) throws IllegalStateException {
        try {
            try (var connection = obtainInterface()) {
                return function.apply(connection);
            }
        } catch (SQLTransientConnectionException e) {
            plugin.getLogger().error("Lost connection to the database; shutting down to prevent inconsistent authentication data", e);
            System.exit(1);
            //Won't return anyway
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Configuration {

        // Keep this declaration order: database name, host, port, user, password.
        public static final ConfigurationKey<String> NAME = new ConfigurationKey<>(
                "database",
                "librelogin",
                "The name of the database.",
                ConfigurateHelper::getString
        );

        public static final ConfigurationKey<String> HOST = new ConfigurationKey<>(
                "host",
                "localhost",
                "The host of the database.",
                ConfigurateHelper::getString
        );

        public static final ConfigurationKey<Integer> PORT = new ConfigurationKey<>(
                "port",
                3306,
                "The port of the database.",
                ConfigurateHelper::getInt
        );

        public static final ConfigurationKey<String> USER = new ConfigurationKey<>(
                "user",
                "root",
                "The user of the database.",
                ConfigurateHelper::getString
        );

        public static final ConfigurationKey<String> PASSWORD = new ConfigurationKey<>(
                "password",
                "",
                "The password of the database.",
                ConfigurateHelper::getString
        );

        public static final ConfigurationKey<Integer> MAX_LIFE_TIME = new ConfigurationKey<>(
                "max-life-time",
                600000,
                "The maximum lifetime of a database connection in milliseconds. Don't touch this if you don't know what you're doing.",
                ConfigurateHelper::getInt
        );

        public static final ConfigurationKey<String> JDBC_URL = new ConfigurationKey<>(
                "jdbc-url",
                "jdbc:mariadb://%host%:%port%/%database%?autoReconnect=true&zeroDateTimeBehavior=convertToNull&sslMode=verify-full",
                """
                        The JDBC URL of the database. Use jdbc:mariadb:// for MariaDB or jdbc:mysql:// for official MySQL. Keep user/password in their separate fields.
                        The default enables TLS (sslMode=verify-full) and validates the server certificate against the JVM trust store.
                        The MariaDB driver defaults to sslMode=disable, which would send password hashes and the database credentials in clear text.
                        Using a self-signed certificate? Point serverSslCert to your CA file, or relax to sslMode=verify-ca.
                        sslMode=trust encrypts the connection without authenticating the server, so only use it as a last resort.
                        With jdbc:mysql:// the equivalent value is sslMode=VERIFY_IDENTITY. Loopback hosts are exempt and warn instead of failing.
                        """,
                ConfigurateHelper::getString
        );
    }
}
