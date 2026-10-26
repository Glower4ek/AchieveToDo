package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityResolvers;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.EquipmentDescriptor;
import com.diskree.achievetodo.restriction.RestrictionActorMode;
import com.diskree.achievetodo.restriction.RestrictionHand;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

public final class BukkitRestrictionResolver {

    private BukkitRestrictionResolver() {
    }

    public static Optional<AbilityType> resolveBlockInteractionAbility(Material material, boolean currentlyOpen) {
        if (material == null || material.isAir()) {
            return Optional.empty();
        }
        return resolveBlockInteractionAbility(materialKey(material), currentlyOpen);
    }

    public static boolean isBlockInteractionRelevant(Material material) {
        return material != null && AbilityResolvers.findBlockAbility(materialKey(material)).isPresent();
    }

    static Optional<AbilityType> resolveBlockInteractionAbility(String blockKey, boolean currentlyOpen) {
        if (blockKey == null || blockKey.isBlank()) {
            return Optional.empty();
        }
        if (blockKey.endsWith("_door")) {
            return currentlyOpen ? Optional.empty() : Optional.of(AbilityType.OPEN_DOOR);
        }
        if (blockKey.endsWith("_trapdoor")) {
            return currentlyOpen ? Optional.empty() : Optional.of(AbilityType.OPEN_TRAPDOOR);
        }
        if (blockKey.endsWith("_fence_gate")) {
            return currentlyOpen ? Optional.empty() : Optional.of(AbilityType.OPEN_FENCE_GATE);
        }
        if (currentlyOpen && isOpenOnlyRestriction(blockKey)) {
            return Optional.empty();
        }
        return AbilityResolvers.findBlockAbility(blockKey);
    }

    public static Optional<AbilityType> resolveInventoryOpenAbility(InventoryType inventoryType) {
        if (inventoryType == null) {
            return Optional.empty();
        }
        if (inventoryType == InventoryType.CHEST) {
            return Optional.of(AbilityType.OPEN_CHEST);
        }
        if (inventoryType == InventoryType.BARREL) {
            return Optional.of(AbilityType.OPEN_BARREL);
        }
        if (inventoryType == InventoryType.SHULKER_BOX) {
            return Optional.of(AbilityType.OPEN_SHULKER_BOX);
        }
        if (inventoryType == InventoryType.ENDER_CHEST) {
            return Optional.of(AbilityType.OPEN_ENDER_CHEST);
        }
        if (inventoryType == InventoryType.WORKBENCH || inventoryType == InventoryType.CRAFTING) {
            return Optional.of(AbilityType.OPEN_CRAFTING_TABLE);
        }
        if (inventoryType == InventoryType.FURNACE) {
            return Optional.of(AbilityType.OPEN_FURNACE);
        }
        if (inventoryType == InventoryType.BLAST_FURNACE) {
            return Optional.of(AbilityType.OPEN_BLAST_FURNACE);
        }
        if (inventoryType == InventoryType.SMOKER) {
            return Optional.of(AbilityType.OPEN_SMOKER);
        }
        if (inventoryType == InventoryType.ANVIL) {
            return Optional.of(AbilityType.OPEN_ANVIL);
        }
        if (inventoryType == InventoryType.BEACON) {
            return Optional.of(AbilityType.OPEN_BEACON);
        }
        if (inventoryType == InventoryType.BREWING) {
            return Optional.of(AbilityType.OPEN_BREWING_STAND);
        }
        if (inventoryType == InventoryType.ENCHANTING) {
            return Optional.of(AbilityType.OPEN_ENCHANTING_TABLE);
        }
        if (inventoryType == InventoryType.GRINDSTONE) {
            return Optional.of(AbilityType.OPEN_GRINDSTONE);
        }
        if (inventoryType == InventoryType.LOOM) {
            return Optional.of(AbilityType.OPEN_LOOM);
        }
        if (inventoryType == InventoryType.CARTOGRAPHY) {
            return Optional.of(AbilityType.OPEN_CARTOGRAPHY_TABLE);
        }
        if (inventoryType == InventoryType.STONECUTTER) {
            return Optional.of(AbilityType.OPEN_STONECUTTER);
        }
        if (inventoryType == InventoryType.SMITHING || inventoryType == InventoryType.SMITHING_NEW) {
            return Optional.of(AbilityType.OPEN_SMITHING_TABLE);
        }
        return Optional.empty();
    }

