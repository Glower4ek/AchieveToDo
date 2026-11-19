package com.diskree.achievetodo.progression;

import java.util.Objects;

public record ResolvedAbilityState(AbilityLockKind kind, boolean demystified) {

    public ResolvedAbilityState {
        kind = Objects.requireNonNull(kind, "kind");
    }

    public AbilityStateSnapshot snapshot() {
        return switch (kind) {
            case UNLOCKED -> AbilityStateSnapshot.UNLOCKED;
            case PERMANENTLY_LOCKED -> AbilityStateSnapshot.PERMANENTLY_LOCKED;
            case LOCKED -> demystified ? AbilityStateSnapshot.DEMYSTIFIED : AbilityStateSnapshot.LOCKED;
        };
    }

    public boolean isLocked() {
        return kind != AbilityLockKind.UNLOCKED;
    }

    public boolean isUnlocked() {
        return kind == AbilityLockKind.UNLOCKED;
    }

    public boolean isPermanentlyLocked() {
        return kind == AbilityLockKind.PERMANENTLY_LOCKED;
    }
}
