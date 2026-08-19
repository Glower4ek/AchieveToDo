package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record SyncObtainedAdvancementsCountPayload(
    int obtainedAdvancementsCount
) implements CustomPacketPayload {

    public static final Type<SyncObtainedAdvancementsCountPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        SyncObtainedAdvancementsCountPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, SyncObtainedAdvancementsCountPayload> CODEC = codec(
        SyncObtainedAdvancementsCountPayload::encode,
        SyncObtainedAdvancementsCountPayload::decode
    );

    @Override
    public Type<?> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(obtainedAdvancementsCount);
    }

    private static @NotNull SyncObtainedAdvancementsCountPayload decode(@NotNull FriendlyByteBuf buf) {
        int obtainedAdvancementsCount = buf.readInt();
        return new SyncObtainedAdvancementsCountPayload(obtainedAdvancementsCount);
    }
}
