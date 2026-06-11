plugins {
    alias(libs.plugins.kotlin.compose)
    id("mpv-base")
    id("mpv-android-app")
}

group = "dev.silenium.libs.mpv.compose.examples"

dependencies {
    implementation(project(":compose:examples:simple:shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
    runtimeOnly(libs.slf4j.android)
}

android {
    namespace = "dev.silenium.libs.mpv.compose.examples.android"
}
