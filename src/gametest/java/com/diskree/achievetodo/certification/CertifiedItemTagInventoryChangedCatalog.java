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
import java.util.Comparator;
import java.util.List;

public final class CertifiedItemTagInventoryChangedCatalog {
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_changed_case_catalog.json");
    private static final String SNAPSHOT = "phase_a_item_tag_inventory_changed_case_catalog";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedItemTagInventoryChangedCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedItemTagInventoryChangedCatalog.class) {
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
        throw new IllegalArgumentException("Missing certified ITEM_TAG_INVENTORY_CHANGED case for " + advancementId + "#" + criterion);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path catalogPath = PhaseAItemTagInventoryChangedExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(catalogPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SNAPSHOT.equals(root.get("snapshot").getAsString())) {
                throw new IllegalStateException("Unexpected item-tag inventory_changed catalog snapshot: " + root.get("snapshot").getAsString());
            }
            JsonArray casesJson = root.getAsJsonArray("cases");
            List<CaseDefinition> cases = new ArrayList<>();
            if (casesJson == null) {
                return cases;
            }
            for (JsonElement caseElement : casesJson) {
                JsonObject caseJson = caseElement.getAsJsonObject();
                if (!AUTOMATION_SUPPORTED.equals(caseJson.get("automationEligibility").getAsString())) {
                    continue;
                }
                Integer requiredCountMin = caseJson.has("requiredCountMin") && !caseJson.get("requiredCountMin").isJsonNull()
                    ? caseJson.get("requiredCountMin").getAsInt()
                    : null;
                Integer requiredCountMax = caseJson.has("requiredCountMax") && !caseJson.get("requiredCountMax").isJsonNull()
                    ? caseJson.get("requiredCountMax").getAsInt()
                    : null;
                List<String> conditionKeys = new ArrayList<>();
                JsonArray conditionKeysJson = caseJson.getAsJsonArray("conditionKeys");
                if (conditionKeysJson != null) {
                    for (JsonElement conditionKeyElement : conditionKeysJson) {
                        conditionKeys.add(conditionKeyElement.getAsString());
                    }
                }
                cases.add(new CaseDefinition(
                    Identifier.parse(caseJson.get("advancementId").getAsString()),
                    caseJson.get("sourcePath").getAsString(),
                    caseJson.get("criterion").getAsString(),
                    caseJson.get("trigger").getAsString(),
                    caseJson.get("itemTag").getAsString(),
                    requiredCountMin,
                    requiredCountMax,
                    List.copyOf(conditionKeys),
                    caseJson.get("automationEligibility").getAsString()
                ));
            }
            cases.sort(Comparator.comparing((CaseDefinition value) -> value.advancementId().toString()).thenComparing(CaseDefinition::criterion));
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load item-tag inventory_changed catalog", e);
        }
    }

    public record CaseDefinition(
        Identifier advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String itemTag,
        Integer requiredCountMin,
        Integer requiredCountMax,
        List<String> conditionKeys,
        String automationEligibility
    ) {
        public int requiredAcquisitionCount() {
            if (requiredCountMin != null && requiredCountMax != null && requiredCountMin.equals(requiredCountMax)) {
                return requiredCountMin;
            }
            return 1;
        }
    }
}
