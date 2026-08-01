# LibreLogin modernization report

## Baseline and branch

- Upstream: `https://github.com/kyngs/LibreLogin.git`
- Working branch: `feature/modernizacion-2026`
- Baseline tag: `0.24.0`
- Modules found: `API` and `Plugin`; Paper and Velocity are source packages in one plugin module.
- Configuration: Configurate YAML with revisioned migrators for `config.yml` and `messages.yml`; HOCON remains a legacy input format.
- Database drivers: MariaDB Java client is the MySQL-compatible implementation; SQLite and PostgreSQL are separate Libby-managed drivers.
- TOTP API: `dev.samstevens.totp:totp`, exposed internally through the existing `TOTPProvider` API.
- QR projection: optional Protocolize integration for proxy platforms.

## Fase 0 — Preparación

- The repository was cloned from upstream and work is isolated on the dedicated branch above.
- CI found: GitHub Dependabot configuration and Woodpecker build/publish pipeline.
- Woodpecker now builds and verifies the platform-labelled artifacts without downloading a fixed historical server. A live Paper/Velocity server matrix remains a release prerequisite.

## Fase 1 — Issue triage

The live GitHub API was queried during this run. The exact count is intentionally not treated as a permanent project fact. The highest-risk reports observed were:

- compatibility and joining failures around recent Minecraft releases;
- Adventure translation class loading on Paper;
- dependency/library download failures;
- Mojang API 503/rate limiting;
- database migration correctness;
- Protocolize/2FA compatibility.

Relevant upstream references observed during triage include #259, #389, #399, #400 and PRs #366 and #391. The current open-item audit also reviewed #402 and #26. The classification is:

- **#402 — horse lost when logging in while mounted:** reproducible steps were provided and the Paper spawn flow now calls `Player#leaveVehicle()` before selecting/teleporting the destination. This is an implemented defensive fix, but still requires confirmation on a live Paper server with a tamed horse.
- **#403 — Adventure `TypeNotPresentException` on Paper 1.20.1:** credible classpath/version conflict, but the report has no complete stack trace or reproduction beyond the exception type. No speculative dependency change was made; it remains pending a controlled Paper 1.20.1 reproduction.
- **#400 — HTTP 503 from a fallback premium API:** external service outage. The existing provider already falls back through Mojang, PlayerDB and MineTools; no code change is justified without a reliable replacement service and failure test.
- **#399 — disconnect on 1.21.4:** insufficient diagnostics (only “it says disconnected”). Requires server log, proxy topology and client/server test matrix before changing the login protocol.
- **#389 — library checksum failure:** the report refers to `LibreLoginNext` and a different repository, so it is not actionable against this LibreLogin tree.
- **#391 — PacketEvents update:** an unmerged compatibility proposal for a newer Minecraft line. It belongs to the version-support phase and must be tested against the existing minimum before applying.
- **#26 — configurable proxy list for Mojang rate limits:** enhancement; current fallback providers partially address the motivation, but configurable outbound proxies are not implemented.

Issue #402 is the only current high-priority report with a sufficiently concrete, low-risk local fix in this phase. The remaining issues are intentionally not marked fixed without reproducible tests; upstream issue state must be rechecked before a release commit.

## Fase 2 — platform versions

Official release metadata was queried from the PaperMC Fill API:

- Paper: `https://fill.papermc.io/v3/projects/paper/versions`
- Velocity: `https://fill.papermc.io/v3/projects/velocity/versions`

The current supported release lines observed during this audit were Paper `26.2` and `26.1.2`, both requiring Java 25, and Velocity `4.0.0`, also requiring Java 25. The current Java-21 line includes Velocity `3.5.1` as a supported official release line. These facts were recorded from the official Fill API during the run and must be rechecked before a future release.

