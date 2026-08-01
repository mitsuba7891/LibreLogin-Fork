/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.database.connector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticMySQLDatabaseConnectorTest {

    @Test
    void selectsMariaDbDriverForMariaDbUrls() {
        assertEquals(
                "xyz.kyngs.librelogin.lib.mariadb.jdbc.Driver",
                AuthenticMySQLDatabaseConnector.resolveDriverClassName(
                        "jdbc:mariadb://localhost:3306/librelogin"
                )
        );
    }

    @Test
    void selectsOfficialMySqlDriverForMySqlUrls() {
        assertEquals(
                "xyz.kyngs.librelogin.lib.mysql.cj.jdbc.Driver",
                AuthenticMySQLDatabaseConnector.resolveDriverClassName(
                        "jdbc:mysql://localhost:3306/librelogin"
                )
        );
    }

    @Test
    void selectsOfficialMySqlDriverForSrvUrls() {
        assertEquals(
                "xyz.kyngs.librelogin.lib.mysql.cj.jdbc.Driver",
                AuthenticMySQLDatabaseConnector.resolveDriverClassName(
                        "jdbc:mysql+srv://mysql.example.com/librelogin"
                )
        );
    }

    @Test
    void rejectsUnknownJdbcSchemes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthenticMySQLDatabaseConnector.resolveDriverClassName(
                        "jdbc:postgresql://localhost:5432/librelogin"
                )
        );
    }
}
