import dev.silenium.gradle.conventions.*
import dev.silenium.build.ProjectConfig

plugins {
    dev.silenium.gradle.conventions.android.library
}

dependencies {
    implementation(project(":mpv-kt"))
    implementation(project(":ffm"))
}

conventions {
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        namespace = "dev.silenium.libs.mpv.android.tests"
    }
}

android {
    externalNativeBuild {
        cmake {
            version = ProjectConfig.CMAKE_VERSION
            path = file("src/androidTest/cpp/CMakeLists.txt")
        }
    }
}
