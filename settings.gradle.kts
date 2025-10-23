rootProject.name = "Oraxen"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }
}

plugins {
    // allows for better class redefinitions with run-paper
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("oraxenLibs") {
            from(files("gradle/oraxenLibs.versions.toml"))
        }
    }
}
