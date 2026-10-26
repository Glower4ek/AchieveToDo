package com.diskree.achievetodo.ui.bukkit;

import org.bukkit.event.inventory.ClickType;

import java.util.Objects;

final class BukkitUiClickPolicy {

    private BukkitUiClickPolicy() {
    }

    static boolean isSemanticUiClick(boolean clickedTopInventory, ClickType clickType) {
        Objects.requireNonNull(clickType, "clickType");
        if (!clickedTopInventory) {
            return false;
        }
        return clickType == ClickType.LEFT || clickType == ClickType.RIGHT;
    }
}
