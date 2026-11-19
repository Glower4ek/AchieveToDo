package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Map;
import java.util.Objects;

public record UiAbilityLayout(UiRoute route, int totalPages, Map<Integer, AbilityType> slotToAbility) {

    public UiAbilityLayout {
        route = Objects.requireNonNull(route, "route");
        totalPages = Math.max(1, totalPages);
        slotToAbility = Map.copyOf(Objects.requireNonNull(slotToAbility, "slotToAbility"));
    }
}
