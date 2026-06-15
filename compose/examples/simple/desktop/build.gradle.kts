import dev.silenium.gradle.conventions.jvm
import dev.silenium.build.ProjectConfig

plugins {
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
    dev.silenium.gradle.conventions.jvm
}

group = "dev.silenium.libs.mpv.compose.examples"

dependencies {
    implementation(project(":compose:examples:simple:shared"))
    implementation(libs.compose.desktop)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(compose.desktop.currentOs)
}

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}
