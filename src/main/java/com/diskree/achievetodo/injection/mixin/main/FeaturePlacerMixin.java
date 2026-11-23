package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.injection.extension.main.ChunkExtension;
import com.diskree.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since 26.3 a feature is a configured instance registered under a {@code ResourceKey<Feature>} and is placed by
 * {@link FeaturePlacer}; the landmark features (desert well, monster room) are recognised by that key.
 */
@Mixin(FeaturePlacer.class)
public class FeaturePlacerMixin {

    @WrapOperation(
        method = "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    public boolean trackFeatureGeneration(
        Feature feature,
        WorldGenLevel world,
        ChunkGenerator generator,
        RandomSource random,
        BlockPos pos,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) @NotNull PlacedFeature placedFeature
    ) {
        LandmarkType landmarkType = placedFeature.feature().unwrapKey().map(LandmarkType.FEATURES::get).orElse(null);
        DimensionType dimensionType = null;
        ServerLevel serverWorld = null;
        LandmarkGenerationTracker tracker = null;
        if (landmarkType != null) {
            serverWorld = world.getLevel();
            dimensionType = DimensionType.findByWorld(serverWorld.dimension());
            if (dimensionType == null) {
                serverWorld = null;
            } else if (world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
                tracker = landmarkGenerationTracker;
                tracker.achievetodo$setLandmarkBlockBox(null);
                tracker.achievetodo$setLandmarkGenerationTrackingEnabled(true);
            }
        }
        boolean result = original.call(feature, world, generator, random, pos);
        if (tracker != null && world.getChunk(pos) instanceof ChunkExtension chunkExtension) {
            BoundingBox blockBox = tracker.achievetodo$getLandmarkBlockBox();
            if (blockBox != null) {
                chunkExtension.achievetodo$addFeatureLandmark(
                    serverWorld,
                    landmarkType,
                    new DimensionalBlockBox(dimensionType, blockBox)
                );
            }
            tracker.achievetodo$setLandmarkGenerationTrackingEnabled(false);
            tracker.achievetodo$setLandmarkBlockBox(null);
        }
        return result;
    }
}
