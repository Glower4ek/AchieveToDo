package com.diskree.achievetodo.landmark;

import java.util.Locale;
import java.util.Objects;

public record LandmarkSource(
    LandmarkSourceKind kind,
    String sourceKey,
    ChunkCoordinate sourceChunk
) {

    public LandmarkSource {
        kind = Objects.requireNonNull(kind, "kind");
        sourceKey = normalizeKey(sourceKey);
        sourceChunk = Objects.requireNonNull(sourceChunk, "sourceChunk");
    }

    private static String normalizeKey(String key) {
        String normalized = Objects.requireNonNull(key, "sourceKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sourceKey must not be blank");
        }
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }
}
