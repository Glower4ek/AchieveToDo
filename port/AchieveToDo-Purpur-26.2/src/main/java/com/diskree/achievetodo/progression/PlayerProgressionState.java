package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PlayerProgressionState {

    private final UUID playerId;
    private final boolean ready;
    private final int obtainedAdvancementCount;
    private final AbilityRequirements requirements;
    private final Set<AbilityType> demystifiedAbilities;
    private final EnumMap<AbilityType, ResolvedAbilityState> abilityStates;

    private PlayerProgressionState(
        UUID playerId,
        boolean ready,
        int obtainedAdvancementCount,
        AbilityRequirements requirements,
        Set<AbilityType> demystifiedAbilities
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.ready = ready;
        this.obtainedAdvancementCount = obtainedAdvancementCount;
        this.requirements = Objects.requireNonNull(requirements, "requirements");
        this.demystifiedAbilities = Collections.unmodifiableSet(copyDemystifiedAbilities(demystifiedAbilities));
        this.abilityStates = resolveAbilityStates(ready, requirements, obtainedAdvancementCount, this.demystifiedAbilities);
    }

    public static PlayerProgressionState create(UUID playerId, int obtainedAdvancementCount, AbilityRequirements requirements) {
        return new PlayerProgressionState(playerId, true, obtainedAdvancementCount, requirements, EnumSet.noneOf(AbilityType.class));
    }

    public static PlayerProgressionState unavailable(UUID playerId, AbilityRequirements requirements) {
        return new PlayerProgressionState(playerId, false, 0, requirements, EnumSet.noneOf(AbilityType.class));
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isReady() {
        return ready;
    }

    public int getObtainedAdvancementCount() {
        return obtainedAdvancementCount;
    }

    public AbilityRequirements getRequirements() {
        return requirements;
    }

    public Set<AbilityType> getDemystifiedAbilities() {
        return demystifiedAbilities;
    }

    public PlayerProgressionState withObtainedAdvancementCount(int newObtainedAdvancementCount) {
        return new PlayerProgressionState(playerId, ready, newObtainedAdvancementCount, requirements, demystifiedAbilities);
    }

    public PlayerProgressionState withReady(boolean newReady) {
        return new PlayerProgressionState(playerId, newReady, obtainedAdvancementCount, requirements, demystifiedAbilities);
    }

    public PlayerProgressionState withDemystified(AbilityType abilityType) {
        EnumSet<AbilityType> updatedDemystifiedAbilities = copyDemystifiedAbilities(demystifiedAbilities);
        updatedDemystifiedAbilities.add(Objects.requireNonNull(abilityType, "abilityType"));
        return new PlayerProgressionState(playerId, ready, obtainedAdvancementCount, requirements, updatedDemystifiedAbilities);
    }

    public PlayerProgressionState withDemystifiedAbilities(Set<AbilityType> abilities) {
        return new PlayerProgressionState(playerId, ready, obtainedAdvancementCount, requirements, abilities);
    }

    public ResolvedAbilityState getAbilityState(AbilityType abilityType) {
        return abilityStates.get(Objects.requireNonNull(abilityType, "abilityType"));
    }

    public Map<AbilityType, ResolvedAbilityState> getAbilityStates() {
        return Collections.unmodifiableMap(abilityStates);
    }

    private static EnumSet<AbilityType> copyDemystifiedAbilities(Set<AbilityType> abilities) {
        EnumSet<AbilityType> copy = EnumSet.noneOf(AbilityType.class);
        copy.addAll(abilities);
        return copy;
    }

    private static EnumMap<AbilityType, ResolvedAbilityState> resolveAbilityStates(
        boolean ready,
        AbilityRequirements requirements,
        int obtainedAdvancementCount,
        Set<AbilityType> demystifiedAbilities
    ) {
        EnumMap<AbilityType, ResolvedAbilityState> states = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            boolean demystified = demystifiedAbilities.contains(abilityType);
            if (!ready) {
                states.put(abilityType, new ResolvedAbilityState(AbilityLockKind.LOCKED, demystified));
                continue;
            }
            states.put(abilityType, AbilityStateResolver.resolve(
                obtainedAdvancementCount,
                requirements.getRequiredCount(abilityType),
                demystified
            ));
        }
        return states;
    }
}
