/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration.packetevents;

final class PacketEventsProtocolSupport {
    static final int MIN_SUPPORTED_PROTOCOL = 393; // Minecraft 1.13
    static final int MAX_SUPPORTED_PROTOCOL = 776; // Minecraft 26.2

    private PacketEventsProtocolSupport() {
    }

    static boolean supports(int protocol) {
        return protocol >= MIN_SUPPORTED_PROTOCOL && protocol <= MAX_SUPPORTED_PROTOCOL;
    }
}
