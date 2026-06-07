import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    com.android.kotlin.multiplatform.library
    org.jetbrains.kotlin.multiplatform
    org.jetbrains.kotlin.plugin.atomicfu
    io.kotest
}

configure<KotlinMultiplatformExtension> {
    jvmToolchain(ProjectConfig.JVM_TARGET.target.toInt())
    jvm {
        compilerOptions.jvmTarget = ProjectConfig.JVM_TARGET
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        compileSdk?.let {
            buildToolsVersion = it.toString()
        }
        minSdk = ProjectConfig.MIN_SDK
        compilerOptions {
            jvmTarget = ProjectConfig.ANDROID_JVM_TARGET
        }
        withHostTest {
            enableCoverage = true
        }
        withDeviceTest {
            enableCoverage = true
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            instrumentationRunnerArguments["notPackage"] = "com.v7878"
        }
        packaging {
            resources.pickFirsts += "META-INF/*"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
