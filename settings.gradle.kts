pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        google()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":ffm",
    ":compose",
    ":compose:examples:simple",
    ":compose:examples:simple:android",
    ":compose:examples:simple:desktop",
    ":compose:examples:simple:shared",
    ":mpv-kt",
    ":mpv-kt:natives:desktop",
    ":mpv-kt:natives:android",
    ":mpv-kt:android-tests",
    ":mpv-kt:examples:android",
)
includeBuild("build-logic")
rootProject.name = "mpv-kt"
