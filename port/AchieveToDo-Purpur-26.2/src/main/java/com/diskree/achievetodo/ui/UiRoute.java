package com.diskree.achievetodo.ui;

import java.util.Objects;

public record UiRoute(UiAbilityGroup group, int page) {

    public UiRoute {
        group = Objects.requireNonNull(group, "group");
        page = Math.max(0, page);
    }

    public static UiRoute initial() {
        return new UiRoute(UiAbilityGroup.MAIN, 0);
    }

    public UiRoute withGroup(UiAbilityGroup group) {
        return new UiRoute(group, 0);
    }

    public UiRoute withPage(int page) {
        return new UiRoute(group, page);
    }
}
