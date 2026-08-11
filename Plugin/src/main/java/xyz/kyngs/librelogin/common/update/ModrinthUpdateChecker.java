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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks the public LibreLogin Fork project on Modrinth without downloading
 * or replacing the installed plugin.
 */
public final class ModrinthUpdateChecker implements UpdateSource {

    public static final String PROJECT_PAGE = "https://modrinth.com/plugin/librelogin-fork";
    private static final String VERSIONS_ENDPOINT =
            "https://api.modrinth.com/v2/project/librelogin-fork/version?include_changelog=false";
    private static final Pattern STABLE_VERSION = Pattern.compile("^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Set<String> PAPER_LOADERS = Set.of("paper", "bukkit", "spigot", "purpur");

    private final Gson gson;
    private final String userAgent;

    public ModrinthUpdateChecker(Gson gson, String installedVersion) {
        this.gson = gson;
        this.userAgent = "mitsuba7891/LibreLogin-Fork/%s (%s)".formatted(installedVersion, PROJECT_PAGE);
    }

    @Override
    public String name() {
        return "Modrinth";
    }

    @Override
    public Optional<UpdateCandidate> findUpdate(SemanticVersion currentVersion, String platform)
            throws IOException {
        var connection = (HttpURLConnection) URI.create(VERSIONS_ENDPOINT).toURL().openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        try {
            var responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Modrinth returned HTTP " + responseCode);
            }

            try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                var versions = gson.fromJson(reader, JsonArray.class);
                return selectUpdate(versions, currentVersion, platform);
            }
        } finally {
            connection.disconnect();
        }
    }

    static Optional<UpdateCandidate> selectUpdate(
            JsonArray versions, SemanticVersion currentVersion, String platform
    ) {
        if (versions == null) {
            return Optional.empty();
        }

        UpdateCandidate latest = null;
        for (var rawVersion : versions) {
            if (!rawVersion.isJsonObject()) {
                continue;
            }

            var version = rawVersion.getAsJsonObject();
            if (!isStableRelease(version) || !supportsPlatform(version, platform)) {
                continue;
            }

            var parsedVersion = parseVersion(stringValue(version, "version_number"));
            if (parsedVersion.isEmpty() || currentVersion.compare(parsedVersion.get()) >= 0) {
                continue;
            }

            var file = selectFile(version, platform);
            if (file.isEmpty()) {
                continue;
            }

            var selectedFile = file.get();
            var releaseName = stringValue(version, "name");
            var candidate = new UpdateCandidate(
                    parsedVersion.get(),
                    releaseName == null || releaseName.isBlank()
                            ? "LibreLogin Fork " + parsedVersion.get()
                            : releaseName,
                    stringValue(selectedFile, "filename"),
                    stringValue(selectedFile, "url"),
                    PROJECT_PAGE
            );
            if (latest == null || latest.version().compare(candidate.version()) < 0) {
                latest = candidate;
            }
        }

        return Optional.ofNullable(latest);
    }

    private static boolean isStableRelease(JsonObject version) {
        var type = stringValue(version, "version_type");
        return type == null || type.equalsIgnoreCase("release");
    }

    private static boolean supportsPlatform(JsonObject version, String platform) {
        if (!version.has("loaders") || !version.get("loaders").isJsonArray()) {
            return true;
        }

        var normalizedPlatform = platform.toLowerCase(Locale.ROOT);
        for (var rawLoader : version.getAsJsonArray("loaders")) {
            if (!rawLoader.isJsonPrimitive()) {
                continue;
            }
            var loader = rawLoader.getAsString().toLowerCase(Locale.ROOT);
            if (loader.equals(normalizedPlatform)
                    || normalizedPlatform.equals("paper") && PAPER_LOADERS.contains(loader)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<JsonObject> selectFile(JsonObject version, String platform) {
        if (!version.has("files") || !version.get("files").isJsonArray()) {
            return Optional.empty();
        }

        JsonObject primary = null;
        JsonObject first = null;
        var platformNeedle = platform.toLowerCase(Locale.ROOT);
        for (var rawFile : version.getAsJsonArray("files")) {
            if (!rawFile.isJsonObject()) {
                continue;
            }

            var file = rawFile.getAsJsonObject();
            var filename = stringValue(file, "filename");
            var url = stringValue(file, "url");
            if (filename == null || url == null) {
                continue;
            }

            if (first == null) {
                first = file;
            }
            if (file.has("primary") && file.get("primary").isJsonPrimitive()
                    && file.get("primary").getAsBoolean()) {
                primary = file;
            }
            if (filename.toLowerCase(Locale.ROOT).contains(platformNeedle)) {
                return Optional.of(file);
            }
        }

        return Optional.ofNullable(primary != null ? primary : first);
    }

    static Optional<SemanticVersion> parseVersion(String value) {
        if (value == null) {
            return Optional.empty();
        }

        var matcher = STABLE_VERSION.matcher(value.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                false
        ));
    }

    private static String stringValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        return object.get(key).getAsString();
    }
}
