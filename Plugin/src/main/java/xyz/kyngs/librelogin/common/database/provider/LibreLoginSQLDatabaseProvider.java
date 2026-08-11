/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.database.provider;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.database.connector.SQLDatabaseConnector;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.database.AuthenticDatabaseProvider;
import xyz.kyngs.librelogin.common.database.AuthenticUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class LibreLoginSQLDatabaseProvider extends AuthenticDatabaseProvider<SQLDatabaseConnector> {

    public LibreLoginSQLDatabaseProvider(SQLDatabaseConnector connector, AuthenticLibreLogin<?, ?> plugin) {
        super(connector, plugin);
    }

    @Override
    public Collection<User> getByIP(String ip) {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT * FROM librepremium_data WHERE ip=?")) {
                statement.setString(1, ip);
                try (var result = statement.executeQuery()) {
                    var users = new ArrayList<User>();
                    User user;
                    while ((user = getUserFromResult(result)) != null) {
                        users.add(user);
                    }
                    return users;
                }
            }
        });
    }

    @Override
    public long countByIP(String ip) {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM librepremium_data WHERE ip=?")) {
                statement.setString(1, ip);
                try (var result = statement.executeQuery()) {
                    return result.next() ? result.getLong(1) : 0L;
                }
            }
        });
    }

    @Override
    public User getByName(String name) {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT * FROM librepremium_data WHERE LOWER(last_nickname)=LOWER(?)")) {
                statement.setString(1, name);
                try (var result = statement.executeQuery()) {
                    return getUserFromResult(result);
                }
            }
        });
    }

    @Override
    public Collection<User> getAllUsers() {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT * FROM librepremium_data");
                 var result = statement.executeQuery()) {
                var users = new ArrayList<User>();
                User user;
                while ((user = getUserFromResult(result)) != null) {
                    users.add(user);
                }
                return users;
            }
        });
    }

    @Override
    public User getByUUID(UUID uuid) {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT * FROM librepremium_data WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (var result = statement.executeQuery()) {
                    return getUserFromResult(result);
                }
            }
        });
    }

    @Override
    public User getByPremiumUUID(UUID uuid) {
        plugin.reportMainThread();
        return connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("SELECT * FROM librepremium_data WHERE premium_uuid=?")) {
                statement.setString(1, uuid.toString());
                try (var result = statement.executeQuery()) {
                    return getUserFromResult(result);
                }
            }
        });
    }

    @Nullable
    private User getUserFromResult(ResultSet rs) throws SQLException {
        if (rs.next()) {
            var id = UUID.fromString(rs.getString("uuid"));
            var premiumUUID = rs.getString("premium_uuid");
            var hashedPassword = rs.getString("hashed_password");
            var salt = rs.getString("salt");
            var algo = rs.getString("algo");
            var lastNickname = rs.getString("last_nickname");
            var joinDate = rs.getTimestamp("joined");
            var lastSeen = rs.getTimestamp("last_seen");

            return new AuthenticUser(
                    id,
                    premiumUUID == null ? null : UUID.fromString(premiumUUID),
                    hashedPassword == null ? null : new HashedPassword(
                            hashedPassword,
                            salt,
                            algo
                    ),
                    lastNickname,
                    joinDate,
                    lastSeen,
                    rs.getString("secret"),
                    rs.getString("ip"),
                    rs.getTimestamp("last_authentication"),
                    rs.getString("last_server"),
                    rs.getString("email")
            );
        } else return null;
    }

    @Override
    public void insertUser(User user) {
        plugin.reportMainThread();
        connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("INSERT INTO librepremium_data(uuid, premium_uuid, hashed_password, salt, algo, last_nickname, joined, last_seen, secret, ip, last_authentication, last_server, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insertToStatement(statement, user);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void insertUsers(Collection<User> users) {
        plugin.reportMainThread();
        connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("INSERT " + getIgnoreSyntax() + " INTO librepremium_data(uuid, premium_uuid, hashed_password, salt, algo, last_nickname, joined, last_seen, secret, ip, last_authentication, last_server, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" + getIgnoreSuffix())) {
                for (User user : users) {
                    insertToStatement(statement, user);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private void insertToStatement(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getUuid().toString());
        ps.setString(2, user.getPremiumUUID() == null ? null : user.getPremiumUUID().toString());
        ps.setString(3, user.getHashedPassword() == null ? null : user.getHashedPassword().hash());
        ps.setString(4, user.getHashedPassword() == null ? null : user.getHashedPassword().salt());
        ps.setString(5, user.getHashedPassword() == null ? null : user.getHashedPassword().algo());
        ps.setString(6, user.getLastNickname());
        ps.setTimestamp(7, user.getJoinDate());
        ps.setTimestamp(8, user.getLastSeen());
        ps.setString(9, user.getSecret());
        ps.setString(10, user.getIp());
        ps.setTimestamp(11, user.getLastAuthentication());
        ps.setString(12, user.getLastServer());
        ps.setString(13, user.getEmail());
    }

    @Override
    public void updateUser(User user) {
        plugin.reportMainThread();
        connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("UPDATE librepremium_data SET premium_uuid=?, hashed_password=?, salt=?, algo=?, last_nickname=?, joined=?, last_seen=?, secret=?, ip=?, last_authentication=?, last_server=?, email=? WHERE uuid=?")) {
                statement.setString(1, user.getPremiumUUID() == null ? null : user.getPremiumUUID().toString());
                statement.setString(2, user.getHashedPassword() == null ? null : user.getHashedPassword().hash());
                statement.setString(3, user.getHashedPassword() == null ? null : user.getHashedPassword().salt());
                statement.setString(4, user.getHashedPassword() == null ? null : user.getHashedPassword().algo());
                statement.setString(5, user.getLastNickname());
                statement.setTimestamp(6, user.getJoinDate());
                statement.setTimestamp(7, user.getLastSeen());
                statement.setString(8, user.getSecret());
                statement.setString(9, user.getIp());
                statement.setTimestamp(10, user.getLastAuthentication());
                statement.setString(11, user.getLastServer());
                statement.setString(12, user.getEmail());
                statement.setString(13, user.getUuid().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void deleteUser(User user) {
        plugin.reportMainThread();
        connector.runQuery(connection -> {
            try (var statement = connection.prepareStatement("DELETE FROM librepremium_data WHERE uuid=?")) {
                statement.setString(1, user.getUuid().toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void validateSchema() {
        connector.runQuery(connection -> {
            execute(connection,
                    "CREATE TABLE IF NOT EXISTS librepremium_data(" +
                            "uuid VARCHAR(255) NOT NULL PRIMARY KEY," +
                            "premium_uuid VARCHAR(255) UNIQUE," +
                            "hashed_password VARCHAR(255)," +
                            "salt VARCHAR(255)," +
                            "algo VARCHAR(255)," +
                    "last_nickname VARCHAR(255) NOT NULL UNIQUE," +
                            "joined TIMESTAMP NULL DEFAULT NULL," +
                            "last_seen TIMESTAMP NULL DEFAULT NULL," +
                            "last_server VARCHAR(255)" +
                            ")"
            );

            var columns = getColumnNames(connection);

            try {
                execute(connection, addUnique("premium_uuid"));
            } catch (SQLException ignored) {
            }

            if (!columns.contains("secret"))
                execute(connection, "ALTER TABLE librepremium_data ADD COLUMN secret VARCHAR(255) NULL DEFAULT NULL");
            if (!columns.contains("ip"))
                execute(connection, "ALTER TABLE librepremium_data ADD COLUMN ip VARCHAR(255) NULL DEFAULT NULL");
            if (!columns.contains("last_authentication"))
                execute(connection, "ALTER TABLE librepremium_data ADD COLUMN last_authentication TIMESTAMP NULL DEFAULT NULL");
            if (!columns.contains("last_server")) {
                execute(connection, "ALTER TABLE librepremium_data ADD COLUMN last_server VARCHAR(255) NULL DEFAULT NULL");
            }
            if (!columns.contains("email")) {
                execute(connection, "ALTER TABLE librepremium_data ADD COLUMN email VARCHAR(255) NULL DEFAULT NULL");
            }

            try {
                execute(connection, addUnique("last_nickname"));
            } catch (SQLException ignored) {
            }
        });
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    protected abstract List<String> getColumnNames(Connection connection) throws SQLException;

    protected String getIgnoreSyntax() {
        return "";
    }

    protected String getIgnoreSuffix() {
        return "";
    }

    protected abstract String addUnique(String column);
}
