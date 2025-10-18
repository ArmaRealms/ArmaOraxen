package io.th0rgal.oraxen.utils;

import dev.jorel.commandapi.CommandAPIConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class BukkitWrapper {

    private BukkitWrapper() {
    }

    @NotNull
    public static CommandAPIConfig<?> createCommandApiConfig(final JavaPlugin plugin) {
        final String paperConfigClass = "dev.jorel.commandapi.CommandAPIPaperConfig";
        final String spigotConfigClass = "dev.jorel.commandapi.CommandAPISpigotConfig";

        if (VersionUtil.isPaperServer()) {
            plugin.getLogger().info("Detected Paper server, attempting to use CommandAPIPaperConfig");
            final CommandAPIConfig<?> paper = tryConstruct(paperConfigClass, plugin);
            if (paper != null) {
                plugin.getLogger().info("Successfully constructed CommandAPIPaperConfig");
                return applyCommonOptions(paper);
            }

            plugin.getLogger().warning("Failed to construct CommandAPIPaperConfig, falling back to SpigotConfig");
        }

        final CommandAPIConfig<?> spigot = tryConstruct(spigotConfigClass, plugin);
        if (spigot != null)
            return applyCommonOptions(spigot);

        throw new IllegalStateException(
                "Neither CommandAPIPaperConfig nor CommandAPISpigotConfig are available on the classpath");
    }

    private static CommandAPIConfig<?> tryConstruct(final String className, final JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName(className);
            final Constructor<?> ctor = clazz.getConstructor(JavaPlugin.class);
            final Object instance = ctor.newInstance(plugin);
            return (CommandAPIConfig<?>) instance;
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private static CommandAPIConfig<?> applyCommonOptions(final CommandAPIConfig<?> config) {
        // Always enable silent logs
        config.silentLogs(true);
        // Best-effort: call skipReloadDatapacks(true) if method exists on this
        // implementation
        invokeIfPresent(config, "skipReloadDatapacks", true);
        return config;
    }

    private static void invokeIfPresent(final Object target, final String methodName, final boolean arg) {
        try {
            final Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, arg);
        } catch (final Throwable ignored) {
        }
    }
}
