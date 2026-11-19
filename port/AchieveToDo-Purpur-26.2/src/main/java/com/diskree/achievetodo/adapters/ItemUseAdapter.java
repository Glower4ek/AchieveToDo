package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionHand;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class ItemUseAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public ItemUseAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) ||
            event.useItemInHand() == Event.Result.DENY ||
            !event.hasItem()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || isBucketMaterial(item.getType())) {
            return;
        }

        Set<AbilityType> abilities = new LinkedHashSet<>();
        BukkitRestrictionResolver.resolveItemUseAbility(item.getType()).ifPresent(abilities::add);
        if (event.hasBlock()) {
            Material type = item.getType();
            if (type == Material.SHEARS) {
                abilities.add(AbilityType.USE_SHEARS);
            } else {
                BukkitRestrictionResolver.resolveToolMaterialAbility(type).ifPresent(abilities::add);
            }
        }
        if (abilities.isEmpty()) {
            return;
        }

        RestrictionTarget target = event.hasBlock()
            ? BukkitRestrictionTargets.fromBlock(event.getClickedBlock()).orElse(RestrictionTarget.none())
            : RestrictionTarget.none();
        if (restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ITEM_USE,
            abilities,
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND ? Set.of(RestrictionFlag.OFFHAND_PATH) : Set.of(),
            event
        )) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        BukkitRestrictionResolver.resolveConsumableAbility(event.getItem().getType()).ifPresent(abilityType ->
            restrictionService.denyIfRestricted(
                event.getPlayer(),
                RestrictionActionKind.ITEM_CONSUME,
                restrictionService.abilityList(abilityType),
                RestrictionTarget.none(),
                RestrictionHand.NONE,
                RestrictionAutomationSource.PLAYER,
                Set.of(),
                event
            )
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        RestrictionTarget target = BukkitRestrictionTargets.fromBlock(event.getBlockPlaced()).orElse(RestrictionTarget.none());
        AbilityType placeAbility = BukkitRestrictionResolver.resolveItemUseAbility(event.getItemInHand().getType()).orElse(null);
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.ITEM_USE,
            restrictionService.abilityList(placeAbility),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    private static boolean isBucketMaterial(Material material) {
        return material == Material.BUCKET ||
            material == Material.WATER_BUCKET ||
            material == Material.LAVA_BUCKET ||
            material == Material.POWDER_SNOW_BUCKET ||
            material == Material.MILK_BUCKET ||
            material.name().endsWith("_BUCKET");
    }
}
