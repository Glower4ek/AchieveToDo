package com.diskree.achievetodo.ability;

import java.util.Objects;

public record DimensionalBlockBox(DimensionType dimensionType, BlockBox blockBox) {

    public DimensionalBlockBox {
        dimensionType = Objects.requireNonNull(dimensionType, "dimensionType");
        blockBox = Objects.requireNonNull(blockBox, "blockBox");
    }
}
