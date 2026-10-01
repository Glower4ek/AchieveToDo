package com.diskree.achievetodo.certification;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseABlockTagItemUsedOnBlockCertificationTest {
    static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    @Test void snapshotMatchesFrozenAuthoritativeInputAndUsesLf() throws Exception {
        String generated = PhaseABlockTagItemUsedOnBlockCertification.generateSnapshot(ROOT);
        assertEquals(Files.readString(ROOT.resolve(PhaseABlockTagItemUsedOnBlockCertification.SNAPSHOT)), generated);
        assertFalse(generated.contains("\r")); assertTrue(generated.endsWith("\n"));
        assertFalse(generated.contains("taskId"));
        assertEquals("sha-256:a1ca1ea46131dcf6bb8e22924c2f279f054e67c0ae58e4887858568479f990b7", PhaseABlockTagItemUsedOnBlockCertification.fingerprint(ROOT));
        assertNotEquals("sha-256:05983665e91b84c5b866fd807e7b0366ddfc9f8a7e884d34339ab807d79edf99", PhaseABlockTagItemUsedOnBlockCertification.fingerprint(ROOT));
        assertEquals(Set.of("frozenBacap"), JsonParser.parseString(generated).getAsJsonObject().getAsJsonObject("provenance").keySet());
        assertEquals(generated, PhaseABlockTagItemUsedOnBlockCertification.generateSnapshot(ROOT));
        assertFalse(generated.contains("runId"));
    }
    @Test void exact30CoversAllSevenAdvancementsWithoutPropagule() throws Exception {
        var catalog = JsonParser.parseString(PhaseABlockTagItemUsedOnBlockCertification.generateSnapshot(ROOT)).getAsJsonObject();
        Set<String> keys = new TreeSet<>(), groups = new TreeSet<>(), ids = new TreeSet<>();
        for (JsonElement e : catalog.getAsJsonArray("cases")) {
            JsonObject c = e.getAsJsonObject(); String id = c.get("advancementId").getAsString();
            assertTrue(keys.add(id + "#" + c.get("criterion").getAsString())); ids.add(id);
            groups.add(id + "#" + c.get("requirementGroupIndex").getAsInt());
            assertTrue(c.get("blockTag").getAsString().startsWith("#minecraft:"));
            assertFalse(c.toString().contains("bone_meal_propagule"));
            assertFalse(c.has("criterionBefore")); assertFalse(c.has("criterionAfter"));
            assertFalse(c.get("expectedCriterionBefore").getAsBoolean()); assertTrue(c.get("expectedCriterionAfter").getAsBoolean());
            assertEquals("frozenBacap", c.get("semanticsSource").getAsString());
            assertEquals("POST_USE_CLICKED_POSITION", c.get("blockTagSampling").getAsString());
            if (id.endsWith("one_course_meal")) { assertEquals("bone_meal", c.get("criterion").getAsString()); assertEquals("#minecraft:logs_that_burn", c.get("blockTag").getAsString()); }
            if (id.endsWith("one_course_meal")) assertEquals("GROW_OAK_SAPLING", c.get("action").getAsString());
            if (id.endsWith("make_a_sign_glow")) { assertEquals("glow_ink_sac", c.get("criterion").getAsString()); assertEquals("minecraft:glow_ink_sac", c.get("heldItem").getAsString()); assertEquals("GLOW_SIGN", c.get("action").getAsString()); }
        }
        assertEquals(30, keys.size()); assertEquals(30, groups.size()); assertEquals(7, ids.size());
        assertEquals(PhaseABlockTagItemUsedOnBlockCertification.expectedKeys(), keys);
        assertTrue(keys.contains("minecraft:husbandry/make_a_sign_glow#glow_ink_sac"));
        assertFalse(keys.contains("minecraft:husbandry/make_a_sign_glow#make_a_sign_glow"));
        assertTrue(keys.contains("minecraft:husbandry/safely_harvest_honey#safely_harvest_honey"));
    }

    @Test void historicalMinecraftDefinitionsAreGuardedBeyondCriterionIdentity() throws Exception {
        try (var zip = new java.util.zip.ZipFile(ROOT.resolve("reference/phase_a_preservation/files/final/bacap.zip").toFile())) {
            for (String[] binding : new String[][]{
                {"make_a_sign_glow", "glow_ink_sac", "cephalight", "glow_and_behold"},
                {"safely_harvest_honey", "safely_harvest_honey", "ya_like_jazz", "bee_our_guest"}
            }) {
                String id = "minecraft:husbandry/" + binding[0];
                byte[] raw;
                try (var input = zip.getInputStream(zip.getEntry("data/minecraft/advancement/husbandry/" + binding[0] + ".json"))) { raw = input.readAllBytes(); }
                var original = JsonParser.parseString(new String(raw, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(Set.of(binding[1]), original.getAsJsonObject("criteria").keySet());
                assertEquals("blazeandcave:animal/" + binding[2], original.get("parent").getAsString());
                assertEquals("bacap_rewards:animal/" + binding[3], original.getAsJsonObject("rewards").get("function").getAsString());
                PhaseABlockTagItemUsedOnBlockCertification.validateHistoricalMinecraftDefinition(id, raw, original);
                assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockCertification.validateHistoricalMinecraftDefinition(id, new byte[0], original));
                var parent = original.deepCopy(); parent.addProperty("parent", "minecraft:husbandry/root");
                assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockCertification.validateHistoricalMinecraftDefinition(id, raw, parent));
                var reward = original.deepCopy(); reward.getAsJsonObject("rewards").addProperty("function", "wrong:reward");
                assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockCertification.validateHistoricalMinecraftDefinition(id, raw, reward));
                var groups = original.deepCopy(); groups.add("requirements", JsonParser.parseString("[[\"wrong\"]]"));
                assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockCertification.validateHistoricalMinecraftDefinition(id, raw, groups));
            }
        }
    }
}
