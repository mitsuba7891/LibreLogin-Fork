/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

import xyz.kyngs.librelogin.api.Logger;

/**
 * Signals that LibreLogin must stop initialising, either for first-time setup
 * or because of an actual startup failure.
 *
 * <p>Actual failures keep the process alive but refuse authentication. First-time
 * setup instead requests a platform restart and is reported as informational,
 * not as a fatal authentication failure.</p>
 *
 * @author kyngs
 */
public class PluginDisabledException extends RuntimeException {

    private final boolean restartRequested;

    public PluginDisabledException(String message) {
        this(message, false);
    }

    public PluginDisabledException(String message, boolean restartRequested) {
        super(message);
        this.restartRequested = restartRequested;
    }

    public boolean restartRequested() {
        return restartRequested;
    }

    public void logOutcome(Logger logger) {
        if (restartRequested) {
            logger.info("First-time setup: configuration files were generated. Review them before the next startup; a server/proxy restart has been requested.");
        } else {
            logger.error("LibreLogin-Fork is disabled and refuses every login until the reported problem is fixed: " + getMessage());
            logger.error("The server/proxy was deliberately left running. Fix the problem above and restart it.");
        }
    }
}
