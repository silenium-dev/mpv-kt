plugins {
    id("mpv-base")
    id("mpv-kmp")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    android {
        namespace = "dev.silenium.compose.mpv"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":mpv-kt"))
                implementation(libs.bundles.compose)
                implementation(libs.compose.gl)
            }
        }
    }
}

mpvBase {
    publish = true
}
