package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Family-owned TEMP recorder. No persistent destination or promotion entry point. */
public final class PhaseABlockTagItemUsedOnBlockExecutionEvidence {
    public static final String FAMILY = "BLOCK_TAG_ITEM_USED_ON_BLOCK";
    public static final String SOURCE = "PhaseABlockTagItemUsedOnBlockGameTest";
    public static final String CANARY = "blazeandcave:building/sign_off";
    private static final String CANARY_CRITERION = "honeycomb";
    private static final String WHITE_DYE = "blazeandcave:building/colors_of_the_wind";
    private static final String GLOW_SIGN = "minecraft:husbandry/make_a_sign_glow";
    private static final String GLOW_SIGN_CRITERION = "glow_ink_sac";
    private static final String MAP_BANNER = "blazeandcave:adventure/im_not_lost_anymore";
    private static final String MAP_BANNER_CRITERION = "map";
    private static final String PLACE_FOOD_ADVANCEMENT = "blazeandcave:building/delicious_hot_schmoes";
    private static final String HONEY_HARVEST = "minecraft:husbandry/safely_harvest_honey";
    private static final String HONEY_HARVEST_CRITERION = "safely_harvest_honey";
    private static final String BONE_MEAL_ADVANCEMENT = "blazeandcave:farming/one_course_meal";
    private static final String BONE_MEAL_CRITERION = "bone_meal";
    private static final String CATALOG_FINGERPRINT = "sha-256:a1ca1ea46131dcf6bb8e22924c2f279f054e67c0ae58e4887858568479f990b7";
    private static final Path CATALOG = Path.of("src/test/resources/phase_a_certification/block_tag_item_used_on_block_case_catalog.json");
    private static final Path TEMP = Path.of("build/tmp/phase_a_certification/block_tag_item_used_on_block_execution_evidence.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<String> CATALOG_FIELDS = List.of(
        "advancementId", "criterion", "requirementGroupIndex", "trigger", "blockTag", "blockTagSampling", "heldItem", "action");
    private static final List<String> OWNERSHIP_FIELDS = List.of(
        "family", "source", "catalogFingerprint", "runId", "minecraftVersion", "compatibilityMarker");
    private static JsonObject activeRun;

