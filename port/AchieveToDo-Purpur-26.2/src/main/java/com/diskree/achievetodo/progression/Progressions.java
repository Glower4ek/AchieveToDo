package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.diskree.achievetodo.ability.AbilityType.ATTACK_WITH_MACE;
import static com.diskree.achievetodo.ability.AbilityType.ATTACK_WITH_TRIDENT;
import static com.diskree.achievetodo.ability.AbilityType.BREAK_BLOCKS;
import static com.diskree.achievetodo.ability.AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y;
import static com.diskree.achievetodo.ability.AbilityType.CHARGE_RESPAWN_ANCHOR;
import static com.diskree.achievetodo.ability.AbilityType.EAT_APPLE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_BAKED_POTATO;
import static com.diskree.achievetodo.ability.AbilityType.EAT_BEEF;
import static com.diskree.achievetodo.ability.AbilityType.EAT_BEETROOT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_BEETROOT_SOUP;
import static com.diskree.achievetodo.ability.AbilityType.EAT_BREAD;
import static com.diskree.achievetodo.ability.AbilityType.EAT_CARROT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_CHICKEN;
import static com.diskree.achievetodo.ability.AbilityType.EAT_CHORUS_FRUIT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COD;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKIE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_BEEF;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_CHICKEN;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_COD;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_MUTTON;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_PORKCHOP;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_RABBIT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_COOKED_SALMON;
import static com.diskree.achievetodo.ability.AbilityType.EAT_DRIED_KELP;
import static com.diskree.achievetodo.ability.AbilityType.EAT_ENCHANTED_GOLDEN_APPLE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_GLOW_BERRIES;
import static com.diskree.achievetodo.ability.AbilityType.EAT_GOLDEN_APPLE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_GOLDEN_CARROT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_HONEY;
import static com.diskree.achievetodo.ability.AbilityType.EAT_MELON_SLICE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_MUSHROOM_STEW;
import static com.diskree.achievetodo.ability.AbilityType.EAT_MUTTON;
import static com.diskree.achievetodo.ability.AbilityType.EAT_POISONOUS_POTATO;
import static com.diskree.achievetodo.ability.AbilityType.EAT_POTATO;
import static com.diskree.achievetodo.ability.AbilityType.EAT_PORKCHOP;
import static com.diskree.achievetodo.ability.AbilityType.EAT_PUFFERFISH;
import static com.diskree.achievetodo.ability.AbilityType.EAT_PUMPKIN_PIE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_RABBIT;
import static com.diskree.achievetodo.ability.AbilityType.EAT_RABBIT_STEW;
import static com.diskree.achievetodo.ability.AbilityType.EAT_ROTTEN_FLESH;
import static com.diskree.achievetodo.ability.AbilityType.EAT_SALMON;
import static com.diskree.achievetodo.ability.AbilityType.EAT_SPIDER_EYE;
import static com.diskree.achievetodo.ability.AbilityType.EAT_SUSPICIOUS_STEW;
import static com.diskree.achievetodo.ability.AbilityType.EAT_SWEET_BERRIES;
import static com.diskree.achievetodo.ability.AbilityType.EAT_TROPICAL_FISH;
import static com.diskree.achievetodo.ability.AbilityType.ENTER_END;
import static com.diskree.achievetodo.ability.AbilityType.ENTER_NETHER;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_CHAINMAIL_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_DIAMOND_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_ELYTRA;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_GOLDEN_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_IRON_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_LEATHER_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_NETHERITE_ARMOR;
import static com.diskree.achievetodo.ability.AbilityType.EQUIP_TURTLE_HELMET;
import static com.diskree.achievetodo.ability.AbilityType.GET_INTO_BOAT;
import static com.diskree.achievetodo.ability.AbilityType.GET_INTO_MINECART;
import static com.diskree.achievetodo.ability.AbilityType.GLIDE_WITH_FIREWORKS;
import static com.diskree.achievetodo.ability.AbilityType.IGNITE_TNT;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_ANCIENT_CITY;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_BASTION_REMNANT;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_BURIED_TREASURE;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_DESERT_PYRAMID;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_DESERT_WELL;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_END_CITY;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_FORTRESS;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_IGLOO;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_JUNGLE_PYRAMID;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_MANSION;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_MINESHAFT;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_MONSTER_ROOM;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_MONUMENT;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_OCEAN_RUIN;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_PILLAGER_OUTPOST;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_RUINED_PORTAL;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_SHIPWRECK;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_STRONGHOLD;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_SWAMP_HUT;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_TRAIL_RUINS;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_TRIAL_CHAMBERS;
import static com.diskree.achievetodo.ability.AbilityType.INTERACT_INSIDE_VILLAGE;
import static com.diskree.achievetodo.ability.AbilityType.JUMP;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_ANVIL;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_BARREL;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_BEACON;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_BLAST_FURNACE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_BREWING_STAND;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_CARTOGRAPHY_TABLE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_CHEST;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_CRAFTING_TABLE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_DOOR;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_ENCHANTING_TABLE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_ENDER_CHEST;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_FENCE_GATE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_FURNACE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_GRINDSTONE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_INVENTORY;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_LOOM;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_SHULKER_BOX;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_SMITHING_TABLE;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_SMOKER;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_STONECUTTER;
import static com.diskree.achievetodo.ability.AbilityType.OPEN_TRAPDOOR;
import static com.diskree.achievetodo.ability.AbilityType.PLACE_END_CRYSTAL;
import static com.diskree.achievetodo.ability.AbilityType.PUT_IN_BUNDLE;
import static com.diskree.achievetodo.ability.AbilityType.SHOOT_BOW;
import static com.diskree.achievetodo.ability.AbilityType.SHOOT_CROSSBOW;
import static com.diskree.achievetodo.ability.AbilityType.SLEEP;
import static com.diskree.achievetodo.ability.AbilityType.SNEAK;
import static com.diskree.achievetodo.ability.AbilityType.SPRINT;
import static com.diskree.achievetodo.ability.AbilityType.SWIM;
import static com.diskree.achievetodo.ability.AbilityType.TELEPORT_OUTER_ISLANDS;
import static com.diskree.achievetodo.ability.AbilityType.THROW_EGG;
import static com.diskree.achievetodo.ability.AbilityType.THROW_ENDER_PEARL;
import static com.diskree.achievetodo.ability.AbilityType.THROW_SNOWBALL;
import static com.diskree.achievetodo.ability.AbilityType.THROW_WIND_CHARGE;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_ARMORER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_BUTCHER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_CARTOGRAPHER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_CLERIC;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_FARMER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_FISHERMAN;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_FLETCHER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_LEATHERWORKER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_LIBRARIAN;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_MASON;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_SHEPHERD;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_TOOLSMITH;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_WANDERING_TRADER;
import static com.diskree.achievetodo.ability.AbilityType.TRADE_WITH_WEAPONSMITH;
import static com.diskree.achievetodo.ability.AbilityType.UNLOCK_VAULT;
import static com.diskree.achievetodo.ability.AbilityType.USE_BRUSH;
import static com.diskree.achievetodo.ability.AbilityType.USE_CAMPFIRE;
import static com.diskree.achievetodo.ability.AbilityType.USE_CAULDRON;
import static com.diskree.achievetodo.ability.AbilityType.USE_COMPOSTER;
import static com.diskree.achievetodo.ability.AbilityType.USE_DIAMOND_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.USE_ENDER_EYE;
import static com.diskree.achievetodo.ability.AbilityType.USE_FISHING_ROD;
import static com.diskree.achievetodo.ability.AbilityType.USE_FLINT_AND_STEEL;
import static com.diskree.achievetodo.ability.AbilityType.USE_GOLDEN_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.USE_IRON_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.USE_JUKEBOX;
import static com.diskree.achievetodo.ability.AbilityType.USE_NETHERITE_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.USE_OMINOUS_BOTTLE;
import static com.diskree.achievetodo.ability.AbilityType.USE_SHEARS;
import static com.diskree.achievetodo.ability.AbilityType.USE_SHIELD;
import static com.diskree.achievetodo.ability.AbilityType.USE_SPYGLASS;
import static com.diskree.achievetodo.ability.AbilityType.USE_STONE_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.USE_WATER_BUCKET;
import static com.diskree.achievetodo.ability.AbilityType.USE_WOODEN_TOOLS;
import static com.diskree.achievetodo.ability.AbilityType.VISION;