    public static Optional<AbilityType> resolveItemUseAbility(Material material) {
        if (material == null || material.isAir()) {
            return Optional.empty();
        }
        return resolveItemUseAbility(materialKey(material));
    }

    public static Optional<AbilityType> resolveConsumableAbility(Material material) {
        if (material == null || material.isAir()) {
            return Optional.empty();
        }
        return resolveConsumableAbility(materialKey(material));
    }

    static Optional<AbilityType> resolveItemUseAbility(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        return AbilityResolvers.findItemAbility(itemKey);
    }

    static Optional<AbilityType> resolveConsumableAbility(String itemKey) {
        Optional<AbilityType> itemAbility = resolveItemUseAbility(itemKey);
        if (itemAbility.isPresent()) {
            return itemAbility;
        }
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        return AbilityResolvers.findFoodAbility(itemKey);
    }

    public static Optional<AbilityType> resolveToolMaterialAbility(Material material) {
        if (material == null || material.isAir()) {
            return Optional.empty();
        }
        return resolveToolMaterialAbility(materialKey(material));
    }

    static Optional<AbilityType> resolveToolMaterialAbility(String itemKey) {
        if (itemKey == null || itemKey.isBlank() || !isToolOrWeaponKey(itemKey)) {
            return Optional.empty();
        }
        return AbilityResolvers.findToolMaterialAbility(resolveMaterialFamily(itemKey));
    }

    public static Optional<AbilityType> resolveEquipmentAbility(ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        Material material = stack.getType();
        if (material.isAir()) {
            return Optional.empty();
        }
        return resolveEquipmentAbility(materialKey(material));
    }

    static Optional<AbilityType> resolveEquipmentAbility(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        Optional<AbilityType> exactItemAbility = AbilityResolvers.findEquipmentAbility(EquipmentDescriptor.ofItem(itemKey));
        if (exactItemAbility.isPresent()) {
            return exactItemAbility;
        }
        String family = switch (itemKey) {
            case "elytra" -> "elytra";
            case "turtle_helmet" -> "turtle_helmet";
            default -> resolveArmorMaterialFamily(itemKey);
        };
        if (family == null) {
            return Optional.empty();
        }
        return AbilityResolvers.findEquipmentAbility(EquipmentDescriptor.ofMaterial(family));
    }

    public static Optional<AbilityType> resolveTradeAbility(AbstractVillager villager) {
        if (villager == null) {
            return Optional.empty();
        }
        if (villager instanceof WanderingTrader) {
            return AbilityResolvers.findTradeAbility("wandering_trader");
        }
        if (villager instanceof Villager concreteVillager) {
            return resolveTradeAbility(concreteVillager.getProfession());
        }
        return Optional.empty();
    }

    public static Optional<AbilityType> resolveTradeAbility(Villager.Profession profession) {
        if (profession == null) {
            return Optional.empty();
        }
        return resolveTradeAbility(profession.getKey().getKey());
    }

    static Optional<AbilityType> resolveTradeAbility(String professionKey) {
        if (professionKey == null || professionKey.isBlank()) {
            return Optional.empty();
        }
        return AbilityResolvers.findTradeAbility(professionKey.toLowerCase(Locale.ROOT));
    }

    public static Optional<AbilityType> resolveVehicleEnterAbility(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        if (entity instanceof Boat) {
            return Optional.of(AbilityType.GET_INTO_BOAT);
        }
        if (entity instanceof Minecart) {
            return Optional.of(AbilityType.GET_INTO_MINECART);
        }
        if (entity instanceof AbstractHorse) {
            return Optional.empty();
        }
        return resolveVehicleEnterAbility(entity.getType().getKey().getKey());
    }

