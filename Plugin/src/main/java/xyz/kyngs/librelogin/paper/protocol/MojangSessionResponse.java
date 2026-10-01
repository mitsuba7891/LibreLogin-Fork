/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.UUID;

public final class MojangSessionResponse {

    private MojangSessionResponse() {
    }

    public static boolean matchesProfile(Reader response, String username, UUID premiumUuid) {
        if (username == null || premiumUuid == null) return false;

        try {
            var parsed = JsonParser.parseReader(response);
            if (!parsed.isJsonObject()) return false;

            var profile = parsed.getAsJsonObject();
            var id = profile.get("id");
            var name = profile.get("name");
            if (!isString(id) || !isString(name)) return false;

            // Session IDs are Mojang UUIDs, not LibreLogin's configurable local UUIDs.
            return id.getAsString().matches("[a-fA-F0-9]{32}")
                    && premiumUuid.toString().replace("-", "").equalsIgnoreCase(id.getAsString())
                    && username.equalsIgnoreCase(name.getAsString());
        } catch (JsonParseException | IllegalStateException exception) {
            return false;
        }
    }

    private static boolean isString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }
}
