package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAEnchantmentInventoryChangedCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void enchantmentInventoryChangedSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAEnchantmentInventoryChangedCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAEnchantmentInventoryChangedCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED catalog snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED catalog snapshot is stale. Run writePhaseAEnchantmentInventoryChangedCertification."
        );
    }

    @Test
    void enchantmentInventoryChangedCatalogMatchesAcceptedFrontier() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAEnchantmentInventoryChangedCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonArray cases = root.getAsJsonArray("cases");
        Set<String> stableAdvancementIds = PhaseAEnchantmentInventoryChangedCertification.stableEnchantmentHolderSetAdvancementIds(PROJECT_ROOT);
        Set<String> acceptedFrontierAdvancementIds = PhaseAEnchantmentInventoryChangedCertification.acceptedFrontierAdvancementIds();
        Map<String, PhaseACertification.StaticValidationEntry> staticEntriesById = new LinkedHashMap<>();
        for (PhaseACertification.StaticValidationEntry entry : PhaseACertification.analyzeStaticRules(PROJECT_ROOT).entries()) {
            staticEntriesById.put(entry.id(), entry);
        }
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(12, summary.get("totalCases").getAsInt());
        assertEquals(11, summary.get("automationSupported").getAsInt());
        assertEquals(1, summary.get("automationDeferred").getAsInt());
        assertEquals(39, stableAdvancementIds.size());
        assertTrue(stableAdvancementIds.containsAll(acceptedFrontierAdvancementIds));

        Set<String> supportedKeys = new LinkedHashSet<>();
        Set<String> deferredKeys = new LinkedHashSet<>();
        Set<String> emittedAdvancementIds = new LinkedHashSet<>();
        boolean godHasCustomNameComponent = false;
        for (int i = 0; i < cases.size(); i++) {
            JsonObject caseJson = cases.get(i).getAsJsonObject();
            String advancementId = caseJson.get("advancementId").getAsString();
            String key = advancementId + "#" + caseJson.get("criterion").getAsString();
            emittedAdvancementIds.add(advancementId);
            assertTrue(stableAdvancementIds.contains(advancementId), "Catalog emitted advancement outside stable ENCHANTMENT_HOLDERSET family: " + advancementId);
            PhaseACertification.StaticValidationEntry staticEntry = staticEntriesById.get(advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, staticEntry.status(), "Unexpected static status for " + advancementId);
            assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, staticEntry.deferredReason(), "Unexpected deferred reason for " + advancementId);
            assertEquals(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET, staticEntry.registryComponent(), "Unexpected registry component for " + advancementId);
            assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET), staticEntry.registryComponents(), "Unexpected registry component set for " + advancementId);
            if ("SUPPORTED".equals(caseJson.get("automationEligibility").getAsString())) {
                supportedKeys.add(key);
                assertEquals("minecraft:inventory_changed", caseJson.get("trigger").getAsString());
                assertFalse(caseJson.getAsJsonArray("enchantmentPredicates").isEmpty(), "SUPPORTED case must expose enchantment predicates: " + key);
            } else {
                deferredKeys.add(key);
                godHasCustomNameComponent = key.equals("blazeandcave:enchanting/god_of_thunder#mjolnir")
                    && caseJson.getAsJsonArray("componentKeys").contains(new com.google.gson.JsonPrimitive("minecraft:custom_name"));
            }
        }

        assertEquals(Set.of(
            "blazeandcave:enchanting/a_rather_pointy_fence_post#rather_pointy_fence_post",
            "blazeandcave:enchanting/boomerang#power",
            "blazeandcave:enchanting/like_a_cat#feather_falling",
            "blazeandcave:enchanting/like_a_ninja#swift_sneak_book",
            "blazeandcave:enchanting/mace_windu#mace_windu",
            "blazeandcave:enchanting/master_fisher#perfect_rod",
            "blazeandcave:enchanting/newtons_flaming_laser_sword#newton",
            "blazeandcave:enchanting/to_infinity_and_beyond#infinity",
            "blazeandcave:enchanting/unbreakable#mending",
            "blazeandcave:enchanting/zeus#power",
            "blazeandcave:mining/the_mistake#the_mistake"
        ), supportedKeys);
        assertEquals(Set.of("blazeandcave:enchanting/god_of_thunder#mjolnir"), deferredKeys);
        assertTrue(godHasCustomNameComponent, "god_of_thunder must remain deferred because of minecraft:custom_name");
        assertEquals(acceptedFrontierAdvancementIds, emittedAdvancementIds);
        assertFalse(emittedAdvancementIds.contains("blazeandcave:enchanting/complete_enchanter"));
        assertFalse(emittedAdvancementIds.contains("blazeandcave:enchanting/master_enchanter"));
        assertFalse(emittedAdvancementIds.contains("blazeandcave:challenges/ultimate_enchanter"));
        for (String advancementId : List.of(
            "blazeandcave:enchanting/complete_enchanter",
            "blazeandcave:enchanting/master_enchanter",
            "blazeandcave:challenges/ultimate_enchanter"
        )) {
            PhaseACertification.StaticValidationEntry entry = staticEntriesById.get(advancementId);
            assertEquals(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, entry.status(), advancementId);
            assertEquals(PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED, entry.deferredReason(), advancementId);
            assertEquals(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET, entry.registryComponent(), advancementId);
            assertEquals(Set.of(PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET), entry.registryComponents(), advancementId);
        }
    }
}
