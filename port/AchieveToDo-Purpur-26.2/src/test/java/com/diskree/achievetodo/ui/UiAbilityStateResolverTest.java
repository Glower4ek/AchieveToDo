package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.progression.AbilityLockKind;
import com.diskree.achievetodo.progression.ProgressionConstants;
import com.diskree.achievetodo.progression.ResolvedAbilityState;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;

public final class UiAbilityStateResolverTest {

    public static void run() {
        assertEquals(
            UiAbilityState.UNKNOWN_LOCKED,
            UiAbilityStateResolver.resolve(new ResolvedAbilityState(AbilityLockKind.LOCKED, false), 5)
        );
        assertEquals(
            UiAbilityState.DEMYSTIFIED_LOCKED,
            UiAbilityStateResolver.resolve(new ResolvedAbilityState(AbilityLockKind.LOCKED, true), 5)
        );
        assertEquals(
            UiAbilityState.UNLOCKED,
            UiAbilityStateResolver.resolve(new ResolvedAbilityState(AbilityLockKind.UNLOCKED, true), 5)
        );
        assertEquals(
            UiAbilityState.INITIALLY_UNLOCKED,
            UiAbilityStateResolver.resolve(new ResolvedAbilityState(AbilityLockKind.UNLOCKED, false), ProgressionConstants.INITIALLY_UNLOCKED_FLAG)
        );
        assertEquals(
            UiAbilityState.PERMANENTLY_LOCKED,
            UiAbilityStateResolver.resolve(new ResolvedAbilityState(AbilityLockKind.PERMANENTLY_LOCKED, false), ProgressionConstants.PERMANENTLY_LOCKED_FLAG)
        );
    }
}
