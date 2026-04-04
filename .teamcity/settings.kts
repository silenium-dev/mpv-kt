import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2025.11"

project {

    buildType(BuildSnapshot)
    buildType(BuildRelease)

    features {
        untrustedBuildsSettings {
            id = "PROJECT_EXT_9"
            defaultAction = UntrustedBuildsSettings.DefaultAction.APPROVE
            enableLog = true
            approvalRules = "(groups:MAINTAINERS):1"
            manualRunsApproved = true
        }
    }
}

object BuildRelease : BuildType({
    name = "Build Release"

    params {
        text("deploy.repo-url", "https://nexus.silenium.dev/repository/maven-releases", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("deploy.password", "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2", display = ParameterDisplay.HIDDEN, readOnly = true)
        text("release.version", "", description = "Version to release", display = ParameterDisplay.PROMPT, allowEmpty = false)
        text("deploy.username", "teamcity-ci", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = "build publish"
            gradleParams = "-Pdeploy.version=%release.version% -Pdeploy.enabled=true -Pdeploy.repo-url=%deploy.repo-url% -Pdeploy.username=%deploy.username% -Pdeploy.password=%deploy.password% --scan --info"
        }
    }

    features {
        perfmon {
        }
        vcsLabeling {
            vcsRootId = "${DslContext.settingsRoot.id}"
            labelingPattern = "%release.version%"
            successfulOnly = true
            branchFilter = "+:*"
        }
    }

    requirements {
        exists("system.nix.store")
    }
})

object BuildSnapshot : BuildType({
    name = "Build Snapshot"

    params {
        text("deploy.repo-url", "https://nexus.silenium.dev/repository/maven-snapshots", display = ParameterDisplay.HIDDEN, readOnly = true)
        text("deploy.username", "teamcity-ci", display = ParameterDisplay.HIDDEN, readOnly = true)
        password("deploy.password", "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2", display = ParameterDisplay.HIDDEN, readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = "build publish"
            gradleParams = "-Pci=true -Pdeploy.enabled=true -Pdeploy.repo-url=%deploy.repo-url% -Pdeploy.username=%deploy.username% -Pdeploy.password=%deploy.password% --scan --info"
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
    }

    requirements {
        exists("system.nix.store")
    }
})
