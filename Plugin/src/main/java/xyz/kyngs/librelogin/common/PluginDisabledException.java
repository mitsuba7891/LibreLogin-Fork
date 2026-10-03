/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

/**
 * Signals that LibreLogin must stop initialising and stay disabled.
 *
 * <p>LibreLogin deliberately does not terminate the JVM in this situation.
 * An authentication plugin that cannot start has to refuse connections; it
 * must not take the proxy or the server, and every other plugin running on
 * it, down as well.</p>
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
}
