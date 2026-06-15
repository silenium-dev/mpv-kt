package dev.silenium.build

import dev.silenium.gradle.conventions.AndroidExtension
import dev.silenium.gradle.conventions.deviceTests
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ProjectConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val NDK_VERSION = "29.0.14206865"
    const val CMAKE_VERSION = "4.1.2"
    val JVM_TARGET = JvmTarget.JVM_25
    val ANDROID_JVM_TARGET = JvmTarget.JVM_11
}

fun AndroidExtension.configureDeviceTests() {
    deviceTests {
        enabled.set(true)
        instrumentationExcludes.add("com.v7878")
    }
}
