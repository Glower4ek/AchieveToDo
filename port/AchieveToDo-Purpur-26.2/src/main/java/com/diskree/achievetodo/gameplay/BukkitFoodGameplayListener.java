package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.util.function.Function;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/** Food transactions only. Native Minecraft owns duration, effects, depletion and remainder placement. */
public final class BukkitFoodGameplayListener implements Listener {
    private final PlayerProgressionLookup progression;
    private final Function<ItemStack, AbilityType> foods;

    public BukkitFoodGameplayListener(PlayerProgressionLookup progression, Function<ItemStack, AbilityType> foods) {
        this.progression = progression;
        this.foods = foods;
    }

    private boolean denied(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        final AbilityType ability;
        try { ability = foods.apply(item); }
        catch (IllegalStateException unavailable) { return true; }
        return ability != null && !GameplayGate.allows(progression.getProgressionState(player.getUniqueId()),
            player.getUniqueId(), false, ability);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void begin(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Native startConsuming guards these foods without suppressing their separate planting path.
        try { if (NativeFoodAbilities.plantable(foods.apply(event.getItem()))) return; }
        catch (IllegalStateException unavailable) { event.setUseItemInHand(Event.Result.DENY); return; }
        // RIGHT_CLICK_AIR has a denied block decision by default, so ignoreCancelled=true would miss it.
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            && event.useItemInHand() != Event.Result.DENY && denied(event.getPlayer(), event.getItem()))
            event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void finish(PlayerItemConsumeEvent event) {
        // Resample on completion: count, team, source, world or config may change during the action.
        // Also check an earlier plugin's replacement consumable, without reviving its cancellation.
        if (denied(event.getPlayer(), event.getPlayer().getInventory().getItem(event.getHand()))
            || denied(event.getPlayer(), event.getItem())) event.setCancelled(true);
    }
}
