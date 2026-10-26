package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionHand;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class BlockBreakAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public BlockBreakAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        Optional<RestrictionTarget> target = BukkitRestrictionTargets.fromBlock(event.getBlock());
        if (target.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.BLOCK_DAMAGE,
            collectBreakAbilities(event.getBlock(), event.getItemInHand()),
            target.get(),
            RestrictionHand.MAIN_HAND,
            RestrictionAutomationSource.PLAYER,
            Set.of(),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Optional<RestrictionTarget> target = BukkitRestrictionTargets.fromBlock(event.getBlock());
        if (target.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.BLOCK_BREAK,
            collectBreakAbilities(event.getBlock(), event.getPlayer().getInventory().getItemInMainHand()),
            target.get(),
            RestrictionHand.MAIN_HAND,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    private Set<AbilityType> collectBreakAbilities(Block block, ItemStack tool) {
        Set<AbilityType> abilities = new LinkedHashSet<>();
        if (block.getY() < 0) {
            abilities.add(AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y);
        }
        abilities.add(AbilityType.BREAK_BLOCKS);
        if (tool != null && tool.getType() == Material.SHEARS) {
            abilities.add(AbilityType.USE_SHEARS);
        }
        BukkitRestrictionResolver.resolveToolMaterialAbility(tool == null ? null : tool.getType()).ifPresent(abilities::add);
        return abilities;
    }
}
