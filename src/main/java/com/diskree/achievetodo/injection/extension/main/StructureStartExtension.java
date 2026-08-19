package com.diskree.achievetodo.injection.extension.main;

import com.diskree.achievetodo.ability.LandmarkType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface StructureStartExtension {
    void achievetodo$setLandmarkType(LandmarkType landmarkType);

    LandmarkType achievetodo$getLandmarkType();

    void achievetodo$setLandmarkBlockBox(BoundingBox blockBox);

    BoundingBox achievetodo$getLandmarkBlockBox();
}
