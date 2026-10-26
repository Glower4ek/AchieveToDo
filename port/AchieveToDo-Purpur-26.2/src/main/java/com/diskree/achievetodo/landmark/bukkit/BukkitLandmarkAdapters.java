package com.diskree.achievetodo.landmark.bukkit;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

final class BukkitLandmarkAdapters {

    private BukkitLandmarkAdapters() {
    }

    public static Optional<LandmarkWorld> toLandmarkWorld(World world) {
        DimensionType dimensionType = switch (world.getEnvironment()) {
            case NORMAL -> DimensionType.OVERWORLD;
            case NETHER -> DimensionType.NETHER;
            case THE_END -> DimensionType.END;
            default -> null;
        };
        if (dimensionType == null) {
            return Optional.empty();
        }
        return Optional.of(new LandmarkWorld(world.getUID().toString(), dimensionType));
    }

    public static BlockBox toBlockBox(BoundingBox boundingBox) {
        int minX = (int) Math.floor(boundingBox.getMinX());
        int minY = (int) Math.floor(boundingBox.getMinY());
        int minZ = (int) Math.floor(boundingBox.getMinZ());
        int maxX = Math.max(minX, (int) Math.ceil(boundingBox.getMaxX()) - 1);
        int maxY = Math.max(minY, (int) Math.ceil(boundingBox.getMaxY()) - 1);
        int maxZ = Math.max(minZ, (int) Math.ceil(boundingBox.getMaxZ()) - 1);
        return new BlockBox(
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );
    }
}
