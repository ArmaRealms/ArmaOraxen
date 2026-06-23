rootProject.name = "oraxen"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mineinabyss.com/releases")
    }
}

plugins {
    // allows for better class redefinitions with run-paper
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
//    repositories {
//        maven("https://repo.mineinabyss.com/releases")
//        maven("https://repo.mineinabyss.com/snapshots")
//        mavenLocal()
//    }

    versionCatalogs {
        create("oraxenLibs") {
            from(files("gradle/oraxenLibs.versions.toml"))
        }
    }
}

// Core plus a single Paper/Paper-fork NMS module. Version-specific behavior is guarded at runtime.
include(
    "core",
    "nms"
)
