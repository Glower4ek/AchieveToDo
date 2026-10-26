package com.diskree.achievetodo.progression;

import java.util.Objects;

public record ResolvedProgressionConfig(
    WorldProgressionConfig worldSelection,
    String fileName,
    ProgressionConfigDefinition definition,
    boolean generated
) {

    public ResolvedProgressionConfig {
        worldSelection = Objects.requireNonNull(worldSelection, "worldSelection");
        fileName = Objects.requireNonNull(fileName, "fileName");
        definition = Objects.requireNonNull(definition, "definition");
    }
}
