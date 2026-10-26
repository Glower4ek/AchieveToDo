package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Set;

public final class VaultAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public VaultAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasBlock() || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.VAULT) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getClickedBlock()).orElse(RestrictionTarget.none());
        if (restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.VAULT_USE,
            restrictionService.abilityList(AbilityType.UNLOCK_VAULT),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.OPENS_UI),
            event
        )) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVaultChangeState(VaultChangeStateEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getBlock()).orElse(RestrictionTarget.none());
        restrictionService.denySilentlyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.VAULT_USE,
            restrictionService.abilityList(AbilityType.UNLOCK_VAULT),
            target,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH, RestrictionFlag.SHARED_STATE_PATH),
            event
        );
    }
}
