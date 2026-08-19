package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class ChunkRegionMixin implements WorldGenLevel, LandmarkGenerationTracker {

    @Unique
    private boolean isLandmarkGenerationTrackingEnabled;

    @Unique
    private BoundingBox landmarkBlockBox;

    @Override
    public void achievetodo$setLandmarkGenerationTrackingEnabled(boolean isLandmarkGenerationTrackingEnabled) {
        this.isLandmarkGenerationTrackingEnabled = isLandmarkGenerationTrackingEnabled;
    }

    @Override
    public void achievetodo$setLandmarkBlockBox(BoundingBox landmarkBlockBox) {
        this.landmarkBlockBox = landmarkBlockBox;
    }

    @Override
    public BoundingBox achievetodo$getLandmarkBlockBox() {
        return landmarkBlockBox;
    }

    @SuppressWarnings("deprecation")
    @Inject(
        method = "setBlock",
        at = @At(value = "TAIL")
    )
    private void trackLandmarkGeneration(
        BlockPos pos,
        BlockState state,
        int flags,
        int maxUpdateDepth,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (isLandmarkGenerationTrackingEnabled) {
            if (landmarkBlockBox == null) {
                landmarkBlockBox = new BoundingBox(pos);
            }
            landmarkBlockBox.encapsulate(pos);
        }
    }
}
