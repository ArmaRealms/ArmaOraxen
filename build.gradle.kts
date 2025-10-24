import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

plugins {
    alias(oraxenLibs.plugins.java)
    alias(oraxenLibs.plugins.runpaper)
    alias(oraxenLibs.plugins.userdev)
    alias(oraxenLibs.plugins.shadow)
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" // Generates plugin.yml
}

val compiled = (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
val pluginPath = project.findProperty("oraxen_plugin_path")?.toString()
val devPluginPath = project.findProperty("oraxen_dev_plugin_path")?.toString()
val foliaPluginPath = project.findProperty("oraxen_folia_plugin_path")?.toString()
val spigotPluginPath = project.findProperty("oraxen_spigot_plugin_path")?.toString()
val pluginVersion: String by project

group = "io.th0rgal"
version = pluginVersion

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper") // Paper
            // extra stuff required by paper
            includeGroup("net.md-5")
            includeGroup("com.mojang")
        }
    }
    maven("https://libraries.minecraft.net/") {
        content { includeGroup("net.minecraft") } // Minecraft repo (commodore)
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content { includeGroup("me.clip") } // PlaceHolderAPI
    }
    maven("https://repo.triumphteam.dev/snapshots") {
        content { includeGroup("me.gabytm.util") } // actions-code, actions-spigot
    }
    maven("https://mvn.lumine.io/repository/maven-public/") {
        metadataSources { artifact() }
        content {
            includeModule("io.lumine", "MythicLib")
            includeModule("io.lumine", "Mythic-Dist")
            includeModule("io.lumine", "MythicCrucible-API")
            includeGroup("com.ticxo.modelengine") // ModelEngine
        }
    }
    maven("https://repo.oraxen.com/releases") {
        content { includeGroup("io.th0rgal") } // protectionlib
    }
    maven("https://repo.oraxen.com/snapshots") {
        content { includeGroup("io.th0rgal") }
    }
    maven("https://repo.auxilor.io/repository/maven-public/") {
        content { includeGroup("com.willfp") } // EcoItems, eco, libreforge
    }
    maven("https://maven.enginehub.org/repo/") {
        content { includeGroupAndSubgroups("com.sk89q.worldedit") } // world edit
    }
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/") {
        content {
            includeModule("io.lumine", "MythicLib-dist")
            includeGroup("net.Indyuce") // MMOItems
        }
    }
    maven("https://repo.codemc.org/repository/maven-public/") {
        content { includeGroup("nl.rutgerkok") } // BlockLocker
    }
    maven("https://repo.codemc.io/repository/maven-releases/") {
        content { includeGroup("com.github.retrooper") }
    }
    mavenCentral()
    flatDir { dirs("libs") }
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
    // libraries in plugin.yml > libraries
    compileOnly(oraxenLibs.bundles.libraries.bukkit) {
        exclude("org.jetbrains", "annotations")
    }
    // things that are included in minecraft but not exposed
    compileOnly(oraxenLibs.bundles.libraries.included)
    // Plugin dependencies
    compileOnly(oraxenLibs.bundles.plugins) {
        exclude("org.jetbrains", "annotations")
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori")
    }
    compileOnly(fileTree("libs"))
    // shaded dependencies
    implementation(oraxenLibs.bundles.libraries.shade) {
        exclude("com.google.code.gson", "gson")
        exclude("net.kyori")
        exclude(group = "com.google.guava")
    }
    implementation(oraxenLibs.commandapi.paper)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filesNotMatching(
            listOf(
                "**/*.png",
                "**/*.ogg",
                "**/models/**",
                "**/textures/**",
                "**/font/**.json",
                "**/plugin.yml"
            )
        ) {
            expand(mapOf(project.version.toString() to pluginVersion))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        downloadPlugins {
            hangar("ProtocolLib", "5.4.0")
        }
        minecraftVersion("1.21.10")
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }

    shadowJar {
        archiveClassifier = null
        oraxenLibs.bundles.libraries.shade.get().forEach {
            val group = it.group!!
                .replace("jeff-media", "jeff_media") // they use a different package than the group...
            val parts = group
                .split(".")
            var relocated = parts[parts.size - 1]
            if (parts.size > 2) {
                relocated = parts[parts.size - 2] + "." + relocated
            }
            logger.lifecycle("Relocating ${group} to io.th0rgal.oraxen.shaded." + relocated)
            relocate(group, "io.th0rgal.oraxen.shaded." + relocated) {
                exclude("io.th0rgal.oraxen.**")
            }
        }
        // exception for this one dunno who includes that...
        relocate("org.intellij.lang.annotations", "io.th0rgal.oraxen.shaded.intellij.annotations")
        relocate("com.udojava.evalex", "io.th0rgal.oraxen.shaded.udojava.evalex")
        relocate("javax.json", "io.th0rgal.oraxen.shaded.javax.json")
        relocate("dev.jorel.commandapi", "io.th0rgal.oraxen.shaded.commandapi")

        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSZ").format(Date.from(Instant.now())),
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${
                        System.getProperty(
                            "java.vm.version"
                        )
                    }",
                    "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                        System.getProperty(
                            "os.version"
                        )
                    }",
                    "Compiled" to (project.findProperty("oraxen_compiled")?.toString() ?: "true").toBoolean()
                )
            )
        }
        archiveFileName.set("Oraxen-${pluginVersion}.jar")
        archiveClassifier.set("")
    }

    compileJava.get().dependsOn(clean)
    build.get().dependsOn(shadowJar)
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "io.th0rgal.oraxen.OraxenPlugin"
    version = pluginVersion
    name = "Oraxen"
    apiVersion = "1.18"
    authors = listOf("th0rgal", "https://github.com/oraxen/oraxen/blob/master/CONTRIBUTORS.md")
    softDepend = listOf(
        "ProtocolLib",
        "LightAPI", "PlaceholderAPI", "MythicMobs", "MMOItems", "MythicCrucible", "MythicMobs", "BossShopPro",
        "CrateReloaded", "ItemBridge", "WorldEdit", "WorldGuard", "Towny", "Factions", "Lands", "PlotSquared",
        "NBTAPI", "ModelEngine", "ViaBackwards", "HuskClaims", "HuskTowns", "BentoBox"
    )
    loadBefore = listOf("Realistic_World")
    permissions.create("oraxen.command") {
        description = "Allows the player to use the /oraxen command"
        default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
    }
    libraries = oraxenLibs.bundles.libraries.bukkit.get().map { it.toString() }
}
