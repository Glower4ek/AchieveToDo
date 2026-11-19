package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.view.MerchantView;

import java.util.Optional;
import java.util.Set;

public final class TradeAdapter implements Listener {

    private final BukkitRestrictionService restrictionService;

    public TradeAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractVillager villager)) {
            return;
        }
        denyMerchantAction(
            event.getPlayer(),
            villager,
            RestrictionActionKind.TRADE_OPEN,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            Set.of(RestrictionFlag.OPENS_UI),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || event.getInventory().getType() != InventoryType.MERCHANT) {
            return;
        }
        if (!(event.getView() instanceof MerchantView merchantView) || !(merchantView.getMerchant() instanceof AbstractVillager villager)) {
            return;
        }
        denyMerchantAction(
            player,
            villager,
            RestrictionActionKind.TRADE_OPEN,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            Set.of(RestrictionFlag.OPENS_UI, RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTradeSelect(TradeSelectEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getMerchant() instanceof AbstractVillager villager)) {
            return;
        }
        denyMerchantAction(
            player,
            villager,
            RestrictionActionKind.TRADE_SELECT,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerTrade(PlayerTradeEvent event) {
        if (!(event.getMerchant() instanceof AbstractVillager villager)) {
            return;
        }
        denyMerchantAction(
            event.getPlayer(),
            villager,
            RestrictionActionKind.TRADE_EXECUTE,
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    private void denyMerchantAction(
        Player player,
        AbstractVillager villager,
        RestrictionActionKind actionKind,
        com.diskree.achievetodo.restriction.RestrictionHand hand,
        Set<RestrictionFlag> flags,
        org.bukkit.event.Cancellable event
    ) {
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolveTradeAbility(villager);
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(villager).orElse(RestrictionTarget.none());
        restrictionService.denyIfRestricted(
            player,
            actionKind,
            restrictionService.abilityList(ability.orElse(null)),
            target,
            hand,
            RestrictionAutomationSource.PLAYER,
            flags,
            event
        );
    }
}
