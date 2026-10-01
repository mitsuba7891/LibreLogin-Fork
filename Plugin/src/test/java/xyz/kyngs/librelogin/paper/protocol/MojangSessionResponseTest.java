/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojangSessionResponseTest {

    private static final UUID PREMIUM_ID = UUID.fromString("12345678-1234-5678-1234-567812345678");
    private static final String PROFILE = "{\"id\":\"12345678123456781234567812345678\",\"name\":\"TestPlayer\",\"properties\":[]}";

    @Test
    void acceptsTheAuthenticatedMojangProfile() {
        assertTrue(matches(PROFILE, "TestPlayer", PREMIUM_ID));
        assertTrue(matches(PROFILE, "testplayer", PREMIUM_ID));
    }

    @Test
    void rejectsAnotherAccountsUuidOrName() {
        assertFalse(matches(PROFILE, "AnotherPlayer", PREMIUM_ID));
        assertFalse(matches(PROFILE, "TestPlayer", UUID.randomUUID()));
    }

    @Test
    void usesThePremiumUuidInsteadOfAnOfflineUuid() {
        var offlineId = UUID.nameUUIDFromBytes("OfflinePlayer:TestPlayer".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(matches(PROFILE, "TestPlayer", offlineId));
    }

    @Test
    void missingExpectedIdentityFailsClosed() {
        assertFalse(matches(PROFILE, "TestPlayer", null));
        assertFalse(matches(PROFILE, null, PREMIUM_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "not json", "null", "[]", "{}", "{\"id\":",
            "{\"id\":\"12345678123456781234567812345678\"}",
            "{\"id\":null,\"name\":\"TestPlayer\"}",
            "{\"id\":12345678123456781234567812345678,\"name\":\"TestPlayer\"}",
            "{\"id\":\"12345678-1234-5678-1234-567812345678\",\"name\":\"TestPlayer\"}",
            "{\"id\":\"invalid\",\"name\":\"TestPlayer\"}",
            "{\"id\":\"12345678123456781234567812345678\",\"name\":[]}",
            "{\"id\":\"12345678123456781234567812345678\",\"name\":null}"
    })
    void rejectsMalformedOrIncompleteSuccessfulResponses(String response) {
        assertFalse(matches(response, "TestPlayer", PREMIUM_ID));
    }

    private static boolean matches(String response, String username, UUID uuid) {
        return MojangSessionResponse.matchesProfile(new StringReader(response), username, uuid);
    }
}
