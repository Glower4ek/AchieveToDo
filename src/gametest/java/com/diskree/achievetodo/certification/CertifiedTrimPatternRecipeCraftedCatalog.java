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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class CertifiedTrimPatternRecipeCraftedCatalog {
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_case_catalog.json"
    );
    public static final String SNAPSHOT = "trim_pattern_recipe_crafted_case_catalog";
    public static final String ADVANCEMENT_ID = "minecraft:adventure/trim_with_any_armor_pattern";
    public static final String SOURCE_PATH = "data/minecraft/advancement/adventure/trim_with_any_armor_pattern.json";
    public static final String TRIGGER = "minecraft:recipe_crafted";
    public static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    public static final List<String> EXPECTED_CRITERIA = List.of(
        "coast_armor_trim",
        "dune_armor_trim",
        "eye_armor_trim",
        "host_armor_trim",
        "raiser_armor_trim",
        "rib_armor_trim",
        "sentry_armor_trim",
        "shaper_armor_trim",
        "silence_armor_trim",
        "snout_armor_trim",
        "spire_armor_trim",
        "tide_armor_trim",
        "vex_armor_trim",
        "ward_armor_trim",
        "wayfinder_armor_trim",
        "wild_armor_trim",
        "bolt_armor_trim",
        "flow_armor_trim"
    );
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedTrimPatternRecipeCraftedCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedTrimPatternRecipeCraftedCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    public static CaseDefinition requiredCase(Identifier advancementId, String criterion) {
        for (CaseDefinition caseDefinition : allCases()) {
            if (caseDefinition.advancementId().equals(advancementId) && caseDefinition.criterion().equals(criterion)) {
                return caseDefinition;
            }
        }
        throw new IllegalArgumentException("Missing TRIM_PATTERN_RECIPE_CRAFTED case for " + advancementId + "#" + criterion);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseATrimPatternRecipeCraftedExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "canonicalSemanticsSource", "reference/phase_a_preservation/files/final/bacap.zip");
            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", EXPECTED_CRITERIA.size());
            requireInt(summary, "uniqueAdvancements", 1);
            requireInt(summary, "requirementGroups", 1);
            requireInt(summary, "automationSupported", EXPECTED_CRITERIA.size());
            requireInt(summary, "automationDeferred", 0);

            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_CRITERIA.size()) {
                throw new IllegalStateException("Expected exactly 18 trim-pattern cases but found " + casesJson.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> seenKeys = new HashSet<>();
            for (int index = 0; index < casesJson.size(); index++) {
                JsonObject caseJson = casesJson.get(index).getAsJsonObject();
                requireExactKeys(caseJson);
                String advancementId = requiredString(caseJson, "advancementId");
                String sourcePath = requiredString(caseJson, "sourcePath");
                String criterion = requiredString(caseJson, "criterion");
                String trigger = requiredString(caseJson, "trigger");
                String expectedRecipeId = requiredString(caseJson, "expectedRecipeId");
                String expectedTrimPattern = requiredString(caseJson, "expectedTrimPattern");
                String expectedTemplateItem = requiredString(caseJson, "expectedTemplateItem");
                int requirementGroup = requiredInt(caseJson, "requirementGroup");
                String automationEligibility = requiredString(caseJson, "automationEligibility");
                String key = advancementId + "#" + criterion;
                if (!seenKeys.add(key)) {
                    throw new IllegalStateException("Duplicate trim-pattern catalog key: " + key);
                }
                if (!ADVANCEMENT_ID.equals(advancementId) || !SOURCE_PATH.equals(sourcePath)) {
                    throw new IllegalStateException("Unexpected advancement/source for " + key);
                }
                if (!EXPECTED_CRITERIA.get(index).equals(criterion)) {
                    throw new IllegalStateException("Unexpected criterion at catalog index " + index + ": " + criterion);
                }
                String pattern = criterion.substring(0, criterion.length() - "_armor_trim".length());
                requireEquals(trigger, TRIGGER, "trigger for " + key);
                requireEquals(expectedRecipeId, "minecraft:" + criterion + "_smithing_template_smithing_trim", "recipe for " + key);
                requireEquals(expectedTrimPattern, "minecraft:" + pattern, "trim pattern for " + key);
                requireEquals(expectedTemplateItem, "minecraft:" + criterion + "_smithing_template", "template for " + key);
                if (requirementGroup != 0) {
                    throw new IllegalStateException("Expected all trim-pattern criteria in requirement group 0: " + key);
                }
                requireEquals(automationEligibility, AUTOMATION_SUPPORTED, "automation eligibility for " + key);
                cases.add(new CaseDefinition(
                    Identifier.parse(advancementId),
                    sourcePath,
                    criterion,
                    trigger,
                    expectedRecipeId,
                    expectedTrimPattern,
                    expectedTemplateItem,
                    requirementGroup,
                    automationEligibility
                ));
            }
            if (!seenKeys.equals(new TreeSet<>(EXPECTED_CRITERIA.stream().map(criterion -> ADVANCEMENT_ID + "#" + criterion).toList()))) {
                throw new IllegalStateException("Trim-pattern catalog criteria do not equal the accepted exact18 set");
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load trim-pattern recipe-crafted catalog", e);
        }
    }

    private static void requireExactKeys(JsonObject json) {
        Set<String> actual = new TreeSet<>();
        for (String key : json.keySet()) {
            actual.add(key);
        }
        Set<String> expected = Set.of(
            "advancementId", "sourcePath", "criterion", "trigger", "expectedRecipeId",
            "expectedTrimPattern", "expectedTemplateItem", "requirementGroup", "automationEligibility"
        );
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Unexpected trim-pattern catalog case fields: " + actual);
        }
    }

    private static JsonObject requiredObject(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in trim-pattern catalog");
        }
        return json.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in trim-pattern catalog");
        }
        return json.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing string " + key + " in trim-pattern catalog");
        }
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank string " + key + " in trim-pattern catalog");
        }
        return value;
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("Missing integer " + key + " in trim-pattern catalog");
        }
        return json.get(key).getAsInt();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        requireEquals(requiredString(json, key), expected, key);
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in trim-pattern catalog");
        }
    }

    private static void requireEquals(String actual, String expected, String context) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + context + "=" + expected + " but got " + actual);
        }
    }

    public record CaseDefinition(
        Identifier advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String expectedRecipeId,
        String expectedTrimPattern,
        String expectedTemplateItem,
        int requirementGroup,
        String automationEligibility
    ) {
    }
}
