/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.update;

import org.junit.jupiter.api.Test;
import xyz.kyngs.librelogin.api.util.SemanticVersion;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FallbackUpdateCheckerTest {

    @Test
    void usesGitHubWhenModrinthFails() throws Exception {
        var expected = new UpdateCandidate(
                SemanticVersion.parse("0.24.10"),
                "LibreLogin Fork 0.24.10",
                "LibreLogin-Paper-0.24.10.jar",
                "https://github/download",
                "https://github/release"
        );
        var checker = new FallbackUpdateChecker(
                new StubSource("Modrinth", (version, platform) -> {
                    throw new IOException("HTTP 404");
                }),
                new StubSource("GitHub fallback", (version, platform) -> Optional.of(expected))
        );

        var result = checker.findUpdate(SemanticVersion.parse("0.24.9"), "paper");

        assertTrue(result.usedFallback());
        assertEquals("GitHub fallback", result.source());
        assertEquals(expected, result.update().orElseThrow());
        assertEquals("HTTP 404", result.primaryFailure().getMessage());
    }

    @Test
    void doesNotQueryGitHubWhenModrinthSucceeds() throws Exception {
        var fallbackCalled = new AtomicBoolean();
        var checker = new FallbackUpdateChecker(
                new StubSource("Modrinth", (version, platform) -> Optional.empty()),
                new StubSource("GitHub fallback", (version, platform) -> {
                    fallbackCalled.set(true);
                    return Optional.empty();
                })
        );

        var result = checker.findUpdate(SemanticVersion.parse("0.24.9"), "velocity");

        assertFalse(result.usedFallback());
        assertEquals("Modrinth", result.source());
        assertFalse(fallbackCalled.get());
    }

    @Test
    void preservesPrimaryFailureWhenBothSourcesFail() {
        var primaryFailure = new IOException("Modrinth unavailable");
        var fallbackFailure = new IOException("GitHub unavailable");
        var checker = new FallbackUpdateChecker(
                new StubSource("Modrinth", (version, platform) -> {
                    throw primaryFailure;
                }),
                new StubSource("GitHub fallback", (version, platform) -> {
                    throw fallbackFailure;
                })
        );

        var thrown = assertThrows(IOException.class, () -> checker.findUpdate(
                SemanticVersion.parse("0.24.9"), "paper"
        ));

        assertSame(fallbackFailure, thrown);
        assertArrayEquals(new Throwable[]{primaryFailure}, thrown.getSuppressed());
    }

    private record StubSource(String name, Finder finder) implements UpdateSource {
        @Override
        public Optional<UpdateCandidate> findUpdate(SemanticVersion currentVersion, String platform)
                throws Exception {
            return finder.find(currentVersion, platform);
        }
    }

    @FunctionalInterface
    private interface Finder {
        Optional<UpdateCandidate> find(SemanticVersion currentVersion, String platform) throws Exception;
    }
}
