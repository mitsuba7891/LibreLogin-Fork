import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.json.JsonOutput
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import java.security.MessageDigest
import java.util.Base64

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.11"
    id("java-library")
}

// The version lives in gradle.properties and is incremented by the root
// 'bumpVersion' task on every build. Reading it lazily here ensures the
// current build picks up the freshly bumped version at execution time.
fun readVersionFromProperties(): String {
    val text = rootProject.file("gradle.properties").readText()
    return Regex("(?m)^version=(\\S+)\\s*$").find(text)?.groupValues?.get(1) ?: "0.24.0"
}

// Package relocations shared between the shadowed jar and the runtime
// libby.json (Libby relocates downloaded libraries on startup).
val relocations = mapOf(
    "co.aikar.acf" to "xyz.kyngs.librelogin.lib.acf",
    "com.github.benmanes.caffeine" to "xyz.kyngs.librelogin.lib.caffeine",
    "com.typesafe.config" to "xyz.kyngs.librelogin.lib.hocon",
    "com.zaxxer.hikari" to "xyz.kyngs.librelogin.lib.hikari",
    "org.mariadb" to "xyz.kyngs.librelogin.lib.mariadb",
    "com.mysql" to "xyz.kyngs.librelogin.lib.mysql",
    "org.bstats" to "xyz.kyngs.librelogin.lib.metrics",
    "org.intellij" to "xyz.kyngs.librelogin.lib.intellij",
    "org.jetbrains" to "xyz.kyngs.librelogin.lib.jetbrains",
    "io.leangen.geantyref" to "xyz.kyngs.librelogin.lib.reflect",
    "org.spongepowered.configurate" to "xyz.kyngs.librelogin.lib.configurate",
    "net.byteflux.libby" to "xyz.kyngs.librelogin.lib.libby",
    "org.postgresql" to "xyz.kyngs.librelogin.lib.postgresql",
)

// Runtime dependencies downloaded by Libby on first startup (declared in the
// dependencies block with libby("group:name:version")). The former Gradle
// plugin (xyz.kyngs.librelogin.libby.plugin, hosted on the now defunct
// repo.kyngs.xyz/gradle-plugins) used to generate libby.json from these.
val libby by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// The removed Gradle plugin also exposed these dependencies on the compile
// classpath without shading them into the jar (they are downloaded at runtime
// by Libby). Replicate that here with compileOnly visibility.
configurations.named("compileOnly") {
    extendsFrom(libby)
}

// Dependency groups excluded from libby.json (replicated from the old DSL).
val libbyExcludedGroups = listOf(
    "org.slf4j",
    "org.checkerframework",
    "com.google.errorprone",
    "com.google.protobuf"
)

// Libraries written to libby.json without a checksum. The Codemc repository
// re-uploads PacketEvents artifacts under the same version with different
// content on occasion, so a build-time checksum goes stale at runtime and the
// download fails (see upstream issue #389 and the old plugin's
// noChecksumDependency). Libby skips verification when the field is absent.
val libbyNoChecksumGroups = listOf(
    "com.github.retrooper"
)

// Repositories mirrored into libby.json for runtime downloads.
val libbyRepositories = listOf(
    "https://repo.maven.apache.org/maven2/",
    "https://jitpack.io",
    "https://repo.opencollab.dev/maven-snapshots/",
    "https://repo.papermc.io/repository/maven-public/",
    "https://hub.spigotmc.org/nexus/",
    "https://repo.kyngs.xyz/public/",
    "https://mvn.exceptionflug.de/repository/exceptionflug-public/",
    "https://repo.dmulloy2.net/repository/public/",
    "https://repo.alessiodp.com/releases/",
    "https://jitpack.io/",
    "https://s01.oss.sonatype.org/content/repositories/snapshots/",
    "https://repo.codemc.io/repository/maven-releases/"
)

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

