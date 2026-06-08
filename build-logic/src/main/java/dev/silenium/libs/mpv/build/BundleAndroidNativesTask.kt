package dev.silenium.libs.mpv.build

import dev.silenium.libs.jni.NativePlatform
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class BundleAndroidNativesTask : DefaultTask() {
    @get:Inject
    abstract val fsOps: FileSystemOperations

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    @get:Classpath
    abstract val nativeLibsZip: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ndkDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @TaskAction
    fun bundle() {
        val outputDir = destination.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val tempDir = temporaryDir.resolve("extracted")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        fsOps.copy {
            from(archiveOps.zipTree(nativeLibsZip.singleFile))
            into(tempDir)
        }

        val hostTag = NativePlatform.platform().osArch
        val abiMap = mapOf(
            "aarch64-linux-android" to "arm64-v8a",
            "arm-linux-androideabi" to "armeabi-v7a",
            "i686-linux-android" to "x86",
            "x86_64-linux-android" to "x86_64",
        )
        val sysrootLibDir =
            ndkDirectory.get().dir("toolchains/llvm/prebuilt/$hostTag/sysroot/usr/lib")

        abiMap.forEach { (triplet, abi) ->
            val source = sysrootLibDir
                .dir(triplet)
                .file("libc++_shared.so")
                .asFile
            if (source.isFile) {
                source.copyTo(outputDir.resolve("$abi/libc++_shared.so"), overwrite = true)
            } else {
                logger.warn("libc++_shared.so not found for $triplet")
            }
        }

        fsOps.copy {
            from(tempDir) {
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
            into(outputDir)
        }
    }
}
