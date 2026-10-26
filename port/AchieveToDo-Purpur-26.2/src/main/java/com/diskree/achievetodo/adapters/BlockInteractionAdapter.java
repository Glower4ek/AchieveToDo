package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.diskree.achievetodo.ui.bukkit.BukkitUiInventories;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class BlockInteractionAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public BlockInteractionAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Event.Result.DENY || !event.hasBlock()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() == Material.VAULT) {
            return;
        }
        boolean currentlyOpen = block.getBlockData() instanceof Openable openable && openable.isOpen();
        if (!BukkitRestrictionResolver.isBlockInteractionRelevant(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        Optional<RestrictionTarget> target = BukkitRestrictionTargets.fromBlock(block);
        if (target.isEmpty()) {
            return;
        }
        List<AbilityType> abilities = restrictionService.abilityList(
            BukkitRestrictionResolver.resolveBlockInteractionAbility(block.getType(), currentlyOpen).orElse(null)
        );
        if (restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.BLOCK_INTERACT,
            abilities,
            target.get(),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.OPENS_UI),
            event
        )) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolveInventoryOpenAbility(event.getInventory().getType());
        Optional<RestrictionTarget> target = BukkitRestrictionTargets.fromInventory(
            event.getInventory(),
            com.diskree.achievetodo.restriction.RestrictionTargetKind.INVENTORY,
            event.getInventory().getType().name().toLowerCase()
        );
        if (ability.isEmpty() && target.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.INVENTORY_OPEN,
            restrictionService.abilityList(ability.orElse(null)),
            target.orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.OPENS_UI, RestrictionFlag.SECONDARY_PATH),
            event
        );
    }
}
