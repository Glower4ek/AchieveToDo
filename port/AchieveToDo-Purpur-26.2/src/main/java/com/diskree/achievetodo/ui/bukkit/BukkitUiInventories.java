package com.diskree.achievetodo.ui.bukkit;

import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public final class BukkitUiInventories {

    private BukkitUiInventories() {
    }

    public static boolean isOwnedUi(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof BukkitUiInventoryHolder;
    }

    public static boolean isOwnedUi(InventoryView view) {
        return view != null && isOwnedUi(view.getTopInventory());
    }

    public static boolean isOwnedUi(InventoryEvent event) {
        return event != null && isOwnedUi(event.getView());
    }
}
