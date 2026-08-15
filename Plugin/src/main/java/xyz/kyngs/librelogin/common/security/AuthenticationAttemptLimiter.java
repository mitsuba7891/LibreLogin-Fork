/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import xyz.kyngs.librelogin.api.event.events.AuthenticatedEvent;
import xyz.kyngs.librelogin.api.event.events.WrongPasswordEvent;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Account-based brute-force protection that survives reconnects for the
 * configured cooldown. Player object identity is deliberately not used.
 */
public final class AuthenticationAttemptLimiter<P, S> {

    private final AuthenticLibreLogin<P, S> plugin;
    private final Cache<UUID, AttemptEntry> attempts;

    public AuthenticationAttemptLimiter(AuthenticLibreLogin<P, S> plugin) {
        this.plugin = plugin;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(
                        Math.max(1L, plugin.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_EXPIRE_LOGIN_ATTEMPTS)),
                        TimeUnit.MILLISECONDS
                )
                .build();

        plugin.getEventProvider().subscribe(plugin.getEventTypes().wrongPassword, this::onWrongPassword);
        plugin.getEventProvider().subscribe(plugin.getEventTypes().authenticated, this::onAuthenticated);
    }

    public boolean isBlocked(UUID uuid) {
        var maximum = maximumAttempts();
        var current = attempts.getIfPresent(uuid);
        return maximum > 0 && current != null && current.count() >= maximum;
    }

    /**
     * @return the milliseconds left until the failed attempts expire and the
     * player can try logging in again, or {@code 0} when the player is not
     * blocked.
     */
    public long getBlockedRemainingMillis(UUID uuid) {
        var entry = attempts.getIfPresent(uuid);
        if (entry == null) return 0;
        var window = Math.max(1L, plugin.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_EXPIRE_LOGIN_ATTEMPTS));
        return Math.max(0L, entry.lastAttemptMillis() + window - System.currentTimeMillis());
    }

    private void onWrongPassword(WrongPasswordEvent<P, S> event) {
        if (!isLoginCredential(event.getSource()) || maximumAttempts() <= 0 || event.getUUID() == null) {
            return;
        }

        var uuid = event.getUUID();
        var entry = attempts.asMap().compute(uuid, (key, current) ->
                new AttemptEntry(current == null ? 1 : current.count() + 1, System.currentTimeMillis()));

        if (entry.count() >= maximumAttempts() && event.getPlayer() != null) {
            var seconds = (long) Math.ceil(getBlockedRemainingMillis(uuid) / 1000.0);
            plugin.getPlatformHandle().kick(event.getPlayer(),
                    plugin.getMessages().getMessage("kick-error-too-many-attempts",
                            "%seconds%", String.valueOf(seconds)));
        }
    }

    private void onAuthenticated(AuthenticatedEvent<P, S> event) {
        if (event.getUUID() != null) {
            attempts.invalidate(event.getUUID());
        }
    }

    private int maximumAttempts() {
        return plugin.getConfiguration().get(ConfigurationKeys.MAX_LOGIN_ATTEMPTS);
    }

    private boolean isLoginCredential(WrongPasswordEvent.AuthenticationSource source) {
        return source == WrongPasswordEvent.AuthenticationSource.LOGIN
                || source == WrongPasswordEvent.AuthenticationSource.TOTP;
    }

    private record AttemptEntry(int count, long lastAttemptMillis) {
    }
}
