package com.diskree.achievetodo.progression;

import java.util.Objects;

public record ProgressionConfigDefinition(
    String id,
    int version,
    ProgressionModeType builtInMode,
    AbilityRequirements requirements
) {

    public ProgressionConfigDefinition {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        requirements = Objects.requireNonNull(requirements, "requirements");
    }
}
