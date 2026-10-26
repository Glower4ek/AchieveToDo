package com.diskree.achievetodo.progression;

public final class AbilityStateResolver {

    private AbilityStateResolver() {
    }

    public static ResolvedAbilityState resolve(int obtainedAdvancementCount, int requiredCount, boolean demystified) {
        if (requiredCount == ProgressionConstants.INITIALLY_UNLOCKED_FLAG) {
            return new ResolvedAbilityState(AbilityLockKind.UNLOCKED, demystified);
        }
        if (requiredCount == ProgressionConstants.PERMANENTLY_LOCKED_FLAG) {
            return new ResolvedAbilityState(AbilityLockKind.PERMANENTLY_LOCKED, demystified);
        }
        if (obtainedAdvancementCount >= requiredCount) {
            return new ResolvedAbilityState(AbilityLockKind.UNLOCKED, demystified);
        }
        return new ResolvedAbilityState(AbilityLockKind.LOCKED, demystified);
    }
}
