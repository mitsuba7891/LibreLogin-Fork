/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFilterTest {

    private final TestLogFilter filter = new TestLogFilter();

    @Test
    void filtersSensitiveCommandsCaseInsensitively() {
        assertFalse(filter.accepts("Player issued server command: /LOGIN secret"));
        assertFalse(filter.accepts("Player executed command /minecraft:register secret secret"));
        assertFalse(filter.accepts("Player executed command: /librelogin user pass-change Alex secret"));
        assertFalse(filter.accepts("Player executed command: /plugin:librelogin   user pass-change Alex secret"));
    }

    @Test
    void doesNotHideUnrelatedCommandsContainingProtectedWords() {
        assertTrue(filter.accepts("Player issued server command: /help login"));
        assertTrue(filter.accepts("Regular log message mentioning login"));
    }

    private static final class TestLogFilter extends LogFilter {
        boolean accepts(String message) {
            return checkMessage(message);
        }

        @Override
        public void inject() {
        }
    }
}
