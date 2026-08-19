package com.diskree.achievetodo.networking.s2c;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.minecraft.network.protocol.common.custom.CustomPacketPayload.codec;

public record SyncAbilitiesConfigurationPayload(
    @NotNull Map<AbilityType, Integer> abilitiesConfiguration
) implements CustomPacketPayload {

    public static final Type<SyncAbilitiesConfigurationPayload> ID = new Type<>(AchieveToDoMod.getIdentifier(
        SyncAbilitiesConfigurationPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final StreamCodec<FriendlyByteBuf, SyncAbilitiesConfigurationPayload> CODEC = codec(
        SyncAbilitiesConfigurationPayload::encode,
        SyncAbilitiesConfigurationPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        int abilityTypesSize = abilitiesConfiguration.size();
        buf.writeInt(abilityTypesSize);
        for (var entry : abilitiesConfiguration.entrySet()) {
            buf.writeEnum(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    private static @NotNull SyncAbilitiesConfigurationPayload decode(@NotNull FriendlyByteBuf buf) {
        Map<AbilityType, Integer> abilitiesConfiguration = new Object2IntOpenHashMap<>();
        int abilityTypesSize = buf.readInt();
        for (int i = 0; i < abilityTypesSize; i++) {
            AbilityType abilityType = buf.readEnum(AbilityType.class);
            int requiredCount = buf.readInt();
            abilitiesConfiguration.put(abilityType, requiredCount);
        }
        return new SyncAbilitiesConfigurationPayload(abilitiesConfiguration);
    }
}
