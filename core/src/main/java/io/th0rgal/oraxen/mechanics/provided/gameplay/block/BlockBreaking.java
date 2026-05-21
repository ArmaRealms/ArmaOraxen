package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockBreaking {

    private final List<Rule> rules;

    public BlockBreaking(ConfigurationSection section, String sourceID) {
        this.rules = parseRules(section.getList("breaking"), sourceID);
    }

    public boolean hasHardness(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null && rule.hardness() >= 0.0D;
    }

    public double hardness(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null ? rule.hardness() : 1.0D;
    }

    public Drop drop(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null ? rule.drop() : Drop.emptyDrop();
    }

    public double attributeSpeedMultiplier(ItemStack tool, Material blockType) {
        double nativeSpeed = nativeToolSpeed(tool, blockType);
        return configuredToolSpeed(tool, blockType) / nativeSpeed;
    }

    public double packetSpeedMultiplier(ItemStack tool, Material blockType) {
        return configuredToolSpeed(tool, blockType);
    }

    private double configuredToolSpeed(ItemStack tool, Material blockType) {
        Rule rule = ruleFor(tool);
        return rule == null || rule.fallback() ? 1.0D : tierToolSpeed(tool, blockType);
    }

    @Nullable
    private Rule ruleFor(ItemStack tool) {
        for (Rule rule : rules) {
            if (!rule.fallback() && rule.matches(tool)) return rule;
        }

        for (Rule rule : rules) {
            if (rule.fallback()) return rule;
        }

        return null;
    }

    private List<Rule> parseRules(List<?> ruleConfigs, String sourceID) {
        if (ruleConfigs == null || ruleConfigs.isEmpty())
            return List.of(new Rule(List.of(), true, 1.0D, Drop.emptyDrop()));

        List<Rule> parsedRules = new ArrayList<>();
        for (Object entry : ruleConfigs) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            boolean fallback = map.containsKey("else");
            List<ToolMatcher> matchers = parseMatchers(map.get("when"));
            double hardness = parseDouble(map.get("hardness"), 1.0D);
            Drop drop = parseDrop(map.get("drops"), sourceID);
            parsedRules.add(new Rule(matchers, fallback, hardness, drop));
        }

        return parsedRules.isEmpty() ? List.of(new Rule(List.of(), true, 1.0D, Drop.emptyDrop())) : List.copyOf(parsedRules);
    }

    private List<ToolMatcher> parseMatchers(Object value) {
        if (value instanceof List<?> values) {
            List<ToolMatcher> matchers = new ArrayList<>();
            for (Object entry : values) {
                ToolMatcher matcher = parseMatcher(entry);
                if (matcher != null) matchers.add(matcher);
            }
            return matchers;
        }

        ToolMatcher matcher = parseMatcher(value);
        return matcher == null ? List.of() : List.of(matcher);
    }

    @Nullable
    private ToolMatcher parseMatcher(Object value) {
        if (value == null) return null;

        String key = value.toString().trim();
        if (key.isEmpty()) return null;
        if (key.startsWith("#")) {
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, namespacedKey(key.substring(1)), Material.class);
            return tag == null ? null : tool -> tool != null && tag.isTagged(tool.getType());
        }

        Material material = Material.matchMaterial(stripMinecraftNamespace(key));
        return material == null ? null : tool -> tool != null && tool.getType() == material;
    }

    private NamespacedKey namespacedKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) return NamespacedKey.fromString(normalized);
        return NamespacedKey.minecraft(normalized);
    }

    private String stripMinecraftNamespace(String key) {
        return key.toLowerCase(Locale.ROOT).startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    @SuppressWarnings("unchecked")
    private Drop parseDrop(Object value, String sourceID) {
        if (!(value instanceof List<?> dropConfigs)) return Drop.emptyDrop();

        List<Loot> loots = new ArrayList<>();
        for (Object entry : dropConfigs) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            loots.add(new Loot(new LinkedHashMap<>((Map<String, Object>) map), sourceID));
        }

        return Drop.emptyDrop(loots);
    }

    private double parseDouble(Object value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static double tierToolSpeed(final ItemStack tool, final Material blockType) {
        if (tool == null) return 1.0D;

        final Material toolType = tool.getType();
        final String toolName = toolType.name();

        if (toolType == Material.SHEARS) {
            if (blockType == Material.COBWEB || Tag.LEAVES.isTagged(blockType)) return 15.0D;
            if (Tag.WOOL.isTagged(blockType)) return 5.0D;
            return 2.0D;
        }

        if (toolName.endsWith("_SWORD")) {
            if (blockType == Material.COBWEB || blockType.name().contains("BAMBOO")) return 15.0D;
            return 1.5D;
        }

        if (toolName.startsWith("GOLDEN_")) return 12.0D;
        if (toolName.startsWith("NETHERITE_")) return 9.0D;
        if (toolName.startsWith("DIAMOND_")) return 8.0D;
        if (toolName.startsWith("IRON_")) return 6.0D;
        if (toolName.startsWith("STONE_")) return 4.0D;
        if (toolName.startsWith("WOODEN_")) return 2.0D;
        return 1.0D;
    }

    private static double nativeToolSpeed(final ItemStack tool, final Material blockType) {
        if (tool == null) return 1.0D;

        Material toolType = tool.getType();
        String toolName = toolType.name();

        if (toolType == Material.SHEARS) {
            if (blockType == Material.COBWEB || Tag.LEAVES.isTagged(blockType)) return 15.0D;
            if (Tag.WOOL.isTagged(blockType)) return 5.0D;
            return 2.0D;
        }

        if (toolName.endsWith("_SWORD")) {
            if (blockType == Material.COBWEB || blockType.name().contains("BAMBOO")) return 15.0D;
            return 1.5D;
        }

        if (toolName.endsWith("_PICKAXE") && isMineable(blockType, "mineable/pickaxe")) return tierToolSpeed(tool, blockType);
        if (toolName.endsWith("_AXE") && isMineable(blockType, "mineable/axe")) return tierToolSpeed(tool, blockType);
        if (toolName.endsWith("_SHOVEL") && isMineable(blockType, "mineable/shovel")) return tierToolSpeed(tool, blockType);
        if (toolName.endsWith("_HOE") && isMineable(blockType, "mineable/hoe")) return tierToolSpeed(tool, blockType);
        return 1.0D;
    }

    private static boolean isMineable(Material blockType, String tagName) {
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
        return tag != null && tag.isTagged(blockType);
    }

    private record Rule(List<ToolMatcher> matchers, boolean fallback, double hardness, Drop drop) {
        private boolean matches(ItemStack tool) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(tool));
        }
    }

    @FunctionalInterface
    private interface ToolMatcher {
        boolean matches(ItemStack tool);
    }
}
