package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runtime-only view of the exact frozen-derived USING_ITEM catalog. */
public final class CertifiedUsingItemCatalog {
    public static final Path CATALOG_PATH = PhaseAUsingItemCertification.SNAPSHOT;
    private static final String SNAPSHOT = PhaseAUsingItemCertification.SNAPSHOT_ID;
    private static final String FAMILY = PhaseAUsingItemCertification.FAMILY;
    private static final String TRIGGER = PhaseAUsingItemCertification.TRIGGER;
    private static final String BOUNDARY = PhaseAUsingItemCertification.BOUNDARY;
    private static final String PACKET_PATH = PhaseAUsingItemCertification.PACKET_PATH;
    private static final String SUPPORTED = PhaseAUsingItemCertification.SUPPORTED;
    private static final String SEMANTICS_SOURCE = PhaseAUsingItemCertification.SEMANTICS_SOURCE;
    private static final String SOURCE_CLASSIFICATION = PhaseAUsingItemCertification.SOURCE_ARTIFACT_CLASSIFICATION;
    private static final List<String> EXPECTED_KEYS = PhaseAUsingItemCertification.EXPECTED_KEYS;
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedUsingItemCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedUsingItemCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    public static CaseDefinition requiredCase(String advancementId, String criterion) {
        String key = advancementId + "#" + criterion;
        for (CaseDefinition definition : allCases()) {
            if (definition.key().equals(key)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Missing certified USING_ITEM case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseAUsingItemExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", SEMANTICS_SOURCE);
            requireString(root, "sourceArtifactClassification", SOURCE_CLASSIFICATION);
            requireString(root, "runtimeBoundary", BOUNDARY);
            requireString(root, "runtimePacketPath", PACKET_PATH);
            requireString(root, "runtimeCriterionBoundary",
                "ServerPlayer.updateUsingItem->CriteriaTriggers.USING_ITEM");

            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", 2);
            requireInt(summary, "uniqueKeys", 2);
            requireInt(summary, "uniqueAdvancements", 2);
            requireInt(summary, "requirementGroups", 2);
            requireInt(summary, "automationSupported", 2);
            requireInt(summary, "automationDeferred", 0);
            JsonObject actions = requiredObject(summary, "actions");
            requireInt(actions, "GOAT_HORN_USE", 1);
            requireInt(actions, "RIPTIDE_TRIDENT_USE", 1);

            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_KEYS.size()) {
                throw new IllegalStateException("Expected exactly two USING_ITEM cases but found " + casesJson.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : casesJson) {
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("USING_ITEM catalog case is not an object");
                }
                CaseDefinition definition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(definition.key())) {
                    throw new IllegalStateException("Duplicate USING_ITEM catalog key: " + definition.key());
                }
                cases.add(definition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException("USING_ITEM catalog key/order mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load USING_ITEM catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in USING_ITEM catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in USING_ITEM catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in USING_ITEM catalog");
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM catalog");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in USING_ITEM catalog");
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM catalog");
        }
    }

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        int requirementGroupCount,
        String trigger,
        String selectedItem,
        String itemPredicateSummary,
        JsonArray requiredItemTags,
        JsonArray requiredEnchantments,
        JsonObject requiredComponents,
        JsonObject environmentalPreconditions,
        JsonObject productionUnlockGate,
        String action,
        String boundary,
        String packetPath,
        JsonObject expectedUseStateSemantics,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification,
        String sourceJsonSha256
    ) {
        static CaseDefinition fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String sourcePath = requiredString(json, "sourcePath");
            String criterion = requiredString(json, "criterion");
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            String selectedItem = requiredString(json, "selectedItem");
            String itemPredicateSummary = requiredString(json, "itemPredicate");
            JsonArray requiredItemTags = requiredArray(json, "requiredItemTags");
            JsonArray requiredEnchantments = requiredArray(json, "requiredEnchantments");
            JsonObject requiredComponents = requiredObject(json, "requiredComponents");
            JsonObject environmentalPreconditions = requiredObject(json, "environmentalPreconditions");
            JsonObject productionUnlockGate = requiredObject(json, "productionUnlockGate");
            String action = requiredString(json, "action");
            requireString(json, "boundary", BOUNDARY);
            String packetPath = requiredString(json, "packetPath");
            JsonObject expectedUseStateSemantics = requiredObject(json, "expectedUseStateSemantics");
            requireString(json, "automationEligibility", SUPPORTED);
            requireString(json, "semanticsSource", SEMANTICS_SOURCE);
            requireString(json, "sourceArtifactClassification", SOURCE_CLASSIFICATION);
            String sourceJsonSha256 = requiredString(json, "sourceJsonSha256");

            if (EXPECTED_KEYS.get(0).equals(advancementId + "#" + criterion)) {
                if (!"minecraft:goat_horn".equals(selectedItem) || !"GOAT_HORN_USE".equals(action)
                    || !itemPredicateSummary.contains("minecraft:goat_horn") || !requiredEnchantments.isEmpty()) {
                    throw new IllegalStateException("Invalid loud-and-proud USING_ITEM catalog case");
                }
                if (productionUnlockGate.get("present").getAsBoolean()) {
                    throw new IllegalStateException("Goat horn unexpectedly has a production gate");
                }
                if (!"9fc4da7b37c91e2913ce3ee60ed46474f2325ce1c5a2f0de018893117612aa35".equals(sourceJsonSha256)) {
                    throw new IllegalStateException("Unexpected frozen loud-and-proud source hash");
                }
                requiredStringValue(environmentalPreconditions, "runtimeWitness", "live biome holder minecraft:deep_dark");
            } else if (EXPECTED_KEYS.get(1).equals(advancementId + "#" + criterion)) {
                if (!"minecraft:trident".equals(selectedItem) || !"RIPTIDE_TRIDENT_USE".equals(action)
                    || !itemPredicateSummary.contains("minecraft:riptide") || requiredEnchantments.size() != 1
                    || !productionUnlockGate.get("present").getAsBoolean()) {
                    throw new IllegalStateException("Invalid do-a-barrel-roll USING_ITEM catalog case");
                }
                requiredStringValue(productionUnlockGate, "ability", "ATTACK_WITH_TRIDENT");
                requireInt(productionUnlockGate, "unlockThreshold", 244);
                requiredStringValue(environmentalPreconditions, "runtimeWitness", "player.isInWaterOrRain() == true");
                if (!"7641d3ca8f47fa14cdab7c13b1822f8efc07127e3c9863b08802f23c9b0c3a61".equals(sourceJsonSha256)) {
                    throw new IllegalStateException("Unexpected frozen do-a-barrel-roll source hash");
                }
            } else {
                throw new IllegalStateException("Unexpected USING_ITEM case " + advancementId + "#" + criterion);
            }
            return new CaseDefinition(
                advancementId,
                sourcePath,
                criterion,
                0,
                1,
                TRIGGER,
                selectedItem,
                itemPredicateSummary,
                requiredItemTags,
                requiredEnchantments,
                requiredComponents,
                environmentalPreconditions,
                productionUnlockGate,
                action,
                BOUNDARY,
                packetPath,
                expectedUseStateSemantics,
                SUPPORTED,
                SEMANTICS_SOURCE,
                SOURCE_CLASSIFICATION,
                sourceJsonSha256
            );
        }

        public String key() {
            return advancementId + "#" + criterion;
        }

        private static String requiredStringValue(JsonObject json, String key, String expected) {
            String actual = requiredString(json, key);
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM catalog");
            }
            return actual;
        }
    }
}
