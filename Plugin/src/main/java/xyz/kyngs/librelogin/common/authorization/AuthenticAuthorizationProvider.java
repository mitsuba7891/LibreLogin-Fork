/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.authorization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.event.events.AuthenticatedEvent;
import xyz.kyngs.librelogin.api.totp.TOTPData;
import xyz.kyngs.librelogin.common.AuthenticHandler;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.event.events.AuthenticAuthenticatedEvent;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthenticAuthorizationProvider<P, S> extends AuthenticHandler<P, S> implements AuthorizationProvider<P> {

    private final Map<P, User> unAuthorized;
    private final Map<P, String> awaiting2FA;
    private final Cache<UUID, EmailVerifyData> emailConfirmCache;
    private final Cache<UUID, String> passwordResetCache;

    public AuthenticAuthorizationProvider(AuthenticLibreLogin<P, S> plugin) {
        super(plugin);
        unAuthorized = new ConcurrentHashMap<>();
        awaiting2FA = new ConcurrentHashMap<>();

        var millis = plugin.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_REFRESH_NOTIFICATION);

        if (millis > 0) {
            plugin.repeat(this::notifyUnauthorized, 0, millis);
        }

        plugin.repeat(this::broadcastActionbars, 0, 1000);

        emailConfirmCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        passwordResetCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    public Cache<UUID, EmailVerifyData> getEmailConfirmCache() {
        return emailConfirmCache;
    }

    public Cache<UUID, String> getPasswordResetCache() {
        return passwordResetCache;
    }

    public void onExit(P player) {
        stopTracking(player);
        awaiting2FA.remove(player);
        emailConfirmCache.invalidate(platformHandle.getUUIDForPlayer(player));
        passwordResetCache.invalidate(platformHandle.getUUIDForPlayer(player));
    }

    @Override
    public boolean isAuthorized(P player) {
        return !unAuthorized.containsKey(player);
    }

    @Override
    public boolean isAwaiting2FA(P player) {
        return awaiting2FA.containsKey(player);
    }

    @Override
    public void authorize(User user, P player, AuthenticatedEvent.AuthenticationReason reason) {
        if (isAuthorized(player)) {
            throw new IllegalStateException("Player is already authorized");
        }
        stopTracking(player);

        user.setLastAuthentication(Timestamp.valueOf(LocalDateTime.now()));
        user.setIp(platformHandle.getIP(player));
        plugin.getDatabaseProvider().updateUser(user);

        var audience = platformHandle.getAudienceForPlayer(player);

        audience.clearTitle();
        audience.sendActionBar(Component.empty());
        plugin.getEventProvider().fire(plugin.getEventTypes().authenticated, new AuthenticAuthenticatedEvent<>(user, player, plugin, reason));
        plugin.authorize(player, user, audience);
    }

    @Override
    public boolean confirmTwoFactorAuth(P player, Integer code, User user) {
        var secret = awaiting2FA.get(player);
        if (plugin.getTOTPProvider().verify(code, secret)) {
            user.setSecret(secret);
            plugin.getDatabaseProvider().updateUser(user);
            return true;
        }
        return false;
    }

    public void startTracking(User user, P player) {
        var audience = platformHandle.getAudienceForPlayer(player);

        unAuthorized.put(player, user);

        plugin.cancelOnExit(plugin.delay(() -> {
            if (!unAuthorized.containsKey(player)) return;
            sendInfoMessage(player, user, audience);
        }, 250), player);

        var limit = plugin.getConfiguration().get(ConfigurationKeys.SECONDS_TO_AUTHORIZE);

        if (limit > 0) {
            plugin.cancelOnExit(plugin.delay(() -> {
                if (!unAuthorized.containsKey(player)) return;
                platformHandle.kick(player, plugin.getMessages().getMessage("kick-time-limit"));
            }, limit * 1000L), player);
        }

        sendInfoMessage(player, user, audience);
    }

    private void broadcastActionbars() {
        var wrong = new HashSet<P>();
        unAuthorized.forEach((player, user) -> {
            var audience = platformHandle.getAudienceForPlayer(player);

            if (audience == null) {
                wrong.add(player);
                return;
            }

            if (!isAwaiting2FA(player)) {
                sendActionBar(user, audience);
            }

        });

        wrong.forEach(unAuthorized::remove);
    }

    private void sendActionBar(User user, Audience audience) {
        if (!plugin.getConfiguration().get(ConfigurationKeys.USE_ACTION_BAR)) return;

        var messageKey = user.isRegistered() ? "action-bar-login" : "action-bar-register";
        var message = user.isRegistered()
                ? plugin.getMessages().getMessage(messageKey,
                "%2fa%", user.getSecret() == null ? "" : " <2fa_code>")
                : plugin.getMessages().getMessage(messageKey);
        audience.sendActionBar(message);
    }

    private void sendInfoMessage(P player, User user, Audience audience) {
        if (audience == null || isAwaiting2FA(player)) return;

        // While registering 2FA, the QR/setup instructions replace the normal
        // login prompt. Repeating "Please login" makes the QR flow confusing
        // and can overwrite the useful setup information on some clients.
        var promptKey = user.isRegistered() ? "prompt-login" : "prompt-register";
        var prompt = user.isRegistered()
                ? plugin.getMessages().getMessage(promptKey,
                "%2fa%", user.getSecret() == null ? "" : " <2fa_code>")
                : plugin.getMessages().getMessage(promptKey);
        audience.sendMessage(prompt);
        if (!plugin.getConfiguration().get(ConfigurationKeys.USE_TITLES)) return;
        var toRefresh = plugin.getConfiguration().get(ConfigurationKeys.MILLISECONDS_TO_REFRESH_NOTIFICATION);
        //noinspection UnstableApiUsage
        audience.showTitle(Title.title(
                plugin.getMessages().getMessage(user.isRegistered() ? "title-login" : "title-register"),
                user.isRegistered()
                        ? plugin.getMessages().getMessage("sub-title-login", "%2fa%", user.getSecret() == null ? "" : " <2fa_code>")
                        : plugin.getMessages().getMessage("sub-title-register"),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(toRefresh > 0 ?
                                (long) (toRefresh * 1.1) :
                                10000
                        ),
                        Duration.ofMillis(0)
                )
        ));
    }

    public void stopTracking(P player) {
        unAuthorized.remove(player);
    }

    public void notifyUnauthorized() {
        var wrong = new HashSet<P>();
        unAuthorized.forEach((player, user) -> {
            var audience = platformHandle.getAudienceForPlayer(player);

            if (audience == null) {
                wrong.add(player);
                return;
            }

            sendInfoMessage(player, user, audience);

        });

        wrong.forEach(unAuthorized::remove);
    }

    public record EmailVerifyData(String email, String token, UUID uuid) {
    }

    /**
     * Keeps the original API for integrations compiled against older releases.
     * Use {@code beginTwoFactorAuthAsync} when the QR must be sent only after
     * the limbo transfer completes.
     */
    public void beginTwoFactorAuth(User user, P player, TOTPData data) {
        beginTwoFactorAuthAsync(user, player, data);
    }

    public CompletableFuture<Throwable> beginTwoFactorAuthAsync(User user, P player, TOTPData data) {
        awaiting2FA.put(player, data.secret());

        var audience = platformHandle.getAudienceForPlayer(player);
        if (audience != null) {
            audience.clearTitle();
            audience.sendActionBar(Component.empty());
        }

        var limbo = plugin.getServerHandler().chooseLimboServer(user, player);

        if (limbo == null) {
            var failure = new IllegalStateException("No limbo server is available for 2FA");
            plugin.getLogger().debug("Could not move " + platformHandle.getUsernameForPlayer(player) + " to a 2FA limbo", failure);
            awaiting2FA.remove(player);
            platformHandle.kick(player, plugin.getMessages().getMessage("kick-no-limbo"));
            return CompletableFuture.completedFuture(failure);
        }

        return platformHandle.movePlayer(player, limbo).whenComplete((t, e) -> {
            if (t != null || e != null) {
                awaiting2FA.remove(player);
                plugin.getLogger().debug(
                        "2FA limbo transfer failed for " + platformHandle.getUsernameForPlayer(player),
                        e != null ? e : t
                );
            } else {
                plugin.getLogger().debug("2FA limbo transfer completed for " + platformHandle.getUsernameForPlayer(player));
            }
        });
    }
}
