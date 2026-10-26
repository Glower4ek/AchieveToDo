package com.diskree.achievetodo.ui.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public record BukkitUiInventoryHolder(UUID ownerId, long sessionId) implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}
