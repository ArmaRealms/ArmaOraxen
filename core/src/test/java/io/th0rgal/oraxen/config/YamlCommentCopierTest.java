package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlCommentCopierTest {

    @Test
    void insertedSettingsOptionIncludesDefaultComments() {
        YamlConfiguration existing = OraxenYaml.loadConfiguration(new StringReader("""
                # Server owner comment
                debug: true
                Pack:
                  generation:
                    generate: false # Custom server value
                """));
        YamlConfiguration defaults = OraxenYaml.loadConfiguration(new StringReader("""
                debug: false
                Pack:
                  generation:
                    generate: true
                    # If true, Oraxen will not create or modify pack.mcmeta.
                    # The file will be included exactly as provided.
                    disable_mcmeta_generation: false # Default inline comment
                """));

        YamlCommentCopier.setWithComments(existing, defaults, "Pack.generation.disable_mcmeta_generation");

        String saved = existing.saveToString();
        assertEquals(false, existing.getBoolean("Pack.generation.generate"));
        assertTrue(saved.contains("# Server owner comment\ndebug: true"));
        assertTrue(saved.contains("generate: false # Custom server value"));
        assertTrue(saved.contains("""
                    # If true, Oraxen will not create or modify pack.mcmeta.
                    # The file will be included exactly as provided.
                    disable_mcmeta_generation: false # Default inline comment
                """));
    }
}
