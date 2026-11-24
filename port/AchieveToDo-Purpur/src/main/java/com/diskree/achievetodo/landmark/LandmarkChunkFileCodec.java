package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.LandmarkType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LandmarkChunkFileCodec {

    private static final String VERSION = "1";

    private LandmarkChunkFileCodec() {
    }

    public static String encode(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) {
        List<LandmarkRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort(Comparator
            .comparing((LandmarkRecord record) -> record.landmarkType().getName())
            .thenComparing(record -> record.source().kind().name())
            .thenComparing(record -> record.source().sourceKey())
            .thenComparingInt(record -> record.source().sourceChunk().x())
            .thenComparingInt(record -> record.source().sourceChunk().z())
            .thenComparingInt(record -> record.blockBox().minX())
            .thenComparingInt(record -> record.blockBox().minY())
            .thenComparingInt(record -> record.blockBox().minZ())
            .thenComparingInt(record -> record.blockBox().maxX())
            .thenComparingInt(record -> record.blockBox().maxY())
            .thenComparingInt(record -> record.blockBox().maxZ()));

        StringBuilder content = new StringBuilder();
        content.append("version=").append(VERSION).append('\n');
        content.append("world=").append(encodeField(world.worldId())).append('\n');
        content.append("dimension=").append(world.dimensionType().getName()).append('\n');
        content.append("chunk=").append(chunk.x()).append(',').append(chunk.z()).append('\n');
        for (LandmarkRecord record : sortedRecords) {
            BlockBox blockBox = record.blockBox();
            ChunkCoordinate sourceChunk = record.source().sourceChunk();
            content.append("record=")
                .append(record.landmarkType().getName()).append('|')
                .append(record.source().kind().name()).append('|')
                .append(encodeField(record.source().sourceKey())).append('|')
                .append(sourceChunk.x()).append('|')
                .append(sourceChunk.z()).append('|')
                .append(blockBox.minX()).append('|')
                .append(blockBox.minY()).append('|')
                .append(blockBox.minZ()).append('|')
                .append(blockBox.maxX()).append('|')
                .append(blockBox.maxY()).append('|')
                .append(blockBox.maxZ()).append('\n');
        }
        return content.toString();
    }

    public static Set<LandmarkRecord> decode(LandmarkWorld world, ChunkCoordinate chunk, String encodedContent) {
        String[] lines = encodedContent.replace("\r", "").split("\n");
        String version = null;
        String encodedWorldId = null;
        String dimension = null;
        String encodedChunk = null;
        Set<LandmarkRecord> records = new LinkedHashSet<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("version=")) {
                version = line.substring("version=".length());
                continue;
            }
            if (line.startsWith("world=")) {
                encodedWorldId = line.substring("world=".length());
                continue;
            }
            if (line.startsWith("dimension=")) {
                dimension = line.substring("dimension=".length());
                continue;
            }
            if (line.startsWith("chunk=")) {
                encodedChunk = line.substring("chunk=".length());
                continue;
            }
            if (!line.startsWith("record=")) {
                throw new IllegalArgumentException("Unsupported landmark chunk line: " + line);
            }
            String[] parts = line.substring("record=".length()).split("\\|", -1);
            if (parts.length != 11) {
                throw new IllegalArgumentException("Malformed landmark record line: " + line);
            }
            LandmarkType landmarkType = LandmarkType.findByName(parts[0])
                .orElseThrow(() -> new IllegalArgumentException("Unknown landmark type: " + parts[0]));
            LandmarkSourceKind sourceKind = LandmarkSourceKind.valueOf(parts[1]);
            String sourceKey = decodeField(parts[2]);
            ChunkCoordinate sourceChunk = new ChunkCoordinate(parseInt(parts[3]), parseInt(parts[4]));
            BlockBox blockBox = new BlockBox(
                parseInt(parts[5]),
                parseInt(parts[6]),
                parseInt(parts[7]),
                parseInt(parts[8]),
                parseInt(parts[9]),
                parseInt(parts[10])
            );
            records.add(new LandmarkRecord(
                landmarkType,
                world,
                blockBox,
                new LandmarkSource(sourceKind, sourceKey, sourceChunk)
            ));
        }

        if (!VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported landmark chunk version: " + version);
        }
        String decodedWorldId = decodeField(encodedWorldId);
        if (!world.worldId().equals(decodedWorldId)) {
            throw new IllegalArgumentException("Landmark chunk world mismatch: expected " + world.worldId() + " but was " + decodedWorldId);
        }
        if (!world.dimensionType().getName().equals(dimension)) {
            throw new IllegalArgumentException("Landmark chunk dimension mismatch: expected " + world.dimensionType().getName() + " but was " + dimension);
        }
        String expectedChunk = chunk.x() + "," + chunk.z();
        if (!expectedChunk.equals(encodedChunk)) {
            throw new IllegalArgumentException("Landmark chunk coordinate mismatch: expected " + expectedChunk + " but was " + encodedChunk);
        }

        return records;
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String encodedValue) {
        if (encodedValue == null) {
            throw new IllegalArgumentException("Missing encoded field");
        }
        return new String(Base64.getUrlDecoder().decode(encodedValue), StandardCharsets.UTF_8);
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value);
    }
}
