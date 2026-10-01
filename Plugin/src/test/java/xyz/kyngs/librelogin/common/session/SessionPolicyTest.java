/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.session;

import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.common.database.AuthenticUser;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final String IP = "192.0.2.1";

    @Test
    void firstJoinRequiresRegistrationEvenWithSessionsEnabled() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(false, false, null), IP, 3600, NOW));
    }

    @Test
    void staleAuthenticationMetadataCannotAuthorizeAnUnregisteredAccount() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(false, false, NOW.minusSeconds(10)), IP, 3600, NOW));
    }

    @Test
    void validRegisteredSessionUsesTheSameIp() {
        assertTrue(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.minusSeconds(10)), IP, 3600, NOW));
    }

    @Test
    void anotherIpCannotReuseTheSession() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.minusSeconds(10)), "192.0.2.2", 3600, NOW));
    }

    @Test
    void disablingSessionsRequiresAnotherLogin() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.minusSeconds(10)), IP, 0, NOW));
    }

    @Test
    void expiryBoundaryDoesNotAuthorize() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.minusSeconds(3600)), IP, 3600, NOW));
    }

    @Test
    void incompleteMetadataDoesNotAuthorize() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(true, false, null), IP, 3600, NOW));
        var user = user(true, false, NOW.minusSeconds(10));
        user.setIp(null);
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user, IP, 3600, NOW));
        assertFalse(SessionPolicy.canAuthenticateAutomatically(null, IP, 3600, NOW));
    }

    @Test
    void futureTimestampDoesNotAuthorize() {
        assertFalse(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.plusSeconds(10)), IP, 3600, NOW));
    }

    @Test
    void premiumAccountsKeepTheirVerifiedAutologin() {
        assertTrue(SessionPolicy.canAuthenticateAutomatically(user(false, true, null), IP, 0, NOW));
    }

    @Test
    void largeConfiguredTimeoutDoesNotOverflowAnInstant() {
        assertTrue(SessionPolicy.canAuthenticateAutomatically(user(true, false, NOW.minusSeconds(10)), IP, Long.MAX_VALUE, NOW));
    }

    private static AuthenticUser user(boolean registered, boolean premium, Instant authenticatedAt) {
        return new AuthenticUser(UUID.randomUUID(), premium ? UUID.randomUUID() : null,
                registered ? new HashedPassword("test-hash", null, "Argon-2ID") : null,
                "TestPlayer", Timestamp.from(NOW), Timestamp.from(NOW), null, IP,
                authenticatedAt == null ? null : Timestamp.from(authenticatedAt), null, null);
    }
}
