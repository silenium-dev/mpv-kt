import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.jvm

plugins {
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
    dev.silenium.gradle.conventions.jvm
}

group = "dev.silenium.compose.mpv.examples.desktop"

dependencies {
    implementation(project(":compose:examples:simple:shared"))
    implementation(project(":mpv-kt:natives:desktop", "natives-linux-x86_64"))
    implementation(project(":mpv-kt:natives:desktop", "natives-linux-arm64"))
    implementation(project(":mpv-kt:natives:desktop", "natives-windows-x86_64"))
    implementation(project(":mpv-kt:natives:desktop", "natives-windows-arm64"))
    implementation(libs.compose.desktop)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(compose.desktop.currentOs)
}

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}
