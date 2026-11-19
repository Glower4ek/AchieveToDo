package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.tests.TestSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FeatureLandmarkDetectorTest {

    private FeatureLandmarkDetectorTest() {
    }

    public static void run() {
        verifyDesertWellDetection();
        verifyMonsterRoomDetection();
        verifyFalsePositiveResistance();
        verifyChunkBorderSourceChunkOwnership();
    }

    private static void verifyDesertWellDetection() {
        FeatureLandmarkDetector detector = new FeatureLandmarkDetector();
        FakeFeatureChunkView view = new FakeFeatureChunkView(new LandmarkWorld("world", DimensionType.OVERWORLD), new ChunkCoordinate(0, 0), -64, 320);
        placeDesertWell(view, 8, 70, 8);

        Set<LandmarkRecord> landmarks = detector.detectDesertWells(view);
        TestSupport.assertEquals(1, landmarks.size());
        LandmarkRecord landmark = landmarks.iterator().next();
        TestSupport.assertEquals(LandmarkType.DESERT_WELL, landmark.landmarkType());
        TestSupport.assertEquals(new BlockBox(6, 68, 6, 10, 74, 10), landmark.blockBox());
    }

    private static void verifyMonsterRoomDetection() {
        FeatureLandmarkDetector detector = new FeatureLandmarkDetector();
        FakeFeatureChunkView view = new FakeFeatureChunkView(new LandmarkWorld("world", DimensionType.OVERWORLD), new ChunkCoordinate(0, 0), -64, 320);
        placeMonsterRoom(view, 6, 20, 6, 2, 3, 0);
        placeMonsterRoom(view, 11, 30, 11, 3, 2, 2);

        Set<LandmarkRecord> landmarks = detector.detectMonsterRooms(view);
        TestSupport.assertEquals(2, landmarks.size());
    }

    private static void verifyFalsePositiveResistance() {
        FeatureLandmarkDetector detector = new FeatureLandmarkDetector();
        FakeFeatureChunkView wrongBiome = new FakeFeatureChunkView(new LandmarkWorld("world", DimensionType.OVERWORLD), new ChunkCoordinate(0, 0), -64, 320);
        placeDesertWell(wrongBiome, 8, 70, 8);
        wrongBiome.fillBiome(new BlockBox(6, 68, 6, 10, 74, 10), "minecraft:plains");
        TestSupport.assertEquals(Set.of(), detector.detectDesertWells(wrongBiome));

        FakeFeatureChunkView brokenDungeon = new FakeFeatureChunkView(new LandmarkWorld("world", DimensionType.OVERWORLD), new ChunkCoordinate(0, 0), -64, 320);
        placeMonsterRoom(brokenDungeon, 8, 25, 8, 2, 2, 1);
        brokenDungeon.setBlock(6, 28, 8, "minecraft:air");
        brokenDungeon.setBlock(7, 28, 8, "minecraft:air");
        brokenDungeon.setBlock(8, 28, 8, "minecraft:air");
        brokenDungeon.setBlock(9, 28, 8, "minecraft:air");
        TestSupport.assertEquals(Set.of(), detector.detectMonsterRooms(brokenDungeon));
    }

    private static void verifyChunkBorderSourceChunkOwnership() {
        FeatureLandmarkDetector detector = new FeatureLandmarkDetector();

        FakeFeatureChunkView desertWellView = new FakeFeatureChunkView(
            new LandmarkWorld("world", DimensionType.OVERWORLD),
            new ChunkCoordinate(1, 0),
            -64,
            320
        );
        placeDesertWell(desertWellView, 17, 70, 8);
        Set<LandmarkRecord> desertWells = detector.detectDesertWells(desertWellView);
        TestSupport.assertEquals(1, desertWells.size());
        TestSupport.assertEquals(new ChunkCoordinate(1, 0), desertWells.iterator().next().source().sourceChunk());

        FakeFeatureChunkView monsterRoomView = new FakeFeatureChunkView(
            new LandmarkWorld("world", DimensionType.OVERWORLD),
            new ChunkCoordinate(1, 0),
            -64,
            320
        );
        placeMonsterRoom(monsterRoomView, 17, 20, 8, 3, 2, 1);
        Set<LandmarkRecord> monsterRooms = detector.detectMonsterRooms(monsterRoomView);
        TestSupport.assertEquals(1, monsterRooms.size());
        TestSupport.assertEquals(new ChunkCoordinate(1, 0), monsterRooms.iterator().next().source().sourceChunk());
    }

    private static void placeDesertWell(FakeFeatureChunkView view, int centerX, int waterY, int centerZ) {
        view.fillBiome(new BlockBox(centerX - 2, waterY - 2, centerZ - 2, centerX + 2, waterY + 4, centerZ + 2), "minecraft:desert");
        view.fill(new BlockBox(centerX - 2, waterY - 2, centerZ - 2, centerX + 2, waterY - 1, centerZ + 2), "minecraft:sandstone");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int absoluteX = Math.abs(dx);
                int absoluteZ = Math.abs(dz);
                if ((dx == 0 && dz == 0) || (absoluteX + absoluteZ == 1)) {
                    view.setBlock(centerX + dx, waterY, centerZ + dz, "minecraft:water");
                } else if (absoluteX == 2 || absoluteZ == 2 || (absoluteX == 1 && absoluteZ == 1)) {
                    view.setBlock(centerX + dx, waterY, centerZ + dz, "minecraft:sandstone");
                }
            }
        }
        view.setBlock(centerX + 2, waterY + 1, centerZ, "minecraft:sandstone_slab");
        view.setBlock(centerX - 2, waterY + 1, centerZ, "minecraft:sandstone_slab");
        view.setBlock(centerX, waterY + 1, centerZ + 2, "minecraft:sandstone_slab");
        view.setBlock(centerX, waterY + 1, centerZ - 2, "minecraft:sandstone_slab");
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                for (int y = waterY + 1; y <= waterY + 3; y++) {
                    view.setBlock(centerX + dx, y, centerZ + dz, "minecraft:sandstone");
                }
            }
        }
        view.fill(new BlockBox(centerX - 1, waterY + 4, centerZ - 1, centerX + 1, waterY + 4, centerZ + 1), "minecraft:sandstone");
    }

    private static void placeMonsterRoom(
        FakeFeatureChunkView view,
        int spawnerX,
        int spawnerY,
        int spawnerZ,
        int xRadius,
        int zRadius,
        int chestCount
    ) {
        BlockBox blockBox = new BlockBox(
            spawnerX - xRadius,
            spawnerY - 1,
            spawnerZ - zRadius,
            spawnerX + xRadius,
            spawnerY + 3,
            spawnerZ + zRadius
        );
        for (int x = blockBox.minX(); x <= blockBox.maxX(); x++) {
            for (int y = blockBox.minY(); y <= blockBox.maxY(); y++) {
                for (int z = blockBox.minZ(); z <= blockBox.maxZ(); z++) {
                    boolean boundary = y == blockBox.minY() || y == blockBox.maxY() ||
                        x == blockBox.minX() || x == blockBox.maxX() ||
                        z == blockBox.minZ() || z == blockBox.maxZ();
                    view.setBlock(x, y, z, boundary ? "minecraft:cobblestone" : "minecraft:air");
                }
            }
        }
        view.setBlock(spawnerX, spawnerY, spawnerZ, "minecraft:spawner");
        if (chestCount > 0) {
            view.setBlock(blockBox.minX() + 1, spawnerY, spawnerZ, "minecraft:chest");
        }
        if (chestCount > 1) {
            view.setBlock(blockBox.maxX() - 1, spawnerY, spawnerZ, "minecraft:chest");
        }
        view.setBlock(blockBox.minX(), spawnerY, spawnerZ, "minecraft:air");
        view.setBlock(blockBox.maxX(), spawnerY, spawnerZ, "minecraft:air");
    }

    private static final class FakeFeatureChunkView implements FeatureChunkView {

        private final LandmarkWorld world;
        private final ChunkCoordinate chunk;
        private final int minY;
        private final int maxY;
        private final Map<String, String> blocks = new HashMap<>();
        private final Map<String, String> biomes = new HashMap<>();

        private FakeFeatureChunkView(LandmarkWorld world, ChunkCoordinate chunk, int minY, int maxY) {
            this.world = world;
            this.chunk = chunk;
            this.minY = minY;
            this.maxY = maxY;
        }

        @Override
        public LandmarkWorld world() {
            return world;
        }

        @Override
        public ChunkCoordinate chunk() {
            return chunk;
        }

        @Override
        public int minY() {
            return minY;
        }

        @Override
        public int maxY() {
            return maxY;
        }

        @Override
        public String getBlockKey(int x, int y, int z) {
            return blocks.getOrDefault(key(x, y, z), "minecraft:air");
        }

        @Override
        public String getBiomeKey(int x, int y, int z) {
            return biomes.getOrDefault(key(x, y, z), "minecraft:plains");
        }

        private void setBlock(int x, int y, int z, String blockKey) {
            blocks.put(key(x, y, z), blockKey);
        }

        private void fill(BlockBox blockBox, String blockKey) {
            for (int x = blockBox.minX(); x <= blockBox.maxX(); x++) {
                for (int y = blockBox.minY(); y <= blockBox.maxY(); y++) {
                    for (int z = blockBox.minZ(); z <= blockBox.maxZ(); z++) {
                        setBlock(x, y, z, blockKey);
                    }
                }
            }
        }

        private void fillBiome(BlockBox blockBox, String biomeKey) {
            for (int x = blockBox.minX(); x <= blockBox.maxX(); x++) {
                for (int y = blockBox.minY(); y <= blockBox.maxY(); y++) {
                    for (int z = blockBox.minZ(); z <= blockBox.maxZ(); z++) {
                        biomes.put(key(x, y, z), biomeKey);
                    }
                }
            }
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }
}
