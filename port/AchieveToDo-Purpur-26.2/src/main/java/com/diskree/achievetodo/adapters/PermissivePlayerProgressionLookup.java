package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.AbilityRequirements;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ProgressionConstants;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PermissivePlayerProgressionLookup implements PlayerProgressionLookup {

    private static final AbilityRequirements REQUIREMENTS = createRequirements();

    @Override
    public PlayerProgressionState getProgressionState(UUID playerId) {
        return PlayerProgressionState.create(playerId, 0, REQUIREMENTS);
    }

    private static AbilityRequirements createRequirements() {
        Map<AbilityType, Integer> requirements = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            requirements.put(abilityType, ProgressionConstants.INITIALLY_UNLOCKED_FLAG);
        }
        return AbilityRequirements.from(requirements);
    }
}
