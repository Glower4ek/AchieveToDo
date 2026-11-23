package com.diskree.achievetodo.tracking;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.AbilityLockKind;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ResolvedAbilityState;

import java.util.EnumSet;
import java.util.Objects;

public final class ProgressionStateSynchronizer {

    private ProgressionStateSynchronizer() {
    }

    public static PlayerProgressionUpdate synchronize(PlayerProgressionState previousState, int obtainedAdvancementCount, boolean ready) {
        Objects.requireNonNull(previousState, "previousState");
        PlayerProgressionState currentState = previousState.withReady(ready).withObtainedAdvancementCount(obtainedAdvancementCount);
        return diff(previousState, currentState);
    }

    public static PlayerProgressionUpdate diff(PlayerProgressionState previousState, PlayerProgressionState currentState) {
        Objects.requireNonNull(previousState, "previousState");
        Objects.requireNonNull(currentState, "currentState");
        EnumSet<AbilityType> unlocked = EnumSet.noneOf(AbilityType.class);
        EnumSet<AbilityType> relocked = EnumSet.noneOf(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            ResolvedAbilityState previousResolved = previousState.getAbilityState(abilityType);
            ResolvedAbilityState currentResolved = currentState.getAbilityState(abilityType);
            if (currentResolved.kind() == AbilityLockKind.UNLOCKED && previousResolved.kind() != AbilityLockKind.UNLOCKED) {
                unlocked.add(abilityType);
            } else if (currentResolved.kind() != AbilityLockKind.UNLOCKED && previousResolved.kind() == AbilityLockKind.UNLOCKED) {
                relocked.add(abilityType);
            }
        }
        return new PlayerProgressionUpdate(previousState, currentState, unlocked, relocked);
    }
}
