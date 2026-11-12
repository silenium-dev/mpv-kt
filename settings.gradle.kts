import dev.silenium.libs.jni.nixEval
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.kotlin.dsl.support.serviceOf

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        google()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
        maven("https://nexus.silenium.dev/repository/maven-snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.silenium.libs.jni.nix-natives") version "0.6.5-22-g876e7ef2" apply false
}

include(
    ":compose",
    ":compose:examples:simple",
    ":compose:examples:simple:android",
    ":compose:examples:simple:desktop",
    ":compose:examples:simple:shared",
    ":mpv-kt",
    ":mpv-kt:natives:desktop",
    ":mpv-kt:natives:android",
    ":mpv-kt:android-tests",
    ":mpv-kt:examples:android",
)
rootProject.name = "mpv-kt"

data class Arch(val native: String, val jni: String) {
    val platform = dev.silenium.libs.jni.Platform(jni)

    constructor(elem: JsonElement) : this(
        native = elem.jsonObject["native"]!!.jsonPrimitive.content,
        jni = elem.jsonObject["jni"]!!.jsonPrimitive.content,
    )
}

val desktopArchs = nixEval(
    objects = serviceOf(),
    providers = serviceOf(),
    projectDir = layout.settingsDirectory.dir("mpv-kt/natives/desktop"),
) {
    expression = ".#lib.architectures"
}.get().jsonArray.map(::Arch)

desktopArchs.forEach { arch ->
    val path = ":mpv-kt:natives:desktop:${arch.jni}"
    include(path)
    project(path).projectDir = file(".gradle/generated-projects/desktop-${arch.jni}").also(File::mkdirs)
}
