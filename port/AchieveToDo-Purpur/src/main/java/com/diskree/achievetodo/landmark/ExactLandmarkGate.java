package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import java.util.*;

/** Result vocabulary and historical S4G stateless regression fixture. Native runtime uses CanonicalLandmarkMembership. */
public final class ExactLandmarkGate {
    public enum Result { OUTSIDE, BYPASS, UNLOCKED, LOCKED, UNAVAILABLE, UNSUPPORTED_MEMBERSHIP }

    @Deprecated
    public static Result evaluate(PlayerProgressionState state, UUID actor, boolean bypass, Set<LandmarkRecord> matches) {
        if (bypass) return Result.BYPASS;
        if (matches.isEmpty()) return Result.OUTSIDE;
        if (state == null || !state.isReady() || !actor.equals(state.getPlayerId())) return Result.UNAVAILABLE;
        Set<AbilityType> abilities = new HashSet<>();
        for (var match : matches) abilities.add(AbilityResolvers.findLandmarkAbility(match.landmarkType()).orElseThrow());
        // Canonical first-match and retained membership are not a stateless any-locked predicate.
        if (abilities.size() != 1) return Result.UNSUPPORTED_MEMBERSHIP;
        var ability = abilities.iterator().next();
        int threshold = state.getRequirements().getRequiredCount(ability);
        if (threshold < 0) return Result.UNSUPPORTED_MEMBERSHIP;
        return state.getAbilityState(ability).isUnlocked() ? Result.UNLOCKED : Result.LOCKED;
    }
    public static boolean denied(Result result) {
        return result == Result.LOCKED || result == Result.UNAVAILABLE || result == Result.UNSUPPORTED_MEMBERSHIP;
    }
    public static boolean denied(Result result, PlayerProgressionState state, UUID actor, AbilityType additionalAbility) {
        if (result == Result.BYPASS) return false;
        // Spatial membership is not progression authority. An empty/previously unlocked
        // membership projection must not authorize a transaction after S3C loses its source.
        if (state == null || !state.isReady() || !actor.equals(state.getPlayerId())) return true;
        if (denied(result)) return true;
        if (additionalAbility == null) return false;
        return !state.getAbilityState(additionalAbility).isUnlocked();
    }
    private ExactLandmarkGate() {}
}
