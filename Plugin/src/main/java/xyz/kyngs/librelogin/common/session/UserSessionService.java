/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Keeps the user resolved during pre-login available throughout the active
 * connection. This avoids repeating the same database lookup in profile,
 * initial-server, join and authentication-command handlers.
 */
public final class UserSessionService {

    private final AuthenticLibreLogin<?, ?> plugin;
    private final Cache<UUID, User> activeUsers;
    private final Cache<String, UUID> activeUsernames;
    private final Cache<String, User> pendingUsers;
    private final Cache<UUID, User> pendingUsersById;

    public UserSessionService(AuthenticLibreLogin<?, ?> plugin) {
        this.plugin = plugin;
        this.activeUsers = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build();
        this.activeUsernames = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build();
        this.pendingUsers = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
        this.pendingUsersById = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    public void stage(User user) {
        pendingUsers.put(normalize(user.getLastNickname()), user);
        pendingUsersById.put(user.getUuid(), user);
    }

    @Nullable
    public User findPending(String username) {
        return pendingUsers.getIfPresent(normalize(username));
    }

    @Nullable
    public User findPending(UUID uuid) {
        return pendingUsersById.getIfPresent(uuid);
    }

    public void connect(User user) {
        pendingUsers.invalidate(normalize(user.getLastNickname()));
        pendingUsersById.invalidate(user.getUuid());
        activeUsers.put(user.getUuid(), user);
        activeUsernames.put(normalize(user.getLastNickname()), user.getUuid());
    }

    public void disconnect(UUID uuid) {
        var user = activeUsers.getIfPresent(uuid);
        if (user != null) {
            activeUsernames.invalidate(normalize(user.getLastNickname()));
        }
        activeUsers.invalidate(uuid);
    }

    public boolean isActive(String username) {
        return activeUsernames.getIfPresent(normalize(username)) != null;
    }

    @Nullable
    public User findActive(UUID uuid) {
        return activeUsers.getIfPresent(uuid);
    }

    @Nullable
    public User findActiveOrLoad(UUID uuid) {
        var cached = findActive(uuid);
        if (cached != null) {
            return cached;
        }

        var loaded = plugin.getDatabaseProvider().getByUUID(uuid);
        if (loaded != null) {
            connect(loaded);
        }
        return loaded;
    }

    public boolean canAuthenticateAutomatically(User user, String ip) {
        if (user.autoLoginEnabled()) {
            return true;
        }

        var timeoutSeconds = plugin.getConfiguration().get(ConfigurationKeys.SESSION_TIMEOUT);
        if (timeoutSeconds <= 0 || user.getLastAuthentication() == null || user.getIp() == null) {
            return false;
        }

        var expiresAt = user.getLastAuthentication().toInstant().plusSeconds(timeoutSeconds);
        return user.getIp().equals(ip) && expiresAt.isAfter(Instant.now());
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
