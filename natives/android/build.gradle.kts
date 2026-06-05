import dev.silenium.libs.mpv.build.AgpCopyCompat

plugins {
    id("mpv-base")
    alias(libs.plugins.android.library)
}

repositories {
    exclusiveContent {
        filter {
            includeGroup("mpv-android")
        }
        forRepository {
            ivy {
                url = uri("https://nexus.silenium.dev/repository/github-releases/")
                patternLayout {
                    artifact("[organisation]/[module]/releases/download/[revision]/debug-objs.[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
    }
    google()
    mavenCentral()
}

val nativeLibs by configurations.creating

dependencies {
    nativeLibs(project.dependencies.variantOf(libs.mpv.android) {
        artifactType("zip")
    })
    androidTestImplementation(libs.bundles.androidx.test)
}

val unzipNativeLibs = tasks.register<AgpCopyCompat>("unzipNativeLibs") {
    description = "Unzips native libraries from mpv-android debug-objs.zip"
    from(zipTree(nativeLibs.singleFile)) {
        exclude("**/libplayer.so")
    }
    destination.convention(layout.buildDirectory.dir("tmp/extracted-natives"))
}

androidComponents.onVariants {
    it.sources.jniLibs?.addGeneratedSourceDirectory(unzipNativeLibs, AgpCopyCompat::destination)
}

android {
    namespace = "dev.silenium.mpv.natives.android"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 31
    }
}