public final class Progressions {

    private Progressions() {
    }

    public static Map<AbilityType, Integer> getEasyProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        progression.put(VISION, 0);
        progression.put(JUMP, 0);
        addCycle(progression, 2,
            EAT_SALMON, SWIM, BREAK_BLOCKS, EAT_COD, SNEAK, EAT_TROPICAL_FISH, SPRINT, EAT_SWEET_BERRIES, OPEN_DOOR,
            EAT_ROTTEN_FLESH, SLEEP, EAT_SPIDER_EYE, OPEN_INVENTORY, PUT_IN_BUNDLE, EAT_GLOW_BERRIES,
            BREAK_BLOCKS_IN_NEGATIVE_Y, EAT_POISONOUS_POTATO, EAT_SUSPICIOUS_STEW, USE_GOLDEN_TOOLS, OPEN_CHEST,
            GET_INTO_BOAT
        );
        addFixedProgressionCycles(progression, 0);
        progression.put(AbilityType.USE_COPPER_TOOLS, 98);
        progression.put(AbilityType.EQUIP_COPPER_ARMOR, 68);
        return progression;
    }

    public static Map<AbilityType, Integer> getNormalProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        progression.put(VISION, 0);
        addCycle(progression, 2,
            EAT_SALMON, JUMP, EAT_COD, EAT_TROPICAL_FISH, SWIM, SNEAK, EAT_SWEET_BERRIES, SPRINT, EAT_ROTTEN_FLESH,
            OPEN_DOOR, EAT_SPIDER_EYE, SLEEP, EAT_GLOW_BERRIES, EAT_POISONOUS_POTATO, OPEN_INVENTORY, BREAK_BLOCKS,
            EAT_SUSPICIOUS_STEW, PUT_IN_BUNDLE, BREAK_BLOCKS_IN_NEGATIVE_Y, USE_GOLDEN_TOOLS, OPEN_CHEST, GET_INTO_BOAT
        );
        addFixedProgressionCycles(progression, 1);
        progression.put(AbilityType.USE_COPPER_TOOLS, 128);
        progression.put(AbilityType.EQUIP_COPPER_ARMOR, 83);
        return progression;
    }

    public static Map<AbilityType, Integer> getHardProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        addCycle(progression, 2,
            VISION, EAT_SALMON, EAT_COD, JUMP, EAT_TROPICAL_FISH, EAT_SWEET_BERRIES, SWIM, EAT_ROTTEN_FLESH,
            EAT_SPIDER_EYE, OPEN_DOOR, EAT_GLOW_BERRIES, SLEEP, EAT_POISONOUS_POTATO, SNEAK, SPRINT, OPEN_INVENTORY,
            BREAK_BLOCKS, EAT_SUSPICIOUS_STEW, USE_GOLDEN_TOOLS, OPEN_CHEST, BREAK_BLOCKS_IN_NEGATIVE_Y, GET_INTO_BOAT,
            PUT_IN_BUNDLE
        );
        addFixedProgressionCycles(progression, 2);
        progression.put(AbilityType.USE_COPPER_TOOLS, 158);
        progression.put(AbilityType.EQUIP_COPPER_ARMOR, 98);
        return progression;
    }

    private static void addFixedProgressionCycles(Map<AbilityType, Integer> progression, int extraSpacing) {
        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            EAT_CHICKEN, OPEN_CRAFTING_TABLE, EAT_CARROT, EQUIP_GOLDEN_ARMOR, OPEN_BARREL, EAT_BEETROOT,
            EQUIP_LEATHER_ARMOR, EAT_POTATO, USE_WOODEN_TOOLS, EAT_DRIED_KELP, OPEN_STONECUTTER, EAT_RABBIT_STEW,
            USE_CAMPFIRE, INTERACT_INSIDE_SHIPWRECK, EAT_APPLE, USE_SHIELD, INTERACT_INSIDE_RUINED_PORTAL,
            EAT_MELON_SLICE, EQUIP_CHAINMAIL_ARMOR, OPEN_FURNACE, EAT_COOKIE, INTERACT_INSIDE_BURIED_TREASURE,
            EAT_MUSHROOM_STEW, USE_STONE_TOOLS, EAT_BEETROOT_SOUP, OPEN_TRAPDOOR, TRADE_WITH_WANDERING_TRADER,
            EAT_HONEY, INTERACT_INSIDE_IGLOO, USE_WATER_BUCKET, EAT_MUTTON, USE_IRON_TOOLS, THROW_SNOWBALL,
            INTERACT_INSIDE_OCEAN_RUIN, GET_INTO_MINECART
        );

        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            EAT_PUFFERFISH, OPEN_FENCE_GATE, EAT_GOLDEN_APPLE, USE_SHEARS, USE_FLINT_AND_STEEL, IGNITE_TNT,
            INTERACT_INSIDE_DESERT_PYRAMID, EAT_PUMPKIN_PIE, USE_FISHING_ROD, INTERACT_INSIDE_VILLAGE, TRADE_WITH_MASON,
            OPEN_BLAST_FURNACE, THROW_ENDER_PEARL, EQUIP_IRON_ARMOR, ENTER_NETHER, EAT_ENCHANTED_GOLDEN_APPLE,
            OPEN_ANVIL, OPEN_LOOM, ATTACK_WITH_TRIDENT, INTERACT_INSIDE_MINESHAFT, THROW_EGG, OPEN_GRINDSTONE,
            USE_BRUSH, SHOOT_CROSSBOW, EAT_RABBIT, INTERACT_INSIDE_SWAMP_HUT, USE_JUKEBOX, TRADE_WITH_CARTOGRAPHER,
            OPEN_SMITHING_TABLE, OPEN_CARTOGRAPHY_TABLE, USE_COMPOSTER, INTERACT_INSIDE_FORTRESS, USE_SPYGLASS,
            THROW_WIND_CHARGE, INTERACT_INSIDE_BASTION_REMNANT
        );

        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            INTERACT_INSIDE_ANCIENT_CITY, EAT_BEEF, SHOOT_BOW, TRADE_WITH_LEATHERWORKER, USE_OMINOUS_BOTTLE,
            EQUIP_DIAMOND_ARMOR, EAT_COOKED_RABBIT, INTERACT_INSIDE_JUNGLE_PYRAMID, OPEN_ENDER_CHEST,
            TRADE_WITH_SHEPHERD, USE_DIAMOND_TOOLS, EAT_PORKCHOP, ATTACK_WITH_MACE, USE_NETHERITE_TOOLS, USE_ENDER_EYE,
            EAT_BREAD, TRADE_WITH_BUTCHER, CHARGE_RESPAWN_ANCHOR, EAT_COOKED_SALMON, INTERACT_INSIDE_PILLAGER_OUTPOST,
            ENTER_END, EAT_BAKED_POTATO, INTERACT_INSIDE_MONUMENT, TRADE_WITH_FARMER, OPEN_BEACON,
            INTERACT_INSIDE_STRONGHOLD, USE_CAULDRON, INTERACT_INSIDE_MONSTER_ROOM, OPEN_SMOKER, EQUIP_TURTLE_HELMET,
            INTERACT_INSIDE_TRAIL_RUINS, EAT_COOKED_CHICKEN, UNLOCK_VAULT, TRADE_WITH_CLERIC, EQUIP_NETHERITE_ARMOR,
            EAT_COOKED_MUTTON, TRADE_WITH_FISHERMAN, INTERACT_INSIDE_MANSION, EAT_COOKED_COD,
            INTERACT_INSIDE_DESERT_WELL, EQUIP_ELYTRA, OPEN_SHULKER_BOX, TRADE_WITH_FLETCHER, INTERACT_INSIDE_END_CITY,
            PLACE_END_CRYSTAL, INTERACT_INSIDE_TRIAL_CHAMBERS, TRADE_WITH_ARMORER, TELEPORT_OUTER_ISLANDS,
            GLIDE_WITH_FIREWORKS, OPEN_BREWING_STAND, EAT_CHORUS_FRUIT, TRADE_WITH_WEAPONSMITH, EAT_COOKED_PORKCHOP,
            EAT_COOKED_BEEF, TRADE_WITH_TOOLSMITH, EAT_GOLDEN_CARROT, TRADE_WITH_LIBRARIAN, OPEN_ENCHANTING_TABLE
        );
    }

    private static void addCycle(Map<AbilityType, Integer> config, int spacing, AbilityType... abilityTypes) {
        int requiredCount = config.isEmpty() ? 0 : Collections.max(config.values());
        for (AbilityType abilityType : abilityTypes) {
            requiredCount += spacing;
            config.put(abilityType, requiredCount);
        }
    }
}
