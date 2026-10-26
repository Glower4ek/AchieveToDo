package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class AbilityRequirements {

    private final EnumMap<AbilityType, Integer> requiredCounts;

    private AbilityRequirements(EnumMap<AbilityType, Integer> requiredCounts) {
        this.requiredCounts = requiredCounts;
    }

    public static AbilityRequirements from(Map<AbilityType, Integer> requiredCounts) {
        Objects.requireNonNull(requiredCounts, "requiredCounts");
        EnumMap<AbilityType, Integer> normalized = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            Integer requiredCount = requiredCounts.get(abilityType);
            if (requiredCount == null) {
                throw new IllegalArgumentException("Missing required count for ability " + abilityType.name());
            }
            normalized.put(abilityType, requiredCount);
        }
        return new AbilityRequirements(normalized);
    }

    public static AbilityRequirements normalize(Map<AbilityType, Integer> requiredCounts) {
        Objects.requireNonNull(requiredCounts, "requiredCounts");
        EnumMap<AbilityType, Integer> normalized = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            normalized.put(abilityType, requiredCounts.getOrDefault(abilityType, ProgressionConstants.INITIALLY_UNLOCKED_FLAG));
        }
        return new AbilityRequirements(normalized);
    }

    public int getRequiredCount(AbilityType abilityType) {
        return requiredCounts.get(Objects.requireNonNull(abilityType, "abilityType"));
    }

    public Map<AbilityType, Integer> asMap() {
        return Collections.unmodifiableMap(requiredCounts);
    }
}
