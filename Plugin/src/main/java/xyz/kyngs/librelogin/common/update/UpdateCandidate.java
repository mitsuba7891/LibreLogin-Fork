/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.update;

import xyz.kyngs.librelogin.api.util.SemanticVersion;

/** A stable update and the platform-specific artifact recommended for it. */
public record UpdateCandidate(
        SemanticVersion version,
        String name,
        String filename,
        String downloadUrl,
        String projectUrl
) {
}
