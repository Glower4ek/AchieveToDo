package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.LandmarkType;

import java.util.Optional;

public final class StructureLandmarkDetector {

    public Optional<LandmarkRecord> detect(LandmarkWorld world, StructureLandmarkDescriptor descriptor) {
        Optional<LandmarkType> landmarkType = LandmarkType.findByStructureKey(descriptor.structureKey());
        if (landmarkType.isEmpty()) {
            return Optional.empty();
        }
        BlockBox blockBox = resolveBoundingBox(descriptor);
        if (blockBox == null) {
            return Optional.empty();
        }
        return Optional.of(new LandmarkRecord(
            landmarkType.get(),
            world,
            blockBox,
            new LandmarkSource(
                LandmarkSourceKind.STRUCTURE,
                descriptor.structureKey(),
                descriptor.sourceChunk()
            )
        ));
    }

    private static BlockBox resolveBoundingBox(StructureLandmarkDescriptor descriptor) {
        BlockBox blockBox = null;
        for (BlockBox pieceBoundingBox : descriptor.pieceBoundingBoxes()) {
            blockBox = blockBox == null ? pieceBoundingBox : blockBox.union(pieceBoundingBox);
        }
        return blockBox != null ? blockBox : descriptor.boundingBox();
    }

}
