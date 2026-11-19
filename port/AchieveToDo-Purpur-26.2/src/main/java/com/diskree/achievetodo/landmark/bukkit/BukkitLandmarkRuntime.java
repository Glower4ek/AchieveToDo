package com.diskree.achievetodo.landmark.bukkit;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.landmark.ChunkCoordinate;
import com.diskree.achievetodo.landmark.FeatureLandmarkDetector;
import com.diskree.achievetodo.landmark.LandmarkEngine;
import com.diskree.achievetodo.landmark.LandmarkRecord;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import com.diskree.achievetodo.landmark.StructureLandmarkDescriptor;
import com.diskree.achievetodo.landmark.StructureLandmarkDetector;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("removal")
public final class BukkitLandmarkRuntime implements Listener, AutoCloseable {

    private static final long FEATURE_SCAN_WARN_NANOS = 15_000_000L;

    private final JavaPlugin plugin;
    private final BukkitWorldFolderLandmarkPersistence persistence;
    private final LandmarkEngine engine;
    private final StructureLandmarkDetector structureDetector = new StructureLandmarkDetector();
    private final FeatureLandmarkDetector featureDetector = new FeatureLandmarkDetector();

    public BukkitLandmarkRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        this.persistence = new BukkitWorldFolderLandmarkPersistence();
        this.engine = new LandmarkEngine(persistence);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (World world : plugin.getServer().getWorlds()) {
            BukkitLandmarkAdapters.toLandmarkWorld(world).ifPresent(landmarkWorld -> {
                persistence.register(landmarkWorld, world.getWorldFolder().toPath());
                for (Chunk chunk : world.getLoadedChunks()) {
                    refreshChunk(chunk, false, FeatureScanMode.COMPATIBILITY);
                }
            });
        }
    }

    public LandmarkEngine engine() {
        return engine;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        refreshChunk(event.getChunk(), false, event.isNewChunk() ? FeatureScanMode.NONE : FeatureScanMode.COMPATIBILITY);
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        refreshChunk(event.getChunk(), false, FeatureScanMode.POPULATION);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Optional<LandmarkWorld> landmarkWorld = BukkitLandmarkAdapters.toLandmarkWorld(event.getWorld());
        if (landmarkWorld.isEmpty()) {
            return;
        }
        try {
            engine.unloadChunk(landmarkWorld.get(), new ChunkCoordinate(event.getChunk().getX(), event.getChunk().getZ()));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to unload landmark chunk slice", exception);
        }
    }

    @EventHandler
    public void onAsyncStructureGenerate(AsyncStructureGenerateEvent event) {
        scheduleStructureRefresh(event.getWorld(), event.getChunkX(), event.getChunkZ(), event.getStructure().key().asString(), BukkitLandmarkAdapters.toBlockBox(event.getBoundingBox()));
    }

    @EventHandler
    public void onAsyncStructureSpawn(AsyncStructureSpawnEvent event) {
        scheduleStructureRefresh(event.getWorld(), event.getChunkX(), event.getChunkZ(), event.getStructure().key().asString(), BukkitLandmarkAdapters.toBlockBox(event.getBoundingBox()));
    }

    @Override
    public void close() throws IOException {
        engine.close();
    }

    private void scheduleStructureRefresh(World world, int chunkX, int chunkZ, String structureKey, BlockBox fallbackBoundingBox) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Optional<LandmarkWorld> landmarkWorld = BukkitLandmarkAdapters.toLandmarkWorld(world);
            if (landmarkWorld.isEmpty()) {
                return;
            }
            persistence.register(landmarkWorld.get(), world.getWorldFolder().toPath());
            ChunkCoordinate chunkCoordinate = new ChunkCoordinate(chunkX, chunkZ);
            try {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    refreshChunk(world.getChunkAt(chunkX, chunkZ, false), false, FeatureScanMode.COMPATIBILITY);
                }
                structureDetector.detect(
                    landmarkWorld.get(),
                    StructureLandmarkDescriptor.of(structureKey, chunkCoordinate, fallbackBoundingBox, Set.of())
                ).ifPresent(record -> {
                    try {
                        engine.registerLandmark(record);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            } catch (RuntimeException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof IOException ioException) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to persist async structure fallback for chunk " + chunkCoordinate, ioException);
                } else {
                    throw exception;
                }
            }
        });
    }

    private void refreshChunk(Chunk chunk, boolean includeStructures, FeatureScanMode featureScanMode) {
        Optional<LandmarkWorld> landmarkWorld = BukkitLandmarkAdapters.toLandmarkWorld(chunk.getWorld());
        if (landmarkWorld.isEmpty()) {
            return;
        }
        LandmarkWorld resolvedWorld = landmarkWorld.get();
        persistence.register(resolvedWorld, chunk.getWorld().getWorldFolder().toPath());
        try {
            ChunkCoordinate chunkCoordinate = new ChunkCoordinate(chunk.getX(), chunk.getZ());
            engine.loadChunk(resolvedWorld, chunkCoordinate);
            if (includeStructures) {
                engine.synchronizeChunk(resolvedWorld, chunkCoordinate, detectStructureLandmarks(chunk, resolvedWorld, true));
            }
            maybeScanFeatures(chunk, resolvedWorld, chunkCoordinate, featureScanMode);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to refresh landmark chunk " + chunk.getX() + "," + chunk.getZ(), exception);
        }
    }

    private Collection<LandmarkRecord> detectStructureLandmarks(Chunk chunk, LandmarkWorld world, boolean includeStructures) {
        Set<LandmarkRecord> detected = new LinkedHashSet<>();
        long structureStartedAt = System.nanoTime();
        if (includeStructures) {
            for (GeneratedStructure structure : chunk.getStructures()) {
                Set<com.diskree.achievetodo.ability.BlockBox> pieceBoxes = new LinkedHashSet<>();
                for (StructurePiece piece : structure.getPieces()) {
                    pieceBoxes.add(BukkitLandmarkAdapters.toBlockBox(piece.getBoundingBox()));
                }
                structureDetector.detect(
                    world,
                    StructureLandmarkDescriptor.of(
                        structure.getStructure().key().asString(),
                        new ChunkCoordinate(chunk.getX(), chunk.getZ()),
                        BukkitLandmarkAdapters.toBlockBox(structure.getBoundingBox()),
                        pieceBoxes
                    )
                ).ifPresent(detected::add);
            }
        }
        return detected;
    }

    private void maybeScanFeatures(
        Chunk chunk,
        LandmarkWorld world,
        ChunkCoordinate chunkCoordinate,
        FeatureScanMode featureScanMode
    ) throws IOException {
        if (featureScanMode == FeatureScanMode.NONE) {
            return;
        }
        if (persistence.isFeatureScanComplete(world, chunkCoordinate)) {
            return;
        }
        if (hasPersistedFeatureSourceRecord(world, chunkCoordinate)) {
            persistence.markFeatureScanComplete(world, chunkCoordinate);
            return;
        }
        long featureStartedAt = System.nanoTime();
        Set<LandmarkRecord> featureRecords = featureDetector.detect(
            new BukkitFeatureChunkView(chunk.getWorld(), world, new ChunkCoordinate(chunk.getX(), chunk.getZ()))
        );
        long featureElapsed = System.nanoTime() - featureStartedAt;
        if (featureElapsed >= FEATURE_SCAN_WARN_NANOS) {
            plugin.getLogger().fine(
                    "Landmark feature scan for chunk " + chunk.getX() + "," + chunk.getZ() +
                    " took " + (featureElapsed / 1_000_000.0d) + " ms and found " + featureRecords.size() + " feature landmark(s)."
            );
        }
        for (LandmarkRecord record : featureRecords) {
            engine.registerLandmark(record);
        }
        persistence.markFeatureScanComplete(world, chunkCoordinate);
    }

    private boolean hasPersistedFeatureSourceRecord(LandmarkWorld world, ChunkCoordinate chunkCoordinate) {
        for (LandmarkRecord record : engine.slice(world, chunkCoordinate)) {
            if (record.source().kind() == com.diskree.achievetodo.landmark.LandmarkSourceKind.FEATURE &&
                record.source().sourceChunk().equals(chunkCoordinate)) {
                return true;
            }
        }
        return false;
    }

    private enum FeatureScanMode {
        NONE,
        COMPATIBILITY,
        POPULATION
    }
}
