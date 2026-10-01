package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAItemTagItemUsedOnBlockCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAItemTagItemUsedOnBlockCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAItemTagItemUsedOnBlockCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "ITEM_TAG_ITEM_USED_ON_BLOCK catalog must be generated and checked in");
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void catalogHasExactAcceptedScopeAndSemanticCategories() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAItemTagItemUsedOnBlockCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals("ITEM_TAG_ITEM_USED_ON_BLOCK", root.get("family").getAsString());
        assertEquals("frozenBacap", root.get("semanticsSource").getAsString());
        assertEquals(15, summary.get("totalCases").getAsInt());
        assertEquals(15, summary.get("uniqueKeys").getAsInt());
        assertEquals(4, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(15, summary.get("requirementGroups").getAsInt());
        assertEquals(15, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());
        assertEquals(15, root.getAsJsonArray("cases").size());

        List<String> keys = new ArrayList<>();
        for (var element : root.getAsJsonArray("cases")) {
            JsonObject caseJson = element.getAsJsonObject();
            keys.add(caseJson.get("advancementId").getAsString() + "#" + caseJson.get("criterion").getAsString());
        }
        assertEquals(PhaseAItemTagItemUsedOnBlockCertification.EXPECTED_KEYS, keys);
        assertEquals("STRIP_WOOD", root.getAsJsonArray("cases").get(0).getAsJsonObject().get("action").getAsString());
        assertEquals("CREATE_PATH", root.getAsJsonArray("cases").get(1).getAsJsonObject().get("action").getAsString());
        assertEquals("STRIP_LOG", root.getAsJsonArray("cases").get(2).getAsJsonObject().get("action").getAsString());
        assertEquals("AXE_COPPER_MUTATION", root.getAsJsonArray("cases").get(14).getAsJsonObject().get("action").getAsString());
        assertEquals("minecraft:waxed_oxidized_copper_bulb", root.getAsJsonArray("cases").get(14).getAsJsonObject().get("fixtureBlockBefore").getAsString());
        assertEquals("minecraft:oxidized_copper_bulb", root.getAsJsonArray("cases").get(14).getAsJsonObject().get("expectedPostBlock").getAsString());
        assertEquals("true", root.getAsJsonArray("cases").get(14).getAsJsonObject().getAsJsonObject("blockPredicate").getAsJsonObject("state").get("lit").getAsString());
    }
}
