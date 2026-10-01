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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAItemTagPlacedBlockCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_placed_block_case_catalog.json");

    private static final String SNAPSHOT_ID = "phase_a_item_tag_placed_block_case_catalog";
    private static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String DEFERRED = "DEFERRED";
    private static final String PLACED_BLOCK_TRIGGER = "minecraft:placed_block";
    private static final String SIMPLE_REASON = "PLACED_BLOCK_MATCH_TOOL_ITEM_TAG";
    private static final Set<String> EXPECTED_SUPPORTED_KEYS = Set.of(
        "blazeandcave:building/en_garde#fence",
        "blazeandcave:building/hanging_around#hanging_sign",
        "blazeandcave:building/its_a_sign#sign",
        "blazeandcave:building/its_a_trap#trapdoor",
        "blazeandcave:building/raise_the_flag#banner"
    );
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagPlacedBlockCertification() {
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
            caseJson.addProperty("automationEligibility", caseDefinition.automationEligibility());
            caseJson.addProperty("deferReason", caseDefinition.deferReason());
            caseJson.addProperty("itemTag", caseDefinition.itemTag());
            casesJson.add(caseJson);
        }
        root.add("cases", casesJson);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private static List<CaseDefinition> loadCases(Path projectRoot) throws IOException {
        List<InventoryEntry> canonicalInventory = loadCanonicalInventory(projectRoot);
        if (canonicalInventory.size() != PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS) {
            throw new IllegalStateException(
                "Expected canonical advancement inventory size " + PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS + " but found " + canonicalInventory.size()
            );
        }
        Map<String, InventoryEntry> inventoryById = new LinkedHashMap<>();
        for (InventoryEntry inventoryEntry : canonicalInventory) {
            inventoryById.put(inventoryEntry.advancementId(), inventoryEntry);
        }

        PhaseACertification.StaticCertificationReport staticReport = PhaseACertification.analyzeStaticRules(projectRoot);
        Map<String, PhaseACertification.StaticValidationEntry> staticEntriesById = new LinkedHashMap<>();
        for (PhaseACertification.StaticValidationEntry entry : staticReport.entries()) {
            staticEntriesById.put(entry.id(), entry);
        }

        Set<String> closedEvidenceKeys = loadClosedEvidenceKeys(projectRoot);
        Map<String, JsonObject> rawAdvancements = loadFrozenAdvancements(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
        List<CaseDefinition> cases = new ArrayList<>();
        for (PhaseACertification.StaticValidationEntry staticEntry : staticReport.entries()) {
            if (staticEntry.status() != PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED
                || staticEntry.deferredReason() != PhaseACertification.DeferredReason.TAG_CONTEXT_REQUIRED
                || staticEntry.registryComponent() != PhaseACertification.RegistryDependentComponent.ITEM_TAG) {
                continue;
            }
            InventoryEntry inventoryEntry = inventoryById.get(staticEntry.id());
            if (inventoryEntry == null) {
                throw new IllegalStateException("Missing canonical inventory entry for " + staticEntry.id());
            }
            JsonObject advancementJson = rawAdvancements.get(inventoryEntry.sourcePath());
            if (advancementJson == null) {
                throw new IllegalStateException("Missing frozen advancement JSON for " + staticEntry.id() + " at " + inventoryEntry.sourcePath());
            }
            cases.addAll(selectSimplePlacedBlockCases(inventoryEntry, staticEntry, advancementJson, closedEvidenceKeys));
        }

        cases.sort(Comparator.comparing(CaseDefinition::advancementId).thenComparing(CaseDefinition::criterion));
        assertExpectedFrontier(cases);
        return List.copyOf(cases);
    }

    private static List<CaseDefinition> selectSimplePlacedBlockCases(
        InventoryEntry inventoryEntry,
        PhaseACertification.StaticValidationEntry staticEntry,
        JsonObject advancementJson,
        Set<String> closedEvidenceKeys
    ) {
        JsonObject criteria = advancementJson.getAsJsonObject("criteria");
        if (criteria == null || criteria.entrySet().isEmpty()) {
            return List.of();
        }
        List<CaseDefinition> cases = new ArrayList<>();
        for (Map.Entry<String, JsonElement> criterionEntry : criteria.entrySet()) {
            String criterion = criterionEntry.getKey();
            JsonObject criterionJson = criterionEntry.getValue().getAsJsonObject();
            String trigger = criterionJson.has("trigger") ? criterionJson.get("trigger").getAsString() : "";
            if (!PLACED_BLOCK_TRIGGER.equals(trigger)) {
                continue;
            }
            String key = inventoryEntry.advancementId() + "#" + criterion;
            if (closedEvidenceKeys.contains(key)) {
                continue;
            }
            String itemTag = extractSimplePlacedBlockItemTag(key, criterionJson);
            if (itemTag == null) {
                continue;
            }
            cases.add(new CaseDefinition(
                inventoryEntry.advancementId(),
                inventoryEntry.sourcePath(),
                criterion,
                trigger,
                SUPPORTED,
                staticEntry.deferredReason().name(),
                itemTag
            ));
        }
        return cases;
    }

    private static String extractSimplePlacedBlockItemTag(String key, JsonObject criterionJson) {
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : null;
        if (conditions == null || !conditionKeys(conditions).equals(List.of("location"))) {
            return null;
        }
        JsonArray location = conditions.getAsJsonArray("location");
        if (location == null || location.size() != 1 || !location.get(0).isJsonObject()) {
            return null;
        }
        JsonObject locationPredicate = location.get(0).getAsJsonObject();
        if (!conditionKeys(locationPredicate).equals(List.of("condition", "predicate"))) {
            return null;
        }
        if (!locationPredicate.has("condition") || !Objects.equals("minecraft:match_tool", locationPredicate.get("condition").getAsString())) {
            return null;
        }
        if (!locationPredicate.has("predicate") || !locationPredicate.get("predicate").isJsonObject()) {
            return null;
        }
        JsonObject predicate = locationPredicate.getAsJsonObject("predicate");
        if (!conditionKeys(predicate).equals(List.of("items"))) {
            return null;
        }
        String itemTag = extractTag(predicate.get("items"));
        if (itemTag == null || itemTag.isBlank()) {
            throw new IllegalStateException("Expected item tag selector for " + key);
        }
        return itemTag;
    }

    private static void assertExpectedFrontier(List<CaseDefinition> cases) {
        Set<String> actualKeys = new TreeSet<>();
        for (CaseDefinition caseDefinition : cases) {
            actualKeys.add(caseDefinition.advancementId() + "#" + caseDefinition.criterion());
            if (!SUPPORTED.equals(caseDefinition.automationEligibility())) {
                throw new IllegalStateException("Derived non-supported placed_block frontier case: " + caseDefinition.advancementId() + "#" + caseDefinition.criterion());
            }
        }
        Set<String> missing = new TreeSet<>(EXPECTED_SUPPORTED_KEYS);
        missing.removeAll(actualKeys);
        Set<String> extras = new TreeSet<>(actualKeys);
        extras.removeAll(EXPECTED_SUPPORTED_KEYS);
        if (!missing.isEmpty() || !extras.isEmpty()) {
            throw new IllegalStateException("Derived ITEM_TAG_PLACED_BLOCK frontier mismatch | missing=" + missing + " | extras=" + extras);
        }
    }

    private static Set<String> loadClosedEvidenceKeys(Path projectRoot) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        accumulateEvidenceKeys(keys, PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        return Set.copyOf(keys);
    }

    private static void accumulateEvidenceKeys(Set<String> keys, Collection<?> runtimeEvidence) {
        for (Object value : runtimeEvidence) {
            if (value instanceof PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData data) {
                addCriteria(keys, data.advancementId(), data.greenCriteria());
            } else if (value instanceof PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData data) {
                addCriteria(keys, data.advancementId(), data.greenCriteria());
            } else if (value instanceof PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData data) {
                addCriteria(keys, data.advancementId(), data.greenCriteria());
            } else if (value instanceof PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData data) {
                addCriteria(keys, data.advancementId(), data.greenCriteria());
            } else {
                throw new IllegalStateException("Unsupported runtime evidence type: " + value.getClass().getName());
            }
        }
    }

    private static void addCriteria(Set<String> keys, String advancementId, Set<String> criteria) {
        for (String criterion : criteria) {
            keys.add(advancementId + "#" + criterion);
        }
    }

    private static List<String> conditionKeys(JsonObject json) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(Comparator.comparingInt(PhaseAItemTagPlacedBlockCertification::conditionKeyRank).thenComparing(String::toString));
        return List.copyOf(keys);
    }

    private static int conditionKeyRank(String key) {
        return switch (key) {
            case "location" -> 0;
            case "condition" -> 0;
            case "predicate" -> 1;
            case "items" -> 0;
            default -> 10;
        };
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

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String automationEligibility,
        String deferReason,
        String itemTag
    ) {
        public String eligibilityReason() {
            return SIMPLE_REASON;
        }
    }
}
