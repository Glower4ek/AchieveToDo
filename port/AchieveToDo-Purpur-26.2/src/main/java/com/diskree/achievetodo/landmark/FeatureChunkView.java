package com.diskree.achievetodo.landmark;

public interface FeatureChunkView {

    LandmarkWorld world();

    ChunkCoordinate chunk();

    int minY();

    int maxY();

    String getBlockKey(int x, int y, int z);

    String getBiomeKey(int x, int y, int z);
}
