/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.security;

import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;

/**
 * Owns password hashing, verification and transparent legacy-hash upgrades.
 * Password values never leave this service through logging or exceptions.
 */
public final class PasswordService {

    private final AuthenticLibreLogin<?, ?> plugin;

    public PasswordService(AuthenticLibreLogin<?, ?> plugin) {
        this.plugin = plugin;
    }

    @Nullable
    public HashedPassword createHash(String password) {
        return plugin.getDefaultCryptoProvider().createHash(password);
    }

    public VerificationResult verify(User user, String candidate) {
        var storedPassword = user.getHashedPassword();
        if (storedPassword == null) {
            return VerificationResult.CORRUPTED;
        }

        var provider = plugin.getCryptoProvider(storedPassword.algo());
        if (provider == null) {
            return VerificationResult.CORRUPTED;
        }

        try {
            if (!provider.matches(candidate, storedPassword)) {
                return VerificationResult.INVALID;
            }
        } catch (RuntimeException corruptedHash) {
            return VerificationResult.CORRUPTED;
        }

        var defaultProvider = plugin.getDefaultCryptoProvider();
        if (defaultProvider.getIdentifier().equals(storedPassword.algo())) {
            return VerificationResult.VALID;
        }

        var upgradedPassword = defaultProvider.createHash(candidate);
        if (upgradedPassword == null) {
            return VerificationResult.VALID;
        }

        user.setHashedPassword(upgradedPassword);
        return VerificationResult.VALID_AND_UPGRADED;
    }

    public enum VerificationResult {
        VALID,
        VALID_AND_UPGRADED,
        INVALID,
        CORRUPTED;

        public boolean isValid() {
            return this == VALID || this == VALID_AND_UPGRADED;
        }
    }
}
