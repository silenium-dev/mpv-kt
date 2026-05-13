pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
