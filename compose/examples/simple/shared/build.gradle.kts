import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.jvm
import dev.silenium.build.ProjectConfig

plugins {
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
    dev.silenium.gradle.conventions.kmp
}

group = "dev.silenium.libs.mpv.compose.examples"

kotlin {
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

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }

    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.libs.mpv.compose.examples"
    }
}
