package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.LandmarkType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RestrictionDecision(
    RestrictionDecisionKind kind,
    AbilityType ability,
    LandmarkType landmark,
    AbilityType landmarkAbility,
    Set<LandmarkType> deniedLandmarks,
    String reason,
    Map<String, String> messageMetadata
) {

    public RestrictionDecision {
        kind = Objects.requireNonNull(kind, "kind");
        reason = normalizeReason(reason);
        deniedLandmarks = Set.copyOf(new LinkedHashSet<>(deniedLandmarks == null ? Set.of() : deniedLandmarks));
        messageMetadata = Map.copyOf(new LinkedHashMap<>(messageMetadata == null ? Map.of() : messageMetadata));
    }

    public static RestrictionDecision allow() {
        return new RestrictionDecision(RestrictionDecisionKind.ALLOW, null, null, null, Set.of(), "allow", Map.of());
    }

    public static RestrictionDecision denyAbility(AbilityType ability, String reason) {
        return new RestrictionDecision(
            RestrictionDecisionKind.DENY_ABILITY,
            Objects.requireNonNull(ability, "ability"),
            null,
            null,
            Set.of(),
            reason,
            Map.of()
        );
    }

    public static RestrictionDecision denyLandmark(
        LandmarkType landmark,
        AbilityType landmarkAbility,
        Set<LandmarkType> deniedLandmarks,
        String reason
    ) {
        return new RestrictionDecision(
            RestrictionDecisionKind.DENY_LANDMARK,
            null,
            Objects.requireNonNull(landmark, "landmark"),
            landmarkAbility,
            deniedLandmarks,
            reason,
            Map.of()
        );
    }

    public static RestrictionDecision denyBoth(
        AbilityType ability,
        LandmarkType landmark,
        AbilityType landmarkAbility,
        Set<LandmarkType> deniedLandmarks,
        String reason
    ) {
        return new RestrictionDecision(
            RestrictionDecisionKind.DENY_BOTH,
            Objects.requireNonNull(ability, "ability"),
            Objects.requireNonNull(landmark, "landmark"),
            landmarkAbility,
            deniedLandmarks,
            reason,
            Map.of()
        );
    }

    public boolean isAllowed() {
        return kind == RestrictionDecisionKind.ALLOW;
    }

    public boolean deniesAbility() {
        return kind == RestrictionDecisionKind.DENY_ABILITY || kind == RestrictionDecisionKind.DENY_BOTH;
    }

    public boolean deniesLandmark() {
        return kind == RestrictionDecisionKind.DENY_LANDMARK || kind == RestrictionDecisionKind.DENY_BOTH;
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            return "unspecified";
        }
        String normalized = reason.trim();
        return normalized.isEmpty() ? "unspecified" : normalized;
    }
}
