package com.diskree.achievetodo.tracking;

import java.util.Objects;

public record DetectedAdvancementsObjective(
    AdvancementsMode mode,
    String objectiveName,
    ProgressionDisplaySlot displaySlot
) {

    public DetectedAdvancementsObjective {
        mode = Objects.requireNonNull(mode, "mode");
        objectiveName = Objects.requireNonNull(objectiveName, "objectiveName");
        displaySlot = Objects.requireNonNull(displaySlot, "displaySlot");
    }
}
