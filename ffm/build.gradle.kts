import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.jvm
import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.publishing

plugins {
    dev.silenium.gradle.conventions.kmp
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.atomicfu)
                api(libs.kotlinx.coroutines.core)
                implementation(kotlin("reflect"))
                implementation(libs.slf4j.api)
                implementation(libs.jetbrains.annotations)
                implementation(libs.jni.utils)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                implementation(libs.panama.android.core)
            }
        }
//        androidHostTest {
//            dependencies {
//                implementation(libs.logback.classic)
//            }
//        }
        jvmMain {
            dependencies {
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.logback.classic)
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

        namespace = "dev.silenium.libs.foreign"
    }
}
