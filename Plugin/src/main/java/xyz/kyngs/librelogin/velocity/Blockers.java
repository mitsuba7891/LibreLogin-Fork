/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.config.HoconPluginConfiguration;

import static xyz.kyngs.librelogin.common.util.CommandLineUtil.root;

public class Blockers {

    private final AuthorizationProvider<Player> authorizationProvider;
    private final HoconPluginConfiguration configuration;

    public Blockers(AuthorizationProvider<Player> authorizationProvider, HoconPluginConfiguration configuration) {
        this.authorizationProvider = authorizationProvider;
        this.configuration = configuration;
    }

    @Subscribe(priority = 100)
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        if (authorizationProvider.isAuthorized(player) && !authorizationProvider.isAwaiting2FA(player))
            return;

        var command = root(event.getCommand());

        for (String allowed : configuration.get(ConfigurationKeys.ALLOWED_COMMANDS_WHILE_UNAUTHORIZED)) {
            if (command.equals(root(allowed))) return;
        }

        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe(priority = 100)
    public void onServerConnect(ServerPreConnectEvent event) {
        if (!authorizationProvider.isAuthorized(event.getPlayer()) || authorizationProvider.isAwaiting2FA(event.getPlayer())) {
            // Check the effective destination: another plugin may have redirected
            // a request that originally targeted a limbo server.
            var target = event.getResult().getServer();
            if (target.isEmpty() || !configuration.get(ConfigurationKeys.LIMBO).contains(target.get().getServerInfo().getName())) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            }
        }
    }

    @Subscribe(priority = 100)
    public void onServerKick(KickedFromServerEvent event) {
        if (!authorizationProvider.isAuthorized(event.getPlayer()) || authorizationProvider.isAwaiting2FA(event.getPlayer())) {
            // Keep the event inside Velocity's kick pipeline. Calling
            // Player#disconnect directly here hides an empty backend reason
            // behind the misleading "Limbo not running" message.
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    event.getServerKickReason().orElse(Component.text("Backend connection closed without a reason"))
            ));
        }
    }

}
