package com.diskree.achievetodo.landmark;

import java.util.Objects;

public record LandmarkChunkKey(LandmarkWorld world, ChunkCoordinate chunk) {

    public LandmarkChunkKey {
        world = Objects.requireNonNull(world, "world");
        chunk = Objects.requireNonNull(chunk, "chunk");
    }
}
