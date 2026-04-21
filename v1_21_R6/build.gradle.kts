plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

// Paper 1.21.11 dev bundle for v1_21_R6 NMS module
// Note: 1.21.11 uses Identifier instead of ResourceLocation (handled by ResourceLocationHelper)
// Note: 1.21.11 dev bundle doesn't provide reobf mappings, so we ship Mojang-mapped

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    // Disable reobfJar - 1.21.11 dev bundle doesn't provide reobf mappings
    // and Paper 1.21.11 runs with Mojang mappings anyway
    matching { it.name == "reobfJar" }.configureEach {
        enabled = false
    }
}

// Root project depends on the `reobf` outgoing configuration when embedding NMS modules.
// Since reobfJar is disabled, publish the plain Mojang-mapped jar.
configurations.named("reobf") {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.named("jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}


