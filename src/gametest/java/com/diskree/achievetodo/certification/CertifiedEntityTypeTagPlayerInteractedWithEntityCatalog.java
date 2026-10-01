package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runtime-only view of the exact frozen-derived interaction catalog. */
public final class CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog {
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_player_interacted_with_entity_case_catalog.json"
    );
    private static final String SNAPSHOT = "phase_a_entity_type_tag_player_interacted_with_entity_case_catalog";
    private static final String FAMILY = "ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY";
    private static final String TRIGGER = "minecraft:player_interacted_with_entity";
    private static final String BOUNDARY = "ServerGamePacketListenerImpl.handleInteract";
    private static final String PACKET_PATH = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY";
    private static final String SEMANTICS_SOURCE = "frozenBacap";
    private static final String SUPPORTED = "SUPPORTED";
    private static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:animal/lead_the_way#lead",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#sugar",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#wheat",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#apple",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_apple",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_carrot",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#hay_block",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#enchanted_golden_apple",
        "blazeandcave:animal/you_lead_ill_follow#lead"
    );
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    public static CaseDefinition requiredCase(Identifier advancementId, String criterion) {
        String key = advancementId + "#" + criterion;
        for (CaseDefinition definition : allCases()) {
            if (definition.key().equals(key)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Missing certified ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", SEMANTICS_SOURCE);
            requireString(root, "runtimeBoundary", BOUNDARY);
            requireString(root, "runtimePacketPath", PACKET_PATH);
            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", EXPECTED_KEYS.size());
            requireInt(summary, "uniqueKeys", EXPECTED_KEYS.size());
            requireInt(summary, "uniqueAdvancements", 3);
            requireInt(summary, "requirementGroups", EXPECTED_KEYS.size());
            requireInt(summary, "automationSupported", EXPECTED_KEYS.size());
            requireInt(summary, "automationDeferred", 0);

            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_KEYS.size()) {
                throw new IllegalStateException("Expected exactly nine interaction cases but found " + casesJson.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : casesJson) {
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("Interaction catalog case is not an object");
                }
                CaseDefinition definition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(definition.key())) {
                    throw new IllegalStateException("Duplicate interaction catalog key: " + definition.key());
                }
                cases.add(definition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException("Interaction catalog key/order mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in interaction catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in interaction catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString()
            || json.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in interaction catalog");
        }
        return json.get(key).getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in interaction catalog");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in interaction catalog");
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in interaction catalog");
        }
    }

    private static boolean requiredBoolean(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isBoolean()) {
            throw new IllegalStateException("Missing boolean " + key + " in interaction catalog");
        }
        return json.get(key).getAsBoolean();
    }

    public record CaseDefinition(
        Identifier advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String itemTag,
        String selectedItem,
        String selectedEntityType,
        Map<String, String> entityFlags,
        String action,
        String interactionHand,
        String boundary,
        String packetPath,
        String expectedInteractionResult,
        String productionPrecondition,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification,
        String sourceJsonSha256,
        String itemTagSource,
        String itemTagSourceSha256
    ) {
        static CaseDefinition fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String criterion = requiredString(json, "criterion");
            requireString(json, "trigger", TRIGGER);
            requireString(json, "automationEligibility", SUPPORTED);
            requireString(json, "semanticsSource", SEMANTICS_SOURCE);
            requireInt(json, "requirementGroupIndex", 0);
            JsonObject entityPredicate = requiredObject(json, "entityPredicate");
            String selectedEntityType = requiredString(json, "selectedEntityType");
            if (!selectedEntityType.equals(requiredString(entityPredicate, "entityType"))) {
                throw new IllegalStateException("Catalog entity predicate/type mismatch for " + advancementId + "#" + criterion);
            }
            JsonObject flagsJson = requiredObject(entityPredicate, "flags");
            Map<String, String> flags = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : flagsJson.entrySet()) {
                flags.put(entry.getKey(), entry.getValue().getAsString());
            }
            if (!requiredString(json, "interactionHand").equals("MAIN_HAND")) {
                throw new IllegalStateException("Only MAIN_HAND is accepted for interaction catalog cases");
            }
            requireString(json, "boundary", BOUNDARY);
            requireString(json, "packetPath", PACKET_PATH);
            requireString(json, "expectedInteractionResult", "SUCCESS");
            requiredString(json, "itemTag");
            requiredString(json, "selectedItem");
            requiredString(json, "action");
            requiredString(json, "productionPrecondition");
            requiredString(json, "sourceArtifactClassification");
            requiredString(json, "sourceJsonSha256");
            requiredString(json, "itemTagSource");
            if (json.has("itemTagSourceSha256")) {
                requiredString(json, "itemTagSourceSha256");
            }
            return new CaseDefinition(
                Identifier.parse(advancementId),
                requiredString(json, "sourcePath"),
                criterion,
                0,
                TRIGGER,
                json.get("itemTag").getAsString(),
                json.get("selectedItem").getAsString(),
                selectedEntityType,
                Map.copyOf(flags),
                json.get("action").getAsString(),
                "MAIN_HAND",
                BOUNDARY,
                PACKET_PATH,
                "SUCCESS",
                json.get("productionPrecondition").getAsString(),
                SUPPORTED,
                SEMANTICS_SOURCE,
                json.get("sourceArtifactClassification").getAsString(),
                json.get("sourceJsonSha256").getAsString(),
                json.get("itemTagSource").getAsString(),
                json.has("itemTagSourceSha256") ? json.get("itemTagSourceSha256").getAsString() : ""
            );
        }

        public String key() {
            return advancementId + "#" + criterion;
        }
    }
}

