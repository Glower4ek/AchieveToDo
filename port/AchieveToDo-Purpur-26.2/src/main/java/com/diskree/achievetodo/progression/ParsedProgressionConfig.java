package com.diskree.achievetodo.progression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ParsedProgressionConfig(int version, boolean hasAbilitiesTable, Map<String, Integer> rawAbilities) {

    public ParsedProgressionConfig {
        rawAbilities = Collections.unmodifiableMap(new LinkedHashMap<>(rawAbilities));
    }
}
