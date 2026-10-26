package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class LandmarkIndex {

    private static final Comparator<LandmarkRecord> RECORD_ORDER = Comparator
        .comparing((LandmarkRecord record) -> record.landmarkType().ordinal())
        .thenComparing(record -> record.source().kind().ordinal())
        .thenComparing(record -> record.source().sourceKey())
        .thenComparingInt(record -> record.source().sourceChunk().x())
        .thenComparingInt(record -> record.source().sourceChunk().z())
        .thenComparingInt(record -> record.blockBox().minX())
        .thenComparingInt(record -> record.blockBox().minY())
        .thenComparingInt(record -> record.blockBox().minZ())
        .thenComparingInt(record -> record.blockBox().maxX())
        .thenComparingInt(record -> record.blockBox().maxY())
        .thenComparingInt(record -> record.blockBox().maxZ());

    private final Set<LandmarkRecord> records = new LinkedHashSet<>();
    private final Map<LandmarkWorld, Set<LandmarkRecord>> recordsByWorld = new java.util.LinkedHashMap<>();
    private final Map<LandmarkChunkKey, Set<LandmarkRecord>> recordsByChunk = new java.util.LinkedHashMap<>();

    public boolean add(LandmarkRecord record) {
        if (!records.add(record)) {
            return false;
        }
        recordsByWorld.computeIfAbsent(record.world(), ignored -> new LinkedHashSet<>()).add(record);
        for (ChunkCoordinate chunk : record.intersectingChunks()) {
            recordsByChunk.computeIfAbsent(new LandmarkChunkKey(record.world(), chunk), ignored -> new LinkedHashSet<>())
                .add(record);
        }
        return true;
    }

    public boolean addAll(Collection<LandmarkRecord> records) {
        boolean changed = false;
        for (LandmarkRecord record : records) {
            changed |= add(record);
        }
        return changed;
    }

    public boolean remove(LandmarkRecord record) {
        if (!records.remove(record)) {
            return false;
        }
        Set<LandmarkRecord> worldRecords = recordsByWorld.get(record.world());
        if (worldRecords != null) {
            worldRecords.remove(record);
            if (worldRecords.isEmpty()) {
                recordsByWorld.remove(record.world());
            }
        }
        for (ChunkCoordinate chunk : record.intersectingChunks()) {
            LandmarkChunkKey chunkKey = new LandmarkChunkKey(record.world(), chunk);
            Set<LandmarkRecord> chunkRecords = recordsByChunk.get(chunkKey);
            if (chunkRecords != null) {
                chunkRecords.remove(record);
                if (chunkRecords.isEmpty()) {
                    recordsByChunk.remove(chunkKey);
                }
            }
        }
        return true;
    }

    public boolean removeAll(Collection<LandmarkRecord> records) {
        boolean changed = false;
        for (LandmarkRecord record : records) {
            changed |= remove(record);
        }
        return changed;
    }

    public boolean removeIf(Predicate<LandmarkRecord> predicate) {
        Set<LandmarkRecord> toRemove = new LinkedHashSet<>();
        for (LandmarkRecord record : records) {
            if (predicate.test(record)) {
                toRemove.add(record);
            }
        }
        return removeAll(toRemove);
    }

    public Set<LandmarkRecord> findByChunk(LandmarkWorld world, ChunkCoordinate chunk) {
        return immutableCopy(recordsByChunk.get(new LandmarkChunkKey(world, chunk)));
    }

    public Set<LandmarkRecord> findIntersecting(LandmarkWorld world, BlockBox targetBox) {
        Set<LandmarkRecord> matches = new java.util.TreeSet<>(RECORD_ORDER);
        for (LandmarkRecord record : findByWorld(world)) {
            if (record.intersects(targetBox)) {
                matches.add(record);
            }
        }
        return Collections.unmodifiableSet(matches);
    }

    public Set<LandmarkRecord> findContaining(LandmarkWorld world, int x, int y, int z) {
        Set<LandmarkRecord> matches = new java.util.TreeSet<>(RECORD_ORDER);
        for (LandmarkRecord record : findByChunk(world, ChunkCoordinate.fromBlock(x, z))) {
            if (record.contains(x, y, z)) {
                matches.add(record);
            }
        }
        return Collections.unmodifiableSet(matches);
    }

    public Set<LandmarkRecord> findByWorld(LandmarkWorld world) {
        return immutableCopy(recordsByWorld.get(world));
    }

    public Set<LandmarkRecord> findByDimension(DimensionType dimensionType) {
        Set<LandmarkRecord> matches = new LinkedHashSet<>();
        for (LandmarkWorld world : recordsByWorld.keySet()) {
            if (world.dimensionType() == dimensionType) {
                matches.addAll(recordsByWorld.get(world));
            }
        }
        return Collections.unmodifiableSet(matches);
    }

    public Set<LandmarkRecord> all() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(records));
    }

    public int size() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    private static Set<LandmarkRecord> immutableCopy(Set<LandmarkRecord> records) {
        if (records == null || records.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(records));
    }
}
