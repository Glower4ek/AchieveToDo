package com.diskree.achievetodo;

import com.diskree.achievetodo.ability.LandmarkType;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/** Byte continuity guards are static evidence, not live reward/reload certification. */
class PhaseDProtectedScopeTest {
    // D-S08-R1 binds the startup-index lifecycle repair; all unrelated historical hashes remain unchanged.
    private static final String PROTECTED = """
src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java 0aff4191700d951a5cc3139d4f90914f774495b29c0362d1bebc429177fd22ea
src/main/java/com/diskree/achievetodo/server/AdvancementsMode.java ee23cae49c2cddc49754aee0de71806ce14027501f2bdb8d40196c654b549d62
src/main/java/com/diskree/achievetodo/injection/mixin/main/PlayerAdvancementTrackerMixin.java 30723f44ef5bdca7b3bcab75b24a1dedb97a8f2ea4ecf1188979c0766c17a4ed
src/main/java/com/diskree/achievetodo/ability/LandmarkType.java d505695c0b007f1c336dad3798eb9211659fc02aafdd261f0ffcea45c86d3d8f
src/main/java/com/diskree/achievetodo/ability/ChaosProgressionGenerator.java 049ed211e1d90ef2c5ea673e7a6eddc683a2324f810fc6af2ad0911e79c42c8e
src/gametest/java/com/diskree/achievetodo/certification/ProgressionReconciliationGameTest.java 25ed819d8d43da216bf0c55b1035b7082a1b3bd7eadaeecbbe7d1a47d13a09b4
src/test/java/com/diskree/achievetodo/server/ObtainedCountReconciliationTest.java ea2974c8a1e4ad112a94cb212f9bbabe3a562b0acb85b7c709ca015237a1f016
        """;

    private static final String ORIGINAL_GAME_TESTS = """
com.diskree.achievetodo.certification.PhaseALocationMovementGameTest
com.diskree.achievetodo.certification.PhaseAStructureLocationGameTest
com.diskree.achievetodo.certification.PhaseAContainerLootGameTest
com.diskree.achievetodo.certification.PhaseAItemTagInventoryChangedGameTest
com.diskree.achievetodo.certification.PhaseAItemTagPlacedBlockGameTest
com.diskree.achievetodo.certification.PhaseAEntityTypeTagPlayerKilledEntityGameTest
com.diskree.achievetodo.certification.PhaseAEnchantmentInventoryChangedGameTest
com.diskree.achievetodo.certification.PhaseAEnchantmentInventoryChangedExpansionDirect16GameTest
com.diskree.achievetodo.certification.PhaseATrimPatternRecipeCraftedGameTest
com.diskree.achievetodo.certification.PhaseABlockTagItemUsedOnBlockGameTest
com.diskree.achievetodo.certification.PhaseAItemTagItemUsedOnBlockGameTest
com.diskree.achievetodo.certification.PhaseAEntityTypeTagStartedRidingGameTest
com.diskree.achievetodo.certification.PhaseAItemTagPlayerInteractedWithEntityGameTest
com.diskree.achievetodo.certification.PhaseAEntityTypeTagPlayerInteractedWithEntityGameTest
com.diskree.achievetodo.certification.PhaseAShotCrossbowGameTest
com.diskree.achievetodo.certification.PhaseAPlayerHurtEntityDamageSourceGameTest
com.diskree.achievetodo.certification.PhaseAUsingItemGameTest
com.diskree.achievetodo.certification.PhaseATameAnimalLlamaGameTest
com.diskree.achievetodo.certification.PhaseARecipeCraftedWaxOnGameTest
com.diskree.achievetodo.certification.PhaseAItemTagInventoryContainmentGameTest
com.diskree.achievetodo.certification.PhaseAFishingRodHookedGameTest
com.diskree.achievetodo.certification.PhaseATrimMaterialInventoryChangedGameTest
com.diskree.achievetodo.certification.PhaseAItemUsedOnBlockContextualGameTest
com.diskree.achievetodo.certification.PhaseASingletonRedstoneClickGameTest
com.diskree.achievetodo.certification.PhaseASingletonMangroveGroveGameTest
com.diskree.achievetodo.certification.PhaseASingletonDiagonAllayGameTest
com.diskree.achievetodo.certification.PhaseASingletonVillageBellGameTest
com.diskree.achievetodo.certification.PhaseASingletonLlamaBreedingGameTest
com.diskree.achievetodo.certification.PhaseASingletonAxolotlMonumentGameTest
com.diskree.achievetodo.certification.PhaseASingletonMaximumResistanceGameTest
com.diskree.achievetodo.certification.PhaseASingletonMiracleDrinkGameTest
com.diskree.achievetodo.certification.PhaseASingletonAncientRestorationGameTest
com.diskree.achievetodo.certification.PhaseASingletonSilkTouchNestGameTest
com.diskree.achievetodo.certification.PhaseAPureEnchantmentInventoryChangedGameTest
com.diskree.achievetodo.certification.PhaseAMixedPerfectRunGameTest
com.diskree.achievetodo.certification.PhaseAStackAllItemsInventoryBacklogGameTest
com.diskree.achievetodo.certification.PhaseAMixedWeaponryMulticlassedGameTest
com.diskree.achievetodo.certification.PhaseAMixedPiglinDistractionGameTest
com.diskree.achievetodo.certification.PhaseALocationHolderSetWorldgenGameTest
com.diskree.achievetodo.certification.PhaseAPlayerKilledEntityRemainderGameTest
com.diskree.achievetodo.certification.PhaseBRuntimeSmoke
com.diskree.achievetodo.certification.ProgressionReconciliationGameTest
com.diskree.achievetodo.certification.PhaseDAbilityRoutesGameTest
com.diskree.achievetodo.certification.PhaseDEquipmentGameTest
com.diskree.achievetodo.certification.PhaseDCopperTransitionsGameTest
com.diskree.achievetodo.certification.PhaseDContextRoutesGameTest
        """;

