plugins {
    alias(oraxenLibs.plugins.java)
    alias(oraxenLibs.plugins.userdev)
    alias(oraxenLibs.plugins.shadow)
}

dependencies {
    compileOnly(project(":core"))
    paperweight.paperDevBundle("1.21.5-no-moonrise-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
} 