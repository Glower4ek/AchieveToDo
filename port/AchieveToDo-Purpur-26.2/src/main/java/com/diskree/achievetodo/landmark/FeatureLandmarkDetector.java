package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.LandmarkType;

import java.util.LinkedHashSet;
import java.util.Set;

public final class FeatureLandmarkDetector {

    private static final String AIR = "minecraft:air";
    private static final String CAVE_AIR = "minecraft:cave_air";
    private static final String WATER = "minecraft:water";
    private static final String DESERT = "minecraft:desert";
    private static final String SANDSTONE = "minecraft:sandstone";
    private static final String SANDSTONE_SLAB = "minecraft:sandstone_slab";
    private static final String COBBLESTONE = "minecraft:cobblestone";
    private static final String MOSSY_COBBLESTONE = "minecraft:mossy_cobblestone";
    private static final String SPAWNER = "minecraft:spawner";
    private static final String CHEST = "minecraft:chest";
    private static final String COBWEB = "minecraft:cobweb";

    public Set<LandmarkRecord> detect(FeatureChunkView view) {
        Set<LandmarkRecord> landmarks = new LinkedHashSet<>();
        landmarks.addAll(detectDesertWells(view));
        landmarks.addAll(detectMonsterRooms(view));
        return landmarks;
    }

