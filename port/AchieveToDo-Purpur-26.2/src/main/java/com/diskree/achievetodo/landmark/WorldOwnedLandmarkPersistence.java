package com.diskree.achievetodo.landmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class WorldOwnedLandmarkPersistence implements LandmarkPersistence {

    private static final String FILE_EXTENSION = ".landmarks";
    private static final String FEATURE_SCAN_EXTENSION = ".feature-scan";

    private final Path storageRoot;

    public WorldOwnedLandmarkPersistence(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    @Override
    public Map<ChunkCoordinate, Set<LandmarkRecord>> loadWorld(LandmarkWorld world) throws IOException {
        Map<ChunkCoordinate, Set<LandmarkRecord>> chunks = new LinkedHashMap<>();
        Path dimensionDirectory = dimensionDirectory(world);
        if (!Files.isDirectory(dimensionDirectory)) {
            return chunks;
        }
        try (var paths = Files.list(dimensionDirectory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(FILE_EXTENSION)).toList()) {
                ChunkCoordinate chunk = parseChunkFileName(path.getFileName().toString());
                chunks.put(chunk, loadChunk(world, chunk));
            }
        }
        return chunks;
    }

    @Override
    public void saveWorld(LandmarkWorld world, Map<ChunkCoordinate, ? extends Collection<LandmarkRecord>> chunks) throws IOException {
        Path dimensionDirectory = dimensionDirectory(world);
        Files.createDirectories(dimensionDirectory);
        Set<String> expectedFileNames = new LinkedHashSet<>();
        for (Map.Entry<ChunkCoordinate, ? extends Collection<LandmarkRecord>> entry : chunks.entrySet()) {
            saveChunk(world, entry.getKey(), entry.getValue());
            expectedFileNames.add(chunkFileName(entry.getKey()));
        }
        try (var paths = Files.list(dimensionDirectory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(FILE_EXTENSION)).toList()) {
                if (!expectedFileNames.contains(path.getFileName().toString())) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Override
    public Set<LandmarkRecord> loadChunk(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        Path file = chunkFile(world, chunk);
        if (!Files.exists(file)) {
            return Set.of();
        }
        return LandmarkChunkFileCodec.decode(world, chunk, Files.readString(file));
    }

    @Override
    public void saveChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) throws IOException {
        Path file = chunkFile(world, chunk);
        if (records.isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, LandmarkChunkFileCodec.encode(world, chunk, records));
    }

    @Override
    public boolean isFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) {
        return Files.exists(featureScanFile(world, chunk));
    }

    @Override
    public void markFeatureScanComplete(LandmarkWorld world, ChunkCoordinate chunk) throws IOException {
        Path file = featureScanFile(world, chunk);
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            Files.writeString(file, "complete");
        }
    }

    private Path chunkFile(LandmarkWorld world, ChunkCoordinate chunk) {
        return dimensionDirectory(world).resolve(chunkFileName(chunk));
    }

    private Path featureScanFile(LandmarkWorld world, ChunkCoordinate chunk) {
        return dimensionDirectory(world).resolve(featureScanFileName(chunk));
    }

    private Path dimensionDirectory(LandmarkWorld world) {
        return storageRoot.resolve(world.dimensionType().getName());
    }

    private static String chunkFileName(ChunkCoordinate chunk) {
        return chunk.x() + "_" + chunk.z() + FILE_EXTENSION;
    }

    private static String featureScanFileName(ChunkCoordinate chunk) {
        return chunk.x() + "_" + chunk.z() + FEATURE_SCAN_EXTENSION;
    }

    private static ChunkCoordinate parseChunkFileName(String fileName) {
        String coordinates = fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
        int separatorIndex = coordinates.indexOf('_');
        if (separatorIndex <= 0 || separatorIndex == coordinates.length() - 1) {
            throw new IllegalArgumentException("Malformed landmark chunk filename: " + fileName);
        }
        return new ChunkCoordinate(
            Integer.parseInt(coordinates.substring(0, separatorIndex)),
            Integer.parseInt(coordinates.substring(separatorIndex + 1))
        );
    }
}
