plugins {
    alias(oraxenLibs.plugins.java)
    alias(oraxenLibs.plugins.userdev)
    alias(oraxenLibs.plugins.shadow)
}

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
} 