package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;

public final class EntityInteractionAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public EntityInteractionAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity targetEntity = event.getRightClicked();
        if (targetEntity instanceof AbstractVillager || targetEntity instanceof ArmorStand || targetEntity instanceof ItemFrame) {
            return;
        }
        Set<AbilityType> abilities = new LinkedHashSet<>();
        ItemStack heldItem = event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
            ? event.getPlayer().getInventory().getItemInOffHand()
            : event.getPlayer().getInventory().getItemInMainHand();
        BukkitRestrictionResolver.resolveItemUseAbility(heldItem == null ? null : heldItem.getType()).ifPresent(abilities::add);
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            abilities,
            BukkitRestrictionTargets.fromEntity(targetEntity).orElse(RestrictionTarget.none()),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND ? Set.of(RestrictionFlag.OFFHAND_PATH) : Set.of(),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            restrictionService.abilityList(AbilityType.USE_SHEARS),
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            restrictionService.abilityList(),
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }
        Set<AbilityType> abilities = new LinkedHashSet<>();
        BukkitRestrictionResolver.resolveConsumableAbility(event.getBredWith() == null ? null : event.getBredWith().getType()).ifPresent(abilities::add);
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.ENTITY_INTERACT,
            abilities,
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ENTITY_INTERACT,
            restrictionService.abilityList(),
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Player player = resolveActor(event.getRemover());
        if (player == null) {
            return;
        }
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.ENTITY_ATTACK,
            restrictionService.abilityList(),
            BukkitRestrictionTargets.fromEntity(event.getEntity()).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            event.getRemover() instanceof Projectile ? RestrictionAutomationSource.PROJECTILE : RestrictionAutomationSource.PLAYER,
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
