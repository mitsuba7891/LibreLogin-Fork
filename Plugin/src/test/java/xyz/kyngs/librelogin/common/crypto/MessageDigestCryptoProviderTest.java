/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.crypto;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDigestCryptoProviderTest {

    @Test
    void hashesSafelyAcrossConcurrentLoginAttempts() throws Exception {
        var provider = new MessageDigestCryptoProvider("SHA-256");
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = IntStream.range(0, 100)
                    .<java.util.concurrent.Callable<Boolean>>mapToObj(index -> () -> {
                        var password = "password-" + index;
                        var hash = provider.createHash(password);
                        return provider.matches(password, hash)
                                && !provider.matches(password + "-wrong", hash);
                    })
                    .toList();

            for (var result : executor.invokeAll(tasks)) {
                assertTrue(result.get());
            }
        }
    }

    @Test
    void rejectsAnIncorrectPassword() {
        var provider = new MessageDigestCryptoProvider("SHA-512");
        var hash = provider.createHash("correct horse battery staple");

        assertTrue(provider.matches("correct horse battery staple", hash));
        assertFalse(provider.matches("incorrect", hash));
    }
}
