package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.progression.ProgressionConstants;
import com.diskree.achievetodo.progression.ResolvedAbilityState;

import java.util.Objects;

public final class UiAbilityStateResolver {

    private UiAbilityStateResolver() {
    }

    public static UiAbilityState resolve(ResolvedAbilityState state, int requiredCount) {
        Objects.requireNonNull(state, "state");
        if (requiredCount == ProgressionConstants.PERMANENTLY_LOCKED_FLAG) {
            return UiAbilityState.PERMANENTLY_LOCKED;
        }
        if (requiredCount == ProgressionConstants.INITIALLY_UNLOCKED_FLAG) {
            return UiAbilityState.INITIALLY_UNLOCKED;
        }
        return switch (state.snapshot()) {
            case LOCKED -> UiAbilityState.UNKNOWN_LOCKED;
            case DEMYSTIFIED -> UiAbilityState.DEMYSTIFIED_LOCKED;
            case UNLOCKED -> UiAbilityState.UNLOCKED;
            case PERMANENTLY_LOCKED -> UiAbilityState.PERMANENTLY_LOCKED;
        };
    }
}
