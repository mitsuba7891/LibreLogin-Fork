/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.update;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.util.SemanticVersion;

import static org.junit.jupiter.api.Assertions.*;

class ModrinthUpdateCheckerTest {

    @Test
    void selectsNewestStableReleaseAndPlatformFile() {
        var versions = JsonParser.parseString("""
                [
                  {
                    "name": "LibreLogin Fork 0.24.9",
                    "version_number": "0.24.9",
                    "version_type": "release",
                    "loaders": ["paper", "velocity"],
                    "files": [
                      {"filename": "LibreLogin-0.24.9.zip", "url": "https://cdn/bundle.zip", "primary": true},
                      {"filename": "LibreLogin-Paper-0.24.9.jar", "url": "https://cdn/paper.jar", "primary": false},
                      {"filename": "LibreLogin-Velocity-0.24.9.jar", "url": "https://cdn/velocity.jar", "primary": false}
                    ]
                  },
                  {
                    "name": "LibreLogin Fork 0.24.10",
                    "version_number": "v0.24.10",
                    "version_type": "release",
                    "loaders": ["paper", "velocity"],
                    "files": [
                      {"filename": "LibreLogin-Paper-0.24.10.jar", "url": "https://cdn/paper-24-10.jar", "primary": true},
                      {"filename": "LibreLogin-Velocity-0.24.10.jar", "url": "https://cdn/velocity-24-10.jar", "primary": false}
                    ]
                  }
                ]
                """).getAsJsonArray();

        var update = ModrinthUpdateChecker.selectUpdate(
                versions, SemanticVersion.parse("0.24.8"), "paper"
        ).orElseThrow();

        assertEquals("0.24.10", update.version().toString());
        assertEquals("LibreLogin-Paper-0.24.10.jar", update.filename());
        assertEquals("https://cdn/paper-24-10.jar", update.downloadUrl());
    }

    @Test
    void ignoresPrereleasesAndUnsupportedPlatforms() {
        var versions = JsonParser.parseString("""
                [
                  {
                    "name": "Beta",
                    "version_number": "0.25.0",
                    "version_type": "beta",
                    "loaders": ["velocity"],
                    "files": [{"filename": "LibreLogin-Velocity-0.25.0.jar", "url": "https://cdn/beta.jar", "primary": true}]
                  },
                  {
                    "name": "Paper only",
                    "version_number": "0.24.10",
                    "version_type": "release",
                    "loaders": ["paper"],
                    "files": [{"filename": "LibreLogin-Paper-0.24.10.jar", "url": "https://cdn/paper.jar", "primary": true}]
                  },
                  {
                    "name": "Velocity stable",
                    "version_number": "0.24.9",
                    "version_type": "release",
                    "loaders": ["velocity"],
                    "files": [{"filename": "LibreLogin-Velocity-0.24.9.jar", "url": "https://cdn/velocity.jar", "primary": true}]
                  }
                ]
                """).getAsJsonArray();

        var update = ModrinthUpdateChecker.selectUpdate(
                versions, SemanticVersion.parse("0.24.8"), "velocity"
        ).orElseThrow();

        assertEquals("0.24.9", update.version().toString());
        assertEquals("LibreLogin-Velocity-0.24.9.jar", update.filename());
    }

    @Test
    void returnsEmptyWhenInstalledVersionIsCurrent() {
        var versions = JsonParser.parseString("""
                [{
                  "name": "Current",
                  "version_number": "0.24.9",
                  "version_type": "release",
                  "loaders": ["paper"],
                  "files": [{"filename": "LibreLogin-Paper-0.24.9.jar", "url": "https://cdn/paper.jar", "primary": true}]
                }]
                """).getAsJsonArray();

        assertTrue(ModrinthUpdateChecker.selectUpdate(
                versions, SemanticVersion.parse("0.24.9"), "paper"
        ).isEmpty());
    }
}