This phase intentionally does **not** claim support for Paper 26.x or Velocity 4.x. The project currently compiles with Java 21, targets Paper API `1.21.4-R0.1-SNAPSHOT`, uses a Velocity API snapshot plus a private `velocity-proxy` snapshot, and relies on internal/reflected classes such as `InitialInboundConnection`, `MinecraftConnection`, `VelocityConfiguration`, and NMS encryption methods. The Paper API coordinates for the queried 26.x lines were not verifiably resolvable using the repository's current Maven pattern, while the stable Velocity API coordinates are available but cannot be adopted independently from the internal proxy coupling.

The safe result of this phase is therefore:

- Java 21 remains the declared build/runtime baseline.
- README now states Java 21+, matching the Gradle toolchain and CI build images.
- PacketEvents was later bumped from 2.7.0 to 2.13.0 (current Codemc release, June 2026) because 2.7.0 cannot parse Paper 26.x version strings (`26.1.2.build.74`) and lacks protocol mappings for the newest Minecraft lines; 2.13.0 adds support for them. Paper, Velocity and Protocolize themselves remain un-bumped, pending live-server verification.
- A real Paper/Velocity minimum-maximum server matrix remains required before declaring expanded support.

Required work before a 26.x/4.x upgrade includes replacing or isolating private proxy/NMS access, updating protocol mappings beyond the current Protocolize ceiling `767`, verifying PacketEvents compatibility, and testing login encryption, fallback routing, limbo, TOTP QR rendering, and configuration migration on live servers.

## Fase 3 — JDBC dependencies

The following current stable versions were verified against Maven Central metadata during this run and applied to the Libby declarations:

- `org.mariadb.jdbc:mariadb-java-client:3.5.10`
- `com.zaxxer:HikariCP:7.1.0`
- `org.xerial:sqlite-jdbc:3.53.2.1`
- `org.postgresql:postgresql:42.7.13`
- `com.mysql:mysql-connector-j:9.3.0` for the official MySQL JDBC implementation, alongside MariaDB Connector/J.

The MySQL-compatible connector selects the implementation from the configured URL: `jdbc:mariadb://`
uses the relocated MariaDB driver, while `jdbc:mysql://` and `jdbc:mysql+srv://` use the relocated
official MySQL driver. Usernames and passwords remain separate Hikari properties and are not embedded
in the URL. Sources used: Maven Central metadata and the exact POM files linked in
[`docs/dependency-licenses.md`](dependency-licenses.md). Recheck these coordinates periodically; the
build remains explicit and reproducible rather than resolving a moving `latest` selector.

The drivers remain Libby-managed runtime libraries. Existing Libby relocation metadata is preserved for MariaDB, HikariCP and PostgreSQL, and the source uses the corresponding relocated MariaDB driver name. SQLite is intentionally not relocated because its jar contains platform-specific native resources; live SQLite startup tests remain required after this bump.

The current TOTP coordinate was already at the latest non-snapshot version returned by the same source, so it was not changed. Protocolize is not a Maven Central artifact in this project; its API is optional and remains compile-only.

## BungeeCord removal

BungeeCord support was intentionally removed after explicit user approval. The complete BungeeCord source package, `bungee.yml`, archive task, API/dependency declarations, CI switches and platform documentation were removed.

Velocity's optional RedisBungee integration remains as a Velocity-only feature; limbo servers are now ordinary registered Paper backends rather than dynamically created servers.

## Fase 4 — 2FA

The existing `TOTPProvider` interface is already the correct seam for the algorithm implementation. `AuthenticTOTPProvider` remains behind that interface; a separate physical TOTP artifact was **not** introduced in this phase. Protocolize is optional and detected at runtime; its QR renderer is not loaded unless the integration is present. The redundant construction path was corrected so the validated projector instance is reused. `/2fa` now checks both the TOTP provider and QR projector before starting the flow, returning the existing generic error message instead of raising a `NullPointerException` when an optional integration is unavailable. Protocolize's implementation now lives under `velocity.integration.protocolize`, keeping the optional proxy dependency out of the common package and out of the Paper artifact through the existing platform filter.

Protocolize upstream metadata observed during the run: `https://github.com/Exceptionflug/protocolize`, latest release metadata available there at the time of inspection. Compatibility is still constrained by the project's explicit version check and packet protocol range; this requires live proxy/client testing before a dependency bump.

