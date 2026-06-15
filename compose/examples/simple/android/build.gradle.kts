import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.build.ProjectConfig

plugins {
    org.jetbrains.kotlin.plugin.compose
    dev.silenium.gradle.conventions.android.application
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

conventions {
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.libs.mpv.compose.examples.android"
    }
}