    public Set<LandmarkRecord> detectDesertWells(FeatureChunkView view) {
        Set<LandmarkRecord> landmarks = new LinkedHashSet<>();
        int minX = view.chunk().minBlockX();
        int maxX = view.chunk().maxBlockX();
        int minZ = view.chunk().minBlockZ();
        int maxZ = view.chunk().maxBlockZ();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = view.minY() + 2; y <= view.maxY() - 4; y++) {
                    if (!WATER.equals(view.getBlockKey(x, y, z))) {
                        continue;
                    }
                    if (!DESERT.equals(view.getBiomeKey(x, y, z))) {
                        continue;
                    }
                    if (!matchesDesertWell(view, x, y, z)) {
                        continue;
                    }
                    BlockBox blockBox = new BlockBox(x - 2, y - 2, z - 2, x + 2, y + 4, z + 2);
                    landmarks.add(new LandmarkRecord(
                        LandmarkType.DESERT_WELL,
                        view.world(),
                        blockBox,
                        new LandmarkSource(
                            LandmarkSourceKind.FEATURE,
                            LandmarkType.DESERT_WELL.getFeatureKey(),
                            view.chunk()
                        )
                    ));
                }
            }
        }
        return landmarks;
    }

    public Set<LandmarkRecord> detectMonsterRooms(FeatureChunkView view) {
        Set<LandmarkRecord> landmarks = new LinkedHashSet<>();
        int minX = view.chunk().minBlockX();
        int maxX = view.chunk().maxBlockX();
        int minZ = view.chunk().minBlockZ();
        int maxZ = view.chunk().maxBlockZ();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = view.minY() + 1; y <= view.maxY() - 3; y++) {
                    if (!SPAWNER.equals(view.getBlockKey(x, y, z))) {
                        continue;
                    }
                    BlockBox blockBox = detectMonsterRoomBox(view, x, y, z);
                    if (blockBox == null) {
                        continue;
                    }
                    landmarks.add(new LandmarkRecord(
                        LandmarkType.MONSTER_ROOM,
                        view.world(),
                        blockBox,
                        new LandmarkSource(
                            LandmarkSourceKind.FEATURE,
                            LandmarkType.MONSTER_ROOM.getFeatureKey(),
                            view.chunk()
                        )
                    ));
                }
            }
        }
        return landmarks;
    }

    private static boolean matchesDesertWell(FeatureChunkView view, int centerX, int waterY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (!SANDSTONE.equals(view.getBlockKey(centerX + dx, waterY - 1, centerZ + dz))) {
                    return false;
                }
                if (!SANDSTONE.equals(view.getBlockKey(centerX + dx, waterY - 2, centerZ + dz))) {
                    return false;
                }
            }
        }

        if (!WATER.equals(view.getBlockKey(centerX, waterY, centerZ)) ||
            !WATER.equals(view.getBlockKey(centerX + 1, waterY, centerZ)) ||
            !WATER.equals(view.getBlockKey(centerX - 1, waterY, centerZ)) ||
            !WATER.equals(view.getBlockKey(centerX, waterY, centerZ + 1)) ||
            !WATER.equals(view.getBlockKey(centerX, waterY, centerZ - 1))) {
            return false;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int absoluteX = Math.abs(dx);
                int absoluteZ = Math.abs(dz);
                if ((dx == 0 && dz == 0) || (absoluteX + absoluteZ == 1)) {
                    continue;
                }
                if (absoluteX == 2 || absoluteZ == 2 || (absoluteX == 1 && absoluteZ == 1)) {
                    if (!SANDSTONE.equals(view.getBlockKey(centerX + dx, waterY, centerZ + dz))) {
                        return false;
                    }
                }
            }
        }

        if (!SANDSTONE_SLAB.equals(view.getBlockKey(centerX + 2, waterY + 1, centerZ)) ||
            !SANDSTONE_SLAB.equals(view.getBlockKey(centerX - 2, waterY + 1, centerZ)) ||
            !SANDSTONE_SLAB.equals(view.getBlockKey(centerX, waterY + 1, centerZ + 2)) ||
            !SANDSTONE_SLAB.equals(view.getBlockKey(centerX, waterY + 1, centerZ - 2))) {
            return false;
        }

        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                for (int y = waterY + 1; y <= waterY + 3; y++) {
                    if (!SANDSTONE.equals(view.getBlockKey(centerX + dx, y, centerZ + dz))) {
                        return false;
                    }
                }
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!SANDSTONE.equals(view.getBlockKey(centerX + dx, waterY + 4, centerZ + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BlockBox detectMonsterRoomBox(FeatureChunkView view, int spawnerX, int spawnerY, int spawnerZ) {
        for (int xRadius = 2; xRadius <= 3; xRadius++) {
            for (int zRadius = 2; zRadius <= 3; zRadius++) {
                BlockBox candidate = new BlockBox(
                    spawnerX - xRadius,
                    spawnerY - 1,
                    spawnerZ - zRadius,
                    spawnerX + xRadius,
                    spawnerY + 3,
                    spawnerZ + zRadius
                );
                if (matchesMonsterRoom(view, candidate, spawnerX, spawnerY, spawnerZ)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean matchesMonsterRoom(
        FeatureChunkView view,
        BlockBox blockBox,
        int spawnerX,
        int spawnerY,
        int spawnerZ
    ) {
        if (!SPAWNER.equals(view.getBlockKey(spawnerX, spawnerY, spawnerZ))) {
            return false;
        }

        int floorCount = 0;
        int ceilingCount = 0;
        int boundarySolidCount = 0;
        int boundaryOpenCount = 0;
        int boundaryTotal = 0;
        int interiorAirLikeCount = 0;
        int interiorTotal = 0;

        for (int x = blockBox.minX(); x <= blockBox.maxX(); x++) {
            for (int z = blockBox.minZ(); z <= blockBox.maxZ(); z++) {
                if (isDungeonSolid(view.getBlockKey(x, blockBox.minY(), z))) {
                    floorCount++;
                }
                if (isDungeonSolid(view.getBlockKey(x, blockBox.maxY(), z))) {
                    ceilingCount++;
                }
            }
        }

        for (int x = blockBox.minX(); x <= blockBox.maxX(); x++) {
            for (int y = blockBox.minY() + 1; y < blockBox.maxY(); y++) {
                for (int z = blockBox.minZ(); z <= blockBox.maxZ(); z++) {
                    boolean boundary = x == blockBox.minX() || x == blockBox.maxX() || z == blockBox.minZ() || z == blockBox.maxZ();
                    String blockKey = view.getBlockKey(x, y, z);
                    if (boundary) {
                        boundaryTotal++;
                        if (isDungeonSolid(blockKey)) {
                            boundarySolidCount++;
                        } else if (isDungeonAirLike(blockKey)) {
                            boundaryOpenCount++;
                        } else {
                            return false;
                        }
                    } else {
                        interiorTotal++;
                        if (isDungeonAirLike(blockKey)) {
                            interiorAirLikeCount++;
                        }
                    }
                }
            }
        }

        int area = (blockBox.maxX() - blockBox.minX() + 1) * (blockBox.maxZ() - blockBox.minZ() + 1);
        if (floorCount < area - 2 || ceilingCount < area - 2) {
            return false;
        }
        if (boundarySolidCount < boundaryTotal - 8 || boundaryOpenCount > 8) {
            return false;
        }
        return interiorAirLikeCount >= Math.max(1, (interiorTotal * 7) / 10);
    }

    private static boolean isDungeonSolid(String blockKey) {
        return COBBLESTONE.equals(blockKey) || MOSSY_COBBLESTONE.equals(blockKey);
    }

    private static boolean isDungeonAirLike(String blockKey) {
        return AIR.equals(blockKey) ||
            CAVE_AIR.equals(blockKey) ||
            SPAWNER.equals(blockKey) ||
            CHEST.equals(blockKey) ||
            COBWEB.equals(blockKey);
    }
}
