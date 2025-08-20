package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.items.ItemBuilder.UNSTACKABLE_KEY;

public class ItemUpdater implements Listener {

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;

        final PlayerInventory inventory = event.getPlayer().getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            final ItemStack oldItem = inventory.getItem(i);
            if (oldItem == null) continue;
            final ItemStack newItem = ItemUpdater.updateItem(oldItem);
            if (oldItem.equals(newItem)) continue;
            inventory.setItem(i, newItem);
        }
    }

    @EventHandler
    public void onPlayerPickUp(final EntityPickupItemEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;
        if (!(event.getEntity() instanceof Player)) return;

        final ItemStack oldItem = event.getItem().getItemStack();
        final ItemStack newItem = ItemUpdater.updateItem(oldItem);
        if (oldItem.equals(newItem)) return;
        event.getItem().setItemStack(newItem);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(final PrepareItemEnchantEvent event) {
        final String id = OraxenItems.getIdByItem(event.getItem());
        final ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(final PrepareAnvilEvent event) {
        final ItemStack item = event.getInventory().getItem(0);
        final ItemStack result = event.getResult();
        final String id = OraxenItems.getIdByItem(item);
        final ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            if (result == null || item == null) return;
            if (!result.getEnchantments().equals(item.getEnchantments()))
                event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseMaxDamageItem(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (!VersionUtil.atOrAbove("1.20.5") || player.getGameMode() == GameMode.CREATIVE) return;
        if (ItemUtils.isEmpty(itemStack) || ItemUtils.isTool(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof final Damageable damageable) || !damageable.hasMaxDamage()) return;

        Optional.ofNullable(OraxenItems.getBuilderByItem(itemStack)).ifPresent(i -> {
            if (i.isDamagedOnBlockBreak()) itemStack.damage(1, player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseMaxDamageItem(final EntityDamageByEntityEvent event) {
        if (!VersionUtil.atOrAbove("1.20.5") || VersionUtil.atOrAbove("1.21.2")) return;
        if (!(event.getDamager() instanceof final LivingEntity entity)) return;
        final ItemStack itemStack = Optional.ofNullable(entity.getEquipment()).map(EntityEquipment::getItemInMainHand).orElse(null);

        if (entity instanceof final Player player && player.getGameMode() == GameMode.CREATIVE) return;
        if (ItemUtils.isEmpty(itemStack) || ItemUtils.isTool(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof final Damageable damageable) || !damageable.hasMaxDamage()) return;

        Optional.ofNullable(OraxenItems.getBuilderByItem(itemStack)).ifPresent(i -> {
            if (i.isDamagedOnEntityHit()) itemStack.damage(1, entity);
        });
    }

    // Until Paper changes getReplacement to use food-component, this is the best way
    @EventHandler(ignoreCancelled = true)
    public void onUseConvertedTo(final PlayerItemConsumeEvent event) {
        final ItemStack itemStack = event.getItem();
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (!VersionUtil.atOrAbove("1.21") && itemMeta == null) return;
        final ItemStack usingConvertsTo = ItemUtils.getUsingConvertsTo(itemMeta);
        if (usingConvertsTo == null || !itemStack.isSimilar(ItemUpdater.updateItem(usingConvertsTo))) return;

        final PlayerInventory inventory = event.getPlayer().getInventory();
        if (inventory.firstEmpty() == -1) event.setItem(event.getItem().add(usingConvertsTo.getAmount()));
        else Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
            for (int i = 0; i < inventory.getSize(); i++) {
                final ItemStack oldItem = inventory.getItem(i);
                final ItemStack newItem = ItemUpdater.updateItem(oldItem);
                if (!itemStack.isSimilar(newItem)) continue;

                // Remove the item and add it to fix stacking
                inventory.setItem(i, null);
                inventory.addItem(newItem);
            }
        });
    }

    private static final NamespacedKey IF_UUID = Objects.requireNonNull(NamespacedKey.fromString("oraxen:if-uuid"));
    private static final NamespacedKey MF_GUI = Objects.requireNonNull(NamespacedKey.fromString("oraxen:mf-gui"));

    public static ItemStack updateItem(final ItemStack oldItem) {
        final String id = OraxenItems.getIdByItem(oldItem);
        if (id == null) return oldItem;

        // Oraxens Inventory adds a dumb PDC entry to items, this will remove them
        // Done here over [ItemsView] as this method is called anyway and supports old items
        ItemUtils.editItemMeta(oldItem, itemMeta -> {
            itemMeta.getPersistentDataContainer().remove(IF_UUID);
            itemMeta.getPersistentDataContainer().remove(MF_GUI);
        });

        final Optional<ItemBuilder> optionalBuilder = OraxenItems.getOptionalItemById(id);
        if (optionalBuilder.isEmpty() || optionalBuilder.get().getOraxenMeta().isNoUpdate()) return oldItem;
        final ItemBuilder newItemBuilder = optionalBuilder.get();

        final ItemStack newItem = NMSHandlers.getHandler() != null ? NMSHandlers.getHandler().copyItemNBTTags(oldItem, newItemBuilder.build()) : newItemBuilder.build();
        newItem.setAmount(oldItem.getAmount());

        ItemUtils.editItemMeta(newItem, itemMeta -> {
            final ItemMeta oldMeta = oldItem.getItemMeta();
            final ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;
            final PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();
            final PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();

            // Transfer over all PDC entries from oldItem to newItem
            final List<Map<?, ?>> oldPdcMap = PersistentDataSerializer.toMapList(oldPdc);
            PersistentDataSerializer.fromMapList(oldPdcMap, itemPdc);

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (final Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (final Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            final Integer cmd = newMeta.hasCustomModelData() ? (Integer) newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? (Integer) oldMeta.getCustomModelData() : null;
            itemMeta.setCustomModelData(cmd);

            // If OraxenItem has no lore, we should assume that 3rd-party plugin has added lore
            if (Settings.OVERRIDE_ITEM_LORE.toBool()) {
                if (VersionUtil.isPaperServer()) itemMeta.lore(newMeta.lore());
                else itemMeta.setLore(newMeta.getLore());
            } else {
                if (VersionUtil.isPaperServer()) itemMeta.lore(oldMeta.lore());
                else itemMeta.setLore(oldMeta.getLore());
            }

            // Update first line of the lore if it is different
            if (VersionUtil.isPaperServer()) {
                updateLore(oldMeta.lore(), newMeta.lore(), itemMeta::lore);
            } else {
                updateLore(oldMeta.getLore(), newMeta.getLore(), itemMeta::setLore);
            }

            // Only change AttributeModifiers if the new item has some
            if (newMeta.hasAttributeModifiers()) itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());
            else if (oldMeta.hasAttributeModifiers()) itemMeta.setAttributeModifiers(oldMeta.getAttributeModifiers());

            // Transfer over durability from old item
            if (itemMeta instanceof final Damageable damageable && oldMeta instanceof final Damageable oldDmg) {
                if (oldDmg.hasDamage()) damageable.setDamage(oldDmg.getDamage());
            }

            if (oldMeta.isUnbreakable()) itemMeta.setUnbreakable(true);

            if (itemMeta instanceof final LeatherArmorMeta leatherMeta && oldMeta instanceof final LeatherArmorMeta oldLeatherMeta && newMeta instanceof final LeatherArmorMeta newLeatherMeta) {
                // If it is not custom armor, keep color
                if (oldItem.getType() == Material.LEATHER_HORSE_ARMOR) leatherMeta.setColor(oldLeatherMeta.getColor());
                    // If it is custom armor we use newLeatherMeta color, since the builder would have been altered
                    // in the process of creating the shader images. Then we just save the builder to update the config
                else {
                    leatherMeta.setColor(newLeatherMeta.getColor());
                    newItemBuilder.save();
                }
            }

            if (itemMeta instanceof final PotionMeta potionMeta && oldMeta instanceof final PotionMeta oldPotionMeta) {
                potionMeta.setColor(oldPotionMeta.getColor());
            }

            if (itemMeta instanceof final MapMeta mapMeta && oldMeta instanceof final MapMeta oldMapMeta) {
                mapMeta.setColor(oldMapMeta.getColor());
            }

            if (VersionUtil.atOrAbove("1.20") && itemMeta instanceof final ArmorMeta armorMeta && oldMeta instanceof final ArmorMeta oldArmorMeta) {
                armorMeta.setTrim(oldArmorMeta.getTrim());
            }

            if (VersionUtil.atOrAbove("1.20.5")) {
                if (newMeta.hasFood()) itemMeta.setFood(newMeta.getFood());
                else if (oldMeta.hasFood()) itemMeta.setFood(oldMeta.getFood());

                if (newMeta.hasEnchantmentGlintOverride()) itemMeta.setEnchantmentGlintOverride(newMeta.getEnchantmentGlintOverride());
                else if (oldMeta.hasEnchantmentGlintOverride()) itemMeta.setEnchantmentGlintOverride(oldMeta.getEnchantmentGlintOverride());

                if (newMeta.hasMaxStackSize()) itemMeta.setMaxStackSize(newMeta.getMaxStackSize());
                else if (oldMeta.hasMaxStackSize()) itemMeta.setMaxStackSize(oldMeta.getMaxStackSize());

                if (VersionUtil.isPaperServer()) {
                    if (newMeta.hasItemName()) itemMeta.itemName(newMeta.itemName());
                    else if (oldMeta.hasItemName()) itemMeta.itemName(oldMeta.itemName());
                } else {
                    if (newMeta.hasItemName()) itemMeta.setItemName(newMeta.getItemName());
                    else if (oldMeta.hasItemName()) itemMeta.setItemName(oldMeta.getItemName());
                }
            }

            if (VersionUtil.atOrAbove("1.21")) {
                if (newMeta.hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(newMeta.getJukeboxPlayable());
                else if (oldMeta.hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(oldMeta.getJukeboxPlayable());
            }

            if (VersionUtil.atOrAbove("1.21.2")) {
                if (newMeta.hasEquippable()) itemMeta.setEquippable(newMeta.getEquippable());
                else if (oldMeta.hasEquippable()) itemMeta.setEquippable(newMeta.getEquippable());

                if (newMeta.isGlider()) itemMeta.setGlider(true);
                else if (oldMeta.isGlider()) itemMeta.setGlider(true);

                if (newMeta.hasItemModel()) itemMeta.setItemModel(newMeta.getItemModel());
                else if (oldMeta.hasItemModel()) itemMeta.setItemModel(oldMeta.getItemModel());

                if (newMeta.hasUseCooldown()) itemMeta.setUseCooldown(newMeta.getUseCooldown());
                else if (oldMeta.hasUseCooldown()) itemMeta.setUseCooldown(oldMeta.getUseCooldown());

                if (newMeta.hasUseRemainder()) itemMeta.setUseRemainder(newMeta.getUseRemainder());
                else if (oldMeta.hasUseRemainder()) itemMeta.setUseRemainder(oldMeta.getUseRemainder());

                if (newMeta.hasDamageResistant()) itemMeta.setDamageResistant(newMeta.getDamageResistant());
                else if (oldMeta.hasDamageResistant()) itemMeta.setDamageResistant(oldMeta.getDamageResistant());

                if (newMeta.hasTooltipStyle()) itemMeta.setTooltipStyle(newMeta.getTooltipStyle());
                else if (oldMeta.hasTooltipStyle()) itemMeta.setTooltipStyle(oldMeta.getTooltipStyle());

                if (newMeta.hasEnchantable()) itemMeta.setEnchantable(newMeta.getEnchantable());
                else if (oldMeta.hasEnchantable()) itemMeta.setEnchantable(oldMeta.getEnchantable());
            }

            // On 1.20.5+ we use ItemName which is different from userchanged displaynames
            if (!VersionUtil.atOrAbove("1.20.5")) {

                final String oldDisplayName = oldMeta.hasDisplayName() ? AdventureUtils.parseLegacy(VersionUtil.isPaperServer() ? AdventureUtils.MINI_MESSAGE.serialize(oldMeta.displayName()) : AdventureUtils.parseLegacy(oldMeta.getDisplayName())) : null;
                String originalName = AdventureUtils.parseLegacy(oldPdc.getOrDefault(ORIGINAL_NAME_KEY, DataType.STRING, ""));

                if (Settings.OVERRIDE_RENAMED_ITEMS.toBool()) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                } else if (!originalName.equals(oldDisplayName)) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(oldMeta.displayName());
                    else itemMeta.setDisplayName(oldMeta.getDisplayName());
                } else {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                }

                originalName = newMeta.hasDisplayName() ? VersionUtil.isPaperServer()
                        ? AdventureUtils.MINI_MESSAGE.serialize(newMeta.displayName())
                        : newMeta.getDisplayName()
                        : null;
                if (originalName != null) itemPdc.set(ORIGINAL_NAME_KEY, DataType.STRING, originalName);
            } else { // Set the displayName/customName if it exists on an item before
                if (newMeta.hasDisplayName() && !newMeta.getDisplayName().isEmpty()) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                } else {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(oldMeta.displayName());
                    else itemMeta.setDisplayName(oldMeta.getDisplayName());
                }
            }


            // If the item is not unstackable, we should remove the unstackable tag
            // Also remove it on 1.20.5+ due to maxStackSize component
            if (VersionUtil.atOrAbove("1.20.5") || !newItemBuilder.isUnstackable()) itemPdc.remove(UNSTACKABLE_KEY);
            else itemPdc.set(UNSTACKABLE_KEY, DataType.UUID, UUID.randomUUID());
        });

        Optional.ofNullable(NMSHandlers.getHandler()).ifPresent(nmsHandler ->
                nmsHandler.consumableComponent(newItem, Optional.ofNullable(nmsHandler.consumableComponent(newItem)).orElse(nmsHandler.consumableComponent(oldItem)))
        );

        return newItem;
    }

    private static <T> void updateLore(final List<T> oldLore, final List<T> newLore, final Consumer<List<T>> applyLore) {
        if (isValidLore(oldLore) && isValidLore(newLore)) {
            // Utilize Pattern Matching for Equality Checks
            if (!(newLore.getFirst() instanceof final T newElement && newElement.equals(oldLore.getFirst()))) {
                final List<T> updatedLore = new ArrayList<>(oldLore);
                updatedLore.set(0, newLore.getFirst());
                applyLore.accept(updatedLore);
            }
        }
    }

    private static <T> boolean isValidLore(final List<T> lore) {
        return lore instanceof final List<T> validList && !validList.isEmpty();
    }

}
