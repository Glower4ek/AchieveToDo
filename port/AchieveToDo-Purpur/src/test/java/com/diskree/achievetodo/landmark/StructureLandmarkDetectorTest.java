package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.tests.TestSupport;

import java.util.Optional;
import java.util.Set;

public final class StructureLandmarkDetectorTest {

    private StructureLandmarkDetectorTest() {
    }

    public static void run() {
        verifyPieceUnionPreferredOverFallbackBounds();
        verifySourceChunkPreserved();
        verifyFallbackBoundsUsedWhenPiecesMissing();
    }

    private static void verifyPieceUnionPreferredOverFallbackBounds() {
        StructureLandmarkDetector detector = new StructureLandmarkDetector();
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);

        Optional<LandmarkRecord> village = detector.detect(
            world,
            StructureLandmarkDescriptor.of(
                "minecraft:village_desert",
                new ChunkCoordinate(7, 9),
                new BlockBox(100, 64, 100, 120, 75, 125),
                Set.of(
                    new BlockBox(100, 64, 100, 110, 70, 110),
                    new BlockBox(112, 64, 111, 124, 76, 126)
                )
            )
        );
        TestSupport.assertTrue(village.isPresent());
        TestSupport.assertEquals(LandmarkType.VILLAGE, village.get().landmarkType());
        TestSupport.assertEquals(new BlockBox(100, 64, 100, 124, 76, 126), village.get().blockBox());
    }

    private static void verifySourceChunkPreserved() {
        StructureLandmarkDetector detector = new StructureLandmarkDetector();
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);

        Optional<LandmarkRecord> village = detector.detect(
            world,
            StructureLandmarkDescriptor.of(
                "minecraft:village_desert",
                new ChunkCoordinate(7, 9),
                new BlockBox(100, 64, 100, 120, 75, 125),
                Set.of(new BlockBox(104, 64, 104, 108, 68, 108))
            )
        );
        TestSupport.assertTrue(village.isPresent());
        TestSupport.assertEquals(new ChunkCoordinate(7, 9), village.get().source().sourceChunk());
    }

    private static void verifyFallbackBoundsUsedWhenPiecesMissing() {
        StructureLandmarkDetector detector = new StructureLandmarkDetector();
        LandmarkWorld world = new LandmarkWorld("world", DimensionType.OVERWORLD);

        Optional<LandmarkRecord> unknown = detector.detect(
            world,
            StructureLandmarkDescriptor.of("minecraft:unknown_structure", new ChunkCoordinate(0, 0), new BlockBox(0, 0, 0, 1, 1, 1), Set.of())
        );
        TestSupport.assertTrue(unknown.isEmpty());

        Optional<LandmarkRecord> stronghold = detector.detect(
            world,
            StructureLandmarkDescriptor.of(
                "minecraft:stronghold",
                new ChunkCoordinate(2, 3),
                new BlockBox(32, 10, 48, 80, 40, 96),
                Set.of()
            )
        );
        TestSupport.assertTrue(stronghold.isPresent());
        TestSupport.assertEquals(new BlockBox(32, 10, 48, 80, 40, 96), stronghold.get().blockBox());
    }
}
