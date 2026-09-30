plugins {
    id("java-library")
    id("maven-publish")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    api("javax.annotation:javax.annotation-api:1.3.2")
    api("net.kyori:adventure-api:4.14.0")
    compileOnlyApi("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.guava:guava:30.0-jre")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

// Publishing the API artifact is opt-in and never targets the upstream
// maintainer's infrastructure. The repository used to be hardcoded to
// repo.kyngs.xyz together with credentials this fork does not own, so a fork
// that enabled it would have written its own artifact into a third party's
// Maven repository. Pass -PpublishRepositoryUrl=<url> to publish into your own
// repository; the credentials are then taken from the releaseUsername /
// releasePassword project properties (or the matching ORG_GRADLE_PROJECT_*
// environment variables).
publishing {
    repositories {
        val publishUrl = providers.gradleProperty("publishRepositoryUrl").orNull

        if (publishUrl != null) {
            maven {
                name = "release"
                url = uri(publishUrl)
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