    public static Path projectRoot() {
        String root = System.getProperty("achievetodo.phaseA.projectRoot");
        if (root == null || root.isBlank()) throw new IllegalStateException("Missing Phase A projectRoot");
        return Path.of(root).toAbsolutePath().normalize();
    }
    private static String fingerprint() throws IOException {
        try { return "sha-256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(projectRoot().resolve(CATALOG)))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static JsonObject definitionFor(String advancementId, String criterion) throws IOException {
        JsonObject catalog = checkedCatalog();
        String requestedKey = key(advancementId, criterion);
        Set<String> keys = new HashSet<>();
        Set<String> advancements = new HashSet<>();
        JsonObject definition = null;
        for (JsonElement element : catalog.getAsJsonArray("cases")) {
            require(element.isJsonObject(), "Invalid catalog case");
            JsonObject candidate = element.getAsJsonObject();
            String id = string(candidate, "advancementId");
            String candidateCriterion = string(candidate, "criterion");
            require(!"bone_meal_propagule".equals(candidateCriterion), "Excluded criterion in catalog");
            require(keys.add(key(id, candidateCriterion)), "Duplicate catalog criterion");
            require(string(candidate, "blockTag").startsWith("#"), "Invalid retained block tag");
            for (String field : CATALOG_FIELDS) if (!candidate.has(field)) throw new IllegalStateException("Missing catalog field: " + field);
            advancements.add(id);
            if (requestedKey.equals(key(id, candidateCriterion))) definition = candidate;
        }
        require(advancements.size() == 7, "Invalid exact30 catalog advancement count");
        require(definition != null, "Unknown or excluded criterion: " + requestedKey);
        return definition.deepCopy();
    }

    public static List<JsonObject> dyeSignDefinitions() throws IOException {
        JsonObject catalog = checkedCatalog();
        List<JsonObject> definitions = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (JsonElement element : catalog.getAsJsonArray("cases")) {
            JsonObject candidate = element.getAsJsonObject();
            if (!"DYE_SIGN".equals(string(candidate, "action"))) continue;
            require(WHITE_DYE.equals(string(candidate, "advancementId")), "DYE_SIGN outside colors_of_the_wind");
            String candidateKey = key(candidate);
            require(keys.add(candidateKey), "Duplicate DYE_SIGN criterion: " + candidateKey);
            definitions.add(definitionFor(candidate.get("advancementId").getAsString(), candidate.get("criterion").getAsString()));
        }
        require(definitions.size() == 16 && keys.size() == 16, "Invalid colors_of_the_wind DYE_SIGN coverage");
        definitions.sort(Comparator.comparingInt(definition -> definition.get("requirementGroupIndex").getAsInt()));
        return definitions;
    }

    public static List<JsonObject> placeFoodDefinitions() throws IOException {
        JsonObject catalog = checkedCatalog();
        List<JsonObject> definitions = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        Set<Integer> requirementGroups = new HashSet<>();
        for (JsonElement element : catalog.getAsJsonArray("cases")) {
            JsonObject candidate = element.getAsJsonObject();
            if (!"PLACE_FOOD".equals(string(candidate, "action"))) continue;
            require(PLACE_FOOD_ADVANCEMENT.equals(string(candidate, "advancementId")), "PLACE_FOOD outside delicious_hot_schmoes");
            require("minecraft:item_used_on_block".equals(string(candidate, "trigger")), "PLACE_FOOD has wrong trigger");
            require("#minecraft:campfires".equals(string(candidate, "blockTag")), "PLACE_FOOD has wrong block tag");
            require("POST_USE_CLICKED_POSITION".equals(string(candidate, "blockTagSampling")), "PLACE_FOOD has wrong block-tag sampling");
            require("frozenBacap".equals(string(candidate, "semanticsSource")), "PLACE_FOOD has wrong semantics source");
            String candidateKey = key(candidate);
            require(keys.add(candidateKey), "Duplicate PLACE_FOOD criterion: " + candidateKey);
            require(requirementGroups.add(candidate.get("requirementGroupIndex").getAsInt()), "Duplicate PLACE_FOOD requirement group: " + candidateKey);
            definitions.add(definitionFor(candidate.get("advancementId").getAsString(), candidate.get("criterion").getAsString()));
        }
        require(definitions.size() == 9 && keys.size() == 9 && requirementGroups.size() == 9, "Invalid delicious_hot_schmoes PLACE_FOOD coverage");
        definitions.sort(Comparator.comparingInt(definition -> definition.get("requirementGroupIndex").getAsInt()));
        return definitions;
    }

    public static JsonObject canaryDefinition() throws IOException {
        JsonObject canary = definitionFor(CANARY, CANARY_CRITERION);
        if (!"minecraft:honeycomb".equals(canary.get("heldItem").getAsString())
            || !"#minecraft:all_signs".equals(canary.get("blockTag").getAsString())
            || !"WAX_SIGN".equals(canary.get("action").getAsString())) throw new IllegalStateException("Invalid canary contract");
        return canary;
    }
    public static synchronized void beginRun() throws IOException {
        canaryDefinition();
        activeRun = new JsonObject(); activeRun.addProperty("family", FAMILY); activeRun.addProperty("source", SOURCE);
        activeRun.addProperty("catalogFingerprint", fingerprint()); activeRun.addProperty("runId", UUID.randomUUID().toString());
        activeRun.addProperty("minecraftVersion", "26.2"); activeRun.addProperty("compatibilityMarker", "compat_26_2_r15");
        activeRun.add("entries", new JsonArray()); write();
    }
    public static synchronized void assertExact30Catalog() throws IOException {
        JsonObject catalog = checkedCatalog();
        Set<String> keys = new HashSet<>();
        for (JsonElement element : catalog.getAsJsonArray("cases")) {
            JsonObject definition = element.getAsJsonObject();
            String criterion = string(definition, "criterion");
            require(!"bone_meal_propagule".equals(criterion), "Excluded criterion in exact30 catalog");
            require(keys.add(key(definition)), "Duplicate exact30 catalog criterion: " + key(definition));
        }
        require(keys.size() == 30, "Exact30 catalog key count is not 30");
    }
    public static synchronized void recordCanaryGreen(JsonObject receipt) throws IOException {
        if (!CANARY.equals(string(receipt, "advancementId")) || !CANARY_CRITERION.equals(string(receipt, "criterion"))) {
            throw new IllegalStateException("Only canary execution is implemented");
        }
        recordGreen(receipt);
    }

    public static synchronized void recordGreen(JsonObject receipt) throws IOException {
        require(activeRun != null, "Missing active run");
        String currentFingerprint = fingerprint();
        require(currentFingerprint.equals(string(activeRun, "catalogFingerprint")), "Missing/stale run");
        String advancementId = string(receipt, "advancementId");
        String criterion = string(receipt, "criterion");
        String receiptKey = key(advancementId, criterion);
        JsonObject definition = definitionFor(advancementId, criterion);
        JsonArray entries = activeRun.getAsJsonArray("entries");
        require(entries != null, "Missing active entries");
        for (JsonElement element : entries) if (receiptKey.equals(key(element.getAsJsonObject()))) throw new IllegalStateException("Duplicate criterion: " + receiptKey);
        for (String field : CATALOG_FIELDS) require(definition.get(field).equals(receipt.get(field)), "Wrong catalog binding: " + field);
        for (String field : OWNERSHIP_FIELDS) {
            if (receipt.has(field)) require(activeRun.get(field).equals(receipt.get(field)), "Wrong ownership: " + field);
        }
        if (receipt.has("result")) equal(receipt, "result", "GREEN");
        equal(receipt, "boundary", "ServerPlayerGameMode.useItemOn");
        equal(receipt, "gameMode", "SURVIVAL");
        equal(receipt, "hand", "MAIN_HAND");
        equal(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        for (String field : List.of("joined", "connectionRegistered", "clientLoaded", "normalScheduler", "buildPermission", "productionPreconditions", "runtimeTagMembership", "interactionConsumesAction", "semanticMutation")) bool(receipt, field, true);
        bool(receipt, "criterionBefore", false);
        bool(receipt, "criterionAfter", true);
        validateActionProof(receipt, definition, receiptKey);

        JsonObject entry = receipt.deepCopy();
        for (String field : OWNERSHIP_FIELDS) entry.add(field, activeRun.get(field).deepCopy());
        entry.addProperty("result", "GREEN");
        entries.add(entry);
        write();
    }

    public static synchronized void assertDyeSignCoverage() throws IOException {
        require(activeRun != null, "Missing active run");
        Set<String> expected = new HashSet<>();
        for (JsonObject definition : dyeSignDefinitions()) expected.add(key(definition));
        JsonArray entries = activeRun.getAsJsonArray("entries");
        require(entries != null, "Missing active entries");
        Set<String> actual = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject receipt = element.getAsJsonObject();
            String receiptKey = key(receipt);
            require(actual.add(receiptKey), "Duplicate DYE_SIGN criterion: " + receiptKey);
        }
        require(entries.size() == 16 && actual.size() == 16 && actual.equals(expected), "DYE_SIGN coverage is not exactly 16 catalog keys");
    }

    public static synchronized void assertDyeSignCoverageInExact30() throws IOException {
        assertActionCoverageInExact30(dyeSignDefinitions(), "DYE_SIGN");
    }

    public static synchronized void assertPlaceFoodCoverage() throws IOException {
        require(activeRun != null, "Missing active run");
        Set<String> expected = new HashSet<>();
        for (JsonObject definition : placeFoodDefinitions()) expected.add(key(definition));
        JsonArray entries = activeRun.getAsJsonArray("entries");
        require(entries != null, "Missing active entries");
        Set<String> actual = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject receipt = element.getAsJsonObject();
            String receiptKey = key(receipt);
            require(actual.add(receiptKey), "Duplicate PLACE_FOOD criterion: " + receiptKey);
        }
        require(entries.size() == 9 && actual.size() == 9 && actual.equals(expected), "PLACE_FOOD coverage is not exactly 9 catalog keys");
    }

