package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Derives the ITEM_TAG_ITEM_USED_ON_BLOCK catalog from the immutable BACAP
 * advancement definitions.  The four accepted advancement IDs are the
 * frontier assignment; no source-wide inventory is performed here.
 */
public final class PhaseAItemTagItemUsedOnBlockCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "item_tag_item_used_on_block_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_item_tag_item_used_on_block_case_catalog";
    public static final String FAMILY = "ITEM_TAG_ITEM_USED_ON_BLOCK";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:item_used_on_block";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String SEMANTICS_SOURCE = "frozenBacap";

    public static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:building/lost_its_bark#stripped_wood",
        "blazeandcave:building/pathways#dirt_path",
        "blazeandcave:building/stripper#stripped_oak_log",
        "blazeandcave:building/stripper#stripped_spruce_log",
        "blazeandcave:building/stripper#stripped_birch_log",
        "blazeandcave:building/stripper#stripped_jungle_log",
        "blazeandcave:building/stripper#stripped_acacia_log",
        "blazeandcave:building/stripper#stripped_dark_oak_log",
        "blazeandcave:building/stripper#stripped_mangrove_log",
        "blazeandcave:building/stripper#stripped_bamboo_block",
        "blazeandcave:building/stripper#stripped_cherry_log",
        "blazeandcave:building/stripper#stripped_crimson_stem",
        "blazeandcave:building/stripper#stripped_warped_stem",
        "blazeandcave:building/stripper#stripped_pale_oak_log",
        "minecraft:adventure/lighten_up#lighten_up"
    );

    private static final String ZIP_SHA1 = FROZEN_BACAP_SHA1;
    private static final String ZIP_SHA256 = FROZEN_BACAP_SHA256;
    private static final String MINECRAFT_VERSION = "26.2";
    private static final Map<String, String> STRIP_LOG_SOURCES = Map.ofEntries(
        Map.entry("minecraft:stripped_oak_log", "minecraft:oak_log"),
        Map.entry("minecraft:stripped_spruce_log", "minecraft:spruce_log"),
        Map.entry("minecraft:stripped_birch_log", "minecraft:birch_log"),
        Map.entry("minecraft:stripped_jungle_log", "minecraft:jungle_log"),
        Map.entry("minecraft:stripped_acacia_log", "minecraft:acacia_log"),
        Map.entry("minecraft:stripped_dark_oak_log", "minecraft:dark_oak_log"),
        Map.entry("minecraft:stripped_mangrove_log", "minecraft:mangrove_log"),
        Map.entry("minecraft:stripped_bamboo_block", "minecraft:bamboo_block"),
        Map.entry("minecraft:stripped_cherry_log", "minecraft:cherry_log"),
        Map.entry("minecraft:stripped_crimson_stem", "minecraft:crimson_stem"),
        Map.entry("minecraft:stripped_warped_stem", "minecraft:warped_stem"),
        Map.entry("minecraft:stripped_pale_oak_log", "minecraft:pale_oak_log")
    );

    private static final List<FrontierAdvancement> FRONTIER = List.of(
        new FrontierAdvancement(
            "blazeandcave:building/lost_its_bark",
            "data/blazeandcave/advancement/building/lost_its_bark.json",
            List.of("stripped_wood"),
            "minecraft:axes",
            "STRIP_WOOD"
        ),
        new FrontierAdvancement(
            "blazeandcave:building/pathways",
            "data/blazeandcave/advancement/building/pathways.json",
            List.of("dirt_path"),
            "minecraft:shovels",
            "CREATE_PATH"
        ),
        new FrontierAdvancement(
            "blazeandcave:building/stripper",
            "data/blazeandcave/advancement/building/stripper.json",
            List.of(
                "stripped_oak_log",
                "stripped_spruce_log",
                "stripped_birch_log",
                "stripped_jungle_log",
                "stripped_acacia_log",
                "stripped_dark_oak_log",
                "stripped_mangrove_log",
                "stripped_bamboo_block",
                "stripped_cherry_log",
                "stripped_crimson_stem",
                "stripped_warped_stem",
                "stripped_pale_oak_log"
            ),
            "minecraft:axes",
            "STRIP_LOG"
        ),
        new FrontierAdvancement(
            "minecraft:adventure/lighten_up",
            "data/minecraft/advancement/adventure/lighten_up.json",
            List.of("lighten_up"),
            "blazeandcave:axes",
            "AXE_COPPER_MUTATION"
        )
    );

    private static final Map<String, String> WOOD_SOURCES = Map.of(
        "minecraft:stripped_oak_wood", "minecraft:oak_wood"
    );
    private static final String PATH_SOURCE = "minecraft:dirt";
    private static final String PATH_TARGET = "minecraft:dirt_path";
    private static final String COPPER_SOURCE = "minecraft:waxed_oxidized_copper_bulb";
    private static final String COPPER_TARGET = "minecraft:oxidized_copper_bulb";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagItemUsedOnBlockCertification() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected project root path");
        }
        writeSnapshot(Path.of(args[0]).toAbsolutePath().normalize());
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Path output = projectRoot.resolve(SNAPSHOT);
        Files.createDirectories(output.getParent());
        Files.writeString(output, generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        Path bacapZip = projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE);
        verifyFrozenBacap(bacapZip);
        List<CaseDefinition> cases = deriveCasesFromBacapZip(bacapZip);
        assertExactCatalog(cases);

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", FAMILY);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        root.addProperty("semanticsSource", SEMANTICS_SOURCE);
        root.addProperty("frozenBacapSha1", FROZEN_BACAP_SHA1);
        root.addProperty("frozenBacapSha256", FROZEN_BACAP_SHA256);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueKeys", cases.stream().map(CaseDefinition::key).distinct().count());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("requirementGroups", cases.size());
        summary.addProperty("automationSupported", cases.stream().filter(c -> SUPPORTED.equals(c.automationEligibility())).count());
        summary.addProperty("automationDeferred", cases.stream().filter(c -> !SUPPORTED.equals(c.automationEligibility())).count());
        JsonObject actionCategories = new JsonObject();
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (CaseDefinition caseDefinition : cases) {
            categoryCounts.merge(caseDefinition.action(), 1L, Long::sum);
        }
        categoryCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> actionCategories.addProperty(entry.getKey(), entry.getValue()));
        summary.add("actionCategories", actionCategories);
        root.add("summary", summary);

        JsonArray casesJson = new JsonArray();
        for (CaseDefinition caseDefinition : cases) {
            casesJson.add(caseDefinition.toJson());
        }
        root.add("cases", casesJson);
        return GSON.toJson(root) + "\n";
    }

    public static List<CaseDefinition> deriveCases(Path projectRoot) throws IOException {
        return deriveCasesFromBacapZip(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
    }

    private static List<CaseDefinition> deriveCasesFromBacapZip(Path bacapZip) throws IOException {
        List<CaseDefinition> cases = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(bacapZip.toFile(), StandardCharsets.UTF_8)) {
            for (FrontierAdvancement frontier : FRONTIER) {
                RawAdvancement raw = loadRawAdvancement(zipFile, frontier.sourcePath());
                JsonObject criteria = requiredObject(raw.json(), "criteria", frontier.advancementId());
                List<String> criterionOrder = new ArrayList<>(criteria.keySet());
                if (!criterionOrder.equals(frontier.criteria())) {
                    throw new IllegalStateException("Frozen criterion order changed for " + frontier.advancementId()
                        + ": expected=" + frontier.criteria() + ", actual=" + criterionOrder);
                }
                verifySingletonRequirements(raw.json(), frontier);
                for (int requirementGroupIndex = 0; requirementGroupIndex < frontier.criteria().size(); requirementGroupIndex++) {
                    String criterion = frontier.criteria().get(requirementGroupIndex);
                    JsonObject criterionJson = criteria.getAsJsonObject(criterion);
                    cases.add(deriveCase(frontier, raw, criterion, criterionJson, requirementGroupIndex));
                }
            }
        }
        return List.copyOf(cases);
    }

    private static CaseDefinition deriveCase(
        FrontierAdvancement frontier,
        RawAdvancement raw,
        String criterion,
        JsonObject criterionJson,
        int requirementGroupIndex
    ) {
        String trigger = requiredString(criterionJson, "trigger", frontier.advancementId() + "#" + criterion);
        if (!TRIGGER.equals(trigger)) {
            throw new IllegalStateException("Unexpected trigger for " + frontier.advancementId() + "#" + criterion + ": " + trigger);
        }
        JsonObject conditions = requiredObject(criterionJson, "conditions", criterion);
        JsonArray locationConditions = requiredArray(conditions, "location", criterion);
        JsonObject blockPredicate = null;
        String itemTag = null;
        for (JsonElement locationElement : locationConditions) {
            JsonObject location = locationElement.getAsJsonObject();
            String condition = requiredString(location, "condition", criterion);
            JsonObject predicate = requiredObject(location, "predicate", criterion);
            if ("minecraft:location_check".equals(condition)) {
                blockPredicate = requiredObject(predicate, "block", criterion);
            } else if ("minecraft:match_tool".equals(condition)) {
                itemTag = extractItemTag(predicate, criterion);
            } else {
                throw new IllegalStateException("Unexpected item_used_on_block condition for " + criterion + ": " + condition);
            }
        }
        if (blockPredicate == null || itemTag == null) {
            throw new IllegalStateException("Incomplete item_used_on_block predicate for " + frontier.advancementId() + "#" + criterion);
        }
        if (!frontier.itemTag().equals(itemTag)) {
            throw new IllegalStateException("Unexpected item tag for " + frontier.advancementId() + "#" + criterion
                + ": expected=" + frontier.itemTag() + ", actual=" + itemTag);
        }

        List<String> predicateBlocks = extractBlockIds(blockPredicate, frontier.advancementId() + "#" + criterion);
        Map<String, String> blockStatePredicate = extractState(blockPredicate, criterion);
        String key = frontier.advancementId() + "#" + criterion;
        String fixtureBlockBefore;
        String expectedPostBlock;
        Map<String, String> fixtureStateBefore = Map.of();
        Map<String, String> expectedPostState = Map.of();
        String action = frontier.action();
        String preferredToolItem = "minecraft:diamond_axe";
        if ("STRIP_WOOD".equals(action)) {
            expectedPostBlock = "minecraft:stripped_oak_wood";
            fixtureBlockBefore = WOOD_SOURCES.get(expectedPostBlock);
            requireContains(predicateBlocks, expectedPostBlock, key);
        } else if ("CREATE_PATH".equals(action)) {
            expectedPostBlock = PATH_TARGET;
            fixtureBlockBefore = PATH_SOURCE;
            preferredToolItem = "minecraft:diamond_shovel";
            requireExactBlocks(predicateBlocks, List.of(PATH_TARGET), key);
        } else if ("STRIP_LOG".equals(action)) {
            expectedPostBlock = "minecraft:" + criterion;
            fixtureBlockBefore = STRIP_LOG_SOURCES.get(expectedPostBlock);
            if (fixtureBlockBefore == null) {
                throw new IllegalStateException("No ordinary source fixture for " + key);
            }
            requireExactBlocks(predicateBlocks, List.of(expectedPostBlock), key);
        } else if ("AXE_COPPER_MUTATION".equals(action)) {
            expectedPostBlock = COPPER_TARGET;
            fixtureBlockBefore = COPPER_SOURCE;
            requireContains(predicateBlocks, expectedPostBlock, key);
            if (!"true".equals(blockStatePredicate.get("lit"))) {
                throw new IllegalStateException("Lighten Up frozen predicate is not lit=true for " + key);
            }
            fixtureStateBefore = Map.of("lit", "true");
            expectedPostState = Map.of("lit", "true");
        } else {
            throw new IllegalStateException("Unsupported action category for " + key + ": " + action);
        }

        return new CaseDefinition(
            frontier.advancementId(),
            frontier.sourcePath(),
            criterion,
            requirementGroupIndex,
            trigger,
            itemTag,
            List.copyOf(predicateBlocks),
            Map.copyOf(blockStatePredicate),
            action,
            fixtureBlockBefore,
            expectedPostBlock,
            fixtureStateBefore,
            expectedPostState,
            preferredToolItem,
            SUPPORTED,
            "EXACT_FROZEN_ITEM_USED_ON_BLOCK_SEMANTICS",
            SEMANTICS_SOURCE,
            raw.sha256()
        );
    }

    private static void verifySingletonRequirements(JsonObject advancement, FrontierAdvancement frontier) {
        JsonElement requirementsElement = advancement.get("requirements");
        if (requirementsElement == null || requirementsElement.isJsonNull()) {
            return;
        }
        if (!requirementsElement.isJsonArray()) {
            throw new IllegalStateException("requirements is not an array for " + frontier.advancementId());
        }
        JsonArray requirements = requirementsElement.getAsJsonArray();
        if (requirements.size() != frontier.criteria().size()) {
            throw new IllegalStateException("Expected one requirement group per criterion for " + frontier.advancementId());
        }
        for (int index = 0; index < requirements.size(); index++) {
            JsonArray group = requirements.get(index).getAsJsonArray();
            if (group.size() != 1 || !frontier.criteria().get(index).equals(group.get(0).getAsString())) {
                throw new IllegalStateException("Requirement group " + index + " is not the accepted singleton group for " + frontier.advancementId());
            }
        }
    }

    private static List<String> extractBlockIds(JsonObject blockPredicate, String key) {
        JsonArray blocks = requiredArray(blockPredicate, "blocks", key);
        List<String> result = new ArrayList<>();
        for (JsonElement element : blocks) {
            String blockId = element.getAsString();
            result.add(blockId.contains(":") ? blockId : "minecraft:" + blockId);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Empty block predicate for " + key);
        }
        return List.copyOf(result);
    }

    private static Map<String, String> extractState(JsonObject blockPredicate, String key) {
        JsonElement stateElement = blockPredicate.get("state");
        if (stateElement == null || stateElement.isJsonNull()) {
            return Map.of();
        }
        JsonObject state = stateElement.getAsJsonObject();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Empty block state predicate for " + key);
        }
        return Map.copyOf(result);
    }

    private static String extractItemTag(JsonObject predicate, String key) {
        JsonElement items = predicate.get("items");
        if (items == null || !items.isJsonPrimitive() || !items.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException("Missing string item tag predicate for " + key);
        }
        String value = items.getAsString();
        if (!value.startsWith("#") || value.length() == 1) {
            throw new IllegalStateException("Expected item tag selector for " + key + ": " + value);
        }
        return value.substring(1);
    }

    private static void assertExactCatalog(List<CaseDefinition> cases) {
        List<String> actual = cases.stream().map(CaseDefinition::key).toList();
        if (!actual.equals(EXPECTED_KEYS)) {
            Set<String> missing = new LinkedHashSet<>(EXPECTED_KEYS);
            missing.removeAll(actual);
            Set<String> extras = new LinkedHashSet<>(actual);
            extras.removeAll(EXPECTED_KEYS);
            throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog mismatch | missing=" + missing + " | extras=" + extras + " | order=" + actual);
        }
        if (cases.size() != 15 || actual.stream().distinct().count() != 15) {
            throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog must contain exactly 15 unique cases");
        }
        if (cases.stream().map(CaseDefinition::advancementId).distinct().count() != 4) {
            throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog must contain exactly four advancements");
        }
        Map<String, Set<Integer>> actualRequirementGroupsByAdvancement = new LinkedHashMap<>();
        for (CaseDefinition caseDefinition : cases) {
            actualRequirementGroupsByAdvancement
                .computeIfAbsent(caseDefinition.advancementId(), ignored -> new LinkedHashSet<>())
                .add(caseDefinition.requirementGroupIndex());
        }
        for (FrontierAdvancement frontier : FRONTIER) {
            Set<Integer> expectedRequirementGroups = new LinkedHashSet<>();
            for (int index = 0; index < frontier.criteria().size(); index++) {
                expectedRequirementGroups.add(index);
            }
            Set<Integer> actualRequirementGroups = actualRequirementGroupsByAdvancement.get(frontier.advancementId());
            if (!expectedRequirementGroups.equals(actualRequirementGroups)) {
                throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog requirement-group set mismatch for "
                    + frontier.advancementId() + " | expected=" + expectedRequirementGroups + " | actual=" + actualRequirementGroups);
            }
        }
    }

    private static RawAdvancement loadRawAdvancement(ZipFile zipFile, String sourcePath) throws IOException {
        ZipEntry entry = zipFile.getEntry(sourcePath);
        if (entry == null) {
            throw new IllegalStateException("Missing frozen BACAP advancement: " + sourcePath);
        }
        byte[] bytes;
        try (InputStream input = zipFile.getInputStream(entry)) {
            bytes = input.readAllBytes();
        }
        return new RawAdvancement(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(), sha256(bytes));
    }

    private static void verifyFrozenBacap(Path bacapZip) throws IOException {
        if (!Files.exists(bacapZip)) {
            throw new IllegalStateException("Missing frozen BACAP semantics source: " + bacapZip);
        }
        byte[] bytes = Files.readAllBytes(bacapZip);
        String actualSha1 = digest("SHA-1", bytes);
        String actualSha256 = digest("SHA-256", bytes);
        if (!ZIP_SHA1.equals(actualSha1) || !ZIP_SHA256.equals(actualSha256)) {
            throw new IllegalStateException("Frozen BACAP hash mismatch | sha1=" + actualSha1 + " | sha256=" + actualSha256);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " for " + context);
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " for " + context);
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing string " + key + " for " + context);
        }
        return value.getAsString();
    }

    private static void requireContains(List<String> actual, String expected, String key) {
        if (!actual.contains(expected)) {
            throw new IllegalStateException("Frozen block predicate does not contain " + expected + " for " + key);
        }
    }

    private static void requireExactBlocks(List<String> actual, List<String> expected, String key) {
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Frozen block predicate mismatch for " + key + " | expected=" + expected + " | actual=" + actual);
        }
    }

    private static String sha256(byte[] bytes) {
        return digest("SHA-256", bytes);
    }

    private static String digest(String algorithm, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] result = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte value : result) {
                hex.append(Character.forDigit((value >>> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest algorithm " + algorithm, e);
        }
    }

    private record FrontierAdvancement(
        String advancementId,
        String sourcePath,
        List<String> criteria,
        String itemTag,
        String action
    ) {
    }

    private record RawAdvancement(JsonObject json, String sha256) {
    }

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String itemTag,
        List<String> predicateBlocks,
        Map<String, String> blockStatePredicate,
        String action,
        String fixtureBlockBefore,
        String expectedPostBlock,
        Map<String, String> fixtureStateBefore,
        Map<String, String> expectedPostState,
        String preferredToolItem,
        String automationEligibility,
        String deferReason,
        String semanticsSource,
        String sourceJsonSha256
    ) {
        public String key() {
            return advancementId + "#" + criterion;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("criterion", criterion);
            json.addProperty("requirementGroupIndex", requirementGroupIndex);
            json.addProperty("trigger", trigger);
            json.addProperty("itemTag", itemTag);
            JsonObject predicate = new JsonObject();
            JsonArray blocks = new JsonArray();
            for (String block : predicateBlocks) {
                blocks.add(block);
            }
            predicate.add("blocks", blocks);
            JsonObject state = new JsonObject();
            blockStatePredicate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> state.addProperty(entry.getKey(), entry.getValue()));
            if (!state.isEmpty()) {
                predicate.add("state", state);
            }
            json.add("blockPredicate", predicate);
            json.addProperty("action", action);
            json.addProperty("fixtureBlockBefore", fixtureBlockBefore);
            json.addProperty("expectedPostBlock", expectedPostBlock);
            addState(json, "fixtureStateBefore", fixtureStateBefore);
            addState(json, "expectedPostState", expectedPostState);
            json.addProperty("preferredToolItem", preferredToolItem);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("deferReason", deferReason);
            json.addProperty("semanticsSource", semanticsSource);
            json.addProperty("sourceJsonSha256", sourceJsonSha256);
            return json;
        }

        private static void addState(JsonObject parent, String key, Map<String, String> stateValues) {
            JsonObject state = new JsonObject();
            stateValues.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> state.addProperty(entry.getKey(), entry.getValue()));
            parent.add(key, state);
        }
    }
}
