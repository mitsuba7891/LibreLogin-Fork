/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandLineUtilTest {

    @Test
    void keepsLogAliasByDefault() {
        assertEquals("login|l|log", CommandLineUtil.loginAliases(true));
    }

    @Test
    void releasesLogWhenAnotherPluginOwnsIt() {
        assertEquals("login|l", CommandLineUtil.loginAliases(false));
    }
}