    public static synchronized void assertPlaceFoodCoverageInExact30() throws IOException {
        assertActionCoverageInExact30(placeFoodDefinitions(), "PLACE_FOOD");
    }

    public static synchronized void assertExact30Coverage() throws IOException {
        require(activeRun != null, "Missing active run");
        JsonArray entries = activeRun.getAsJsonArray("entries");
        require(entries != null, "Missing active entries");

        JsonObject catalog = checkedCatalog();
        Set<String> expected = new HashSet<>();
        for (JsonElement element : catalog.getAsJsonArray("cases")) {
            JsonObject definition = element.getAsJsonObject();
            String criterion = string(definition, "criterion");
            require(!"bone_meal_propagule".equals(criterion), "Excluded criterion in exact30 catalog");
            require(expected.add(key(definition)), "Duplicate exact30 catalog criterion: " + key(definition));
        }
        require(expected.size() == 30, "Exact30 catalog key count is not 30");

        Set<String> actual = new HashSet<>();
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "Invalid exact30 receipt");
            JsonObject receipt = element.getAsJsonObject();
            String receiptKey = key(receipt);
            require(actual.add(receiptKey), "Duplicate exact30 criterion: " + receiptKey);
            require(expected.contains(receiptKey), "Unknown exact30 criterion: " + receiptKey);
            equal(receipt, "result", "GREEN");
            for (String field : OWNERSHIP_FIELDS) {
                require(receipt.has(field) && activeRun.has(field) && activeRun.get(field).equals(receipt.get(field)),
                    "Inconsistent exact30 ownership: " + field + " for " + receiptKey);
            }
        }
        require(entries.size() == 30 && actual.size() == 30 && actual.equals(expected),
            "Exact30 coverage is not exactly the current 30 catalog keys");
    }

    private static void assertActionCoverageInExact30(List<JsonObject> definitions, String action) {
        require(activeRun != null, "Missing active run");
        Set<String> expected = new HashSet<>();
        for (JsonObject definition : definitions) require(expected.add(key(definition)), "Duplicate " + action + " definition");
        JsonArray entries = activeRun.getAsJsonArray("entries");
        require(entries != null, "Missing active entries");
        Set<String> actual = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject receipt = element.getAsJsonObject();
            String receiptKey = key(receipt);
            if (expected.contains(receiptKey)) require(actual.add(receiptKey), "Duplicate " + action + " criterion: " + receiptKey);
        }
        require(actual.size() == expected.size() && actual.equals(expected), action + " exact30 coverage is incomplete");
    }

    private static void validateActionProof(JsonObject receipt, JsonObject definition, String receiptKey) throws IOException {
        String action = string(definition, "action");
        JsonObject proof = receipt.get("actionProof") != null && receipt.get("actionProof").isJsonObject()
            ? receipt.getAsJsonObject("actionProof") : null;
        require(proof != null, "Missing action proof");
        int heldBefore = receipt.get("heldCountBefore").getAsInt();
        int heldAfter = receipt.get("heldCountAfter").getAsInt();
        switch (action) {
            case "WAX_SIGN" -> {
                require(CANARY.equals(string(definition, "advancementId")) && CANARY_CRITERION.equals(string(definition, "criterion")), "Unsupported WAX_SIGN case");
                bool(proof, "waxedBefore", false); bool(proof, "waxedAfter", true);
                require(heldBefore == 1 && heldAfter == 0, "Missing canary held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1 && receipt.get("interactionStackCountAfter").getAsInt() == 0
                    && receipt.get("interactionStackConsumed").getAsBoolean(), "Missing canary interaction-stack mutation");
                require(!receipt.get("sameStackReference").getAsBoolean(), "Canary selected stack replacement expectation missing");
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing canary selected-hand observation");
            }
            case "DYE_SIGN" -> {
                require(WHITE_DYE.equals(string(definition, "advancementId")), "Unsupported DYE_SIGN advancement");
                require(receipt.has("activeSideFront") && receipt.get("activeSideFront").isJsonPrimitive() && receipt.get("activeSideFront").getAsJsonPrimitive().isBoolean(), "Missing active sign side");
                String expectedColor = catalogDyeColor(definition);
                equal(receipt, "expectedColor", expectedColor);
                bool(proof, "textPresent", true); bool(proof, "waxedBefore", false);
                if (proof.has("waxedAfter")) bool(proof, "waxedAfter", false);
                String colorBefore = string(proof, "colorBefore");
                String colorAfter = string(proof, "colorAfter");
                require(!expectedColor.equals(colorBefore), "Dye starting color is not contrasting");
                require(!colorBefore.equals(colorAfter), "No dye mutation");
                require(expectedColor.equals(colorAfter), "Wrong dye result");
                require(heldBefore == 1 && heldAfter == 0, "Missing dye held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1 && receipt.get("interactionStackCountAfter").getAsInt() == 0, "Missing dye interaction-stack mutation");
                bool(receipt, "interactionStackConsumed", true);
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandItemAfter").isJsonPrimitive() && receipt.getAsJsonPrimitive("selectedHandItemAfter").isString()
                    && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing dye selected-hand observation");
                require(receipt.has("sameStackReference") && receipt.get("sameStackReference").isJsonPrimitive() && receipt.getAsJsonPrimitive("sameStackReference").isBoolean(), "Missing dye stack-reference observation");
            }
            case "GLOW_SIGN" -> {
                require(GLOW_SIGN.equals(string(definition, "advancementId")) && GLOW_SIGN_CRITERION.equals(string(definition, "criterion")), "Unsupported GLOW_SIGN case");
                require(receipt.has("activeSideFront") && receipt.get("activeSideFront").isJsonPrimitive() && receipt.getAsJsonPrimitive("activeSideFront").isBoolean(), "Missing active sign side");
                bool(proof, "textPresent", true); bool(proof, "waxedBefore", false); bool(proof, "glowingBefore", false); bool(proof, "glowingAfter", true);
                if (proof.has("waxedAfter")) bool(proof, "waxedAfter", false);
                require(heldBefore == 1 && heldAfter == 0, "Missing glow-sign held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1 && receipt.get("interactionStackCountAfter").getAsInt() == 0, "Missing glow-sign interaction-stack mutation");
                bool(receipt, "interactionStackConsumed", true);
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandItemAfter").isJsonPrimitive() && receipt.getAsJsonPrimitive("selectedHandItemAfter").isString()
                    && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing glow-sign selected-hand observation");
                require(receipt.has("sameStackReference") && receipt.get("sameStackReference").isJsonPrimitive() && receipt.getAsJsonPrimitive("sameStackReference").isBoolean(), "Missing glow-sign stack-reference observation");
            }
            case "MAP_BANNER" -> {
                require(MAP_BANNER.equals(string(definition, "advancementId")) && MAP_BANNER_CRITERION.equals(string(definition, "criterion")), "Unsupported MAP_BANNER case");
                bool(proof, "mapCoversBanner", true); bool(proof, "markerBefore", false); bool(proof, "markerAfter", true);
                bool(proof, "markerMatchesBanner", true);
                String mapId = string(proof, "mapId");
                string(proof, "markerId"); string(proof, "bannerPosition");
                require(string(proof, "bannerPosition").equals(string(receipt, "clickedPosition")), "Banner position mismatch");
                require(string(proof, "markerPosition").equals(string(receipt, "clickedPosition")), "Map marker position mismatch");
                require(heldBefore == 1 && heldAfter == 1, "Filled map count changed");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1
                    && receipt.get("interactionStackCountAfter").getAsInt() == 1
                    && !receipt.get("interactionStackConsumed").getAsBoolean(), "Filled map interaction stack changed");
                require(receipt.has("mapIdAfter") && receipt.get("mapIdAfter").isJsonPrimitive() && receipt.getAsJsonPrimitive("mapIdAfter").isString()
                    && mapId.equals(string(receipt, "mapIdAfter")) && receipt.has("sameMapIdentity")
                    && receipt.get("sameMapIdentity").isJsonPrimitive() && receipt.getAsJsonPrimitive("sameMapIdentity").isBoolean()
                    && receipt.get("sameMapIdentity").getAsBoolean(), "Filled map identity changed");
                require(receipt.has("sameStackReference") && receipt.get("sameStackReference").isJsonPrimitive() && receipt.getAsJsonPrimitive("sameStackReference").isBoolean(), "Missing map stack-reference observation");
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandItemAfter").isJsonPrimitive() && receipt.getAsJsonPrimitive("selectedHandItemAfter").isString()
                    && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing map selected-hand observation");
            }
            case "PLACE_FOOD" -> {
                Set<String> expectedPlaceFoodKeys = new HashSet<>();
                for (JsonObject placeFoodDefinition : placeFoodDefinitions()) expectedPlaceFoodKeys.add(key(placeFoodDefinition));
                require(expectedPlaceFoodKeys.contains(receiptKey), "Unsupported PLACE_FOOD case: " + receiptKey);
                bool(proof, "placementAllowed", true); bool(proof, "campfireRecipe", true);
                bool(proof, "useCampfireUnlocked", true); bool(proof, "lockedLandmark", false);
                bool(proof, "slotEmptyBefore", true); equal(proof, "slotItemAfter", string(definition, "heldItem"));
                require(proof.get("slotCountAfter").getAsInt() == 1 && proof.get("slotIndex").getAsInt() >= 0 && proof.get("slotIndex").getAsInt() < 4, "No actual food placement");
                require(heldBefore == 1 && heldAfter == 0, "Missing PLACE_FOOD held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1
                    && receipt.get("interactionStackCountAfter").getAsInt() == 0
                    && receipt.get("interactionStackConsumed").getAsBoolean(), "Missing PLACE_FOOD interaction-stack mutation");
                require(receipt.has("sameStackReference") && receipt.get("sameStackReference").isJsonPrimitive()
                    && receipt.getAsJsonPrimitive("sameStackReference").isBoolean(), "Missing PLACE_FOOD stack-reference observation");
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandItemAfter").isJsonPrimitive()
                    && receipt.getAsJsonPrimitive("selectedHandItemAfter").isString()
                    && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing PLACE_FOOD selected-hand observation");
            }
            case "BOTTLE_HONEY" -> {
                require(HONEY_HARVEST.equals(string(definition, "advancementId"))
                    && HONEY_HARVEST_CRITERION.equals(string(definition, "criterion")), "Unsupported BOTTLE_HONEY case");
                bool(proof, "litCampfireBelow", true);
                bool(proof, "smokeyBefore", true);
                bool(proof, "smokeyAfter", true);
                require(proof.get("honeyLevelBefore").getAsInt() == 5
                    && proof.get("honeyLevelAfter").getAsInt() == 0, "Honey level unchanged");
                bool(proof, "honeyBottleProduced", true);
                require(heldBefore == 1 && heldAfter == 0, "Missing honey held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1
                    && receipt.get("interactionStackCountAfter").getAsInt() == 0
                    && receipt.get("interactionStackConsumed").getAsBoolean(), "Missing honey interaction-stack mutation");
                require(!receipt.get("sameStackReference").getAsBoolean(), "Honey selected stack replacement expectation missing");
                require(receipt.has("selectedHandItemAfter")
                    && "minecraft:honey_bottle".equals(receipt.get("selectedHandItemAfter").getAsString())
                    && receipt.get("selectedHandCountAfter").getAsInt() == 1
                    && receipt.get("selectedHandSlot").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() < 9, "Missing honey selected-hand replacement");
            }
            case "GROW_OAK_SAPLING" -> {
                require(BONE_MEAL_ADVANCEMENT.equals(string(definition, "advancementId"))
                    && BONE_MEAL_CRITERION.equals(string(definition, "criterion")), "Unsupported GROW_OAK_SAPLING case");
                bool(proof, "planted", true);
                bool(proof, "validSupport", true);
                bool(proof, "clearGrowthVolume", true);
                bool(proof, "successfulTreeGrowth", true);
                require(string(proof, "mutationPosition").equals(string(receipt, "clickedPosition")), "Tree mutation position mismatch");
                equal(proof, "mutationBlockBefore", "minecraft:oak_sapling");
                require(proof.has("saplingStageBefore") && proof.get("saplingStageBefore").isJsonPrimitive()
                    && proof.get("saplingStageBefore").getAsInt() == 1, "Expected stage-1 oak sapling");
                equal(proof, "mutationBlockAfter", "minecraft:oak_log");
                require(heldBefore == 1 && heldAfter == 0, "Missing bone-meal held-stack mutation");
                require(receipt.get("interactionStackCountBefore").getAsInt() == 1
                    && receipt.get("interactionStackCountAfter").getAsInt() == 0
                    && receipt.get("interactionStackConsumed").getAsBoolean(), "Missing bone-meal interaction-stack mutation");
                require(receipt.has("sameStackReference") && receipt.get("sameStackReference").isJsonPrimitive()
                    && receipt.getAsJsonPrimitive("sameStackReference").isBoolean(), "Missing bone-meal stack-reference observation");
                require(receipt.has("selectedHandItemAfter") && receipt.get("selectedHandItemAfter").isJsonPrimitive()
                    && receipt.getAsJsonPrimitive("selectedHandItemAfter").isString()
                    && receipt.get("selectedHandCountAfter").getAsInt() >= 0
                    && receipt.get("selectedHandSlot").getAsInt() >= 0 && receipt.get("selectedHandSlot").getAsInt() < 9,
                    "Missing bone-meal selected-hand observation");
            }
            default -> throw new IllegalStateException("Unsupported action in this implementation slice: " + action);
        }
    }

    private static String catalogDyeColor(JsonObject definition) {
        String heldItem = string(definition, "heldItem");
        String prefix = "minecraft:";
        String suffix = "_dye";
        require(heldItem.startsWith(prefix) && heldItem.endsWith(suffix), "Invalid DYE_SIGN held item");
        String color = heldItem.substring(prefix.length(), heldItem.length() - suffix.length());
        require(!color.isBlank(), "Invalid DYE_SIGN color");
        return color;
    }

    private static JsonObject checkedCatalog() throws IOException {
        require(CATALOG_FINGERPRINT.equals(fingerprint()), "Unreviewed catalog fingerprint");
        JsonObject catalog = JsonParser.parseString(Files.readString(projectRoot().resolve(CATALOG))).getAsJsonObject();
        require(FAMILY.equals(string(catalog, "family")), "Wrong catalog family");
        require(catalog.has("cases") && catalog.get("cases").isJsonArray() && catalog.getAsJsonArray("cases").size() == 30, "Invalid exact30 catalog");
        require(catalog.get("totalCases").getAsInt() == 30 && catalog.get("uniqueAdvancements").getAsInt() == 7
            && catalog.get("requirementGroups").getAsInt() == 30, "Invalid catalog structure");
        return catalog;
    }

    private static String key(String advancementId, String criterion) {
        require(advancementId != null && !advancementId.isBlank() && criterion != null && !criterion.isBlank(), "Blank criterion key");
        return advancementId + "#" + criterion;
    }

    private static String key(JsonObject object) { return key(string(object, "advancementId"), string(object, "criterion")); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static String string(JsonObject object, String field) {
        require(object != null && object.has(field) && object.get(field).isJsonPrimitive() && object.getAsJsonPrimitive(field).isString(), "Missing string: " + field);
        String value = object.get(field).getAsString(); require(!value.isBlank(), "Blank " + field); return value;
    }
    private static void equal(JsonObject object, String field, String expected) { require(expected.equals(string(object, field)), "Wrong " + field); }
    private static void bool(JsonObject object, String field, boolean expected) {
        require(object != null && object.has(field) && object.get(field).isJsonPrimitive() && object.getAsJsonPrimitive(field).isBoolean()
            && object.get(field).getAsBoolean() == expected, "Wrong " + field);
    }
    private static void write() throws IOException {
        Path path = projectRoot().resolve(TEMP); Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(activeRun) + "\n");
    }
}
