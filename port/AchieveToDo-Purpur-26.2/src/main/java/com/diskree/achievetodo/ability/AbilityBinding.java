package com.diskree.achievetodo.ability;

import java.util.Locale;
import java.util.Objects;

public record AbilityBinding(AbilityBindingType type, String key) {

    public AbilityBinding {
        type = Objects.requireNonNull(type, "type");
        key = normalizeKey(key);
    }

    public static AbilityBinding item(String key) {
        return new AbilityBinding(AbilityBindingType.ITEM, key);
    }

    public static AbilityBinding block(String key) {
        return new AbilityBinding(AbilityBindingType.BLOCK, key);
    }

    public static AbilityBinding food(String key) {
        return new AbilityBinding(AbilityBindingType.FOOD, key);
    }

    public static AbilityBinding toolMaterial(String key) {
        return new AbilityBinding(AbilityBindingType.TOOL_MATERIAL, key);
    }

    public static AbilityBinding equipmentMaterial(String key) {
        return new AbilityBinding(AbilityBindingType.EQUIPMENT_MATERIAL, key);
    }

    public static AbilityBinding equipmentItem(String key) {
        return new AbilityBinding(AbilityBindingType.EQUIPMENT_ITEM, key);
    }

    public static AbilityBinding portal(String key) {
        return new AbilityBinding(AbilityBindingType.PORTAL, key);
    }

    public static AbilityBinding tradeTarget(String key) {
        return new AbilityBinding(AbilityBindingType.TRADE_TARGET, key);
    }

    public static AbilityBinding landmark(LandmarkType landmarkType) {
        return new AbilityBinding(AbilityBindingType.LANDMARK, landmarkType.getName());
    }

    public LandmarkType landmarkType() {
        if (type != AbilityBindingType.LANDMARK) {
            return null;
        }
        return LandmarkType.findByName(key).orElse(null);
    }

    public static String normalizeKey(String key) {
        String normalized = Objects.requireNonNull(key, "key").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Binding key cannot be blank");
        }
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
