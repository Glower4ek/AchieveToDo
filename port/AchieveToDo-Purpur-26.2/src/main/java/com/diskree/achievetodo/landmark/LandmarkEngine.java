package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.AbilityResolvers;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.BlockBox;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class LandmarkEngine implements AutoCloseable {

    private final LandmarkPersistence persistence;
    private final LandmarkIndex runtimeIndex = new LandmarkIndex();
    private final Map<LandmarkChunkKey, Set<LandmarkRecord>> loadedChunkSlices = new LinkedHashMap<>();
    private final Set<LandmarkChunkKey> dirtyChunkSlices = new LinkedHashSet<>();
    private final Map<LandmarkRecord, Integer> activeReferences = new LinkedHashMap<>();

    public LandmarkEngine(LandmarkPersistence persistence) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    public LandmarkIndex runtimeIndex() {
        return runtimeIndex;
    }

    public void loadChunk(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(world, chunk);
        if (loadedChunkSlices.containsKey(chunkKey)) {
            return;
        }
        Set<LandmarkRecord> records = new LinkedHashSet<>(persistence.loadChunk(world, chunk));
        loadedChunkSlices.put(chunkKey, records);
        activate(records);
    }

    public void unloadChunk(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(world, chunk);
        Set<LandmarkRecord> records = loadedChunkSlices.remove(chunkKey);
        if (records == null) {
            return;
        }
        flushChunk(chunkKey, records);
        deactivate(records);
    }

    public boolean synchronizeChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> detectedRecords) throws IOException {
        loadChunk(world, chunk);
        boolean changed = false;
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(world, chunk);
        Set<LandmarkRecord> currentSlice = new LinkedHashSet<>(loadedChunkSlices.get(chunkKey));

        for (LandmarkRecord existing : currentSlice) {
            if (existing.source().kind() == LandmarkSourceKind.FEATURE &&
                existing.source().sourceChunk().equals(chunk)) {
                changed |= removeLandmark(existing);
            }
        }

        for (LandmarkRecord record : detectedRecords) {
            if (record.source().kind() == LandmarkSourceKind.STRUCTURE) {
                Set<LandmarkRecord> currentRecords = new LinkedHashSet<>(slice(world, chunk));
                boolean alreadyPresent = currentRecords.contains(record);
                for (LandmarkRecord existing : currentRecords) {
                    if (existing.source().kind() == LandmarkSourceKind.STRUCTURE &&
                        existing.landmarkType() == record.landmarkType() &&
                        existing.source().sourceKey().equals(record.source().sourceKey()) &&
                        existing.blockBox().equals(record.blockBox()) &&
                        !existing.equals(record)) {
                        changed |= removeLandmark(existing);
                    }
                }
                if (alreadyPresent && !changed) {
                    continue;
                }
            }
            changed |= registerLandmark(record);
        }
        return changed;
    }

    public boolean registerLandmark(LandmarkRecord record) throws IOException {
        boolean changed = false;
        for (ChunkCoordinate intersectingChunk : record.intersectingChunks()) {
            changed |= addToChunkSlice(record, intersectingChunk);
        }
        return changed;
    }

    public boolean removeLandmark(LandmarkRecord record) throws IOException {
        boolean changed = false;
        for (ChunkCoordinate intersectingChunk : record.intersectingChunks()) {
            changed |= removeFromChunkSlice(record, intersectingChunk);
        }
        return changed;
    }

    public Set<LandmarkRecord> slice(LandmarkWorld world, ChunkCoordinate chunk) {
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(world, chunk);
        Set<LandmarkRecord> loadedSlice = loadedChunkSlices.get(chunkKey);
        if (loadedSlice == null) {
            return Set.of();
        }
        return Set.copyOf(loadedSlice);
    }

    public Set<LandmarkRecord> findIntersecting(LandmarkWorld world, BlockBox blockBox) {
        return runtimeIndex.findIntersecting(world, blockBox);
    }

    public Set<LandmarkRecord> findContaining(LandmarkWorld world, int x, int y, int z) {
        return runtimeIndex.findContaining(world, x, y, z);
    }

    public LandmarkQueryResult query(LandmarkWorld world, BlockBox blockBox) {
        return new LandmarkQueryResult(world, blockBox, findIntersecting(world, blockBox));
    }

    public boolean isInsideLockedLandmark(
        LandmarkWorld world,
        BlockBox blockBox,
        Predicate<AbilityType> lockedAbilityPredicate
    ) {
        for (LandmarkRecord record : findIntersecting(world, blockBox)) {
            if (AbilityResolvers.findLandmarkAbility(record.landmarkType()).filter(lockedAbilityPredicate).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public void flush() throws IOException {
        for (Map.Entry<LandmarkChunkKey, Set<LandmarkRecord>> entry : loadedChunkSlices.entrySet()) {
            flushChunk(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    private boolean addToChunkSlice(LandmarkRecord record, ChunkCoordinate chunk) throws IOException {
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(record.world(), chunk);
        Set<LandmarkRecord> loadedSlice = loadedChunkSlices.get(chunkKey);
        if (loadedSlice != null) {
            if (!loadedSlice.add(record)) {
                return false;
            }
            dirtyChunkSlices.add(chunkKey);
            activate(record);
            return true;
        }

        Set<LandmarkRecord> persistedSlice = new LinkedHashSet<>(persistence.loadChunk(record.world(), chunk));
        if (!persistedSlice.add(record)) {
            return false;
        }
        persistence.saveChunk(record.world(), chunk, persistedSlice);
        return true;
    }

    private boolean removeFromChunkSlice(LandmarkRecord record, ChunkCoordinate chunk) throws IOException {
        LandmarkChunkKey chunkKey = new LandmarkChunkKey(record.world(), chunk);
        Set<LandmarkRecord> loadedSlice = loadedChunkSlices.get(chunkKey);
        if (loadedSlice != null) {
            if (!loadedSlice.remove(record)) {
                return false;
            }
            dirtyChunkSlices.add(chunkKey);
            deactivate(record);
            return true;
        }

        Set<LandmarkRecord> persistedSlice = new LinkedHashSet<>(persistence.loadChunk(record.world(), chunk));
        if (!persistedSlice.remove(record)) {
            return false;
        }
        persistence.saveChunk(record.world(), chunk, persistedSlice);
        return true;
    }

    private void activate(Collection<LandmarkRecord> records) {
        for (LandmarkRecord record : records) {
            activate(record);
        }
    }

    private void deactivate(Collection<LandmarkRecord> records) {
        for (LandmarkRecord record : records) {
            deactivate(record);
        }
    }

    private void activate(LandmarkRecord record) {
        Integer current = activeReferences.get(record);
        if (current == null) {
            activeReferences.put(record, 1);
            runtimeIndex.add(record);
            return;
        }
        activeReferences.put(record, current + 1);
    }

    private void deactivate(LandmarkRecord record) {
        Integer current = activeReferences.get(record);
        if (current == null) {
            return;
        }
        if (current <= 1) {
            activeReferences.remove(record);
            runtimeIndex.remove(record);
            return;
        }
        activeReferences.put(record, current - 1);
    }

    private void flushChunk(LandmarkChunkKey chunkKey, Set<LandmarkRecord> records) throws IOException {
        if (!dirtyChunkSlices.remove(chunkKey)) {
            return;
        }
        persistence.saveChunk(chunkKey.world(), chunkKey.chunk(), records);
    }
}
