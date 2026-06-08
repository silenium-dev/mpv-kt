import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.mpv.build.AgpCopyCompat

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

val unzipNativeLibs = tasks.register<AgpCopyCompat>("unzipNativeLibs") {
    description = "Unzips native libraries from mpv-android debug-objs.zip"
    from(zipTree(nativeLibs.singleFile))
    destination.convention(layout.buildDirectory.dir("tmp/extracted-natives"))
}

val bundleLibcxx = tasks.register<AgpCopyCompat>("bundleLibcxx") {
    description = "Bundles libc++ with the app"
    dependsOn(unzipNativeLibs)
    val hostTag = NativePlatform.platform().osArch
    val abiMap = mapOf(
        "aarch64-linux-android" to "arm64-v8a",
        "arm-linux-androideabi" to "armeabi-v7a",
        "i686-linux-android" to "x86",
        "x86_64-linux-android" to "x86_64",
    )
    val ndkDir = androidComponents.sdkComponents.ndkDirectory
    val sysrootLibDir = ndkDir.map { it.dir("toolchains/llvm/prebuilt/$hostTag/sysroot/usr/lib") }
    abiMap.forEach { (triplet, abi) ->
        val source = sysrootLibDir.map { sysrootLib ->
            sysrootLib
                .dir(triplet)
                .file("libc++_shared.so")
        }
        from(source) {
            into(abi)
        }
    }
    from(unzipNativeLibs) {
        include(
            "*/libavcodec.so",
            "*/libavdevice.so",
            "*/libavfilter.so",
            "*/libavformat.so",
            "*/libavutil.so",
            "*/libc++_shared.so",
            "*/libmpv.so",
            "*/libswresample.so",
            "*/libswscale.so",
        )
    }

    destination.convention(layout.buildDirectory.dir("tmp/bundled-natives"))
}

androidComponents.onVariants {
    it.sources.jniLibs?.addGeneratedSourceDirectory(bundleLibcxx, AgpCopyCompat::destination)
}

android {
    namespace = "dev.silenium.mpv.natives.android"
}
