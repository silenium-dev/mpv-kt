import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    com.android.kotlin.multiplatform.library
    org.jetbrains.kotlin.multiplatform
    org.jetbrains.kotlin.plugin.atomicfu
}

configure<KotlinMultiplatformExtension> {
    jvmToolchain(25)
    jvm()

    android {
        namespace = "dev.silenium.libs.mpv"
        compileSdk {
            version = release(37)
        }
        compileSdk?.let {
            buildToolsVersion = it.toString()
        }
        minSdk = 31
        withHostTest {
            isIncludeAndroidResources = true
            enableCoverage = true
        }
    }
}
