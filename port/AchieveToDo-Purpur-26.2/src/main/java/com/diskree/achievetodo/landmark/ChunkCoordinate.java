package com.diskree.achievetodo.landmark;

public record ChunkCoordinate(int x, int z) {

    public static ChunkCoordinate fromBlock(int blockX, int blockZ) {
        return new ChunkCoordinate(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }

    public int minBlockX() {
        return x << 4;
    }

    public int maxBlockX() {
        return minBlockX() + 15;
    }

    public int minBlockZ() {
        return z << 4;
    }

    public int maxBlockZ() {
        return minBlockZ() + 15;
    }
}
