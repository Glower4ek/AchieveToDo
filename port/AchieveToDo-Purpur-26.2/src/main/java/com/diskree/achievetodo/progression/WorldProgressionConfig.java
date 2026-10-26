package com.diskree.achievetodo.progression;

import java.util.Objects;

public record WorldProgressionConfig(String configName) {

    public WorldProgressionConfig {
        configName = Objects.requireNonNull(configName, "configName").trim();
        if (configName.isEmpty()) {
            throw new IllegalArgumentException("configName cannot be blank");
        }
        if (!configName.matches("[A-Za-z0-9_-]+") || configName.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
            throw new IllegalArgumentException("Unsafe progression config name: " + configName);
        }
    }
}
