package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.diskree.achievetodo.ui.bukkit.BukkitUiInventories;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public final class BundleAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public BundleAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !touchesBundle(event.getCursor(), event.getCurrentItem())) {
            return;
        }
        if (restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.BUNDLE_MUTATION,
            restrictionService.abilityList(AbilityType.PUT_IN_BUNDLE),
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        )) {
            player.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !isBundle(event.getCursor())) {
            return;
        }
        if (restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.BUNDLE_MUTATION,
            restrictionService.abilityList(AbilityType.PUT_IN_BUNDLE),
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        )) {
            player.closeInventory();
        }
    }

    private boolean touchesBundle(ItemStack first, ItemStack second) {
        return isBundle(first) || isBundle(second);
    }

    private boolean isBundle(ItemStack item) {
        return item != null && item.getType() == org.bukkit.Material.BUNDLE;
    }
}
