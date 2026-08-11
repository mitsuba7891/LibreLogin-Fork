/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.crypto;

import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.Logger;

import static org.junit.jupiter.api.Assertions.*;

class Argon2IDCryptoProviderTest {

    private final Argon2IDCryptoProvider provider = new Argon2IDCryptoProvider(new SilentLogger());

    @Test
    void hashesWithRandomSaltAndRejectsWrongPassword() {
        var first = provider.createHash("correct horse battery staple");
        var second = provider.createHash("correct horse battery staple");

        assertNotNull(first);
        assertNotNull(second);
        assertEquals("Argon-2ID", first.algo());
        assertNotEquals(first.salt(), second.salt());
        assertTrue(provider.matches("correct horse battery staple", first));
        assertFalse(provider.matches("wrong password", first));
    }

    private static final class SilentLogger implements Logger {
        @Override public void info(String message) { }
        @Override public void info(String message, Throwable throwable) { }
        @Override public void warn(String message) { }
        @Override public void warn(String message, Throwable throwable) { }
        @Override public void error(String message) { }
        @Override public void error(String message, Throwable throwable) { }
        @Override public void debug(String message) { }
        @Override public void debug(String message, Throwable throwable) { }
    }
}