## Fase 5 — artifacts

The build exposes separate Paper and Velocity archive tasks and names their outputs with the platform and project version. The artifacts are filtered from the shared plugin output.

This is an intermediate packaging step, not a claim that all runtime dependency graphs are fully minimized. Protocolize is isolated under the Velocity integration package, but a future source-set/module split is still required to remove all platform-only implementation dependencies completely. The former `mcupload` publication plugin was removed because it accepted one shared file and could not safely publish the two platform artifacts independently. The CI pipeline now builds and verifies both jars; publication to Modrinth, Polymart, GitHub Releases or Discord must be handled manually or by a future platform-aware release pipeline.

## Fase 6 — configuration and translations

HOCON-to-YAML is now implemented through Configurate's format-independent node tree. Existing `.conf` files are converted to `.yml`, validated, and retained as `.pre-yaml.bak` backups. The migration details are documented in `docs/configuration-migration.md`.

No language source files exist in the repository: messages are generated from `MessageKeys` into the administrator's `messages.yml`. Therefore no separate language conversion script was needed; the same generated message keys and placeholders are serialized into YAML. Automated tests now verify that message keys are unique, defaults have valid placeholder syntax, and generated YAML can be reloaded after HOCON conversion.

## Explicitly incomplete acceptance items

This branch does not claim completion of the following requested items:

- full support through the latest Paper/Velocity release lines;
- a true source-set/module split with minimal dependency graphs;
- a separate physical TOTP/Protocolize artifact;
- a checked-in translation corpus for independent language-diff testing;
- a platform-aware automated publication pipeline for both jars;
- live server compatibility and security/dependency-license matrix testing.

## Fase 7/8 — QA and delivery status

### Java compatibility matrix

The project is compiled with Java 21 bytecode using `JavaCompile.options.release = 21` for both
subprojects. The complete Gradle build was validated with Gradle 9.6.1 on JDK 21, 22, 23, 24
and 25. Every generated class inspected had major version 65 (Java 21).

The verification performed during this run was:

| Environment | Result | Meaning |
| --- | --- | --- |
| JDK 21 + Gradle 9.6.1 | Pass | Full build, license checks, tests and platform artifacts |
| JDK 22 + Gradle 9.6.1 | Pass | Full build, license checks, tests and platform artifacts |
| JDK 23 + Gradle 9.6.1 | Pass | Full build, license checks, tests and platform artifacts |
| JDK 24 + Gradle 9.6.1 | Pass | Full build, license checks, tests and platform artifacts |
| JDK 25 + Gradle 9.6.1 | Pass | Full build, license checks, tests and platform artifacts |

This establishes Java 21–25 as supported build environments for this branch. It does not by
itself certify every Paper, Velocity, Protocolize or Minecraft-version combination; those still
require live server testing. Java 21 remains the bytecode and minimum runtime target.

The old Cadixdev license plugin and Kyori Blossom plugin were removed. License headers are now
checked and updated by native Gradle tasks, and the Velocity versioned source is generated from
a checked-in template. Shadow was migrated to `com.gradleup.shadow:8.3.11`. The former
`xyz.kyngs.librelogin.libby.plugin` Gradle plugin was removed because its repository
(`repo.kyngs.xyz/gradle-plugins`) no longer exists (404) and the plugin is absent from the Gradle
Plugin Portal. `libby.json` is now generated by the `generateLibbyJson` task in
`Plugin/build.gradle.kts`: it resolves the `libby` configuration, computes SHA-256 base64
checksums of every artifact, and writes the same schema (version/libraries/repositories/
relocations, groups with `{}`). Output was verified byte-identical in checksums to the previous
plugin's JSON (39 libraries). The `libby` dependencies are exposed on the compile classpath via
`compileOnly.extendsFrom(libby)` without being shaded into the jar, preserving the runtime
download behavior.


Authoritative references used for the Java/Gradle distinction:

