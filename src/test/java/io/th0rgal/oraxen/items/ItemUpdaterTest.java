package io.th0rgal.oraxen.items;

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.recipes.CustomWorkstationRecipe;
import io.th0rgal.oraxen.recipes.CustomWorkstationRegistry;
import io.th0rgal.oraxen.recipes.listeners.CustomWorkstationEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.utils.InventoryUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Isolated
class ItemUpdaterTest {
    private Field pluginField;
    private Object previousPlugin;
    private MockedStatic<OraxenItems> items;
    private MockedStatic<NMSHandlers> handlers;
    private MockedStatic<VersionUtil> versions;
    private ItemBuilder builder;
    private OraxenMeta oraxenMeta;
    private Enchantment efficiency;
    private Enchantment fortune;

    @BeforeEach
    void setUp() throws Exception {
        efficiency = mock(Enchantment.class);
        fortune = mock(Enchantment.class);

        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        doReturn("oraxen").when(plugin).namespace();
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("ItemUpdater.update_items", false);
        settings.set("ItemUpdater.override_item_lore", false);
        when(configs.getSettings()).thenReturn(settings);
        doReturn(configs).when(plugin).getConfigsManager();
        pluginField = OraxenPlugin.class.getDeclaredField("oraxen");
        pluginField.setAccessible(true);
        previousPlugin = pluginField.get(null);
        Field pluginFile = JavaPlugin.class.getDeclaredField("file");
        pluginFile.setAccessible(true);
        pluginFile.set(plugin, new File("missing-oraxen-test.jar"));
        pluginField.set(null, plugin);
        Settings.invalidateCache();
        versions = mockStatic(VersionUtil.class);
        versions.when(() -> VersionUtil.atOrAbove("1.20.5")).thenReturn(true);
        items = mockStatic(OraxenItems.class);
        handlers = mockStatic(NMSHandlers.class);
        builder = mock(ItemBuilder.class);
        oraxenMeta = new OraxenMeta();
        when(builder.hasOraxenMeta()).thenReturn(true);
        when(builder.getOraxenMeta()).thenReturn(oraxenMeta);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (handlers != null) handlers.close();
        if (items != null) items.close();
        if (versions != null) versions.close();
        if (pluginField != null) pluginField.set(null, previousPlugin);
        Settings.invalidateCache();
    }

    @Test
    void configuredLevelOverridesBothLowerAndHigherExistingLevels() {
        assertEquals(Map.of(efficiency, 5), update(Map.of(efficiency, 3), Map.of(efficiency, 5)));
        assertEquals(Map.of(efficiency, 5), update(Map.of(efficiency, 7), Map.of(efficiency, 5)));
    }

    @Test
    void configuredEnchantmentsAreRestoredOnAnUnenchantedItem() {
        assertEquals(Map.of(efficiency, 5), update(Map.of(), Map.of(efficiency, 5)));
    }

    @Test
    void extraEnchantmentsAreRemovedAndAnEmptyConfigurationClearsAllEnchantments() {
        assertEquals(Map.of(efficiency, 5), update(Map.of(fortune, 3), Map.of(efficiency, 5)));
        assertEquals(Map.of(), update(Map.of(fortune, 3), Map.of()));
    }

    @Test
    void enchantingIsBlockedByDefaultAndExplicitOptInIsRespected() {
        assertFalse(oraxenMeta.isEnchantable());
        PrepareItemEnchantEvent event = mock(PrepareItemEnchantEvent.class);
        ItemStack item = mock(ItemStack.class);
        when(event.getItem()).thenReturn(item);
        items.when(() -> OraxenItems.getIdByItem(item)).thenReturn("test");
        items.when(() -> OraxenItems.getItemById("test")).thenReturn(builder);
        ItemUpdater updater = new ItemUpdater();
        updater.onItemEnchant(event);
        verify(event).setCancelled(true);
        clearInvocations(event);
        oraxenMeta.setEnchantable(true);
        updater.onItemEnchant(event);
        verify(event, never()).setCancelled(true);
    }

