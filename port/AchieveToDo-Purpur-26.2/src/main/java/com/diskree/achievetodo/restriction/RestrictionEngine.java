package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.ability.AbilityResolvers;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.landmark.LandmarkEngine;
import com.diskree.achievetodo.landmark.LandmarkQueryResult;
import com.diskree.achievetodo.landmark.LandmarkRecord;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import com.diskree.achievetodo.progression.AbilityLockKind;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ResolvedAbilityState;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class RestrictionEngine {

    private final LandmarkEngine landmarkEngine;

    public RestrictionEngine(LandmarkEngine landmarkEngine) {
        this.landmarkEngine = Objects.requireNonNull(landmarkEngine, "landmarkEngine");
    }

    public RestrictionDecision checkAbility(
        PlayerProgressionState progressionState,
        AbilityType abilityType,
        RestrictionActorMode actorMode
    ) {
        Objects.requireNonNull(progressionState, "progressionState");
        if (abilityType == null || isBypassed(actorMode)) {
            return RestrictionDecision.allow();
        }
        if (isAbilityLocked(progressionState, abilityType)) {
            return RestrictionDecision.denyAbility(abilityType, "ability_locked");
        }
        return RestrictionDecision.allow();
    }

    public RestrictionDecision checkLandmark(
        PlayerProgressionState progressionState,
        LandmarkWorld world,
        BlockBox targetBox,
        RestrictionActorMode actorMode
    ) {
        Objects.requireNonNull(progressionState, "progressionState");
        if (world == null || targetBox == null || isBypassed(actorMode)) {
            return RestrictionDecision.allow();
        }
        LandmarkQueryResult query = landmarkEngine.query(world, targetBox);
        if (query.isEmpty()) {
            return RestrictionDecision.allow();
        }

        Set<LandmarkType> deniedLandmarks = new LinkedHashSet<>();
        LandmarkType primaryLandmark = null;
        AbilityType primaryLandmarkAbility = null;
        for (LandmarkRecord record : query.matches()) {
            AbilityType landmarkAbility = AbilityResolvers.findLandmarkAbility(record.landmarkType()).orElse(null);
            if (landmarkAbility == null || !isAbilityLocked(progressionState, landmarkAbility)) {
                continue;
            }
            deniedLandmarks.add(record.landmarkType());
            if (primaryLandmark == null) {
                primaryLandmark = record.landmarkType();
                primaryLandmarkAbility = landmarkAbility;
            }
        }
        if (primaryLandmark == null) {
            return RestrictionDecision.allow();
        }
        return RestrictionDecision.denyLandmark(primaryLandmark, primaryLandmarkAbility, deniedLandmarks, "landmark_locked");
    }

    public RestrictionDecision check(PlayerProgressionState progressionState, RestrictionContext context) {
        Objects.requireNonNull(progressionState, "progressionState");
        Objects.requireNonNull(context, "context");

        RestrictionDecision abilityDecision = checkAbility(
            progressionState,
            context.abilityCandidate(),
            context.actorMode()
        );
        RestrictionDecision landmarkDecision = checkLandmark(
            progressionState,
            context.target().world(),
            context.target().blockBox(),
            context.actorMode()
        );
        if (abilityDecision.isAllowed() && landmarkDecision.isAllowed()) {
            return RestrictionDecision.allow();
        }
        if (abilityDecision.isAllowed()) {
            return landmarkDecision;
        }
        if (landmarkDecision.isAllowed()) {
            return abilityDecision;
        }
        return RestrictionDecision.denyBoth(
            abilityDecision.ability(),
            landmarkDecision.landmark(),
            landmarkDecision.landmarkAbility(),
            landmarkDecision.deniedLandmarks(),
            "ability_and_landmark_locked"
        );
    }

    public boolean isAbilityLocked(PlayerProgressionState progressionState, AbilityType abilityType) {
        ResolvedAbilityState resolvedAbilityState = progressionState.getAbilityState(abilityType);
        return resolvedAbilityState.kind() != AbilityLockKind.UNLOCKED;
    }

    private static boolean isBypassed(RestrictionActorMode actorMode) {
        return actorMode != null && actorMode.bypassesRestrictions();
    }
}
