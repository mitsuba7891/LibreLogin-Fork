/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.packetevents;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsProtocolSupportTest {
    @Test
    void supportsTheRequestedProtocolRange() {
        assertTrue(PacketEventsProtocolSupport.supports(393)); // 1.13
        assertTrue(PacketEventsProtocolSupport.supports(768)); // 1.21.2/1.21.3
        assertTrue(PacketEventsProtocolSupport.supports(769)); // 1.21.4
        assertTrue(PacketEventsProtocolSupport.supports(776)); // 26.2
        assertTrue(PacketEventsProtocolSupport.supports(777)); // 26.3
    }

    @Test
    void rejectsProtocolsOutsideTheSupportedPacketEventsRange() {
        assertFalse(PacketEventsProtocolSupport.supports(392));
        assertFalse(PacketEventsProtocolSupport.supports(778));
    }

    @Test
    void upperBoundMatchesTheActualPacketEvents263Mapping() {
        assertEquals(ClientVersion.V_26_3.getProtocolVersion(), PacketEventsProtocolSupport.MAX_SUPPORTED_PROTOCOL);
        assertTrue(PacketEventsImageProjector.compatible());
    }

    @Test
    void olderPacketEventsCannotAdvertiseUnknownProtocolsAs263() {
        assertFalse(PacketEventsProtocolSupport.hasRequiredMappings(776));
        assertFalse(PacketEventsProtocolSupport.hasRequiredMappings(-1));
        assertTrue(PacketEventsProtocolSupport.hasRequiredMappings(777));
        assertTrue(PacketEventsProtocolSupport.hasRequiredMappings(778));
    }

    @Test
    void missingIntegrationNoticeExplainsManual2faAndTheNewMinimumVersion() {
        String notice = PacketEventsImageProjector.MISSING_INTEGRATION_NOTICE;
        assertTrue(notice.contains("Manual 2FA setup remains available"));
        assertTrue(notice.contains("2.14.0+"));
        assertTrue(notice.contains("1.13-26.3"));
        assertTrue(notice.contains("totp.qr-enabled=false"));
        assertFalse(notice.contains("2FA image support is disabled"));
    }
}
