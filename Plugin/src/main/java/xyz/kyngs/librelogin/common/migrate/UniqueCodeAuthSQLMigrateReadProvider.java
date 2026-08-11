/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.migrate;

import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.database.connector.SQLDatabaseConnector;
import xyz.kyngs.librelogin.api.premium.PremiumException;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.database.AuthenticUser;
import xyz.kyngs.librelogin.common.util.GeneralUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class UniqueCodeAuthSQLMigrateReadProvider extends SQLMigrateReadProvider {

    private final AuthenticLibreLogin<?, ?> plugin;

    public UniqueCodeAuthSQLMigrateReadProvider(String tableName, Logger logger, SQLDatabaseConnector connector, AuthenticLibreLogin<?, ?> plugin) {
        super(tableName, logger, connector);
        this.plugin = plugin;
    }

    @Override
    public Collection<User> getAllUsers() {
        return connector.runQuery(connection -> {
            try (var ps = connection.prepareStatement("SELECT * FROM `%s`".formatted(tableName));
                 var rs = ps.executeQuery()) {

            var users = new HashSet<User>();

            while (rs.next()) {
                try {
                    var name = rs.getString("name");
                    // The source plugin stores this value as plain text, so hash it immediately.
                    var password = rs.getString("password");
                    var premium = rs.getBoolean("premium");

                    if (password.equals("n"))
                        password = null; // Source schema uses "n" as its null sentinel.

                    var hashed = password == null
                            ? null
                            : plugin.getDefaultCryptoProvider().createHash(password);

                    var uuid = GeneralUtil.getCrackedUUIDFromName(name);
                    UUID premiumUUID = null;

                    if (premium) {
                        logger.info("Attempting to get premium UUID for " + name);
                        try {
                            var premiumUser = plugin.getPremiumProvider().getUserForName(name);
                            if (premiumUser == null) {
                                logger.warn("User " + name + " is no longer premium, skipping");
                            } else {
                                premiumUUID = premiumUser.uuid();
                                logger.info("Got premium UUID for " + name + ": " + uuid);
                            }
                        } catch (PremiumException e) {
                            logger.error("Error while getting premium UUID for " + name + ": " + e.getMessage());
                        }
                    }

                    users.add(new AuthenticUser(
                            premiumUUID == null ? uuid : premiumUUID,
                            premiumUUID,
                            hashed,
                            name,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
                } catch (Exception e) {
                    logger.error("Error while reading a user from the UniqueCodeAuth database", e);
                }
            }

                return users;
            }
        });
    }
}
