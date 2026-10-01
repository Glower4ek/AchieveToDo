package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAPlayerHurtEntityDamageSourceCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAPlayerHurtEntityDamageSourceCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAPlayerHurtEntityDamageSourceCertification.SNAPSHOT);
        assertTrue(Files.isRegularFile(snapshot));
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void catalogHasExactThreeSingletonDamageSourceCases() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseAPlayerHurtEntityDamageSourceCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(PhaseAPlayerHurtEntityDamageSourceCertification.FAMILY,
            root.get("family").getAsString());
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(3, summary.get("totalCases").getAsInt());
        assertEquals(3, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(3, summary.get("requirementGroups").getAsInt());
        assertEquals(3, summary.get("automationSupported").getAsInt());
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(3, cases.size());
        assertEquals("minecraft:wind_charge", cases.get(0).getAsJsonObject()
            .get("expectedDamageType").getAsString());
        assertEquals("minecraft:is_projectile", cases.get(0).getAsJsonObject()
            .getAsJsonArray("requiredDamageTypeTags").get(0).getAsString());
        assertEquals("minecraft:fishes", cases.get(1).getAsJsonObject()
            .get("sourceEquipmentTag").getAsString());
        assertEquals("minecraft:axes", cases.get(2).getAsJsonObject()
            .get("sourceEquipmentTag").getAsString());
        assertEquals(
            List.of(
                "blazeandcave:adventure/from_under_your_feet#from_under_your_feet",
                "blazeandcave:weaponry/slapfish#slapfish",
                "blazeandcave:weaponry/viking#axe"
            ),
            cases.asList().stream().map(caseJson -> {
                JsonObject json = caseJson.getAsJsonObject();
                return json.get("advancementId").getAsString() + "#" + json.get("criterion").getAsString();
            }).toList()
        );
    }
}
