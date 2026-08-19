package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record LockedLandmarkResizedPayload(
    @NotNull LandmarkType landmarkType,
    @NotNull DimensionType dimensionType,
    @NotNull BoundingBox oldBlockBox,
    @NotNull BoundingBox newBlockBox
) implements CustomPacketPayload {

    public static final Type<LockedLandmarkResizedPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        LockedLandmarkResizedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, LockedLandmarkResizedPayload> CODEC = codec(
        LockedLandmarkResizedPayload::encode,
        LockedLandmarkResizedPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(landmarkType);
        buf.writeEnum(dimensionType);
        int oldMinX = oldBlockBox.minX();
        int oldMinY = oldBlockBox.minY();
        int oldMinZ = oldBlockBox.minZ();
        int oldMaxX = oldBlockBox.maxX();
        int oldMaxY = oldBlockBox.maxY();
        int oldMaxZ = oldBlockBox.maxZ();
        buf.writeInt(oldMinX);
        buf.writeInt(oldMinY);
        buf.writeInt(oldMinZ);
        buf.writeInt(oldMaxX);
        buf.writeInt(oldMaxY);
        buf.writeInt(oldMaxZ);

        int newMinX = newBlockBox.minX();
        int newMinY = newBlockBox.minY();
        int newMinZ = newBlockBox.minZ();
        int newMaxX = newBlockBox.maxX();
        int newMaxY = newBlockBox.maxY();
        int newMaxZ = newBlockBox.maxZ();
        buf.writeInt(newMinX);
        buf.writeInt(newMinY);
        buf.writeInt(newMinZ);
        buf.writeInt(newMaxX);
        buf.writeInt(newMaxY);
        buf.writeInt(newMaxZ);
    }

    private static @NotNull LockedLandmarkResizedPayload decode(@NotNull FriendlyByteBuf buf) {
        LandmarkType landmarkType = buf.readEnum(LandmarkType.class);
        DimensionType dimensionType = buf.readEnum(DimensionType.class);
        int oldMinX = buf.readInt();
        int oldMinY = buf.readInt();
        int oldMinZ = buf.readInt();
        int oldMaxX = buf.readInt();
        int oldMaxY = buf.readInt();
        int oldMaxZ = buf.readInt();
        BoundingBox oldBlockBox = new BoundingBox(
            oldMinX,
            oldMinY,
            oldMinZ,
            oldMaxX,
            oldMaxY,
            oldMaxZ
        );

        int newMinX = buf.readInt();
        int newMinY = buf.readInt();
        int newMinZ = buf.readInt();
        int newMaxX = buf.readInt();
        int newMaxY = buf.readInt();
        int newMaxZ = buf.readInt();
        BoundingBox newBlockBox = new BoundingBox(
            newMinX,
            newMinY,
            newMinZ,
            newMaxX,
            newMaxY,
            newMaxZ
        );
        return new LockedLandmarkResizedPayload(landmarkType, dimensionType, oldBlockBox, newBlockBox);
    }
}
