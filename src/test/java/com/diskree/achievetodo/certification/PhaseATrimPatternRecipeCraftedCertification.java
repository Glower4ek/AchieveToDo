package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseATrimPatternRecipeCraftedCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_case_catalog.json"
    );
    static final String SNAPSHOT_ID = "trim_pattern_recipe_crafted_case_catalog";
    static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    static final String FROZEN_ADVANCEMENT_PATH = "data/minecraft/advancement/adventure/trim_with_any_armor_pattern.json";
    static final String ADVANCEMENT_ID = "minecraft:adventure/trim_with_any_armor_pattern";
    static final String TRIGGER = "minecraft:recipe_crafted";
    static final String RECIPE_TYPE = "minecraft:smithing_trim";
    static final String ADDITION_TAG = "#minecraft:trim_materials";
    static final String BASE_TAG = "#minecraft:trimmable_armor";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final Path CURRENT_MINECRAFT_JAR = Path.of(
        ".gradle-user-home", "caches", "fabric-loom", "26.2", "minecraft-client.jar"
    );
    private static final List<String> EXPECTED_CRITERIA = List.of(
        "coast_armor_trim", "dune_armor_trim", "eye_armor_trim", "host_armor_trim", "raiser_armor_trim",
        "rib_armor_trim", "sentry_armor_trim", "shaper_armor_trim", "silence_armor_trim", "snout_armor_trim",
        "spire_armor_trim", "tide_armor_trim", "vex_armor_trim", "ward_armor_trim", "wayfinder_armor_trim",
        "wild_armor_trim", "bolt_armor_trim", "flow_armor_trim"
    );
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseATrimPatternRecipeCraftedCertification() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected project root path");
        }
        writeSnapshot(Path.of(args[0]).toAbsolutePath().normalize());
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve(SNAPSHOT), generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        JsonObject advancement = loadFrozenAdvancement(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
        JsonObject criteria = requiredObject(advancement, "criteria", ADVANCEMENT_ID);
        if (criteria.size() != 18) {
            throw new IllegalStateException("Expected exactly 18 criteria but found " + criteria.size());
        }
        Set<String> expectedCriteria = new HashSet<>(EXPECTED_CRITERIA);
        if (!new HashSet<>(criteria.keySet()).equals(expectedCriteria)) {
            throw new IllegalStateException("Frozen advancement criteria do not equal accepted exact18 set");
        }
        validateRequirements(advancement, expectedCriteria);

        List<CaseDefinition> cases = new ArrayList<>();
        for (String criterion : EXPECTED_CRITERIA) {
            JsonObject criterionJson = requiredObject(criteria, criterion, ADVANCEMENT_ID + "#" + criterion);
            String trigger = requiredString(criterionJson, "trigger");
            if (!TRIGGER.equals(trigger)) {
                throw new IllegalStateException("Expected recipe_crafted trigger for " + criterion + " but got " + trigger);
            }
            JsonObject conditions = requiredObject(criterionJson, "conditions", criterion);
            requireExactKeys(conditions, Set.of("recipe_id"), "criterion conditions for " + criterion);
            String recipeId = requiredString(conditions, "recipe_id");
            String patternName = criterion.substring(0, criterion.length() - "_armor_trim".length());
            String expectedRecipeId = "minecraft:" + criterion + "_smithing_template_smithing_trim";
            String expectedPattern = "minecraft:" + patternName;
            String expectedTemplate = "minecraft:" + criterion + "_smithing_template";
            if (!expectedRecipeId.equals(recipeId)) {
                throw new IllegalStateException("Unexpected recipe mapping for " + criterion + ": " + recipeId);
            }
            JsonObject recipe = loadCurrentRecipe(projectRoot, recipeId);
            requireExactKeys(recipe, Set.of("type", "addition", "base", "pattern", "template"), "recipe " + recipeId);
            requireString(recipe, "type", RECIPE_TYPE, recipeId);
            requireString(recipe, "addition", ADDITION_TAG, recipeId);
            requireString(recipe, "base", BASE_TAG, recipeId);
            requireString(recipe, "pattern", expectedPattern, recipeId);
            requireString(recipe, "template", expectedTemplate, recipeId);
            cases.add(new CaseDefinition(
                ADVANCEMENT_ID,
                FROZEN_ADVANCEMENT_PATH,
                criterion,
                trigger,
                recipeId,
                expectedPattern,
                expectedTemplate,
                0,
                "SUPPORTED"
            ));
        }

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("canonicalAdvancementCount", 1152);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueAdvancements", 1);
        summary.addProperty("requirementGroups", 1);
        summary.addProperty("automationSupported", cases.size());
        summary.addProperty("automationDeferred", 0);
        root.add("summary", summary);
        JsonArray casesJson = new JsonArray();
        for (CaseDefinition caseDefinition : cases) {
            JsonObject caseJson = new JsonObject();
            caseJson.addProperty("advancementId", caseDefinition.advancementId());
            caseJson.addProperty("sourcePath", caseDefinition.sourcePath());
            caseJson.addProperty("criterion", caseDefinition.criterion());
            caseJson.addProperty("trigger", caseDefinition.trigger());
            caseJson.addProperty("expectedRecipeId", caseDefinition.expectedRecipeId());
            caseJson.addProperty("expectedTrimPattern", caseDefinition.expectedTrimPattern());
            caseJson.addProperty("expectedTemplateItem", caseDefinition.expectedTemplateItem());
            caseJson.addProperty("requirementGroup", caseDefinition.requirementGroup());
            caseJson.addProperty("automationEligibility", caseDefinition.automationEligibility());
            casesJson.add(caseJson);
        }
        root.add("cases", casesJson);
        return GSON.toJson(root) + "\n";
    }

    private static void validateRequirements(JsonObject advancement, Set<String> expectedCriteria) {
        JsonArray requirements = requiredArray(advancement, "requirements", ADVANCEMENT_ID);
        if (requirements.size() != 1 || !requirements.get(0).isJsonArray()) {
            throw new IllegalStateException("Expected exactly one requirement group for " + ADVANCEMENT_ID);
        }
        JsonArray group = requirements.get(0).getAsJsonArray();
        if (group.size() != expectedCriteria.size()) {
            throw new IllegalStateException("Expected requirement group to contain exact18 criteria");
        }
        Set<String> actual = new HashSet<>();
        for (JsonElement criterion : group) {
            if (!criterion.isJsonPrimitive() || !actual.add(criterion.getAsString())) {
                throw new IllegalStateException("Duplicate or non-string criterion in requirement group");
            }
        }
        if (!actual.equals(expectedCriteria)) {
            throw new IllegalStateException("Requirement group does not contain exact18 criteria");
        }
    }

    private static JsonObject loadFrozenAdvancement(Path bacapZip) throws IOException {
        try (ZipFile zipFile = new ZipFile(bacapZip.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zipFile.getEntry(FROZEN_ADVANCEMENT_PATH);
            if (entry == null) {
                throw new IllegalStateException("Missing frozen advancement entry " + FROZEN_ADVANCEMENT_PATH);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return readJson(inputStream);
            }
        }
    }

    private static JsonObject loadCurrentRecipe(Path projectRoot, String recipeId) throws IOException {
        String resourcePath = "data/" + recipeId.substring(0, recipeId.indexOf(':')) + "/recipe/"
            + recipeId.substring(recipeId.indexOf(':') + 1) + ".json";
        Path minecraftJar = projectRoot.resolve(CURRENT_MINECRAFT_JAR);
        if (Files.exists(minecraftJar)) {
            try (ZipFile zipFile = new ZipFile(minecraftJar.toFile(), StandardCharsets.UTF_8)) {
                ZipEntry entry = zipFile.getEntry(resourcePath);
                if (entry == null) {
                    throw new IllegalStateException("Missing current vanilla recipe resource " + resourcePath);
                }
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    return readJson(inputStream);
                }
            }
        }
        InputStream inputStream = PhaseATrimPatternRecipeCraftedCertification.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Missing current vanilla recipe resource " + resourcePath);
        }
        try (InputStream stream = inputStream) {
            return readJson(stream);
        }
    }

    private static JsonObject readJson(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject requiredObject(JsonObject json, String key, String context) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalStateException("Expected object " + key + " for " + context);
        }
        return json.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key, String context) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalStateException("Expected array " + key + " for " + context);
        }
        return json.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject json, String key) {
        return requiredString(json, key, "JSON");
    }

    private static String requiredString(JsonObject json, String key, String context) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Expected string " + key + " for " + context);
        }
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank string " + key + " for " + context);
        }
        return value;
    }

    private static void requireString(JsonObject json, String key, String expected, String context) {
        String actual = requiredString(json, key, context);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " for " + context + " but got " + actual);
        }
    }

    private static void requireExactKeys(JsonObject json, Set<String> expected, String context) {
        Set<String> actual = new TreeSet<>(json.keySet());
        if (!actual.equals(new TreeSet<>(expected))) {
            throw new IllegalStateException("Unexpected keys for " + context + ": " + actual);
        }
    }

    private record CaseDefinition(
        String advancementId,
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
