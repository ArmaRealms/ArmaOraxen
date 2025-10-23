package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Drop {

    final boolean silktouch;
    final boolean fortune;
    final String sourceID;
    private final List<Loot> loots;
    private final List<String> bestTools;
    String minimalType;
    private List<String> hierarchy;

    public Drop(final List<String> hierarchy, final List<Loot> loots, final boolean silktouch, final boolean fortune, final String sourceID,
                final String minimalType, final List<String> bestTools) {
        this.hierarchy = hierarchy;
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        this.minimalType = minimalType;
        this.bestTools = bestTools;
    }

    public Drop(final List<Loot> loots, final boolean silktouch, final boolean fortune, final String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        this.bestTools = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static Drop createDrop(final List<String> toolTypes, @NotNull final ConfigurationSection dropSection, final String sourceID) {
        final List<Loot> loots = ((List<LinkedHashMap<String, Object>>) dropSection.getList("loots", new ArrayList<>())).stream().map(c -> new Loot(c, sourceID)).toList();
        return new Drop(toolTypes, loots, dropSection.getBoolean("silktouch"),
                dropSection.getBoolean("fortune"), sourceID,
                dropSection.getString("minimal_type", ""), dropSection.getStringList("best_tools"));
    }

    public static Drop emptyDrop() {
        return new Drop(new ArrayList<>(), false, false, "");
    }

    public static Drop emptyDrop(final List<Loot> loots) {
        return new Drop(loots, false, false, "");
    }

    public static Drop clone(final Drop drop, final List<Loot> newLoots) {
        return new Drop(drop.hierarchy, newLoots, drop.silktouch, drop.fortune, drop.sourceID, drop.minimalType, drop.bestTools);
    }

    public String getItemType(final ItemStack itemInHand) {
        final String itemID = OraxenItems.getIdByItem(itemInHand);
        final ItemTypeMechanicFactory factory = ItemTypeMechanicFactory.get();
        if (factory == null || factory.isNotImplementedIn(itemID)) {
            final String[] content = itemInHand.getType().toString().split("_");
            return content.length >= 2 ? content[0] : "";
        } else {
            final ItemTypeMechanic mechanic = (ItemTypeMechanic) factory.getMechanic(itemID);
            return mechanic.itemType;
        }
    }

    public boolean canDrop(final ItemStack itemInHand) {
        return minimalType == null || minimalType.isEmpty() || isToolEnough(itemInHand) && isTypeEnough(itemInHand);
    }

    public boolean isTypeEnough(final ItemStack itemInHand) {
        if (minimalType != null && !minimalType.isEmpty()) {
            final String itemType = itemInHand == null ? "" : getItemType(itemInHand);
            return !itemType.isEmpty() && hierarchy.contains(itemType)
                    && (hierarchy.indexOf(itemType) >= hierarchy.indexOf(minimalType));
        } else return true;
    }

    public boolean isToolEnough(final ItemStack itemInHand) {
        if (!bestTools.isEmpty()) {
            final String itemID = OraxenItems.getIdByItem(itemInHand);
            final String type = (itemInHand == null ? Material.AIR : itemInHand.getType()).toString().toUpperCase();
            if (itemID != null && bestTools.stream().anyMatch(itemID::equalsIgnoreCase)) return true;
            else if (bestTools.contains(type)) return true;
            else return bestTools.stream().anyMatch(toolName -> type.endsWith(toolName.toUpperCase()));
        } else return true;
    }

    public int getDiff(final ItemStack item) {
        return (minimalType == null) ? 0 : hierarchy.indexOf(getItemType(item)) - hierarchy.indexOf(minimalType);
    }

    public boolean isSilktouch() {
        return silktouch;
    }

    public boolean isFortune() {
        return fortune;
    }

    public String getSourceID() {
        return sourceID;
    }

    public String getMinimalType() {
        return minimalType;
    }

    public List<String> getBestTools() {
        return bestTools;
    }

    public List<String> getHierarchy() {
        return hierarchy;
    }

    public List<Loot> getLoots() {
        return loots;
    }

    public Drop setLoots(final List<Loot> loots) {
        this.loots.clear();
        this.loots.addAll(loots);
        return this;
    }

    public void spawns(final Location location, final ItemStack itemInHand) {
        if (!canDrop(itemInHand) || !BlockHelpers.isLoaded(location)) return;

        if (sourceID != null && silktouch
                && itemInHand.hasItemMeta()
                && itemInHand.getItemMeta().hasEnchant(EnchantmentWrapper.SILK_TOUCH)) {
            final ItemStack baseItem = OraxenItems.getItemById(sourceID).build();
            location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), baseItem);
        } else dropLoot(loots, location, getFortuneMultiplier(itemInHand));
    }

    public void furnitureSpawns(final Entity baseEntity, final ItemStack itemInHand) {
        final ItemStack baseItem = OraxenItems.getItemById(sourceID).build();
        final Location location = BlockHelpers.toBlockLocation(baseEntity.getLocation());
        final ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
        ItemUtils.editItemMeta(furnitureItem, itemMeta -> {
            final ItemMeta baseMeta = baseItem.getItemMeta();
            if (baseMeta != null && baseMeta.hasDisplayName())
                itemMeta.setDisplayName(baseMeta.getDisplayName());
        });

        if (!canDrop(itemInHand) || !location.isWorldLoaded()) return;
        if (location.getWorld() == null) return;

        location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), furnitureItem);
    }

    private int getFortuneMultiplier(final ItemStack itemInHand) {
        int fortuneMultiplier = 1;
        if (itemInHand != null) {
            final ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta != null && fortune && itemMeta.hasEnchant(EnchantmentWrapper.FORTUNE)) {
                fortuneMultiplier += ThreadLocalRandom.current().nextInt(itemMeta.getEnchantLevel(EnchantmentWrapper.FORTUNE));
            }
        }
        return fortuneMultiplier;
    }

    private void dropLoot(final List<Loot> loots, final Location location, final int fortuneMultiplier) {
        for (final Loot loot : loots) loot.dropNaturally(location, fortuneMultiplier);
    }

    /**
     * Get the loots that will drop based on a given Player
     *
     * @param player the player that triggered this drop
     * @return the loots that will drop
     */
    public List<Loot> getLootToDrop(final Player player) {
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final int fortuneMultiplier = getFortuneMultiplier(itemInHand);
        final List<Loot> droppedLoots = new ArrayList<>();
        for (final Loot loot : loots) {
            final ItemStack item = loot.getItem(fortuneMultiplier);

            if (!canDrop(itemInHand) || item == null) continue;
            if (Math.random() > loot.getProbability()) continue;

            droppedLoots.add(loot);
        }
        return droppedLoots;
    }
}
