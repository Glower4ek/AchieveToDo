package com.diskree.achievetodo.tracking;

import java.util.UUID;

public interface NearbyEntityProgressProvider {

    int getNearbyEntityProgress(UUID playerId, TrackedNearbyEntitiesType type);
}
