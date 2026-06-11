plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    id("mpv-base")
}

group = "dev.silenium.libs.mpv.compose.examples"

dependencies {
    implementation(project(":compose:examples:simple:shared"))
    implementation(libs.compose.desktop)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(compose.desktop.currentOs)
}
