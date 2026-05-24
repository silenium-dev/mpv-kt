import dev.silenium.libs.jni.nixJavaLauncher
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    id("dev.silenium.libs.jni.nix-natives") version "0.5.1" apply false
}

repositories {
    mavenCentral()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
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

val templateSrc = layout.projectDirectory.dir("src/main/templates")
val templateDst = layout.buildDirectory.dir("generated/templates")
val templateProps = mapOf(
    "LIBRARY_NAME" to rootProject.name,
)
tasks {
    val generateTemplates = register<Copy>("generateTemplates") {
        description = "generate BuildConstants"
        from(templateSrc)
        into(templateDst)
        expand(templateProps)

        inputs.dir(templateSrc)
        inputs.properties(templateProps)
        outputs.dir(templateDst)
    }

    test {
        useJUnitPlatform()
        javaLauncher = nixJavaLauncher()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

    withType<Jar> {
        dependsOn(generateTemplates)
        manifest {
            from("src/main/resources/META-INF/MANIFEST.MF")
        }
    }

    compileKotlin {
        dependsOn(generateTemplates)
    }
}
sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}
