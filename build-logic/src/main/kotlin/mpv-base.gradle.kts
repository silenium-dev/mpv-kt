import dev.silenium.libs.jni.nixJavaLauncher
import dev.silenium.libs.mpv.build.ProjectPlugin

plugins {
    `maven-publish`
    base
}

apply<ProjectPlugin>()

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

group = "dev.silenium.libs.mpv"
val gitVersionProvider = providers.gradleProperty("ci").flatMap {
    if (it.toBoolean()) {
        providers.exec {
            commandLine("git", "describe", "--tags", "--always", "--dirty", "--abbrev=8")
            workingDir = layout.projectDirectory.asFile
        }.standardOutput.asText.map(String::trim)
    } else null
}
version = providers
    .gradleProperty("deploy.version")
    .orElse(gitVersionProvider)
    .orElse("0.0.0-SNAPSHOT")
    .get()

tasks.withType<Test> {
    javaLauncher = nixJavaLauncher(rootProject.layout.projectDirectory)
    environment("EGL_PLATFORM", "surfaceless")
    environment("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe")
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
