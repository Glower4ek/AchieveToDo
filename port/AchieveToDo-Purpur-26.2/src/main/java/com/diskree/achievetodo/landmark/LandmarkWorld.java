package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.DimensionType;

import java.util.Objects;

public record LandmarkWorld(String worldId, DimensionType dimensionType) {

    public LandmarkWorld {
        worldId = Objects.requireNonNull(worldId, "worldId").trim();
        if (worldId.isEmpty()) {
            throw new IllegalArgumentException("worldId must not be blank");
        }
        dimensionType = Objects.requireNonNull(dimensionType, "dimensionType");
    }
}
