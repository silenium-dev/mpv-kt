import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ProjectConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    val JVM_TARGET = JvmTarget.JVM_25
    val ANDROID_JVM_TARGET = JvmTarget.JVM_11
}
