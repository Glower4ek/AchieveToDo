package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseATameAnimalLlamaCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void catalogSnapshotMatchesFrozenDefinitions() throws IOException {
        assertEquals(
            JsonParser.parseString(PhaseATameAnimalLlamaCertification.generateSnapshot(PROJECT_ROOT)),
            JsonParser.parseString(Files.readString(
                PROJECT_ROOT.resolve(PhaseATameAnimalLlamaCertification.SNAPSHOT), StandardCharsets.UTF_8
            ))
        );
    }

    @Test
    void catalogHasAllAndOnlyTheSixLlamaTameCriteria() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseATameAnimalLlamaCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        assertEquals("TAME_ANIMAL_LLAMA", root.get("family").getAsString());
        assertEquals(6, root.getAsJsonObject("summary").get("totalCases").getAsInt());
        assertEquals(600, root.getAsJsonObject("summary").get("boundedNativeTamingTicks").getAsInt());
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(6, cases.size());
        assertEquals("strength", cases.get(0).getAsJsonObject().get("criterion").getAsString());
        assertEquals(5, cases.get(0).getAsJsonObject().getAsJsonObject("entityPredicate").get("strength").getAsInt());
        assertEquals("GRAY", cases.get(4).getAsJsonObject().getAsJsonObject("entityPredicate").get("variant").getAsString());
        assertTrue(root.get("runtimePacketPath").getAsString().contains("tameWithName"));
    }
}
