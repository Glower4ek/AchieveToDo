package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.List;
import java.util.Objects;

public record UiAbilityViewModel(
    AbilityType abilityType,
    UiAbilityState state,
    UiIconKey iconKey,
    String title,
    List<String> lore,
    boolean revealsIdentity
) {

    public UiAbilityViewModel {
        abilityType = Objects.requireNonNull(abilityType, "abilityType");
        state = Objects.requireNonNull(state, "state");
        iconKey = Objects.requireNonNull(iconKey, "iconKey");
        title = Objects.requireNonNull(title, "title");
        lore = List.copyOf(Objects.requireNonNull(lore, "lore"));
    }
}
