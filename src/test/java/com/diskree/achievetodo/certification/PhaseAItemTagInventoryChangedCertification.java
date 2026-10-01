package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAItemTagInventoryChangedCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_changed_case_catalog.json");

    private static final String SNAPSHOT_ID = "phase_a_item_tag_inventory_changed_case_catalog";
    private static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String DEFERRED = "DEFERRED";
    private static final String INVENTORY_CHANGED_TRIGGER = "minecraft:inventory_changed";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagInventoryChangedCertification() {
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
        List<CaseDefinition> cases = loadCases(projectRoot);
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("automationSupported", cases.stream().filter(caseDefinition -> SUPPORTED.equals(caseDefinition.automationEligibility())).count());
        summary.addProperty("automationDeferred", cases.stream().filter(caseDefinition -> DEFERRED.equals(caseDefinition.automationEligibility())).count());
        JsonObject byEligibility = new JsonObject();
        Map<String, Long> countsByEligibility = new TreeMap<>();
        for (CaseDefinition caseDefinition : cases) {
            countsByEligibility.merge(caseDefinition.automationEligibility(), 1L, Long::sum);
        }
        for (Map.Entry<String, Long> entry : countsByEligibility.entrySet()) {
            byEligibility.addProperty(entry.getKey(), entry.getValue());
        }
        summary.add("byEligibility", byEligibility);
        root.add("summary", summary);

        JsonArray casesJson = new JsonArray();
        for (CaseDefinition caseDefinition : cases) {
            JsonObject caseJson = new JsonObject();
            caseJson.addProperty("advancementId", caseDefinition.advancementId());
            caseJson.addProperty("sourcePath", caseDefinition.sourcePath());
            caseJson.addProperty("criterion", caseDefinition.criterion());
            caseJson.addProperty("trigger", caseDefinition.trigger());
            if (caseDefinition.itemTag() == null) {
                caseJson.add("itemTag", null);
            } else {
                caseJson.addProperty("itemTag", caseDefinition.itemTag());
            }
            if (caseDefinition.requiredCountMin() == null) {
                caseJson.add("requiredCountMin", null);
            } else {
                caseJson.addProperty("requiredCountMin", caseDefinition.requiredCountMin());
            }
            if (caseDefinition.requiredCountMax() == null) {
                caseJson.add("requiredCountMax", null);
            } else {
                caseJson.addProperty("requiredCountMax", caseDefinition.requiredCountMax());
            }
            JsonArray conditionKeys = new JsonArray();
            for (String conditionKey : caseDefinition.conditionKeys()) {
                conditionKeys.add(conditionKey);
            }
            caseJson.add("conditionKeys", conditionKeys);
            caseJson.addProperty("automationEligibility", caseDefinition.automationEligibility());
            caseJson.addProperty("eligibilityReason", caseDefinition.eligibilityReason());
            casesJson.add(caseJson);
        }
        root.add("cases", casesJson);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private static List<CaseDefinition> loadCases(Path projectRoot) throws IOException {
        List<InventoryEntry> inventoryEntries = loadCanonicalInventory(projectRoot);
        if (inventoryEntries.size() != PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS) {
            throw new IllegalStateException(
                "Expected canonical advancement inventory size " + PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS + " but found " + inventoryEntries.size()
            );
        }

        Map<String, JsonObject> rawAdvancements = loadFrozenAdvancements(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
        List<CaseDefinition> cases = new ArrayList<>();
        for (InventoryEntry inventoryEntry : inventoryEntries) {
            JsonObject advancementJson = rawAdvancements.get(inventoryEntry.sourcePath());
            if (advancementJson == null) {
                throw new IllegalStateException("Missing frozen advancement JSON for " + inventoryEntry.advancementId() + " at " + inventoryEntry.sourcePath());
            }
            cases.add(selectCase(inventoryEntry, advancementJson));
        }
        cases.sort(Comparator.comparing(CaseDefinition::advancementId).thenComparing(CaseDefinition::criterion));
        return List.copyOf(cases);
    }

    private static CaseDefinition selectCase(InventoryEntry inventoryEntry, JsonObject advancementJson) {
        JsonObject criteria = advancementJson.getAsJsonObject("criteria");
        if (criteria == null || criteria.entrySet().isEmpty()) {
            return new CaseDefinition(
                inventoryEntry.advancementId(),
                inventoryEntry.sourcePath(),
                "",
                "",
                null,
                null,
                null,
                List.of(),
                DEFERRED,
                "NO_CRITERIA"
            );
        }

        List<CriterionCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, JsonElement> criterionEntry : criteria.entrySet()) {
            candidates.add(analyzeCriterion(criterionEntry.getKey(), criterionEntry.getValue().getAsJsonObject()));
        }
        candidates.sort(Comparator.comparing(CriterionCandidate::criterion));

        List<CriterionCandidate> supportedCandidates = candidates.stream()
            .filter(candidate -> SUPPORTED.equals(candidate.automationEligibility()))
            .toList();
        if (supportedCandidates.size() > 1) {
            throw new IllegalStateException(
                "Advancement " + inventoryEntry.advancementId() + " has multiple SUPPORTED criteria: "
                    + supportedCandidates.stream().map(CriterionCandidate::criterion).sorted().toList()
            );
        }
        CriterionCandidate selectedCandidate = supportedCandidates.isEmpty() ? null : supportedCandidates.getFirst();
        if (selectedCandidate == null) {
            selectedCandidate = candidates.stream()
                .filter(candidate -> INVENTORY_CHANGED_TRIGGER.equals(candidate.trigger()))
                .findFirst()
                .orElse(candidates.getFirst());
        }

        return new CaseDefinition(
            inventoryEntry.advancementId(),
            inventoryEntry.sourcePath(),
            selectedCandidate.criterion(),
            selectedCandidate.trigger(),
            selectedCandidate.itemTag(),
            selectedCandidate.requiredCountMin(),
            selectedCandidate.requiredCountMax(),
            selectedCandidate.conditionKeys(),
            selectedCandidate.automationEligibility(),
            selectedCandidate.eligibilityReason()
        );
    }

    private static CriterionCandidate analyzeCriterion(String criterion, JsonObject criterionJson) {
        String trigger = criterionJson.has("trigger") ? criterionJson.get("trigger").getAsString() : "";
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : null;

        if (!INVENTORY_CHANGED_TRIGGER.equals(trigger)) {
            return new CriterionCandidate(criterion, trigger, null, null, null, List.of(), DEFERRED, "TRIGGER_NOT_INVENTORY_CHANGED");
        }
        if (conditions == null) {
            return new CriterionCandidate(criterion, trigger, null, null, null, List.of(), DEFERRED, "MISSING_CONDITIONS");
        }
        if (!conditionKeys(conditions).equals(List.of("items"))) {
            return new CriterionCandidate(criterion, trigger, null, null, null, List.of(), DEFERRED, "ADDITIONAL_TRIGGER_CONDITIONS");
        }
        if (!conditions.has("items") || !conditions.get("items").isJsonArray()) {
            return new CriterionCandidate(criterion, trigger, null, null, null, List.of(), DEFERRED, "MISSING_ITEMS_ARRAY");
        }

        JsonArray items = conditions.getAsJsonArray("items");
        if (items.size() != 1 || !items.get(0).isJsonObject()) {
            return new CriterionCandidate(criterion, trigger, null, null, null, List.of(), DEFERRED, "MULTIPLE_ITEM_PREDICATE_ENTRIES");
        }

        JsonObject itemPredicate = items.get(0).getAsJsonObject();
        List<String> itemPredicateKeys = conditionKeys(itemPredicate);
        if (!itemPredicate.has("items")) {
            return new CriterionCandidate(criterion, trigger, null, null, null, itemPredicateKeys, DEFERRED, "MISSING_ITEM_SELECTOR");
        }
        if (itemPredicate.has("components")) {
            return new CriterionCandidate(
                criterion,
                trigger,
                extractTag(itemPredicate.get("items")),
                null,
                null,
                itemPredicateKeys,
                DEFERRED,
                "MEANINGFUL_COMPONENT_MATCHER"
            );
        }
        for (String key : itemPredicateKeys) {
            if (!Objects.equals(key, "items") && !Objects.equals(key, "count")) {
                return new CriterionCandidate(
                    criterion,
                    trigger,
                    extractTag(itemPredicate.get("items")),
                    null,
                    null,
                    itemPredicateKeys,
                    DEFERRED,
                    "UNSUPPORTED_ITEM_PREDICATE_KEYS"
                );
            }
        }

        String itemTag = extractTag(itemPredicate.get("items"));
        if (itemTag == null) {
            return new CriterionCandidate(criterion, trigger, null, null, null, itemPredicateKeys, DEFERRED, "ITEM_SELECTOR_IS_NOT_TAG");
        }

        CountBounds countBounds = parseCountBounds(itemPredicate.get("count"));
        if (!countBounds.supported()) {
            return new CriterionCandidate(criterion, trigger, itemTag, null, null, itemPredicateKeys, DEFERRED, "NON_DETERMINISTIC_COUNT");
        }

        return new CriterionCandidate(
            criterion,
            trigger,
            itemTag,
            countBounds.min(),
            countBounds.max(),
            itemPredicateKeys,
            SUPPORTED,
            "EXACT_SINGLE_TAG_ITEM_PREDICATE"
        );
    }

    private static CountBounds parseCountBounds(JsonElement countElement) {
        if (countElement == null || countElement.isJsonNull()) {
            return new CountBounds(null, null, true);
        }
        if (countElement.isJsonPrimitive() && countElement.getAsJsonPrimitive().isNumber()) {
            int exactCount = countElement.getAsInt();
            return new CountBounds(exactCount, exactCount, true);
        }
        if (!countElement.isJsonObject()) {
            return new CountBounds(null, null, false);
        }
        JsonObject countObject = countElement.getAsJsonObject();
        Integer min = countObject.has("min") && countObject.get("min").isJsonPrimitive() && countObject.get("min").getAsJsonPrimitive().isNumber()
            ? countObject.get("min").getAsInt()
            : null;
        Integer max = countObject.has("max") && countObject.get("max").isJsonPrimitive() && countObject.get("max").getAsJsonPrimitive().isNumber()
            ? countObject.get("max").getAsInt()
            : null;
        boolean exact = min != null && min.equals(max);
        return new CountBounds(min, max, exact);
    }

    private static String extractTag(JsonElement itemsElement) {
        if (itemsElement == null || itemsElement.isJsonNull() || !itemsElement.isJsonPrimitive() || !itemsElement.getAsJsonPrimitive().isString()) {
            return null;
        }
        String rawValue = itemsElement.getAsString();
        if (!rawValue.startsWith("#")) {
            return null;
        }
        return rawValue.substring(1);
    }

    private static List<String> conditionKeys(JsonObject json) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(Comparator.comparingInt(PhaseAItemTagInventoryChangedCertification::conditionKeyRank).thenComparing(String::toString));
        return List.copyOf(keys);
    }

    private static int conditionKeyRank(String key) {
        return switch (key) {
            case "items" -> 0;
            case "count" -> 1;
            case "components" -> 2;
            default -> 10;
        };
    }

    private static List<InventoryEntry> loadCanonicalInventory(Path projectRoot) throws IOException {
        JsonObject root = JsonParser.parseString(PhaseACertification.generate(projectRoot).inventoryJson()).getAsJsonObject();
        JsonArray entries = root.getAsJsonArray("entries");
        List<InventoryEntry> inventory = new ArrayList<>();
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            inventory.add(new InventoryEntry(
                entry.get("id").getAsString(),
                entry.get("sourcePath").getAsString()
            ));
        }
        inventory.sort(Comparator.comparing(InventoryEntry::advancementId));
        return List.copyOf(inventory);
    }

    private static Map<String, JsonObject> loadFrozenAdvancements(Path bacapZip) throws IOException {
        Map<String, JsonObject> advancements = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(bacapZip.toFile(), StandardCharsets.UTF_8)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith("data/") || !entryName.contains("/advancement/") || !entryName.endsWith(".json")) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                    advancements.put(entryName, JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        }
        return Map.copyOf(advancements);
    }

    private record InventoryEntry(String advancementId, String sourcePath) {
    }

    private record CriterionCandidate(
        String criterion,
        String trigger,
        String itemTag,
        Integer requiredCountMin,
        Integer requiredCountMax,
        List<String> conditionKeys,
        String automationEligibility,
        String eligibilityReason
    ) {
    }

    private record CountBounds(Integer min, Integer max, boolean supported) {
    }

    private record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String itemTag,
        Integer requiredCountMin,
        Integer requiredCountMax,
        List<String> conditionKeys,
        String automationEligibility,
        String eligibilityReason
    ) {
    }
}
