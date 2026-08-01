Unreleased — Modernization audit

- Migrated active configuration and messages to YAML with automatic HOCON conversion and backups.
- Added platform-labelled Paper and Velocity artifact tasks.
- Updated the MariaDB, SQLite and PostgreSQL JDBC versions from Maven Central metadata.
- Removed the obsolete BungeeCord platform, dependencies, metadata and build path; Paper and Velocity remain supported.
- Reused the validated Protocolize projector during proxy 2FA setup.
- Updated Woodpecker artifact verification to avoid retired PaperMC endpoints and fixed historical server downloads.
- Documented the remaining release blockers and incomplete modernization acceptance items.
- Prevented players from carrying vehicles into Paper limbo during login, addressing the horse-death reproduction from #402 (pending live-server confirmation).
- Audited official Paper/Velocity release lines and retained the Java 21 support line until Java 25 and internal API compatibility are tested.
- Updated MariaDB Connector/J, HikariCP, SQLite JDBC and PostgreSQL JDBC to current stable Maven Central releases; retained Libby loading and documented SQLite native-resource and LGPL obligations.
- Verified the complete Gradle build on JDK 21, 22, 23, 24 and 25, producing Java 21 bytecode (major 65) on every JVM.
- Replaced the legacy license-header and Blossom plugins with native Gradle tasks and generated Velocity sources; upgraded the wrapper to Gradle 9.6.1 and Shadow to 8.3.11 for Libby compatibility.
- Removed the `mcupload` publication plugin and its credentials from CI; Paper and Velocity jars are still generated and verified, while external publication is now manual until a platform-aware release pipeline is added.
- Isolated the optional Protocolize QR implementation under the Velocity integration package and added automated HOCON-to-YAML migration and MessageKeys integrity tests.
- Closed the modernization audit phases with documented Paper/Velocity artifacts, YAML migration, 2FA safeguards, QA results and remaining live-server/release prerequisites.
- Added an automatic version bump: every build that compiles the Plugin module increments the patch version in gradle.properties (0.24.x), and jar names plus plugin.yml/velocity-plugin.json follow the bumped value. Use -PnoBump to keep the version fixed (CI/releases).
- Replaced the xyz.kyngs.librelogin.libby.plugin Gradle plugin (its repository repo.kyngs.xyz/gradle-plugins no longer exists) with a self-contained generateLibbyJson task producing the identical libby.json: SHA-256 base64 checksums, repositories and relocations. The dependencies stay compile-only and are downloaded by Libby at runtime.
- Wrote generated YAML configuration in block style (NodeStyle.BLOCK) instead of flow style, so config.yml/messages.yml are readable one-key-per-line.
- Hardened Paper startup for the Paper 26.x plugin lifecycle (shared onLoad/onEnable initialization, null-safe onDisable, diagnostic warning), fixing the NullPointerException when enabling on Paper 26.1.2.
- Bumped PacketEvents from 2.7.0 to 2.13.0: 2.7.0 cannot parse Paper 26.x version strings ('26.1.2.build.74') and lacks mappings for the newest Minecraft lines; 2.13.0 adds support for them. PacketEvents checksums are omitted from libby.json because Codemc re-uploads those artifacts with the same version (runtime checksum failures), matching the old plugin's noChecksumDependency intent.
- Added per-key guide comments to the generated YAML config: every key in config.yml now carries a '#' guide line above it (Configurate 4.1.x drops node comments in YAML output, so ConfigurateConfiguration re-injects them on save, atomically and with a validation reload). Fresh configs and legacy .conf migrations are both written in readable block style with comments.
- Hardened the configuration migration path: the revision-7 migrator no longer crashes startup when a legacy config lacks the optional kick-on-wrong-password key, and the mail: section now emits its guide comment (its key was made public so the key extractor includes it).
- Added the official MySQL Connector/J alongside MariaDB Connector/J. The MySQL database connector now selects the driver from the URL scheme: `jdbc:mariadb://` uses MariaDB and `jdbc:mysql://` uses MySQL, while credentials remain in separate username/password fields.
- Fixed the Velocity authorization crash caused by Adventure API incompatibility: replaced removed `Title.Times.of(...)` with `Title.Times.times(...)`, preserving title notifications on modern Velocity versions.
- Converted the config dump tests into regression tests: ConfigDumpTest asserts block style (no inline flow maps), the presence of the guide comments (including the mail section and revision), and legacy-value preservation across HOCON-to-YAML migration.

0.24.0 - 1.21.1 - 1.21.4 Support

- Add support for 1.21.4
- Add support for Java 23
- Fix "logged in from another location" issue on Paper (see GH #296)