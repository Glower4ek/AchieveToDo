package com.diskree.achievetodo.tracking;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class BacapObjectiveDetector {

    private static final List<ProgressionDisplaySlot> PRIORITY = List.of(
        ProgressionDisplaySlot.SIDEBAR,
        ProgressionDisplaySlot.LIST,
        ProgressionDisplaySlot.BELOW_NAME
    );

    public Optional<DetectedAdvancementsObjective> detect(Map<ProgressionDisplaySlot, String> slotObjectives) {
        Objects.requireNonNull(slotObjectives, "slotObjectives");
        for (ProgressionDisplaySlot slot : PRIORITY) {
            String objectiveName = slotObjectives.get(slot);
            Optional<AdvancementsMode> mode = AdvancementsMode.findByObjectiveName(objectiveName);
            if (mode.isPresent()) {
                return Optional.of(new DetectedAdvancementsObjective(mode.get(), objectiveName, slot));
            }
        }
        return Optional.empty();
    }
}
