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

include(":lib", ":natives:desktop", ":natives:android")
includeBuild("build-logic")
rootProject.name = "mpv-kt"
