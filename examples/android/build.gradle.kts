plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("mpv-base")
}

group = "dev.silenium.libs.mpv.examples"

dependencies {
    implementation(project(":lib"))
    implementation(project(":ffm"))
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}

android {
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = ProjectConfig.MIN_SDK
    }
    packaging {
        resources.pickFirsts += "META-INF/*"
    }
    ndkVersion = "29.0.14206865"
    namespace = "dev.silenium.mpv.examples.android"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}
