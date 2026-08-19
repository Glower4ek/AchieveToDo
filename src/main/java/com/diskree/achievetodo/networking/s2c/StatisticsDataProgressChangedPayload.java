package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.tracking.TrackedStatisticsDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record StatisticsDataProgressChangedPayload(
    @NotNull TrackedStatisticsDataType trackedStatisticsDataType,
    int newProgress
) implements CustomPacketPayload {

    public static final Type<StatisticsDataProgressChangedPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        StatisticsDataProgressChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, StatisticsDataProgressChangedPayload> CODEC = codec(
        StatisticsDataProgressChangedPayload::encode,
        StatisticsDataProgressChangedPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(trackedStatisticsDataType);
        buf.writeInt(newProgress);
    }

    private static @NotNull StatisticsDataProgressChangedPayload decode(@NotNull FriendlyByteBuf buf) {
        TrackedStatisticsDataType trackedStatisticsDataType = buf.readEnum(TrackedStatisticsDataType.class);
        int progress = buf.readInt();
        return new StatisticsDataProgressChangedPayload(trackedStatisticsDataType, progress);
    }
}
