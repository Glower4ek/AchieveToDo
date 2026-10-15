package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Independent P1 fixture, not an oracle derived from the tested enum. */
public final class PhaseDAbilityMappingTest {
    // P1 ZIP 7113500765610899cf2ada45f6667dea02776e7e91ad2cc2f0d44e4118fc298e
    // pe15_enum_network_identity.json SHA256 d99f48546321cf760ab196de9fd86767b96ae4dd84bb63987c18f62ae9c6c6e4; /identities/0..150.
    private static final String OLD_IDENTITIES = """
VISION vision achievetodo:abilities/vision ACTION MAIN
EAT_SALMON eat_salmon achievetodo:abilities/eat_salmon FOOD FOOD
EAT_COD eat_cod achievetodo:abilities/eat_cod FOOD FOOD
EAT_TROPICAL_FISH eat_tropical_fish achievetodo:abilities/eat_tropical_fish FOOD FOOD
JUMP jump achievetodo:abilities/jump ACTION MAIN
SWIM swim achievetodo:abilities/swim ACTION MAIN
EAT_ROTTEN_FLESH eat_rotten_flesh achievetodo:abilities/eat_rotten_flesh FOOD FOOD
EAT_SPIDER_EYE eat_spider_eye achievetodo:abilities/eat_spider_eye FOOD FOOD
EAT_SWEET_BERRIES eat_sweet_berries achievetodo:abilities/eat_sweet_berries FOOD FOOD
OPEN_DOOR open_door achievetodo:abilities/open_door ACTION ACTIONS
EAT_GLOW_BERRIES eat_glow_berries achievetodo:abilities/eat_glow_berries FOOD FOOD
INTERACT_INSIDE_VILLAGE interact_inside_village achievetodo:abilities/interact_inside_village LANDMARK LANDMARK
SLEEP sleep achievetodo:abilities/sleep ACTION MAIN
EAT_POISONOUS_POTATO eat_poisonous_potato achievetodo:abilities/eat_poisonous_potato FOOD FOOD
SNEAK sneak achievetodo:abilities/sneak ACTION MAIN
SPRINT sprint achievetodo:abilities/sprint ACTION MAIN
OPEN_INVENTORY open_inventory achievetodo:abilities/open_inventory ACTION MAIN
BREAK_BLOCKS break_blocks achievetodo:abilities/break_blocks ACTION MAIN
EAT_SUSPICIOUS_STEW eat_suspicious_stew achievetodo:abilities/eat_suspicious_stew FOOD FOOD
USE_GOLDEN_TOOLS use_golden_tools achievetodo:abilities/use_golden_tools TOOL UPGRADE
OPEN_CHEST open_chest achievetodo:abilities/open_chest BLOCK BLOCKS
EAT_CHICKEN eat_chicken achievetodo:abilities/eat_chicken FOOD FOOD
EAT_CARROT eat_carrot achievetodo:abilities/eat_carrot FOOD FOOD
OPEN_CRAFTING_TABLE open_crafting_table achievetodo:abilities/open_crafting_table BLOCK BLOCKS
EQUIP_GOLDEN_ARMOR equip_golden_armor achievetodo:abilities/equip_golden_armor EQUIPMENT UPGRADE
OPEN_BARREL open_barrel achievetodo:abilities/open_barrel BLOCK BLOCKS
EAT_BEETROOT eat_beetroot achievetodo:abilities/eat_beetroot FOOD FOOD
EAT_DRIED_KELP eat_dried_kelp achievetodo:abilities/eat_dried_kelp FOOD FOOD
EAT_POTATO eat_potato achievetodo:abilities/eat_potato FOOD FOOD
OPEN_STONECUTTER open_stonecutter achievetodo:abilities/open_stonecutter BLOCK BLOCKS
GET_INTO_BOAT get_into_boat achievetodo:abilities/get_into_boat ITEM MAIN
EAT_APPLE eat_apple achievetodo:abilities/eat_apple FOOD FOOD
INTERACT_INSIDE_SHIPWRECK interact_inside_shipwreck achievetodo:abilities/interact_inside_shipwreck LANDMARK LANDMARK
USE_SHIELD use_shield achievetodo:abilities/use_shield EQUIPMENT UPGRADE
EAT_MELON_SLICE eat_melon_slice achievetodo:abilities/eat_melon_slice FOOD FOOD
INTERACT_INSIDE_RUINED_PORTAL interact_inside_ruined_portal achievetodo:abilities/interact_inside_ruined_portal LANDMARK LANDMARK
OPEN_FURNACE open_furnace achievetodo:abilities/open_furnace BLOCK BLOCKS
EAT_COOKIE eat_cookie achievetodo:abilities/eat_cookie FOOD FOOD
INTERACT_INSIDE_BURIED_TREASURE interact_inside_buried_treasure achievetodo:abilities/interact_inside_buried_treasure LANDMARK LANDMARK
EAT_MUSHROOM_STEW eat_mushroom_stew achievetodo:abilities/eat_mushroom_stew FOOD FOOD
EAT_BEETROOT_SOUP eat_beetroot_soup achievetodo:abilities/eat_beetroot_soup FOOD FOOD
PUT_IN_BUNDLE put_in_bundle achievetodo:abilities/put_in_bundle ITEM MAIN
EAT_RABBIT_STEW eat_rabbit_stew achievetodo:abilities/eat_rabbit_stew FOOD FOOD
OPEN_TRAPDOOR open_trapdoor achievetodo:abilities/open_trapdoor ACTION ACTIONS
USE_WOODEN_TOOLS use_wooden_tools achievetodo:abilities/use_wooden_tools TOOL UPGRADE
EAT_HONEY eat_honey achievetodo:abilities/eat_honey FOOD FOOD
INTERACT_INSIDE_IGLOO interact_inside_igloo achievetodo:abilities/interact_inside_igloo LANDMARK LANDMARK
USE_WATER_BUCKET use_water_bucket achievetodo:abilities/use_water_bucket ACTION ACTIONS
EAT_MUTTON eat_mutton achievetodo:abilities/eat_mutton FOOD FOOD
THROW_SNOWBALL throw_snowball achievetodo:abilities/throw_snowball ITEM ACTIONS
INTERACT_INSIDE_OCEAN_RUIN interact_inside_ocean_ruin achievetodo:abilities/interact_inside_ocean_ruin LANDMARK LANDMARK
EQUIP_LEATHER_ARMOR equip_leather_armor achievetodo:abilities/equip_leather_armor EQUIPMENT UPGRADE
EAT_PUFFERFISH eat_pufferfish achievetodo:abilities/eat_pufferfish FOOD FOOD
OPEN_FENCE_GATE open_fence_gate achievetodo:abilities/open_fence_gate ACTION ACTIONS
EAT_PUMPKIN_PIE eat_pumpkin_pie achievetodo:abilities/eat_pumpkin_pie FOOD FOOD
EAT_GOLDEN_APPLE eat_golden_apple achievetodo:abilities/eat_golden_apple FOOD FOOD
USE_SHEARS use_shears achievetodo:abilities/use_shears ITEM ACTIONS
INTERACT_INSIDE_DESERT_PYRAMID interact_inside_desert_pyramid achievetodo:abilities/interact_inside_desert_pyramid LANDMARK LANDMARK
BREAK_BLOCKS_IN_NEGATIVE_Y break_blocks_in_negative_y achievetodo:abilities/break_blocks_in_negative_y ACTION MAIN
EAT_ENCHANTED_GOLDEN_APPLE eat_enchanted_golden_apple achievetodo:abilities/eat_enchanted_golden_apple FOOD FOOD
INTERACT_INSIDE_MINESHAFT interact_inside_mineshaft achievetodo:abilities/interact_inside_mineshaft LANDMARK LANDMARK
THROW_EGG throw_egg achievetodo:abilities/throw_egg ITEM ACTIONS
USE_STONE_TOOLS use_stone_tools achievetodo:abilities/use_stone_tools TOOL UPGRADE
OPEN_GRINDSTONE open_grindstone achievetodo:abilities/open_grindstone BLOCK BLOCKS
SHOOT_CROSSBOW shoot_crossbow achievetodo:abilities/shoot_crossbow WEAPON UPGRADE
EAT_RABBIT eat_rabbit achievetodo:abilities/eat_rabbit FOOD FOOD
INTERACT_INSIDE_SWAMP_HUT interact_inside_swamp_hut achievetodo:abilities/interact_inside_swamp_hut LANDMARK LANDMARK
EQUIP_CHAINMAIL_ARMOR equip_chainmail_armor achievetodo:abilities/equip_chainmail_armor EQUIPMENT UPGRADE
OPEN_ANVIL open_anvil achievetodo:abilities/open_anvil BLOCK BLOCKS
USE_FLINT_AND_STEEL use_flint_and_steel achievetodo:abilities/use_flint_and_steel ITEM ACTIONS
IGNITE_TNT ignite_tnt achievetodo:abilities/ignite_tnt BLOCK BLOCKS
INTERACT_INSIDE_ANCIENT_CITY interact_inside_ancient_city achievetodo:abilities/interact_inside_ancient_city LANDMARK LANDMARK
ENTER_NETHER enter_nether achievetodo:abilities/enter_nether PORTAL UPGRADE
TRADE_WITH_WANDERING_TRADER trade_with_wandering_trader achievetodo:abilities/trade_with_wandering_trader TRADING TRADING
GET_INTO_MINECART get_into_minecart achievetodo:abilities/get_into_minecart ITEM ACTIONS
INTERACT_INSIDE_FORTRESS interact_inside_fortress achievetodo:abilities/interact_inside_fortress LANDMARK LANDMARK
USE_OMINOUS_BOTTLE use_ominous_bottle achievetodo:abilities/use_ominous_bottle ITEM ACTIONS
USE_IRON_TOOLS use_iron_tools achievetodo:abilities/use_iron_tools TOOL UPGRADE
INTERACT_INSIDE_JUNGLE_PYRAMID interact_inside_jungle_pyramid achievetodo:abilities/interact_inside_jungle_pyramid LANDMARK LANDMARK
ATTACK_WITH_TRIDENT attack_with_trident achievetodo:abilities/attack_with_trident WEAPON UPGRADE
EQUIP_IRON_ARMOR equip_iron_armor achievetodo:abilities/equip_iron_armor EQUIPMENT UPGRADE
SHOOT_BOW shoot_bow achievetodo:abilities/shoot_bow WEAPON UPGRADE
INTERACT_INSIDE_BASTION_REMNANT interact_inside_bastion_remnant achievetodo:abilities/interact_inside_bastion_remnant LANDMARK LANDMARK
USE_JUKEBOX use_jukebox achievetodo:abilities/use_jukebox BLOCK BLOCKS
THROW_ENDER_PEARL throw_ender_pearl achievetodo:abilities/throw_ender_pearl ITEM ACTIONS
USE_COMPOSTER use_composter achievetodo:abilities/use_composter BLOCK BLOCKS
CHARGE_RESPAWN_ANCHOR charge_respawn_anchor achievetodo:abilities/charge_respawn_anchor BLOCK BLOCKS
EAT_BEEF eat_beef achievetodo:abilities/eat_beef FOOD FOOD
INTERACT_INSIDE_PILLAGER_OUTPOST interact_inside_pillager_outpost achievetodo:abilities/interact_inside_pillager_outpost LANDMARK LANDMARK
TRADE_WITH_MASON trade_with_mason achievetodo:abilities/trade_with_mason TRADING TRADING
USE_FISHING_ROD use_fishing_rod achievetodo:abilities/use_fishing_rod ITEM ACTIONS
EAT_PORKCHOP eat_porkchop achievetodo:abilities/eat_porkchop FOOD FOOD
TRADE_WITH_CARTOGRAPHER trade_with_cartographer achievetodo:abilities/trade_with_cartographer TRADING TRADING
INTERACT_INSIDE_MONUMENT interact_inside_monument achievetodo:abilities/interact_inside_monument LANDMARK LANDMARK
USE_DIAMOND_TOOLS use_diamond_tools achievetodo:abilities/use_diamond_tools TOOL UPGRADE
USE_CAULDRON use_cauldron achievetodo:abilities/use_cauldron BLOCK BLOCKS
INTERACT_INSIDE_MONSTER_ROOM interact_inside_monster_room achievetodo:abilities/interact_inside_monster_room LANDMARK LANDMARK
EAT_BAKED_POTATO eat_baked_potato achievetodo:abilities/eat_baked_potato FOOD FOOD
OPEN_SMOKER open_smoker achievetodo:abilities/open_smoker BLOCK BLOCKS
EQUIP_TURTLE_HELMET equip_turtle_helmet achievetodo:abilities/equip_turtle_helmet EQUIPMENT UPGRADE
INTERACT_INSIDE_TRAIL_RUINS interact_inside_trail_ruins achievetodo:abilities/interact_inside_trail_ruins LANDMARK LANDMARK
USE_BRUSH use_brush achievetodo:abilities/use_brush ITEM ACTIONS
EQUIP_DIAMOND_ARMOR equip_diamond_armor achievetodo:abilities/equip_diamond_armor EQUIPMENT UPGRADE
UNLOCK_VAULT unlock_vault achievetodo:abilities/unlock_vault BLOCK BLOCKS
OPEN_BLAST_FURNACE open_blast_furnace achievetodo:abilities/open_blast_furnace BLOCK BLOCKS
INTERACT_INSIDE_STRONGHOLD interact_inside_stronghold achievetodo:abilities/interact_inside_stronghold LANDMARK LANDMARK
TRADE_WITH_LEATHERWORKER trade_with_leatherworker achievetodo:abilities/trade_with_leatherworker TRADING TRADING
USE_SPYGLASS use_spyglass achievetodo:abilities/use_spyglass ITEM ACTIONS
INTERACT_INSIDE_MANSION interact_inside_mansion achievetodo:abilities/interact_inside_mansion LANDMARK LANDMARK
OPEN_BEACON open_beacon achievetodo:abilities/open_beacon BLOCK BLOCKS
THROW_WIND_CHARGE throw_wind_charge achievetodo:abilities/throw_wind_charge ITEM ACTIONS
ENTER_END enter_end achievetodo:abilities/enter_end PORTAL UPGRADE
OPEN_CARTOGRAPHY_TABLE open_cartography_table achievetodo:abilities/open_cartography_table BLOCK BLOCKS
INTERACT_INSIDE_DESERT_WELL interact_inside_desert_well achievetodo:abilities/interact_inside_desert_well LANDMARK LANDMARK
EAT_COOKED_SALMON eat_cooked_salmon achievetodo:abilities/eat_cooked_salmon FOOD FOOD
EQUIP_ELYTRA equip_elytra achievetodo:abilities/equip_elytra EQUIPMENT UPGRADE
TRADE_WITH_SHEPHERD trade_with_shepherd achievetodo:abilities/trade_with_shepherd TRADING TRADING
TRADE_WITH_BUTCHER trade_with_butcher achievetodo:abilities/trade_with_butcher TRADING TRADING
INTERACT_INSIDE_TRIAL_CHAMBERS interact_inside_trial_chambers achievetodo:abilities/interact_inside_trial_chambers LANDMARK LANDMARK
OPEN_ENDER_CHEST open_ender_chest achievetodo:abilities/open_ender_chest BLOCK BLOCKS
ATTACK_WITH_MACE attack_with_mace achievetodo:abilities/attack_with_mace WEAPON UPGRADE
INTERACT_INSIDE_END_CITY interact_inside_end_city achievetodo:abilities/interact_inside_end_city LANDMARK LANDMARK
USE_ENDER_EYE use_ender_eye achievetodo:abilities/use_ender_eye ITEM ACTIONS
TELEPORT_OUTER_ISLANDS teleport_outer_islands achievetodo:abilities/teleport_outer_islands PORTAL UPGRADE
USE_NETHERITE_TOOLS use_netherite_tools achievetodo:abilities/use_netherite_tools TOOL UPGRADE
EAT_COOKED_COD eat_cooked_cod achievetodo:abilities/eat_cooked_cod FOOD FOOD
TRADE_WITH_FARMER trade_with_farmer achievetodo:abilities/trade_with_farmer TRADING TRADING
GLIDE_WITH_FIREWORKS glide_with_fireworks achievetodo:abilities/glide_with_fireworks ACTION ACTIONS
TRADE_WITH_CLERIC trade_with_cleric achievetodo:abilities/trade_with_cleric TRADING TRADING
EQUIP_NETHERITE_ARMOR equip_netherite_armor achievetodo:abilities/equip_netherite_armor EQUIPMENT UPGRADE
OPEN_BREWING_STAND open_brewing_stand achievetodo:abilities/open_brewing_stand BLOCK BLOCKS
EAT_COOKED_RABBIT eat_cooked_rabbit achievetodo:abilities/eat_cooked_rabbit FOOD FOOD
PLACE_END_CRYSTAL place_end_crystal achievetodo:abilities/place_end_crystal ITEM ACTIONS
TRADE_WITH_FISHERMAN trade_with_fisherman achievetodo:abilities/trade_with_fisherman TRADING TRADING
OPEN_SMITHING_TABLE open_smithing_table achievetodo:abilities/open_smithing_table BLOCK BLOCKS
EAT_COOKED_CHICKEN eat_cooked_chicken achievetodo:abilities/eat_cooked_chicken FOOD FOOD
EAT_CHORUS_FRUIT eat_chorus_fruit achievetodo:abilities/eat_chorus_fruit FOOD FOOD
TRADE_WITH_FLETCHER trade_with_fletcher achievetodo:abilities/trade_with_fletcher TRADING TRADING
EAT_COOKED_MUTTON eat_cooked_mutton achievetodo:abilities/eat_cooked_mutton FOOD FOOD
TRADE_WITH_ARMORER trade_with_armorer achievetodo:abilities/trade_with_armorer TRADING TRADING
EAT_COOKED_PORKCHOP eat_cooked_porkchop achievetodo:abilities/eat_cooked_porkchop FOOD FOOD
EAT_BREAD eat_bread achievetodo:abilities/eat_bread FOOD FOOD
TRADE_WITH_WEAPONSMITH trade_with_weaponsmith achievetodo:abilities/trade_with_weaponsmith TRADING TRADING
EAT_COOKED_BEEF eat_cooked_beef achievetodo:abilities/eat_cooked_beef FOOD FOOD
OPEN_LOOM open_loom achievetodo:abilities/open_loom BLOCK BLOCKS
USE_CAMPFIRE use_campfire achievetodo:abilities/use_campfire BLOCK BLOCKS
OPEN_SHULKER_BOX open_shulker_box achievetodo:abilities/open_shulker_box BLOCK BLOCKS
EAT_GOLDEN_CARROT eat_golden_carrot achievetodo:abilities/eat_golden_carrot FOOD FOOD
TRADE_WITH_TOOLSMITH trade_with_toolsmith achievetodo:abilities/trade_with_toolsmith TRADING TRADING
TRADE_WITH_LIBRARIAN trade_with_librarian achievetodo:abilities/trade_with_librarian TRADING TRADING
OPEN_ENCHANTING_TABLE open_enchanting_table achievetodo:abilities/open_enchanting_table BLOCK BLOCKS
        """;

