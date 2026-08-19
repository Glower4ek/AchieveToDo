package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.LandmarkType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record LandmarkTypesUnlockedPayload(
    @NotNull Set<LandmarkType> unlockedLandmarkTypes
) implements CustomPacketPayload {

    public static final Type<LandmarkTypesUnlockedPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        LandmarkTypesUnlockedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, LandmarkTypesUnlockedPayload> CODEC = codec(
        LandmarkTypesUnlockedPayload::encode,
        LandmarkTypesUnlockedPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        int unlockedLandmarkTypesSize = unlockedLandmarkTypes.size();
        buf.writeInt(unlockedLandmarkTypesSize);
        for (LandmarkType landmarkType : unlockedLandmarkTypes) {
            buf.writeEnum(landmarkType);
        }
    }

    private static @NotNull LandmarkTypesUnlockedPayload decode(@NotNull FriendlyByteBuf buf) {
        int unlockedLandmarkTypesSize = buf.readInt();
        Set<LandmarkType> landmarks = new HashSet<>(unlockedLandmarkTypesSize);
        for (int i = 0; i < unlockedLandmarkTypesSize; i++) {
            landmarks.add(buf.readEnum(LandmarkType.class));
        }
        return new LandmarkTypesUnlockedPayload(landmarks);
    }
}