- [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [Gradle Java toolchains](https://docs.gradle.org/current/userguide/toolchains.html)
- [Gradle current release metadata](https://services.gradle.org/versions/current)

### Checks completed

- `:API:checkLicenses`, `:Plugin:checkLicenses` and root `licenseCheck`: pass.
- `:Plugin:compileJava`, `:Plugin:shadowJar` and existing tests: pass.
- `:Plugin:platformJars`: pass on JDK 21, 22, 23, 24 and 25.
- Generated artifacts: platform jars under `Plugin/build/libs/platform/`. The version
  auto-increments in `gradle.properties` on every build that compiles the Plugin module
  (`0.24.x`), and jar names plus `plugin.yml`/`velocity-plugin.json` follow the bumped value;
  `-PnoBump` keeps the version fixed (used by CI/releases, where it must stay aligned with the
  git tag). Old jars from previous versions are not deleted automatically; `./gradlew clean`
  removes them.
- Header audit: 182 Java/source-template files checked, 0 missing headers.
- Class-file audit: major version 65, confirming Java 21 bytecode.

### Additional fixes applied

- YAML output is written in block style (`NodeStyle.BLOCK`): maps are expanded one key per line
  and lists use `- item`, both on HOCON-to-YAML migration and on regular saves. Existing
  flow-style files keep parsing and are rewritten in block style on the next save. The migration
  test fixture now covers nested flow-style maps and lists.
- Paper startup was hardened for the Paper 26.x plugin lifecycle: initialization is shared
  between `onLoad` and `onEnable` (idempotent), `onDisable` is null-safe, and a diagnostic
  warning is logged when `onLoad` did not initialize the plugin. This fixes the
  `NullPointerException` observed when enabling on Paper 26.1.2 (Paper 1.21.4 was unaffected). A
  live test on Paper 26.1.2 with the regenerated jar is still required to confirm the root
  cause.
- PacketEvents was bumped from 2.7.0 to 2.13.0 so the runtime version-string parsing handles
  Paper 26.x builds (`26.1.2.build.74`). The compile passed against the new API unchanged, and
  `libby.json` now lists the 2.13.0 artifacts; their checksums are omitted (Codemc re-uploads
  PacketEvents under the same version, which would fail the runtime checksum check). A live
  Paper 26.1.2 boot remains the acceptance gate.
- Fixed a Velocity authorization crash on `4.1.0-SNAPSHOT`: the plugin had been compiled against
  Adventure 4.14.0 using the removed `Title.Times.of(Duration, Duration, Duration)` factory,
  while the proxy supplied a newer Adventure API. The call now uses `Title.Times.times(...)`,
  which preserves title notifications on the modern runtime. The full Plugin test suite and
  Paper/Velocity artifact build passed with this change; a live Velocity login remains the final
  compatibility check.
- Generated `config.yml`/`messages.yml` now carry per-key guide comments: Configurate 4.1.x does
  not write node comments to YAML, so `ConfigurateConfiguration` re-injects them as `#` lines
  above each key when saving (atomically, with a validation reload before the file move). Fresh
  and migrated-from-HOCON files are block style with comments; `ConfigDumpTest` asserts block
  style (no inline flow maps), the guide comments (including the `mail:` section and the
  revision) and legacy-value preservation as regression guards. The revision-7 config migrator
  is null-safe for a missing `kick-on-wrong-password`, and the `mail:` section comment is now
  emitted (the `MAIL` key was made public so `GeneralUtil.extractKeys` includes it).

### Remaining release prerequisites

This branch documents the checks and known blockers. A release-quality claim still requires:

- live Paper and Velocity server tests at the minimum and maximum supported versions;
- Protocolize QR tests on supported client protocol versions;
- live MariaDB, SQLite and PostgreSQL connection tests, including SQLite native extraction;
- fixture tests for every configuration revision and database migration;
- an explicit dependency-license report for the final resolved graph;
- live Paper and Velocity compatibility tests at the selected minimum and maximum versions.

The modernization phases are therefore complete as an audited implementation and documentation
pass, but the branch is not a release certification for every Minecraft version, proxy version,
database backend or Java build environment.
