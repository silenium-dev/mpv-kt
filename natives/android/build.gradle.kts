import dev.silenium.libs.mpv.build.BundleAndroidNativesTask

plugins {
    id("mpv-base")
    id("mpv-android-lib")
}

group = "dev.silenium.libs.mpv.natives"

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
}

val nativeLibs by configurations.creating

dependencies {
    nativeLibs(project.dependencies.variantOf(libs.mpv.android) {
        artifactType("zip")
    })
    androidTestImplementation(libs.panama.android.core)
    androidTestImplementation(libs.bundles.androidx.test)
}

val bundleNatives = tasks.register<BundleAndroidNativesTask>("bundleNatives") {
    description = "Bundles all mpv native libraries for Android"
    nativeLibsZip.from(nativeLibs)
    ndkDirectory = androidComponents.sdkComponents.ndkDirectory
    destination.convention(layout.buildDirectory.dir("generated/jniLibs"))
}

androidComponents.onVariants {
    it.sources.jniLibs?.addGeneratedSourceDirectory(
        bundleNatives,
        BundleAndroidNativesTask::destination
    )
}

android {
    namespace = "dev.silenium.mpv.natives.android"
}
