import dev.silenium.build.ProjectConfig
import dev.silenium.build.configureDeviceTests
import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.publishing

plugins {
    dev.silenium.gradle.conventions.android.library
}

group = "dev.silenium.libs.mpv.natives"

repositories {
    exclusiveContent {
        filter {
            includeGroup("mpv-android")
        }
        forRepository {
            ivy {
                url = uri("https://nexus.silenium.dev/repository/github-releases/")
                patternLayout {
                    artifact("[organisation]/[module]/releases/download/[revision]/debug-objs.[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
    }
}

val nativeLibs by configurations.creating

dependencies {
    nativeLibs(project.dependencies.variantOf(libs.mpv.android) {
        artifactType("zip")
    })
    androidTestImplementation(libs.panama.android.core)
    androidTestImplementation(libs.bundles.androidx.test)
}

conventions {
    publishing {
        enabled = true
    }

    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET
        ndkVersion = ProjectConfig.NDK_VERSION
        cmakeVersion = ProjectConfig.CMAKE_VERSION

        namespace = "dev.silenium.mpv.natives.android"
        configureDeviceTests()
    }
}
