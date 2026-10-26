package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.tests.TestSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class LandmarkModelAndPersistenceTest {

    private LandmarkModelAndPersistenceTest() {
    }

    public static void run() {
        verifyDeduplication();
        verifyMultipleBoxesPerType();
        verifyDifferentDimensionsDoNotIntersect();
        verifyBoxIntersectionAndContainingQueries();
        verifyChunkIndexing();
        verifySerializationRoundTrip();
        verifyFeatureScanMarkerRoundTrip();
        verifyDeleteOnlyRemovesRequestedLandmark();
    }

    private static void verifyDeduplication() {
        LandmarkIndex index = new LandmarkIndex();
        LandmarkRecord record = village(new LandmarkWorld("world", DimensionType.OVERWORLD), new BlockBox(0, 60, 0, 10, 70, 10));
        TestSupport.assertTrue(index.add(record));
        TestSupport.assertFalse(index.add(record));
        TestSupport.assertEquals(1, index.size());
    }

    private static void verifyMultipleBoxesPerType() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkIndex index = new LandmarkIndex();
        LandmarkRecord first = village(world, new BlockBox(0, 60, 0, 10, 70, 10));
        LandmarkRecord second = village(world, new BlockBox(40, 60, 40, 50, 70, 50));
        index.add(first);
        index.add(second);
        TestSupport.assertEquals(Set.of(first, second), index.findByWorld(world));
    }

    private static void verifyDifferentDimensionsDoNotIntersect() {
        LandmarkIndex index = new LandmarkIndex();
        LandmarkRecord overworldVillage = village(new LandmarkWorld("world", DimensionType.OVERWORLD), new BlockBox(0, 60, 0, 10, 70, 10));
        LandmarkRecord netherVillage = village(new LandmarkWorld("world_nether", DimensionType.NETHER), new BlockBox(0, 60, 0, 10, 70, 10));
        index.add(overworldVillage);
        index.add(netherVillage);
        TestSupport.assertEquals(Set.of(overworldVillage), index.findIntersecting(overworldVillage.world(), new BlockBox(5, 60, 5, 6, 61, 6)));
        TestSupport.assertEquals(Set.of(netherVillage), index.findIntersecting(netherVillage.world(), new BlockBox(5, 60, 5, 6, 61, 6)));
        TestSupport.assertEquals(Set.of(overworldVillage), index.findByDimension(DimensionType.OVERWORLD));
        TestSupport.assertEquals(Set.of(netherVillage), index.findByDimension(DimensionType.NETHER));
    }

    private static void verifyBoxIntersectionAndContainingQueries() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkIndex index = new LandmarkIndex();
        LandmarkRecord record = village(world, new BlockBox(0, 60, 0, 10, 70, 10));
        index.add(record);
        TestSupport.assertEquals(Set.of(record), index.findIntersecting(world, new BlockBox(10, 70, 10, 20, 80, 20)));
        TestSupport.assertEquals(Set.of(record), index.findContaining(world, 3, 64, 3));
        TestSupport.assertEquals(Set.of(), index.findContaining(world, 30, 64, 30));
    }

    private static void verifyChunkIndexing() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkRecord record = village(world, new BlockBox(15, 50, 15, 17, 55, 17));
        LandmarkIndex index = new LandmarkIndex();
        index.add(record);
        TestSupport.assertEquals(Set.of(
            new ChunkCoordinate(0, 0),
            new ChunkCoordinate(0, 1),
            new ChunkCoordinate(1, 0),
            new ChunkCoordinate(1, 1)
        ), record.intersectingChunks());
        TestSupport.assertEquals(Set.of(record), index.findByChunk(world, new ChunkCoordinate(0, 0)));
        TestSupport.assertEquals(Set.of(record), index.findByChunk(world, new ChunkCoordinate(1, 1)));
    }

    private static void verifySerializationRoundTrip() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        ChunkCoordinate chunk = new ChunkCoordinate(3, -2);
        Set<LandmarkRecord> records = Set.of(
            village(world, new BlockBox(48, 64, -32, 58, 72, -22)),
            new LandmarkRecord(
                LandmarkType.MONSTER_ROOM,
                world,
                new BlockBox(50, 20, -30, 56, 24, -24),
                new LandmarkSource(LandmarkSourceKind.FEATURE, "minecraft:monster_room", new ChunkCoordinate(3, -2))
            )
        );
        String encoded = LandmarkChunkFileCodec.encode(world, chunk, records);
        TestSupport.assertEquals(records, LandmarkChunkFileCodec.decode(world, chunk, encoded));

        try {
            Path tempDirectory = Files.createTempDirectory("achievetodo-landmarks-test");
            WorldOwnedLandmarkPersistence persistence = new WorldOwnedLandmarkPersistence(tempDirectory);
            persistence.saveChunk(world, chunk, records);
            TestSupport.assertEquals(records, persistence.loadChunk(world, chunk));

            persistence.saveWorld(world, Map.of(
                chunk, records,
                new ChunkCoordinate(4, -2), Set.of(village(world, new BlockBox(64, 64, -32, 70, 72, -26)))
            ));
            Map<ChunkCoordinate, Set<LandmarkRecord>> loadedWorld = persistence.loadWorld(world);
            TestSupport.assertEquals(2, loadedWorld.size());
            TestSupport.assertEquals(records, loadedWorld.get(chunk));
        } catch (IOException exception) {
            throw new AssertionError("Unexpected landmark persistence IO failure", exception);
        }
    }

    private static void verifyDeleteOnlyRemovesRequestedLandmark() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkIndex index = new LandmarkIndex();
        LandmarkRecord first = village(world, new BlockBox(0, 60, 0, 10, 70, 10));
        LandmarkRecord second = village(world, new BlockBox(40, 60, 40, 50, 70, 50));
        index.add(first);
        index.add(second);
        TestSupport.assertTrue(index.remove(first));
        TestSupport.assertEquals(Set.of(second), index.findByWorld(world));
        TestSupport.assertEquals(Set.of(), index.findByChunk(world, new ChunkCoordinate(0, 0)));
        TestSupport.assertEquals(Set.of(second), index.findByChunk(world, new ChunkCoordinate(2, 2)));
    }

    private static void verifyFeatureScanMarkerRoundTrip() {
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        ChunkCoordinate chunk = new ChunkCoordinate(12, -5);
        try {
            Path tempDirectory = Files.createTempDirectory("achievetodo-feature-scan-marker-test");
            WorldOwnedLandmarkPersistence persistence = new WorldOwnedLandmarkPersistence(tempDirectory);
            TestSupport.assertFalse(persistence.isFeatureScanComplete(world, chunk));
            persistence.markFeatureScanComplete(world, chunk);
            TestSupport.assertTrue(persistence.isFeatureScanComplete(world, chunk));
            TestSupport.assertEquals(Set.of(), persistence.loadChunk(world, chunk));
        } catch (IOException exception) {
            throw new AssertionError("Unexpected feature scan marker IO failure", exception);
        }
    }

    private static LandmarkRecord village(LandmarkWorld world, BlockBox blockBox) {
        return new LandmarkRecord(
            LandmarkType.VILLAGE,
            world,
            blockBox,
            new LandmarkSource(LandmarkSourceKind.STRUCTURE, "minecraft:village_plains", ChunkCoordinate.fromBlock(blockBox.minX(), blockBox.minZ()))
        );
    }
}
