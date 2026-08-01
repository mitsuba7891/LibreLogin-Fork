/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.packetevents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsProtocolSupportTest {
    @Test
    void supportsTheRequestedProtocolRange() {
        assertTrue(PacketEventsProtocolSupport.supports(393)); // 1.13
        assertTrue(PacketEventsProtocolSupport.supports(768)); // 1.21.2/1.21.3
        assertTrue(PacketEventsProtocolSupport.supports(769)); // 1.21.4
        assertTrue(PacketEventsProtocolSupport.supports(776)); // 26.2
    }

    @Test
    void rejectsProtocolsOutsideTheSupportedPacketEventsRange() {
        assertFalse(PacketEventsProtocolSupport.supports(392));
        assertFalse(PacketEventsProtocolSupport.supports(777));
    }
}
