import dev.silenium.build.Arch
import dev.silenium.build.ProjectConfig
import dev.silenium.build.gradleArch
import dev.silenium.build.gradleOs
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing
import dev.silenium.libs.jni.nixEval
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.w3c.dom.Element

plugins {
    dev.silenium.gradle.conventions.jvm
    dev.silenium.libs.jni.`nix-natives` apply false
}

group = "dev.silenium.libs.mpv.natives.desktop"

val architectures = nixEval {
    expression = ".#lib.architectures"
    workingDir = parent!!.projectDir
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

dependencies {
    architectures.forEach { arch ->
        runtimeOnly(project(parent!!.path)) {
            attributes {
                attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, gradleOs(arch))
                attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, gradleArch(arch) ?: return@attributes)
            }
        }
    }
}

afterEvaluate {
    val desktop = project(parent!!.path)
    publishing.publications.named<MavenPublication>("main") {
        suppressPomMetadataWarningsFor("runtimeElements")
        pom.withXml {
            val root = asElement()
            val doc = root.ownerDocument

            fun Element.childElements(name: String): List<Element> =
                (0 until childNodes.length)
                    .map { childNodes.item(it) }
                    .filterIsInstance<Element>()
                    .filter { it.localName == name || it.nodeName == name }

            fun Element.text(name: String) = childElements(name).firstOrNull()?.textContent

            fun Element.add(name: String, value: String) {
                appendChild(doc.createElement(name).also { it.textContent = value })
            }

            val dependencies = root.childElements("dependencies").firstOrNull()
                ?: doc.createElement("dependencies").also { root.appendChild(it) }

            // drop the plain (classifier-less) entries Gradle generated for the attribute-based dependencies
            dependencies.childElements("dependency")
                .filter {
                    it.text("groupId") == desktop.group && it.text("artifactId") == desktop.name
                }
                .forEach(dependencies::removeChild)

            architectures.forEach { arch ->
                val dependency = doc.createElement("dependency")
                dependency.add("groupId", desktop.group.toString())
                dependency.add("artifactId", desktop.name)
                dependency.add("version", desktop.version.toString())
                dependency.add("classifier", arch.jni)
                dependency.add("scope", "runtime")
                dependencies.appendChild(dependency)
            }
        }
    }
}