    public enum Level { UNIT, GAMETEST, RUNTIME }
    public enum Outcome {
        NATIVE_ALLOWED_ATD_ALLOWED, NATIVE_ALLOWED_ATD_DENIED, NATIVE_DENIED,
        AUTONOMOUS_OR_ADMIN, CONDITIONAL_ROUTE, RUNTIME_REQUIRED, PRESERVATION_CONTROL
    }

    /** Immutable expected contract. Execution is recorded separately; construction is never PASS. */
    public record CaseContract(String caseId, List<String> sourcePE, List<String> sourceCH,
            String originalBlockedEntryReference, String ownerAuthority, String nativeRouteReference,
            String initiatingActor, String nativeEligibility, String applicableAbility, Outcome outcome,
            Map<String, String> expectedObservations, List<String> persistenceExpectations,
            String requiredStage, Level level, String activationDependency, String initialExecutionStatus,
            String evidenceReference) {
        public CaseContract {
            for (String value : List.of(caseId, originalBlockedEntryReference, ownerAuthority,
                    nativeRouteReference, initiatingActor, nativeEligibility, applicableAbility,
                    requiredStage, activationDependency, initialExecutionStatus, evidenceReference)) {
                if (value.isBlank()) throw new IllegalArgumentException("Blank contract field");
            }
            sourcePE = List.copyOf(sourcePE);
            sourceCH = List.copyOf(sourceCH);
            persistenceExpectations = List.copyOf(persistenceExpectations);
            expectedObservations = Map.copyOf(expectedObservations);
            if (sourceCH.isEmpty() || expectedObservations.isEmpty() || outcome == null || level == null
                    || !initialExecutionStatus.equals("NOT_RUN")) {
                throw new IllegalArgumentException("Incomplete contract or fabricated execution");
            }
            expectedObservations.forEach((key, value) -> {
                if (key.isBlank() || value.isBlank()) throw new IllegalArgumentException("Blank observation");
            });
        }
    }

