/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.event.events.AuthenticatedEvent;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.config.HoconPluginConfiguration;
import xyz.kyngs.librelogin.common.config.key.ConfigurationKey;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockersTest {

    @Test
    void unauthenticatedPlayerCannotEnterTheLobby() {
        var event = event("lobby");
        blockers(false, false).onServerConnect(event);
        assertFalse(event.getResult().isAllowed());
    }

    @Test
    void unauthenticatedPlayerCanEnterTheConfiguredLimbo() {
        var event = event("auth");
        blockers(false, false).onServerConnect(event);
        assertTrue(event.getResult().isAllowed());
    }

    @Test
    void twoFactorSetupCannotLeaveTheLimbo() {
        var event = event("lobby");
        blockers(true, true).onServerConnect(event);
        assertFalse(event.getResult().isAllowed());
    }

    @Test
    void authorizedPlayerCanEnterTheLobby() {
        var event = event("lobby");
        var originalResult = event.getResult();
        blockers(true, false).onServerConnect(event);
        assertSame(originalResult, event.getResult());
    }

    @Test
    void redirectingAnAuthRequestToTheLobbyDoesNotBypassAuthentication() {
        var event = event("auth");
        event.setResult(ServerPreConnectEvent.ServerResult.allowed(server("lobby")));
        blockers(false, false).onServerConnect(event);
        assertFalse(event.getResult().isAllowed());
    }

    @Test
    void redirectingToTheLimboIsAllowed() {
        var event = event("lobby");
        event.setResult(ServerPreConnectEvent.ServerResult.allowed(server("auth")));
        blockers(false, false).onServerConnect(event);
        assertTrue(event.getResult().isAllowed());
    }

    @Test
    void anExistingDenialIsNotReopened() {
        var event = event("auth");
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        blockers(false, false).onServerConnect(event);
        assertFalse(event.getResult().isAllowed());
    }

    private static Blockers blockers(boolean authorized, boolean awaitingTwoFactor) {
        var authorization = new AuthorizationProvider<Player>() {
            @Override public boolean isAuthorized(Player player) { return authorized; }
            @Override public boolean isAwaiting2FA(Player player) { return awaitingTwoFactor; }
            @Override public void authorize(User user, Player player, AuthenticatedEvent.AuthenticationReason reason) {
                throw new UnsupportedOperationException();
            }
            @Override public boolean confirmTwoFactorAuth(Player player, Integer code, User user) {
                throw new UnsupportedOperationException();
            }
        };
        var configuration = new HoconPluginConfiguration(null, List.of()) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(ConfigurationKey<T> key) {
                if (key == ConfigurationKeys.LIMBO) return (T) List.of("auth");
                throw new AssertionError("Unexpected configuration key");
            }
        };
        return new Blockers(authorization, configuration);
    }

    private static ServerPreConnectEvent event(String target) {
        var player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> { throw new UnsupportedOperationException(method.getName()); });
        return new ServerPreConnectEvent(player, server(target), null);
    }

    private static RegisteredServer server(String name) {
        var info = new ServerInfo(name, new InetSocketAddress("127.0.0.1", 25565));
        return (RegisteredServer) Proxy.newProxyInstance(RegisteredServer.class.getClassLoader(), new Class<?>[]{RegisteredServer.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getServerInfo")) return info;
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
