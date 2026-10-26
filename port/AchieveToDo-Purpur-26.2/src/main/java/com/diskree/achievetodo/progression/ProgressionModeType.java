package com.diskree.achievetodo.progression;

import java.util.Locale;
import java.util.Optional;

public enum ProgressionModeType {

    EASY(1),
    NORMAL(1),
    HARD(1),
    CHAOS(1);

    private final int version;

    ProgressionModeType(int version) {
        this.version = version;
    }

    public static ProgressionModeType getDefaultMode() {
        return NORMAL;
    }

    public static Optional<ProgressionModeType> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        if (name.equalsIgnoreCase("medium")) return Optional.of(NORMAL);
        for (ProgressionModeType progressionModeType : values()) {
            if (progressionModeType.name().equalsIgnoreCase(name)) {
                return Optional.of(progressionModeType);
            }
        }
        return Optional.empty();
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
