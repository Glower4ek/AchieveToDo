package com.diskree.achievetodo.ui.bukkit;

import org.bukkit.event.inventory.ClickType;

import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class BukkitUiClickPolicyTest {

    private BukkitUiClickPolicyTest() {
    }

    public static void run() {
        assertTrue(BukkitUiClickPolicy.isSemanticUiClick(true, ClickType.LEFT));
        assertTrue(BukkitUiClickPolicy.isSemanticUiClick(true, ClickType.RIGHT));
        assertFalse(BukkitUiClickPolicy.isSemanticUiClick(true, ClickType.NUMBER_KEY));
        assertFalse(BukkitUiClickPolicy.isSemanticUiClick(true, ClickType.SHIFT_LEFT));
        assertFalse(BukkitUiClickPolicy.isSemanticUiClick(true, ClickType.SWAP_OFFHAND));
        assertFalse(BukkitUiClickPolicy.isSemanticUiClick(false, ClickType.LEFT));
    }
}
