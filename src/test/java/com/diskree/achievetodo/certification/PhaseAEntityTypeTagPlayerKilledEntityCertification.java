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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAEntityTypeTagPlayerKilledEntityCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification", "entity_type_tag_player_killed_entity_case_catalog.json"
    );

    private static final String SNAPSHOT_ID = "phase_a_entity_type_tag_player_killed_entity_case_catalog";
    private static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String PLAYER_KILLED_ENTITY_TRIGGER = "minecraft:player_killed_entity";
    private static final String ELIGIBILITY_REASON = "RUNTIME_TAG_ONLY_ENTITY_PREDICATE";
    private static final Set<String> EXPECTED_SUPPORTED_KEYS = Set.of(
        "blazeandcave:nether/cultural_misunderstandings#piglin",
        "minecraft:adventure/kill_a_mob#monster_hunter"
    );
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAEntityTypeTagPlayerKilledEntityCertification() {
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
        summary.addProperty("automationDeferred", 0);
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
            caseJson.addProperty("staticStatus", caseDefinition.staticStatus());
            JsonArray registryComponents = new JsonArray();
            caseDefinition.registryComponents().forEach(registryComponents::add);
            caseJson.add("registryComponents", registryComponents);
            caseJson.addProperty("entityTypeTag", caseDefinition.entityTypeTag());
            caseJson.addProperty("eligibilityReason", caseDefinition.eligibilityReason());
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
        Set<String> closedEvidenceKeys = loadClosedEvidenceKeys(projectRoot);
        Map<String, JsonObject> rawAdvancements = loadFrozenAdvancements(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
        List<CaseDefinition> cases = new ArrayList<>();
        for (PhaseACertification.StaticValidationEntry staticEntry : staticReport.entries()) {
            if (staticEntry.status() != PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED) {
                continue;
            }
            if (!Set.of(PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG).equals(staticEntry.registryComponents())) {
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
            cases.addAll(selectStrictRuntimeTagOnlyCases(inventoryEntry, staticEntry, advancementJson, closedEvidenceKeys));
        }

        cases.sort(Comparator.comparing(CaseDefinition::advancementId).thenComparing(CaseDefinition::criterion));
        assertExpectedFrontier(cases);
        return List.copyOf(cases);
    }

    private static List<CaseDefinition> selectStrictRuntimeTagOnlyCases(
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
            if (!PLAYER_KILLED_ENTITY_TRIGGER.equals(trigger)) {
                continue;
            }
            String key = inventoryEntry.advancementId() + "#" + criterion;
            if (closedEvidenceKeys.contains(key)) {
                continue;
            }
            String entityTypeTag = extractStrictEntityTypeTagOnly(key, criterionJson);
            if (entityTypeTag == null) {
                continue;
            }
            cases.add(new CaseDefinition(
                inventoryEntry.advancementId(),
                inventoryEntry.sourcePath(),
                criterion,
                trigger,
                SUPPORTED,
                staticEntry.deferredReason() == null ? "" : staticEntry.deferredReason().name(),
                staticEntry.status().name(),
                List.of(PhaseACertification.RegistryDependentComponent.ENTITY_TYPE_TAG.name()),
                entityTypeTag
            ));
        }
        return cases;
    }

    private static String extractStrictEntityTypeTagOnly(String key, JsonObject criterionJson) {
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : null;
        if (conditions == null || !sortedKeys(conditions).equals(List.of("entity"))) {
            return null;
        }
        if (!conditions.get("entity").isJsonObject()) {
            return null;
        }
        JsonObject entity = conditions.getAsJsonObject("entity");
        if (!sortedKeys(entity).equals(List.of("type"))) {
            return null;
        }
        String entityTypeTag = extractTag(entity.get("type"));
        if (entityTypeTag == null || entityTypeTag.isBlank()) {
            throw new IllegalStateException("Expected entity-type tag selector for " + key);
        }
        return entityTypeTag;
    }

    private static void assertExpectedFrontier(List<CaseDefinition> cases) {
        Set<String> actualKeys = new TreeSet<>();
        for (CaseDefinition caseDefinition : cases) {
            String key = caseDefinition.advancementId() + "#" + caseDefinition.criterion();
            actualKeys.add(key);
            if (!SUPPORTED.equals(caseDefinition.automationEligibility())) {
                throw new IllegalStateException("Derived non-supported ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY case: " + key);
            }
        }
        Set<String> missing = new TreeSet<>(EXPECTED_SUPPORTED_KEYS);
        missing.removeAll(actualKeys);
        Set<String> extras = new TreeSet<>(actualKeys);
        extras.removeAll(EXPECTED_SUPPORTED_KEYS);
        if (!missing.isEmpty() || !extras.isEmpty()) {
            throw new IllegalStateException(
                "Derived ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY frontier mismatch | missing=" + missing + " | extras=" + extras
            );
        }
    }

    private static Set<String> loadClosedEvidenceKeys(Path projectRoot) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        accumulateEvidenceKeys(keys, PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
        accumulateEvidenceKeys(keys, PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot).values());
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
            } else if (value instanceof PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData data) {
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

    private static List<String> sortedKeys(JsonObject json) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(String::compareTo);
        return List.copyOf(keys);
    }

    private static String extractTag(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String rawValue = element.getAsString();
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
        String staticStatus,
        List<String> registryComponents,
        String entityTypeTag
    ) {
        public String eligibilityReason() {
            return ELIGIBILITY_REASON;
        }
    }
}
