package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.injection.extension.main.ChunkExtension;
import com.diskree.achievetodo.injection.extension.main.SerializedChunkExtension;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(SerializableChunkData.class)
public class SerializedChunkMixin implements SerializedChunkExtension {

    @Unique
    private CompoundTag featureLandmarksNbt;

    @Override
    public void achievetodo$setFeatureLandmarksNbt(CompoundTag featureLandmarksNbt) {
        this.featureLandmarksNbt = featureLandmarksNbt;
    }

    @ModifyReturnValue(
        method = "copyOf",
        at = @At(value = "TAIL")
    )
    private static SerializableChunkData writeFeatureLandmarkNbt(
        SerializableChunkData original,
        @Local(argsOnly = true) ServerLevel world,
        @Local(argsOnly = true) ChunkAccess chunk
    ) {
        if (chunk instanceof ChunkExtension chunkExtension) {
            SerializedChunkExtension serializedChunkExtension = MixinCasting.serializedChunk(original);
            Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = chunkExtension.achievetodo$getFeatureLandmarks();
            if (landmarks != null && !landmarks.isEmpty()) {
                CompoundTag featureLandmarksNbt = new CompoundTag();
                for (var entry : landmarks.entrySet()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = entry.getValue();
                    ListTag blockBoxesNbt = new ListTag();
                    for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                        BoundingBox blockBox = dimensionalBlockBox.blockBox();
                        CompoundTag blockBoxNbt = new CompoundTag();
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_X, blockBox.minX());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Y, blockBox.minY());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Z, blockBox.minZ());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_X, blockBox.maxX());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Y, blockBox.maxY());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Z, blockBox.maxZ());
                        blockBoxesNbt.add(blockBoxNbt);
                    }
                    LandmarkType landmarkType = entry.getKey();
                    featureLandmarksNbt.put(landmarkType.getName(), blockBoxesNbt);
                }
                serializedChunkExtension.achievetodo$setFeatureLandmarksNbt(featureLandmarksNbt);
            }
        }
        return original;
    }

    @ModifyReturnValue(
        method = "parse",
        at = @At(value = "TAIL")
    )
    private static SerializableChunkData readFeatureLandmarksNbt(
        SerializableChunkData serializedChunk,
        @Local(argsOnly = true) @NotNull CompoundTag nbt
    ) {
        CompoundTag featureLandmarksNbt = nbt.getCompoundOrEmpty(Constants.NbtKey.FEATURE_LANDMARKS);
        if (!featureLandmarksNbt.isEmpty()) {
            SerializedChunkExtension serializedChunkExtension = MixinCasting.serializedChunk(serializedChunk);
            serializedChunkExtension.achievetodo$setFeatureLandmarksNbt(featureLandmarksNbt);
        }
        return serializedChunk;
    }

    @ModifyReturnValue(
        method = "write",
        at = @At(value = "TAIL")
    )
    private CompoundTag serializeLandmarksNbt(CompoundTag original) {
        if (featureLandmarksNbt != null) {
            original.put(Constants.NbtKey.FEATURE_LANDMARKS, featureLandmarksNbt);
        }
        return original;
    }

    @Inject(
        method = "read",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setAllReferences(Ljava/util/Map;)V",
            shift = At.Shift.AFTER
        )
    )
    private void convertFeatureLandmarksNbt(
        ServerLevel world,
        PoiManager poiStorage,
        RegionStorageInfo key,
        ChunkPos expectedPos,
        CallbackInfoReturnable<ProtoChunk> cir,
        @Local ChunkAccess chunk
    ) {
        if (featureLandmarksNbt != null && chunk instanceof ChunkExtension chunkExtension) {
            DimensionType dimensionType = DimensionType.findByWorld(world.dimension());
            if (dimensionType == null) {
                return;
            }
            Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks = null;
            for (String landmarkName : featureLandmarksNbt.keySet()) {
                LandmarkType landmarkType = LandmarkType.findByName(landmarkName);
                if (landmarkType == null) {
                    continue;
                }
                ListTag blockBoxesNbt = featureLandmarksNbt.getListOrEmpty(landmarkName);
                if (blockBoxesNbt.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < blockBoxesNbt.size(); i++) {
                    CompoundTag blockBoxNbt = blockBoxesNbt.getCompoundOrEmpty(i);
                    if (!blockBoxNbt.isEmpty()) {
                        BoundingBox blockBox = new BoundingBox(
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_X, 0),
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_Y, 0),
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MIN_Z, 0),
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_X, 0),
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_Y, 0),
                            blockBoxNbt.getIntOr(Constants.NbtKey.BLOCK_BOX_MAX_Z, 0)
                        );
                        if (featureLandmarks == null) {
                            featureLandmarks = new HashMap<>();
                        }
                        featureLandmarks
                            .computeIfAbsent(landmarkType, k -> new HashSet<>())
                            .add(new DimensionalBlockBox(dimensionType, blockBox));
                    }
                }
            }
            chunkExtension.achievetodo$setFeatureLandmarks(world, featureLandmarks);
        }
    }
}
