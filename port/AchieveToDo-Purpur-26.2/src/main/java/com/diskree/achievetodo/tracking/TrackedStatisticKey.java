package com.diskree.achievetodo.tracking;

import java.util.Locale;
import java.util.Objects;

public record TrackedStatisticKey(TrackedStatisticCategory category, String key) {

    public TrackedStatisticKey {
        category = Objects.requireNonNull(category, "category");
        key = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be blank");
        }
    }

    public static TrackedStatisticKey custom(String key) {
        return new TrackedStatisticKey(TrackedStatisticCategory.CUSTOM, key);
    }

    public static TrackedStatisticKey itemUsed(String key) {
        return new TrackedStatisticKey(TrackedStatisticCategory.ITEM_USED, key);
    }

    public static TrackedStatisticKey blockMined(String key) {
        return new TrackedStatisticKey(TrackedStatisticCategory.BLOCK_MINED, key);
    }

    public static TrackedStatisticKey entityKilled(String key) {
        return new TrackedStatisticKey(TrackedStatisticCategory.ENTITY_KILLED, key);
    }
}
