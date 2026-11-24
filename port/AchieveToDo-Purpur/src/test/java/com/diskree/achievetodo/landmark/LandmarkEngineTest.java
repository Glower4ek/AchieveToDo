package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.tests.TestSupport;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class LandmarkEngineTest {

    private LandmarkEngineTest() {
    }

    public static void run() {
        verifyCrossChunkPersistenceAndRefCounting();
        verifyLockedLandmarkQuery();
        verifyChunkReloadRestoresPersistedIntersectingSlices();
        verifyFeatureOnlySyncPreservesStructureRegistration();
    }

    private static void verifyCrossChunkPersistenceAndRefCounting() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        LandmarkEngine engine = new LandmarkEngine(persistence);
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkRecord record = new LandmarkRecord(
            LandmarkType.VILLAGE,
            world,
            new BlockBox(15, 64, 15, 17, 70, 17),
            new LandmarkSource(LandmarkSourceKind.STRUCTURE, "minecraft:village_plains", new ChunkCoordinate(0, 0))
        );
        try {
            TestSupport.assertTrue(engine.registerLandmark(record));
            TestSupport.assertEquals(Set.of(record), persistence.loadChunk(world, new ChunkCoordinate(0, 0)));
            TestSupport.assertEquals(Set.of(record), persistence.loadChunk(world, new ChunkCoordinate(1, 1)));

            engine.loadChunk(world, new ChunkCoordinate(0, 0));
            engine.loadChunk(world, new ChunkCoordinate(1, 1));
            TestSupport.assertEquals(1, engine.runtimeIndex().size());

            engine.unloadChunk(world, new ChunkCoordinate(0, 0));
            TestSupport.assertEquals(1, engine.runtimeIndex().size());

            engine.unloadChunk(world, new ChunkCoordinate(1, 1));
            TestSupport.assertEquals(0, engine.runtimeIndex().size());
        } catch (IOException exception) {
            throw new AssertionError("Unexpected engine IO failure", exception);
        }
    }

    private static void verifyLockedLandmarkQuery() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        LandmarkEngine engine = new LandmarkEngine(persistence);
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkRecord record = new LandmarkRecord(
            LandmarkType.VILLAGE,
            world,
            new BlockBox(0, 60, 0, 10, 70, 10),
            new LandmarkSource(LandmarkSourceKind.STRUCTURE, "minecraft:village_plains", new ChunkCoordinate(0, 0))
        );
        try {
            engine.registerLandmark(record);
            engine.loadChunk(world, new ChunkCoordinate(0, 0));
            TestSupport.assertTrue(engine.isInsideLockedLandmark(
                world,
                new BlockBox(1, 64, 1, 2, 65, 2),
                abilityType -> abilityType == AbilityType.INTERACT_INSIDE_VILLAGE
            ));
            TestSupport.assertFalse(engine.isInsideLockedLandmark(
                world,
                new BlockBox(20, 64, 20, 21, 65, 21),
                abilityType -> abilityType == AbilityType.INTERACT_INSIDE_VILLAGE
            ));
        } catch (IOException exception) {
            throw new AssertionError("Unexpected engine IO failure", exception);
        }
    }

    private static void verifyChunkReloadRestoresPersistedIntersectingSlices() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        LandmarkRecord record = new LandmarkRecord(
            LandmarkType.VILLAGE,
            world,
            new BlockBox(15, 64, 15, 17, 70, 17),
            new LandmarkSource(LandmarkSourceKind.STRUCTURE, "minecraft:village_plains", new ChunkCoordinate(0, 0))
        );
        try {
            LandmarkEngine writer = new LandmarkEngine(persistence);
            writer.registerLandmark(record);
            writer.loadChunk(world, new ChunkCoordinate(0, 0));
            writer.loadChunk(world, new ChunkCoordinate(1, 1));
            writer.close();

            LandmarkEngine reader = new LandmarkEngine(persistence);
            reader.loadChunk(world, new ChunkCoordinate(1, 1));
            TestSupport.assertEquals(Set.of(record), reader.findContaining(world, 17, 65, 17));
            reader.unloadChunk(world, new ChunkCoordinate(1, 1));
            TestSupport.assertEquals(0, reader.runtimeIndex().size());
        } catch (IOException exception) {
            throw new AssertionError("Unexpected engine IO failure", exception);
        }
    }

    private static void verifyFeatureOnlySyncPreservesStructureRegistration() {
        InMemoryPersistence persistence = new InMemoryPersistence();
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);
        StructureLandmarkDetector detector = new StructureLandmarkDetector();
        LandmarkRecord record = detector.detect(
            world,
            StructureLandmarkDescriptor.of(
                "minecraft:village_desert",
                new ChunkCoordinate(7, 9),
                new BlockBox(120, 64, 152, 136, 76, 168),
                Set.of(new BlockBox(120, 64, 152, 136, 76, 168))
            )
        ).orElseThrow();
        try {
            LandmarkEngine engine = new LandmarkEngine(persistence);
            TestSupport.assertTrue(engine.registerLandmark(record));
            TestSupport.assertEquals(new ChunkCoordinate(7, 9), record.source().sourceChunk());
            TestSupport.assertEquals(Set.of(record), persistence.loadChunk(world, new ChunkCoordinate(7, 9)));
            TestSupport.assertEquals(Set.of(record), persistence.loadChunk(world, new ChunkCoordinate(8, 10)));

            TestSupport.assertFalse(engine.synchronizeChunk(world, new ChunkCoordinate(7, 9), Set.of()));
            engine.loadChunk(world, new ChunkCoordinate(8, 10));
            TestSupport.assertEquals(Set.of(record), engine.findContaining(world, 129, 70, 160));
        } catch (IOException exception) {
            throw new AssertionError("Unexpected engine IO failure", exception);
        }
    }

    private static final class InMemoryPersistence implements LandmarkPersistence {

        private final Map<LandmarkChunkKey, Set<LandmarkRecord>> slices = new LinkedHashMap<>();

        @Override
        public Map<ChunkCoordinate, Set<LandmarkRecord>> loadWorld(LandmarkWorld world) {
            Map<ChunkCoordinate, Set<LandmarkRecord>> results = new LinkedHashMap<>();
            for (Map.Entry<LandmarkChunkKey, Set<LandmarkRecord>> entry : slices.entrySet()) {
                if (entry.getKey().world().equals(world)) {
                    results.put(entry.getKey().chunk(), Set.copyOf(entry.getValue()));
                }
            }
            return results;
        }

        @Override
        public void saveWorld(LandmarkWorld world, Map<ChunkCoordinate, ? extends Collection<LandmarkRecord>> chunks) {
            slices.entrySet().removeIf(entry -> entry.getKey().world().equals(world));
            for (Map.Entry<ChunkCoordinate, ? extends Collection<LandmarkRecord>> entry : chunks.entrySet()) {
                saveChunk(world, entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Set<LandmarkRecord> loadChunk(LandmarkWorld world, ChunkCoordinate chunk) {
            return Set.copyOf(slices.getOrDefault(new LandmarkChunkKey(world, chunk), Set.of()));
        }

        @Override
        public void saveChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) {
            LandmarkChunkKey key = new LandmarkChunkKey(world, chunk);
            if (records.isEmpty()) {
                slices.remove(key);
                return;
            }
            slices.put(key, new LinkedHashSet<>(records));
        }
    }
}
