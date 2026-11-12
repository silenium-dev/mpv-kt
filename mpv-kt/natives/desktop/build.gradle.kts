import dev.silenium.build.Arch
import dev.silenium.build.ProjectConfig
import dev.silenium.build.gradleArch
import dev.silenium.build.gradleOs
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing
import dev.silenium.libs.jni.NixBuildTask
import dev.silenium.libs.jni.Platform
import dev.silenium.libs.jni.nixEval
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

plugins {
    dev.silenium.gradle.conventions.jvm
    dev.silenium.libs.jni.`nix-natives` apply false
}

group = "dev.silenium.libs.mpv.natives"

val architectures = nixEval {
    expression = ".#lib.architectures"
    workingDir = projectDir
}.get().let {
    Json.decodeFromJsonElement<List<JsonElement>>(it).map(::Arch)
}

conventions {
    publishing {
        enabled = true
    }
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}

val compressPlatforms = architectures.associateWith { architecture ->
    val buildOutput = layout.buildDirectory.dir("nix-result/${architecture.jni}")
    val build = tasks.register<NixBuildTask>("nixBuild${architecture.jni}") {
        doFirst {
            buildOutput.get().asFile.deleteRecursively()
        }

        description = "build the native artifact for ${architecture.jni}"
        libName = "${rootProject.name}-${architecture.native}"
        inputs.files("flake.nix", "src", "meson.build", "subprojects.tpl")
        resultDir = buildOutput
        showLogs = providers.environmentVariable("CI").orElse("false").map { it != "false" }
    }

    tasks.register<Jar>("compressPlatform${architecture.jni}") {
        description = "compress the native artifact for ${architecture.jni}"
        from(build) {
            into("natives/${architecture.jni}")
        }
        archiveClassifier = architecture.jni
        outputs.cacheIf { true }
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants

compressPlatforms.forEach { (arch, task) ->
    val natives = configurations.consumable("natives-${arch.jni}") {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, gradleOs(arch))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, gradleArch(arch) ?: return@attributes)
        }
        outgoing.artifact(task)
    }
    javaComponent.addVariantsFromConfiguration(natives.get()) { mapToOptional() }
}
