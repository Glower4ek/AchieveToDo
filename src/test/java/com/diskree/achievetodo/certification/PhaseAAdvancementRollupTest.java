package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAAdvancementRollupTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String SOURCE = "PhaseAStructureLocationGameTest";
    private static final String CONTAINER_SOURCE = "PhaseAContainerLootGameTest";
    private static final String ITEM_TAG_SOURCE = PhaseAItemTagInventoryChangedExecutionEvidenceValidation.SOURCE;
    private static final String ITEM_TAG_FAMILY = PhaseAItemTagInventoryChangedExecutionEvidenceValidation.FAMILY;
    private static final String ENCHANTMENT_SOURCE = PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.SOURCE;
    private static final String ENCHANTMENT_FAMILY = PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.FAMILY;
    private static final String EXPANSION_DIRECT16_SOURCE =
        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.SOURCE;
    private static final String EXPANSION_DIRECT16_FAMILY =
        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.FAMILY;
    private static final String PLACED_BLOCK_SOURCE = PhaseAItemTagPlacedBlockExecutionEvidenceValidation.SOURCE;
    private static final String PLACED_BLOCK_FAMILY = PhaseAItemTagPlacedBlockExecutionEvidenceValidation.FAMILY;
    private static final String TRIM_PATTERN_SOURCE = PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.SOURCE;
    private static final String TRIM_PATTERN_FAMILY = PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.FAMILY;
    private static final String SHOT_CROSSBOW_SOURCE = PhaseAShotCrossbowExecutionEvidenceValidation.SOURCE;
    private static final String SHOT_CROSSBOW_FAMILY = PhaseAShotCrossbowExecutionEvidenceValidation.FAMILY;
    private static final String TRIM_PATTERN_TARGET = "minecraft:adventure/trim_with_any_armor_pattern";
    private static final Set<String> EXPANSION_DIRECT16_ADVANCEMENTS = Set.of(
        "blazeandcave:enchanting/armor_for_the_masses",
        "blazeandcave:enchanting/bane_of_one_shotting_spiders",
        "blazeandcave:enchanting/bow_down_to_me",
        "blazeandcave:enchanting/curses",
        "blazeandcave:enchanting/fiery",
        "blazeandcave:enchanting/fortunate_son",
        "blazeandcave:enchanting/gotta_go_fast",
        "blazeandcave:enchanting/knocking_your_socks_off",
        "blazeandcave:enchanting/master_arbalist",
        "blazeandcave:enchanting/master_macerator",
        "blazeandcave:enchanting/master_sniper",
        "blazeandcave:enchanting/master_tridenteer",
        "blazeandcave:enchanting/needle_sharp",
        "blazeandcave:enchanting/scuba_gear",
        "blazeandcave:enchanting/super_efficient",
        "blazeandcave:enchanting/undead_slayer"
    );

    @TempDir
    Path tempDir;

    @Test
    void advancementRollupSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAAdvancementRollup.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "Phase A advancement roll-up snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "Phase A advancement roll-up snapshot is stale. Run PhaseAAdvancementRollup."
        );
    }

    @Test
    void advancementRollupMaintainsCanonicalInvariants() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonObject statusTotals = summary.getAsJsonObject("statusTotals");
        JsonArray entries = root.getAsJsonArray("entries");

        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(1152, summary.get("totalAdvancements").getAsInt());
        assertEquals(1152, entries.size());

        Set<String> ids = new HashSet<>();
        int missingReasons = 0;
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            String id = entry.get("id").getAsString();
            if (!ids.add(id)) {
                throw new AssertionError("Duplicate advancement id in roll-up: " + id);
            }
            if (!entry.has("reason") || entry.get("reason").getAsString().isBlank()) {
                missingReasons++;
            }
            if (!entry.has("runtimeEvidenceSources")) {
                throw new AssertionError("Missing runtimeEvidenceSources for " + id);
            }
            if (!entry.has("runtimeEvidenceFamilies")) {
                throw new AssertionError("Missing runtimeEvidenceFamilies for " + id);
            }
        }

        assertEquals(1152, ids.size());
        assertEquals(0, missingReasons);
        assertEquals(1152, statusTotals.entrySet().stream().mapToInt(e -> e.getValue().getAsInt()).sum());
        assertFalse(statusTotals.has("UNKNOWN"));
        assertFalse(statusTotals.has("UNTESTED"));
        assertTrue(summary.get("totalCertified").getAsInt() <= 1152);
        assertTrue(summary.get("unresolvedOrDeferred").getAsInt() >= 0);
    }

    @Test
    void trimEvidenceAdapterTransfersGreenCriteriaFamilyAndSource() {
        PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData source =
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData.empty(TRIM_PATTERN_TARGET);
        source.greenCriteria().addAll(Set.of("coast_armor_trim", "flow_armor_trim"));
        source.criteriaByFamily().computeIfAbsent(TRIM_PATTERN_FAMILY, ignored -> new HashSet<>())
            .addAll(source.greenCriteria());
        source.criteriaBySource().computeIfAbsent(TRIM_PATTERN_SOURCE, ignored -> new HashSet<>())
            .addAll(source.greenCriteria());
        source.families().add(TRIM_PATTERN_FAMILY);
        source.sources().add(TRIM_PATTERN_SOURCE);

        PhaseAAdvancementRollup.RuntimeEvidence adapted = PhaseAAdvancementRollup
            .adaptTrimPatternRecipeCraftedEvidence(Map.of(TRIM_PATTERN_TARGET, source))
            .get(TRIM_PATTERN_TARGET);

        assertEquals(Set.of("coast_armor_trim", "flow_armor_trim"), adapted.greenCriteria());
        assertEquals(Set.of(TRIM_PATTERN_FAMILY), adapted.families());
        assertEquals(Set.of(TRIM_PATTERN_SOURCE), adapted.sources());
        assertEquals(
            Set.of("coast_armor_trim", "flow_armor_trim"),
            adapted.criteriaByFamily().get(TRIM_PATTERN_FAMILY)
        );
        assertEquals(
            Set.of("coast_armor_trim", "flow_armor_trim"),
            adapted.criteriaBySource().get(TRIM_PATTERN_SOURCE)
        );
    }

    @Test
    void shotCrossbowEvidenceAdapterTransfersGreenCriteriaFamilyAndSource() {
        String target = "blazeandcave:enchanting/machine_bow";
        var source = PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeEvidenceData.empty(target);
        source.greenCriteria().add("shot_crossbow");
        source.criteriaByFamily().put(SHOT_CROSSBOW_FAMILY, Set.of("shot_crossbow"));
        source.criteriaBySource().put(SHOT_CROSSBOW_SOURCE, Set.of("shot_crossbow"));
        source.families().add(SHOT_CROSSBOW_FAMILY);
        source.sources().add(SHOT_CROSSBOW_SOURCE);

        PhaseAAdvancementRollup.RuntimeEvidence adapted = PhaseAAdvancementRollup
            .adaptShotCrossbowEvidence(Map.of(target, source))
            .get(target);

        assertEquals(Set.of("shot_crossbow"), adapted.greenCriteria());
        assertEquals(Set.of(SHOT_CROSSBOW_FAMILY), adapted.families());
        assertEquals(Set.of(SHOT_CROSSBOW_SOURCE), adapted.sources());
        assertEquals(Set.of("shot_crossbow"), adapted.criteriaByFamily().get(SHOT_CROSSBOW_FAMILY));
        assertEquals(Set.of("shot_crossbow"), adapted.criteriaBySource().get(SHOT_CROSSBOW_SOURCE));
    }

    @Test
    void playerHurtEntityDamageSourceEvidenceAdapterTransfersGreenCriteriaFamilyAndSource() {
        String target = "blazeandcave:weaponry/slapfish";
        var source = PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeEvidenceData.empty(target);
        source.greenCriteria().add("slapfish");
        source.criteriaByFamily().put(
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY,
            Set.of("slapfish")
        );
        source.criteriaBySource().put(
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE,
            Set.of("slapfish")
        );
        source.families().add(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY);
        source.sources().add(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE);

        PhaseAAdvancementRollup.RuntimeEvidence adapted = PhaseAAdvancementRollup
            .adaptPlayerHurtEntityDamageSourceEvidence(Map.of(target, source))
            .get(target);

        assertEquals(Set.of("slapfish"), adapted.greenCriteria());
        assertEquals(Set.of(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY),
            adapted.families());
        assertEquals(Set.of(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE),
            adapted.sources());
        assertEquals(Set.of("slapfish"), adapted.criteriaByFamily()
            .get(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY));
        assertEquals(Set.of("slapfish"), adapted.criteriaBySource()
            .get(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE));
    }

    @Test
    void shotCrossbowPromotionIsPresentInCanonicalRollup() throws IOException {
        JsonObject root = generatePreUsingItemPromotionSnapshot();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonObject statusTotals = summary.getAsJsonObject("statusTotals");
        JsonArray entries = root.getAsJsonArray("entries");

        assertEquals(110, statusTotals.get("RUNTIME_CERTIFIED").getAsInt());
        assertEquals(66, statusTotals.get("RUNTIME_DEFERRED").getAsInt());
        assertEquals(1068, summary.get("totalCertified").getAsInt());
        assertEquals(111, summary.get("totalStrengthenedByRuntime").getAsInt());
        assertEquals(83, summary.get("unresolvedOrDeferred").getAsInt());
        assertEquals(118, summary.get("uniqueRuntimeTouchedAdvancements").getAsInt());

        for (String id : PhaseAShotCrossbowCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .toList()) {
            JsonObject entry = findEntry(entries, id);
            assertEquals("RUNTIME_DEFERRED", entry.get("staticStatus").getAsString(), id);
            assertEquals("RUNTIME_CERTIFIED", entry.get("advancementStatus").getAsString(), id);
            assertTrue(entry.get("requirementsSatisfied").getAsBoolean(), id);
            assertEquals(Set.of("shot_crossbow"), asSet(entry.getAsJsonArray("runtimeGreenCriteria")), id);
            assertArrayEquals(Set.of(SHOT_CROSSBOW_FAMILY), entry.getAsJsonArray("runtimeEvidenceFamilies"));
            assertArrayEquals(Set.of(SHOT_CROSSBOW_SOURCE), entry.getAsJsonArray("runtimeEvidenceSources"));
        }
    }

    @Test
    void playerHurtEntityDamageSourcePromotionIsPresentInCanonicalRollup() throws IOException {
        JsonObject root = generatePreUsingItemPromotionSnapshot();
        JsonObject entries = root.getAsJsonObject("summary");
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(1068, entries.get("totalCertified").getAsInt());
        for (String id : PhaseAPlayerHurtEntityDamageSourceCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .toList()) {
            JsonObject entry = findEntry(root.getAsJsonArray("entries"), id);
            assertEquals("RUNTIME_DEFERRED", entry.get("staticStatus").getAsString(), id);
            assertEquals("RUNTIME_CERTIFIED", entry.get("advancementStatus").getAsString(), id);
            assertTrue(entry.get("requirementsSatisfied").getAsBoolean(), id);
            assertEquals(Set.of(entry.get("runtimeGreenCriteria").getAsJsonArray().get(0).getAsString()),
                asSet(entry.getAsJsonArray("runtimeGreenCriteria")), id);
            assertArrayEquals(Set.of(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY),
                entry.getAsJsonArray("runtimeEvidenceFamilies"));
            assertArrayEquals(Set.of(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE),
                entry.getAsJsonArray("runtimeEvidenceSources"));
        }
    }

    @Test
    void trimEvidenceSatisfiesRealFrozenTargetRequirementGroup() throws IOException {
        JsonObject generated = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject targetEntry = generated.getAsJsonArray("entries").asList().stream()
            .map(JsonElement::getAsJsonObject)
            .filter(entry -> TRIM_PATTERN_TARGET.equals(entry.get("id").getAsString()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing frozen trim target"));

        JsonObject rawTarget = new JsonObject();
        rawTarget.add("requirements", targetEntry.getAsJsonArray("completionRequirements").deepCopy());
        PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData source =
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData.empty(TRIM_PATTERN_TARGET);
        for (JsonElement group : targetEntry.getAsJsonArray("completionRequirements")) {
            for (JsonElement criterion : group.getAsJsonArray()) {
                source.greenCriteria().add(criterion.getAsString());
            }
        }
        source.families().add(TRIM_PATTERN_FAMILY);
        source.sources().add(TRIM_PATTERN_SOURCE);
        source.criteriaByFamily().put(TRIM_PATTERN_FAMILY, new HashSet<>(source.greenCriteria()));
        source.criteriaBySource().put(TRIM_PATTERN_SOURCE, new HashSet<>(source.greenCriteria()));
        PhaseAAdvancementRollup.RuntimeEvidence adapted = PhaseAAdvancementRollup
            .adaptTrimPatternRecipeCraftedEvidence(Map.of(TRIM_PATTERN_TARGET, source))
            .get(TRIM_PATTERN_TARGET);
        PhaseACertification.StaticValidationStatus staticStatus = PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED;

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                staticStatus,
                rawTarget,
                adapted
            )
        );
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, staticStatus);
    }

    @Test
    void missingTrimPersistentArtifactContributesNothing() throws IOException {
        installExpansionDirect16Fixture();

        assertFalse(Files.exists(tempDir.resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.PERSISTENT_ARTIFACT)));
        assertFalse(PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir).containsKey(TRIM_PATTERN_TARGET));
    }

    @Test
    void expansionDirect16EvidenceProducesStablePostPromotionRollup() throws IOException {
        // Keep the historical Direct16 baseline independent of subsequently closed families.
        var direct16Stage = PhaseAAdvancementRollup.loadRuntimeEvidence(PROJECT_ROOT);
        direct16Stage.remove(TRIM_PATTERN_TARGET);
        PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.keySet().forEach(direct16Stage::remove);
        PhaseAItemTagItemUsedOnBlockCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        PhaseAEntityTypeTagStartedRidingCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        PhaseAItemTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        PhaseAShotCrossbowCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        PhaseAEntityTypeTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        PhaseAUsingItemCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(direct16Stage::remove);
        removeTameAnimalLlamaEvidence(direct16Stage);
        removeRecipeCraftedWaxOnEvidence(direct16Stage);
        removeItemTagInventoryContainmentEvidence(direct16Stage);
        removeFishingRodHookedEvidence(direct16Stage);
        removeTrimMaterialInventoryChangedEvidence(direct16Stage);
        direct16Stage.remove(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID);
        direct16Stage.remove(PhaseASingletonRedstoneClickCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonMangroveGroveCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonDiagonAllayCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonVillageBellCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonMaximumResistanceCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonAncientRestorationCertification.ADVANCEMENT);
        direct16Stage.remove(PhaseASingletonSilkTouchNestCertification.ADVANCEMENT);
        PhaseAPureEnchantmentInventoryChangedCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        PhaseAPlayerKilledEntityRemainderCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        PhaseALocationHolderSetWorldgenCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        PhaseAMixedPiglinDistractionCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        PhaseAMixedWeaponryMulticlassedCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        PhaseAMixedPerfectRunCertification.FROZEN.keySet().forEach(direct16Stage::remove);
        restoreHistoricalItemTagStackSlice(direct16Stage);
        JsonObject generated =
            JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT, direct16Stage)).getAsJsonObject();
        JsonObject summary = generated.getAsJsonObject("summary");
        JsonObject statusTotals = summary.getAsJsonObject("statusTotals");

        Set<String> validatedEvidenceIds = new HashSet<>();
        for (Map.Entry<String, PhaseAAdvancementRollup.RuntimeEvidence> entry :
            PhaseAAdvancementRollup.loadRuntimeEvidence(PROJECT_ROOT).entrySet()) {
            if (entry.getValue().families().contains(EXPANSION_DIRECT16_FAMILY)
                || entry.getValue().sources().contains(EXPANSION_DIRECT16_SOURCE)) {
                validatedEvidenceIds.add(entry.getKey());
            }
        }
        assertEquals(EXPANSION_DIRECT16_ADVANCEMENTS, validatedEvidenceIds);

        Set<String> generatedEvidenceIds = new HashSet<>();
        for (JsonElement element : generated.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("id").getAsString();
            Set<String> families = asSet(entry.getAsJsonArray("runtimeEvidenceFamilies"));
            Set<String> sources = asSet(entry.getAsJsonArray("runtimeEvidenceSources"));
            if (families.contains(EXPANSION_DIRECT16_FAMILY)
                || sources.contains(EXPANSION_DIRECT16_SOURCE)) {
                generatedEvidenceIds.add(id);
                assertTrue(EXPANSION_DIRECT16_ADVANCEMENTS.contains(id));
            }
            if (EXPANSION_DIRECT16_ADVANCEMENTS.contains(id)) {
                assertEquals("RUNTIME_CERTIFIED", entry.get("advancementStatus").getAsString());
            }
        }
        assertEquals(EXPANSION_DIRECT16_ADVANCEMENTS, generatedEvidenceIds);

        assertEquals(958, statusTotals.get("STATIC_CERTIFIED").getAsInt());
        assertEquals(87, statusTotals.get("RUNTIME_CERTIFIED").getAsInt());
        assertEquals(1, statusTotals.get("RUNTIME_PARTIAL").getAsInt());
        assertEquals(89, statusTotals.get("RUNTIME_DEFERRED").getAsInt());
        assertEquals(17, statusTotals.get("STATIC_FAIL_UNRESOLVED").getAsInt());
        assertEquals(1045, summary.get("totalCertified").getAsInt());
        assertEquals(88, summary.get("totalStrengthenedByRuntime").getAsInt());
        assertEquals(106, summary.get("unresolvedOrDeferred").getAsInt());
        assertEquals(
            107,
            summary.get("runtimePartial").getAsInt() + summary.get("unresolvedOrDeferred").getAsInt()
        );
        assertEquals(95, summary.get("uniqueRuntimeTouchedAdvancements").getAsInt());
        assertEquals(6, summary.get("staticCertifiedWithRuntimeEvidence").getAsInt());
    }

    @Test
    void blockTagPersistentLoaderAndAdapterRetainExactSevenAndThirty() throws IOException {
        installBlockTagRollupFixture();
        var loaded = PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir);
        Set<String> familyIds = new HashSet<>();
        int criteriaCount = 0;
        for (var entry : loaded.entrySet()) {
            var data = entry.getValue();
            if (!data.families().contains(PhaseABlockTagItemUsedOnBlockCertification.FAMILY)) continue;
            familyIds.add(entry.getKey());
            Set<String> expected = Set.copyOf(PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.get(entry.getKey()));
            assertEquals(expected, data.greenCriteria());
            assertEquals(expected, data.criteriaByFamily().get(PhaseABlockTagItemUsedOnBlockCertification.FAMILY));
            assertEquals(expected, data.criteriaBySource().get(PhaseABlockTagItemUsedOnBlockCertification.SOURCE));
            assertEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.FAMILY), data.families());
            assertEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.SOURCE), data.sources());
            criteriaCount += data.greenCriteria().size();
        }
        assertEquals(PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.keySet(), familyIds);
        assertEquals(7, familyIds.size());
        assertEquals(30, criteriaCount);
        assertFalse(Files.exists(tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.TEMP)));

        // An invalid existing persistent file must fail, not silently contribute no evidence.
        Files.writeString(tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT), "{}");
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    private void installBlockTagRollupFixture() throws IOException {
        installExpansionDirect16Fixture();
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidationTest.installPersistentFixture(tempDir);
    }

    @Test
    void blockTagExactSevenTransitionsPreserveAllUnrelatedEntriesAndProduceExpectedSummary() throws IOException {
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidationTest.installPersistentFixture(tempDir);
        var baselineEvidence = PhaseAAdvancementRollup.loadRuntimeEvidence(PROJECT_ROOT);
        Set<String> targets = PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.keySet();
        targets.forEach(baselineEvidence::remove);
        PhaseAItemTagItemUsedOnBlockCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        PhaseAEntityTypeTagStartedRidingCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        PhaseAItemTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        PhaseAShotCrossbowCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        PhaseAEntityTypeTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        PhaseAUsingItemCertification.EXPECTED_KEYS.stream()
            .map(key -> key.substring(0, key.indexOf('#')))
            .forEach(baselineEvidence::remove);
        removeTameAnimalLlamaEvidence(baselineEvidence);
        removeRecipeCraftedWaxOnEvidence(baselineEvidence);
        removeItemTagInventoryContainmentEvidence(baselineEvidence);
        removeFishingRodHookedEvidence(baselineEvidence);
        removeTrimMaterialInventoryChangedEvidence(baselineEvidence);
        baselineEvidence.remove(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID);
        baselineEvidence.remove(PhaseASingletonRedstoneClickCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonMangroveGroveCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonDiagonAllayCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonVillageBellCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonMaximumResistanceCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonAncientRestorationCertification.ADVANCEMENT);
        baselineEvidence.remove(PhaseASingletonSilkTouchNestCertification.ADVANCEMENT);
        PhaseAPureEnchantmentInventoryChangedCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        PhaseAPlayerKilledEntityRemainderCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        PhaseALocationHolderSetWorldgenCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        PhaseAMixedPiglinDistractionCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        PhaseAMixedWeaponryMulticlassedCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        PhaseAMixedPerfectRunCertification.FROZEN.keySet().forEach(baselineEvidence::remove);
        restoreHistoricalItemTagStackSlice(baselineEvidence);
        var before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT, baselineEvidence)).getAsJsonObject();
        var addition = PhaseAAdvancementRollup.adaptBlockTagItemUsedOnBlockEvidence(
            PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
        PhaseAAdvancementRollup.mergeRuntimeEvidence(baselineEvidence, addition, "block-tag-item_used_on_block");
        var after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT, baselineEvidence)).getAsJsonObject();
        Set<String> transitions = new HashSet<>();
        int retained = 0;
        for (JsonElement element : after.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("id").getAsString();
            JsonObject old = findEntry(before.getAsJsonArray("entries"), id);
            if (!targets.contains(id)) { assertEquals(old, entry, id); continue; }
            assertEquals("RUNTIME_DEFERRED", old.get("advancementStatus").getAsString(), id);
            assertEquals("RUNTIME_CERTIFIED", entry.get("advancementStatus").getAsString(), id);
            assertEquals("RUNTIME_DEFERRED", entry.get("staticStatus").getAsString(), id);
            assertTrue(entry.get("requirementsSatisfied").getAsBoolean(), id);
            assertEquals(Set.copyOf(PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.get(id)), asSet(entry.getAsJsonArray("runtimeGreenCriteria")), id);
            assertArrayEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.FAMILY), entry.getAsJsonArray("runtimeEvidenceFamilies"));
            assertArrayEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.SOURCE), entry.getAsJsonArray("runtimeEvidenceSources"));
            retained += entry.getAsJsonArray("runtimeGreenCriteria").size();
            transitions.add(id);
            JsonObject unchangedFields = entry.deepCopy(), oldFields = old.deepCopy();
            for (String field : java.util.List.of("advancementStatus", "requirementsSatisfied", "runtimeGreenCriteria", "runtimeEvidenceFamilies", "runtimeEvidenceSources", "reason")) {
                unchangedFields.remove(field); oldFields.remove(field);
            }
            assertEquals(oldFields, unchangedFields, id);
        }
        assertEquals(targets, transitions);
        assertEquals(30, retained);
        assertEquals(before.get("locationMovementImpact"), after.get("locationMovementImpact"));
        assertEquals(1152, after.get("canonicalAdvancementCount").getAsInt());
        assertBlockTagSummary(before.getAsJsonObject("summary"), 88, 88, 1046, 89, 105, 96);
        assertBlockTagSummary(after.getAsJsonObject("summary"), 95, 81, 1053, 96, 98, 103);
    }

    private static void assertBlockTagSummary(JsonObject summary, int certified, int deferred, int total, int strengthened, int unresolved, int touched) {
        JsonObject totals = summary.getAsJsonObject("statusTotals");
        assertEquals(958, totals.get("STATIC_CERTIFIED").getAsInt());
        assertEquals(certified, totals.get("RUNTIME_CERTIFIED").getAsInt());
        assertEquals(deferred, totals.get("RUNTIME_DEFERRED").getAsInt());
        assertEquals(1, totals.get("RUNTIME_PARTIAL").getAsInt());
        assertEquals(17, totals.get("STATIC_FAIL_UNRESOLVED").getAsInt());
        assertEquals(1152, totals.entrySet().stream().mapToInt(e -> e.getValue().getAsInt()).sum());
        assertEquals(1152, summary.get("totalAdvancements").getAsInt());
        assertEquals(total, summary.get("totalCertified").getAsInt());
        assertEquals(strengthened, summary.get("totalStrengthenedByRuntime").getAsInt());
        assertEquals(certified, summary.get("runtimeCertified").getAsInt());
        assertEquals(1, summary.get("runtimePartial").getAsInt());
        assertEquals(unresolved, summary.get("unresolvedOrDeferred").getAsInt());
        assertEquals(touched, summary.get("uniqueRuntimeTouchedAdvancements").getAsInt());
        assertEquals(6, summary.get("staticCertifiedWithRuntimeEvidence").getAsInt());
    }

    @Test
    void blockTagFrozenCompletionRequiresSixteenDyesNineFoodsAndOnlyOneMealAlternative() throws IOException {
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidationTest.installPersistentFixture(tempDir);
        var evidence = PhaseAAdvancementRollup.adaptBlockTagItemUsedOnBlockEvidence(
            PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
        var frozenEntries = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT)).getAsJsonObject().getAsJsonArray("entries");
        String meal = "blazeandcave:farming/one_course_meal";
        var mealRequirements = findEntry(frozenEntries, meal).getAsJsonArray("completionRequirements");
        assertEquals(1, mealRequirements.size());
        assertArrayEquals(Set.of("bone_meal", "bone_meal_propagule"), mealRequirements.get(0).getAsJsonArray());
        assertEquals(Set.of("bone_meal"), evidence.get(meal).greenCriteria());
        for (String id : java.util.List.of(meal, "blazeandcave:building/colors_of_the_wind", "blazeandcave:building/delicious_hot_schmoes")) {
            var raw = new JsonObject();
            var groups = findEntry(frozenEntries, id).getAsJsonArray("completionRequirements");
            raw.add("requirements", groups);
            int expectedGroups = id.equals(meal) ? 1 : id.endsWith("colors_of_the_wind") ? 16 : 9;
            assertEquals(expectedGroups, groups.size());
            var data = evidence.get(id);
            assertEquals(expectedGroups, data.greenCriteria().size());
            assertEquals(PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
                PhaseAAdvancementRollup.classify(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, raw, data));
            for (String criterion : Set.copyOf(data.greenCriteria())) {
                data.greenCriteria().remove(criterion);
                assertEquals(id.equals(meal) ? PhaseAAdvancementRollup.RollupStatus.RUNTIME_DEFERRED : PhaseAAdvancementRollup.RollupStatus.RUNTIME_PARTIAL,
                    PhaseAAdvancementRollup.classify(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, raw, data));
                data.greenCriteria().add(criterion);
            }
        }
    }

    @Test
    void matrixAutomatedGreenWithoutPersistentReceiptDoesNotRuntimeCertifyAdvancement() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence = loadRuntimeEvidenceWithExpansionFixture();

        assertFalse(evidence.containsKey("adv1"));
        assertTrue(evidence.values().stream().anyMatch(entry -> entry.families().contains(EXPANSION_DIRECT16_FAMILY)));
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_DEFERRED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"crit1\":{}}}"),
                PhaseAAdvancementRollup.RuntimeEvidence.empty("adv1")
            )
        );
    }

    @Test
    void locationEvidenceAloneStillWorks() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));

        PhaseAAdvancementRollup.RuntimeEvidence evidence = loadRuntimeEvidenceWithExpansionFixture().get("adv1");

        assertEquals(Set.of("crit1"), evidence.greenCriteria());
        assertEquals(Set.of(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY), evidence.families());
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"crit1\":{}}}"),
                evidence
            )
        );
    }

    @Test
    void containerEvidenceAloneContributesGreenCriteria() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> adapted = PhaseAAdvancementRollup.adaptContainerEvidence(Map.of(
            "adv1",
            containerEvidence("adv1", "loot_crit")
        ));

        PhaseAAdvancementRollup.RuntimeEvidence evidence = adapted.get("adv1");
        assertEquals(Set.of("loot_crit"), evidence.greenCriteria());
        assertEquals(Set.of(PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY), evidence.families());
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"loot_crit\":{}}}"),
                evidence
            )
        );
    }

    @Test
    void itemTagEvidenceAloneContributesGreenCriteria() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> adapted = PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of(
            "adv1",
            itemTagEvidence("adv1", "smithing_template")
        ));

        PhaseAAdvancementRollup.RuntimeEvidence evidence = adapted.get("adv1");
        assertEquals(Set.of("smithing_template"), evidence.greenCriteria());
        assertEquals(Set.of(ITEM_TAG_FAMILY), evidence.families());
        assertEquals(Set.of(ITEM_TAG_SOURCE), evidence.sources());
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"smithing_template\":{}}}"),
                evidence
            )
        );
    }

    @Test
    void itemTagEvidenceCanLeaveAdvancementRuntimePartial() {
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_PARTIAL,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("""
                    {
                      "criteria": {"a": {}, "b": {}},
                      "requirements": [["a"], ["b"]]
                    }
                    """),
                PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of(
                    "adv1",
                    itemTagEvidence("adv1", "a")
                )).get("adv1")
            )
        );
    }

    @Test
    void enchantmentEvidenceAloneContributesGreenCriteria() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> adapted = PhaseAAdvancementRollup.adaptEnchantmentEvidence(Map.of(
            "adv1",
            enchantmentEvidence("adv1", "mending")
        ));

        PhaseAAdvancementRollup.RuntimeEvidence evidence = adapted.get("adv1");
        assertEquals(Set.of("mending"), evidence.greenCriteria());
        assertEquals(Set.of(ENCHANTMENT_FAMILY), evidence.families());
        assertEquals(Set.of(ENCHANTMENT_SOURCE), evidence.sources());
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"mending\":{}}}"),
                evidence
            )
        );
    }

    @Test
    void expansionDirect16EvidenceAloneContributesGreenCriteria() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> adapted =
            PhaseAAdvancementRollup.adaptExpansionDirect16Evidence(Map.of(
                "adv1",
                expansionDirect16Evidence("adv1", "power")
            ));

        PhaseAAdvancementRollup.RuntimeEvidence evidence = adapted.get("adv1");
        assertEquals(Set.of("power"), evidence.greenCriteria());
        assertEquals(Set.of(EXPANSION_DIRECT16_FAMILY), evidence.families());
        assertEquals(Set.of(EXPANSION_DIRECT16_SOURCE), evidence.sources());
    }

    @Test
    void evidenceFromBothValidatorsForSameAdvancementIsUnioned() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "crit1")))
        );

        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "crit2"))),
            "container-loot"
        );

        PhaseAAdvancementRollup.RuntimeEvidence evidence = merged.get("adv1");
        assertEquals(Set.of("crit1", "crit2"), evidence.greenCriteria());
        assertEquals(
            Set.of(
                PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
                PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY
            ),
            evidence.families()
        );
        assertEquals(Set.of(SOURCE, CONTAINER_SOURCE), evidence.sources());
    }

    @Test
    void allSixValidatedEvidenceSourcesMergeThroughNeutralRuntimeEvidence() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "location"))));
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "container"))),
            "container-loot"
        );
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of("adv1", itemTagEvidence("adv1", "item"))),
            "item-tag-inventory_changed"
        );
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptEnchantmentEvidence(Map.of("adv1", enchantmentEvidence("adv1", "enchantment"))),
            "enchantment-holderset-inventory_changed"
        );
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptExpansionDirect16Evidence(
                Map.of("adv1", expansionDirect16Evidence("adv1", "expansion"))
            ),
            "enchantment-expansion-direct16-inventory_changed"
        );
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "placed_block"))),
            "item-tag-placed_block"
        );

        PhaseAAdvancementRollup.RuntimeEvidence evidence = merged.get("adv1");
        assertEquals(Set.of("container", "enchantment", "expansion", "item", "location", "placed_block"), evidence.greenCriteria());
        assertEquals(
            Set.of(
                PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
                PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY,
                ITEM_TAG_FAMILY,
                ENCHANTMENT_FAMILY,
                EXPANSION_DIRECT16_FAMILY,
                PLACED_BLOCK_FAMILY
            ),
            evidence.families()
        );
        assertEquals(
            Set.of(SOURCE, CONTAINER_SOURCE, ITEM_TAG_SOURCE, ENCHANTMENT_SOURCE, EXPANSION_DIRECT16_SOURCE, PLACED_BLOCK_SOURCE),
            evidence.sources()
        );
    }

    @Test
    void duplicateCriterionAcrossExpansionDirect16AndEnchantmentEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptEnchantmentEvidence(Map.of("adv1", enchantmentEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptExpansionDirect16Evidence(
                    Map.of("adv1", expansionDirect16Evidence("adv1", "shared"))
                ),
                "enchantment-expansion-direct16-inventory_changed"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(EXPANSION_DIRECT16_FAMILY));
        assertTrue(error.getMessage().contains(EXPANSION_DIRECT16_SOURCE));
    }

    @Test
    void differentCriteriaFromDifferentFamiliesCanJointlySatisfyRequirements() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "location"))));
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "loot"))),
            "container-loot"
        );

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("""
                    {
                      "criteria": {"location": {}, "loot": {}},
                      "requirements": [["location"], ["loot"]]
                    }
                    """),
                merged.get("adv1")
            )
        );
    }

    @Test
    void duplicateCriterionAcrossEvidenceSourcesIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "shared"))),
                "container-loot"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
    }

    @Test
    void duplicateCriterionAcrossItemTagAndLocationEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of("adv1", itemTagEvidence("adv1", "shared"))),
                "item-tag-inventory_changed"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(ITEM_TAG_FAMILY));
        assertTrue(error.getMessage().contains(ITEM_TAG_SOURCE));
    }

    @Test
    void duplicateCriterionAcrossItemTagAndContainerEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of("adv1", itemTagEvidence("adv1", "shared"))),
                "item-tag-inventory_changed"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(ITEM_TAG_FAMILY));
        assertTrue(error.getMessage().contains(ITEM_TAG_SOURCE));
    }

    @Test
    void duplicateCriterionAcrossEnchantmentAndLocationEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptEnchantmentEvidence(Map.of("adv1", enchantmentEvidence("adv1", "shared"))),
                "enchantment-holderset-inventory_changed"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(ENCHANTMENT_FAMILY));
        assertTrue(error.getMessage().contains(ENCHANTMENT_SOURCE));
    }

    @Test
    void duplicateCriterionAcrossPlacedBlockAndLocationEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "shared"))),
                "item-tag-placed_block"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_FAMILY));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_SOURCE));
        assertTrue(error.getMessage().contains(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY));
    }

    @Test
    void duplicateCriterionAcrossPlacedBlockAndContainerEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "shared"))),
                "item-tag-placed_block"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_FAMILY));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_SOURCE));
        assertTrue(error.getMessage().contains(PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY));
    }

    @Test
    void duplicateCriterionAcrossPlacedBlockAndItemTagEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of("adv1", itemTagEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "shared"))),
                "item-tag-placed_block"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_FAMILY));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_SOURCE));
        assertTrue(error.getMessage().contains(ITEM_TAG_FAMILY));
    }

    @Test
    void duplicateCriterionAcrossPlacedBlockAndEnchantmentEvidenceIsRejected() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptEnchantmentEvidence(Map.of("adv1", enchantmentEvidence("adv1", "shared"))));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAAdvancementRollup.mergeRuntimeEvidence(
                merged,
                PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "shared"))),
                "item-tag-placed_block"
            )
        );

        assertTrue(error.getMessage().contains("adv1#shared"));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_FAMILY));
        assertTrue(error.getMessage().contains(PLACED_BLOCK_SOURCE));
        assertTrue(error.getMessage().contains(ENCHANTMENT_FAMILY));
    }

    @Test
    void placedBlockEvidenceCanSatisfyFinalRequirementOfRuntimeDeferredAdvancement() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptItemTagEvidence(Map.of("adv1", itemTagEvidence("adv1", "a"))));
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptPlacedBlockEvidence(Map.of("adv1", placedBlockEvidence("adv1", "b"))),
            "item-tag-placed_block"
        );

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("""
                    {
                      "criteria": {"a": {}, "b": {}},
                      "requirements": [["a"], ["b"]]
                    }
                    """),
                merged.get("adv1")
            )
        );
    }

    @Test
    void andRequirementsNeedEvidenceForBothGroups() {
        JsonObject advancement = advancementJson("""
            {
              "criteria": {"a": {}, "b": {}},
              "requirements": [["a"], ["b"]]
            }
            """);

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_PARTIAL,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancement,
                runtimeEvidence("adv1", "a")
            )
        );
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancement,
                runtimeEvidence("adv1", "a", "b")
            )
        );
    }

    @Test
    void orRequirementsAllowOneCriterionWithinAGroup() {
        JsonObject advancement = advancementJson("""
            {
              "criteria": {"a": {}, "b": {}, "c": {}},
              "requirements": [["a", "b"], ["c"]]
            }
            """);

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancement,
                runtimeEvidence("adv1", "b", "c")
            )
        );
    }

    @Test
    void partialRuntimeEvidenceStaysRuntimePartial() {
        JsonObject advancement = advancementJson("""
            {
              "criteria": {"a": {}, "b": {}},
              "requirements": [["a"], ["b"]]
            }
            """);

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_PARTIAL,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancement,
                runtimeEvidence("adv1", "a")
            )
        );
    }

    @Test
    void zeroRuntimeEvidenceLeavesRuntimeDeferred() {
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.RUNTIME_DEFERRED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED,
                advancementJson("{\"criteria\":{\"crit1\":{}}}"),
                PhaseAAdvancementRollup.RuntimeEvidence.empty("adv1")
            )
        );
    }

    @Test
    void staticFailRemainsUnresolvedEvenWhenRuntimeRequirementsAreSatisfied() {
        JsonObject advancement = advancementJson("""
            {
              "criteria": {"a": {}, "b": {}},
              "requirements": [["a"], ["b"]]
            }
            """);

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.STATIC_FAIL_UNRESOLVED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.STATIC_FAIL,
                advancement,
                runtimeEvidence("adv1", "a", "b")
            )
        );
    }

    @Test
    void staticPassAlwaysMapsToStaticCertified() {
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.STATIC_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.STATIC_PASS,
                advancementJson("{\"criteria\":{\"crit1\":{}}}"),
                runtimeEvidence("adv1", "crit1")
            )
        );
    }

    @Test
    void staticPassRemainsStaticCertifiedEvenWithContainerRuntimeEvidence() {
        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.STATIC_CERTIFIED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.STATIC_PASS,
                advancementJson("{\"criteria\":{\"loot_crit\":{}}}"),
                runtimeEvidence("adv1", "loot_crit")
            )
        );
    }

    @Test
    void staticFailRemainsStaticFailUnresolvedEvenWithMergedRuntimeEvidence() {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> merged = new LinkedHashMap<>(
            PhaseAAdvancementRollup.adaptRuntimeEvidence(Map.of("adv1", locationEvidence("adv1", "a"))));
        PhaseAAdvancementRollup.mergeRuntimeEvidence(
            merged,
            PhaseAAdvancementRollup.adaptContainerEvidence(Map.of("adv1", containerEvidence("adv1", "b"))),
            "container-loot"
        );

        assertEquals(
            PhaseAAdvancementRollup.RollupStatus.STATIC_FAIL_UNRESOLVED,
            PhaseAAdvancementRollup.classify(
                PhaseACertification.StaticValidationStatus.STATIC_FAIL,
                advancementJson("""
                    {
                      "criteria": {"a": {}, "b": {}},
                      "requirements": [["a"], ["b"]]
                    }
                    """),
                merged.get("adv1")
            )
        );
    }

    @Test
    void missingContainerPersistentArtifactContributesNoContainerEvidence() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));
        writeContainerCatalog(containerCatalogJson(containerCaseJson("adv2", "loot_crit", "minecraft:chests/test", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence = loadRuntimeEvidenceWithExpansionFixture();

        assertTrue(evidence.containsKey("adv1"));
        assertFalse(evidence.containsKey("adv2"));
    }

    @Test
    void missingItemTagPersistentArtifactContributesNoItemTagEvidence() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));
        writeContainerCatalog(containerCatalogJson(containerCaseJson("adv2", "loot_crit", "minecraft:chests/test", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeContainerArtifact(containerArtifactJson(currentContainerFingerprint(), containerEntryJson(
            "adv2",
            "loot_crit",
            "minecraft:chests/test",
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));
        writeItemTagCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence = loadRuntimeEvidenceWithExpansionFixture();

        assertFalse(evidence.values().stream().anyMatch(entry -> entry.families().contains(ITEM_TAG_FAMILY)));
        assertFalse(evidence.values().stream().anyMatch(entry -> entry.sources().contains(ITEM_TAG_SOURCE)));
    }

    @Test
    void missingEnchantmentPersistentArtifactContributesNoEnchantmentEvidence() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));
        writeContainerCatalog(containerCatalogJson(containerCaseJson("adv2", "loot_crit", "minecraft:chests/test", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeContainerArtifact(containerArtifactJson(currentContainerFingerprint(), containerEntryJson(
            "adv2",
            "loot_crit",
            "minecraft:chests/test",
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));
        writeItemTagCatalog(catalogJson(
            caseJson("adv3", "tag_crit", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeItemTagArtifact(itemTagArtifactJson(
            currentItemTagFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            itemTagEntryJson("adv3", "tag_crit", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));
        writeEnchantmentCatalog(enchantmentCatalogJson(
            enchantmentCaseJson("adv4", "ench_crit", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentPredicateJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))
        ));

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence = loadRuntimeEvidenceWithExpansionFixture();

        assertFalse(evidence.values().stream().anyMatch(entry -> entry.families().contains(ENCHANTMENT_FAMILY)));
        assertFalse(evidence.values().stream().anyMatch(entry -> entry.sources().contains(ENCHANTMENT_SOURCE)));
    }

    @Test
    void missingPlacedBlockPersistentArtifactContributesNoPlacedBlockEvidence() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));
        writePlacedBlockCatalog(placedBlockCatalogJson(
            placedBlockCaseJson("adv5", "placed_crit", "minecraft:fences", PhaseAItemTagPlacedBlockExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence = loadRuntimeEvidenceWithExpansionFixture();

        assertFalse(evidence.values().stream().anyMatch(entry -> entry.families().contains(PLACED_BLOCK_FAMILY)));
        assertFalse(evidence.values().stream().anyMatch(entry -> entry.sources().contains(PLACED_BLOCK_SOURCE)));
    }

    @Test
    void invalidStaleItemTagPersistentEvidenceIsRejectedByValidation() throws IOException {
        writeItemTagCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeItemTagArtifact(itemTagArtifactJson(
            "sha-256:stale",
            "run-1",
            "2026-08-22T00:00:00Z",
            itemTagEntryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        installExpansionDirect16Fixture();
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    @Test
    void locationMovementImpactStaysLocationOnly() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject impact = root.getAsJsonObject("locationMovementImpact");

        assertEquals(16, impact.get("uniqueAdvancementsTouched").getAsInt());
        assertEquals(15, impact.get("runtimeCertified").getAsInt());
        assertEquals(0, impact.get("runtimePartial").getAsInt());
        assertEquals(0, impact.get("runtimeDeferred").getAsInt());
        assertEquals(0, impact.get("staticCertified").getAsInt());
        assertEquals(1, impact.get("staticFailUnresolved").getAsInt());
    }

    @Test
    void generatedSnapshotSerializesRuntimeEvidenceFamiliesAlongsideSources() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonArray entries = root.getAsJsonArray("entries");

        JsonObject iAmRoot = findEntry(entries, "blazeandcave:nether/i_am_root");
        assertArrayEquals(Set.of(ITEM_TAG_FAMILY), iAmRoot.getAsJsonArray("runtimeEvidenceFamilies"));
        assertArrayEquals(Set.of(ITEM_TAG_SOURCE), iAmRoot.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject whatAFungi = findEntry(entries, "blazeandcave:nether/what_a_fungi");
        assertArrayEquals(Set.of(ITEM_TAG_FAMILY), whatAFungi.getAsJsonArray("runtimeEvidenceFamilies"));
        assertArrayEquals(Set.of(ITEM_TAG_SOURCE), whatAFungi.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject raidinMaster = findEntry(entries, "blazeandcave:adventure/raidin_master");
        assertArrayEquals(
            Set.of(
                PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY,
                PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY
            ),
            raidinMaster.getAsJsonArray("runtimeEvidenceFamilies")
        );
        assertArrayEquals(Set.of(SOURCE, CONTAINER_SOURCE), raidinMaster.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject untouched = findEntry(entries, "blazeandcave:adventure/a_chiptune_relic");
        assertEquals(0, untouched.getAsJsonArray("runtimeEvidenceFamilies").size());
        assertEquals(0, untouched.getAsJsonArray("runtimeEvidenceSources").size());

        JsonObject likeANinja = findEntry(entries, "blazeandcave:enchanting/like_a_ninja");
        assertTrue(asSet(likeANinja.getAsJsonArray("runtimeEvidenceFamilies")).contains(ENCHANTMENT_FAMILY));
        assertTrue(asSet(likeANinja.getAsJsonArray("runtimeEvidenceSources")).contains(ENCHANTMENT_SOURCE));

        JsonObject theMistake = findEntry(entries, "blazeandcave:mining/the_mistake");
        assertTrue(asSet(theMistake.getAsJsonArray("runtimeEvidenceFamilies")).contains(ENCHANTMENT_FAMILY));
        assertTrue(asSet(theMistake.getAsJsonArray("runtimeEvidenceSources")).contains(ENCHANTMENT_SOURCE));

        JsonObject enGarde = findEntry(entries, "blazeandcave:building/en_garde");
        assertArrayEquals(Set.of(PLACED_BLOCK_FAMILY), enGarde.getAsJsonArray("runtimeEvidenceFamilies"));
        assertArrayEquals(Set.of(PLACED_BLOCK_SOURCE), enGarde.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject itsATrap = findEntry(entries, "blazeandcave:building/its_a_trap");
        assertArrayEquals(Set.of(PLACED_BLOCK_FAMILY), itsATrap.getAsJsonArray("runtimeEvidenceFamilies"));
        assertArrayEquals(Set.of(PLACED_BLOCK_SOURCE), itsATrap.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject raiseTheFlag = findEntry(entries, "blazeandcave:building/raise_the_flag");
        assertArrayEquals(Set.of(PLACED_BLOCK_FAMILY), raiseTheFlag.getAsJsonArray("runtimeEvidenceFamilies"));
        assertArrayEquals(Set.of(PLACED_BLOCK_SOURCE), raiseTheFlag.getAsJsonArray("runtimeEvidenceSources"));

        JsonObject godOfThunder = findEntry(entries, "blazeandcave:enchanting/god_of_thunder");
        assertFalse(asSet(godOfThunder.getAsJsonArray("runtimeEvidenceFamilies")).contains(ENCHANTMENT_FAMILY));
        assertFalse(asSet(godOfThunder.getAsJsonArray("runtimeEvidenceSources")).contains(ENCHANTMENT_SOURCE));
    }

    @Test
    void invalidContainerPersistentEvidenceIsRejectedByValidation() throws IOException {
        writeContainerCatalog(containerCatalogJson(containerCaseJson("adv1", "loot_crit", "minecraft:chests/test", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeContainerArtifact(containerArtifactJson("sha-256:stale", containerEntryJson(
            "adv1",
            "loot_crit",
            "minecraft:chests/test",
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));

        installExpansionDirect16Fixture();
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    @Test
    void invalidEnchantmentPersistentEvidenceIsRejectedByValidation() throws IOException {
        writeEnchantmentCatalog(enchantmentCatalogJson(
            enchantmentCaseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentPredicateJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))
        ));
        writeEnchantmentArtifact(enchantmentArtifactJson(
            currentEnchantmentFingerprint(),
            enchantmentEntryJson("adv1", "crit1", "minecraft:stone_sword", PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.GREEN, enchantmentConfiguredEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        installExpansionDirect16Fixture();
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    @Test
    void invalidPersistentLocationEvidenceIsRejectedByValidation() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson("sha-256:stale", entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        installExpansionDirect16Fixture();
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    @Test
    void invalidPlacedBlockPersistentEvidenceIsRejectedByValidation() throws IOException {
        writePlacedBlockCatalog(placedBlockCatalogJson(
            placedBlockCaseJson("adv1", "crit1", "minecraft:fences", PhaseAItemTagPlacedBlockExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writePlacedBlockArtifact(placedBlockArtifactJson(
            "sha-256:stale",
            "run-1",
            placedBlockEntryJson("adv1", "crit1", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence", currentPlacedBlockFingerprint(), "run-1")
        ));

        installExpansionDirect16Fixture();
        assertThrows(IllegalStateException.class, () -> PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir));
    }

    private Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadRuntimeEvidenceWithExpansionFixture() throws IOException {
        installExpansionDirect16Fixture();
        return PhaseAAdvancementRollup.loadRuntimeEvidence(tempDir);
    }

    private void installExpansionDirect16Fixture() throws IOException {
        // Both closed loaders require their persistent artifacts even in tests of other families.
        for (Path relative : java.util.List.of(
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.CATALOG_PATH,
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.PERSISTENT_ARTIFACT
        )) {
            Files.createDirectories(tempDir.resolve(relative).getParent());
            Files.copy(PROJECT_ROOT.resolve(relative), tempDir.resolve(relative));
        }
        Path fixtureCatalog =
            tempDir.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.CATALOG_PATH);
        Path fixtureArtifact =
            tempDir.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(fixtureCatalog.getParent());
        Files.copy(
            PROJECT_ROOT.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.CATALOG_PATH),
            fixtureCatalog
        );
        Files.copy(
            PROJECT_ROOT.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.PERSISTENT_ARTIFACT),
            fixtureArtifact
        );
    }

    private void writeMatrix(String json) throws IOException {
        Path path = tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.MATRIX_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeContainerCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeContainerArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeItemTagCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeItemTagArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeEnchantmentCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeEnchantmentArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writePlacedBlockCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writePlacedBlockArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseARuntimeExecutionEvidenceValidation.fingerprint(tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.MATRIX_PATH));
    }

    private String currentContainerFingerprint() throws IOException {
        return PhaseAContainerLootExecutionEvidenceValidation.fingerprint(tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.CATALOG_PATH));
    }

    private String currentItemTagFingerprint() throws IOException {
        return PhaseAItemTagInventoryChangedExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private String currentEnchantmentFingerprint() throws IOException {
        return PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private String currentPlacedBlockFingerprint() throws IOException {
        return PhaseAItemTagPlacedBlockExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private static PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData itemTagEvidence(String advancementId, String... criteria) {
        PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData evidence = PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(ITEM_TAG_FAMILY);
            evidence.sources().add(ITEM_TAG_SOURCE);
            evidence.itemTags().add("minecraft:test_tag");
            evidence.selectedItems().add("minecraft:test_item");
            evidence.criteriaByFamily().computeIfAbsent(ITEM_TAG_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(ITEM_TAG_SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static PhaseAAdvancementRollup.RuntimeEvidence runtimeEvidence(String advancementId, String... criteria) {
        PhaseAAdvancementRollup.RuntimeEvidence evidence = PhaseAAdvancementRollup.RuntimeEvidence.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
        }
        return evidence;
    }

    private static PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData placedBlockEvidence(String advancementId, String... criteria) {
        PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData evidence = PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(PLACED_BLOCK_FAMILY);
            evidence.sources().add(PLACED_BLOCK_SOURCE);
            evidence.criteriaByFamily().computeIfAbsent(PLACED_BLOCK_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(PLACED_BLOCK_SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData enchantmentEvidence(String advancementId, String... criteria) {
        PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData evidence = PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(ENCHANTMENT_FAMILY);
            evidence.sources().add(ENCHANTMENT_SOURCE);
            evidence.selectedItems().add("minecraft:test_item");
            evidence.configuredEnchantments().add("minecraft:test_enchantment@1|ENCHANTMENTS");
            evidence.criteriaByFamily().computeIfAbsent(ENCHANTMENT_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(ENCHANTMENT_SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData
    expansionDirect16Evidence(String advancementId, String... criteria) {
        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData evidence =
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(EXPANSION_DIRECT16_FAMILY);
            evidence.sources().add(EXPANSION_DIRECT16_SOURCE);
            evidence.criteriaByFamily().computeIfAbsent(EXPANSION_DIRECT16_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(EXPANSION_DIRECT16_SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData locationEvidence(String advancementId, String... criteria) {
        PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData evidence = PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY);
            evidence.sources().add(SOURCE);
            evidence.criteriaByFamily().computeIfAbsent(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData containerEvidence(String advancementId, String... criteria) {
        PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData evidence = PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData.empty(advancementId);
        for (String criterion : criteria) {
            evidence.greenCriteria().add(criterion);
            evidence.families().add(PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY);
            evidence.lootTables().add("minecraft:chests/test");
            evidence.sources().add(CONTAINER_SOURCE);
            evidence.criteriaByFamily().computeIfAbsent(PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, ignored -> new HashSet<>()).add(criterion);
            evidence.criteriaBySource().computeIfAbsent(CONTAINER_SOURCE, ignored -> new HashSet<>()).add(criterion);
        }
        return evidence;
    }

    private static JsonObject advancementJson(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static String matrixJson(String category, String structureLocationType, String advancementId, String criterion, String automationEligibility, String certificationStatus) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"cases\":[{");
        builder.append("\"category\":\"").append(category).append("\",");
        if (structureLocationType != null) {
            builder.append("\"structureLocationType\":\"").append(structureLocationType).append("\",");
        }
        builder.append("\"advancementId\":\"").append(advancementId).append("\",");
        builder.append("\"criterion\":\"").append(criterion).append("\",");
        builder.append("\"automationEligibility\":\"").append(automationEligibility).append("\",");
        builder.append("\"certificationStatus\":\"").append(certificationStatus).append("\"");
        builder.append("}]}");
        return builder.toString();
    }

    private static String artifactJson(String fingerprint, String entryJson) {
        return "{"
            + "\"snapshot\":\"phase_a_runtime_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"matrixFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"run-1\","
            + "\"generatedAt\":\"2026-08-22T00:00:00Z\","
            + "\"entries\":[" + entryJson + "]"
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String family, String source, String result, String matrixCertificationStatus) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + family + "\","
            + "\"source\":\"" + source + "\","
            + "\"result\":\"" + result + "\","
            + "\"matrixCertificationStatus\":\"" + matrixCertificationStatus + "\""
            + "}";
    }

    private static String containerCatalogJson(String... cases) {
        return "{"
            + "\"snapshot\":\"phase_a_container_loot_case_catalog\","
            + "\"cases\":[" + String.join(",", cases) + "]"
            + "}";
    }

    private static String containerCaseJson(String advancementId, String criterion, String lootTable, String automationEligibility) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"lootTable\":\"" + lootTable + "\","
            + "\"automationEligibility\":\"" + automationEligibility + "\""
            + "}";
    }

    private static String containerArtifactJson(String fingerprint, String... entries) {
        return "{"
            + "\"snapshot\":\"phase_a_container_loot_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"container-run-1\","
            + "\"generatedAt\":\"2026-08-22T00:00:00Z\","
            + "\"entries\":[" + String.join(",", entries) + "]"
            + "}";
    }

    private static String containerEntryJson(String advancementId, String criterion, String lootTable, String result) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY + "\","
            + "\"lootTable\":\"" + lootTable + "\","
            + "\"source\":\"" + CONTAINER_SOURCE + "\","
            + "\"result\":\"" + result + "\""
            + "}";
    }

    private static String itemTagArtifactJson(String fingerprint, String runId, String generatedAt, String... entries) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_inventory_changed_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"" + generatedAt + "\","
            + "\"entries\":[" + String.join(",", entries) + "]"
            + "}";
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_inventory_changed_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String enchantmentCatalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(
        String advancementId,
        String criterion,
        String itemTag,
        Integer requiredCountMin,
        Integer requiredCountMax,
        String automationEligibility
    ) {
        String min = requiredCountMin == null ? "null" : requiredCountMin.toString();
        String max = requiredCountMax == null ? "null" : requiredCountMax.toString();
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"requiredCountMin\":" + min + ","
            + "\"requiredCountMax\":" + max + ","
            + "\"automationEligibility\":\"" + automationEligibility + "\""
            + "}";
    }

    private static String enchantmentCaseJson(
        String advancementId,
        String criterion,
        String automationEligibility,
        String[] allowedItems,
        String... enchantments
    ) {
        StringBuilder allowedItemsJson = new StringBuilder();
        for (int i = 0; i < allowedItems.length; i++) {
            if (i > 0) {
                allowedItemsJson.append(",");
            }
            allowedItemsJson.append("\"").append(allowedItems[i]).append("\"");
        }
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"automationEligibility\":\"" + automationEligibility + "\","
            + "\"allowedItems\":[" + allowedItemsJson + "],"
            + "\"enchantmentPredicates\":[" + String.join(",", enchantments) + "]"
            + "}";
    }

    private static String itemTagEntryJson(String advancementId, String criterion, String itemTag, String selectedItem, int requiredCount, String result) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + ITEM_TAG_FAMILY + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"requiredCount\":" + requiredCount + ","
            + "\"source\":\"" + ITEM_TAG_SOURCE + "\","
            + "\"result\":\"" + result + "\""
            + "}";
    }

    private static String enchantmentPredicateJson(String selector, Integer minLevel, Integer maxLevel, String storageType) {
        return "{"
            + "\"selector\":\"" + selector + "\","
            + "\"storageType\":\"" + storageType + "\","
            + "\"minLevel\":" + (minLevel == null ? "null" : minLevel) + ","
            + "\"maxLevel\":" + (maxLevel == null ? "null" : maxLevel)
            + "}";
    }

    private static String enchantmentArtifactJson(String fingerprint, String... entries) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"enchantment-run-1\","
            + "\"generatedAt\":\"2026-08-23T00:00:00Z\","
            + "\"entries\":[" + String.join(",", entries) + "]"
            + "}";
    }

    private static String placedBlockCatalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_placed_block_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String placedBlockCaseJson(String advancementId, String criterion, String itemTag, String automationEligibility) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"automationEligibility\":\"" + automationEligibility + "\""
            + "}";
    }

    private static String placedBlockArtifactJson(String fingerprint, String runId, String... entries) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_placed_block_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"2026-08-23T00:00:00Z\","
            + "\"entries\":[" + String.join(",", entries) + "]"
            + "}";
    }

    private static String placedBlockEntryJson(String advancementId, String criterion, String itemTag, String selectedItem, String placedBlock, String fingerprint, String runId) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"placedBlock\":\"" + placedBlock + "\","
            + "\"family\":\"" + PLACED_BLOCK_FAMILY + "\","
            + "\"source\":\"" + PLACED_BLOCK_SOURCE + "\","
            + "\"result\":\"GREEN\","
            + "\"criterionBefore\":false,"
            + "\"criterionAfter\":true,"
            + "\"handCountBefore\":1,"
            + "\"handCountAfter\":0,"
            + "\"interactionResult\":\"Success[swingSource=CLIENT, itemContext=ItemContext[wasItemInteraction=true, heldItemTransformedTo=null]]\","
            + "\"targetPos\":\"1, 2, 1\","
            + "\"supportPos\":\"1, 1, 1\","
            + "\"placedBlockBefore\":\"minecraft:air\","
            + "\"placedBlockAfter\":\"" + placedBlock + "\","
            + "\"tagMembership\":true,"
            + "\"tagMemberCount\":13,"
            + "\"ticksToCriterion\":0,"
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\""
            + "}";
    }

    private static String enchantmentEntryJson(String advancementId, String criterion, String selectedItem, String result, String... enchantments) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + ENCHANTMENT_FAMILY + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"enchantments\":[" + String.join(",", enchantments) + "],"
            + "\"source\":\"" + ENCHANTMENT_SOURCE + "\","
            + "\"result\":\"" + result + "\","
            + "\"inventoryBefore\":\"[]\","
            + "\"inventoryAfter\":\"[0=ItemStack]\","
            + "\"itemEntityBefore\":\"alive=true\","
            + "\"itemEntityAfter\":\"alive=false\","
            + "\"ticksToPickup\":2"
            + "}";
    }

    private static String enchantmentConfiguredEntryJson(String selector, int level, String storageType) {
        return "{"
            + "\"selector\":\"" + selector + "\"," 
            + "\"level\":" + level + ","
            + "\"storageType\":\"" + storageType + "\""
            + "}";
    }

    private static JsonObject generatePreUsingItemPromotionSnapshot() throws IOException {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence =
            PhaseAAdvancementRollup.loadRuntimeEvidence(PROJECT_ROOT);
        for (String key : PhaseAUsingItemCertification.EXPECTED_KEYS) {
            evidence.remove(key.substring(0, key.indexOf('#')));
        }
        for (String key : PhaseAEntityTypeTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS) {
            evidence.remove(key.substring(0, key.indexOf('#')));
        }
        removeTameAnimalLlamaEvidence(evidence);
        removeRecipeCraftedWaxOnEvidence(evidence);
        removeItemTagInventoryContainmentEvidence(evidence);
        removeFishingRodHookedEvidence(evidence);
        removeTrimMaterialInventoryChangedEvidence(evidence);
        evidence.remove(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID);
        evidence.remove(PhaseASingletonRedstoneClickCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonMangroveGroveCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonDiagonAllayCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonVillageBellCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonMaximumResistanceCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonAncientRestorationCertification.ADVANCEMENT);
        evidence.remove(PhaseASingletonSilkTouchNestCertification.ADVANCEMENT);
        PhaseAPureEnchantmentInventoryChangedCertification.FROZEN.keySet().forEach(evidence::remove);
        PhaseAPlayerKilledEntityRemainderCertification.FROZEN.keySet().forEach(evidence::remove);
        PhaseALocationHolderSetWorldgenCertification.FROZEN.keySet().forEach(evidence::remove);
        PhaseAMixedPiglinDistractionCertification.FROZEN.keySet().forEach(evidence::remove);
        PhaseAMixedWeaponryMulticlassedCertification.FROZEN.keySet().forEach(evidence::remove);
        PhaseAMixedPerfectRunCertification.FROZEN.keySet().forEach(evidence::remove);
        restoreHistoricalItemTagStackSlice(evidence);
        return JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(PROJECT_ROOT, evidence)).getAsJsonObject();
    }

    private static void restoreHistoricalItemTagStackSlice(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) throws IOException {
        // Historical pre-backlog fixtures retain the original accepted smithing-template slice.
        evidence.put(PhaseAStackAllItemsCertification.ADVANCEMENT,
            PhaseAAdvancementRollup.adaptItemTagEvidence(
                PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(PROJECT_ROOT)
            ).get(PhaseAStackAllItemsCertification.ADVANCEMENT));
    }

    private static void removeTameAnimalLlamaEvidence(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) {
        for (PhaseATameAnimalLlamaCertification.Case definition : PhaseATameAnimalLlamaCertification.CASES) {
            evidence.remove(definition.advancementId());
        }
    }

    private static void removeRecipeCraftedWaxOnEvidence(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) {
        evidence.remove(PhaseARecipeCraftedWaxOnCertification.ADVANCEMENT_ID);
    }

    private static void removeItemTagInventoryContainmentEvidence(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) {
        for (PhaseAItemTagInventoryContainmentCertification.Case definition : PhaseAItemTagInventoryContainmentCertification.CASES) {
            evidence.remove(definition.advancementId());
        }
    }

    private static void removeFishingRodHookedEvidence(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) {
        evidence.remove(PhaseAFishingRodHookedCertification.ADVANCEMENT_ID);
    }

    private static void removeTrimMaterialInventoryChangedEvidence(Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence) {
        for (var definition : PhaseATrimMaterialInventoryChangedCertification.cases()) evidence.remove(definition.advancementId());
    }

    private static JsonObject findEntry(JsonArray entries, String id) {
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            if (id.equals(entry.get("id").getAsString())) {
                return entry;
            }
        }
        throw new AssertionError("Missing entry " + id);
    }

    private static void assertArrayEquals(Set<String> expected, JsonArray actual) {
        Set<String> actualValues = asSet(actual);
        assertEquals(expected, actualValues);
        assertEquals(expected.size(), actual.size());
    }

    private static Set<String> asSet(JsonArray actual) {
        Set<String> actualValues = new HashSet<>();
        for (JsonElement element : actual) {
            actualValues.add(element.getAsString());
        }
        return actualValues;
    }
}
