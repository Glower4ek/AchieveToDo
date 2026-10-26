package com.diskree.achievetodo.progression;

import java.util.Optional;

public interface WorldProgressionConfigStore {
    Optional<WorldProgressionConfig> load();

    void save(WorldProgressionConfig config);
}
