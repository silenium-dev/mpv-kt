import dev.silenium.libs.jni.nixJavaLauncher

plugins {
    `maven-publish`
    base
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false

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

publishing {
    repositories {
        if (deployEnabled) {
            val url = findProperty("deploy.repo-url") as? String ?: error("No deploy.repo-url specified")
            maven(url) {
                name = "nexus"
                credentials {
                    username = findProperty("deploy.username") as? String ?: ""
                    password = findProperty("deploy.password") as? String ?: ""
                }
            }
        }
    }
}

tasks.withType<Test> {
    javaLauncher = nixJavaLauncher(rootProject.layout.projectDirectory)
    environment("EGL_PLATFORM", "surfaceless")
    environment("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe")
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
