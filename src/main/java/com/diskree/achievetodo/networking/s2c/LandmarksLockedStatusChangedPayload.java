package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record LandmarksLockedStatusChangedPayload(
    @NotNull Map<LandmarkType, Set<DimensionalBlockBox>> landmarks,
    boolean isLocked
) implements CustomPacketPayload {

    public static final Type<LandmarksLockedStatusChangedPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        LandmarksLockedStatusChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, LandmarksLockedStatusChangedPayload> CODEC = codec(
        LandmarksLockedStatusChangedPayload::encode,
        LandmarksLockedStatusChangedPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        int landmarkTypesSize = landmarks.size();
        buf.writeInt(landmarkTypesSize);
        for (var entry : landmarks.entrySet()) {
            LandmarkType landmarkType = entry.getKey();
            buf.writeEnum(landmarkType);
            Set<DimensionalBlockBox> dimensionalBlockBoxes = entry.getValue();
            int dimensionalBlockBoxesSize = dimensionalBlockBoxes.size();
            buf.writeInt(dimensionalBlockBoxesSize);
            for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                DimensionType dimensionType = dimensionalBlockBox.dimensionType();
                buf.writeEnum(dimensionType);
                BoundingBox blockBox = dimensionalBlockBox.blockBox();
                int mixX = blockBox.minX();
                int mixY = blockBox.minY();
                int mixZ = blockBox.minZ();
                int maxX = blockBox.maxX();
                int maxY = blockBox.maxY();
                int maxZ = blockBox.maxZ();
                buf.writeInt(mixX);
                buf.writeInt(mixY);
                buf.writeInt(mixZ);
                buf.writeInt(maxX);
                buf.writeInt(maxY);
                buf.writeInt(maxZ);
            }
        }
        buf.writeBoolean(isLocked);
    }

    private static @NotNull LandmarksLockedStatusChangedPayload decode(@NotNull FriendlyByteBuf buf) {
        int landmarkTypesSize = buf.readInt();
        Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = new HashMap<>(landmarkTypesSize);
        for (int mapIndex = 0; mapIndex < landmarkTypesSize; mapIndex++) {
            LandmarkType landmarkType = buf.readEnum(LandmarkType.class);
            int dimensionalBlockBoxesSize = buf.readInt();
            Set<DimensionalBlockBox> dimensionalBlockBoxes = new HashSet<>(dimensionalBlockBoxesSize);
            for (int listIndex = 0; listIndex < dimensionalBlockBoxesSize; listIndex++) {
                DimensionType dimensionType = buf.readEnum(DimensionType.class);
                int minX = buf.readInt();
                int minY = buf.readInt();
                int minZ = buf.readInt();
                int maxX = buf.readInt();
                int maxY = buf.readInt();
                int maxZ = buf.readInt();
                BoundingBox blockBox = new BoundingBox(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
                );
                dimensionalBlockBoxes.add(new DimensionalBlockBox(dimensionType, blockBox));
            }
            landmarks.put(landmarkType, dimensionalBlockBoxes);
        }
        boolean isLocked = buf.readBoolean();
        return new LandmarksLockedStatusChangedPayload(landmarks, isLocked);
    }
}
