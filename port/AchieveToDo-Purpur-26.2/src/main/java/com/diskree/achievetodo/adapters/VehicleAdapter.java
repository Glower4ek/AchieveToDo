package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Optional;
import java.util.Set;

public final class VehicleAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public VehicleAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }
        denyVehicleEnter(player, event.getVehicle(), Set.of(RestrictionFlag.VEHICLE_DRIVER_IS_ACTOR), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getMount() instanceof org.bukkit.entity.Vehicle) {
            return;
        }
        denyVehicleEnter(player, event.getMount(), Set.of(RestrictionFlag.VEHICLE_DRIVER_IS_ACTOR, RestrictionFlag.SECONDARY_PATH), event);
    }

    private void denyVehicleEnter(Player player, Entity mount, Set<RestrictionFlag> flags, org.bukkit.event.Cancellable event) {
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolveVehicleEnterAbility(mount);
        RestrictionTarget target = BukkitRestrictionTargets.fromBoundingBox(
            mount.getWorld(),
            mount.getBoundingBox(),
            com.diskree.achievetodo.restriction.RestrictionTargetKind.VEHICLE,
            mount.getType().getKey().getKey()
        ).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.VEHICLE_ENTER,
            restrictionService.abilityList(ability.orElse(null)),
            target,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.VEHICLE,
            flags,
            event
        );
    }
}
