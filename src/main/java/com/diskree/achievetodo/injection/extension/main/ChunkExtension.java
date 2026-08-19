package com.diskree.achievetodo.injection.extension.main;

import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;

public interface ChunkExtension {

    void achievetodo$setFeatureLandmarks(ServerLevel world, Map<LandmarkType, Set<DimensionalBlockBox>> landmarks);

    Map<LandmarkType, Set<DimensionalBlockBox>> achievetodo$getFeatureLandmarks();

    void achievetodo$addFeatureLandmark(
        ServerLevel world,
        LandmarkType landmarkType,
        DimensionalBlockBox dimensionalBlockBox
    );
}
