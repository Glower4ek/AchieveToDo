package com.diskree.achievetodo.ui.bukkit;

import java.util.Locale;
import java.util.Optional;

public enum UiPresentationMode {

    FALLBACK("off"),
    ENHANCED("on");

    private final String commandValue;

    UiPresentationMode(String commandValue) {
        this.commandValue = commandValue;
    }

    public String commandValue() {
        return commandValue;
    }

    public static Optional<UiPresentationMode> fromCommandValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (UiPresentationMode mode : values()) {
            if (mode.commandValue.equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
