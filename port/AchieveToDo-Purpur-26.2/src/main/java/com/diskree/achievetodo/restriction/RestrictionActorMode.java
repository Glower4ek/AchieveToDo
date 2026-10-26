package com.diskree.achievetodo.restriction;

public enum RestrictionActorMode {
    SURVIVAL,
    CREATIVE,
    SPECTATOR,
    ADVENTURE,
    UNKNOWN;

    public boolean bypassesRestrictions() {
        return this == CREATIVE || this == SPECTATOR;
    }
}
