package com.diskree.achievetodo.landmark;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface LandmarkPersistence {

    Map<ChunkCoordinate, Set<LandmarkRecord>> loadWorld(LandmarkWorld world) throws IOException;

    void saveWorld(LandmarkWorld world, Map<ChunkCoordinate, ? extends Collection<LandmarkRecord>> chunks) throws IOException;

    Set<LandmarkRecord> loadChunk(LandmarkWorld world, ChunkCoordinate chunk) throws IOException;

    void saveChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) throws IOException;

    default boolean isFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        return false;
    }

    default void markFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
    }
}
