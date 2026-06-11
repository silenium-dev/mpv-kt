plugins {
    id("mpv-base")
    id("mpv-kmp")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

group = "dev.silenium.libs.mpv.compose.examples"

kotlin {
    android {
        namespace = "dev.silenium.libs.mpv.compose.examples"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.bundles.compose)
                implementation(libs.compose.material)
                api(project(":compose"))
            }
        }
    }
}
