plugins {
    id("mpv-base")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nix.natives)
}

group = "dev.silenium.libs.mpv.natives"

nixNatives {
    libName = rootProject.name
    libVersion = "0.1.0"
    nixFlake = file("flake.nix")

    sourceFiles.from("src", "meson.build", "subprojects.tpl")
    showLogs = providers.environmentVariable("CI").orElse("false").map { it != "false" }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
