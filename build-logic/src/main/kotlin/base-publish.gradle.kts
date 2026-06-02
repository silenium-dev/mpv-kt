plugins {
    `maven-publish`
    base
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
