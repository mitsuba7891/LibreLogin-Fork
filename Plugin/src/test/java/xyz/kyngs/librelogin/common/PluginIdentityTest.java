/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PluginIdentityTest {

    @Test
    void paperDescriptorsUseTheForkNameAndKeepTheLegacyProviderAlias() throws Exception {
        for (String descriptor : new String[]{"plugin.yml", "paper-plugin.yml"}) {
            try (var input = getClass().getClassLoader().getResourceAsStream(descriptor)) {
                assertNotNull(input, descriptor);
                String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(text.contains("name: \"librelogin-fork\""), descriptor);
                assertTrue(text.contains("provides: [LibreLogin]"), descriptor);
            }
        }
    }

    @Test
    void generatedVelocityDescriptorUsesTheForkId() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("velocity-plugin.json")) {
            assertNotNull(input);
            var json = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("librelogin-fork", json.get("id").getAsString());
            assertEquals("librelogin-fork", json.get("name").getAsString());
        }
    }
}
