plugins {
    id("mpv-base")
    id("mpv-kmp")
}

kotlin {
    android {
        namespace = "dev.silenium.libs.foreign"
    }
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
        androidHostTest {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
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
