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

class GitHubUpdateCheckerTest {

    @Test
    void selectsNewestStableForkAssetForPlatform() {
        var releases = JsonParser.parseString("""
                [
                  {
                    "tag_name": "v0.25.0",
                    "name": "Preview",
                    "draft": false,
                    "prerelease": true,
                    "html_url": "https://github/releases/preview",
                    "assets": [{"name": "LibreLogin-Paper-0.25.0.jar", "browser_download_url": "https://github/preview.jar"}]
                  },
                  {
                    "tag_name": "v0.24.10",
                    "name": "LibreLogin Fork 0.24.10",
                    "draft": false,
                    "prerelease": false,
                    "html_url": "https://github/releases/0.24.10",
                    "assets": [
                      {"name": "LibreLogin-0.24.10.zip", "browser_download_url": "https://github/bundle.zip"},
                      {"name": "LibreLogin-Paper-0.24.10.jar", "browser_download_url": "https://github/paper.jar"},
                      {"name": "LibreLogin-Velocity-0.24.10.jar", "browser_download_url": "https://github/velocity.jar"}
                    ]
                  },
                  {
                    "tag_name": "v0.24.9",
                    "name": "LibreLogin Fork 0.24.9",
                    "draft": false,
                    "prerelease": false,
                    "html_url": "https://github/releases/0.24.9",
                    "assets": [{"name": "LibreLogin-Paper-0.24.9.jar", "browser_download_url": "https://github/old.jar"}]
                  }
                ]
                """).getAsJsonArray();

        var update = GitHubUpdateChecker.selectUpdate(
                releases, SemanticVersion.parse("0.24.8"), "paper"
        ).orElseThrow();

        assertEquals("0.24.10", update.version().toString());
        assertEquals("LibreLogin-Paper-0.24.10.jar", update.filename());
        assertEquals("https://github/paper.jar", update.downloadUrl());
        assertEquals("https://github/releases/0.24.10", update.projectUrl());
    }

    @Test
    void ignoresReleaseWithoutMatchingPlatformAsset() {
        var releases = JsonParser.parseString("""
                [{
                  "tag_name": "v0.24.10",
                  "name": "Paper only",
                  "draft": false,
                  "prerelease": false,
                  "assets": [{"name": "LibreLogin-Paper-0.24.10.jar", "browser_download_url": "https://github/paper.jar"}]
                }]
                """).getAsJsonArray();

        assertTrue(GitHubUpdateChecker.selectUpdate(
                releases, SemanticVersion.parse("0.24.9"), "velocity"
        ).isEmpty());
    }
}
