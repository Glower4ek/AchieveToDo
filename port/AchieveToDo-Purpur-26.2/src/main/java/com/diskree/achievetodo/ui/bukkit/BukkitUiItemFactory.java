package com.diskree.achievetodo.ui.bukkit;

import com.diskree.achievetodo.ability.AbilityBinding;
import com.diskree.achievetodo.ability.AbilityBindingType;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.ui.UiAbilityGroup;
import com.diskree.achievetodo.ui.UiAbilityState;
import com.diskree.achievetodo.ui.UiAbilityViewModel;
import com.diskree.achievetodo.ui.UiIconKey;
import com.diskree.achievetodo.ui.UiTabViewModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BukkitUiItemFactory {

    ItemStack createTabItem(UiTabViewModel tab, UiPresentationMode presentationMode) {
        Material material = groupMaterial(tab.group());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(tab.title(), tab.selected() ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(toLore(tab.lore(), tab.selected() ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        applyPresentationModel(meta, presentationMode, "tab_" + tab.group().name().toLowerCase(Locale.ROOT) + (tab.selected() ? "_selected" : ""));
        if (tab.selected()) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createAbilityItem(UiAbilityViewModel viewModel, UiPresentationMode presentationMode) {
        Material material = materialForAbility(viewModel.abilityType(), viewModel.state(), viewModel.iconKey());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(viewModel.title(), titleColor(viewModel.state()))
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(toLore(viewModel.lore(), loreColor(viewModel.state())));
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
            ItemFlag.HIDE_ARMOR_TRIM,
            ItemFlag.HIDE_UNBREAKABLE
        );
        if (presentationMode == UiPresentationMode.ENHANCED && viewModel.state() == UiAbilityState.UNKNOWN_LOCKED) {
            applyAbilityPresentationModel(meta, viewModel);
        }
        if (viewModel.state() == UiAbilityState.UNLOCKED || viewModel.state() == UiAbilityState.INITIALLY_UNLOCKED) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createNavigationItem(UiIconKey iconKey, String title, List<String> lore, boolean active, UiPresentationMode presentationMode) {
        Material material = switch (iconKey.value()) {
            case "navigation.previous" -> Material.ARROW;
            case "navigation.next" -> Material.SPECTRAL_ARROW;
            case "navigation.close" -> Material.BARRIER;
            default -> Material.PAPER;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, active ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(toLore(lore, active ? NamedTextColor.GRAY : NamedTextColor.DARK_GRAY));
        applyPresentationModel(meta, presentationMode, iconKey.value().replace('.', '_') + (active ? "" : "_disabled"));
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createInfoItem(String title, List<String> lore, UiPresentationMode presentationMode) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(toLore(lore, NamedTextColor.GRAY));
        applyPresentationModel(meta, presentationMode, "info");
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createFiller(UiPresentationMode presentationMode) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        applyPresentationModel(meta, presentationMode, "filler");
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> toLore(List<String> lore, NamedTextColor color) {
        List<Component> components = new ArrayList<>(lore.size());
        for (String line : lore) {
            components.add(Component.text(line, color).decoration(TextDecoration.ITALIC, false));
        }
        return components;
    }

    private NamedTextColor titleColor(UiAbilityState state) {
        return switch (state) {
            case UNKNOWN_LOCKED -> NamedTextColor.DARK_GRAY;
            case DEMYSTIFIED_LOCKED -> NamedTextColor.RED;
            case UNLOCKED -> NamedTextColor.GREEN;
            case INITIALLY_UNLOCKED -> NamedTextColor.GRAY;
            case PERMANENTLY_LOCKED -> NamedTextColor.DARK_RED;
        };
    }

    private NamedTextColor loreColor(UiAbilityState state) {
        return switch (state) {
            case UNKNOWN_LOCKED -> NamedTextColor.GRAY;
            case DEMYSTIFIED_LOCKED -> NamedTextColor.GRAY;
            case UNLOCKED -> NamedTextColor.GRAY;
            case INITIALLY_UNLOCKED -> NamedTextColor.GRAY;
            case PERMANENTLY_LOCKED -> NamedTextColor.RED;
        };
    }

    private Material groupMaterial(UiAbilityGroup group) {
        return switch (group) {
            case MAIN -> Material.NETHER_STAR;
            case ACTIONS -> Material.WIND_CHARGE;
            case BLOCKS -> Material.BRICKS;
            case UPGRADE -> Material.IRON_PICKAXE;
            case FOOD -> Material.GOLDEN_CARROT;
            case TRADING -> Material.EMERALD;
            case LANDMARK -> Material.MAP;
        };
    }

    private Material materialForAbility(AbilityType abilityType, UiAbilityState state, UiIconKey iconKey) {
        if (state == UiAbilityState.UNKNOWN_LOCKED) {
            return Material.PAPER;
        }
        AbilityBinding binding = abilityType.getBinding();
        return switch (binding.type()) {
            case ITEM, BLOCK, FOOD, EQUIPMENT_ITEM -> exactMaterial(binding.key(), Material.PAPER);
            case TOOL_MATERIAL -> switch (binding.key()) {
                case "wood" -> Material.WOODEN_PICKAXE;
                case "stone" -> Material.STONE_PICKAXE;
                case "gold" -> Material.GOLDEN_PICKAXE;
                case "iron" -> Material.IRON_PICKAXE;
                case "diamond" -> Material.DIAMOND_PICKAXE;
                case "netherite" -> Material.NETHERITE_PICKAXE;
                default -> Material.IRON_PICKAXE;
            };
            case EQUIPMENT_MATERIAL -> switch (binding.key()) {
                case "leather" -> Material.LEATHER_CHESTPLATE;
                case "chain" -> Material.CHAINMAIL_CHESTPLATE;
                case "gold" -> Material.GOLDEN_CHESTPLATE;
                case "iron" -> Material.IRON_CHESTPLATE;
                case "diamond" -> Material.DIAMOND_CHESTPLATE;
                case "netherite" -> Material.NETHERITE_CHESTPLATE;
                default -> Material.CHEST;
            };
            case PORTAL -> switch (binding.key()) {
                case "netherportalblock" -> Material.OBSIDIAN;
                case "endportalblock" -> Material.END_PORTAL_FRAME;
                case "endgatewayblock" -> Material.END_GATEWAY;
                default -> Material.OBSIDIAN;
            };
            case TRADE_TARGET -> Material.EMERALD;
            case LANDMARK -> landmarkMaterial(abilityType.getLandmarkType());
        };
    }

    private Material exactMaterial(String key, Material fallback) {
        Material material = Material.matchMaterial("minecraft:" + key);
        if (material != null) {
            return material;
        }
        material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private Material landmarkMaterial(LandmarkType landmarkType) {
        if (landmarkType == null) {
            return Material.MAP;
        }
        return switch (landmarkType) {
            case VILLAGE -> Material.BELL;
            case SHIPWRECK -> Material.OAK_BOAT;
            case RUINED_PORTAL -> Material.CRYING_OBSIDIAN;
            case BURIED_TREASURE -> Material.CHEST;
            case IGLOO -> Material.SNOW_BLOCK;
            case OCEAN_RUIN -> Material.PRISMARINE_BRICKS;
            case DESERT_PYRAMID -> Material.CHISELED_SANDSTONE;
            case MINESHAFT -> Material.RAIL;
            case SWAMP_HUT -> Material.CAULDRON;
            case ANCIENT_CITY -> Material.SCULK_SHRIEKER;
            case FORTRESS -> Material.NETHER_BRICKS;
            case JUNGLE_PYRAMID -> Material.MOSSY_COBBLESTONE;
            case BASTION_REMNANT -> Material.GILDED_BLACKSTONE;
            case PILLAGER_OUTPOST -> Material.CROSSBOW;
            case MONUMENT -> Material.PRISMARINE;
            case MONSTER_ROOM -> Material.SPAWNER;
            case TRAIL_RUINS -> Material.BRICKS;
            case STRONGHOLD -> Material.END_PORTAL_FRAME;
            case MANSION -> Material.TOTEM_OF_UNDYING;
            case DESERT_WELL -> Material.SANDSTONE;
            case TRIAL_CHAMBERS -> Material.TRIAL_KEY;
            case END_CITY -> Material.PURPUR_BLOCK;
        };
    }

    private void applyAbilityPresentationModel(ItemMeta meta, UiAbilityViewModel viewModel) {
        if (viewModel.state() == UiAbilityState.UNKNOWN_LOCKED) {
            applyPresentationModel(meta, UiPresentationMode.ENHANCED, "ability_unknown");
        }
    }

    private void applyPresentationModel(ItemMeta meta, UiPresentationMode presentationMode, String modelKey) {
        if (presentationMode != UiPresentationMode.ENHANCED) {
            return;
        }
        meta.setItemModel(NamespacedKey.fromString("achievetodo:ui/" + modelKey));
    }
}
