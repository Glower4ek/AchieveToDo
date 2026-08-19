package com.diskree.achievetodo.ability;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record DimensionalBlockBox(DimensionType dimensionType, BoundingBox blockBox) {
}
