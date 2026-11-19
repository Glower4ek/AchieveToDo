package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionDecision;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.diskree.achievetodo.ui.bukkit.BukkitUiInventories;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EquipmentAdapter implements Listener {

    private static final int PLAYER_OFFHAND_RAW_SLOT = 45;

    private final JavaPlugin plugin;
    private final BukkitRestrictionService restrictionService;

    public EquipmentAdapter(JavaPlugin plugin, BukkitRestrictionService restrictionService) {
        this.plugin = plugin;
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) || !event.hasItem()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Optional<AbilityType> equipmentAbility = BukkitRestrictionResolver.resolveEquipmentAbility(item);
        if (equipmentAbility.isEmpty() || !canDirectEquip(event.getPlayer(), item.getType())) {
            return;
        }
        RestrictionTarget target = BukkitRestrictionTargets.fromEntity(event.getPlayer()).orElse(RestrictionTarget.none());
        if (restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.EQUIPMENT_CHANGE,
            restrictionService.abilityList(equipmentAbility.orElse(null)),
            target,
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(),
            event
        )) {
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        ItemStack newItem = event.getNewItemStack();
        Optional<AbilityType> equipmentAbility = BukkitRestrictionResolver.resolveEquipmentAbility(newItem);
        if (equipmentAbility.isEmpty()) {
            return;
        }
        EquipmentSlot slot = newItem.getType().getEquipmentSlot();
        if (slot == null || slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack equipped = player.getInventory().getItem(slot);
        if (!matchesEquippedItem(equipped, newItem)) {
            return;
        }
        RestrictionDecision decision = restrictionService.check(
            player,
            RestrictionActionKind.EQUIPMENT_CHANGE,
            restrictionService.abilityList(equipmentAbility.orElse(null)),
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH)
        );
        if (decision.isAllowed()) {
            return;
        }
        event.setShouldTriggerAdvancements(false);
        plugin.getServer().getScheduler().runTask(plugin, () -> rollbackDirectEquip(player, slot, event.getOldItemStack(), newItem, decision));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Set<AbilityType> abilities = collectInventoryClickAbilities(player, event);
        if (abilities.isEmpty()) {
            return;
        }
        if (restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.INVENTORY_MUTATION,
            abilities,
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            event.getRawSlot() == PLAYER_OFFHAND_RAW_SLOT ? Set.of(RestrictionFlag.OFFHAND_PATH, RestrictionFlag.SECONDARY_PATH) : Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        )) {
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (BukkitUiInventories.isOwnedUi(event)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean touchesArmorOrOffhand = event.getRawSlots().stream().anyMatch(slot -> slot == PLAYER_OFFHAND_RAW_SLOT || slot < 9);
        if (!touchesArmorOrOffhand) {
            return;
        }
        Set<AbilityType> abilities = new LinkedHashSet<>();
        collectItemAbilities(event.getCursor(), true, abilities);
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            if (entry.getKey() == PLAYER_OFFHAND_RAW_SLOT || entry.getKey() < 9) {
                collectItemAbilities(entry.getValue(), true, abilities);
            }
        }
        if (abilities.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.INVENTORY_MUTATION,
            abilities,
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Set<AbilityType> abilities = new LinkedHashSet<>();
        collectItemAbilities(event.getOffHandItem(), true, abilities);
        collectItemAbilities(event.getMainHandItem(), false, abilities);
        if (abilities.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.EQUIPMENT_CHANGE,
            abilities,
            BukkitRestrictionTargets.fromEntity(event.getPlayer()).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.OFF_HAND,
            RestrictionAutomationSource.OFFHAND,
            Set.of(RestrictionFlag.OFFHAND_PATH, RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDispenseArmor(BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player player)) {
            return;
        }
        Set<AbilityType> abilities = new LinkedHashSet<>();
        collectItemAbilities(event.getItem(), true, abilities);
        if (abilities.isEmpty()) {
            return;
        }
        restrictionService.denyIfRestricted(
            player,
            RestrictionActionKind.EQUIPMENT_CHANGE,
            abilities,
            BukkitRestrictionTargets.fromEntity(player).orElse(RestrictionTarget.none()),
            com.diskree.achievetodo.restriction.RestrictionHand.NONE,
            RestrictionAutomationSource.DISPENSER,
            Set.of(RestrictionFlag.AUTOMATION_PATH, RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        if (event.getNewItem().getType().isAir()) {
            return;
        }
        BukkitRestrictionResolver.resolveEquipmentAbility(event.getNewItem()).ifPresent(ability ->
            restrictionService.check(
                event.getPlayer(),
                RestrictionActionKind.EQUIPMENT_CHANGE,
                restrictionService.abilityList(ability),
                BukkitRestrictionTargets.fromEntity(event.getPlayer()).orElse(RestrictionTarget.none()),
                com.diskree.achievetodo.restriction.RestrictionHand.NONE,
                RestrictionAutomationSource.UNKNOWN,
                Set.of(RestrictionFlag.SECONDARY_PATH)
            )
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        Set<AbilityType> abilities = new LinkedHashSet<>();
        collectItemAbilities(event.getPlayerItem(), true, abilities);
        collectItemAbilities(event.getArmorStandItem(), true, abilities);
        restrictionService.denyIfRestricted(
            event.getPlayer(),
            RestrictionActionKind.EQUIPMENT_CHANGE,
            abilities,
            BukkitRestrictionTargets.fromEntity(armorStand).orElse(RestrictionTarget.none()),
            BukkitRestrictionResolver.resolveHand(event.getHand()),
            RestrictionAutomationSource.PLAYER,
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    private Set<AbilityType> collectInventoryClickAbilities(Player player, InventoryClickEvent event) {
        Set<AbilityType> abilities = new LinkedHashSet<>();
        boolean armorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean offhandSlot = event.getRawSlot() == PLAYER_OFFHAND_RAW_SLOT;
        if (armorSlot || offhandSlot) {
            collectItemAbilities(event.getCursor(), true, abilities);
            if (event.getClick().isKeyboardClick() && event.getHotbarButton() >= 0 && event.getHotbarButton() < 9) {
                collectItemAbilities(player.getInventory().getItem(event.getHotbarButton()), offhandSlot, abilities);
            }
            if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR || event.getAction() == InventoryAction.PICKUP_ALL) {
                collectItemAbilities(event.getCurrentItem(), offhandSlot, abilities);
            }
        }
        if (event.isShiftClick() && canAutoEquip(player, event.getCurrentItem())) {
            collectItemAbilities(event.getCurrentItem(), false, abilities);
        }
        return abilities;
    }

    private void collectItemAbilities(ItemStack item, boolean includeUseAbility, Set<AbilityType> abilities) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        BukkitRestrictionResolver.resolveEquipmentAbility(item).ifPresent(abilities::add);
        if (includeUseAbility) {
            BukkitRestrictionResolver.resolveItemUseAbility(item.getType()).ifPresent(abilities::add);
        }
    }

    private boolean canDirectEquip(Player player, Material material) {
        if (material == null || material.isAir()) {
            return false;
        }
        EquipmentSlot slot = material.getEquipmentSlot();
        if (slot == null || slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) {
            return false;
        }
        ItemStack equipped = player.getInventory().getItem(slot);
        return equipped == null || equipped.getType().isAir();
    }

    private boolean canAutoEquip(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Optional<AbilityType> equipmentAbility = BukkitRestrictionResolver.resolveEquipmentAbility(item);
        if (equipmentAbility.isEmpty()) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        EquipmentSlot slot = item.getType().getEquipmentSlot();
        if (slot == null || slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) {
            return false;
        }
        ItemStack equipped = inventory.getItem(slot);
        return equipped == null || equipped.getType().isAir();
    }

    private boolean matchesEquippedItem(ItemStack equipped, ItemStack expected) {
        if (equipped == null || expected == null) {
            return false;
        }
        return equipped.getType() == expected.getType() && equipped.getAmount() == expected.getAmount();
    }

    private void rollbackDirectEquip(
        Player player,
        EquipmentSlot slot,
        ItemStack previousArmorItem,
        ItemStack equippedItem,
        RestrictionDecision decision
    ) {
        ItemStack currentEquipped = player.getInventory().getItem(slot);
        if (!matchesEquippedItem(currentEquipped, equippedItem)) {
            return;
        }
        player.getInventory().setItem(slot, sanitize(previousArmorItem));
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(equippedItem.clone());
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.updateInventory();
        restrictionService.handleDeniedDecision(player, decision);
    }

    private ItemStack sanitize(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }
}
