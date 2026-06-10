import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gitHubRelease
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2026.1"

project {
    buildType(BuildSnapshot)
    buildType(BuildRelease)

    features {
        untrustedBuildsSettings {
            enableLog = true
            manualRunsApproved = true
            defaultAction = UntrustedBuildsSettings.DefaultAction.APPROVE
            approvalRules = """
                (groups:MAINTAINERS):1
            """.trimIndent()
        }
    }
}

object BuildRelease : BuildType({
    name = "Build Release"

    vcs {
        root(DslContext.settingsRoot)
        branchFilter = """
            |+:*
        """.trimMargin()
    }

    features {
        perfmon {
        }
    }

    requirements {
        exists("system.nix.store")
    }

    params {
        text(
            "release.version",
            "",
            description = "Version to release",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false
        )
        text(
            "deploy.repo-url",
            "https://nexus.silenium.dev/repository/maven-releases",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "deploy.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "deploy.password",
            "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
    }

    steps {
        with(Nix) {
            loadNixEnv()
        }
        gradle {
            tasks = """
                |build
                |utpGroupCheck
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pdeploy.version=%release.version%
                |-Pdeploy.enabled=true
                |-Pdeploy.repo-url=%deploy.repo-url%
                |-Pdeploy.username=%deploy.username%
                |-Pdeploy.password=%deploy.password%
                |--info
            """.trimMargin().replace("\n", " ")
        }
        gitHubRelease {
            name = "Create GitHub Release"
            targetVcsRootId = "${DslContext.settingsRoot.id}"
            githubUrl = "https://api.github.com"
            tagName = "%release.version%"
            generateReleaseNotes = true
            draft = true
            authType = vcsRoot()
        }
    }
})

object BuildSnapshot : BuildType({
    name = "Build Snapshot"

    vcs {
        root(DslContext.settingsRoot)
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

    params {
        text(
            "deploy.repo-url",
            "https://nexus.silenium.dev/repository/maven-snapshots",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "deploy.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "deploy.password",
            "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
    }

    steps {
        with(Nix) {
            loadNixEnv()
        }
        gradle {
            tasks = """
                |build
                |utpGroupCheck
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pci=true
                |-Pdeploy.enabled=true
                |-Pdeploy.repo-url=%deploy.repo-url%
                |-Pdeploy.username=%deploy.username%
                |-Pdeploy.password=%deploy.password%
                |--info
            """.trimMargin().replace("\n", " ")
        }
    }
})

object BuildPR : BuildType({
    name = "Build Pull Request"

    vcs {
        root(DslContext.settingsRoot)
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

    requirements {
        exists("system.nix.store")
    }

    steps {
        with(Nix) {
            loadNixEnv()
        }
        gradle {
            tasks = """
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pdeploy.enabled=false
                |--info
            """.trimMargin().replace("\n", " ")
        }
    }
})

object Nix {
    fun BuildSteps.loadNixEnv(flakeDir: String = ".", devShell: String? = null) {
        script {
            name = "Load Nix Environment"
            // language="Shell Script"
            scriptContent = """
                |set -xeuo pipefail
                |
                |FLAKE_REF="$flakeDir#${devShell.orEmpty()}"
                |
                |tmp_before=$(mktemp)
                |tmp_after=$(mktemp)
                |
                |env | sort > "${"$"}tmp_before"
                |nix develop "${"$"}FLAKE_REF" --command env | sort > "${"$"}tmp_after"
                |
                |MODIFIED_ENV=$(
                |  {
                |    diff "${"$"}tmp_before" "${"$"}tmp_after" || status=${'$'}?
                |    if [ "${"$"}{status:-0}" -gt 1 ]; then
                |      exit "${"$"}status"
                |    fi
                |  } | sed -n 's/^> //p'
                |)
                |while IFS="=" read -r name value; do
                |   escaped_value="${"$"}{value//|/||}"
                |   escaped_value="${"$"}{escaped_value//$'\n'/|n}"
                |   escaped_value="${"$"}{escaped_value//$'\r'/|r}"
                |   escaped_value="${"$"}{escaped_value//\'/|\'}"
                |   escaped_value="${"$"}{escaped_value//[/|[}"
                |   escaped_value="${"$"}{escaped_value//]/|]}"
                |   echo "##teamcity[setParameter name='env.${"$"}{name}' value='${"$"}{escaped_value}']"
                |done <<< "${"$"}MODIFIED_ENV"
            """.trimMargin()
        }
    }
}