    @Test
    void anvilBlocksEnchantmentChangesButAllowsUnchangedEnchantments() {
        ItemStack item = item(Map.of(efficiency, 5));
        ItemStack result = item(Map.of(efficiency, 6));
        items.when(() -> OraxenItems.getBuilderByItem(item)).thenReturn(builder);
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getItem(0)).thenReturn(item);
        when(event.getResult()).thenReturn(result);
        ItemUpdater updater = new ItemUpdater();
        updater.onItemEnchant(event);
        verify(event).setResult(null);
        clearInvocations(event);
        when(result.getEnchantments()).thenReturn(Map.of(efficiency, 5));
        updater.onItemEnchant(event);
        verify(event, never()).setResult(null);
    }

    @Test
    void anvilCannotTransferEnchantmentsFromAProtectedBookInTheSecondSlot() {
        ItemStack target = item(Map.of());
        ItemStack book = item(Map.of());
        EnchantmentStorageMeta meta = mock(EnchantmentStorageMeta.class);
        when(book.getItemMeta()).thenReturn(meta);
        when(meta.getStoredEnchants()).thenReturn(Map.of(efficiency, 5));
        items.when(() -> OraxenItems.getBuilderByItem(book)).thenReturn(builder);
        ItemStack result = item(Map.of(efficiency, 5));
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getItem(0)).thenReturn(target);
        when(inventory.getItem(1)).thenReturn(book);
        when(event.getResult()).thenReturn(result);

        new ItemUpdater().onItemEnchant(event);

        verify(event).setResult(null);
        clearInvocations(event);
        oraxenMeta.setEnchantable(true);
        new ItemUpdater().onItemEnchant(event);
        verify(event, never()).setResult(null);
    }

    @Test
    void unenchantedAnvilAdditionMayRepairAnItemWithoutChangingItsEnchantments() {
        ItemStack target = item(Map.of(efficiency, 5));
        ItemStack addition = item(Map.of());
        ItemStack result = item(Map.of(efficiency, 5));
        items.when(() -> OraxenItems.getBuilderByItem(target)).thenReturn(builder);
        items.when(() -> OraxenItems.getBuilderByItem(addition)).thenReturn(builder);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(inventory.getItem(0)).thenReturn(target);
        when(inventory.getItem(1)).thenReturn(addition);
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getResult()).thenReturn(result);
        new ItemUpdater().onItemEnchant(event);
        verify(event, never()).setResult(null);

        CustomWorkstationRecipe recipe = mock(CustomWorkstationRecipe.class);
        when(recipe.createResult()).thenReturn(result);
        try (var registry = mockStatic(CustomWorkstationRegistry.class);
             var builders = mockStatic(RecipesBuilderEvents.class);
             var inventoryUtils = mockStatic(InventoryUtils.class)) {
            registry.when(() -> CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.ANVIL, target, addition))
                    .thenReturn(recipe);
            new CustomWorkstationEvents().prepareAnvil(event);
            verify(event).setResult(result);
        }
    }

    @Test
    void grindstoneProtectsEitherInputAndAllowsExplicitOptIn() {
        ItemUpdater updater = new ItemUpdater();
        for (int slot = 0; slot < 2; slot++) {
            ItemStack item = item(Map.of(efficiency, 5));
            items.when(() -> OraxenItems.getBuilderByItem(item)).thenReturn(builder);
            PrepareGrindstoneEvent event = mock(PrepareGrindstoneEvent.class);
            GrindstoneInventory inventory = mock(GrindstoneInventory.class);
            when(event.getInventory()).thenReturn(inventory);
            when(inventory.getItem(slot)).thenReturn(item);
            ItemStack result = item(Map.of());
            when(event.getResult()).thenReturn(result);
            updater.onItemDisenchant(event);
            verify(event).setResult(null);
            clearInvocations(event);
            oraxenMeta.setEnchantable(true);
            updater.onItemDisenchant(event);
            verify(event, never()).setResult(null);
            oraxenMeta.setEnchantable(false);
        }
    }

    @Test
    void storedBookEnchantmentsAreProtectedAsWell() {
        ItemStack book = item(Map.of());
        EnchantmentStorageMeta meta = mock(EnchantmentStorageMeta.class);
        when(book.getItemMeta()).thenReturn(meta);
        when(meta.getStoredEnchants()).thenReturn(Map.of(efficiency, 5));
        items.when(() -> OraxenItems.getBuilderByItem(book)).thenReturn(builder);
        assertTrue(ItemUpdater.isEnchantmentChangeBlocked(book, item(Map.of())));
        oraxenMeta.setEnchantable(true);
        assertFalse(ItemUpdater.isEnchantmentChangeBlocked(book, item(Map.of())));
    }

    @Test
    void vanillaItemsAreNotProtected() {
        assertFalse(ItemUpdater.isEnchantmentChangeBlocked(item(Map.of(efficiency, 5)), item(Map.of())));
    }

    @Test
    void itemConfigurationDefaultsToProtectedAndHonorsAnExplicitOptIn() {
        var config = new YamlConfiguration();
        var section = config.createSection("test");
        ItemProperties properties = new ItemProperties(section, Material.PAPER, oraxenMeta, Map.of());
        properties.applyMiscAndVanilla(builder, section);
        assertFalse(oraxenMeta.isEnchantable());
        section.set("enchantable", true);
        properties.applyMiscAndVanilla(builder, section);
        assertTrue(oraxenMeta.isEnchantable());
    }

    @Test
    void customGrindstoneRecipesCannotBypassProtectionWhenPreparingOrTakingAResult() {
        ItemStack item = item(Map.of(efficiency, 5));
        ItemStack result = item(Map.of());
        items.when(() -> OraxenItems.getBuilderByItem(item)).thenReturn(builder);
        GrindstoneInventory inventory = mock(GrindstoneInventory.class);
        when(inventory.getItem(1)).thenReturn(item);
        var prepare = mock(org.bukkit.event.inventory.PrepareGrindstoneEvent.class);
        when(prepare.getInventory()).thenReturn(inventory);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getRawSlot()).thenReturn(2);
        Player player = mock(Player.class);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getInventory()).thenReturn(inventory);
        CustomWorkstationRecipe recipe = mock(CustomWorkstationRecipe.class);
        when(recipe.createResult()).thenReturn(result);
        try (var registry = mockStatic(CustomWorkstationRegistry.class);
             var builders = mockStatic(RecipesBuilderEvents.class);
             var inventoryUtils = mockStatic(InventoryUtils.class)) {
            registry.when(() -> CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.GRINDSTONE, null, item))
                    .thenReturn(recipe);
            CustomWorkstationEvents listener = new CustomWorkstationEvents();
            listener.prepareGrindstone(prepare);
            verify(prepare).setResult(null);
            listener.takeResult(click);
            verify(click).setCancelled(true);
            verify(inventory, never()).setItem(anyInt(), any());
            clearInvocations(prepare);
            oraxenMeta.setEnchantable(true);
            listener.prepareGrindstone(prepare);
            verify(prepare).setResult(result);
        }
    }

    @Test
    void customAnvilRecipesProtectTheSecondInputAsWell() {
        ItemStack target = item(Map.of());
        ItemStack addition = item(Map.of(efficiency, 5));
        ItemStack result = item(Map.of());
        items.when(() -> OraxenItems.getBuilderByItem(addition)).thenReturn(builder);
        AnvilInventory inventory = mock(AnvilInventory.class);
        when(inventory.getItem(0)).thenReturn(target);
        when(inventory.getItem(1)).thenReturn(addition);
        PrepareAnvilEvent prepare = mock(PrepareAnvilEvent.class);
        when(prepare.getInventory()).thenReturn(inventory);
        CustomWorkstationRecipe recipe = mock(CustomWorkstationRecipe.class);
        when(recipe.createResult()).thenReturn(result);
        try (var registry = mockStatic(CustomWorkstationRegistry.class);
             var builders = mockStatic(RecipesBuilderEvents.class);
             var inventoryUtils = mockStatic(InventoryUtils.class)) {
            registry.when(() -> CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.ANVIL, target, addition))
                    .thenReturn(recipe);
            new CustomWorkstationEvents().prepareAnvil(prepare);
            verify(prepare).setResult(null);
        }
    }

    @Test
    void blockedCustomRecipeDoesNotCancelUnrelatedDoubleClickCollection() {
        ItemStack ingredient = item(Map.of(efficiency, 5));
        ItemStack result = item(Map.of());
        items.when(() -> OraxenItems.getBuilderByItem(ingredient)).thenReturn(builder);
        GrindstoneInventory inventory = mock(GrindstoneInventory.class);
        when(inventory.getItem(0)).thenReturn(ingredient);
        when(inventory.getItem(2)).thenReturn(result);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getAction()).thenReturn(InventoryAction.COLLECT_TO_CURSOR);
        when(click.getRawSlot()).thenReturn(10);
        when(click.getInventory()).thenReturn(inventory);
        Player player = mock(Player.class);
        when(click.getWhoClicked()).thenReturn(player);
        ItemStack cursor = item(Map.of());
        when(click.getCursor()).thenReturn(cursor);
        CustomWorkstationRecipe recipe = mock(CustomWorkstationRecipe.class);
        when(recipe.createResult()).thenReturn(result);
        try (var registry = mockStatic(CustomWorkstationRegistry.class)) {
            registry.when(() -> CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.GRINDSTONE, ingredient, null))
                    .thenReturn(recipe);
            CustomWorkstationEvents listener = new CustomWorkstationEvents();
            listener.takeResult(click);
            verify(click, never()).setCancelled(true);

            when(cursor.isSimilar(result)).thenReturn(true);
            listener.takeResult(click);
            verify(click).setCancelled(true);
            verify(player, never()).setItemOnCursor(any());
            verify(inventory, never()).setItem(anyInt(), any());
        }
    }

    private Map<Enchantment, Integer> update(Map<Enchantment, Integer> oldEnchants,
                                           Map<Enchantment, Integer> configuredEnchants) {
        ItemStack oldItem = item(oldEnchants);
        ItemStack newItem = item(configuredEnchants);
        ItemMeta newMeta = newItem.getItemMeta();
        Map<Enchantment, Integer> applied = new HashMap<>(configuredEnchants);
        doAnswer(call -> { applied.put(call.getArgument(0), call.getArgument(1)); return true; })
                .when(newMeta).addEnchant(any(), anyInt(), eq(true));
        doAnswer(call -> applied.remove(call.<Enchantment>getArgument(0)) != null)
                .when(newMeta).removeEnchant(any());
        items.when(() -> OraxenItems.getIdByItem(oldItem)).thenReturn("test");
        items.when(() -> OraxenItems.getOptionalItemById("test")).thenReturn(Optional.of(builder));
        when(builder.build()).thenReturn(newItem);
        NMSHandler handler = mock(NMSHandler.class);
        when(handler.copyItemNBTTags(oldItem, newItem)).thenReturn(newItem);
        handlers.when(NMSHandlers::getHandler).thenReturn(handler);
        assertSame(newItem, ItemUpdater.updateItem(oldItem));
        return applied;
    }

    private ItemStack item(Map<Enchantment, Integer> enchants) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.getEnchantments()).thenReturn(enchants);
        when(meta.getEnchants()).thenReturn(enchants);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        return item;
    }
}
