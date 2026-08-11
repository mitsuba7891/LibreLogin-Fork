/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.update;

import xyz.kyngs.librelogin.api.util.SemanticVersion;

import java.util.Optional;

/** Uses the secondary source only when the primary source cannot be queried. */
public final class FallbackUpdateChecker {

    private final UpdateSource primary;
    private final UpdateSource fallback;

    public FallbackUpdateChecker(UpdateSource primary, UpdateSource fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    public CheckResult findUpdate(SemanticVersion currentVersion, String platform) throws Exception {
        try {
            return new CheckResult(primary.name(), primary.findUpdate(currentVersion, platform), null);
        } catch (Exception primaryFailure) {
            try {
                return new CheckResult(
                        fallback.name(),
                        fallback.findUpdate(currentVersion, platform),
                        primaryFailure
                );
            } catch (Exception fallbackFailure) {
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;
            }
        }
    }

    public record CheckResult(
            String source,
            Optional<UpdateCandidate> update,
            Exception primaryFailure
    ) {
        public boolean usedFallback() {
            return primaryFailure != null;
        }
    }
}
