import org.gradle.kotlin.dsl.implementation

plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    google()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
}

dependencies {
    operator fun NamedDomainObjectProvider<Configuration>.invoke(plugin: Provider<PluginDependency>): ExternalModuleDependency {
        val plugin = plugin.get()
        return this("${plugin.pluginId}:${plugin.pluginId}.gradle.plugin") {
            version {
                plugin.version.preferredVersion.takeIf { it.isNotBlank() }?.let {
                    prefer(it)
                }
                plugin.version.requiredVersion.takeIf { it.isNotBlank() }?.let {
                    require(it)
                }
                plugin.version.strictVersion.takeIf { it.isNotBlank() }?.let {
                    strictly(it)
                }
                plugin.version.rejectedVersions.takeIf { it.isNotEmpty() }?.let {
                    reject(*it.toTypedArray())
                }
                branch = plugin.version.branch
            }
        }
    }
    configurations.implementation(plugin = libs.plugins.kotlin.multiplatform)
    configurations.implementation(plugin = libs.plugins.kotlin.atomicfu)
    configurations.implementation(plugin = libs.plugins.android.kotlin)
    configurations.implementation(plugin = libs.plugins.nix.natives)
    configurations.implementation(plugin = libs.plugins.kotest)
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(files(libs.javaClass.protectionDomain.codeSource.location.file))
}
