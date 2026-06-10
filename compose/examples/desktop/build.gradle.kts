plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    id("mpv-base")
}

group = "dev.silenium.compose.mpv.examples"

dependencies {
    implementation(project(":compose"))
    implementation(libs.logback.classic)
    implementation(libs.compose.desktop)
    runtimeOnly(compose.desktop.currentOs)
}
