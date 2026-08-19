package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.gui.AbilityUnlockedToastType;
import com.diskree.achievetodo.client.gui.DesignCodePalette;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum AbilityType {

    VISION(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.ENDER_EYE),
    EAT_SALMON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.SALMON),
    EAT_COD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COD),
    EAT_TROPICAL_FISH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.TROPICAL_FISH),
    JUMP(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.SLIME_BLOCK),
    SWIM(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.HEART_OF_THE_SEA),
    EAT_ROTTEN_FLESH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.ROTTEN_FLESH),
    EAT_SPIDER_EYE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.SPIDER_EYE),
    EAT_SWEET_BERRIES(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.SWEET_BERRIES),
    OPEN_DOOR(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_DOOR),
    EAT_GLOW_BERRIES(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.GLOW_BERRIES),
    INTERACT_INSIDE_VILLAGE(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.VILLAGE),
    SLEEP(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, itemByPath("light_gray_bed")),
    EAT_POISONOUS_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.POISONOUS_POTATO),
    SNEAK(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.CHAINMAIL_LEGGINGS),
    SPRINT(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.CHAINMAIL_BOOTS),
    OPEN_INVENTORY(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, itemByPath("light_gray_bundle")),
    BREAK_BLOCKS(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.COBBLESTONE),
    EAT_SUSPICIOUS_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.SUSPICIOUS_STEW),
    USE_GOLDEN_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.GOLD),
    OPEN_CHEST(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CHEST),
    EAT_CHICKEN(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.CHICKEN),
    EAT_CARROT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.CARROT),
    OPEN_CRAFTING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CRAFTING_TABLE),
    EQUIP_GOLDEN_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.GOLD),
    OPEN_BARREL(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BARREL),
    EAT_BEETROOT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.BEETROOT),
    EAT_DRIED_KELP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.DRIED_KELP),
    EAT_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.POTATO),
    OPEN_STONECUTTER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.STONECUTTER),
    GET_INTO_BOAT(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.MAIN, Items.PALE_OAK_BOAT),
    EAT_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.APPLE),
    INTERACT_INSIDE_SHIPWRECK(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.SHIPWRECK),
    USE_SHIELD(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.SHIELD),
    EAT_MELON_SLICE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.MELON_SLICE),
    INTERACT_INSIDE_RUINED_PORTAL(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.RUINED_PORTAL),
    OPEN_FURNACE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.FURNACE),
    EAT_COOKIE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKIE),
    INTERACT_INSIDE_BURIED_TREASURE(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.BURIED_TREASURE),
    EAT_MUSHROOM_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.MUSHROOM_STEW),
    EAT_BEETROOT_SOUP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.BEETROOT_SOUP),
    PUT_IN_BUNDLE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.MAIN, Items.BUNDLE),
    EAT_RABBIT_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.RABBIT_STEW),
    OPEN_TRAPDOOR(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_TRAPDOOR),
    USE_WOODEN_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.WOOD),
    EAT_HONEY(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.HONEY_BOTTLE),
    INTERACT_INSIDE_IGLOO(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.IGLOO),
    USE_WATER_BUCKET(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.WATER_BUCKET),
    EAT_MUTTON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.MUTTON),
    THROW_SNOWBALL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SNOWBALL),
    INTERACT_INSIDE_OCEAN_RUIN(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.OCEAN_RUIN),
    EQUIP_LEATHER_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.LEATHER),
    EAT_PUFFERFISH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.PUFFERFISH),
    OPEN_FENCE_GATE(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_FENCE_GATE),
    EAT_PUMPKIN_PIE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.PUMPKIN_PIE),
    EAT_GOLDEN_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.GOLDEN_APPLE),
    USE_SHEARS(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SHEARS),
    INTERACT_INSIDE_DESERT_PYRAMID(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.DESERT_PYRAMID),
    BREAK_BLOCKS_IN_NEGATIVE_Y(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.COBBLED_DEEPSLATE),
    EAT_ENCHANTED_GOLDEN_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.ENCHANTED_GOLDEN_APPLE),
    INTERACT_INSIDE_MINESHAFT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MINESHAFT),
    THROW_EGG(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.EGG),
    USE_STONE_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.STONE),
    OPEN_GRINDSTONE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.GRINDSTONE),
    SHOOT_CROSSBOW(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.CROSSBOW),
    EAT_RABBIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.RABBIT),
    INTERACT_INSIDE_SWAMP_HUT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.SWAMP_HUT),
    EQUIP_CHAINMAIL_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.CHAINMAIL),
    OPEN_ANVIL(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ANVIL),
    USE_FLINT_AND_STEEL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.FLINT_AND_STEEL),
    IGNITE_TNT(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.TNT),
    INTERACT_INSIDE_ANCIENT_CITY(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.ANCIENT_CITY),
    ENTER_NETHER(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, NetherPortalBlock.class),
    TRADE_WITH_WANDERING_TRADER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, Items.WANDERING_TRADER_SPAWN_EGG),
    GET_INTO_MINECART(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.MINECART),
    INTERACT_INSIDE_FORTRESS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.FORTRESS),
    USE_OMINOUS_BOTTLE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.OMINOUS_BOTTLE),
    USE_IRON_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.IRON),
    INTERACT_INSIDE_JUNGLE_PYRAMID(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.JUNGLE_PYRAMID),
    ATTACK_WITH_TRIDENT(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.TRIDENT),
    EQUIP_IRON_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.IRON),
    SHOOT_BOW(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.BOW),
    INTERACT_INSIDE_BASTION_REMNANT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.BASTION_REMNANT),
    USE_JUKEBOX(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.JUKEBOX),
    THROW_ENDER_PEARL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.ENDER_PEARL),
    USE_COMPOSTER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.COMPOSTER),
    CHARGE_RESPAWN_ANCHOR(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.RESPAWN_ANCHOR),
    EAT_BEEF(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.BEEF),
    INTERACT_INSIDE_PILLAGER_OUTPOST(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.PILLAGER_OUTPOST),
    TRADE_WITH_MASON(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.MASON),
    USE_FISHING_ROD(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.FISHING_ROD),
    EAT_PORKCHOP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.PORKCHOP),
    TRADE_WITH_CARTOGRAPHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.CARTOGRAPHER),
    INTERACT_INSIDE_MONUMENT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MONUMENT),
    USE_DIAMOND_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.DIAMOND),
    USE_CAULDRON(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CAULDRON),
    INTERACT_INSIDE_MONSTER_ROOM(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MONSTER_ROOM),
    EAT_BAKED_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.BAKED_POTATO),
    OPEN_SMOKER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.SMOKER),
    EQUIP_TURTLE_HELMET(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.TURTLE_HELMET),
    INTERACT_INSIDE_TRAIL_RUINS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.TRAIL_RUINS),
    USE_BRUSH(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.BRUSH),
    EQUIP_DIAMOND_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.DIAMOND),
    UNLOCK_VAULT(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.VAULT),
    OPEN_BLAST_FURNACE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BLAST_FURNACE),
    INTERACT_INSIDE_STRONGHOLD(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.STRONGHOLD),
    TRADE_WITH_LEATHERWORKER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.LEATHERWORKER),
    USE_SPYGLASS(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SPYGLASS),
    INTERACT_INSIDE_MANSION(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MANSION),
    OPEN_BEACON(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BEACON),
    THROW_WIND_CHARGE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.WIND_CHARGE),
    ENTER_END(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, EndPortalBlock.class),
    OPEN_CARTOGRAPHY_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CARTOGRAPHY_TABLE),
    INTERACT_INSIDE_DESERT_WELL(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.DESERT_WELL),
    EAT_COOKED_SALMON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_SALMON),
    EQUIP_ELYTRA(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.ELYTRA),
    TRADE_WITH_SHEPHERD(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.SHEPHERD),
    TRADE_WITH_BUTCHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.BUTCHER),
    INTERACT_INSIDE_TRIAL_CHAMBERS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.TRIAL_CHAMBERS),
    OPEN_ENDER_CHEST(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ENDER_CHEST),
    ATTACK_WITH_MACE(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.MACE),
    INTERACT_INSIDE_END_CITY(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.END_CITY),
    USE_ENDER_EYE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.ENDER_EYE),
    TELEPORT_OUTER_ISLANDS(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, EndGatewayBlock.class),
    USE_NETHERITE_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.NETHERITE),
    EAT_COOKED_COD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_COD),
    TRADE_WITH_FARMER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FARMER),
    GLIDE_WITH_FIREWORKS(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.FIREWORK_ROCKET),
    TRADE_WITH_CLERIC(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.CLERIC),
    EQUIP_NETHERITE_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.NETHERITE),
    OPEN_BREWING_STAND(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BREWING_STAND),
    EAT_COOKED_RABBIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_RABBIT),
    PLACE_END_CRYSTAL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.END_CRYSTAL),
    TRADE_WITH_FISHERMAN(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FISHERMAN),
    OPEN_SMITHING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.SMITHING_TABLE),
    EAT_COOKED_CHICKEN(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_CHICKEN),
    EAT_CHORUS_FRUIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.CHORUS_FRUIT),
    TRADE_WITH_FLETCHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FLETCHER),
    EAT_COOKED_MUTTON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_MUTTON),
    TRADE_WITH_ARMORER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.ARMORER),
    EAT_COOKED_PORKCHOP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_PORKCHOP),
    EAT_BREAD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.BREAD),
    TRADE_WITH_WEAPONSMITH(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.WEAPONSMITH),
    EAT_COOKED_BEEF(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.COOKED_BEEF),
    OPEN_LOOM(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.LOOM),
    USE_CAMPFIRE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CAMPFIRE),
    OPEN_SHULKER_BOX(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, blockByPath("light_gray_shulker_box")),
    EAT_GOLDEN_CARROT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, Foods.GOLDEN_CARROT),
    TRADE_WITH_TOOLSMITH(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.TOOLSMITH),
    TRADE_WITH_LIBRARIAN(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.LIBRARIAN),
    OPEN_ENCHANTING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ENCHANTING_TABLE);

    private final AbilitiesHierarchyLayerType hierarchyLayerType;
    private final AbilityUnlockedToastType unlockToastType;
    private final Item item;
    private final Block block;
    private final FoodProperties foodComponent;
    private final ToolMaterial toolMaterial;
    private final ArmorMaterial equipmentMaterial;
    private final Class<? extends Portal> portal;
    private final ResourceKey<VillagerProfession> villager;
    private final LandmarkType landmarkType;

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Item item) {
        this(unlockToastType, hierarchyLayerType, item, null, null, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Block block) {
        this(unlockToastType, hierarchyLayerType, null, null, block, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, FoodProperties foodComponent) {
        this(unlockToastType, hierarchyLayerType, null, foodComponent, null, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, ToolMaterial toolMaterial) {
        this(unlockToastType, hierarchyLayerType, null, null, null, toolMaterial, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, ArmorMaterial armorMaterial) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, armorMaterial, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Class<? extends Portal> portal) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, portal, null, null);
    }

    AbilityType(
        AbilityUnlockedToastType unlockToastType,
        AbilitiesHierarchyLayerType hierarchyLayerType,
        ResourceKey<VillagerProfession> villager
    ) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, null, villager, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, LandmarkType landmarkType) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, null, null, landmarkType);
    }

    AbilityType(
        AbilityUnlockedToastType unlockToastType,
        AbilitiesHierarchyLayerType hierarchyLayerType,
        Item item,
        FoodProperties foodComponent,
        Block block,
        ToolMaterial toolMaterial,
        ArmorMaterial equipmentMaterial,
        Class<? extends Portal> portal,
        ResourceKey<VillagerProfession> villager,
        LandmarkType landmarkType
    ) {
        this.hierarchyLayerType = hierarchyLayerType;
        this.unlockToastType = unlockToastType;
        this.item = item;
        this.foodComponent = foodComponent;
        this.block = block;
        this.toolMaterial = toolMaterial;
        this.equipmentMaterial = equipmentMaterial;
        this.portal = portal;
        this.villager = villager;
        this.landmarkType = landmarkType;
    }

    public AbilitiesHierarchyLayerType getHierarchyLayerType() {
        return hierarchyLayerType;
    }

    public AbilityUnlockedToastType getUnlockToastType() {
        return unlockToastType;
    }

    public LandmarkType getLandmarkType() {
        return landmarkType;
    }

    public Component buildUnlockProgressMessage(int leftCount) {
        return buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.left_to_unlock", leftCount))
            .withStyle(DesignCodePalette.TEXT_COLOR);
    }

    public Component buildPermanentlyLockedMessage() {
        return buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.permanently_locked"))
            .withStyle(ChatFormatting.RED);
    }

    private MutableComponent buildLockedMessagePrefix() {
        return AchieveToDoClient.translate("ability." + getName() + ".locked_message")
            .append("." + (FabricLoader.getInstance().isModLoaded("multilineactionbar") ? "\n" : " "));
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @Nullable Item getIcon() {
        if (item != null) {
            return item;
        }
        if (block != null) {
            return block.asItem();
        }
        if (foodComponent != null) {
            return getFoodIcon(foodComponent);
        }
        if (toolMaterial != null) {
            return BuiltInRegistries.ITEM.stream()
                .filter(item -> false)
                .findFirst()
                .orElseGet(() -> getToolMaterialIcon(toolMaterial));
        }
        if (equipmentMaterial != null) {
            return getEquipmentMaterialIcon(equipmentMaterial);
        }
        if (portal != null) {
            return switch (this) {
                case ENTER_NETHER -> Items.OBSIDIAN;
                case ENTER_END -> Items.END_PORTAL_FRAME;
                case TELEPORT_OUTER_ISLANDS -> Items.CHORUS_FLOWER;
                default -> null;
            };
        }
        if (villager != null) {
            PoiType poi = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getValue(
                Identifier.withDefaultNamespace(villager.identifier().getPath())
            );
            if (poi != null && poi.matchingStates() != null) {
                List<BlockState> blockStates = new ArrayList<>(poi.matchingStates());
                BlockState blockState = blockStates.getFirst();
                if (blockState != null) {
                    return blockState.getBlock().asItem();
                }
            }
        }
        if (landmarkType != null) {
            return switch (landmarkType) {
                case DESERT_PYRAMID -> Items.CHISELED_SANDSTONE;
                case DESERT_WELL -> Items.SANDSTONE;
                case JUNGLE_PYRAMID -> Items.MOSSY_COBBLESTONE;
                case PILLAGER_OUTPOST -> Items.DARK_OAK_PLANKS;
                case IGLOO -> Items.SNOW_BLOCK;
                case SWAMP_HUT -> Items.CAULDRON;
                case MANSION -> Items.DARK_OAK_LOG;
                case VILLAGE -> Items.BELL;
                case RUINED_PORTAL -> Items.CRYING_OBSIDIAN;
                case BURIED_TREASURE -> Items.SAND;
                case SHIPWRECK -> Items.OAK_PLANKS;
                case OCEAN_RUIN -> Items.SEA_LANTERN;
                case MONUMENT -> Items.PRISMARINE;
                case MONSTER_ROOM -> Items.SPAWNER;
                case MINESHAFT -> Items.RAIL;
                case TRAIL_RUINS -> Items.SUSPICIOUS_GRAVEL;
                case ANCIENT_CITY -> Items.REINFORCED_DEEPSLATE;
                case TRIAL_CHAMBERS -> Items.TRIAL_SPAWNER;
                case STRONGHOLD -> Items.INFESTED_CRACKED_STONE_BRICKS;
                case FORTRESS -> Items.CHISELED_NETHER_BRICKS;
                case BASTION_REMNANT -> Items.GILDED_BLACKSTONE;
                case END_CITY -> Items.PURPUR_BLOCK;
            };
        }
        throw new IllegalStateException("Ability " + this + " haven't icon!");
    }

    public @NotNull Component getTitle() {
        return AchieveToDoClient.translate("ability." + getName() + ".name");
    }

    public @NotNull Component getDescription() {
        return AchieveToDoClient.translate("ability." + getName() + ".description");
    }

    public int getChaosPriority() {
        if (this == OPEN_CRAFTING_TABLE) {
            return 100;
        }
        if (this == INTERACT_INSIDE_FORTRESS ||
            this == INTERACT_INSIDE_STRONGHOLD ||
            this == USE_ENDER_EYE ||
            this == ENTER_NETHER ||
            this == ENTER_END
        ) {
            return 90;
        }
        if (this == OPEN_BEACON ||
            this == OPEN_ENDER_CHEST ||
            this == OPEN_SMITHING_TABLE ||
            this == OPEN_ANVIL ||
            this == OPEN_STONECUTTER
        ) {
            return 80;
        }
        if (this == USE_GOLDEN_TOOLS ||
            this == USE_WOODEN_TOOLS ||
            this == USE_STONE_TOOLS ||
            this == USE_IRON_TOOLS ||
            this == USE_SHIELD ||
            this == USE_WATER_BUCKET ||
            this == USE_FLINT_AND_STEEL
        ) {
            return 70;
        }
        if (this == EQUIP_LEATHER_ARMOR ||
            this == EQUIP_CHAINMAIL_ARMOR ||
            this == EQUIP_IRON_ARMOR ||
            this == EQUIP_ELYTRA
        ) {
            return 60;
        }
        if (this == THROW_ENDER_PEARL ||
            this == USE_OMINOUS_BOTTLE ||
            this == PLACE_END_CRYSTAL
        ) {
            return 50;
        }
        if (this == EAT_PUFFERFISH ||
            this == EAT_ROTTEN_FLESH ||
            this == EAT_SPIDER_EYE ||
            this == EAT_SUSPICIOUS_STEW ||
            this == EAT_POISONOUS_POTATO
        ) {
            return 40;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.UPGRADE ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.BLOCKS ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.ACTIONS
        ) {
            return 30;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.TRADING ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.LANDMARK ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.FOOD
        ) {
            return 0;
        }
        return 100;
    }

    public static @Nullable AbilityType findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (abilityType.name().equalsIgnoreCase(name)) {
                return abilityType;
            }
        }
        return null;
    }

    public static AbilityType findByAdvancement(@NotNull AdvancementNode advancement) {
        return findByAdvancement(advancement.holder());
    }

    public static AbilityType findByAdvancement(@NotNull AdvancementHolder advancement) {
        return findByAdvancement(advancement.id());
    }

    public static @Nullable AbilityType findByAdvancement(Identifier advancementId) {
        if (advancementId == null || !BuildConfig.MOD_ID.equals(advancementId.getNamespace())) {
            return null;
        }
        String path = advancementId.getPath();
        if (path.startsWith(AbilityAdvancementsGenerator.ABILITY_PATH_PREFIX)) {
            return findByName(path.split(AbilityAdvancementsGenerator.ABILITY_PATH_PREFIX)[1]);
        }
        return null;
    }

    public static @Nullable AbilityType findEatFoodAbility(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        FoodProperties foodComponent = stack.get(DataComponents.FOOD);
        if (foodComponent == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (foodComponent == abilityType.foodComponent) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findToolMaterialUsageAbility(ToolMaterial toolMaterial) {
        if (toolMaterial == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (toolMaterial == abilityType.toolMaterial) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findEquipmentEquipAbility(Item item) {
        if (item == null) {
            return null;
        }
        if (item == Items.TURTLE_HELMET) {
            return EQUIP_TURTLE_HELMET;
        }
        if (item == Items.ELYTRA) {
            return EQUIP_ELYTRA;
        }
        ArmorMaterial material = getArmorMaterial(item);
        if (material != null) {
            for (AbilityType abilityType : values()) {
                if (material == abilityType.equipmentMaterial) {
                    return abilityType;
                }
            }
        }
        return null;
    }

    public static @Nullable AbilityType findPortalTeleportAbility(Portal portal) {
        if (portal == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (portal.getClass() == abilityType.portal) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findTradeAbility(ResourceKey<VillagerProfession> profession) {
        if (profession == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (profession == abilityType.villager) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findByLandmarkType(LandmarkType landmarkType) {
        if (landmarkType == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (landmarkType == abilityType.landmarkType) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findToolMaterialUsageAbility(Item item) {
        if (item == null) {
            return null;
        }
        if (item == Items.WOODEN_SWORD ||
            item == Items.WOODEN_PICKAXE ||
            item == Items.WOODEN_AXE ||
            item == Items.WOODEN_SHOVEL ||
            item == Items.WOODEN_HOE
        ) {
            return USE_WOODEN_TOOLS;
        }
        if (item == Items.STONE_SWORD ||
            item == Items.STONE_PICKAXE ||
            item == Items.STONE_AXE ||
            item == Items.STONE_SHOVEL ||
            item == Items.STONE_HOE ||
            item == Items.COPPER_SWORD ||
            item == Items.COPPER_PICKAXE ||
            item == Items.COPPER_AXE ||
            item == Items.COPPER_SHOVEL ||
            item == Items.COPPER_HOE
        ) {
            return USE_STONE_TOOLS;
        }
        if (item == Items.GOLDEN_SWORD ||
            item == Items.GOLDEN_PICKAXE ||
            item == Items.GOLDEN_AXE ||
            item == Items.GOLDEN_SHOVEL ||
            item == Items.GOLDEN_HOE
        ) {
            return USE_GOLDEN_TOOLS;
        }
        if (item == Items.IRON_SWORD ||
            item == Items.IRON_PICKAXE ||
            item == Items.IRON_AXE ||
            item == Items.IRON_SHOVEL ||
            item == Items.IRON_HOE
        ) {
            return USE_IRON_TOOLS;
        }
        if (item == Items.DIAMOND_SWORD ||
            item == Items.DIAMOND_PICKAXE ||
            item == Items.DIAMOND_AXE ||
            item == Items.DIAMOND_SHOVEL ||
            item == Items.DIAMOND_HOE
        ) {
            return USE_DIAMOND_TOOLS;
        }
        if (item == Items.NETHERITE_SWORD ||
            item == Items.NETHERITE_PICKAXE ||
            item == Items.NETHERITE_AXE ||
            item == Items.NETHERITE_SHOVEL ||
            item == Items.NETHERITE_HOE
        ) {
            return USE_NETHERITE_TOOLS;
        }
        return null;
    }

    private static Item itemByPath(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }

    private static Block blockByPath(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path));
    }

    private static @Nullable Item getFoodIcon(@NotNull FoodProperties food) {
        if (food == Foods.SALMON) return Items.SALMON;
        if (food == Foods.COD) return Items.COD;
        if (food == Foods.TROPICAL_FISH) return Items.TROPICAL_FISH;
        if (food == Foods.ROTTEN_FLESH) return Items.ROTTEN_FLESH;
        if (food == Foods.SPIDER_EYE) return Items.SPIDER_EYE;
        if (food == Foods.SWEET_BERRIES) return Items.SWEET_BERRIES;
        if (food == Foods.GLOW_BERRIES) return Items.GLOW_BERRIES;
        if (food == Foods.POISONOUS_POTATO) return Items.POISONOUS_POTATO;
        if (food == Foods.SUSPICIOUS_STEW) return Items.SUSPICIOUS_STEW;
        if (food == Foods.CHICKEN) return Items.CHICKEN;
        if (food == Foods.CARROT) return Items.CARROT;
        if (food == Foods.BEETROOT) return Items.BEETROOT;
        if (food == Foods.DRIED_KELP) return Items.DRIED_KELP;
        if (food == Foods.POTATO) return Items.POTATO;
        if (food == Foods.APPLE) return Items.APPLE;
        if (food == Foods.MELON_SLICE) return Items.MELON_SLICE;
        if (food == Foods.COOKIE) return Items.COOKIE;
        if (food == Foods.MUSHROOM_STEW) return Items.MUSHROOM_STEW;
        if (food == Foods.BEETROOT_SOUP) return Items.BEETROOT_SOUP;
        if (food == Foods.RABBIT_STEW) return Items.RABBIT_STEW;
        if (food == Foods.HONEY_BOTTLE) return Items.HONEY_BOTTLE;
        if (food == Foods.MUTTON) return Items.MUTTON;
        if (food == Foods.PUFFERFISH) return Items.PUFFERFISH;
        if (food == Foods.PUMPKIN_PIE) return Items.PUMPKIN_PIE;
        if (food == Foods.GOLDEN_APPLE) return Items.GOLDEN_APPLE;
        if (food == Foods.ENCHANTED_GOLDEN_APPLE) return Items.ENCHANTED_GOLDEN_APPLE;
        if (food == Foods.RABBIT) return Items.RABBIT;
        if (food == Foods.BEEF) return Items.BEEF;
        if (food == Foods.PORKCHOP) return Items.PORKCHOP;
        if (food == Foods.BREAD) return Items.BREAD;
        if (food == Foods.CHORUS_FRUIT) return Items.CHORUS_FRUIT;
        if (food == Foods.BAKED_POTATO) return Items.BAKED_POTATO;
        if (food == Foods.GOLDEN_CARROT) return Items.GOLDEN_CARROT;
        if (food == Foods.COOKED_COD) return Items.COOKED_COD;
        if (food == Foods.COOKED_CHICKEN) return Items.COOKED_CHICKEN;
        if (food == Foods.COOKED_MUTTON) return Items.COOKED_MUTTON;
        if (food == Foods.COOKED_BEEF) return Items.COOKED_BEEF;
        if (food == Foods.COOKED_PORKCHOP) return Items.COOKED_PORKCHOP;
        if (food == Foods.COOKED_RABBIT) return Items.COOKED_RABBIT;
        if (food == Foods.COOKED_SALMON) return Items.COOKED_SALMON;
        throw new IllegalStateException("Unknown food icon mapping");
    }

    private static @Nullable Item getToolMaterialIcon(ToolMaterial toolMaterial) {
        if (toolMaterial == ToolMaterial.WOOD) {
            return Items.WOODEN_PICKAXE;
        }
        if (toolMaterial == ToolMaterial.STONE) {
            return Items.STONE_PICKAXE;
        }
        if (toolMaterial == ToolMaterial.GOLD) {
            return Items.GOLDEN_PICKAXE;
        }
        if (toolMaterial == ToolMaterial.IRON) {
            return Items.IRON_PICKAXE;
        }
        if (toolMaterial == ToolMaterial.DIAMOND) {
            return Items.DIAMOND_PICKAXE;
        }
        if (toolMaterial == ToolMaterial.NETHERITE) {
            return Items.NETHERITE_PICKAXE;
        }
        return null;
    }

    private static @Nullable Item getEquipmentMaterialIcon(ArmorMaterial equipmentMaterial) {
        if (equipmentMaterial == ArmorMaterials.LEATHER) {
            return Items.LEATHER_CHESTPLATE;
        }
        if (equipmentMaterial == ArmorMaterials.GOLD) {
            return Items.GOLDEN_CHESTPLATE;
        }
        if (equipmentMaterial == ArmorMaterials.CHAINMAIL) {
            return Items.CHAINMAIL_CHESTPLATE;
        }
        if (equipmentMaterial == ArmorMaterials.IRON) {
            return Items.IRON_CHESTPLATE;
        }
        if (equipmentMaterial == ArmorMaterials.DIAMOND) {
            return Items.DIAMOND_CHESTPLATE;
        }
        if (equipmentMaterial == ArmorMaterials.NETHERITE) {
            return Items.NETHERITE_CHESTPLATE;
        }
        return null;
    }

    private static @Nullable ArmorMaterial getArmorMaterial(Item item) {
        if (item == Items.LEATHER_HELMET ||
            item == Items.LEATHER_CHESTPLATE ||
            item == Items.LEATHER_LEGGINGS ||
            item == Items.LEATHER_BOOTS
        ) {
            return ArmorMaterials.LEATHER;
        }
        if (item == Items.GOLDEN_HELMET ||
            item == Items.GOLDEN_CHESTPLATE ||
            item == Items.GOLDEN_LEGGINGS ||
            item == Items.GOLDEN_BOOTS
        ) {
            return ArmorMaterials.GOLD;
        }
        if (item == Items.CHAINMAIL_HELMET ||
            item == Items.CHAINMAIL_CHESTPLATE ||
            item == Items.CHAINMAIL_LEGGINGS ||
            item == Items.CHAINMAIL_BOOTS
        ) {
            return ArmorMaterials.CHAINMAIL;
        }
        if (item == Items.IRON_HELMET ||
            item == Items.IRON_CHESTPLATE ||
            item == Items.IRON_LEGGINGS ||
            item == Items.IRON_BOOTS
        ) {
            return ArmorMaterials.IRON;
        }
        if (item == Items.DIAMOND_HELMET ||
            item == Items.DIAMOND_CHESTPLATE ||
            item == Items.DIAMOND_LEGGINGS ||
            item == Items.DIAMOND_BOOTS
        ) {
            return ArmorMaterials.DIAMOND;
        }
        if (item == Items.NETHERITE_HELMET ||
            item == Items.NETHERITE_CHESTPLATE ||
            item == Items.NETHERITE_LEGGINGS ||
            item == Items.NETHERITE_BOOTS
        ) {
            return ArmorMaterials.NETHERITE;
        }
        return null;
    }
}
