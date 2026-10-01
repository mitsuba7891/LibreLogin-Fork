# API and Build

[Home](Home.md) · [Repository](https://github.com/mitsuba7891/LibreLogin-Fork)

**Jump to:** [Verification](#verify-the-project) · [Packaging](#package-the-artifacts) · [Integration API](#integration-api) · [Javadoc](#generate-local-javadoc)

## Requirements

- **JDK 25** to compile against Paper API 26.2.
- The repository's `./gradlew` wrapper.
- Access to dependency repositories for the first build.

> **Keep the version unchanged:** `gradle.properties` currently defines `0.25.0`. The build can increment its patch automatically; use **`-PnoBump`** when verifying or packaging that version.

## Verify the project

With `JAVA_HOME` pointing to your JDK 25:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :Plugin:compileJava :Plugin:test :Plugin:licenseCheck
```

In this fork's working environment, the temporary JDK is at `/home/ubuntu/.jdk25`:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths=/home/ubuntu/.jdk25 \
  :Plugin:compileJava :Plugin:test :Plugin:licenseCheck
```

That directory is not distributed or required on other machines. If Gradle reports `languageVersion=25`, check your JDK installation path.

Test results are in `Plugin/build/test-results/test/`; the HTML report is `Plugin/build/reports/tests/test/index.html`.

## Package the artifacts

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" \
  :Plugin:platformJars :Plugin:releaseArchive :Plugin:licenseCheck
```

Outputs for the current base version:

```text
Plugin/build/libs/platform/LibreLogin-Paper-0.25.0.jar
Plugin/build/libs/platform/LibreLogin-Velocity-0.25.0.jar
Plugin/build/libs/platform/AuthLimbo-1.0.0.jar
Plugin/build/distributions/LibreLogin-0.25.0.zip
```

Building does not publish commits, tags, releases or artifacts. Published ZIP/JAR files belong to their release revision; a local `beta` build may include later fixes or documentation changes.

## Integration API

Interfaces are in the [API module](https://github.com/mitsuba7891/LibreLogin-Fork/tree/beta/API/src/main/java/xyz/kyngs/librelogin/api):

| Interface or package | Purpose |
|---|---|
| `LibreLoginPlugin` | Services provided by the plugin. |
| `authorization/AuthorizationProvider` | Authorization state and 2FA confirmation. |
| `database/User` | Account profile and metadata. |
| `event` | Authentication and account-change events. |
| `totp/TOTPProvider` | Factor setup and verification. |

Use interfaces from the revision you deploy. A connected client is not necessarily authenticated; integrations that move players must respect authorization and 2FA state.

## Generate local Javadoc

The fork does not have a deployed Javadoc website. Generate local documentation with:

```bash
./gradlew -PnoBump -Dorg.gradle.java.installations.paths="$JAVA_HOME" :API:javadoc
```

Output: **`API/build/docs/javadoc/`**. Maven publication is opt-in through `publishRepositoryUrl` and requires credentials for the chosen repository; it does not automatically publish to a third party's Maven server.

## Licenses

`licenseCheck` checks source headers, not dependency licensing. See the [dependency license report](https://github.com/mitsuba7891/LibreLogin-Fork/blob/beta/docs/dependency-licenses.md) for that inventory's status.
