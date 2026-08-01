/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.protocolize;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.item.component.MapIdComponent;
import dev.simplix.protocolize.api.providers.ModuleProvider;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.packets.HeldItemChange;
import dev.simplix.protocolize.data.packets.SetSlot;
import dev.simplix.protocolize.data.packets.WindowItems;
import xyz.kyngs.librelogin.api.image.ImageProjector;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.image.AuthenticImageProjector;
import xyz.kyngs.librelogin.velocity.integration.protocolize.packet.MapDataPacket;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ProtocolizeImageProjector<P, S> extends AuthenticImageProjector<P, S> implements ImageProjector<P> {

    public ProtocolizeImageProjector(AuthenticLibreLogin<P, S> plugin) {
        super(plugin);
    }

    public boolean compatible() {
        return !Protocolize.version().equals("2.2.2");
    }

    @Override
    public void enable() {
        Protocolize.getService(ModuleProvider.class).registerModule(new ProtocolizeImageModule());
    }

    /**
     * <b>This implementation only really renders pure black and everything else as transparent. Shouldn't be used for anything else than a QR code.</b>
     *
     * @param image  The image to render.
     * @param player The player to render the image to.
     */
    @Override
    public void project(BufferedImage image, P player) {
        var id = platformHandle.getUUIDForPlayer(player);

        var protocolize = Protocolize.playerProvider().player(id);
        var protocol = protocolize.protocolVersion();
        var item = new ItemStack(
                ItemType.FILLED_MAP,
                1,
                (short) 0
        );

        if (protocol >= ProtocolVersions.MINECRAFT_1_20_5) {
            item.addComponent(MapIdComponent.create(0));
        } else if (protocol >= ProtocolVersions.MINECRAFT_1_17) {
            item.nbtData()
                    .putInt("map", 0);
        }

        var inventory = new ArrayList<dev.simplix.protocolize.api.item.BaseItemStack>(46);
        for (int slot = 0; slot < 46; slot++) {
            inventory.add(ItemStack.NO_DATA);
        }
        inventory.set(36, item);

        // A lightweight limbo backend may not send the normal player-inventory
        // snapshot. Send a complete snapshot first so clients initialize window 0 and accept
        // the map in the hotbar.
        protocolize.sendPacket(new WindowItems(0, inventory, 0));
        protocolize.sendPacket(
                new SetSlot()
                        .windowId(0)
                        .stateId(0)
                        .slot((short) 36)
                        .itemStack(item)
        );

        protocolize.sendPacket(
                new HeldItemChange()
                        .newSlot((short) 0)
        );

        if (image.getWidth() != 128 || image.getHeight() != 128) {
            var resized = new BufferedImage(128, 128, image.getType());

            var graphics = resized.createGraphics();
            graphics.drawImage(image, 0, 0, 128, 128, 0, 0, image.getWidth(), image.getHeight(), null);
            graphics.dispose();

            image = resized;
        }

        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        byte[] data = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            data[i] = (byte) (pixels[i] == -16777216 ? 116 : 56);
        }

        protocolize.sendPacket(new MapDataPacket(0, (byte) 0, new MapData(128, 128, 0, 0, data)));
    }

    @Override
    public boolean canProject(P player) {
        var id = platformHandle.getUUIDForPlayer(player);

        var protocolize = Protocolize.playerProvider().player(id);

        return protocolize.protocolVersion() >= ProtocolVersions.MINECRAFT_1_13
                && protocolize.protocolVersion() <= MapDataPacket.MAX_SUPPORTED_PROTOCOL;
    }

}
