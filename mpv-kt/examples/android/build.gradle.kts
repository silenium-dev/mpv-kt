import dev.silenium.gradle.conventions.*
import dev.silenium.build.ProjectConfig

plugins {
    org.jetbrains.kotlin.plugin.compose
    dev.silenium.gradle.conventions.android.application
}

group = "dev.silenium.libs.mpv.examples"

dependencies {
    implementation(project(":mpv-kt"))
    implementation(project(":ffm"))
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}

conventions {
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.mpv.examples.android"
    }
}

android {
    externalNativeBuild {
        cmake {
            version = ProjectConfig.CMAKE_VERSION
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}
