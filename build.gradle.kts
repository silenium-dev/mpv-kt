import dev.silenium.libs.jni.nixJavaLauncher
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.nix.natives) apply false
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
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)

    testImplementation(kotlin("test"))
    testImplementation(libs.logback.classic)
    testImplementation(platform(libs.lwjgl.bom))
    testImplementation(libs.bundles.lwjgl)
    testImplementation(libs.lwjgl.egl)
    libs.bundles.lwjgl.get().forEach {
        testRuntimeOnly(variantOf(provider { it }) { classifier("natives-linux") })
    }
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
        environment("EGL_PLATFORM", "surfaceless")
        environment("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe")
        environment("LIBGL_ALWAYS_SOFTWARE", "1")
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

    withType<Jar> {
        manifest {
            from("src/main/resources/META-INF/MANIFEST.MF")
        }
    }
}
