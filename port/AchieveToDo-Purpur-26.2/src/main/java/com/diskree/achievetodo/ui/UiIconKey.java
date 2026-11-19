package com.diskree.achievetodo.ui;

import java.util.Objects;

public record UiIconKey(String value) {

    public UiIconKey {
        value = Objects.requireNonNull(value, "value");
    }

    public static UiIconKey ability(String abilityName) {
        return new UiIconKey("ability." + abilityName);
    }

    public static UiIconKey category(UiAbilityGroup group) {
        return new UiIconKey("category." + group.name().toLowerCase());
    }

    public static UiIconKey navigation(String key) {
        return new UiIconKey("navigation." + key);
    }

    public static UiIconKey state(String key) {
        return new UiIconKey("state." + key);
    }
}
