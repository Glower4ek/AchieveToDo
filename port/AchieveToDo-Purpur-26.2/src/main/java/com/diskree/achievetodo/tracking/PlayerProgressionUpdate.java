package com.diskree.achievetodo.tracking;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;

import java.util.Objects;
import java.util.Set;

public record PlayerProgressionUpdate(
    PlayerProgressionState previousState,
    PlayerProgressionState currentState,
    Set<AbilityType> unlockedAbilities,
    Set<AbilityType> relockedAbilities
) {

    public PlayerProgressionUpdate {
        previousState = Objects.requireNonNull(previousState, "previousState");
        currentState = Objects.requireNonNull(currentState, "currentState");
        unlockedAbilities = Set.copyOf(unlockedAbilities);
        relockedAbilities = Set.copyOf(relockedAbilities);
    }
}
