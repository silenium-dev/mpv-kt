import dev.silenium.libs.jni.nixJavaLauncher
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    com.android.kotlin.multiplatform.library
    org.jetbrains.kotlin.multiplatform
    org.jetbrains.kotlin.plugin.atomicfu
}

repositories {
    mavenCentral()
    google()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

configure<KotlinMultiplatformExtension> {
    jvmToolchain(25)
    jvm()

    android {
        namespace = "dev.silenium.libs.mpv"
        compileSdk {
            version = release(36)
        }
        minSdk = 31
        withHostTest {
            isIncludeAndroidResources = true
            enableCoverage = true
        }
    }
}

tasks.withType<Test> {
    javaLauncher = nixJavaLauncher()
    environment("EGL_PLATFORM", "surfaceless")
    environment("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe")
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
