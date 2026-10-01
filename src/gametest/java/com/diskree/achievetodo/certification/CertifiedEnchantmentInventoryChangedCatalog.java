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

public final class CertifiedEnchantmentInventoryChangedCatalog {
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_case_catalog.json");
    private static final String SNAPSHOT = "phase_a_enchantment_inventory_changed_case_catalog";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedEnchantmentInventoryChangedCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedEnchantmentInventoryChangedCatalog.class) {
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
        throw new IllegalArgumentException("Missing certified ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED case for " + advancementId + "#" + criterion);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path catalogPath = PhaseAEnchantmentInventoryChangedExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(catalogPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SNAPSHOT.equals(root.get("snapshot").getAsString())) {
                throw new IllegalStateException("Unexpected enchantment inventory_changed catalog snapshot: " + root.get("snapshot").getAsString());
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
                List<String> allowedItems = new ArrayList<>();
                JsonArray allowedItemsJson = caseJson.getAsJsonArray("allowedItems");
                if (allowedItemsJson != null) {
                    for (JsonElement allowedItem : allowedItemsJson) {
                        allowedItems.add(allowedItem.getAsString());
                    }
                }
                List<EnchantmentRequirement> enchantments = new ArrayList<>();
                JsonArray enchantmentsJson = caseJson.getAsJsonArray("enchantmentPredicates");
                if (enchantmentsJson != null) {
                    for (JsonElement enchantmentElement : enchantmentsJson) {
                        JsonObject enchantmentJson = enchantmentElement.getAsJsonObject();
                        enchantments.add(new EnchantmentRequirement(
                            enchantmentJson.get("selector").getAsString(),
                            enchantmentJson.get("storageType").getAsString(),
                            nullableInt(enchantmentJson, "minLevel"),
                            nullableInt(enchantmentJson, "maxLevel")
                        ));
                    }
                }
                cases.add(new CaseDefinition(
                    Identifier.parse(caseJson.get("advancementId").getAsString()),
                    caseJson.get("sourcePath").getAsString(),
                    caseJson.get("criterion").getAsString(),
                    caseJson.get("trigger").getAsString(),
                    caseJson.get("automationEligibility").getAsString(),
                    caseJson.get("deferReason").getAsString(),
                    nullableString(caseJson, "baseItemConstraint"),
                    List.copyOf(allowedItems),
                    List.copyOf(enchantments)
                ));
            }
            cases.sort(Comparator.comparing((CaseDefinition value) -> value.advancementId().toString()).thenComparing(CaseDefinition::criterion));
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load enchantment inventory_changed catalog", e);
        }
    }

    private static String nullableString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static Integer nullableInt(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsInt();
    }

    public record CaseDefinition(
        Identifier advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String automationEligibility,
        String deferReason,
        String baseItemConstraint,
        List<String> allowedItems,
        List<EnchantmentRequirement> enchantments
    ) {
    }

    public record EnchantmentRequirement(
        String selector,
        String storageType,
        Integer minLevel,
        Integer maxLevel
    ) {
    }
}