repositories {
    // mavenLocal()
    maven { url = uri("https://repo.opencollab.dev/maven-snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/") }
    maven { url = uri("https://repo.kyngs.xyz/public/") }
    maven { url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.alessiodp.com/releases/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

val generatedSources = layout.buildDirectory.dir("generated/sources/versioned/main")
val generatedLibbyDir = layout.buildDirectory.dir("generated/libby/main")

val generateVersionedSources = tasks.register("generateVersionedSources") {
    val template = layout.projectDirectory.file("src/main/java-templates/xyz/kyngs/librelogin/velocity/VelocityBootstrap.java.peb")
    inputs.file(template)
    inputs.property("version", providers.provider { readVersionFromProperties() })
    outputs.dir(generatedSources)

    doLast {
        val output = generatedSources.get().file("xyz/kyngs/librelogin/velocity/VelocityBootstrap.java").asFile
        output.parentFile.mkdirs()
        output.writeText(template.asFile.readText().replace("@version@", readVersionFromProperties()))
    }
}

sourceSets {
    named("main") {
        java.srcDir(generatedSources)
        resources.srcDir(generatedLibbyDir)
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateVersionedSources)
}

tasks.withType<ShadowJar> {
    archiveFileName.set("LibreLogin.jar")


    dependencies {
        exclude(dependency("org.slf4j:.*:.*"))
        exclude(dependency("org.checkerframework:.*:.*"))
        exclude(dependency("com.google.errorprone:.*:.*"))
        exclude(dependency("com.google.protobuf:.*:.*"))
    }

    relocations.forEach { (from, to) -> relocate(from, to) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Jar> {
    from("../LICENSE.txt")
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.google.guava:guava:30.0-jre")
    testImplementation("dev.simplix:protocolize-api:2.4.2")
    testImplementation("org.spongepowered:configurate-yaml:4.1.2")
    testImplementation("org.spongepowered:configurate-hocon:4.1.2")
    // Runtime deps needed by unit tests that instantiate HoconMessages
    // (MiniMessage + LegacyMessage are compileOnly/libby at runtime).
    testImplementation("net.kyori:adventure-text-minimessage:4.14.0")
    testImplementation("com.github.kyngs:LegacyMessage:0.2.0")

    //API
    implementation(project(":API"))

    //Velocity
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-proxy:3.2.0-SNAPSHOT-277")

    // MySQL-compatible databases
    // MariaDB remains the default; the official MySQL Connector/J is also
    // downloaded so jdbc:mysql:// URLs use the correct driver automatically.
    libby("org.mariadb.jdbc:mariadb-java-client:3.5.10")
    libby("com.mysql:mysql-connector-j:9.3.0")
    libby("com.zaxxer:HikariCP:7.1.0")

    //SQLite
    libby("org.xerial:sqlite-jdbc:3.53.2.1")

    //PostgreSQL
    libby("org.postgresql:postgresql:42.7.13")

    //ACF
    libby("com.github.kyngs.commands:acf-velocity:7d5bf7cac0")
    libby("com.github.kyngs.commands:acf-paper:7d5bf7cac0")

    //Utils
    libby("com.github.ben-manes.caffeine:caffeine:3.2.0")
    // YAML is the active configuration format; HOCON remains for one-time legacy conversion.
    libby("org.spongepowered:configurate-yaml:4.1.2")
    libby("org.spongepowered:configurate-hocon:4.1.2")
    libby("at.favre.lib:bcrypt:0.10.2")
    libby("dev.samstevens.totp:totp:1.7.1")
    compileOnly("dev.simplix:protocolize-api:2.4.2")
    // PacketEvents is installed as an external Velocity/Paper plugin. Do not
    // shade or relocate its API: the platform plugin owns these classes.
    compileOnly("com.github.retrooper:packetevents-velocity:2.13.0")
    libby("org.bouncycastle:bcprov-jdk18on:1.80")
    libby("org.apache.commons:commons-email:1.6.0")
    libby("net.kyori:adventure-text-minimessage:4.14.0")
    libby("com.github.kyngs:LegacyMessage:0.2.0")

    //Geyser
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    //LuckPerms
    compileOnly("net.luckperms:api:5.4")

    // RedisBungee is used by the Velocity multi-proxy integration.
    compileOnly("com.github.ProxioDev.ValioBungee:RedisBungee-Bungee:0.12.5")

    //BStats
    libby("org.bstats:bstats-velocity:3.0.2")
    libby("org.bstats:bstats-bukkit:3.0.2")

    //Paper
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    //compileOnly "com.comphenix.protocol:ProtocolLib:5.1.0"
    // 2.13.0+ parses the new Paper 26.x version strings (e.g. '26.1.2.build.74')
    // and ships protocol mappings for the newest Minecraft lines. PacketEvents
    // is resolved from the Codemc repository listed below.
    libby("com.github.retrooper:packetevents-spigot:2.13.0")
    compileOnly("io.netty:netty-transport:4.1.108.Final")
    compileOnly("com.mojang:datafixerupper:5.0.28") //I hate this so much
    compileOnly("org.apache.logging.log4j:log4j-core:2.23.1")

    //Libby
    implementation("xyz.kyngs.libby:libby-bukkit:1.6.0")
    implementation("xyz.kyngs.libby:libby-velocity:1.6.0")
    implementation("xyz.kyngs.libby:libby-paper:1.6.0")

}

// Generates the libby.json resource consumed by LibraryManager.configureFromJSON()
// at runtime. Replaces the xyz.kyngs.librelogin.libby.plugin Gradle plugin whose
// repository (repo.kyngs.xyz/gradle-plugins) no longer exists.
val generateLibbyJson = tasks.register("generateLibbyJson") {
    inputs.files(libby)
    outputs.dir(generatedLibbyDir)

    doLast {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val libraries = libby.incoming.artifacts.resolvedArtifacts.get()
            .filter { it.id.componentIdentifier is ModuleComponentIdentifier }
            .filter { (it.id.componentIdentifier as ModuleComponentIdentifier).group !in libbyExcludedGroups }
            .sortedBy { artifact ->
                val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                "${id.group}:${id.module}:${id.version}"
            }
            .map { artifact ->
                val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                val entry = linkedMapOf(
                    "group" to id.group.replace(".", "{}"),
                    "name" to id.module,
                    "version" to id.version
                )
                if (id.group !in libbyNoChecksumGroups) {
                    entry["checksum"] = Base64.getEncoder().encodeToString(sha256.digest(artifact.file.readBytes()))
                }
                entry
            }

        val reloc = relocations.mapKeys { it.key.replace(".", "{}") }
            .mapValues { it.value.replace(".", "{}") }

        val json = linkedMapOf(
            "version" to 0,
            "libraries" to libraries,
            "repositories" to libbyRepositories,
            "relocations" to reloc
        )

        val output = generatedLibbyDir.get().file("libby.json").asFile
        output.parentFile.mkdirs()
        output.writeText(JsonOutput.toJson(json))
    }
}

tasks.test {
    useJUnitPlatform()
}

fun platformJar(name: String, excluded: List<String>) = tasks.register<Zip>(name) {
    dependsOn(tasks.named("shadowJar"))
    from(tasks.named<ShadowJar>("shadowJar").map { zipTree(it.archiveFile) }) {
        excluded.forEach { exclude(it) }
    }
    destinationDirectory.set(layout.buildDirectory.dir("libs/platform"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val paperJar = platformJar("paperJar", listOf(
    "xyz/kyngs/librelogin/velocity/**",
    "xyz/kyngs/librelogin/paper/tinyauth/**",
    "authlimbo-plugin.yml"
))
paperJar.configure {
    archiveFileName.set(providers.provider { "LibreLogin-Paper-${readVersionFromProperties()}.jar" })
}

val velocityJar = platformJar("velocityJar", listOf(
    "xyz/kyngs/librelogin/paper/**",
    "plugin.yml",
    "paper-plugin.yml",
    "authlimbo-plugin.yml"
))
velocityJar.configure {
    archiveFileName.set(providers.provider { "LibreLogin-Velocity-${readVersionFromProperties()}.jar" })
}

val authLimboJar = tasks.register<Jar>("authLimboJar") {
    dependsOn("compileJava")
    archiveFileName.set("AuthLimbo-1.0.0.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs/platform"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output) {
        include("xyz/kyngs/librelogin/paper/tinyauth/**")
    }
    from(layout.projectDirectory.file("src/main/resources/authlimbo-plugin.yml")) {
        rename { "plugin.yml" }
    }
}

tasks.register("platformJars") {
    dependsOn("paperJar", "velocityJar", authLimboJar)
}

// Creates the human-facing release bundle with one folder per installable
// plugin. The READMEs are kept in the repository's release/ directory so the
// archive can be regenerated without copying files by hand.
val releaseArchive = tasks.register<Zip>("releaseArchive") {
    dependsOn(paperJar, velocityJar, authLimboJar)
    archiveFileName.set(providers.provider { "LibreLogin-${readVersionFromProperties()}.zip" })
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(paperJar) {
        into("LibreLogin-Paper")
        rename { "LibreLogin-Paper-${readVersionFromProperties()}.jar" }
    }
    from(velocityJar) {
        into("LibreLogin-Velocity")
        rename { "LibreLogin-Velocity-${readVersionFromProperties()}.jar" }
    }
    from(authLimboJar) {
        into("AuthLimbo")
        rename { "AuthLimbo-1.0.0.jar" }
    }

    from(layout.projectDirectory.dir("../release/LibreLogin-Paper")) {
        into("LibreLogin-Paper")
    }
    from(layout.projectDirectory.dir("../release/LibreLogin-Velocity")) {
        into("LibreLogin-Velocity")
    }
    from(layout.projectDirectory.dir("../release/AuthLimbo")) {
        into("AuthLimbo")
    }
    from(layout.projectDirectory.file("../release/README.md"))
    from(layout.projectDirectory.file("../release/CHANGELOG.md"))
    // Preserve the upstream license in the distributable package. LibreLogin
    // is MPL-2.0; the MIT notice under licenses/ belongs to a dependency.
    from(layout.projectDirectory.file("../LICENSE"))
}

// Bump the version before anything that embeds it runs (template generation and
// resource expansion are prerequisites of the compile/jar chain, so this orders
// the bump before shadowJar and the platform jars).
tasks.named("generateVersionedSources") {
    dependsOn(rootProject.tasks.named("bumpVersion"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(rootProject.tasks.named("bumpVersion"))
    dependsOn(generateLibbyJson)
}

tasks.withType<ProcessResources> {
    outputs.upToDateWhen { false }
    filesMatching("plugin.yml") {
        // Read lazily at execution time so the freshly bumped version is used.
        expand(mapOf("version" to readVersionFromProperties()))
    }
    filesMatching("paper-plugin.yml") {
        expand(mapOf("version" to readVersionFromProperties()))
    }
}
