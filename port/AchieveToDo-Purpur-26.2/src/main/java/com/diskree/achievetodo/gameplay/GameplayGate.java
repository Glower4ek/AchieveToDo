package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import java.util.Set;
import java.util.UUID;

/** Numeric gates only; no count storage, network input, criteria writes or reward effects. */
public final class GameplayGate {
    public static final Set<AbilityType> CLIENT_DEPENDENT = Set.of(AbilityType.VISION,
        AbilityType.OPEN_INVENTORY, AbilityType.USE_SPYGLASS);
    public static final Set<AbilityType> IMPLEMENTED;
    static {
        var abilities = java.util.EnumSet.of(AbilityType.BREAK_BLOCKS,
        AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y, AbilityType.SLEEP, AbilityType.GET_INTO_BOAT,
        AbilityType.GET_INTO_MINECART, AbilityType.OPEN_DOOR, AbilityType.OPEN_TRAPDOOR,
        AbilityType.OPEN_FENCE_GATE, AbilityType.OPEN_CHEST, AbilityType.OPEN_BARREL,
        AbilityType.OPEN_FURNACE, AbilityType.THROW_SNOWBALL, AbilityType.THROW_EGG,
        AbilityType.THROW_ENDER_PEARL, AbilityType.THROW_WIND_CHARGE, AbilityType.USE_FISHING_ROD);
        abilities.addAll(GameplayMenuAbilities.abilities());
        abilities.addAll(NativeFoodAbilities.abilities());
        abilities.addAll(NativeEquipmentAbilities.abilities());
        abilities.addAll(NativeActionBindings.ABILITIES);
        abilities.addAll(NativeToolBindings.ABILITIES);
        abilities.addAll(NativeComplexBindings.ABILITIES);
        abilities.addAll(NativePortalBindings.ABILITIES);
        abilities.addAll(NativeMovementBindings.ABILITIES);
        // S4M: server permission projection for local render/input plus the native spyglass gate.
        abilities.addAll(CLIENT_DEPENDENT);
        IMPLEMENTED = Set.copyOf(abilities);
    }

    public static boolean allows(PlayerProgressionState state, UUID actor, boolean nativeBypass, AbilityType ability) {
        if (!IMPLEMENTED.contains(ability)) throw new IllegalArgumentException("Not an implemented gameplay ability: " + ability);
        // Creative/spectator bypass is part of the original Fabric semantics.
        if (nativeBypass) return true;
        return state != null && actor.equals(state.getPlayerId()) && state.isReady()
            && state.getAbilityState(ability).isUnlocked();
    }

    public static boolean allowsBreak(PlayerProgressionState state, UUID actor, boolean nativeBypass, int y) {
        return allows(state, actor, nativeBypass, AbilityType.BREAK_BLOCKS)
            && (y >= 0 || allows(state, actor, nativeBypass, AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y));
    }

    private GameplayGate() {}
}
