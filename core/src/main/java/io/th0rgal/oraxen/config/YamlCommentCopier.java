package io.th0rgal.oraxen.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class YamlCommentCopier {

    private YamlCommentCopier() {
    }

    static void setWithComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path) {
        target.set(path, source.get(path));
        copyComments(target, source, path);
    }

    private static void copyComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path) {
        copyPathComments(target, source, path);

        Object value = source.get(path);
        if (!(value instanceof ConfigurationSection section))
            return;

        for (String childKey : section.getKeys(true))
            copyPathComments(target, source, path + "." + childKey);
    }

    private static void copyPathComments(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source,
            @NotNull String path) {
        List<String> comments = source.getComments(path);
        if (!comments.isEmpty())
            target.setComments(path, comments);

        List<String> inlineComments = source.getInlineComments(path);
        if (!inlineComments.isEmpty())
            target.setInlineComments(path, inlineComments);
    }
}
