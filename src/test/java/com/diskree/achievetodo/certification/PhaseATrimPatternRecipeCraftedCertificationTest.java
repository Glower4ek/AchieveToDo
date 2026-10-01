package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseATrimPatternRecipeCraftedCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void snapshotMatchesCheckedInCatalog() throws IOException {
        String generated = PhaseATrimPatternRecipeCraftedCertification.generateSnapshot(PROJECT_ROOT);
        String checkedIn = Files.readString(
            PROJECT_ROOT.resolve(PhaseATrimPatternRecipeCraftedCertification.SNAPSHOT),
            StandardCharsets.UTF_8
        );
        assertEquals(checkedIn, generated, "TRIM_PATTERN_RECIPE_CRAFTED catalog snapshot is stale");
    }

    @Test
    void summaryStaysAtAcceptedExact18Scope() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseATrimPatternRecipeCraftedCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(18, summary.get("totalCases").getAsInt());
        assertEquals(1, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(1, summary.get("requirementGroups").getAsInt());
        assertEquals(18, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());
    }

    @Test
    void exactSentryMappingIsPresent() throws IOException {
        JsonObject root = JsonParser.parseString(
            Files.readString(PROJECT_ROOT.resolve(PhaseATrimPatternRecipeCraftedCertification.SNAPSHOT), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        JsonObject sentry = root.getAsJsonArray("cases").asList().stream()
            .map(element -> element.getAsJsonObject())
            .filter(entry -> "sentry_armor_trim".equals(entry.get("criterion").getAsString()))
            .findFirst()
            .orElseThrow();
        assertEquals("minecraft:sentry_armor_trim_smithing_template_smithing_trim", sentry.get("expectedRecipeId").getAsString());
        assertEquals("minecraft:sentry", sentry.get("expectedTrimPattern").getAsString());
        assertEquals("minecraft:sentry_armor_trim_smithing_template", sentry.get("expectedTemplateItem").getAsString());
        assertTrue(sentry.get("requirementGroup").getAsInt() == 0);
    }
}
