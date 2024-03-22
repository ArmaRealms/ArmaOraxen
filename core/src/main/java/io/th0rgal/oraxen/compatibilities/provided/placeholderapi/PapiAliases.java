package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PapiAliases {

    private PapiAliases() {}

    @NotNull
    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @NotNull
    @Contract("_, _ -> param2")
    public static ItemStack setPlaceholders(Player player, @NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName())
                meta.setDisplayName(setPlaceholders(player, meta.getDisplayName()));
            List<String> itemLore = meta.getLore();
            if (itemLore != null && !itemLore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (var line : itemLore) {
                    lore.add(setPlaceholders(player, line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
