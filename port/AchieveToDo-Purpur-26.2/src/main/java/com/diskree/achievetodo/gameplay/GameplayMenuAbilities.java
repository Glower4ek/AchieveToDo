package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

/** Exact native menu sources, not generic inventory titles or player unlock caches. */
final class GameplayMenuAbilities {
    record BlockMenu(Material material, AbilityType ability) {}

    static final Map<InventoryType, BlockMenu> BLOCKS = Map.ofEntries(
        Map.entry(InventoryType.WORKBENCH, new BlockMenu(Material.CRAFTING_TABLE, AbilityType.OPEN_CRAFTING_TABLE)),
        Map.entry(InventoryType.STONECUTTER, new BlockMenu(Material.STONECUTTER, AbilityType.OPEN_STONECUTTER)),
        Map.entry(InventoryType.GRINDSTONE, new BlockMenu(Material.GRINDSTONE, AbilityType.OPEN_GRINDSTONE)),
        Map.entry(InventoryType.ANVIL, new BlockMenu(Material.ANVIL, AbilityType.OPEN_ANVIL)),
        Map.entry(InventoryType.SMOKER, new BlockMenu(Material.SMOKER, AbilityType.OPEN_SMOKER)),
        Map.entry(InventoryType.BLAST_FURNACE, new BlockMenu(Material.BLAST_FURNACE, AbilityType.OPEN_BLAST_FURNACE)),
        Map.entry(InventoryType.BEACON, new BlockMenu(Material.BEACON, AbilityType.OPEN_BEACON)),
        Map.entry(InventoryType.CARTOGRAPHY, new BlockMenu(Material.CARTOGRAPHY_TABLE, AbilityType.OPEN_CARTOGRAPHY_TABLE)),
        Map.entry(InventoryType.BREWING, new BlockMenu(Material.BREWING_STAND, AbilityType.OPEN_BREWING_STAND)),
        Map.entry(InventoryType.SMITHING, new BlockMenu(Material.SMITHING_TABLE, AbilityType.OPEN_SMITHING_TABLE)),
        Map.entry(InventoryType.LOOM, new BlockMenu(Material.LOOM, AbilityType.OPEN_LOOM)),
        Map.entry(InventoryType.ENCHANTING, new BlockMenu(Material.ENCHANTING_TABLE, AbilityType.OPEN_ENCHANTING_TABLE))
    );

    static final Map<Villager.Profession, AbilityType> PROFESSIONS = Map.ofEntries(
        Map.entry(Villager.Profession.ARMORER, AbilityType.TRADE_WITH_ARMORER),
        Map.entry(Villager.Profession.BUTCHER, AbilityType.TRADE_WITH_BUTCHER),
        Map.entry(Villager.Profession.CARTOGRAPHER, AbilityType.TRADE_WITH_CARTOGRAPHER),
        Map.entry(Villager.Profession.CLERIC, AbilityType.TRADE_WITH_CLERIC),
        Map.entry(Villager.Profession.FARMER, AbilityType.TRADE_WITH_FARMER),
        Map.entry(Villager.Profession.FISHERMAN, AbilityType.TRADE_WITH_FISHERMAN),
        Map.entry(Villager.Profession.FLETCHER, AbilityType.TRADE_WITH_FLETCHER),
        Map.entry(Villager.Profession.LEATHERWORKER, AbilityType.TRADE_WITH_LEATHERWORKER),
        Map.entry(Villager.Profession.LIBRARIAN, AbilityType.TRADE_WITH_LIBRARIAN),
        Map.entry(Villager.Profession.MASON, AbilityType.TRADE_WITH_MASON),
        Map.entry(Villager.Profession.SHEPHERD, AbilityType.TRADE_WITH_SHEPHERD),
        Map.entry(Villager.Profession.TOOLSMITH, AbilityType.TRADE_WITH_TOOLSMITH),
        Map.entry(Villager.Profession.WEAPONSMITH, AbilityType.TRADE_WITH_WEAPONSMITH)
    );

    static Set<AbilityType> abilities() {
        var result = java.util.EnumSet.of(AbilityType.TRADE_WITH_WANDERING_TRADER);
        BLOCKS.values().forEach(menu -> result.add(menu.ability()));
        result.addAll(PROFESSIONS.values());
        return Set.copyOf(result);
    }

    static AbilityType merchant(Merchant merchant) {
        if (merchant instanceof Villager villager) return PROFESSIONS.get(villager.getProfession());
        if (merchant instanceof WanderingTrader) return AbilityType.TRADE_WITH_WANDERING_TRADER;
        return null; // Plugin-created virtual merchants are not villager interactions.
    }

    static AbilityType opening(Inventory inventory) {
        if (inventory instanceof MerchantInventory merchant) return merchant(merchant.getMerchant());
        var menu = BLOCKS.get(inventory.getType());
        if (menu == null) return null;
        var location = inventory.getLocation();
        if (location == null || location.getWorld() == null) return null;
        var material = location.getBlock().getType();
        if (material == menu.material() || menu.ability() == AbilityType.OPEN_ANVIL
                && (material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL)) return menu.ability();
        return null; // Personal crafting, virtual menus and unrelated block locations stay distinct.
    }

    private GameplayMenuAbilities() {}
}
