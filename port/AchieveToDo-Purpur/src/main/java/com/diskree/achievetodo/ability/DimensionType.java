package com.diskree.achievetodo.ability;

import java.util.Locale;
import java.util.Optional;

public enum DimensionType {
    OVERWORLD,
    NETHER,
    END;

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DimensionType> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        for (DimensionType dimensionType : values()) {
            if (dimensionType.name().equalsIgnoreCase(name)) {
                return Optional.of(dimensionType);
            }
        }
        return Optional.empty();
    }
}
