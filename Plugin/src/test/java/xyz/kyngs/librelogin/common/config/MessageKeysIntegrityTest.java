/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config;

import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.common.config.key.ConfigurationKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKeysIntegrityTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%[A-Za-z0-9_]+%");

    @Test
    void messageKeysAreUniqueAndDefaultsHaveValidPlaceholders() throws Exception {
        var keys = new HashSet<String>();
        var fields = 0;

        for (Field field : MessageKeys.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != ConfigurationKey.class) {
                continue;
            }

            fields++;
            var key = (ConfigurationKey<?>) field.get(null);
            assertNotNull(key, field.getName());
            assertFalse(key.key().isBlank(), field.getName());
            assertTrue(keys.add(key.key()), "Duplicate message key: " + key.key());

            if (key.defaultValue() instanceof String value) {
                var remaining = PLACEHOLDER.matcher(value).replaceAll("");
                assertFalse(remaining.contains("%"), "Malformed placeholder in " + key.key() + ": " + value);
            }
        }

        assertTrue(fields > 100, "MessageKeys reflection found unexpectedly few keys");
        assertTrue(MessageKeys.PREFIX.defaultValue().isEmpty(), "global message prefix is opt-in and must default to disabled");
        assertTrue(MessageKeys.PROMPT_LOGIN.defaultValue().contains("%2fa%"), "login prompt must expose the dynamic 2FA placeholder");
        assertTrue(MessageKeys.SYNTAX_LOGIN.defaultValue().contains("[2fa_code]"), "login syntax must keep the optional 2FA argument");
    }
}