    @BeforeAll
    static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void phaseCRewardCountingNonrepairAndPriorTestsStayByteIdentical() throws Exception {
        for (String row : PROTECTED.lines().map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            String[] columns = row.split(" ");
            assertEquals(columns[1], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(Path.of(columns[0])))), columns[0]);
        }
    }

    @Test
    void originalLandmarkSelectorsStayAtGenerationOnlyBaseline() {
        assertEquals(22, LandmarkType.values().length);
        assertEquals(33, java.util.Arrays.stream(LandmarkType.values()).filter(LandmarkType::isStructure)
            .mapToInt(t -> t.getStructureRegistryKeys().size()).sum());
        assertEquals(2, LandmarkType.FEATURES.size());
        assertEquals(7, LandmarkType.RUINED_PORTAL.getStructureRegistryKeys().size());
        assertSame(LandmarkType.RUINED_PORTAL,
            LandmarkType.findByStructureRegistryKey(BuiltinStructures.RUINED_PORTAL_STANDARD));
        // D-S07 appends the native Nether key only; LEGACY-01/A permits no automatic reconstruction.
        assertSame(LandmarkType.RUINED_PORTAL,
            LandmarkType.findByStructureRegistryKey(BuiltinStructures.RUINED_PORTAL_NETHER));
    }

    @Test
    void phaseDRegistrationAppendsFourAndRetainsOriginalEntrypointsAndMixins() throws Exception {
        var metadata = JsonParser.parseString(Files.readString(Path.of("src/gametest/resources/fabric.mod.json")))
            .getAsJsonObject();
        var entries = metadata.getAsJsonObject("entrypoints").getAsJsonArray("fabric-gametest");
        var actual = new java.util.ArrayList<String>();
        entries.forEach(e -> actual.add(e.getAsString()));
        var expected = ORIGINAL_GAME_TESTS.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        assertEquals(47, actual.size());
        assertEquals(actual.size(), new java.util.HashSet<>(actual).size(), "Duplicate registration");
        var appended = new java.util.ArrayList<>(expected);
        appended.add("com.diskree.achievetodo.certification.PhaseDCombatRoutesGameTest");
        assertEquals(appended, actual, "Only the authorized combat class may append");
        assertEquals(1, metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").size());
        assertEquals("com.diskree.achievetodo.certification.PhaseAGameTestBootstrap",
            metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").get(0).getAsString());
        assertEquals(2, metadata.getAsJsonArray("mixins").size());
        assertEquals("achievetodo-test.mixins.json", metadata.getAsJsonArray("mixins").get(0).getAsString());
        assertEquals("achievetodo-b10.mixins.json", metadata.getAsJsonArray("mixins").get(1).getAsString());
    }
}
