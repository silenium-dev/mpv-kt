import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildSteps.exec
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

    steps {
        exec {
            name = "Build"
            path = "nix"
            arguments = "build -L --rebuild"
            formatStderrAsError = false
        }
    }
})
