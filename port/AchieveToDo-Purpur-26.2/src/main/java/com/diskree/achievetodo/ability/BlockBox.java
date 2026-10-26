package com.diskree.achievetodo.ability;

public record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public BlockBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("BlockBox minimum bounds must not exceed maximum bounds");
        }
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX &&
            y >= minY && y <= maxY &&
            z >= minZ && z <= maxZ;
    }

    public boolean intersects(BlockBox other) {
        return other != null &&
            minX <= other.maxX &&
            maxX >= other.minX &&
            minY <= other.maxY &&
            maxY >= other.minY &&
            minZ <= other.maxZ &&
            maxZ >= other.minZ;
    }

    public BlockBox union(BlockBox other) {
        if (other == null) {
            return this;
        }
        return new BlockBox(
            Math.min(minX, other.minX),
            Math.min(minY, other.minY),
            Math.min(minZ, other.minZ),
            Math.max(maxX, other.maxX),
            Math.max(maxY, other.maxY),
            Math.max(maxZ, other.maxZ)
        );
    }
}
