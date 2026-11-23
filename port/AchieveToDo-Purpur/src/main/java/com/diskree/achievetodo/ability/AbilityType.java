package com.diskree.achievetodo.ability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.diskree.achievetodo.ability.AbilityBinding.block;
import static com.diskree.achievetodo.ability.AbilityBinding.equipmentItem;
import static com.diskree.achievetodo.ability.AbilityBinding.equipmentMaterial;
import static com.diskree.achievetodo.ability.AbilityBinding.food;
import static com.diskree.achievetodo.ability.AbilityBinding.item;
import static com.diskree.achievetodo.ability.AbilityBinding.landmark;
import static com.diskree.achievetodo.ability.AbilityBinding.portal;
import static com.diskree.achievetodo.ability.AbilityBinding.toolMaterial;
import static com.diskree.achievetodo.ability.AbilityBinding.tradeTarget;

public enum AbilityType {

    VISION(AbilitiesHierarchyLayerType.MAIN, item("ender_eye")),
    EAT_SALMON(AbilitiesHierarchyLayerType.FOOD, food("salmon")),
    EAT_COD(AbilitiesHierarchyLayerType.FOOD, food("cod")),
    EAT_TROPICAL_FISH(AbilitiesHierarchyLayerType.FOOD, food("tropical_fish")),
    JUMP(AbilitiesHierarchyLayerType.MAIN, item("slime_block")),
    SWIM(AbilitiesHierarchyLayerType.MAIN, item("heart_of_the_sea")),
    EAT_ROTTEN_FLESH(AbilitiesHierarchyLayerType.FOOD, food("rotten_flesh")),
    EAT_SPIDER_EYE(AbilitiesHierarchyLayerType.FOOD, food("spider_eye")),
    EAT_SWEET_BERRIES(AbilitiesHierarchyLayerType.FOOD, food("sweet_berries")),
    OPEN_DOOR(AbilitiesHierarchyLayerType.ACTIONS, item("pale_oak_door")),
    EAT_GLOW_BERRIES(AbilitiesHierarchyLayerType.FOOD, food("glow_berries")),
    INTERACT_INSIDE_VILLAGE(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.VILLAGE)),
    SLEEP(AbilitiesHierarchyLayerType.MAIN, item("light_gray_bed")),
    EAT_POISONOUS_POTATO(AbilitiesHierarchyLayerType.FOOD, food("poisonous_potato")),
    SNEAK(AbilitiesHierarchyLayerType.MAIN, item("chainmail_leggings")),
    SPRINT(AbilitiesHierarchyLayerType.MAIN, item("chainmail_boots")),
    OPEN_INVENTORY(AbilitiesHierarchyLayerType.MAIN, item("light_gray_bundle")),
    BREAK_BLOCKS(AbilitiesHierarchyLayerType.MAIN, item("cobblestone")),
    EAT_SUSPICIOUS_STEW(AbilitiesHierarchyLayerType.FOOD, food("suspicious_stew")),
    USE_GOLDEN_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("gold")),
    OPEN_CHEST(AbilitiesHierarchyLayerType.BLOCKS, block("chest")),
    EAT_CHICKEN(AbilitiesHierarchyLayerType.FOOD, food("chicken")),
    EAT_CARROT(AbilitiesHierarchyLayerType.FOOD, food("carrot")),
    OPEN_CRAFTING_TABLE(AbilitiesHierarchyLayerType.BLOCKS, block("crafting_table")),
    EQUIP_GOLDEN_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("gold")),
    OPEN_BARREL(AbilitiesHierarchyLayerType.BLOCKS, block("barrel")),
    EAT_BEETROOT(AbilitiesHierarchyLayerType.FOOD, food("beetroot")),
    EAT_DRIED_KELP(AbilitiesHierarchyLayerType.FOOD, food("dried_kelp")),
    EAT_POTATO(AbilitiesHierarchyLayerType.FOOD, food("potato")),
    OPEN_STONECUTTER(AbilitiesHierarchyLayerType.BLOCKS, block("stonecutter")),
    GET_INTO_BOAT(AbilitiesHierarchyLayerType.MAIN, item("pale_oak_boat")),
    EAT_APPLE(AbilitiesHierarchyLayerType.FOOD, food("apple")),
    INTERACT_INSIDE_SHIPWRECK(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.SHIPWRECK)),
    USE_SHIELD(AbilitiesHierarchyLayerType.UPGRADE, item("shield")),
    EAT_MELON_SLICE(AbilitiesHierarchyLayerType.FOOD, food("melon_slice")),
    INTERACT_INSIDE_RUINED_PORTAL(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.RUINED_PORTAL)),
    OPEN_FURNACE(AbilitiesHierarchyLayerType.BLOCKS, block("furnace")),
    EAT_COOKIE(AbilitiesHierarchyLayerType.FOOD, food("cookie")),
    INTERACT_INSIDE_BURIED_TREASURE(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.BURIED_TREASURE)),
    EAT_MUSHROOM_STEW(AbilitiesHierarchyLayerType.FOOD, food("mushroom_stew")),
    EAT_BEETROOT_SOUP(AbilitiesHierarchyLayerType.FOOD, food("beetroot_soup")),
    PUT_IN_BUNDLE(AbilitiesHierarchyLayerType.MAIN, item("bundle")),
    EAT_RABBIT_STEW(AbilitiesHierarchyLayerType.FOOD, food("rabbit_stew")),
    OPEN_TRAPDOOR(AbilitiesHierarchyLayerType.ACTIONS, item("pale_oak_trapdoor")),
    USE_WOODEN_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("wood")),
    EAT_HONEY(AbilitiesHierarchyLayerType.FOOD, food("honey_bottle")),
    INTERACT_INSIDE_IGLOO(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.IGLOO)),
    USE_WATER_BUCKET(AbilitiesHierarchyLayerType.ACTIONS, item("water_bucket")),
    EAT_MUTTON(AbilitiesHierarchyLayerType.FOOD, food("mutton")),
    THROW_SNOWBALL(AbilitiesHierarchyLayerType.ACTIONS, item("snowball")),
    INTERACT_INSIDE_OCEAN_RUIN(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.OCEAN_RUIN)),
    EQUIP_LEATHER_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("leather")),
    EAT_PUFFERFISH(AbilitiesHierarchyLayerType.FOOD, food("pufferfish")),
    OPEN_FENCE_GATE(AbilitiesHierarchyLayerType.ACTIONS, item("pale_oak_fence_gate")),
    EAT_PUMPKIN_PIE(AbilitiesHierarchyLayerType.FOOD, food("pumpkin_pie")),
    EAT_GOLDEN_APPLE(AbilitiesHierarchyLayerType.FOOD, food("golden_apple")),
    USE_SHEARS(AbilitiesHierarchyLayerType.ACTIONS, item("shears")),
    INTERACT_INSIDE_DESERT_PYRAMID(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.DESERT_PYRAMID)),
    BREAK_BLOCKS_IN_NEGATIVE_Y(AbilitiesHierarchyLayerType.MAIN, item("cobbled_deepslate")),
    EAT_ENCHANTED_GOLDEN_APPLE(AbilitiesHierarchyLayerType.FOOD, food("enchanted_golden_apple")),
    INTERACT_INSIDE_MINESHAFT(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.MINESHAFT)),
    THROW_EGG(AbilitiesHierarchyLayerType.ACTIONS, item("egg")),
    USE_STONE_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("stone")),
    OPEN_GRINDSTONE(AbilitiesHierarchyLayerType.BLOCKS, block("grindstone")),
    SHOOT_CROSSBOW(AbilitiesHierarchyLayerType.UPGRADE, item("crossbow")),
    EAT_RABBIT(AbilitiesHierarchyLayerType.FOOD, food("rabbit")),
    INTERACT_INSIDE_SWAMP_HUT(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.SWAMP_HUT)),
    EQUIP_CHAINMAIL_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("chain")),
    OPEN_ANVIL(AbilitiesHierarchyLayerType.BLOCKS, block("anvil")),
    USE_FLINT_AND_STEEL(AbilitiesHierarchyLayerType.ACTIONS, item("flint_and_steel")),
    IGNITE_TNT(AbilitiesHierarchyLayerType.BLOCKS, block("tnt")),
    INTERACT_INSIDE_ANCIENT_CITY(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.ANCIENT_CITY)),
    ENTER_NETHER(AbilitiesHierarchyLayerType.UPGRADE, portal("NetherPortalBlock")),
    TRADE_WITH_WANDERING_TRADER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("wandering_trader")),
    GET_INTO_MINECART(AbilitiesHierarchyLayerType.ACTIONS, item("minecart")),
    INTERACT_INSIDE_FORTRESS(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.FORTRESS)),
    USE_OMINOUS_BOTTLE(AbilitiesHierarchyLayerType.ACTIONS, item("ominous_bottle")),
    USE_IRON_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("iron")),
    INTERACT_INSIDE_JUNGLE_PYRAMID(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.JUNGLE_PYRAMID)),
    ATTACK_WITH_TRIDENT(AbilitiesHierarchyLayerType.UPGRADE, item("trident")),
    EQUIP_IRON_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("iron")),
    SHOOT_BOW(AbilitiesHierarchyLayerType.UPGRADE, item("bow")),
    INTERACT_INSIDE_BASTION_REMNANT(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.BASTION_REMNANT)),
    USE_JUKEBOX(AbilitiesHierarchyLayerType.BLOCKS, block("jukebox")),
    THROW_ENDER_PEARL(AbilitiesHierarchyLayerType.ACTIONS, item("ender_pearl")),
    USE_COMPOSTER(AbilitiesHierarchyLayerType.BLOCKS, block("composter")),
    CHARGE_RESPAWN_ANCHOR(AbilitiesHierarchyLayerType.BLOCKS, block("respawn_anchor")),
    EAT_BEEF(AbilitiesHierarchyLayerType.FOOD, food("beef")),
    INTERACT_INSIDE_PILLAGER_OUTPOST(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.PILLAGER_OUTPOST)),
    TRADE_WITH_MASON(AbilitiesHierarchyLayerType.TRADING, tradeTarget("mason")),
    USE_FISHING_ROD(AbilitiesHierarchyLayerType.ACTIONS, item("fishing_rod")),
    EAT_PORKCHOP(AbilitiesHierarchyLayerType.FOOD, food("porkchop")),
    TRADE_WITH_CARTOGRAPHER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("cartographer")),
    INTERACT_INSIDE_MONUMENT(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.MONUMENT)),
    USE_DIAMOND_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("diamond")),
    USE_CAULDRON(AbilitiesHierarchyLayerType.BLOCKS, block("cauldron")),
    INTERACT_INSIDE_MONSTER_ROOM(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.MONSTER_ROOM)),
    EAT_BAKED_POTATO(AbilitiesHierarchyLayerType.FOOD, food("baked_potato")),
    OPEN_SMOKER(AbilitiesHierarchyLayerType.BLOCKS, block("smoker")),
    EQUIP_TURTLE_HELMET(AbilitiesHierarchyLayerType.UPGRADE, equipmentItem("turtle_helmet")),
    INTERACT_INSIDE_TRAIL_RUINS(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.TRAIL_RUINS)),
    USE_BRUSH(AbilitiesHierarchyLayerType.ACTIONS, item("brush")),
    EQUIP_DIAMOND_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("diamond")),
    UNLOCK_VAULT(AbilitiesHierarchyLayerType.BLOCKS, block("vault")),
    OPEN_BLAST_FURNACE(AbilitiesHierarchyLayerType.BLOCKS, block("blast_furnace")),
    INTERACT_INSIDE_STRONGHOLD(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.STRONGHOLD)),
    TRADE_WITH_LEATHERWORKER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("leatherworker")),
    USE_SPYGLASS(AbilitiesHierarchyLayerType.ACTIONS, item("spyglass")),
    INTERACT_INSIDE_MANSION(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.MANSION)),
    OPEN_BEACON(AbilitiesHierarchyLayerType.BLOCKS, block("beacon")),
    THROW_WIND_CHARGE(AbilitiesHierarchyLayerType.ACTIONS, item("wind_charge")),
    ENTER_END(AbilitiesHierarchyLayerType.UPGRADE, portal("EndPortalBlock")),
    OPEN_CARTOGRAPHY_TABLE(AbilitiesHierarchyLayerType.BLOCKS, block("cartography_table")),
    INTERACT_INSIDE_DESERT_WELL(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.DESERT_WELL)),
    EAT_COOKED_SALMON(AbilitiesHierarchyLayerType.FOOD, food("cooked_salmon")),
    EQUIP_ELYTRA(AbilitiesHierarchyLayerType.UPGRADE, equipmentItem("elytra")),
    TRADE_WITH_SHEPHERD(AbilitiesHierarchyLayerType.TRADING, tradeTarget("shepherd")),
    TRADE_WITH_BUTCHER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("butcher")),
    INTERACT_INSIDE_TRIAL_CHAMBERS(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.TRIAL_CHAMBERS)),
    OPEN_ENDER_CHEST(AbilitiesHierarchyLayerType.BLOCKS, block("ender_chest")),
    ATTACK_WITH_MACE(AbilitiesHierarchyLayerType.UPGRADE, item("mace")),
    INTERACT_INSIDE_END_CITY(AbilitiesHierarchyLayerType.LANDMARK, landmark(LandmarkType.END_CITY)),
    USE_ENDER_EYE(AbilitiesHierarchyLayerType.ACTIONS, item("ender_eye")),
    TELEPORT_OUTER_ISLANDS(AbilitiesHierarchyLayerType.UPGRADE, portal("EndGatewayBlock")),
    USE_NETHERITE_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("netherite")),
    EAT_COOKED_COD(AbilitiesHierarchyLayerType.FOOD, food("cooked_cod")),
    TRADE_WITH_FARMER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("farmer")),
    GLIDE_WITH_FIREWORKS(AbilitiesHierarchyLayerType.ACTIONS, item("firework_rocket")),
    TRADE_WITH_CLERIC(AbilitiesHierarchyLayerType.TRADING, tradeTarget("cleric")),
    EQUIP_NETHERITE_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("netherite")),
    OPEN_BREWING_STAND(AbilitiesHierarchyLayerType.BLOCKS, block("brewing_stand")),
    EAT_COOKED_RABBIT(AbilitiesHierarchyLayerType.FOOD, food("cooked_rabbit")),
    PLACE_END_CRYSTAL(AbilitiesHierarchyLayerType.ACTIONS, item("end_crystal")),
    TRADE_WITH_FISHERMAN(AbilitiesHierarchyLayerType.TRADING, tradeTarget("fisherman")),
    OPEN_SMITHING_TABLE(AbilitiesHierarchyLayerType.BLOCKS, block("smithing_table")),
    EAT_COOKED_CHICKEN(AbilitiesHierarchyLayerType.FOOD, food("cooked_chicken")),
    EAT_CHORUS_FRUIT(AbilitiesHierarchyLayerType.FOOD, food("chorus_fruit")),
    TRADE_WITH_FLETCHER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("fletcher")),
    EAT_COOKED_MUTTON(AbilitiesHierarchyLayerType.FOOD, food("cooked_mutton")),
    TRADE_WITH_ARMORER(AbilitiesHierarchyLayerType.TRADING, tradeTarget("armorer")),
    EAT_COOKED_PORKCHOP(AbilitiesHierarchyLayerType.FOOD, food("cooked_porkchop")),
    EAT_BREAD(AbilitiesHierarchyLayerType.FOOD, food("bread")),
    TRADE_WITH_WEAPONSMITH(AbilitiesHierarchyLayerType.TRADING, tradeTarget("weaponsmith")),
    EAT_COOKED_BEEF(AbilitiesHierarchyLayerType.FOOD, food("cooked_beef")),
    OPEN_LOOM(AbilitiesHierarchyLayerType.BLOCKS, block("loom")),
    USE_CAMPFIRE(AbilitiesHierarchyLayerType.BLOCKS, block("campfire")),
    OPEN_SHULKER_BOX(AbilitiesHierarchyLayerType.BLOCKS, block("light_gray_shulker_box")),
    EAT_GOLDEN_CARROT(AbilitiesHierarchyLayerType.FOOD, food("golden_carrot")),
    TRADE_WITH_TOOLSMITH(AbilitiesHierarchyLayerType.TRADING, tradeTarget("toolsmith")),
    TRADE_WITH_LIBRARIAN(AbilitiesHierarchyLayerType.TRADING, tradeTarget("librarian")),
    OPEN_ENCHANTING_TABLE(AbilitiesHierarchyLayerType.BLOCKS, block("enchanting_table")),
    USE_COPPER_TOOLS(AbilitiesHierarchyLayerType.UPGRADE, toolMaterial("copper")),
    EQUIP_COPPER_ARMOR(AbilitiesHierarchyLayerType.UPGRADE, equipmentMaterial("copper"));

    public static final String MOD_ID = "achievetodo";
    public static final String ADVANCEMENT_PATH_PREFIX = "abilities/";

    private static final Map<String, AbilityType> BY_NAME;
    private static final Map<String, AbilityType> BY_ADVANCEMENT_ID;

    static {
        Map<String, AbilityType> byName = new HashMap<>();
        Map<String, AbilityType> byAdvancementId = new HashMap<>();
        for (AbilityType abilityType : values()) {
            byName.put(abilityType.getName(), abilityType);
            byAdvancementId.put(abilityType.getAdvancementId(), abilityType);
        }
        BY_NAME = Collections.unmodifiableMap(byName);
        BY_ADVANCEMENT_ID = Collections.unmodifiableMap(byAdvancementId);
    }

    private final AbilitiesHierarchyLayerType hierarchyLayerType;
    private final AbilityBinding binding;

    AbilityType(AbilitiesHierarchyLayerType hierarchyLayerType, AbilityBinding binding) {
        this.hierarchyLayerType = hierarchyLayerType;
        this.binding = binding;
    }

    public AbilitiesHierarchyLayerType getHierarchyLayerType() {
        return hierarchyLayerType;
    }

    public AbilityBinding getBinding() {
        return binding;
    }

    public LandmarkType getLandmarkType() {
        return binding.landmarkType();
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getAdvancementId() {
        return MOD_ID + ":" + ADVANCEMENT_PATH_PREFIX + getName();
    }

    public int getChaosPriority() {
        if (this == OPEN_CRAFTING_TABLE) {
            return 100;
        }
        if (this == INTERACT_INSIDE_FORTRESS ||
            this == INTERACT_INSIDE_STRONGHOLD ||
            this == USE_ENDER_EYE ||
            this == ENTER_NETHER ||
            this == ENTER_END) {
            return 90;
        }
        if (this == OPEN_BEACON ||
            this == OPEN_ENDER_CHEST ||
            this == OPEN_SMITHING_TABLE ||
            this == OPEN_ANVIL ||
            this == OPEN_STONECUTTER) {
            return 80;
        }
        if (this == USE_COPPER_TOOLS || this == USE_GOLDEN_TOOLS ||
            this == USE_WOODEN_TOOLS ||
            this == USE_STONE_TOOLS ||
            this == USE_IRON_TOOLS ||
            this == USE_SHIELD ||
            this == USE_WATER_BUCKET ||
            this == USE_FLINT_AND_STEEL) {
            return 70;
        }
        if (this == EQUIP_COPPER_ARMOR || this == EQUIP_LEATHER_ARMOR ||
            this == EQUIP_CHAINMAIL_ARMOR ||
            this == EQUIP_IRON_ARMOR ||
            this == EQUIP_ELYTRA) {
            return 60;
        }
        if (this == THROW_ENDER_PEARL ||
            this == USE_OMINOUS_BOTTLE ||
            this == PLACE_END_CRYSTAL) {
            return 50;
        }
        if (this == EAT_PUFFERFISH ||
            this == EAT_ROTTEN_FLESH ||
            this == EAT_SPIDER_EYE ||
            this == EAT_SUSPICIOUS_STEW ||
            this == EAT_POISONOUS_POTATO) {
            return 40;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.UPGRADE ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.BLOCKS ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.ACTIONS) {
            return 30;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.TRADING ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.LANDMARK ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.FOOD) {
            return 0;
        }
        return 100;
    }

    public static Optional<AbilityType> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.trim().toLowerCase(Locale.ROOT)));
    }

    public static Optional<AbilityType> findByAdvancementId(String advancementId) {
        if (advancementId == null || advancementId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ADVANCEMENT_ID.get(advancementId.trim().toLowerCase(Locale.ROOT)));
    }
}
