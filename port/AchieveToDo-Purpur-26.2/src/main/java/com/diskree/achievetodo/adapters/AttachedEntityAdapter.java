package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Set;

public final class AttachedEntityAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public AttachedEntityAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(event.getRightClicked()).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            restrictionService.abilityList(
                BukkitRestrictionResolver.resolveItemUseAbility(
                    event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                        ? event.getPlayer().getInventory().getItemInOffHand().getType()
                        : event.getPlayer().getInventory().getItemInMainHand().getType()
                ).orElse(null)
            ),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND ? Set.of(RestrictionFlag.OFFHAND_PATH) : Set.of(),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand) && !(event.getEntity() instanceof ItemFrame)) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            restrictionService.abilityList(),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand) && !(event.getEntity() instanceof ItemFrame)) {
            return;
        }
        Player player = resolveActor(event.getDamager());
        if (player == null) {
            return;
        }
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.ENTITY_ATTACK,
            restrictionService.abilityList(),
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            event.getDamager() instanceof Projectile ? RestrictionAutomationSource.PROJECTILE : RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    private Player resolveActor(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
