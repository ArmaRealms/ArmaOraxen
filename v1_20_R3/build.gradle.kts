plugins {
    alias(oraxenLibs.plugins.java)
    alias(oraxenLibs.plugins.userdev)
    alias(oraxenLibs.plugins.shadow)
}

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
    pluginRemapper("net.fabricmc:tiny-remapper:0.10.3:fat")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}