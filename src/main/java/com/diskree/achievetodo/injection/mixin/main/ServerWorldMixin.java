package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin extends Level implements LandmarkGenerationTracker {

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
        BoundingBox temp = landmarkBlockBox;
        landmarkBlockBox = null;
        return temp;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        boolean result = super.setBlock(pos, state, flags, maxUpdateDepth);
        if (result && isLandmarkGenerationTrackingEnabled) {
            if (landmarkBlockBox == null) {
                landmarkBlockBox = new BoundingBox(pos);
            }
            landmarkBlockBox.encapsulate(pos);
        }
        return result;
    }

    protected ServerWorldMixin(
        WritableLevelData properties,
        ResourceKey<Level> registryRef,
        RegistryAccess registryManager,
        Holder<DimensionType> dimensionEntry,
        boolean isClient,
        boolean debugWorld,
        long seed,
        int maxChainedNeighborUpdates
    ) {
        super(
            properties,
            registryRef,
            registryManager,
            dimensionEntry,
            isClient,
            debugWorld,
            seed,
            maxChainedNeighborUpdates
        );
    }
}