    static Optional<AbilityType> resolveVehicleEnterAbility(String entityKey) {
        if (entityKey == null || entityKey.isBlank()) {
            return Optional.empty();
        }
        if (entityKey.endsWith("_boat") || entityKey.endsWith("_raft") || entityKey.endsWith("_chest_boat")) {
            return Optional.of(AbilityType.GET_INTO_BOAT);
        }
        if (entityKey.contains("minecart")) {
            return Optional.of(AbilityType.GET_INTO_MINECART);
        }
        return Optional.empty();
    }

    public static Optional<AbilityType> resolvePortalAbility(PlayerTeleportEvent.TeleportCause cause) {
        if (cause == null) {
            return Optional.empty();
        }
        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return Optional.of(AbilityType.ENTER_NETHER);
        }
        if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return Optional.of(AbilityType.ENTER_END);
        }
        if (cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            return Optional.of(AbilityType.TELEPORT_OUTER_ISLANDS);
        }
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return Optional.of(AbilityType.THROW_ENDER_PEARL);
        }
        return Optional.empty();
    }

    public static RestrictionActorMode resolveActorMode(GameMode gameMode) {
        if (gameMode == null) {
            return RestrictionActorMode.UNKNOWN;
        }
        if (gameMode == GameMode.SURVIVAL) {
            return RestrictionActorMode.SURVIVAL;
        }
        if (gameMode == GameMode.CREATIVE) {
            return RestrictionActorMode.CREATIVE;
        }
        if (gameMode == GameMode.SPECTATOR) {
            return RestrictionActorMode.SPECTATOR;
        }
        if (gameMode == GameMode.ADVENTURE) {
            return RestrictionActorMode.ADVENTURE;
        }
        return RestrictionActorMode.UNKNOWN;
    }

    public static RestrictionHand resolveHand(EquipmentSlot hand) {
        if (hand == null) {
            return RestrictionHand.NONE;
        }
        if (hand == EquipmentSlot.HAND) {
            return RestrictionHand.MAIN_HAND;
        }
        if (hand == EquipmentSlot.OFF_HAND) {
            return RestrictionHand.OFF_HAND;
        }
        return RestrictionHand.NONE;
    }

    public static String materialKey(Material material) {
        return material.getKey().getKey();
    }

    private static boolean isOpenOnlyRestriction(String blockKey) {
        return blockKey.endsWith("_door") || blockKey.endsWith("_trapdoor") || blockKey.endsWith("_fence_gate");
    }

    private static boolean isToolOrWeaponKey(String key) {
        return key.endsWith("_pickaxe") ||
            key.endsWith("_axe") ||
            key.endsWith("_shovel") ||
            key.endsWith("_hoe") ||
            key.endsWith("_sword");
    }

    private static String resolveMaterialFamily(String key) {
        if (key.startsWith("wooden_")) {
            return "wood";
        }
        if (key.startsWith("stone_")) {
            return "stone";
        }
        if (key.startsWith("golden_")) {
            return "gold";
        }
        if (key.startsWith("iron_")) {
            return "iron";
        }
        if (key.startsWith("diamond_")) {
            return "diamond";
        }
        if (key.startsWith("netherite_")) {
            return "netherite";
        }
        return null;
    }

    private static String resolveArmorMaterialFamily(String key) {
        if (key.startsWith("leather_")) {
            return "leather";
        }
        if (key.startsWith("chainmail_")) {
            return "chain";
        }
        if (key.startsWith("golden_")) {
            return "gold";
        }
        if (key.startsWith("iron_")) {
            return "iron";
        }
        if (key.startsWith("diamond_")) {
            return "diamond";
        }
        if (key.startsWith("netherite_")) {
            return "netherite";
        }
        return null;
    }
}
