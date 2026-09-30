/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.database.connector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void detectsExplicitlyEncryptedUrls() {
        assertFalse(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:mariadb://db:3306/librelogin?autoReconnect=true"
        ));
        assertFalse(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:mariadb://db:3306/librelogin?sslMode=disable"
        ));
        assertFalse(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:postgresql://db:5432/librelogin?ssl=false"
        ));

        assertTrue(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:mariadb://db:3306/librelogin?sslMode=verify-full"
        ));
        assertTrue(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:mysql://db:3306/librelogin?sslMode=VERIFY_IDENTITY"
        ));
        assertTrue(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:postgresql://db:5432/librelogin?sslmode=verify-full"
        ));
        assertTrue(AuthenticMySQLDatabaseConnector.requestsEncryption(
                "jdbc:mariadb://db:3306/librelogin?useSsl=true"
        ));
    }

    @Test
    void recognisesLoopbackHosts() {
        assertTrue(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("jdbc:mariadb://localhost:3306/librelogin"));
        assertTrue(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("jdbc:mariadb://127.0.0.1:3306/librelogin?sslMode=disable"));
        assertTrue(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("jdbc:postgresql://[::1]:5432/librelogin"));

        assertFalse(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("jdbc:mariadb://db.internal:3306/librelogin"));
        assertFalse(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("jdbc:mariadb://localhost.example.com:3306/librelogin"));
        assertFalse(AuthenticMySQLDatabaseConnector.isLoopbackJdbcUrl("not-a-jdbc-url"));
    }

    @Test
    void defaultMariaDbUrlRequiresVerifiedTls() {
        var url = AuthenticMySQLDatabaseConnector.Configuration.JDBC_URL.defaultValue();

        assertTrue(AuthenticMySQLDatabaseConnector.requestsEncryption(url));
    }
}
