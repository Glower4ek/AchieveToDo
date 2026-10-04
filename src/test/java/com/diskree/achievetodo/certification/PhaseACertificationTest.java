package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseACertificationTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void canonicalInventorySnapshotMatchesGeneratedArtifacts() throws IOException {
        PhaseACertification.CertificationArtifacts artifacts = PhaseACertification.generate(PROJECT_ROOT,
            Path.of("src/test/resources/phase_a_certification/frozen_runtime_ru_overlay.json"),
            Path.of("src/test/resources/phase_a_certification/frozen_internal_resourcepacks.zip"));
        Path inventory = PROJECT_ROOT.resolve(PhaseACertification.INVENTORY_SNAPSHOT);
        assertTrue(Files.exists(inventory), "Canonical inventory snapshot must be generated and checked in");
        assertEquals(
            artifacts.inventoryJson(),
            Files.readString(inventory, StandardCharsets.UTF_8),
            "Canonical advancement inventory snapshot is stale. Run writePhaseACertificationAssets."
        );
    }

    @Test
    void canonicalInventoryCurrentBindingsMatchPhaseB() throws IOException {
        var current = com.google.gson.JsonParser.parseString(PhaseACertification.generate(PROJECT_ROOT).inventoryJson()).getAsJsonObject();
        var historical = com.google.gson.JsonParser.parseString(Files.readString(PROJECT_ROOT.resolve(PhaseACertification.INVENTORY_SNAPSHOT))).getAsJsonObject();
        var oldEntries = historical.remove("entries").getAsJsonArray();
        var newEntries = current.remove("entries").getAsJsonArray();
        assertEquals(historical, current, "Every non-entry inventory field remains exact");
        assertEquals(1152, oldEntries.size()); assertEquals(oldEntries.size(), newEntries.size());
        var b6 = com.google.gson.JsonParser.parseString(Files.readString(Path.of("reference/phase_b/b6_ru_translation_manifest.json"))).getAsJsonObject();
        Set<String> removed = new LinkedHashSet<>();
        for (var row : b6.getAsJsonArray("actualRemovals")) {
            assertEquals(0, row.getAsJsonObject().get("consumerCount").getAsInt());
            removed.add(row.getAsJsonObject().get("key").getAsString());
        }
        int locale = 0, owner = 0;
        for (int i = 0; i < oldEntries.size(); i++) {
            var before = oldEntries.get(i).getAsJsonObject(); var after = newEntries.get(i).getAsJsonObject();
            assertEquals(before.keySet(), after.keySet()); assertEquals(before.get("id"), after.get("id"));
            for (String field : before.keySet()) {
                if (before.get(field).equals(after.get(field))) continue;
                if (field.equals("titleKeyPresentInRuntimeRussian") || field.equals("descriptionKeyPresentInRuntimeRussian")) {
                    assertTrue(before.get(field).getAsBoolean()); assertFalse(after.get(field).getAsBoolean());
                    String key = before.get(field.startsWith("title") ? "titleKey" : "descriptionKey").getAsString();
                    assertTrue(removed.contains(key), key); locale++;
                } else {
                    assertEquals("rewardFunctionSources", field); assertEquals("minecraft:story/root", before.get("id").getAsString());
                    var expectedBefore = new com.google.gson.JsonObject();
                    expectedBefore.addProperty("bacap_rewards:bacap/benchmarking", "bacap.zip::data/bacap_rewards/function/bacap/benchmarking.mcfunction");
                    assertEquals(expectedBefore, before.get(field));
                    var expectedAfter = new com.google.gson.JsonObject();
                    expectedAfter.addProperty("bacap_rewards:bacap/benchmarking", "bacap_override::" + Path.of("bacap_override/data/bacap_rewards/function/bacap/benchmarking.mcfunction"));
                    assertEquals(expectedAfter, after.get(field)); owner++;
                }
            }
        }
        assertEquals(21, locale); assertEquals(1, owner); assertEquals(22, locale + owner);
    }

    @Test
    void runtimeMatrixSnapshotMatchesGeneratedArtifacts() throws IOException {
        PhaseACertification.CertificationArtifacts artifacts = PhaseACertification.generate(PROJECT_ROOT);
        Path matrix = PROJECT_ROOT.resolve(PhaseACertification.MATRIX_SNAPSHOT);
        assertTrue(Files.exists(matrix), "Runtime matrix snapshot must be generated and checked in");
        assertEquals(
            artifacts.matrixJson(),
            Files.readString(matrix, StandardCharsets.UTF_8),
            "Runtime test matrix snapshot is stale. Run writePhaseACertificationAssets."
        );
    }

    @Test
    void staticCertificationRetainsExactAcceptedUnresolvedDebt() throws IOException {
        var report = PhaseACertification.analyzeStaticRules(PROJECT_ROOT);
        Set<String> failures = report.entries().stream()
            .filter(entry -> entry.status() == PhaseACertification.StaticValidationStatus.STATIC_FAIL)
            .map(PhaseACertification.StaticValidationEntry::id).collect(Collectors.toSet());
        assertEquals(Set.of(
            "blazeandcave:biomes/boatception", "blazeandcave:biomes/the_mighty_jungle",
            "blazeandcave:building/happy_birthday", "blazeandcave:building/setting_up_the_mood",
            "blazeandcave:building/washing_machine", "blazeandcave:enchanting/let_it_go",
            "blazeandcave:enchanting/master_armorer", "blazeandcave:enchanting/master_axeman",
            "blazeandcave:enchanting/master_digger", "blazeandcave:enchanting/master_farmer",
            "blazeandcave:enchanting/master_knight", "blazeandcave:enchanting/master_miner",
            "blazeandcave:redstone/travelling_bard", "minecraft:husbandry/complete_catalogue",
            "minecraft:husbandry/leash_all_frog_variants", "minecraft:husbandry/wax_off",
            "minecraft:husbandry/whole_pack"
        ), failures);
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> PhaseACertification.certifyStaticRules(PROJECT_ROOT)).getMessage().startsWith("STATIC_FAIL"));
    }

    @Test
    void locationHolderSetFamilyReconcilesByExactIdSets() throws IOException {
        PhaseACertification.HolderSetFamilyReport familyReport = PhaseACertification.analyzeLocationHolderSetFamily(PROJECT_ROOT);
        assertEquals(
            familyReport.familyTotal(),
            familyReport.biomeOnlyIds().size()
                + familyReport.structureOnlyIds().size()
                + familyReport.bothIds().size()
                + familyReport.otherLocationHolderIds().size(),
            "Location HolderSet family set algebra must reconcile exactly"
        );
        assertEquals(familyReport.unionIds().size(), familyReport.familyTotal() - familyReport.otherLocationHolderIds().size());
    }

    @Test
    void locationHolderSetControlCasesDeferByStructureAndContext() throws IOException {
        assertHolderSetControlCase(
            "minecraft:nether/explore_nether",
            PhaseACertification.RegistryDependentComponent.BIOME_HOLDERSET
        );
        assertHolderSetControlCase(
            "minecraft:story/follow_ender_eye",
            PhaseACertification.RegistryDependentComponent.STRUCTURE_HOLDERSET
        );
        assertHolderSetControlCase(
            "minecraft:adventure/play_jukebox_in_meadows",
            PhaseACertification.RegistryDependentComponent.BIOME_HOLDERSET
        );
        assertHolderSetControlCase(
            "blazeandcave:adventure/oh_look_it_dings",
            PhaseACertification.RegistryDependentComponent.STRUCTURE_HOLDERSET
        );
    }

    @Test
    void staticCertificationTriStateStillCoversCanonicalInventory() throws IOException {
        PhaseACertification.StaticCertificationReport report = PhaseACertification.analyzeStaticRules(PROJECT_ROOT);
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, report.entries().size());
        assertEquals(
            PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS,
            report.staticPassCount() + report.staticFailCount() + report.runtimeDeferredCount()
        );
    }

    @Test
    void bannerPatternCriterionControlCaseNowPassesStaticValidation() throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(
            PROJECT_ROOT,
            "blazeandcave:adventure/superhero_of_the_village"
        );
        // A successful codec parse has no diagnostic entry; the full report owns STATIC_PASS.
        assertEquals(null, controlCase.validationEntry());
        var entry = PhaseACertification.analyzeStaticRules(PROJECT_ROOT).entries().stream()
            .filter(row -> row.id().equals(controlCase.advancementId())).findFirst().orElseThrow();
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, entry.status());
    }

    @Test
    void bannerPatternDisplayOnlyControlCaseNowPassesStaticValidation() throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(
            PROJECT_ROOT,
            "blazeandcave:adventure/riot_shield"
        );
        // A successful codec parse has no diagnostic entry; the full report owns STATIC_PASS.
        assertEquals(null, controlCase.validationEntry());
        var entry = PhaseACertification.analyzeStaticRules(PROJECT_ROOT).entries().stream()
            .filter(row -> row.id().equals(controlCase.advancementId())).findFirst().orElseThrow();
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, entry.status());
    }

    @Test
    void instrumentRegistryContextControlCaseNowPassesStaticValidation() throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(
            PROJECT_ROOT,
            "blazeandcave:animal/caprymphony"
        );
        // A successful codec parse has no diagnostic entry; the full report owns STATIC_PASS.
        assertEquals(null, controlCase.validationEntry());
        var entry = PhaseACertification.analyzeStaticRules(PROJECT_ROOT).entries().stream()
            .filter(row -> row.id().equals(controlCase.advancementId())).findFirst().orElseThrow();
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, entry.status());
    }

    @Test
    void allowedVanillaRegistryFallbackEligibilityRemainsStrictlySingletonAndPure() {
        assertTrue(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
        )));
        assertTrue(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:instrument]"
        )));
        assertFalse(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]; Not a json array: \"minecraft:sharpness\""
        )));
        assertFalse(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:instrument]; Not a json array: \"minecraft:sharpness\""
        )));
        assertFalse(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]; Registry does not exist: ResourceKey[minecraft:root / minecraft:instrument]"
        )));
        assertFalse(PhaseACertification.isPureAllowedVanillaRegistryContextFailure(List.of(
            "Registry does not exist: ResourceKey[minecraft:root / minecraft:structure]"
        )));
    }

    @Test
    void tagClassifierPostFixAuditUsesExactBaseStackAndDisjointFamilies() throws IOException {
        PhaseACertification.StaticCertificationReport report = PhaseACertification.analyzeStaticRules(PROJECT_ROOT);
        PhaseACertification.TagContextSourceStackReport sourceStackReport = PhaseACertification.analyzeTagContextSourceStack(PROJECT_ROOT);
        PhaseACertification.MissingTagDisjointReport disjointReport = PhaseACertification.analyzeMissingTagDisjoint(PROJECT_ROOT);
        PhaseACertification.MissingTagFamilyReport familyReport = PhaseACertification.analyzeMissingTagFamily(PROJECT_ROOT);
        PhaseACertification.RemainingStaticFailBreakdownReport staticFailBreakdown = PhaseACertification.analyzeRemainingStaticFailBreakdown(PROJECT_ROOT);
        PhaseACertification.MissingTagNegativeControlReport negativeControl = PhaseACertification.analyzeNegativeMissingTagControl(PROJECT_ROOT);
        long sourceStackSum = sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.VANILLA_26_2)
            + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.BACAP_BASE)
            + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.BASE_INTERNAL_PACK)
            + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.OPTIONAL_VARIANT_ONLY)
            + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.MULTIPLE);
        long disjointSum = disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.TAG_CONTEXT_DEFERRED)
            + disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.ACTUALLY_MISSING_RUNTIME_TAG)
            + disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.MIXED_OR_OTHER_STATIC_FAIL);
        Map<PhaseACertification.MissingTagStaticFailReason, Long> missingTagStaticFailReasons = disjointReport.entries().stream()
            .filter(entry -> entry.staticFailReason() != null)
            .collect(Collectors.groupingBy(PhaseACertification.MissingTagAdvancementClassification::staticFailReason, Collectors.counting()));
        Map<String, String> staleSources = familyReport.entries().stream()
            .filter(entry -> List.of(
                "minecraft:non_underwater_blocks",
                "minecraft:nether_fungus",
                "minecraft:nether_roots"
            ).contains(entry.tagId()))
            .collect(Collectors.toMap(
                PhaseACertification.MissingTagFamilyEntry::tagId,
                PhaseACertification.MissingTagFamilyEntry::runtimeTagSource,
                (left, right) -> left,
                java.util.LinkedHashMap::new
            ));
        PhaseACertification.StaticFailFamily nextSystemicFamily = List.of(
                PhaseACertification.StaticFailFamily.REGISTRY_MISSING,
                PhaseACertification.StaticFailFamily.JSON_ARRAY_SHAPE,
                PhaseACertification.StaticFailFamily.ACTUALLY_MISSING_TAG,
                PhaseACertification.StaticFailFamily.MIXED_CODEC_ERROR,
                PhaseACertification.StaticFailFamily.OTHER
            ).stream()
            .max(Comparator.comparingLong(staticFailBreakdown::countByFamily))
            .orElseThrow();
        assertEquals(sourceStackReport.deferredTagsTotal(), sourceStackSum, "Source provenance counts must partition audited deferred advancements exactly");
        assertEquals(disjointReport.totalAdvancements(), disjointSum, "Missing-tag disjoint partition must reconcile exactly");
        assertEquals(
            report.staticFailCount(),
            staticFailBreakdown.entries().size(),
            "Remaining static-fail breakdown must classify every STATIC_FAIL advancement exactly once"
        );
        if (negativeControl.available()) {
            assertTrue(negativeControl.tagReferencePreserved(), "Negative control must preserve the exact missing tag reference through normalization");
            assertTrue(!negativeControl.baseRuntimeExists(), "Negative control must point at a tag missing from the exact base runtime source stack");
            assertEquals(PhaseACertification.StaticValidationStatus.STATIC_FAIL, negativeControl.finalStatus());
            assertEquals(null, negativeControl.deferredReason(), "Negative control must remain a plain STATIC_FAIL");
            assertEquals(null, negativeControl.registryComponent(), "Negative control must not be masked by deferred registry classification");
            assertTrue(negativeControl.exactCodecError().contains("Missing tag:"), "Negative control must still fail on an exact missing-tag codec diagnostic");
        }
        System.out.println("TRI_STATE_PASS=" + report.staticPassCount());
        System.out.println("TRI_STATE_FAIL=" + report.staticFailCount());
        System.out.println("TRI_STATE_DEFERRED=" + report.runtimeDeferredCount());
        System.out.println("SOURCE_STACK_TOTAL=" + sourceStackReport.deferredTagsTotal());
        System.out.println("SOURCE_STACK_VANILLA_26_2=" + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.VANILLA_26_2));
        System.out.println("SOURCE_STACK_BACAP_BASE=" + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.BACAP_BASE));
        System.out.println("SOURCE_STACK_BASE_INTERNAL_PACK=" + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.BASE_INTERNAL_PACK));
        System.out.println("SOURCE_STACK_OPTIONAL_VARIANT_ONLY=" + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.OPTIONAL_VARIANT_ONLY));
        System.out.println("SOURCE_STACK_MULTIPLE=" + sourceStackReport.countByProvenance(PhaseACertification.SourceProvenance.MULTIPLE));
        System.out.println("CLASSIFIER_SOURCE_INDEX_CHANGED=YES");
        System.out.println("CLASSIFIER_TAG_CONTEXT_DEFERRED_VALID=" + sourceStackReport.validDeferredCount());
        System.out.println("CLASSIFIER_OPTIONAL_PACK_FALSE_POSITIVE=" + sourceStackReport.optionalPackFalsePositiveIds().size());
        System.out.println("CLASSIFIER_BASE_INTERNAL_FALSE_NEGATIVE=" + sourceStackReport.baseInternalFalseNegativeIds().size());
        System.out.println("CLASSIFIER_OPTIONAL_FALSE_POSITIVE_IDS=" + String.join(",", sourceStackReport.optionalPackFalsePositiveIds().stream().sorted().toList()));
        System.out.println("CLASSIFIER_BASE_INTERNAL_FALSE_NEGATIVE_IDS=" + String.join(",", sourceStackReport.baseInternalFalseNegativeIds().stream().sorted().toList()));
        System.out.println("MISSING_TAG_TOTAL=" + disjointReport.totalAdvancements());
        System.out.println("MISSING_TAG_TAG_CONTEXT_DEFERRED=" + disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.TAG_CONTEXT_DEFERRED));
        System.out.println("MISSING_TAG_ACTUALLY_MISSING_RUNTIME_TAG=" + disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.ACTUALLY_MISSING_RUNTIME_TAG));
        System.out.println("MISSING_TAG_MIXED_OR_OTHER_STATIC_FAIL=" + disjointReport.countByDisposition(PhaseACertification.MissingTagDisposition.MIXED_OR_OTHER_STATIC_FAIL));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_ACTUALLY_MISSING_RUNTIME_TAG=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.ACTUALLY_MISSING_RUNTIME_TAG, 0L));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_MIXED_CODEC_ERROR=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.MIXED_CODEC_ERROR, 0L));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_TAG_REFERENCE_NOT_PRESERVED=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.TAG_REFERENCE_NOT_PRESERVED, 0L));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_JUNIT_ALREADY_RESOLVES=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.JUNIT_ALREADY_RESOLVES, 0L));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_UNSUPPORTED_TAG_REGISTRY=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.UNSUPPORTED_TAG_REGISTRY, 0L));
        System.out.println("MISSING_TAG_STATIC_FAIL_REASON_OTHER_UNCLASSIFIED=" + missingTagStaticFailReasons.getOrDefault(PhaseACertification.MissingTagStaticFailReason.OTHER_UNCLASSIFIED, 0L));
        System.out.println("NEGATIVE_CONTROL_ADVANCEMENT=" + negativeControl.advancementId());
        System.out.println("NEGATIVE_CONTROL_TAG=" + negativeControl.tagId());
        System.out.println("NEGATIVE_CONTROL_ASSERTION_ADDED=" + negativeControl.available());
        System.out.println("NEGATIVE_CONTROL_BASE_RUNTIME_EXISTS=" + negativeControl.baseRuntimeExists());
        System.out.println("NEGATIVE_CONTROL_FINAL_STATUS=" + negativeControl.finalStatus());
        System.out.println("PREVIOUS_STALE_non_underwater_blocks=" + staleSources.getOrDefault("minecraft:non_underwater_blocks", ""));
        System.out.println("PREVIOUS_STALE_nether_fungus=" + staleSources.getOrDefault("minecraft:nether_fungus", ""));
        System.out.println("PREVIOUS_STALE_nether_roots=" + staleSources.getOrDefault("minecraft:nether_roots", ""));
        System.out.println("STATIC_FAIL_REGISTRY_MISSING_TOTAL=" + staticFailBreakdown.countByFamily(PhaseACertification.StaticFailFamily.REGISTRY_MISSING));
        System.out.println("STATIC_FAIL_REGISTRY_MISSING_BANNER_PATTERN=" + staticFailBreakdown.countByRegistrySubtype(PhaseACertification.RegistryMissingSubtype.BANNER_PATTERN));
        System.out.println("STATIC_FAIL_REGISTRY_MISSING_INSTRUMENT=" + staticFailBreakdown.countByRegistrySubtype(PhaseACertification.RegistryMissingSubtype.INSTRUMENT));
        System.out.println("STATIC_FAIL_REGISTRY_MISSING_OTHER=" + staticFailBreakdown.countByRegistrySubtype(PhaseACertification.RegistryMissingSubtype.OTHER));
        System.out.println("STATIC_FAIL_JSON_ARRAY_SHAPE_TOTAL=" + staticFailBreakdown.countByFamily(PhaseACertification.StaticFailFamily.JSON_ARRAY_SHAPE));
        System.out.println("STATIC_FAIL_JSON_ARRAY_ITEM_ENTITY_PREDICATE_SHAPE=" + staticFailBreakdown.countByJsonArraySubtype(PhaseACertification.JsonArraySubtype.ITEM_ENTITY_PREDICATE_SHAPE));
        System.out.println("STATIC_FAIL_JSON_ARRAY_OTHER=" + staticFailBreakdown.countByJsonArraySubtype(PhaseACertification.JsonArraySubtype.OTHER_JSON_ARRAY));
        System.out.println("STATIC_FAIL_ACTUALLY_MISSING_TAG=" + staticFailBreakdown.countByFamily(PhaseACertification.StaticFailFamily.ACTUALLY_MISSING_TAG));
        System.out.println("STATIC_FAIL_MIXED_CODEC_ERROR=" + staticFailBreakdown.countByFamily(PhaseACertification.StaticFailFamily.MIXED_CODEC_ERROR));
        System.out.println("STATIC_FAIL_OTHER=" + staticFailBreakdown.countByFamily(PhaseACertification.StaticFailFamily.OTHER));
        System.out.println("NEXT_SYSTEMIC_FAMILY=" + nextSystemicFamily);
    }

    @Test
    void missingTagControlCasesRespectStrictTagContextContract() throws IOException {
        assertTagControlCase(
            "blazeandcave:adventure/from_under_your_feet",
            "minecraft:trapdoors",
            "minecraft:block",
            PhaseACertification.RegistryDependentComponent.BLOCK_TAG
        );
        assertTagControlCase(
            "minecraft:husbandry/make_a_sign_glow",
            "minecraft:all_signs",
            "minecraft:block",
            PhaseACertification.RegistryDependentComponent.BLOCK_TAG
        );
    }

    @Test
    void enchantmentPredicateCompatibilityFixMovesExactExpectedTwentyAndKeepsControlsStable() throws IOException {
        PhaseACertification.StaticCertificationReport report = PhaseACertification.analyzeStaticRules(PROJECT_ROOT);
        assertEquals(958, report.staticPassCount());
        assertEquals(177, report.runtimeDeferredCount());
        assertEquals(17, report.staticFailCount());
        Map<String, PhaseACertification.StaticValidationEntry> byId = report.entries().stream()
            .collect(Collectors.toMap(PhaseACertification.StaticValidationEntry::id, entry -> entry, (left, right) -> left));
        Set<String> expectedGroupADeferred = Set.of(
            "blazeandcave:challenges/ultimate_enchanter",
            "blazeandcave:enchanting/armor_for_the_masses",
            "blazeandcave:enchanting/bane_of_one_shotting_spiders",
            "blazeandcave:enchanting/bow_down_to_me",
            "blazeandcave:enchanting/complete_enchanter",
            "blazeandcave:enchanting/curses",
            "blazeandcave:enchanting/fiery",
            "blazeandcave:enchanting/fortunate_son",
            "blazeandcave:enchanting/gotta_go_fast",
            "blazeandcave:enchanting/knocking_your_socks_off",
            "blazeandcave:enchanting/master_arbalist",
            "blazeandcave:enchanting/master_enchanter",
            "blazeandcave:enchanting/master_macerator",
            "blazeandcave:enchanting/master_sniper",
            "blazeandcave:enchanting/master_tridenteer",
            "blazeandcave:enchanting/needle_sharp",
            "blazeandcave:enchanting/scuba_gear",
            "blazeandcave:enchanting/super_efficient",
            "blazeandcave:enchanting/undead_slayer",
            "blazeandcave:monsters/maximum_resistance"
        );
        Set<String> expectedPureGroupBDeferred = Set.of(
            "blazeandcave:enchanting/a_rather_pointy_fence_post",
            "blazeandcave:enchanting/boomerang",
            "blazeandcave:enchanting/do_a_barrel_roll",
            "blazeandcave:enchanting/god_of_thunder",
            "blazeandcave:enchanting/handmade_blinding",
            "blazeandcave:enchanting/like_a_cat",
            "blazeandcave:enchanting/like_a_ninja",
            "blazeandcave:enchanting/mace_windu",
            "blazeandcave:enchanting/machine_bow",
            "blazeandcave:enchanting/master_fisher",
            "blazeandcave:enchanting/newtons_flaming_laser_sword",
            "blazeandcave:enchanting/shotbow",
            "blazeandcave:enchanting/silent_but_deadly",
            "blazeandcave:enchanting/to_infinity_and_beyond",
            "blazeandcave:enchanting/unbreakable",
            "blazeandcave:enchanting/zeus",
            "blazeandcave:mining/the_mistake",
            "blazeandcave:nether/stepping_on_legos",
            "minecraft:husbandry/silk_touch_nest"
        );
        Map<String, Set<PhaseACertification.RegistryDependentComponent>> expectedMultiDeferred = Map.of(
            "blazeandcave:nether/instant_mining", Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.ITEM_TAG
            ),
            "blazeandcave:nether/ludicrous_speed", Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.BLOCK_TAG
            ),
            "blazeandcave:nether/soul_runnings", Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.BLOCK_TAG
            )
        );
        Set<String> actualEnchantmentHolderSet = new LinkedHashSet<>();
        for (String advancementId : expectedGroupADeferred) {
            PhaseACertification.StaticValidationEntry entry = byId.get(advancementId);
            assertNotNull(entry, "Expected enchantment-shape advancement missing from static report: " + advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, entry.status(), advancementId);
            assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, entry.deferredReason(), advancementId);
            assertEquals(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET, entry.registryComponent(), advancementId);
            assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET), entry.registryComponents(), advancementId);
            actualEnchantmentHolderSet.add(advancementId);
        }
        for (String advancementId : expectedPureGroupBDeferred) {
            PhaseACertification.StaticValidationEntry entry = byId.get(advancementId);
            assertNotNull(entry, "Expected stable enchantment-holder-set advancement missing from static report: " + advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, entry.status(), advancementId);
            assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, entry.deferredReason(), advancementId);
            assertEquals(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET, entry.registryComponent(), advancementId);
            assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET), entry.registryComponents(), advancementId);
            actualEnchantmentHolderSet.add(advancementId);
        }
        for (Map.Entry<String, Set<PhaseACertification.RegistryDependentComponent>> expectedMulti : expectedMultiDeferred.entrySet()) {
            PhaseACertification.StaticValidationEntry entry = byId.get(expectedMulti.getKey());
            assertNotNull(entry, "Expected multi-context advancement missing from static report: " + expectedMulti.getKey());
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, entry.status(), expectedMulti.getKey());
            assertEquals(PhaseACertification.DeferredReason.MULTIPLE_CONTEXT_REQUIRED, entry.deferredReason(), expectedMulti.getKey());
            assertEquals(expectedMulti.getValue(), entry.registryComponents(), expectedMulti.getKey());
            assertEquals(null, entry.registryComponent(), "Compatibility accessor must stay null for multi-context entries: " + expectedMulti.getKey());
        }
        for (String advancementId : List.of("minecraft:adventure/voluntary_exile", "minecraft:story/deflect_arrow")) {
            PhaseACertification.StaticValidationEntry entry = byId.get(advancementId);
            assertNotNull(entry, "Expected fallback-plus-tag advancement missing from static report: " + advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, entry.status(), advancementId);
            assertEquals(PhaseACertification.DeferredReason.TAG_CONTEXT_REQUIRED, entry.deferredReason(), advancementId);
            assertEquals(PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG, entry.registryComponent(), advancementId);
            assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG), entry.registryComponents(), advancementId);
        }
        Set<String> expectedUnion = new LinkedHashSet<>(expectedGroupADeferred);
        expectedUnion.addAll(expectedPureGroupBDeferred);
        assertEquals(20, expectedGroupADeferred.size());
        assertEquals(19, expectedPureGroupBDeferred.size());
        assertEquals(39, expectedUnion.size());
        assertEquals(expectedUnion, byId.values().stream()
            .filter(entry -> entry.status() == PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED)
            .filter(entry -> entry.deferredReason() == PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED)
            .filter(entry -> entry.registryComponent() == PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET)
            .map(PhaseACertification.StaticValidationEntry::id)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
        assertEquals(expectedUnion, actualEnchantmentHolderSet);

        PhaseACertification.StaticValidationEntry letItGo = byId.get("blazeandcave:enchanting/let_it_go");
        assertNotNull(letItGo);
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_FAIL, letItGo.status());
        assertNotNull(letItGo.exactCodecError());
        assertFalse(letItGo.exactCodecError().contains("Not a json array: \"minecraft:frost_walker\""));
        assertTrue(letItGo.exactCodecError().contains("Not a json array: \"minecraft:deep_"));

        PhaseACertification.StaticValidationEntry travellingBard = byId.get("blazeandcave:redstone/travelling_bard");
        assertNotNull(travellingBard);
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_FAIL, travellingBard.status());
        assertNotNull(travellingBard.exactCodecError());
        assertTrue(travellingBard.exactCodecError().contains("pale_garden"));

        Set<String> expectedBannerPasses = Set.of(
            "blazeandcave:adventure/i_am_ravager_hear_me_roar",
            "blazeandcave:adventure/riot_shield",
            "blazeandcave:adventure/superhero_of_the_village",
            "blazeandcave:adventure/the_shielding",
            "blazeandcave:animal/battle_of_the_bands",
            "blazeandcave:biomes/captain_america",
            "blazeandcave:end/dragon_shield",
            "blazeandcave:end/shouldnt_my_shield_levitate_too",
            "blazeandcave:monsters/blast_shield",
            "blazeandcave:monsters/ricochet_swoop",
            "blazeandcave:monsters/tridented_shield",
            "blazeandcave:nether/fire_blast_shield",
            "blazeandcave:nether/the_nethers_shield",
            "blazeandcave:weaponry/loser"
        );
        Set<String> actualBannerPasses = new LinkedHashSet<>();
        for (String advancementId : expectedBannerPasses) {
            PhaseACertification.StaticValidationEntry entry = byId.get(advancementId);
            assertNotNull(entry, "Expected banner-pattern advancement missing from static report: " + advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, entry.status(), advancementId);
            actualBannerPasses.add(advancementId);
        }
        assertEquals(expectedBannerPasses, actualBannerPasses);
        Set<String> expectedInstrumentPasses = Set.of("blazeandcave:animal/caprymphony");
        Set<String> actualInstrumentPasses = new LinkedHashSet<>();
        for (String advancementId : expectedInstrumentPasses) {
            PhaseACertification.StaticValidationEntry entry = byId.get(advancementId);
            assertNotNull(entry, "Expected instrument advancement missing from static report: " + advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, entry.status(), advancementId);
            actualInstrumentPasses.add(advancementId);
        }
        assertEquals(expectedInstrumentPasses, actualInstrumentPasses);

        PhaseACertification.ControlCaseAnalysis deferredControl = PhaseACertification.analyzeControlCase(
            PROJECT_ROOT,
            "minecraft:story/follow_ender_eye"
        );
        assertNotNull(deferredControl.validationEntry(), "Deferred control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, deferredControl.validationEntry().status());
        assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, deferredControl.validationEntry().deferredReason());
        assertEquals(PhaseACertification.RegistryDependentComponent.STRUCTURE_HOLDERSET, deferredControl.validationEntry().registryComponent());
    }

    @Test
    void enchantmentPredicateCompatibilityControlCasesCoverSimpleMasterEquipmentAndNegativeControls() throws IOException {
        assertEnchantmentHolderSetDeferredControlCase("blazeandcave:enchanting/needle_sharp");
        assertEnchantmentHolderSetDeferredControlCase("blazeandcave:enchanting/master_arbalist");
        assertEnchantmentHolderSetDeferredControlCase("blazeandcave:monsters/maximum_resistance");
        assertMultiContextDeferredControlCase(
            "blazeandcave:nether/instant_mining",
            Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.ITEM_TAG
            )
        );
        assertMultiContextDeferredControlCase(
            "blazeandcave:nether/ludicrous_speed",
            Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.BLOCK_TAG
            )
        );
        assertMultiContextDeferredControlCase(
            "blazeandcave:nether/soul_runnings",
            Set.of(
                PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET,
                PhaseACertification.RegistryDependentComponent.BLOCK_TAG
            )
        );
        assertTagControlCase("minecraft:adventure/voluntary_exile", "minecraft:raiders", "minecraft:entity_type", PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG);
        assertTagControlCase("minecraft:story/deflect_arrow", "minecraft:skeletons", "minecraft:entity_type", PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG);

        PhaseACertification.ControlCaseAnalysis letItGo = PhaseACertification.analyzeControlCase(PROJECT_ROOT, "blazeandcave:enchanting/let_it_go");
        assertNotNull(letItGo.validationEntry());
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_FAIL, letItGo.validationEntry().status());
        assertNotNull(letItGo.validationEntry().exactCodecError());
        assertFalse(letItGo.validationEntry().exactCodecError().contains("Not a json array: \"minecraft:frost_walker\""));
        assertTrue(letItGo.validationEntry().exactCodecError().contains("minecraft:deep_"));

        PhaseACertification.ControlCaseAnalysis travellingBard = PhaseACertification.analyzeControlCase(PROJECT_ROOT, "blazeandcave:redstone/travelling_bard");
        assertNotNull(travellingBard.validationEntry());
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_FAIL, travellingBard.validationEntry().status());
    }

    @Test
    void allowedFallbackPlusTagCollapseRemainsNarrowAndFailClosed() {
        List<PhaseACertification.MissingTagReferenceMatch> validatedEntityTag = List.of(new PhaseACertification.MissingTagReferenceMatch(
            "minecraft:raiders",
            "minecraft:entity_type",
            "#minecraft:raiders",
            List.of("#minecraft:raiders"),
            List.of("#minecraft:raiders"),
            true,
            true,
            "vanilla::data/minecraft/tags/entity_type/raiders.json",
            false
        ));
        assertTrue(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
            ),
            List.of("Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'"),
            validatedEntityTag
        ));
        assertTrue(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
            ),
            List.of(),
            validatedEntityTag
        ));
        assertFalse(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
            ),
            List.of("Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'"),
            List.of()
        ));
        assertFalse(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
            ),
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Not a json array: \"minecraft:sharpness\""
            ),
            validatedEntityTag
        ));
        assertFalse(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]",
                "Not a json array: \"minecraft:sharpness\""
            ),
            List.of(),
            validatedEntityTag
        ));
        assertFalse(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:instrument]"
            ),
            List.of("Missing tag: 'minecraft:raiders' in 'minecraft:entity_type'"),
            validatedEntityTag
        ));
        assertFalse(PhaseACertification.canCollapseAllowedFallbackDiagnosticsToPureTag(
            List.of(
                "Missing tag: 'minecraft:skeletons' in 'minecraft:entity_type'",
                "Registry does not exist: ResourceKey[minecraft:root / minecraft:banner_pattern]"
            ),
            List.of(),
            validatedEntityTag
        ));
    }

    private static void assertEnchantmentHolderSetDeferredControlCase(String advancementId) throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(PROJECT_ROOT, advancementId);
        assertNotNull(controlCase.validationEntry(), "Control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, controlCase.validationEntry().status(), advancementId);
        assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, controlCase.validationEntry().deferredReason(), advancementId);
        assertEquals(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET, controlCase.validationEntry().registryComponent(), advancementId);
        assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET), controlCase.validationEntry().registryComponents(), advancementId);
    }

    private static void assertMultiContextDeferredControlCase(
        String advancementId,
        Set<PhaseACertification.RegistryDependentComponent> expectedComponents
    ) throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(PROJECT_ROOT, advancementId);
        assertNotNull(controlCase.validationEntry(), "Control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, controlCase.validationEntry().status(), advancementId);
        assertEquals(PhaseACertification.DeferredReason.MULTIPLE_CONTEXT_REQUIRED, controlCase.validationEntry().deferredReason(), advancementId);
        assertEquals(expectedComponents, controlCase.validationEntry().registryComponents(), advancementId);
        assertEquals(null, controlCase.validationEntry().registryComponent(), advancementId);
    }

    private static void assertHolderSetControlCase(
        String advancementId,
        PhaseACertification.RegistryDependentComponent expectedComponent
    ) throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(PROJECT_ROOT, advancementId);
        assertTrue(controlCase.subtreePreserved(), "Relevant failing subtree must be preserved for " + advancementId);
        assertNotNull(controlCase.validationEntry(), "Control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, controlCase.validationEntry().status());
        assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, controlCase.validationEntry().deferredReason());
        assertEquals(expectedComponent, controlCase.validationEntry().registryComponent());
        assertTrue(
            controlCase.locationHolderMatches().stream().allMatch(match -> match.rawValues().equals(match.normalizedValues())),
            "Relevant HolderSet subtree must remain unchanged after compatibility normalization for " + advancementId
        );
        assertTrue(!controlCase.locationHolderMatches().isEmpty(), "Control case must expose at least one HolderSet location match for " + advancementId);
    }

    private static void assertTagControlCase(
        String advancementId,
        String expectedTagId,
        String expectedRegistryId,
        PhaseACertification.RegistryDependentComponent expectedComponent
    ) throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(PROJECT_ROOT, advancementId);
        assertNotNull(controlCase.validationEntry(), "Tag control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, controlCase.validationEntry().status());
        assertEquals(PhaseACertification.DeferredReason.TAG_CONTEXT_REQUIRED, controlCase.validationEntry().deferredReason());
        assertEquals(expectedComponent, controlCase.validationEntry().registryComponent());
        List<PhaseACertification.MissingTagReferenceMatch> matchingTags = controlCase.missingTagMatches().stream()
            .filter(match -> match.tagId().equals(expectedTagId) && match.registryId().equals(expectedRegistryId))
            .toList();
        assertEquals(1, matchingTags.size(), "Control case must expose the expected missing-tag diagnostic for " + advancementId);
        PhaseACertification.MissingTagReferenceMatch tagMatch = matchingTags.getFirst();
        assertTrue(controlCase.tagReferencePreserved(), "Tag reference must remain unchanged after compatibility normalization for " + advancementId);
        assertEquals("#" + expectedTagId, tagMatch.tagReference());
        assertTrue(tagMatch.runtimeTagExists(), "Runtime source stack must contain the referenced tag for " + advancementId);
        assertTrue(!tagMatch.junitTagResolvable(), "Current JUnit lookup must still miss the exact tag binding for " + advancementId);
        assertTrue(!tagMatch.runtimeTagSource().isBlank(), "Runtime-existing tag must record a concrete source path for " + advancementId);
    }

    private static void assertStaticPassControlCase(String advancementId) throws IOException {
        PhaseACertification.ControlCaseAnalysis controlCase = PhaseACertification.analyzeControlCase(PROJECT_ROOT, advancementId);
        assertNotNull(controlCase.validationEntry(), "Control case must produce a validation entry");
        assertEquals(PhaseACertification.StaticValidationStatus.STATIC_PASS, controlCase.validationEntry().status(), advancementId);
    }
}
