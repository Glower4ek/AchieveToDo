package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record StructureLandmarkDescriptor(
    String structureKey,
    ChunkCoordinate sourceChunk,
    BlockBox boundingBox,
    Set<BlockBox> pieceBoundingBoxes
) {

    public StructureLandmarkDescriptor {
        structureKey = Objects.requireNonNull(structureKey, "structureKey");
        sourceChunk = Objects.requireNonNull(sourceChunk, "sourceChunk");
        pieceBoundingBoxes = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(pieceBoundingBoxes, "pieceBoundingBoxes")));
    }

    public static StructureLandmarkDescriptor of(
        String structureKey,
        ChunkCoordinate sourceChunk,
        BlockBox boundingBox,
        Collection<BlockBox> pieceBoundingBoxes
    ) {
        return new StructureLandmarkDescriptor(structureKey, sourceChunk, boundingBox, new LinkedHashSet<>(pieceBoundingBoxes));
    }
}
