package dev.silenium.build

import dev.silenium.libs.jni.Platform
import kotlinx.serialization.json.*
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily

data class Arch(val native: String, val jni: String) {
    val platform = Platform(jni)
    constructor(elem: JsonElement) : this(
        native = elem.jsonObject["native"]!!.jsonPrimitive.content,
        jni = elem.jsonObject["jni"]!!.jsonPrimitive.content,
    )
}

fun Project.gradleArch(arch: Arch): MachineArchitecture? = when (arch.platform.arch) {
    Platform.Arch.X86 -> project.objects.named(MachineArchitecture.X86)
    Platform.Arch.X86_64 -> project.objects.named(MachineArchitecture.X86_64)
    Platform.Arch.ARM64 -> project.objects.named(MachineArchitecture.ARM64)
    else -> {
        project.logger.warn("unsupported arch: ${arch.platform.arch}")
        null
    }
}

fun Project.gradleOs(arch: Arch): OperatingSystemFamily = when (arch.platform.os) {
    Platform.OS.WINDOWS -> objects.named(OperatingSystemFamily.WINDOWS)
    Platform.OS.LINUX -> objects.named(OperatingSystemFamily.LINUX)
    Platform.OS.DARWIN -> objects.named(OperatingSystemFamily.MACOS)
}
