package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Optional;
import java.util.Set;

public final class FluidAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public FluidAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        AbilityType abilityType = event.getBlock().getType() == Material.WATER ? AbilityType.USE_WATER_BUCKET : null;
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getBlock()).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.FLUID_INTERACT,
            restrictionService.abilityList(abilityType),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        AbilityType abilityType = event.getBucket() == Material.WATER_BUCKET ? AbilityType.USE_WATER_BUCKET : null;
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getBlock()).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.FLUID_INTERACT,
            restrictionService.abilityList(abilityType),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH, RestrictionFlag.WATERLOGGING_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.FLUID_INTERACT,
            restrictionService.abilityList(event.getEntityBucket().getType() == Material.WATER_BUCKET ? AbilityType.USE_WATER_BUCKET : null),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }
}
