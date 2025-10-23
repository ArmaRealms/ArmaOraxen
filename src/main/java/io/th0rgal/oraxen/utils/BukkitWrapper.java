package io.th0rgal.oraxen.utils;

import dev.jorel.commandapi.CommandAPIConfig;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class BukkitWrapper {

    private BukkitWrapper() {
    }

    @NotNull
    public static CommandAPIConfig<?> createCommandApiConfig(final JavaPlugin plugin) {
        if (VersionUtil.isPaperServer()) {
            plugin.getLogger().info("Detected Paper server, enabling Paper optimizations for CommandAPI.");
            return new CommandAPIPaperConfig(plugin).silentLogs(true);
        }

        throw new UnsupportedOperationException("Only Paper servers are supported at this time.");
    }
}
