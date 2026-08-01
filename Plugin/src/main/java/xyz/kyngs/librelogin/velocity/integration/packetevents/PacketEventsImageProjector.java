/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import xyz.kyngs.librelogin.api.image.ImageProjector;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.image.AuthenticImageProjector;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PacketEventsImageProjector<P, S> extends AuthenticImageProjector<P, S> implements ImageProjector<P> {
    public PacketEventsImageProjector(AuthenticLibreLogin<P, S> plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        // PacketEvents is initialized by its own Velocity plugin.
    }

    @Override
    public void project(BufferedImage image, P player) {
        User user = user(player);
        if (user == null || !PacketEventsProtocolSupport.supports(user.getClientVersion().getProtocolVersion())) {
            return;
        }

        if (image.getWidth() != 128 || image.getHeight() != 128) {
            var resized = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            var graphics = resized.createGraphics();
            graphics.drawImage(image, 0, 0, 128, 128, 0, 0, image.getWidth(), image.getHeight(), null);
            graphics.dispose();
            image = resized;
        }

        var itemBuilder = ItemStack.builder()
                .type(ItemTypes.FILLED_MAP)
                .amount(1)
                .user(user);

        if (user.getClientVersion().getProtocolVersion() >= 766) {
            itemBuilder.component(ComponentTypes.MAP_ID, 0);
        } else {
            itemBuilder.nbt("map", new NBTInt(0));
        }

        var mapItem = itemBuilder.build();
        var inventory = new ArrayList<ItemStack>(46);
        for (int slot = 0; slot < 46; slot++) {
            inventory.add(ItemStack.EMPTY);
        }
        inventory.set(36, mapItem);

        // A lightweight limbo backend may not emit the normal player-inventory
        // snapshot. Initialize the client inventory before the single-slot update so the
        // map item is accepted by modern clients instead of being ignored.
        user.sendPacket(new WrapperPlayServerWindowItems(0, 0, inventory, ItemStack.EMPTY));
        user.sendPacket(new WrapperPlayServerSetSlot(0, 0, 36, mapItem));
        // Only send downstream packets. Injecting a fake clientbound/upstream
        // HeldItemChange into the backend can violate the protocol state during
        // a limbo transfer and disconnect the player with a generic error.
        user.sendPacket(new WrapperPlayServerHeldItemChange(0));

        int[] pixels = image.getRGB(0, 0, 128, 128, null, 0, 128);
        byte[] data = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            data[i] = (byte) (pixels[i] == -16777216 ? 116 : 56);
        }

        user.sendPacket(new WrapperPlayServerMapData(
                0,
                (byte) 0,
                false,
                false,
                null,
                128,
                128,
                0,
                0,
                data
        ));
    }

    @Override
    public boolean canProject(P player) {
        User user = user(player);
        return user != null && PacketEventsProtocolSupport.supports(user.getClientVersion().getProtocolVersion());
    }

    private User user(P player) {
        var api = PacketEvents.getAPI();
        if (api == null) {
            return null;
        }

        return api.getPlayerManager().getUser(player);
    }
}
