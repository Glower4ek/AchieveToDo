package com.diskree.achievetodo.injection.extension.main;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface LandmarkGenerationTracker {
    void achievetodo$setLandmarkGenerationTrackingEnabled(boolean isLandmarkGenerationTrackingEnabled);

    void achievetodo$setLandmarkBlockBox(BoundingBox landmarkBlockBox);

    BoundingBox achievetodo$getLandmarkBlockBox();
}