    @BeforeAll
    static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    public static void verifyOldIdentities() {
        List<String> rows = OLD_IDENTITIES.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        assertEquals(151, rows.size());
        assertTrue(AbilityType.values().length >= 151);
        for (int i = 0; i < rows.size(); i++) {
            String[] columns = rows.get(i).split(" ");
            AbilityType actual = AbilityType.values()[i];
            assertEquals(columns[0], actual.name(), "ordinal " + i);
            assertEquals(columns[1], actual.getName(), columns[0]);
            assertEquals(columns[2], AbilityAdvancementsGenerator.buildAdvancementId(actual).toString());
            assertEquals(columns[3], actual.getUnlockToastType().name());
            assertEquals(columns[4], actual.getHierarchyLayerType().name());
            assertSame(actual, AbilityType.findByName(columns[1]));
        }
    }

    @Test
    void original151OrdinalsNamesIdsAndCategories() { verifyOldIdentities(); }

    @Test
    void oldSixToolMaterialsKeepTheirSelectors() {
        assertSame(AbilityType.USE_WOODEN_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.WOOD));
        assertSame(AbilityType.USE_STONE_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.STONE));
        assertSame(AbilityType.USE_GOLDEN_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.GOLD));
        assertSame(AbilityType.USE_IRON_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.IRON));
        assertSame(AbilityType.USE_DIAMOND_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.DIAMOND));
        assertSame(AbilityType.USE_NETHERITE_TOOLS, AbilityType.findToolMaterialUsageAbility(ToolMaterial.NETHERITE));
        assertSame(AbilityType.USE_IRON_TOOLS, AbilityType.findToolMaterialUsageAbility(Items.IRON_AXE));
        assertSame(AbilityType.USE_DIAMOND_TOOLS, AbilityType.findToolMaterialUsageAbility(Items.DIAMOND_SWORD));
    }

    @Test
    void nullAndUnrelatedItemsCreateNoMapping() {
        assertNull(AbilityType.findToolMaterialUsageAbility((Item) null));
        assertNull(AbilityType.findToolMaterialUsageAbility((ToolMaterial) null));
        assertNull(AbilityType.findToolMaterialUsageAbility(Items.HONEYCOMB));
        assertNull(AbilityType.findEquipmentEquipAbility(Items.SADDLE));
        assertNull(AbilityType.findEquipmentEquipAbility(Items.WOLF_ARMOR));
        assertNull(AbilityType.findEquipmentEquipAbility(null));
    }

    @Test
    void pendingContractCannotClaimExecutedSuccess() {
        assertThrows(IllegalArgumentException.class, () -> new CaseContract("fixture", List.of(),
            List.of("CH-20"), "R1 U-01", "owner D-S00 only", "N/A pure contract", "N/A",
            "N/A", "N/A", Outcome.PRESERVATION_CONTROL, Map.of("result", "expected"),
            List.of(), "D-S01", Level.UNIT, "D-S01 implementation/controller acceptance", "PASS", "NOT_RUN"));
    }
}
