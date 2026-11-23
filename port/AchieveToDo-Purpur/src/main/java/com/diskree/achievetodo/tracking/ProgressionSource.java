package com.diskree.achievetodo.tracking;

import java.util.Optional;
import java.util.UUID;

public interface ProgressionSource {

    boolean isReady();

    Optional<DetectedAdvancementsObjective> getDetectedObjective();

    int getObtainedAdvancementCount(UUID playerId);

    int getTrackedScore(UUID playerId, TrackedScoreType type);

    int getTrackedStatistic(UUID playerId, TrackedStatisticsDataType type);

    int getNearbyEntityProgress(UUID playerId, TrackedNearbyEntitiesType type);

    Optional<TeamProgressionSnapshot> getTeamProgress(UUID playerId);
}
