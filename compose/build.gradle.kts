import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.jvm
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.publishing

plugins {
    org.jetbrains.kotlin.plugin.compose
    dev.silenium.gradle.conventions.kmp
}

kotlin {
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

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
    publishing {
        enabled = true
    }
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.compose.mpv"
    }
}
