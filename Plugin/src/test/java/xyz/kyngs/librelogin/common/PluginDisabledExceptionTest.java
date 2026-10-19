/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginDisabledExceptionTest {

    @Test
    void firstTimeSetupIsInformationalNotAnAuthenticationFailure() {
        var logger = new RecordingLogger();
        var setup = new PluginDisabledException("a new configuration was generated and has to be filled in", true);
        setup.logOutcome(logger);
        assertTrue(setup.restartRequested());
        assertEquals(1, logger.info.size());
        assertTrue(logger.info.getFirst().contains("First-time setup"));
        assertTrue(logger.info.getFirst().contains("restart"));
        assertTrue(logger.warnings.isEmpty());
        assertTrue(logger.errors.isEmpty(), "Generating default files is not a fatal failure");
    }

    @Test
    void realFailuresStillExplainThatConnectionsAreRefused() {
        var logger = new RecordingLogger();
        var failure = new PluginDisabledException("database is unavailable");
        failure.logOutcome(logger);
        assertFalse(failure.restartRequested());
        assertTrue(logger.info.isEmpty());
        assertEquals(2, logger.errors.size());
        assertTrue(logger.errors.getFirst().contains("refuses every login"));
        assertTrue(logger.errors.getFirst().contains("database is unavailable"));
        assertTrue(logger.errors.get(1).contains("left running"));
    }

    private static final class RecordingLogger implements Logger {
        private final List<String> info = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        @Override public void info(String message) { info.add(message); }
        @Override public void info(String message, Throwable throwable) { info.add(message); }
        @Override public void warn(String message) { warnings.add(message); }
        @Override public void warn(String message, Throwable throwable) { warnings.add(message); }
        @Override public void error(String message) { errors.add(message); }
        @Override public void error(String message, Throwable throwable) { errors.add(message); }
        @Override public void debug(String message) { }
        @Override public void debug(String message, Throwable throwable) { }
    }
}
