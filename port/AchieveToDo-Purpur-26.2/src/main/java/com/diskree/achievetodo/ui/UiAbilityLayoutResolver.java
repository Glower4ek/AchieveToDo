package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiAbilityLayoutResolver {

    private static final int CONTENT_ROW_COUNT = 4;
    private static final int CONTENT_COLUMNS = 9;
    private final Map<UiAbilityGroup, List<AbilityType>> abilitiesByGroup;

    public UiAbilityLayoutResolver() {
        this.abilitiesByGroup = buildAbilitiesByGroup();
    }

    public UiAbilityLayout resolve(UiRoute requestedRoute) {
        List<AbilityType> groupAbilities = abilitiesByGroup.get(requestedRoute.group());
        List<List<AbilityType>> logicalRows = splitIntoRows(groupAbilities, requestedRoute.group().rowCount());
        int totalPages = determineTotalPages(logicalRows);
        int safePage = Math.min(requestedRoute.page(), totalPages - 1);
        Map<Integer, AbilityType> slotToAbility = new LinkedHashMap<>();
        for (int rowIndex = 0; rowIndex < CONTENT_ROW_COUNT; rowIndex++) {
            List<AbilityType> logicalRow = rowIndex < logicalRows.size() ? logicalRows.get(rowIndex) : List.of();
            int pageStart = safePage * CONTENT_COLUMNS;
            for (int column = 0; column < CONTENT_COLUMNS; column++) {
                int logicalIndex = pageStart + column;
                if (logicalIndex >= logicalRow.size()) {
                    continue;
                }
                slotToAbility.put(contentSlot(rowIndex, column), logicalRow.get(logicalIndex));
            }
        }
        return new UiAbilityLayout(new UiRoute(requestedRoute.group(), safePage), totalPages, slotToAbility);
    }

    public Map<UiAbilityGroup, List<AbilityType>> abilitiesByGroup() {
        return abilitiesByGroup;
    }

    static int contentSlot(int rowIndex, int column) {
        return 9 + (rowIndex * CONTENT_COLUMNS) + column;
    }

    private static Map<UiAbilityGroup, List<AbilityType>> buildAbilitiesByGroup() {
        Map<UiAbilityGroup, List<AbilityType>> grouped = new EnumMap<>(UiAbilityGroup.class);
        for (UiAbilityGroup group : UiAbilityGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }
        for (AbilityType abilityType : AbilityType.values()) {
            grouped.get(UiAbilityGroup.fromLayer(abilityType.getHierarchyLayerType())).add(abilityType);
        }
        Map<UiAbilityGroup, List<AbilityType>> immutable = new EnumMap<>(UiAbilityGroup.class);
        for (Map.Entry<UiAbilityGroup, List<AbilityType>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutable);
    }

    private static List<List<AbilityType>> splitIntoRows(List<AbilityType> abilities, int rowCount) {
        int normalizedRows = Math.max(1, Math.min(CONTENT_ROW_COUNT, rowCount));
        List<List<AbilityType>> rows = new ArrayList<>(normalizedRows);
        int baseSize = abilities.size() / normalizedRows;
        int remainder = abilities.size() % normalizedRows;
        int cursor = 0;
        for (int rowIndex = 0; rowIndex < normalizedRows; rowIndex++) {
            int rowSize = baseSize + (rowIndex < remainder ? 1 : 0);
            rows.add(List.copyOf(abilities.subList(cursor, cursor + rowSize)));
            cursor += rowSize;
        }
        return rows;
    }

    private static int determineTotalPages(List<List<AbilityType>> rows) {
        int totalPages = 1;
        for (List<AbilityType> row : rows) {
            totalPages = Math.max(totalPages, (row.size() + CONTENT_COLUMNS - 1) / CONTENT_COLUMNS);
        }
        return totalPages;
    }
}
