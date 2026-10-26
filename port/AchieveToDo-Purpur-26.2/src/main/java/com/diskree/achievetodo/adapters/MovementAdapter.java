package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import java.util.Set;

public final class MovementAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public MovementAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        denyMovement(player, AbilityType.EQUIP_ELYTRA, Set.of(RestrictionFlag.SECONDARY_PATH), event);
    }

    private void denyMovement(Player player, AbilityType ability, Set<RestrictionFlag> flags, org.bukkit.event.Cancellable event) {
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.MOVEMENT,
            restrictionService.abilityList(ability),
            target,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            flags,
            event
        );
    }
}
