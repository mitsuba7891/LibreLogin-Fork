/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.packetevents;

final class PacketEventsProtocolSupport {
    static final int MIN_SUPPORTED_PROTOCOL = 393; // Minecraft 1.13
    static final int MAX_SUPPORTED_PROTOCOL = 777; // Minecraft 26.3, PacketEvents 2.14.0+

    private PacketEventsProtocolSupport() {
    }

    static boolean supports(int protocol) {
        return protocol >= MIN_SUPPORTED_PROTOCOL && protocol <= MAX_SUPPORTED_PROTOCOL;
    }

    static boolean hasRequiredMappings(int latestRuntimeProtocol) {
        return latestRuntimeProtocol >= MAX_SUPPORTED_PROTOCOL;
    }
}
