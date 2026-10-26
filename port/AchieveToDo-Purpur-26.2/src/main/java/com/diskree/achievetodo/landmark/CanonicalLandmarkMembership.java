package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import java.util.*;

/** Session-only projection of canonical setObtainedCount; never a progression authority or disk ledger. */
public final class CanonicalLandmarkMembership {
    private record Session(PlayerProgressionState previous, EnumSet<LandmarkType> types) {}
    private final Map<UUID, Session> sessions = new HashMap<>();
    // Canonical uses a shared HashMap, including empty keys retained by disconnect.
    // Per-player sets or sorting change first-match semantics after resize/removal.
    private final Map<LandmarkType, Set<UUID>> playersByLockedLandmarkTypes = new HashMap<>();

    public void observe(PlayerProgressionState state) {
        if (state == null || !state.isReady()) return; // Canonical isNotReady returns before updating oldCount.
        UUID actor = state.getPlayerId();
        Session session = sessions.get(actor);
        int oldCount = session == null ? Integer.MIN_VALUE : session.previous().getObtainedAdvancementCount();
        int count = state.getObtainedAdvancementCount();
        var types = session == null ? EnumSet.noneOf(LandmarkType.class) : session.types();
        for (AbilityType ability : AbilityType.values()) {
            LandmarkType type = ability.getLandmarkType();
            if (type == null) continue;
            int required = state.getRequirements().getRequiredCount(ability);
            if (required == 0 || required == -1) continue;
            if (count >= required && oldCount < required) {
                types.remove(type);
                Set<UUID> players = playersByLockedLandmarkTypes.get(type);
                if (players != null && players.remove(actor) && players.isEmpty()) playersByLockedLandmarkTypes.remove(type);
            } else if (oldCount == Integer.MIN_VALUE || count < required && oldCount >= required) {
                types.add(type);
                playersByLockedLandmarkTypes.computeIfAbsent(type, ignored -> new HashSet<>()).add(actor);
            }
        }
        sessions.put(actor, new Session(state, types));
    }

    public boolean contains(UUID actor, LandmarkType type) {
        Session session = sessions.get(actor);
        return session != null && session.types().contains(type);
    }
    public Set<LandmarkType> types(UUID actor) {
        Session session = sessions.get(actor);
        return session == null ? Set.of() : Set.copyOf(session.types());
    }
    public void disconnect(UUID actor) {
        sessions.remove(actor);
        for (Set<UUID> players : playersByLockedLandmarkTypes.values()) players.remove(actor);
    }
    public void clear() { sessions.clear(); playersByLockedLandmarkTypes.clear(); }

    List<LandmarkType> iterationOrder() { return List.copyOf(playersByLockedLandmarkTypes.keySet()); }

    /** An explicit canonical iteration order is useful to an oracle, but must not be invented by the runtime. */
    public ExactLandmarkGate.Result ordered(PlayerProgressionState state, UUID actor, boolean bypass,
                                            List<LandmarkRecord> canonicalOrder) {
        if (bypass) return ExactLandmarkGate.Result.BYPASS;
        for (var record : canonicalOrder) {
            if (!contains(actor, record.landmarkType())) continue;
            if (state == null || !state.isReady() || !actor.equals(state.getPlayerId())) return ExactLandmarkGate.Result.UNAVAILABLE;
            return numeric(state, record.landmarkType()); // Includes false; do not continue searching.
        }
        return ExactLandmarkGate.Result.OUTSIDE;
    }

    /** First spatial result in the canonical shared HashMap, including an unlocked result. */
    public ExactLandmarkGate.Result evaluate(PlayerProgressionState state, UUID actor, boolean bypass,
                                             Set<LandmarkRecord> matches) {
        if (bypass) return ExactLandmarkGate.Result.BYPASS;
        if (matches.isEmpty()) return ExactLandmarkGate.Result.OUTSIDE;
        for (var entry : playersByLockedLandmarkTypes.entrySet()) {
            if (!entry.getValue().contains(actor)) continue;
            for (var record : matches) {
                if (record.landmarkType() != entry.getKey()) continue;
                if (state == null || !state.isReady() || !actor.equals(state.getPlayerId())) return ExactLandmarkGate.Result.UNAVAILABLE;
                return numeric(state, entry.getKey());
            }
        }
        return ExactLandmarkGate.Result.OUTSIDE;
    }

    private static ExactLandmarkGate.Result numeric(PlayerProgressionState state, LandmarkType type) {
        return state.getAbilityState(AbilityResolvers.findLandmarkAbility(type).orElseThrow()).isUnlocked()
            ? ExactLandmarkGate.Result.UNLOCKED : ExactLandmarkGate.Result.LOCKED;
    }
}
