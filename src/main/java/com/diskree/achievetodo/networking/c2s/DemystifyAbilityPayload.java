package com.diskree.achievetodo.networking.c2s;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record DemystifyAbilityPayload(
    @NotNull AbilityType abilityType
) implements CustomPacketPayload {

    public static final Type<DemystifyAbilityPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        DemystifyAbilityPayload.class.getName().toLowerCase(Locale.ROOT).toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, DemystifyAbilityPayload> CODEC = codec(
        DemystifyAbilityPayload::encode,
        DemystifyAbilityPayload::decode
    );

    @Override
    public Type<?> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(abilityType);
    }

    private static @NotNull DemystifyAbilityPayload decode(@NotNull FriendlyByteBuf buf) {
        AbilityType abilityType = buf.readEnum(AbilityType.class);
        return new DemystifyAbilityPayload(abilityType);
    }
}
