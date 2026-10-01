package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAItemTagInventoryChangedCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void itemTagInventoryChangedSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAItemTagInventoryChangedCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAItemTagInventoryChangedCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "ITEM_TAG_INVENTORY_CHANGED catalog snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "ITEM_TAG_INVENTORY_CHANGED catalog snapshot is stale. Run writePhaseAItemTagInventoryChangedCertification."
        );
    }

    @Test
    void itemTagInventoryChangedCatalogMatchesExpectedSemanticFrontier() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAItemTagInventoryChangedCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, summary.get("totalCases").getAsInt());
        assertEquals(16, summary.get("automationSupported").getAsInt());

        Set<String> supportedKeys = new LinkedHashSet<>();
        Set<String> allKeys = new LinkedHashSet<>();
        Map<String, JsonObject> casesByKey = new HashMap<>();
        int supportedCount = 0;
        boolean organizationalWizardSupported = false;
        Integer smithingTemplateMin = null;
        Integer smithingTemplateMax = null;
        Integer emeraldMinerMin = null;
        Integer emeraldMinerMax = null;
        for (int i = 0; i < cases.size(); i++) {
            JsonObject caseJson = cases.get(i).getAsJsonObject();
            String advancementId = caseJson.get("advancementId").getAsString();
            String criterion = caseJson.get("criterion").getAsString();
            String key = advancementId + "#" + criterion;
            assertTrue(allKeys.add(key), "Duplicate advancementId#criterion: " + key);
            casesByKey.put(key, caseJson);
            if ("SUPPORTED".equals(caseJson.get("automationEligibility").getAsString())) {
                supportedCount++;
                supportedKeys.add(key);
                assertEquals("minecraft:inventory_changed", caseJson.get("trigger").getAsString());
                assertFalse(caseJson.get("itemTag").isJsonNull(), "SUPPORTED case must expose nonblank itemTag: " + key);
            }
            if ("blazeandcave:end/organizational_wizard#shulker_box".equals(key)
                && "SUPPORTED".equals(caseJson.get("automationEligibility").getAsString())) {
                organizationalWizardSupported = true;
            }
            if ("blazeandcave:challenges/stack_all_the_items#smithing_template".equals(key)) {
                smithingTemplateMin = caseJson.get("requiredCountMin").isJsonNull() ? null : caseJson.get("requiredCountMin").getAsInt();
                smithingTemplateMax = caseJson.get("requiredCountMax").isJsonNull() ? null : caseJson.get("requiredCountMax").getAsInt();
            }
            if ("blazeandcave:mining/emerald_miner#silk_touch".equals(key)) {
                emeraldMinerMin = caseJson.get("requiredCountMin").isJsonNull() ? null : caseJson.get("requiredCountMin").getAsInt();
                emeraldMinerMax = caseJson.get("requiredCountMax").isJsonNull() ? null : caseJson.get("requiredCountMax").getAsInt();
            }
        }

        assertEquals(16, supportedCount);
        assertFalse(organizationalWizardSupported, "organizational_wizard must stay deferred because of minecraft:custom_name");
        assertEquals(64, smithingTemplateMin);
        assertEquals(64, smithingTemplateMax);
        assertEquals(64, emeraldMinerMin);
        assertEquals(64, emeraldMinerMax);
        JsonObject salvageSherd = casesByKey.get("minecraft:adventure/salvage_sherd#has_sherd");
        assertTrue(salvageSherd != null, "salvage_sherd regression case must exist");
        assertEquals("minecraft:inventory_changed", salvageSherd.get("trigger").getAsString());
        assertEquals("minecraft:decorated_pot_sherds", salvageSherd.get("itemTag").getAsString());
        assertEquals("SUPPORTED", salvageSherd.get("automationEligibility").getAsString());
        assertTrue(!salvageSherd.has("requiredCountMin") || salvageSherd.get("requiredCountMin").isJsonNull());
        assertTrue(!salvageSherd.has("requiredCountMax") || salvageSherd.get("requiredCountMax").isJsonNull());
        assertEquals(
            Set.of(
                "blazeandcave:adventure/good_lookin_treasure#trim_templates",
                "blazeandcave:animal/humble_bundle#bundle",
                "blazeandcave:building/cut_in_half#slab",
                "blazeandcave:building/stairs_no#stairs",
                "blazeandcave:building/your_door_was_locked#door",
                "blazeandcave:challenges/stack_all_the_items#smithing_template",
                "blazeandcave:end/portable_storage#shulker_box",
                "blazeandcave:farming/root#hoe",
                "blazeandcave:farming/trimming_the_treetops#leaves",
                "blazeandcave:mining/emerald_miner#silk_touch",
                "blazeandcave:mining/root#pickaxe",
                "blazeandcave:monsters/root#sword",
                "blazeandcave:nether/i_am_root#nether_roots",
                "blazeandcave:nether/what_a_fungi#nether_fungus",
                "blazeandcave:weaponry/root#sword",
                "minecraft:adventure/salvage_sherd#has_sherd"
            ),
            supportedKeys
        );
    }
}
