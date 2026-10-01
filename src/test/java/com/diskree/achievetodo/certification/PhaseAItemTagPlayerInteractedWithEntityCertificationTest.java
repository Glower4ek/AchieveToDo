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

class PhaseAItemTagPlayerInteractedWithEntityCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAItemTagPlayerInteractedWithEntityCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAItemTagPlayerInteractedWithEntityCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "Interaction catalog must be generated and checked in");
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void catalogHasExactMappedScopeAndPacketSemantics() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseAItemTagPlayerInteractedWithEntityCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals("ITEM_TAG_PLAYER_INTERACTED_WITH_ENTITY", root.get("family").getAsString());
        assertEquals("minecraft:player_interacted_with_entity", root.getAsJsonArray("cases").get(0)
            .getAsJsonObject().get("trigger").getAsString());
        assertEquals("ServerGamePacketListenerImpl.handleInteract", root.get("runtimeBoundary").getAsString());
        assertEquals(
            "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY",
            root.get("runtimePacketPath").getAsString()
        );
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(4, summary.get("totalCases").getAsInt());
        assertEquals(4, summary.get("uniqueKeys").getAsInt());
        assertEquals(4, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(4, summary.get("requirementGroups").getAsInt());
        assertEquals(4, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());

        JsonArray cases = root.getAsJsonArray("cases");
        List<String> keys = new ArrayList<>();
        for (JsonElementHolder holder : JsonElementHolder.of(cases)) {
            JsonObject value = holder.value();
            keys.add(value.get("advancementId").getAsString() + "#" + value.get("criterion").getAsString());
            assertEquals(0, value.get("requirementGroupIndex").getAsInt());
            assertEquals("SUPPORTED", value.get("automationEligibility").getAsString());
            assertEquals("MAIN_HAND", value.get("interactionHand").getAsString());
            assertEquals("ServerGamePacketListenerImpl.handleInteract", value.get("boundary").getAsString());
            assertTrue(value.getAsJsonObject("entityPredicate").has("entityType"));
            assertTrue(value.getAsJsonObject("entityPredicate").has("flags"));
        }
        assertEquals(PhaseAItemTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS, keys);
        assertEquals("minecraft:cod", cases.get(0).getAsJsonObject().get("selectedItem").getAsString());
        assertEquals("minecraft:flint_and_steel", cases.get(2).getAsJsonObject().get("selectedItem").getAsString());
        assertEquals("USE_FLINT_AND_STEEL_UNLOCKED", cases.get(2).getAsJsonObject().get("productionPrecondition").getAsString());
        assertEquals("true", cases.get(3).getAsJsonObject().getAsJsonObject("entityPredicate")
            .getAsJsonObject("flags").get("is_baby").getAsString());
    }

    private record JsonElementHolder(JsonObject value) {
        static List<JsonElementHolder> of(JsonArray array) {
            List<JsonElementHolder> holders = new ArrayList<>();
            array.forEach(element -> holders.add(new JsonElementHolder(element.getAsJsonObject())));
            return holders;
        }
    }
}
