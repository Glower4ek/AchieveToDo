package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseAEnchantmentInventoryChangedExpansionDirect16CertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void snapshotMatchesCheckedInCatalog() throws IOException {
        String generated = PhaseAEnchantmentInventoryChangedExpansionDirect16Certification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16Certification.SNAPSHOT);
        String checkedIn = Files.readString(snapshot, StandardCharsets.UTF_8);
        assertEquals(
            JsonParser.parseString(checkedIn),
            JsonParser.parseString(generated),
            "ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16 catalog snapshot is stale. Run PhaseAEnchantmentInventoryChangedExpansionDirect16Certification."
        );
    }

    @Test
    void summaryStaysAtAcceptedScope() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseAEnchantmentInventoryChangedExpansionDirect16Certification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(25, summary.get("totalCases").getAsInt());
        assertEquals(16, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(25, summary.get("selectedRequirementGroups").getAsInt());
        assertEquals("ENCHANTMENTS", summary.get("storageType").getAsString());
    }
}
