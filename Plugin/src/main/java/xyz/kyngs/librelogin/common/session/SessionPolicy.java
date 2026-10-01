/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.session;

import xyz.kyngs.librelogin.api.database.User;

import java.time.Duration;
import java.time.Instant;

final class SessionPolicy {

    private SessionPolicy() {
    }

    static boolean canAuthenticateAutomatically(User user, String ip, long timeoutSeconds, Instant now) {
        if (user == null) return false;
        if (user.autoLoginEnabled()) return true;

        // Stale metadata must not turn an unregistered offline account into an
        // authenticated session, even when IP-based sessions are enabled.
        var lastAuthentication = user.getLastAuthentication();
        if (!user.isRegistered() || timeoutSeconds <= 0 || lastAuthentication == null || user.getIp() == null) {
            return false;
        }

        var authenticatedAt = lastAuthentication.toInstant();
        return user.getIp().equals(ip)
                && !authenticatedAt.isAfter(now)
                && Duration.between(authenticatedAt, now).compareTo(Duration.ofSeconds(timeoutSeconds)) < 0;
    }
}
