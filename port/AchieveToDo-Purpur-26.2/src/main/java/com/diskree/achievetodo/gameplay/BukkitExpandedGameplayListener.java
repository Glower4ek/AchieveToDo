package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

/** Cancels native transactions before their effects; never creates replacement gameplay effects. */
public final class BukkitExpandedGameplayListener implements Listener {
    private final PlayerProgressionLookup progression;

    public BukkitExpandedGameplayListener(PlayerProgressionLookup progression) {
        this.progression = progression;
    }

    private boolean locked(Player p, AbilityType ability) {
        return !GameplayGate.allows(progression.getProgressionState(p.getUniqueId()), p.getUniqueId(),
            p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR, ability);
    }

    static AbilityType openingAbility(BlockData data) {
        // OPEN_* forbids opening only. Closing and redstone changes are unrestricted in Fabric.
        if (data instanceof Door door && !door.isOpen()) return AbilityType.OPEN_DOOR;
        if (data instanceof TrapDoor door && !door.isOpen()) return AbilityType.OPEN_TRAPDOOR;
        if (data instanceof Gate gate && !gate.isOpen()) return AbilityType.OPEN_FENCE_GATE;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null
            || e.useInteractedBlock() == Event.Result.DENY) return;
        AbilityType ability = openingAbility(e.getClickedBlock().getBlockData());
        if (ability != null && locked(e.getPlayer(), ability)) {
            // Keep the item-use decision intact: buckets/tools can run before useWithoutItem.
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    static AbilityType containerAbility(InventoryHolder holder) {
        // InventoryType.CHEST also describes virtual menus and vehicle storage; neither is ChestBlock.
        if (holder instanceof Chest || holder instanceof DoubleChest) return AbilityType.OPEN_CHEST;
        if (holder instanceof Barrel) return AbilityType.OPEN_BARREL;
        if (holder instanceof Furnace furnace && furnace.getType() == Material.FURNACE) return AbilityType.OPEN_FURNACE;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void open(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        AbilityType ability = containerAbility(e.getInventory().getHolder(false));
        if (ability == null) ability = GameplayMenuAbilities.opening(e.getInventory());
        if (ability != null && locked(p, ability)) e.setCancelled(true);
    }

    static AbilityType projectileAbility(Projectile projectile) {
        if (projectile instanceof Snowball) return AbilityType.THROW_SNOWBALL;
        if (projectile instanceof Egg) return AbilityType.THROW_EGG;
        if (projectile instanceof EnderPearl) return AbilityType.THROW_ENDER_PEARL;
        if (projectile instanceof WindCharge) return AbilityType.THROW_WIND_CHARGE;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void throwItem(PlayerLaunchProjectileEvent e) {
        // Paper fires this before spawn, stats, consumption, sound and cooldown in pinned 2620.
        AbilityType ability = projectileAbility(e.getProjectile());
        if (ability != null && locked(e.getPlayer(), ability)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void launch(ProjectileLaunchEvent e) {
        // Covers the generic player launch API too; dispenser/mob projectiles have no player actor.
        AbilityType ability = projectileAbility(e.getEntity());
        if (ability != null && e.getEntity().getShooter() instanceof Player p && locked(p, ability))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fish(PlayerFishEvent e) {
        // Original ordinal-1 hook guards casting, after the separate retrieval branch.
        if (e.getState() == PlayerFishEvent.State.FISHING && locked(e.getPlayer(), AbilityType.USE_FISHING_ROD))
            e.setCancelled(true);
    }
}
