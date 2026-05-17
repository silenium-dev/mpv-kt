plugins {
    kotlin("jvm") version "2.3.21"
    id("dev.silenium.libs.jni.nix-natives") version "0.4.1"
}

repositories {
    mavenCentral()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

dependencies {
    implementation("dev.silenium.libs.jni:jni-utils:0.4.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

nixNatives {
    libName = "mpv-jni-rs"
    libVersion = "0.1.0"
    nixFlake = file("flake.nix")
    nixFlakeLock = file("flake.lock")
    showLogs = true
    sourceFiles.from(
        file("natives/Cargo.toml"),
        file("natives/Cargo.lock"),
        file("natives/src"),
    )
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
    }

    withType<Jar> {
        dependsOn(generateTemplates)
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
