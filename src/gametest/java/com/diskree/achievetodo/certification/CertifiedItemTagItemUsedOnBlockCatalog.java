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

/** Runtime view of the exact frozen-derived ITEM_TAG_ITEM_USED_ON_BLOCK catalog. */
public final class CertifiedItemTagItemUsedOnBlockCatalog {
    private static final Path CATALOG_PATH = PhaseAItemTagItemUsedOnBlockExecutionEvidence.CATALOG_PATH;
    private static final String SNAPSHOT = "phase_a_item_tag_item_used_on_block_case_catalog";
    private static final String FAMILY = "ITEM_TAG_ITEM_USED_ON_BLOCK";
    private static final String TRIGGER = "minecraft:item_used_on_block";
    private static final String SEMANTICS_SOURCE = "frozenBacap";
    private static final String SUPPORTED = "SUPPORTED";
    private static final List<String> EXPECTED_KEYS = List.of(
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
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedItemTagItemUsedOnBlockCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedItemTagItemUsedOnBlockCatalog.class) {
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
        throw new IllegalArgumentException("Missing certified ITEM_TAG_ITEM_USED_ON_BLOCK case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path catalogPath = PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(catalogPath, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", SEMANTICS_SOURCE);
            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", 15);
            requireInt(summary, "uniqueKeys", 15);
            requireInt(summary, "uniqueAdvancements", 4);
            requireInt(summary, "requirementGroups", 15);
            requireInt(summary, "automationSupported", 15);
            requireInt(summary, "automationDeferred", 0);
            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_KEYS.size()) {
                throw new IllegalStateException("Expected exactly 15 ITEM_TAG_ITEM_USED_ON_BLOCK cases but found " + casesJson.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : casesJson) {
                CaseDefinition caseDefinition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(caseDefinition.key())) {
                    throw new IllegalStateException("Duplicate catalog key: " + caseDefinition.key());
                }
                cases.add(caseDefinition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog key set/order mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ITEM_TAG_ITEM_USED_ON_BLOCK catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in ITEM_TAG_ITEM_USED_ON_BLOCK catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in ITEM_TAG_ITEM_USED_ON_BLOCK catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key + " in ITEM_TAG_ITEM_USED_ON_BLOCK catalog");
        }
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank " + key + " in ITEM_TAG_ITEM_USED_ON_BLOCK catalog");
        }
        return value;
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key + " in ITEM_TAG_ITEM_USED_ON_BLOCK catalog");
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    public record CaseDefinition(
        Identifier advancementId,
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
        static CaseDefinition fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String criterion = requiredString(json, "criterion");
            requireString(json, "trigger", TRIGGER);
            requireString(json, "automationEligibility", SUPPORTED);
            requireString(json, "semanticsSource", SEMANTICS_SOURCE);
            String itemTag = requiredString(json, "itemTag");
            String action = requiredString(json, "action");
            String fixtureBlockBefore = requiredString(json, "fixtureBlockBefore");
            String expectedPostBlock = requiredString(json, "expectedPostBlock");
            String preferredToolItem = requiredString(json, "preferredToolItem");
            JsonObject predicate = requiredObject(json, "blockPredicate");
            List<String> predicateBlocks = readStringArray(predicate, "blocks");
            Map<String, String> blockStatePredicate = readState(predicate, "state", false);
            Map<String, String> fixtureStateBefore = readState(json, "fixtureStateBefore", true);
            Map<String, String> expectedPostState = readState(json, "expectedPostState", true);
            return new CaseDefinition(
                Identifier.parse(advancementId),
                requiredString(json, "sourcePath"),
                criterion,
                requiredInt(json, "requirementGroupIndex"),
                TRIGGER,
                itemTag,
                List.copyOf(predicateBlocks),
                Map.copyOf(blockStatePredicate),
                action,
                fixtureBlockBefore,
                expectedPostBlock,
                Map.copyOf(fixtureStateBefore),
                Map.copyOf(expectedPostState),
                preferredToolItem,
                SUPPORTED,
                requiredString(json, "deferReason"),
                SEMANTICS_SOURCE,
                requiredString(json, "sourceJsonSha256")
            );
        }

        public String key() {
            return advancementId + "#" + criterion;
        }

        private static List<String> readStringArray(JsonObject parent, String key) {
            JsonArray array = parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : null;
            if (array == null || array.isEmpty()) {
                throw new IllegalStateException("Missing non-empty " + key + " in catalog case");
            }
            List<String> values = new ArrayList<>();
            for (JsonElement element : array) {
                values.add(element.getAsString());
            }
            return values;
        }

        private static Map<String, String> readState(JsonObject parent, String key, boolean required) {
            if (!parent.has(key)) {
                if (required) {
                    throw new IllegalStateException("Missing " + key + " in catalog case");
                }
                return Map.of();
            }
            JsonObject state = parent.getAsJsonObject(key);
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            }
            return values;
        }
    }
}
