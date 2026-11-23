package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Objects;
import java.util.UUID;

public interface DemystificationSink {

    DemystificationSink NO_OP = (playerId, abilityType) -> {
    };

    void demystify(UUID playerId, AbilityType abilityType);

    static DemystificationSink noop() {
        return NO_OP;
    }

    static DemystificationSink requireNonNull(DemystificationSink sink) {
        return Objects.requireNonNull(sink, "sink");
    }
}
