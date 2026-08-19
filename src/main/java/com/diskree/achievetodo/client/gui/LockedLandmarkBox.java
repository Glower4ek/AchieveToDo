package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import net.minecraft.world.phys.AABB;

public record LockedLandmarkBox(LandmarkType landmarkType, DimensionType dimensionType, AABB box) {
}
