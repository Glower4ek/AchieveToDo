package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.Set;

public final class ProjectileAdapter implements Listener {

    private static final String DEFAULT_TARGET_KEY = "projectile";

    private final BukkitRestrictionService restrictionService;
    private final NamespacedKey abilityKey;

    public ProjectileAdapter(JavaPlugin plugin, BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
        this.abilityKey = new NamespacedKey(plugin, "restriction_projectile_ability");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerLaunchProjectile(PlayerLaunchProjectileEvent event) {
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolveItemUseAbility(event.getItemStack().getType());
        if (restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.PROJECTILE_LAUNCH,
            restrictionService.abilityList(ability.orElse(null)),
            BukkitRestrictionTargets.fromEntity(event.getPlayer()).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.MAIN_HAND,
            RestrictionAutomationSource.PROJECTILE,
            Set.of(RestrictionFlag.PROJECTILE_OWNER_IS_ACTOR),
            event
        )) {
            return;
        }
        ability.ifPresent(value -> event.getProjectile().getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, value.getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null) {
            return;
        }
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolveItemUseAbility(bow.getType());
        if (ability.isEmpty()) {
            return;
        }
        if (restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.PROJECTILE_LAUNCH,
            restrictionService.abilityList(ability.orElse(null)),
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.MAIN_HAND,
            RestrictionAutomationSource.PROJECTILE,
            Set.of(RestrictionFlag.PROJECTILE_OWNER_IS_ACTOR),
            event
        )) {
            return;
        }
        projectile.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, ability.get().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        Optional<AbilityType> ability = resolveStoredAbility(event.getEntity());
        if (event.getHitEntity() != null) {
            restrictionService.denyIfRestricted(
                player,
                RestrictionActionKind.PROJECTILE_HIT_ENTITY,
                restrictionService.abilityList(ability.orElse(null)),
                BukkitRestrictionTargets.fromEntity(event.getHitEntity()).orElse(RestrictionTarget.none()),
                com.diskree.achievetodo.restriction.RestrictionHand.NONE,
                RestrictionAutomationSource.PROJECTILE,
                Set.of(RestrictionFlag.PROJECTILE_OWNER_IS_ACTOR),
                event
            );
            return;
        }
        if (event.getHitBlock() != null) {
            restrictionService.denyIfRestricted(
                player,
                RestrictionActionKind.PROJECTILE_HIT_BLOCK,
                restrictionService.abilityList(ability.orElse(null)),
                BukkitRestrictionTargets.fromBlock(event.getHitBlock()).orElse(RestrictionTarget.none()),
                com.diskree.achievetodo.restriction.RestrictionHand.NONE,
                RestrictionAutomationSource.PROJECTILE,
                Set.of(RestrictionFlag.PROJECTILE_OWNER_IS_ACTOR, RestrictionFlag.SECONDARY_PATH),
                event
            );
        }
    }

    private Optional<AbilityType> resolveStoredAbility(Projectile projectile) {
        PersistentDataContainer dataContainer = projectile.getPersistentDataContainer();
        String abilityName = dataContainer.get(abilityKey, PersistentDataType.STRING);
        if (abilityName != null) {
            return AbilityType.findByName(abilityName);
        }
        return Optional.empty();
    }
}
