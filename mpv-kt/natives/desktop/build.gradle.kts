import dev.silenium.build.Arch
import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.jvm.JvmPlugin
import dev.silenium.gradle.conventions.publishing
import dev.silenium.libs.jni.NixBuildTask
import dev.silenium.libs.jni.NixNativesExtension
import dev.silenium.libs.jni.NixNativesPlugin
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

val flakeInputs = files("flake.nix", "flake.lock", "src", "meson.build", "subprojects.tpl")

subprojects {
    apply<JvmPlugin>()
    apply<NixNativesPlugin>()

    val arch = architectures.first { it.jni == this.name }
    group = "${parent!!.group}.desktop"

    conventions {
        publishing {
            enabled = true
        }
        jvm {
            jvmTarget = ProjectConfig.JVM_TARGET
        }
    }

    configure<NixNativesExtension> {
        flakeDir = parent!!.projectDir
        libName = "${rootProject.name}-${arch.native}"
        sourceFiles = parent!!.layout.projectDirectory.files("src", "meson.build", "meson.options", "subprojects.tpl")
        processResources = false
        showLogs = true
    }

    tasks {
        processResources {
            from(named<NixBuildTask>("nixBuild")) {
                into("natives/${arch.jni}")
            }
            dirPermissions { unix("755") }
        }
    }
}

dependencies {
    architectures.forEach { arch ->
        implementation(project(":mpv-kt:natives:desktop:${arch.jni}"))
    }
}

conventions {
    publishing {
        enabled = true
    }
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}
