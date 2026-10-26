package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import java.util.Set;

public final class SleepAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public SleepAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getBed()).orElse(RestrictionTarget.none());
        if (restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.SLEEP,
            restrictionService.abilityList(AbilityType.SLEEP),
            target,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(),
            event
        )) {
            event.setUseBed(org.bukkit.event.Event.Result.DENY);
        }
    }
}
