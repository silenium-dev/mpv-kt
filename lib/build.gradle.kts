plugins {
    id("base-kotlin")
    id("base-publish")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.atomicfu)
                api(libs.kotlinx.coroutines.core)
                api(libs.slf4j.api)
                api(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
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
