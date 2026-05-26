package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.utils.IntegerRange;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.executableitems.WrappedExecutableItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Loot {

    private final String sourceID;
    private ItemStack itemStack;
    private final double probability;
    private final IntegerRange amount;
    private final boolean requiresSilkTouch;
    private final double fortuneBonus;
    private LinkedHashMap<String, Object> config;

    public Loot(LinkedHashMap<String, Object> config, String sourceID) {
        this.probability = parseDouble(config.get("probability"), 1.0D);
        if (config.getOrDefault("amount", "") instanceof String amount && amount.contains("..")) {
            this.amount = Utils.parseToRange(amount);
        } else this.amount = new IntegerRange(1,1);
        this.requiresSilkTouch = Boolean.parseBoolean(config.getOrDefault("silk-touch", false).toString());
        this.fortuneBonus = parseDouble(config.get("fortune"), 0.0D);
        this.config = config;
        this.sourceID = sourceID;
    }

    public Loot(ItemStack itemStack, double probability) {
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = new IntegerRange(1,1);
        this.requiresSilkTouch = false;
        this.fortuneBonus = 0.0D;
        this.sourceID = null;
    }

    public Loot(String sourceID, ItemStack itemStack, double probability, int minAmount, int maxAmount) {
        this.sourceID = sourceID;
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = new IntegerRange(minAmount, maxAmount);
        this.requiresSilkTouch = false;
        this.fortuneBonus = 0.0D;
    }

    public Loot(String sourceID, ItemStack itemStack, double probability, IntegerRange amount) {
        this.sourceID = sourceID;
        this.itemStack = itemStack;
        this.probability = Math.min(1.0, probability);
        this.amount = amount;
        this.requiresSilkTouch = false;
        this.fortuneBonus = 0.0D;
    }

    public ItemStack getItemStack() {
        if (itemStack != null) return ItemUpdater.updateItem(itemStack);

        String oraxenItemId = getConfigString("oraxen_item");
        String crucibleItemId = getConfigString("crucible_item");
        String mmoItemsId = getConfigString("mmoitems_id");
        String mmoItemsType = getConfigString("mmoitems_type");
        String ecoItemId = getConfigString("ecoitem");
        String executableItemId = getConfigString("executableitem");
        String minecraftType = getConfigString("minecraft_type");
        String itemId = getConfigString("item");

        if (itemId != null) {
            itemStack = resolveItem(itemId);
        } else if (oraxenItemId != null) {
            if (OraxenItems.getItemById(oraxenItemId) != null)
                itemStack = OraxenItems.getItemById(oraxenItemId).build();
        } else if (crucibleItemId != null) {
            itemStack = new WrappedCrucibleItem(crucibleItemId).build();
        } else if (mmoItemsId != null && mmoItemsType != null) {
            String type = mmoItemsType;
            String id = mmoItemsId;
            itemStack = MMOItems.plugin.getItem(type, id);
        } else if (ecoItemId != null) {
            itemStack = new WrappedEcoItem(ecoItemId).build();
        } else if (executableItemId != null) {
            itemStack = new WrappedExecutableItem(executableItemId).build();
        } else if (minecraftType != null) {
            Material material = OraxenYaml.getMaterial(minecraftType);
            itemStack = material != null ? new ItemStack(material) : null;
        } else if (config.containsKey("minecraft_item")) {
            itemStack = (ItemStack) config.get("minecraft_item");
        }

        if (itemStack == null && sourceID != null && OraxenItems.getItemById(sourceID) != null)
            itemStack = OraxenItems.getItemById(sourceID).build();

        if (itemStack == null)
            Logs.logWarning("Failed to resolve loot item for source " + sourceID + " with config " + config);

        return itemStack != null ? ItemUpdater.updateItem(itemStack) : null;
    }

    private ItemStack resolveItem(String itemId) {
        String normalized = itemId.startsWith("oraxen:") ? itemId.substring("oraxen:".length()) : itemId;
        ItemBuilder builder = OraxenItems.getItemById(normalized);
        Material material = getMaterial(itemId);
        if (builder != null) {
            if (!itemId.contains(":") && material != null) {
                Logs.logWarning("Loot item '" + itemId + "' for source " + sourceID + " matches both an Oraxen item and a vanilla material; using the Oraxen item. Prefix with 'minecraft:' to use the vanilla material.");
            }
            return builder.build();
        }

        if (material != null) return new ItemStack(material);

        Logs.logWarning("Failed to resolve loot item '" + itemId + "' for source " + sourceID + " (tried as " + getAttemptedLookupTypes(itemId) + ")");
        return null;
    }

    private String getAttemptedLookupTypes(String itemId) {
        if (itemId.toLowerCase().startsWith("minecraft:")) return "vanilla material";
        if (itemId.toLowerCase().startsWith("oraxen:")) return "Oraxen item";
        return "Oraxen item and vanilla material";
    }

    private Material getMaterial(String itemId) {
        String materialId = itemId.toLowerCase().startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
        return Material.matchMaterial(materialId);
    }

    private static double parseDouble(Object value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public double getProbability() {
        return probability;
    }

    public IntegerRange amount() {
        return this.amount;
    }

    public int getMaxAmount() {
        return amount.getUpperBound();
    }

    /**
     * Drops this loot without a tool context. Loot entries that require Silk Touch are skipped,
     * matching vanilla behavior when no harvesting tool is available.
     * <p>
     * Behavior change from pre-1.215.0: this overload now suppresses {@code silk-touch: true}
     * loots when called without a harvesting tool. Pass an explicit tool to
     * {@link #dropNaturally(Location, int, ItemStack)} when tool-sensitive drops are needed.
     */
    public void dropNaturally(Location location, int amountMultiplier) {
        dropNaturally(location, amountMultiplier, null);
    }

    public void dropNaturally(Location location, int amountMultiplier, ItemStack tool) {
        if (!canDropWith(tool)) return;
        if (Math.random() <= probability)
            dropItems(location, amountMultiplier, tool);
    }

    public ItemStack getItem(int amountMultiplier) {
        return getItem(amountMultiplier, null);
    }

    public ItemStack getItem(int amountMultiplier, ItemStack tool) {
        if (!canDropWith(tool)) return null;
        ItemStack baseStack = getItemStack();
        if (baseStack == null) return null;

        ItemStack stack = baseStack.clone();
        int dropAmount = Math.max(1, ThreadLocalRandom.current().nextInt(amount.getLowerBound(), amount.getUpperBound() + 1));
        stack.setAmount(Math.max(1, stack.getAmount() * amountMultiplier * applyFortune(dropAmount, tool)));
        return ItemUpdater.updateItem(stack);
    }

    public boolean canDropWith(ItemStack tool) {
        return !requiresSilkTouch || (tool != null && tool.containsEnchantment(EnchantmentWrapper.SILK_TOUCH));
    }

    public boolean hasFortuneBonus() {
        return fortuneBonus > 0;
    }

    public double getFortuneBonus() {
        return fortuneBonus;
    }

    private int applyFortune(int amount, ItemStack tool) {
        if (amount <= 0) return amount;
        if (fortuneBonus <= 0 || tool == null) return amount;

        int fortuneLevel = tool.getEnchantmentLevel(EnchantmentWrapper.FORTUNE);
        if (fortuneLevel <= 0) return amount;

        double multiplier = 1.0D + fortuneBonus * fortuneLevel;
        double exactAmount = amount * multiplier;
        int roundedAmount = (int) Math.floor(exactAmount);
        if (Math.random() < exactAmount - roundedAmount) roundedAmount++;
        return Math.max(1, roundedAmount);
    }

    private void dropItems(Location location, int amountMultiplier, ItemStack tool) {
        if (location.getWorld() == null) return;
        for (ItemStack item : getDropItems(amountMultiplier, tool)) {
            location.getWorld().dropItemNaturally(location, item);
        }
    }

    private List<ItemStack> getDropItems(int amountMultiplier, ItemStack tool) {
        ItemStack item = getItem(amountMultiplier, tool);
        if (item == null) return List.of();

        int maxStackSize = Math.max(1, item.getMaxStackSize());
        if (item.getAmount() <= maxStackSize) return List.of(item);

        List<ItemStack> items = new ArrayList<>();
        int remainingAmount = item.getAmount();
        while (remainingAmount > 0) {
            ItemStack splitItem = item.clone();
            splitItem.setAmount(Math.min(maxStackSize, remainingAmount));
            items.add(splitItem);
            remainingAmount -= splitItem.getAmount();
        }
        return items;
    }

    private String getConfigString(String key) {
        Object value = config.get(key);
        if (value == null) return null;

        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }
}
