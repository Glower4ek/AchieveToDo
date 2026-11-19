package com.diskree.achievetodo.landmark.bukkit;

import com.diskree.achievetodo.landmark.ChunkCoordinate;
import com.diskree.achievetodo.landmark.FeatureChunkView;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import org.bukkit.World;

final class BukkitFeatureChunkView implements FeatureChunkView {

    private final World world;
    private final LandmarkWorld landmarkWorld;
    private final ChunkCoordinate chunk;

    BukkitFeatureChunkView(World world, LandmarkWorld landmarkWorld, ChunkCoordinate chunk) {
        this.world = world;
        this.landmarkWorld = landmarkWorld;
        this.chunk = chunk;
    }

    @Override
    public LandmarkWorld world() {
        return landmarkWorld;
    }

    @Override
    public ChunkCoordinate chunk() {
        return chunk;
    }

    @Override
    public int minY() {
        return world.getMinHeight();
    }

    @Override
    public int maxY() {
        return world.getMaxHeight() - 1;
    }

    @Override
    public String getBlockKey(int x, int y, int z) {
        if (y < minY() || y > maxY()) {
            return "minecraft:air";
        }
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return "minecraft:air";
        }
        return world.getBlockAt(x, y, z).getType().getKey().toString();
    }

    @Override
    public String getBiomeKey(int x, int y, int z) {
        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return "minecraft:plains";
        }
        return world.getBiome(x, y, z).getKey().toString();
    }
}
