package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RecipesView {

    private final FontManager fontManager = OraxenPlugin.get().getFontManager();
    final String menuTexture = ChatColor.WHITE +
            fontManager.getShift(-7) +
            fontManager.getGlyphFromName("menu_recipe").getCharacter();

    public Gui create(final int page, final List<CustomRecipe> filteredRecipes) {
        final Gui gui = Gui.gui().rows(6).title(AdventureUtils.LEGACY_SERIALIZER.deserialize(menuTexture)).create();

        final CustomRecipe currentRecipe = filteredRecipes.get(page);

        // Check if last page
        final boolean lastPage = filteredRecipes.size() - 1 == page;
        gui.setItem(1, 5, new GuiItem(currentRecipe.getResult()));

        for (int i = 0; i < currentRecipe.getIngredients().size(); i++) {
            final ItemStack itemStack = currentRecipe.getIngredients().get(i);
            if (itemStack != null && itemStack.getType() != Material.AIR)
                gui.setItem(3 + i / 3, 4 + i % 3, new GuiItem(itemStack));
        }

        // Close RecipeShowcase inventory button
        gui.setItem(6, 5, new GuiItem(iconOrDefault("exit_icon", Material.BARRIER)
                .setDisplayName(Message.EXIT_MENU).build(),
                (event -> event.getWhoClicked().closeInventory())));

        // Previous Page button
        if (page > 0)
            gui.setItem(4, 2, new GuiItem(iconOrDefault("arrow_previous_icon", Material.ARROW)
                    .setDisplayName(pageName(page))
                    .build(),
                    event -> create(page - 1,
                            filteredRecipes).open(event.getWhoClicked())));

        // Next page button
        if (!lastPage)
            gui.setItem(4, 8, new GuiItem(iconOrDefault("arrow_next_icon", Material.ARROW)
                    .setDisplayName(pageName(page + 2))
                    .build(),
                    event -> create(page + 1, filteredRecipes)
                            .open(event.getWhoClicked())));

        gui.setDefaultClickAction(event -> event.setCancelled(true));
        gui.setPlayerInventoryAction(event -> event.setCancelled(true));
        gui.setOutsideClickAction(event -> event.setCancelled(true));
        gui.setDragAction(event -> event.setCancelled(true));
        return gui;
    }

    private ItemBuilder iconOrDefault(String itemId, Material fallback) {
        ItemBuilder icon = OraxenItems.getItemById(itemId);
        return icon != null ? icon : new ItemBuilder(fallback);
    }

    private String pageName(int page) {
        return AdventureUtils.LEGACY_SERIALIZER.serialize(Component.text("Open page " + page, NamedTextColor.YELLOW));
    }

}
