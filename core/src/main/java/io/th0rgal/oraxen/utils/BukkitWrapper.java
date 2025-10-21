package io.th0rgal.oraxen.utils;

import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import dev.jorel.commandapi.CommandAPISpigotConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitWrapper {

    private BukkitWrapper() {
    }

    @NotNull
    public static CommandAPIConfig<?> createCommandApiConfig(final JavaPlugin plugin) {
        if (VersionUtil.isPaperServer()) {
            return new CommandAPIPaperConfig(plugin).silentLogs(true);
        } else {
            return new CommandAPISpigotConfig(plugin).silentLogs(true).skipReloadDatapacks(true);
        }
    }
}
