package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;

public final class AbilityStateResolverTest {

    private AbilityStateResolverTest() {
    }

    public static void run() {
        ResolvedAbilityState state = AbilityStateResolver.resolve(0, 0, false);
        assertEquals(AbilityStateSnapshot.UNLOCKED, state.snapshot());
        state = AbilityStateResolver.resolve(5, 10, false);
        assertEquals(AbilityStateSnapshot.LOCKED, state.snapshot());
        state = AbilityStateResolver.resolve(5, 10, true);
        assertEquals(AbilityStateSnapshot.DEMYSTIFIED, state.snapshot());
        state = AbilityStateResolver.resolve(10, 10, true);
        assertEquals(AbilityStateSnapshot.UNLOCKED, state.snapshot());
        state = AbilityStateResolver.resolve(100, -1, true);
        assertEquals(AbilityStateSnapshot.PERMANENTLY_LOCKED, state.snapshot());

        AbilityRequirements requirements = AbilityRequirements.from(singleAbilityRequirement(AbilityType.OPEN_CHEST, 10));
        PlayerProgressionState initial = PlayerProgressionState.create(UUID.randomUUID(), 10, requirements)
            .withDemystified(AbilityType.OPEN_CHEST);
        PlayerProgressionState relocked = initial.withObtainedAdvancementCount(9);
        assertEquals(AbilityStateSnapshot.UNLOCKED, initial.getAbilityState(AbilityType.OPEN_CHEST).snapshot());
        assertEquals(AbilityStateSnapshot.DEMYSTIFIED, relocked.getAbilityState(AbilityType.OPEN_CHEST).snapshot());
    }

    private static Map<AbilityType, Integer> singleAbilityRequirement(AbilityType targetAbility, int requiredCount) {
        EnumMap<AbilityType, Integer> requirements = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            requirements.put(abilityType, 0);
        }
        requirements.put(targetAbility, requiredCount);
        return requirements;
    }
}
