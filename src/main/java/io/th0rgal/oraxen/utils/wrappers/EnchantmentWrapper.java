package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnchantmentWrapper {
    public static @NotNull Enchantment FORTUNE;
    public static @NotNull Enchantment EFFICIENCY;
    public static @NotNull Enchantment SILK_TOUCH;

    static {
        Enchantment FORTUNE_VALUE;
        Enchantment EFFICIENCY_VALUE;
        Enchantment SILK_TOUCH_VALUE;
        try {
            if (VersionUtil.isPaperServer()) {
                FORTUNE_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));
                EFFICIENCY_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
                SILK_TOUCH_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("silk_touch"));
            } else {
                // Fallback for non-Paper servers using standard Bukkit API
                FORTUNE_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
                EFFICIENCY_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
                SILK_TOUCH_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("silk_touch"));
            }
        } catch (final NoSuchMethodError e) {
            // Fallback if Registry.ENCHANTMENT is not available
            FORTUNE_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
            EFFICIENCY_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
            SILK_TOUCH_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("silk_touch"));
        }
        FORTUNE = Objects.requireNonNull(FORTUNE_VALUE);
        EFFICIENCY = Objects.requireNonNull(EFFICIENCY_VALUE);
        SILK_TOUCH = Objects.requireNonNull(SILK_TOUCH_VALUE);
    }
}
