import dev.silenium.gradle.conventions.*
import dev.silenium.build.ProjectConfig

plugins {
    dev.silenium.gradle.conventions.kmp
}

group = "dev.silenium.libs.mpv"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.atomicfu)
                api(libs.kotlinx.coroutines.core)
                implementation(kotlin("reflect"))
                implementation(libs.slf4j.api)
                implementation(libs.jetbrains.annotations)
                api(project(":ffm"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.slf4j.api)
                implementation(libs.bundles.tests)
            }
        }
        androidMain {
            dependencies {
                implementation(project(":mpv-kt:natives:android"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.jni.utils)
                implementation(project(":mpv-kt:natives:desktop"))
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.logback.classic)
                implementation(project.dependencies.platform(libs.lwjgl.bom))
                implementation(libs.bundles.lwjgl)
                implementation(libs.lwjgl.egl)
                libs.bundles.lwjgl.get().forEach {
                    runtimeOnly(project.dependencies.variantOf(provider { it }) { classifier("natives-linux") })
                }
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

        namespace = "dev.silenium.libs.mpv"
    }
}
