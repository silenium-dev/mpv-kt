import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.exec
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2025.11"

project {
    features {
        untrustedBuildsSettings {
            id = "PROJECT_EXT_9"
            defaultAction = UntrustedBuildsSettings.DefaultAction.APPROVE
            enableLog = true
            approvalRules = "(groups:MAINTAINERS):1"
            manualRunsApproved = true
        }
    }

    buildType(NixBuild)
}

object NixBuild : BuildType({
    name = "Nix Build"

    vcs {
        root(DslContext.settingsRoot)
        branchFilter = """
            |+:*
        """.trimMargin()
    }
    triggers {
        vcs {
            branchFilter = """
                |+:*
            """.trimMargin()
        }
    }

    features {
        perfmon {
        }

        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = github {
                authType = vcsRoot()
                ignoreDrafts = true
            }
        }

        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
    }

    steps {
        gradle {
            jdkHome = "%env.JDK_25_0%"
            tasks = "clean build"
            useGradleWrapper = true
        }
    }
})
