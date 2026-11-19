package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.diskree.achievetodo.restriction.RestrictionTargetKind;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

public final class BukkitRestrictionTargets {

    private BukkitRestrictionTargets() {
    }

    public static Optional<RestrictionTarget> fromBlock(Block block) {
        if (block == null) {
            return Optional.empty();
        }
        return toLandmarkWorld(block.getWorld()).map(world -> RestrictionTarget.block(
            world,
            new BlockBox(block.getX(), block.getY(), block.getZ(), block.getX(), block.getY(), block.getZ()),
            BukkitRestrictionResolver.materialKey(block.getType())
        ));
    }

    public static Optional<RestrictionTarget> fromEntity(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return toLandmarkWorld(entity.getWorld()).map(world -> RestrictionTarget.entity(
            world,
            toBlockBox(entity.getBoundingBox()),
            entity.getUniqueId(),
            entity.getType().getKey().getKey()
        ));
    }

    public static Optional<RestrictionTarget> fromInventory(Inventory inventory, RestrictionTargetKind kind, String targetKey) {
        if (inventory == null) {
            return Optional.empty();
        }
        Location location = inventory.getLocation();
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return toLandmarkWorld(location.getWorld()).map(world -> new RestrictionTarget(
            kind,
            world,
            new BlockBox(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getBlockX(), location.getBlockY(), location.getBlockZ()),
            null,
            targetKey
        ));
    }

    public static Optional<RestrictionTarget> fromBoundingBox(World world, BoundingBox boundingBox, RestrictionTargetKind kind, String targetKey) {
        if (world == null || boundingBox == null) {
            return Optional.empty();
        }
        return toLandmarkWorld(world).map(landmarkWorld -> new RestrictionTarget(kind, landmarkWorld, toBlockBox(boundingBox), null, targetKey));
    }

    private static Optional<LandmarkWorld> toLandmarkWorld(World world) {
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

    private static BlockBox toBlockBox(BoundingBox boundingBox) {
        return new BlockBox(
            (int) Math.floor(boundingBox.getMinX()),
            (int) Math.floor(boundingBox.getMinY()),
            (int) Math.floor(boundingBox.getMinZ()),
            (int) Math.ceil(boundingBox.getMaxX()) - 1,
            (int) Math.ceil(boundingBox.getMaxY()) - 1,
            (int) Math.ceil(boundingBox.getMaxZ()) - 1
        );
    }
}
