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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void acceptsOnlyTheEncryptedNonceFromThisLoginRequest() throws Exception {
        var keys = EncryptionUtil.generateKeyPair();
        var token = new byte[]{1, 2, 3, 4};
        var cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keys.getPublic());
        var encryptedToken = cipher.doFinal(token);

        assertTrue(EncryptionUtil.verifyNonce(token, keys.getPrivate(), encryptedToken));
        assertFalse(EncryptionUtil.verifyNonce(new byte[]{1, 2, 3, 5}, keys.getPrivate(), encryptedToken));
        assertFalse(EncryptionUtil.verifyNonce(new byte[]{1, 2, 3}, keys.getPrivate(), encryptedToken));
    }

    private static void assertFalseSameBytes(byte[] expected, byte[] actual) {
        if (Arrays.equals(expected, actual)) {
            throw new AssertionError("AES/CFB8 encryption must change the plaintext");
        }
    }
}
