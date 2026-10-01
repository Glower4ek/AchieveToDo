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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runtime adapter for the exact frozen started_riding catalog. */
public final class CertifiedEntityTypeTagStartedRidingCatalog {
    private static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_started_riding_case_catalog.json"
    );
    private static final String SNAPSHOT = "phase_a_entity_type_tag_started_riding_case_catalog";
    private static final String FAMILY = "ENTITY_TYPE_TAG_STARTED_RIDING";
    private static final String TRIGGER = "minecraft:started_riding";
    private static final String SUPPORTED = "SUPPORTED";
    private static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:animal/swine_sailing#swine_sailing",
        "blazeandcave:biomes/boaty_mcboatface#boat",
        "blazeandcave:biomes/cargo_carrier#boat",
        "blazeandcave:nether/jenga#jenga",
        "minecraft:husbandry/ride_a_boat_with_a_goat#ride_a_boat_with_a_goat"
    );
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedEntityTypeTagStartedRidingCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedEntityTypeTagStartedRidingCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    public static CaseDefinition requiredCase(Identifier advancementId, String criterion) {
        String key = advancementId + "#" + criterion;
        for (CaseDefinition caseDefinition : allCases()) {
            if (caseDefinition.key().equals(key)) {
                return caseDefinition;
            }
        }
        throw new IllegalArgumentException("Missing certified ENTITY_TYPE_TAG_STARTED_RIDING case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", "frozenBacap");
            requireString(root, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", 5);
            requireInt(summary, "uniqueKeys", 5);
            requireInt(summary, "uniqueAdvancements", 5);
            requireInt(summary, "requirementGroups", 5);
            requireInt(summary, "automationSupported", 5);
            requireInt(summary, "automationDeferred", 0);
            JsonArray jsonCases = requiredArray(root, "cases");
            if (jsonCases.size() != 5) {
                throw new IllegalStateException("Expected exactly five ENTITY_TYPE_TAG_STARTED_RIDING cases but found " + jsonCases.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : jsonCases) {
                CaseDefinition caseDefinition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(caseDefinition.key())) {
                    throw new IllegalStateException("Duplicate catalog key: " + caseDefinition.key());
                }
                cases.add(caseDefinition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException("ENTITY_TYPE_TAG_STARTED_RIDING catalog key order/set mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ENTITY_TYPE_TAG_STARTED_RIDING catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in started_riding catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in started_riding catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in started_riding catalog");
        }
        return json.get(key).getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in started_riding catalog");
        }
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (!json.has(key) || json.get(key).getAsInt() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in started_riding catalog");
        }
    }

    public record CaseDefinition(
        Identifier advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String entityTypeTag,
        String selectedEntityType,
        String directVehicleType,
        List<String> vehiclePredicateChain,
        String requiredPassengerType,
        String action,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification
    ) {
        public String key() {
            return advancementId + "#" + criterion;
        }

        private static CaseDefinition fromJson(JsonObject json) {
            String id = requiredString(json, "advancementId");
            String trigger = requiredString(json, "trigger");
            if (!TRIGGER.equals(trigger)) {
                throw new IllegalStateException("Unexpected trigger in started_riding catalog: " + trigger);
            }
            int group = json.get("requirementGroupIndex").getAsInt();
            if (group != 0) {
                throw new IllegalStateException("Expected requirementGroupIndex=0 in started_riding catalog");
            }
            if (!SUPPORTED.equals(requiredString(json, "automationEligibility"))) {
                throw new IllegalStateException("Unexpected automation eligibility in started_riding catalog");
            }
            JsonArray chainJson = requiredArray(json, "vehiclePredicateChain");
            if (chainJson.isEmpty()) {
                throw new IllegalStateException("Empty vehicle predicate chain in started_riding catalog");
            }
            List<String> chain = new ArrayList<>();
            for (JsonElement element : chainJson) {
                chain.add(requiredStringValue(element, "vehiclePredicateChain"));
            }
            return new CaseDefinition(
                Identifier.parse(id),
                requiredString(json, "sourcePath"),
                requiredString(json, "criterion"),
                group,
                TRIGGER,
                requiredString(json, "entityTypeTag"),
                requiredString(json, "selectedEntityType"),
                requiredString(json, "directVehicleType"),
                List.copyOf(chain),
                json.has("requiredPassengerType") ? json.get("requiredPassengerType").getAsString() : "",
                requiredString(json, "action"),
                SUPPORTED,
                requiredString(json, "semanticsSource"),
                requiredString(json, "sourceArtifactClassification")
            );
        }

        private static String requiredStringValue(JsonElement element, String key) {
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isBlank()) {
                throw new IllegalStateException("Invalid " + key + " value in started_riding catalog");
            }
            return element.getAsString();
        }
    }
}
