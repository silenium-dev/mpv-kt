plugins {
    id("mpv-base")
    id("mpv-kmp")
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
            }
        }
        androidMain {
            dependencies {
                implementation(project(":natives:android"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.jni.utils)
                implementation(project(":natives:desktop"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
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
