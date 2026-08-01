/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.protocolize.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapDataPacketTest {

    @Test
    void supportsProtocolizeTwoPointFourPointTwoRange() {
        assertEquals(768, MapDataPacket.MAX_SUPPORTED_PROTOCOL);
        assertTrue(hasMapping(767, 0x2c));
        assertTrue(hasMapping(768, 0x2d));
    }

    @Test
    void doesNotAdvertiseProtocolsBeyondProtocolizeSupport() {
        assertTrue(MapDataPacket.MAPPINGS.stream()
                .noneMatch(mapping -> mapping.inRange(769)));
    }

    private static boolean hasMapping(int protocol, int packetId) {
        return MapDataPacket.MAPPINGS.stream()
                .anyMatch(mapping -> mapping.inRange(protocol) && mapping.id() == packetId);
    }
}
