package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.AABB;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record CheckTargetInLockedLandmarkPayload(
    @NotNull DimensionType targetDimensionType,
    @NotNull AABB targetBox
) implements CustomPacketPayload {

    public static final Type<CheckTargetInLockedLandmarkPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        CheckTargetInLockedLandmarkPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, CheckTargetInLockedLandmarkPayload> CODEC = codec(
        CheckTargetInLockedLandmarkPayload::encode,
        CheckTargetInLockedLandmarkPayload::decode
    );

    @Override
    public Type<?> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(targetDimensionType);
        double minX = targetBox.minX;
        double minY = targetBox.minY;
        double minZ = targetBox.minZ;
        double maxX = targetBox.maxX;
        double maxY = targetBox.maxY;
        double maxZ = targetBox.maxZ;
        buf.writeDouble(minX);
        buf.writeDouble(minY);
        buf.writeDouble(minZ);
        buf.writeDouble(maxX);
        buf.writeDouble(maxY);
        buf.writeDouble(maxZ);
    }

    private static @NotNull CheckTargetInLockedLandmarkPayload decode(@NotNull FriendlyByteBuf buf) {
        DimensionType dimensionType = buf.readEnum(DimensionType.class);
        double minX = buf.readDouble();
        double minY = buf.readDouble();
        double minZ = buf.readDouble();
        double maxX = buf.readDouble();
        double maxY = buf.readDouble();
        double maxZ = buf.readDouble();
        AABB box = new AABB(
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );
        return new CheckTargetInLockedLandmarkPayload(dimensionType, box);
    }
}
