/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xyz.kyngs.librelogin.api.util.SemanticVersion;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/** Public GitHub release fallback for the LibreLogin fork repository. */
public final class GitHubUpdateChecker implements UpdateSource {

    public static final String RELEASES_PAGE =
            "https://github.com/mitsuba7891/LibreLogin-Fork/releases";
    private static final String RELEASES_ENDPOINT =
            "https://api.github.com/repos/mitsuba7891/LibreLogin-Fork/releases?per_page=30";

    private final Gson gson;
    private final String userAgent;

    public GitHubUpdateChecker(Gson gson, String installedVersion) {
        this.gson = gson;
        this.userAgent = "mitsuba7891/LibreLogin-Fork/" + installedVersion;
    }

    @Override
    public String name() {
        return "GitHub fallback";
    }

    @Override
    public Optional<UpdateCandidate> findUpdate(SemanticVersion currentVersion, String platform)
            throws IOException {
        var connection = (HttpURLConnection) URI.create(RELEASES_ENDPOINT).toURL().openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        try {
            var responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub returned HTTP " + responseCode);
            }

            try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                var releases = gson.fromJson(reader, JsonArray.class);
                return selectUpdate(releases, currentVersion, platform);
            }
        } finally {
            connection.disconnect();
        }
    }

    static Optional<UpdateCandidate> selectUpdate(
            JsonArray releases, SemanticVersion currentVersion, String platform
    ) {
        if (releases == null) {
            return Optional.empty();
        }

        UpdateCandidate latest = null;
        for (var rawRelease : releases) {
            if (!rawRelease.isJsonObject()) {
                continue;
            }

            var release = rawRelease.getAsJsonObject();
            if (booleanValue(release, "draft") || booleanValue(release, "prerelease")) {
                continue;
            }

            var parsedVersion = ModrinthUpdateChecker.parseVersion(stringValue(release, "tag_name"));
            if (parsedVersion.isEmpty() || currentVersion.compare(parsedVersion.get()) >= 0) {
                continue;
            }

            var asset = selectPlatformAsset(release, platform);
            if (asset.isEmpty()) {
                continue;
            }

            var selectedAsset = asset.get();
            var releaseName = stringValue(release, "name");
            var page = stringValue(release, "html_url");
            var candidate = new UpdateCandidate(
                    parsedVersion.get(),
                    releaseName == null || releaseName.isBlank()
                            ? "LibreLogin Fork " + parsedVersion.get()
                            : releaseName,
                    stringValue(selectedAsset, "name"),
                    stringValue(selectedAsset, "browser_download_url"),
                    page == null ? RELEASES_PAGE : page
            );
            if (latest == null || latest.version().compare(candidate.version()) < 0) {
                latest = candidate;
            }
        }
        return Optional.ofNullable(latest);
    }

    private static Optional<JsonObject> selectPlatformAsset(JsonObject release, String platform) {
        if (!release.has("assets") || !release.get("assets").isJsonArray()) {
            return Optional.empty();
        }

        var platformNeedle = platform.toLowerCase(Locale.ROOT);
        for (var rawAsset : release.getAsJsonArray("assets")) {
            if (!rawAsset.isJsonObject()) {
                continue;
            }
            var asset = rawAsset.getAsJsonObject();
            var name = stringValue(asset, "name");
            var downloadUrl = stringValue(asset, "browser_download_url");
            if (name != null && downloadUrl != null
                    && name.toLowerCase(Locale.ROOT).contains(platformNeedle)) {
                return Optional.of(asset);
            }
        }
        return Optional.empty();
    }

    private static boolean booleanValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
    }

    private static String stringValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        return object.get(key).getAsString();
    }
}
