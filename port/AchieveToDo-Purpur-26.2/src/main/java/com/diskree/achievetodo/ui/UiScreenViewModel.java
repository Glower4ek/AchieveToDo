package com.diskree.achievetodo.ui;

import java.util.Map;
import java.util.Objects;

public record UiScreenViewModel(
    String title,
    UiRoute route,
    int totalPages,
    Map<Integer, UiTabViewModel> tabs,
    Map<Integer, UiAbilityViewModel> abilities,
    Map<Integer, UiSlotAction> actions,
    String footerLine,
    String pageLine
) {

    public UiScreenViewModel {
        title = Objects.requireNonNull(title, "title");
        route = Objects.requireNonNull(route, "route");
        tabs = Map.copyOf(Objects.requireNonNull(tabs, "tabs"));
        abilities = Map.copyOf(Objects.requireNonNull(abilities, "abilities"));
        actions = Map.copyOf(Objects.requireNonNull(actions, "actions"));
        footerLine = Objects.requireNonNull(footerLine, "footerLine");
        pageLine = Objects.requireNonNull(pageLine, "pageLine");
    }
}
