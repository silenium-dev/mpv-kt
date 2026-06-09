plugins {
    alias(libs.plugins.kotlin.compose)
    id("mpv-base")
    id("mpv-android-app")
}

group = "dev.silenium.compose.mpv.examples"

dependencies {
    implementation(project(":compose"))
    implementation(project(":mpv-kt"))
    implementation(project(":ffm"))
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}

android {
    namespace = "dev.silenium.compose.mpv.examples.android"
}
