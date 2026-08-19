package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.tracking.TrackedScoreType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record ScoreProgressChangedPayload(
    @NotNull TrackedScoreType progressType,
    int progress
) implements CustomPacketPayload {

    public static final Type<ScoreProgressChangedPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        ScoreProgressChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, ScoreProgressChangedPayload> CODEC = codec(
        ScoreProgressChangedPayload::encode,
        ScoreProgressChangedPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(progressType);
        buf.writeInt(progress);
    }

    private static @NotNull ScoreProgressChangedPayload decode(@NotNull FriendlyByteBuf buf) {
        TrackedScoreType trackedScoreType = buf.readEnum(TrackedScoreType.class);
        int progress = buf.readInt();
        return new ScoreProgressChangedPayload(trackedScoreType, progress);
    }
}
