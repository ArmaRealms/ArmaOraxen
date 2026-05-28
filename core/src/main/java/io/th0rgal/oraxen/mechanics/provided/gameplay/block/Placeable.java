package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

public class Placeable {

    private final boolean floor;
    private final boolean wall;
    private final boolean roof;

    public Placeable(ConfigurationSection section) {
        boolean parsedFloor = true;
        boolean parsedWall = true;
        boolean parsedRoof = true;

        ConfigurationSection placeableSection = section.getConfigurationSection("placeable");
        if (placeableSection != null) {
            parsedFloor = placeableSection.getBoolean("floor", true);
            parsedWall = placeableSection.getBoolean("wall", true);
            parsedRoof = placeableSection.getBoolean("roof", true);
        } else if (section.isList("placeable")) {
            for (Map<?, ?> entry : section.getMapList("placeable")) {
                if (entry.containsKey("floor")) parsedFloor = Boolean.parseBoolean(entry.get("floor").toString());
                if (entry.containsKey("wall")) parsedWall = Boolean.parseBoolean(entry.get("wall").toString());
                if (entry.containsKey("roof")) parsedRoof = Boolean.parseBoolean(entry.get("roof").toString());
            }
        }

        floor = parsedFloor;
        wall = parsedWall;
        roof = parsedRoof;
    }

    public boolean canPlaceOn(BlockFace face) {
        return switch (face) {
            case UP -> floor;
            case DOWN -> roof;
            case NORTH, EAST, SOUTH, WEST -> wall;
            default -> true;
        };
    }
}
