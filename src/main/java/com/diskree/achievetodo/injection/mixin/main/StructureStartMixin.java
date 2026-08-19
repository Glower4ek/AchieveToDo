package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import com.diskree.achievetodo.injection.extension.main.StructureStartExtension;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

@Mixin(StructureStart.class)
public class StructureStartMixin implements StructureStartExtension {

    @Unique
    private LandmarkType landmarkType;

    @Unique
    private BoundingBox landmarkBlockBox;

    @Override
    public void achievetodo$setLandmarkType(LandmarkType landmarkType) {
        this.landmarkType = landmarkType;
    }

    @Override
    public LandmarkType achievetodo$getLandmarkType() {
        return landmarkType;
    }

    @Override
    public void achievetodo$setLandmarkBlockBox(BoundingBox landmarkBlockBox) {
        this.landmarkBlockBox = landmarkBlockBox;
    }

    @Override
    public BoundingBox achievetodo$getLandmarkBlockBox() {
        return landmarkBlockBox;
    }

    @Shadow
    @Final
    private ChunkPos chunkPos;

    @ModifyReturnValue(
        method = "loadStaticStart",
        at = @At(
            value = "RETURN",
            ordinal = 2
        )
    )
    private static @NotNull StructureStart readLandmarkBlockBoxFromNbt(
        StructureStart original,
        @Local(argsOnly = true) @NotNull CompoundTag nbt
    ) {
        CompoundTag landmarkNbt = nbt.getCompoundOrEmpty(Constants.NbtKey.STRUCTURE_LANDMARK);
        if (landmarkNbt.contains(Constants.NbtKey.LANDMARK_TYPE)) {
            StructureStartExtension structureStartExtension = MixinCasting.structureStart(original);
            LandmarkType landmarkType = LandmarkType.findByName(
                landmarkNbt.getStringOr(Constants.NbtKey.LANDMARK_TYPE, "")
            );
            if (landmarkType != null) {
                structureStartExtension.achievetodo$setLandmarkType(landmarkType);
                if (landmarkNbt.contains(Constants.NbtKey.BLOCK_BOX_MIN_X)) {
                    structureStartExtension.achievetodo$setLandmarkBlockBox(
                        new BoundingBox(
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_X, 0),
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_Y, 0),
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_Z, 0),
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_X, 0),
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_Y, 0),
                            landmarkNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_Z, 0)
                        )
                    );
                }
            }
        }
        return original;
    }

    @Inject(
        method = "createTag",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void writeLandmarkBlockBoxToNbt(
        StructurePieceSerializationContext context,
        ChunkPos chunkPos,
        CallbackInfoReturnable<CompoundTag> cir,
        @Local CompoundTag nbtCompound
    ) {
        if (landmarkType != null) {
            CompoundTag landmarkNbt = new CompoundTag();
            landmarkNbt.putString(Constants.NbtKey.LANDMARK_TYPE, landmarkType.getName());
            if (landmarkBlockBox != null) {
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_X, landmarkBlockBox.minX());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Y, landmarkBlockBox.minY());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Z, landmarkBlockBox.minZ());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_X, landmarkBlockBox.maxX());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Y, landmarkBlockBox.maxY());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Z, landmarkBlockBox.maxZ());
            }
            nbtCompound.put(Constants.NbtKey.STRUCTURE_LANDMARK, landmarkNbt);
        }
    }

    @Inject(
        method = "placeInChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;getCenter()Lnet/minecraft/core/BlockPos;",
            shift = At.Shift.AFTER
        )
    )
    private void startStructureGenerationTracker(
        WorldGenLevel world,
        StructureManager structureAccessor,
        ChunkGenerator chunkGenerator,
        RandomSource random,
        BoundingBox chunkBox,
        ChunkPos chunkPos,
        CallbackInfo ci
    ) {
        if (landmarkType != null) {
            ServerLevel serverWorld = world.getLevel();
            DimensionType dimensionType = DimensionType.findByWorld(serverWorld.dimension());
            if (dimensionType != null && world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
                landmarkGenerationTracker.achievetodo$setLandmarkGenerationTrackingEnabled(true);
                if (landmarkBlockBox != null) {
                    landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(new BoundingBox(
                        landmarkBlockBox.minX(),
                        landmarkBlockBox.minY(),
                        landmarkBlockBox.minZ(),
                        landmarkBlockBox.maxX(),
                        landmarkBlockBox.maxY(),
                        landmarkBlockBox.maxZ()
                    ));
                } else {
                    landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(null);
                }
            }
        }
    }

    @Inject(
        method = "placeInChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/Structure;afterPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;)V",
            shift = At.Shift.AFTER
        )
    )
    private void stopStructureGenerationTracker(
        WorldGenLevel world,
        StructureManager structureAccessor,
        ChunkGenerator chunkGenerator,
        RandomSource random,
        BoundingBox chunkBox,
        ChunkPos chunkPos,
        CallbackInfo ci
    ) {
        if (landmarkType != null && world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
            BoundingBox newLandmarkBlockBox = landmarkGenerationTracker.achievetodo$getLandmarkBlockBox();
            if (newLandmarkBlockBox != null) {
                ServerLevel serverWorld = world.getLevel();
                DimensionType dimensionType = DimensionType.findByWorld(serverWorld.dimension());
                DimensionalBlockBox newDimensionalBlockBox = new DimensionalBlockBox(
                    dimensionType,
                    newLandmarkBlockBox
                );
                if (landmarkBlockBox == null) {
                    AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(
                        serverWorld,
                        chunkPos,
                        Map.of(landmarkType, Set.of(newDimensionalBlockBox)),
                        true
                    );
                } else {
                    DimensionalBlockBox oldDimensionalBlockBox = new DimensionalBlockBox(
                        dimensionType,
                        landmarkBlockBox
                    );
                    if (!newLandmarkBlockBox.equals(landmarkBlockBox)) {
                        AchieveToDoMod.getServer().onLandmarkResized(
                            serverWorld,
                            chunkPos,
                            landmarkType,
                            oldDimensionalBlockBox,
                            newDimensionalBlockBox
                        );
                    }
                }
                landmarkBlockBox = newLandmarkBlockBox;
            }
            landmarkGenerationTracker.achievetodo$setLandmarkGenerationTrackingEnabled(false);
            landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(null);
        }
    }
}
