package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PapiAliases {

    private PapiAliases() {
    }

    @NotNull
    public static String setPlaceholders(final Player player, final String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @NotNull
    public static String setPlaceholders(final String text) {
        return PlaceholderAPI.setPlaceholders(null, text);
    }

    @NotNull
    @Contract("_, _ -> param2")
    public static ItemStack updateLore(final Player player, @NotNull final ItemStack item) {
        if (item.hasItemMeta()) {
            final ItemMeta meta = item.getItemMeta();
            final List<String> itemLore = meta.getLore();
            if (itemLore != null && !itemLore.isEmpty()) {
                meta.setLore(PlaceholderAPI.setPlaceholders(player, itemLore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
