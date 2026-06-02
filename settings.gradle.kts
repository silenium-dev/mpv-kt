pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        google()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":lib")
includeBuild("build-logic")
