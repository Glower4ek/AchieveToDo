package com.diskree.achievetodo.landmark.bukkit;

import com.diskree.achievetodo.landmark.ChunkCoordinate;
import com.diskree.achievetodo.landmark.LandmarkPersistence;
import com.diskree.achievetodo.landmark.LandmarkRecord;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import com.diskree.achievetodo.landmark.WorldOwnedLandmarkPersistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class BukkitWorldFolderLandmarkPersistence implements LandmarkPersistence {

    private final Map<String, WorldOwnedLandmarkPersistence> delegates = new LinkedHashMap<>();

    public void register(LandmarkWorld world, Path worldFolder) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(worldFolder, "worldFolder");
        delegates.computeIfAbsent(
            world.worldId(),
            ignored -> new WorldOwnedLandmarkPersistence(worldFolder.resolve("data").resolve("achievetodo-landmarks"))
        );
    }

    @Override
    public Map<ChunkCoordinate, Set<LandmarkRecord>> loadWorld(LandmarkWorld world) throws IOException {
        return delegate(world).loadWorld(world);
    }

    @Override
    public void saveWorld(LandmarkWorld world, Map<ChunkCoordinate, ? extends Collection<LandmarkRecord>> chunks) throws IOException {
        delegate(world).saveWorld(world, chunks);
    }

    @Override
    public Set<LandmarkRecord> loadChunk(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        return delegate(world).loadChunk(world, chunk);
    }

    @Override
    public void saveChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) throws IOException {
        delegate(world).saveChunk(world, chunk, records);
    }

    @Override
    public boolean isFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        return delegate(world).isFeatureScanComplete(world, chunk);
    }

    @Override
    public void markFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        delegate(world).markFeatureScanComplete(world, chunk);
    }

    private WorldOwnedLandmarkPersistence delegate(LandmarkWorld world) {
        WorldOwnedLandmarkPersistence persistence = delegates.get(world.worldId());
        if (persistence == null) {
            throw new IllegalStateException("World persistence is not registered for " + world.worldId());
        }
        return persistence;
    }
}
