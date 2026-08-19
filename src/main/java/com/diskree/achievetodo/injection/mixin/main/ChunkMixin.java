package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.injection.extension.main.ChunkExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(ChunkAccess.class)
public abstract class ChunkMixin implements ChunkExtension {

    @Unique
    private Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks;

    @Override
    public void achievetodo$setFeatureLandmarks(
        @NotNull ServerLevel world,
        Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks
    ) {
        this.featureLandmarks = featureLandmarks;
        if (featureLandmarks != null) {
            AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(world, chunkPos, featureLandmarks, true);
        }
        markUnsaved();
    }

    @Override
    public Map<LandmarkType, Set<DimensionalBlockBox>> achievetodo$getFeatureLandmarks() {
        return featureLandmarks;
    }

    @Override
    public void achievetodo$addFeatureLandmark(
        @NotNull ServerLevel world,
        LandmarkType featureLandmarkType,
        DimensionalBlockBox dimensionalBlockBox
    ) {
        if (featureLandmarks == null) {
            featureLandmarks = new HashMap<>();
        }
        featureLandmarks
            .computeIfAbsent(featureLandmarkType, k -> new HashSet<>())
            .add(dimensionalBlockBox);
        AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(
            world,
            chunkPos,
            Map.of(featureLandmarkType, Set.of(dimensionalBlockBox)),
            true
        );
        markUnsaved();
    }

    @Shadow
    @Final
    protected ChunkPos chunkPos;

    @Shadow
    public abstract void markUnsaved();
}
