/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.command.commands.tfa;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.audience.Audience;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.command.Command;
import xyz.kyngs.librelogin.common.command.InvalidCommandArgument;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;

import java.util.concurrent.CompletionStage;

@CommandAlias("2fa|2fauth|2fauthcode")
public class TwoFactorAuthCommand<P> extends Command<P> {
    public TwoFactorAuthCommand(AuthenticLibreLogin<P, ?> plugin) {
        super(plugin);
    }

    @Default
    public CompletionStage<Void> onTwoFactorAuth(Audience sender, P player) {
        return runAsync(() -> {
            checkAuthorized(player);
            var user = getUser(player);
            var auth = plugin.getAuthorizationProvider();

            if (user.autoLoginEnabled()) {
                throw new InvalidCommandArgument(getMessage("error-not-cracked"));
            }

            if (auth.isAwaiting2FA(player)) {
                throw new InvalidCommandArgument(getMessage("totp-show-info"));
            }

            var imageProjector = plugin.getImageProjector();
            var totpProvider = plugin.getTOTPProvider();

            if (imageProjector == null || totpProvider == null) {
                throw new InvalidCommandArgument(getMessage("error-unknown"));
            }

            if (!imageProjector.canProject(player)) {
                throw new InvalidCommandArgument(getMessage("totp-wrong-version",
                        "%low%", "1.13",
                        "%high%", "26.2"
                ));
            }

            sender.sendMessage(getMessage("totp-generating"));

            var data = totpProvider.generate(user);

            auth.beginTwoFactorAuthAsync(user, player, data).whenComplete((failure, transferError) -> {
                if (failure != null || transferError != null) return;

                plugin.cancelOnExit(plugin.delay(() -> {
                    try {
                        if (!auth.isAwaiting2FA(player)) return;

                        var currentServer = plugin.getPlatformHandle().getPlayersServerName(player);
                        var onLimbo = currentServer != null
                                && plugin.getConfiguration().get(ConfigurationKeys.LIMBO).contains(currentServer);
                        if (!onLimbo) {
                            plugin.getLogger().debug("Skipping 2FA QR projection for " + player
                                    + ": player is no longer on a limbo server (current=" + currentServer + ")");
                            return;
                        }

                        imageProjector.project(data.qr(), player);
                        plugin.getLogger().debug("2FA QR projected for " + player);
                    } catch (Throwable throwable) {
                        // QR delivery must never tear down the player's login
                        // connection. The manual secret/URI remains usable.
                        plugin.getLogger().debug("2FA QR projection failed for " + player, throwable);
                    }

                    sender.sendMessage(getMessage("totp-show-info"));
                    sender.sendMessage(getMessage("totp-manual-info",
                            "%secret%", data.secret(),
                            "%uri%", data.provisioningUri() == null ? "unavailable" : data.provisioningUri()
                    ));
                }, plugin.getConfiguration().get(ConfigurationKeys.TOTP_DELAY)), player);
            });
        });
    }
}
