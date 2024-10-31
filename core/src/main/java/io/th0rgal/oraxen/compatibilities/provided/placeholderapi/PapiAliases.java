package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.utils.VersionUtil;
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
    public static String setPlaceholders(final Player player, final String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @NotNull
    @Contract("_, _, _ -> param2")
    public static ItemStack setPlaceholders(final Player player, @NotNull final ItemStack item, final boolean updateLore) {

        if (item.hasItemMeta()) {
            final ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName())
                meta.setDisplayName(setPlaceholders(player, meta.getDisplayName()));
            if (VersionUtil.atOrAbove("1.21")) {
                if (meta.hasItemName()) {
                    meta.setItemName(setPlaceholders(player, meta.getItemName()));
                }
            }
            if (updateLore) {
                final List<String> itemLore = meta.getLore();
                if (itemLore != null && !itemLore.isEmpty()) {
                    final List<String> lore = new ArrayList<>();
                    for (final var line : itemLore) {
                        lore.add(setPlaceholders(player, line));
                    }
                    meta.setLore(lore);
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
