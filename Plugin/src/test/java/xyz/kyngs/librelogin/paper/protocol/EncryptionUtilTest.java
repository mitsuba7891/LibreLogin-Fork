/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper.protocol;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EncryptionUtilTest {

    @Test
    void minecraftCfb8CipherRoundTrips() throws Exception {
        var key = new SecretKeySpec(
                "0123456789abcdef".getBytes(StandardCharsets.US_ASCII),
                "AES"
        );
        var plaintext = "LibreLogin Paper login encryption".getBytes(StandardCharsets.UTF_8);

        var encryptor = EncryptionUtil.createCipher(Cipher.ENCRYPT_MODE, key);
        var decryptor = EncryptionUtil.createCipher(Cipher.DECRYPT_MODE, key);
        var encrypted = encryptor.doFinal(plaintext);
        var decrypted = decryptor.doFinal(encrypted);

        assertArrayEquals(plaintext, decrypted);
        // Ensure the test is exercising encryption rather than an accidental
        // identity transformation.
        assertFalseSameBytes(plaintext, encrypted);
    }

    private static void assertFalseSameBytes(byte[] expected, byte[] actual) {
        if (Arrays.equals(expected, actual)) {
            throw new AssertionError("AES/CFB8 encryption must change the plaintext");
        }
    }
}
