package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.progression.PlayerProgressionState;

import java.util.UUID;

public interface PlayerProgressionLookup {

    PlayerProgressionState getProgressionState(UUID playerId);
}
