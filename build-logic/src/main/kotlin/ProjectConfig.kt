import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import dev.silenium.libs.mpv.build.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ProjectConfig {
    const val COMPILE_SDK = 37
    const val NDK_VERSION = "29.0.14206865"
    const val MIN_SDK = 26
    val JVM_TARGET = JvmTarget.JVM_25
    val ANDROID_JVM_TARGET = JvmTarget.JVM_11
}

fun CommonExtension.commonConfig() {
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK)
    }
    ndkVersion = ProjectConfig.NDK_VERSION
    buildToolsVersion = ProjectConfig.COMPILE_SDK.toString()

    defaultConfig.minSdk = ProjectConfig.MIN_SDK
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    defaultConfig.testInstrumentationRunnerArguments["notPackage"] = "com.v7878"

    compileOptions.targetCompatibility =
        JavaVersion.toVersion(ProjectConfig.ANDROID_JVM_TARGET.target)
    compileOptions.sourceCompatibility =
        JavaVersion.toVersion(ProjectConfig.ANDROID_JVM_TARGET.target)

    packaging.resources.pickFirsts += "META-INF/*"

    val pixel =
        testOptions.managedDevices.allDevices.create<ManagedVirtualDevice>("pixel10X86_64") {
        device = "Pixel 10 Pro"
        sdkVersion = 30
        testedAbi = "x86_64"
        require64Bit = true
        systemImageSource = "aosp-atd"
    }
    testOptions.managedDevices.groups.create("utp") {
        targetDevices.add(pixel)
    }
}

fun Project.androidTestDependencies() {
    val androidTestImplementation =
        configurations.getByName<Configuration>("androidTestImplementation")
    dependencies {
        androidTestImplementation(libs.slf4j.android)
        androidTestImplementation(libs.bundles.androidx.test)
        androidTestImplementation(libs.bundles.tests)
        androidTestImplementation(libs.kotest.runner.junit4)
    }
}
