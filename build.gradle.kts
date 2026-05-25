import dev.silenium.libs.jni.nixJavaLauncher
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.atomicfu") version "2.3.21"
    id("dev.silenium.libs.jni.nix-natives") version "0.5.1" apply false
    `maven-publish`
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

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

    repositories {
        mavenCentral()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
    }

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
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:atomicfu:0.32.1")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
    compilerOptions.jvmTarget = JvmTarget.JVM_25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    test {
        useJUnitPlatform()
        javaLauncher = nixJavaLauncher()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

    withType<Jar> {
        manifest {
            from("src/main/resources/META-INF/MANIFEST.MF")
        }
    }
}
