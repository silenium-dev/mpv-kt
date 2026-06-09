package dev.silenium.libs.mpv.build

import gradle.kotlin.dsl.accessors._e3e75d9eed3de87ba322431308ccbb6d.publishing
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven

abstract class ProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val mpvBase = target.extensions.create<MpvBaseExtension>("mpvBase")
        target.afterEvaluate {
            if (mpvBase.publish.getOrElse(false)) {
                val deployEnabled =
                    target.findProperty("deploy.enabled")
                        .let { (it as String?)?.toBoolean() ?: false }
                target.publishing {
                    repositories {
                        if (deployEnabled) {
                            val url = findProperty("deploy.repo-url") as? String
                                ?: error("No deploy.repo-url specified")
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
        }
    }
}
