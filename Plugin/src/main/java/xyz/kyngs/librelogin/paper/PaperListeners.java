/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.config.MessageKeys;
import xyz.kyngs.librelogin.common.listener.AuthenticListeners;
import xyz.kyngs.librelogin.paper.protocol.ClientPublicKey;
import xyz.kyngs.librelogin.paper.protocol.EncryptionUtil;
import xyz.kyngs.librelogin.paper.protocol.MojangSessionResponse;
import xyz.kyngs.librelogin.paper.protocol.ProtocolUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.*;

import static xyz.kyngs.librelogin.paper.protocol.ProtocolUtil.getServerVersion;

public class PaperListeners extends AuthenticListeners<PaperLibreLogin, Player, World> implements Listener {

    private static volatile Method encryptMethod;

    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();
    private final Random random = new SecureRandom();
    private final Cache<String, EncryptionData> encryptionDataCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();
    private final FloodgateHelper floodgateHelper;
    private final Cache<UUID, String> ipCache;
    private final Cache<UUID, Location> spawnLocationCache;

    public PaperListeners(PaperLibreLogin plugin) {
        super(plugin);

        floodgateHelper = this.plugin.floodgateEnabled() ? new FloodgateHelper() : null;

        ipCache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();

        spawnLocationCache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    public Cache<UUID, Location> getSpawnLocationCache() {
        return spawnLocationCache;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        onPlayerDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.isDisabledByFailure()) {
            event.getPlayer().kick(Component.text("LibreLogin is disabled, contact the server administrator."));
            return;
        }
        var data = plugin.getUserSessionService().findPending(event.getPlayer().getUniqueId());
        if (data == null && !plugin.fromFloodgate(event.getPlayer().getName())) {
            event.getPlayer().kick(Component.text("Internal error, please try again later."));
            return;
        }
        onPostLogin(event.getPlayer(), data);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Fail closed. Initialisation problems no longer terminate the server,
        // so refusing every connection here is what keeps an incomplete
        // authentication setup from letting players through unverified.
        if (plugin.isDisabledByFailure()) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("LibreLogin is disabled, contact the server administrator.")
            );
            return;
        }

        if (plugin.fromFloodgate(event.getName())) {
            ipCache.put(event.getUniqueId(), event.getAddress().getHostAddress());
            return;
        }

        var user = plugin.getUserSessionService().findPending(event.getName());
        if (user == null) {
            user = plugin.getDatabaseProvider().getByName(event.getName());
        }
        if (user == null) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Internal error, please try again later.")
            );
            return;
        }

        var newProfile = Bukkit.createProfileExact(user.getUuid(), event.getName());

        event.setPlayerProfile(newProfile);

        plugin.getUserSessionService().stage(user);
        ipCache.put(user.getUuid(), event.getAddress().getHostAddress());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void chooseWorld(AsyncPlayerSpawnLocationEvent event) {
        var profile = event.getConnection().getProfile();
        var playerId = profile.getId();
        if (playerId == null) {
            plugin.getLogger().error("Paper did not provide a UUID during async player spawn; refusing the connection");
            return;
        }

        var ip = ipCache.getIfPresent(playerId);
        if (ip == null) {
            plugin.getLogger().error("No staged IP address was found for " + profile.getName() + " during async player spawn; refusing to select a destination");
            return;
        }

        var user = plugin.getUserSessionService().findPending(playerId);
        var world = chooseServer(playerId, ip, user, null);
        ipCache.invalidate(playerId);

        spawnLocationCache.invalidate(playerId);
        if (world.value() == null) {
            plugin.getLogger().error("No " + (world.key() ? "lobby" : "limbo") + " world is available for " + profile.getName());
        } else {
            // Preserve the original location so a successful login can return the player there.
            if (!event.isNewPlayer() && !plugin.getConfiguration().get(ConfigurationKeys.LIMBO).contains(event.getSpawnLocation().getWorld().getName())) {
                if (plugin.getConfiguration().get(ConfigurationKeys.LIMBO).contains(world.value().getName())) {
                    spawnLocationCache.put(playerId, event.getSpawnLocation());
                } else {
                    return;
                }
            }

            event.setSpawnLocation(world.value().getSpawnLocation());

        }
    }

    public void asyncPacketReceive(PacketReceiveEvent event) {
        var user = event.getUser();
        var type = event.getPacketType();

        plugin.getLogger().debug("Login packet received: " + type);

        if (type == PacketType.Login.Client.LOGIN_START) {
            var packet = new WrapperLoginClientLoginStart(event);
            var sessionKey = user.getAddress().toString();

            encryptionDataCache.invalidate(sessionKey);

            if (plugin.floodgateEnabled()) {
                var success = floodgateHelper.processFloodgateTasks(event, packet);
                // don't continue execution if the player was kicked by Floodgate
                if (!success) {
                    return;
                }
            }
            var username = packet.getUsername();

            Optional<ClientPublicKey> clientKey;

            if (getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
                clientKey = Optional.empty();
            } else {
                var signature = packet.getSignatureData();

                clientKey = signature.map(data -> {
                    var expires = data.getTimestamp();
                    var key = data.getPublicKey();
                    var signatureData = data.getSignature();

                    return new ClientPublicKey(expires, key, signatureData);
                });
            }

            if (plugin.getUserSessionService().isActive(username)) {
                kickPlayer(plugin.getMessages().getMessage(MessageKeys.KICK_ALREADY_CONNECTED.key()), user);
                return;
            }

            if (plugin.fromFloodgate(username)) {
                //Floodgate player, do not handle, only retransmit the packet. The UUID will be set by Floodgate
                receiveFakeStartPacket(username, clientKey.orElse(null), event.getChannel(), UUID.randomUUID());
                return;
            }
            var preLoginResult = onPreLogin(username, user.getAddress().getAddress());
            if (preLoginResult.user() != null) {
                plugin.getUserSessionService().stage(preLoginResult.user());
            }
            switch (preLoginResult.state()) {
                case DENIED -> {
                    assert preLoginResult.message() != null;
                    kickPlayer(preLoginResult.message(), user);
                }
                case FORCE_ONLINE -> {
                    byte[] token;
                    try {
                        token = EncryptionUtil.generateVerifyToken(random);

                        var newPacket = new WrapperLoginServerEncryptionRequest("", keyPair.getPublic(), token);

                        encryptionDataCache.put(sessionKey, new EncryptionData(username, token, clientKey.orElse(null),
                                preLoginResult.user().getUuid(), preLoginResult.user().getPremiumUUID()));

                        PacketEvents.getAPI().getProtocolManager().sendPacket(event.getChannel(), newPacket);
                    } catch (Exception e) {
                        plugin.getLogger().error("Failed to start encrypted login; kicking player", e);
                        kickPlayer("Internal error", user);
                    }
                }
                default -> {
                    // The original event has been cancelled, so we need to send a fake start packet. It should be safe to set a random UUID as it will be replaced by the real one later
                    receiveFakeStartPacket(username, clientKey.orElse(null), event.getChannel(), UUID.randomUUID());
                }
            }
        } else {
            var packet = new WrapperLoginClientEncryptionResponse(event);
            var sharedSecret = packet.getEncryptedSharedSecret();

            var encryptionKey = user.getAddress().toString();
            var data = encryptionDataCache.getIfPresent(encryptionKey);
            encryptionDataCache.invalidate(encryptionKey);

            if (data == null) {
                kickPlayer("Illegal encryption state", user);
                return;
            }

            var expectedToken = data.token().clone();

            if (!verifyNonce(packet, data.publicKey(), expectedToken)) {
                kickPlayer("Invalid nonce", user);
                return;
            }

            //Verify session
            var privateKey = keyPair.getPrivate();

            SecretKey loginKey;

            try {
                loginKey = EncryptionUtil.decryptSharedKey(privateKey, sharedSecret);
            } catch (GeneralSecurityException securityEx) {
                kickPlayer("Cannot decrypt shared secret", user);
                return;
            }

            try {
                if (!enableEncryption(loginKey, user, event.getChannel())) {
                    return;
                }
            } catch (Exception e) {
                kickPlayer("Cannot decrypt shared secret", user);
                return;
            }

            var serverId = EncryptionUtil.getServerIdHashString("", loginKey, keyPair.getPublic());
            var username = data.username();
            var address = user.getAddress();

            try {
                if (hasJoined(username, serverId, address.getAddress(), data.premiumUuid())) {
                    receiveFakeStartPacket(username, data.publicKey(), event.getChannel(), data.uuid());
                } else {
                    kickPlayer("Invalid session", user);
                }
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException) {
                    plugin.getLogger().warn("Session verification timed out (5 seconds) for " + username);
                }
                kickPlayer("Cannot verify session", user);
            }
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        event.setCancelled(true);

        var copy = event.clone();

        AuthenticLibreLogin.EXECUTOR.execute(() -> {
            try {
                asyncPacketReceive(copy);
            } finally {
                copy.cleanUp();
            }
        });
    }

    /**
     * fake a new login packet in order to let the server handle all the other stuff
     *
     * @author games647 and FastLogin contributors
     */
    private void receiveFakeStartPacket(String username, ClientPublicKey clientKey, Object channel, UUID uuid) {
        WrapperLoginClientLoginStart startPacket;
        if (getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20)) {
            startPacket = new WrapperLoginClientLoginStart(getServerVersion().toClientVersion(), username, clientKey == null ? null : clientKey.toSignatureData(), uuid);
        } else if (getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) {
            startPacket = new WrapperLoginClientLoginStart(getServerVersion().toClientVersion(), username, clientKey == null ? null : clientKey.toSignatureData());
        } else {
            startPacket = new WrapperLoginClientLoginStart(getServerVersion().toClientVersion(), username);
        }
        PacketEvents.getAPI().getProtocolManager().receivePacketSilently(channel, startPacket);
    }

    public boolean hasJoined(String username, String serverHash, InetAddress hostIp) throws IOException {
        var user = plugin.getUserSessionService().findPending(username);
        return user != null && hasJoined(username, serverHash, hostIp, user.getPremiumUUID());
    }

    private boolean hasJoined(String username, String serverHash, InetAddress hostIp, UUID premiumUuid) throws IOException {
        if (premiumUuid == null) return false;

        var encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        var encodedServerHash = URLEncoder.encode(serverHash, StandardCharsets.UTF_8);
        String url;
        if (hostIp instanceof Inet6Address || plugin.getConfiguration().get(ConfigurationKeys.ALLOW_PROXY_CONNECTIONS)) {
            url = String.format("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s", encodedUsername, encodedServerHash);
        } else {
            var encodedIP = URLEncoder.encode(hostIp.getHostAddress(), StandardCharsets.UTF_8);
            url = String.format("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s&ip=%s", encodedUsername, encodedServerHash, encodedIP);
        }

        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            var responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    return MojangSessionResponse.matchesProfile(reader, username, premiumUuid);
                }
            }
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return false;
            }
            throw new IOException("Unexpected session server response: " + responseCode);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * @author games647 and FastLogin contributors, kyngs
     */
    private boolean enableEncryption(SecretKey loginKey, com.github.retrooper.packetevents.protocol.player.User user, Object channel) throws IllegalArgumentException {
        // Initialize method reflections
        if (encryptMethod == null) {
            var networkManagers = SpigotReflectionUtil.getNetworkManagers();
            if (networkManagers.isEmpty()) {
                kickPlayer("Could not initialize network encryption", user);
                return false;
            }
            Class<?> networkManagerClass = networkManagers.get(0).getClass();

            // Try to get the old (pre MC 1.16.4) encryption method
            encryptMethod = Reflection.getMethod(networkManagerClass, "setupEncryption", SecretKey.class);

            if (encryptMethod == null) {
                // Try to get the new encryption method
                encryptMethod = Reflection.getMethod(networkManagerClass, "setEncryptionKey", SecretKey.class);
            }

            if (encryptMethod == null) {
                // 1.16.4+ uses pre-built AES ciphers. Do not use the old
                // net.minecraft.util.MinecraftEncryption helper here: that NMS
                // class was removed/renamed in newer Paper mappings.
                encryptMethod = Reflection.getMethod(networkManagerClass, "setEncryptionKey", Cipher.class, Cipher.class);
            }

            if (encryptMethod == null) {
                // Fallback for mappings that expose the same operation under
                // the older method name.
                encryptMethod = Reflection.getMethod(networkManagerClass, "setupEncryption", Cipher.class, Cipher.class);
            }

            if (encryptMethod == null) {
                throw new IllegalStateException("Could not find a compatible network encryption method on " + networkManagerClass.getName());
            }
        }

        try {
            Object networkManager = ProtocolUtil.findNetworkManager(channel);
            if (networkManager == null) {
                throw new IllegalStateException("Could not find the network manager for the login channel");
            }

            // Older servers accept the SecretKey directly. Modern servers
            // accept AES/CFB8 ciphers, which are created without NMS classes.
            if (encryptMethod.getParameterCount() == 1) {
                encryptMethod.invoke(networkManager, loginKey);
            } else {
                var decryptionCipher = EncryptionUtil.createCipher(Cipher.DECRYPT_MODE, loginKey);
                var encryptionCipher = EncryptionUtil.createCipher(Cipher.ENCRYPT_MODE, loginKey);
                encryptMethod.invoke(networkManager, decryptionCipher, encryptionCipher);
            }
        } catch (Exception ex) {
            kickPlayer("Couldn't enable encryption", user);
            plugin.getLogger().error("Failed to enable login encryption", ex);
            return false;
        }

        return true;
    }

    private void kickPlayer(String reason, com.github.retrooper.packetevents.protocol.player.User player) {
        kickPlayer(Component.text(reason), player);
    }

    private void kickPlayer(Component reason, com.github.retrooper.packetevents.protocol.player.User player) {
        // Cannot use Player#kick(Component) because it doesn't work in the login state
        var kickPacket = new WrapperLoginServerDisconnect(reason);
        try {
            //send kick packet at login state
            PacketEvents.getAPI().getProtocolManager().sendPacket(player.getChannel(), kickPacket);
        } finally {
            //tell the server that we want to close the connection
            player.closeConnection();
        }
    }

    /**
     * @author games647 and FastLogin contributors
     */
    private boolean verifyNonce(WrapperLoginClientEncryptionResponse packet,
                                ClientPublicKey clientPublicKey, byte[] expectedToken) {
        try {
            if (getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19)
                && !getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
                if (clientPublicKey == null) {
                    var encryptedToken = packet.getEncryptedVerifyToken();
                    return encryptedToken.isPresent()
                            && EncryptionUtil.verifyNonce(expectedToken, keyPair.getPrivate(), encryptedToken.get());
                } else {
                    PublicKey publicKey = clientPublicKey.key();
                    var optSignature = packet.getSaltSignature();
                    if (optSignature.isEmpty()) {
                        return false;
                    }
                    var signature = optSignature.get();

                    return EncryptionUtil.verifySignedNonce(expectedToken, publicKey, signature.getSalt(), signature.getSignature());
                }
            } else {
                var encryptedToken = packet.getEncryptedVerifyToken();
                return encryptedToken.isPresent()
                        && EncryptionUtil.verifyNonce(expectedToken, keyPair.getPrivate(), encryptedToken.get());
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchPaddingException
                 | IllegalBlockSizeException | BadPaddingException signatureEx) {
            return false;
        }
    }
}
