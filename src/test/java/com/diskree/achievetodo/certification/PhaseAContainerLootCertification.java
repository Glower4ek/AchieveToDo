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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAContainerLootCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_case_catalog.json");

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private static final Path FROZEN_BACAP = Path.of("reference", "phase_a_preservation", "files", "final", "bacap.zip");
    private static final String SNAPSHOT_ID = "phase_a_container_loot_case_catalog";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final String AUTOMATION_DEFERRED = "DEFERRED";
    private static final String TARGET_TRIGGER = "minecraft:player_generates_container_loot";

    private PhaseAContainerLootCertification() {
    }

    public static void main(String[] args) throws IOException {
        Path projectRoot = args.length > 0
            ? Path.of(args[0]).toAbsolutePath().normalize()
            : Path.of("").toAbsolutePath().normalize();
        writeSnapshot(projectRoot);
    }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        Map<String, InventorySource> canonicalInventory = loadCanonicalInventory(projectRoot.resolve(PhaseACertification.INVENTORY_SNAPSHOT));
        Map<String, JsonObject> frozenAdvancements = loadAdvancements(projectRoot.resolve(FROZEN_BACAP), canonicalInventory.values());

        List<ContainerLootCase> cases = new ArrayList<>();
        for (InventorySource inventorySource : canonicalInventory.values()) {
            JsonObject advancement = frozenAdvancements.get(inventorySource.id());
            if (advancement == null || !advancement.has("criteria")) {
                continue;
            }
            JsonObject criteria = advancement.getAsJsonObject("criteria");
            for (Map.Entry<String, JsonElement> criterionEntry : criteria.entrySet()) {
                if (!criterionEntry.getValue().isJsonObject()) {
                    continue;
                }
                ContainerLootCase lootCase = classifyCase(inventorySource, criterionEntry.getKey(), criterionEntry.getValue().getAsJsonObject());
                if (lootCase != null) {
                    cases.add(lootCase);
                }
            }
        }
        cases.sort(Comparator
            .comparing(ContainerLootCase::advancementId)
            .thenComparing(ContainerLootCase::criterion)
            .thenComparing(ContainerLootCase::lootTable, Comparator.nullsLast(String::compareTo)));

        Summary summary = summarize(cases);
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", FROZEN_BACAP.toString().replace('\\', '/'));
        root.add("summary", summary.toJson());

        JsonArray caseArray = new JsonArray();
        for (ContainerLootCase lootCase : cases) {
            caseArray.add(lootCase.toJson());
        }
        root.add("cases", caseArray);
        return GSON.toJson(root) + System.lineSeparator();
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve(SNAPSHOT), generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    private static ContainerLootCase classifyCase(InventorySource inventorySource, String criterion, JsonObject criterionJson) {
        String trigger = criterionJson.has("trigger") ? criterionJson.get("trigger").getAsString() : "";
        if (!TARGET_TRIGGER.equals(trigger)) {
            return null;
        }
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : new JsonObject();
        Set<String> conditionKeys = new LinkedHashSet<>(conditions.keySet());
        String lootTable = conditions.has("loot_table") && conditions.get("loot_table").isJsonPrimitive()
            ? conditions.get("loot_table").getAsString()
            : null;
        boolean semanticallySimple = lootTable != null && conditionKeys.size() == 1 && conditionKeys.contains("loot_table");
        String automationEligibility = semanticallySimple ? AUTOMATION_SUPPORTED : AUTOMATION_DEFERRED;
        String eligibilityReason = semanticallySimple
            ? "ONLY_LOOT_TABLE_CONDITION"
            : "EXTRA_OR_NON_SIMPLE_CONDITIONS: " + conditionKeys;
        return new ContainerLootCase(
            inventorySource.id(),
            inventorySource.sourcePath(),
            criterion,
            trigger,
            lootTable,
            List.copyOf(conditionKeys),
            automationEligibility,
            eligibilityReason
        );
    }

    private static Summary summarize(List<ContainerLootCase> cases) {
        Map<String, Integer> byEligibility = new TreeMap<>();
        Set<String> uniqueAdvancements = new LinkedHashSet<>();
        for (ContainerLootCase lootCase : cases) {
            byEligibility.merge(lootCase.automationEligibility(), 1, Integer::sum);
            uniqueAdvancements.add(lootCase.advancementId());
        }
        return new Summary(
            cases.size(),
            uniqueAdvancements.size(),
            byEligibility.getOrDefault(AUTOMATION_SUPPORTED, 0),
            byEligibility.getOrDefault(AUTOMATION_DEFERRED, 0),
            byEligibility
        );
    }

    private static Map<String, InventorySource> loadCanonicalInventory(Path inventoryPath) throws IOException {
        JsonObject inventory = JsonParser.parseString(Files.readString(inventoryPath, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, InventorySource> entries = new LinkedHashMap<>();
        for (JsonElement entry : inventory.getAsJsonArray("entries")) {
            JsonObject object = entry.getAsJsonObject();
            entries.put(
                object.get("id").getAsString(),
                new InventorySource(
                    object.get("id").getAsString(),
                    object.get("sourcePath").getAsString()
                )
            );
        }
        return entries;
    }

    private static Map<String, JsonObject> loadAdvancements(Path zipPath, Iterable<InventorySource> inventory) throws IOException {
        Map<String, JsonObject> advancements = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            for (InventorySource entry : inventory) {
                ZipEntry zipEntry = zipFile.getEntry(entry.sourcePath());
                if (zipEntry == null) {
                    continue;
                }
                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    advancements.put(entry.id(), readJson(inputStream));
                }
            }
        }
        return advancements;
    }

    private static JsonObject readJson(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private record InventorySource(String id, String sourcePath) {
    }

    private record Summary(
        int totalCases,
        int uniqueAdvancements,
        int automationSupported,
        int automationDeferred,
        Map<String, Integer> byEligibility
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("totalCases", totalCases);
            json.addProperty("uniqueAdvancements", uniqueAdvancements);
            json.addProperty("automationSupported", automationSupported);
            json.addProperty("automationDeferred", automationDeferred);
            JsonObject byEligibilityJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : byEligibility.entrySet()) {
                byEligibilityJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("byEligibility", byEligibilityJson);
            return json;
        }
    }

    private record ContainerLootCase(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String lootTable,
        List<String> conditionKeys,
        String automationEligibility,
        String eligibilityReason
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("criterion", criterion);
            json.addProperty("trigger", trigger);
            if (lootTable == null) {
                json.add("lootTable", null);
            } else {
                json.addProperty("lootTable", lootTable);
            }
            JsonArray conditionKeyArray = new JsonArray();
            for (String conditionKey : conditionKeys) {
                conditionKeyArray.add(conditionKey);
            }
            json.add("conditionKeys", conditionKeyArray);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("eligibilityReason", eligibilityReason);
            return json;
        }
    }
}
