package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.LandmarkType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record LandmarkRecord(
    LandmarkType landmarkType,
    LandmarkWorld world,
    BlockBox blockBox,
    LandmarkSource source
) {

    public LandmarkRecord {
        landmarkType = Objects.requireNonNull(landmarkType, "landmarkType");
        world = Objects.requireNonNull(world, "world");
        blockBox = Objects.requireNonNull(blockBox, "blockBox");
        source = Objects.requireNonNull(source, "source");
    }

    public boolean contains(int x, int y, int z) {
        return blockBox.contains(x, y, z);
    }

    public boolean intersects(BlockBox targetBox) {
        return blockBox.intersects(targetBox);
    }

    public Set<ChunkCoordinate> intersectingChunks() {
        int minChunkX = Math.floorDiv(blockBox.minX(), 16);
        int maxChunkX = Math.floorDiv(blockBox.maxX(), 16);
        int minChunkZ = Math.floorDiv(blockBox.minZ(), 16);
        int maxChunkZ = Math.floorDiv(blockBox.maxZ(), 16);
        Set<ChunkCoordinate> chunks = new LinkedHashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkCoordinate(chunkX, chunkZ));
            }
        }
        return chunks;
    }
}
