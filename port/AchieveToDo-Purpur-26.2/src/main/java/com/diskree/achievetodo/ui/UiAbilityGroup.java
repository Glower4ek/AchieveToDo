package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilitiesHierarchyLayerType;

import java.util.LinkedHashMap;
import java.util.Map;

public enum UiAbilityGroup {

    MAIN(AbilitiesHierarchyLayerType.MAIN, "achievetodo.ui.group.main"),
    ACTIONS(AbilitiesHierarchyLayerType.ACTIONS, "achievetodo.ui.group.actions"),
    BLOCKS(AbilitiesHierarchyLayerType.BLOCKS, "achievetodo.ui.group.blocks"),
    UPGRADE(AbilitiesHierarchyLayerType.UPGRADE, "achievetodo.ui.group.upgrade"),
    FOOD(AbilitiesHierarchyLayerType.FOOD, "achievetodo.ui.group.food"),
    TRADING(AbilitiesHierarchyLayerType.TRADING, "achievetodo.ui.group.trading"),
    LANDMARK(AbilitiesHierarchyLayerType.LANDMARK, "achievetodo.ui.group.landmark");

    private static final Map<AbilitiesHierarchyLayerType, UiAbilityGroup> BY_LAYER = buildByLayer();

    private final AbilitiesHierarchyLayerType layerType;
    private final String translationKey;

    UiAbilityGroup(AbilitiesHierarchyLayerType layerType, String translationKey) {
        this.layerType = layerType;
        this.translationKey = translationKey;
    }

    public AbilitiesHierarchyLayerType layerType() {
        return layerType;
    }

    public String translationKey() {
        return translationKey;
    }

    public int rowCount() {
        return layerType.getRowsCount();
    }

    public static UiAbilityGroup fromLayer(AbilitiesHierarchyLayerType layerType) {
        UiAbilityGroup group = BY_LAYER.get(layerType);
        if (group == null) {
            throw new IllegalArgumentException("Unsupported ability layer: " + layerType);
        }
        return group;
    }

    private static Map<AbilitiesHierarchyLayerType, UiAbilityGroup> buildByLayer() {
        Map<AbilitiesHierarchyLayerType, UiAbilityGroup> byLayer = new LinkedHashMap<>();
        for (UiAbilityGroup group : values()) {
            byLayer.put(group.layerType, group);
        }
        return Map.copyOf(byLayer);
    }
}
