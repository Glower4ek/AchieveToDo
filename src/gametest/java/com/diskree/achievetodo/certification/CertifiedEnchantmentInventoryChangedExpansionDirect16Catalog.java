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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog {
    private static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_expansion_direct16_case_catalog.json"
    );
    private static final String SNAPSHOT = "phase_a_enchantment_inventory_changed_expansion_direct16_case_catalog";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.class) {
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
        throw new IllegalArgumentException("Missing ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16 case for " + advancementId + "#" + criterion);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path catalogPath = PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(catalogPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SNAPSHOT.equals(root.get("snapshot").getAsString())) {
                throw new IllegalStateException("Unexpected expansion catalog snapshot: " + root.get("snapshot").getAsString());
            }
            JsonArray casesJson = root.getAsJsonArray("cases");
            List<CaseDefinition> cases = new ArrayList<>();
            if (casesJson == null) {
                throw new IllegalStateException(
                    "Missing cases array in expansion direct16 enchantment catalog");
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
                List<String> itemPredicateKeys = new ArrayList<>();
                JsonArray itemPredicateKeysJson = caseJson.getAsJsonArray("itemPredicateKeys");
                if (itemPredicateKeysJson != null) {
                    for (JsonElement key : itemPredicateKeysJson) {
                        itemPredicateKeys.add(key.getAsString());
                    }
                }
                List<String> componentKeys = new ArrayList<>();
                JsonArray componentKeysJson = caseJson.getAsJsonArray("componentKeys");
                if (componentKeysJson != null) {
                    for (JsonElement key : componentKeysJson) {
                        componentKeys.add(key.getAsString());
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
                    caseJson.get("selectedItem").getAsString(),
                    List.copyOf(allowedItems),
                    List.copyOf(itemPredicateKeys),
                    List.copyOf(componentKeys),
                    List.copyOf(enchantments)
                ));
            }
            cases.sort(Comparator.comparing((CaseDefinition value) -> value.advancementId().toString()).thenComparing(CaseDefinition::criterion));
            validateSupportedScope(cases);
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load expansion direct16 enchantment catalog", e);
        }
    }

    private static void validateSupportedScope(List<CaseDefinition> cases) {
        if (cases.size() != 25) {
            throw new IllegalStateException(
                "Expected 25 supported expansion direct16 cases but found " + cases.size());
        }
        long uniqueAdvancements = cases.stream()
            .map(caseDefinition -> caseDefinition.advancementId().toString())
            .distinct()
            .count();
        if (uniqueAdvancements != 16) {
            throw new IllegalStateException(
                "Expected 16 supported expansion direct16 advancements but found " + uniqueAdvancements);
        }
        Set<String> seenKeys = new HashSet<>();
        for (CaseDefinition caseDefinition : cases) {
            String key = caseDefinition.advancementId() + "#" + caseDefinition.criterion();
            if (!seenKeys.add(key)) {
                throw new IllegalStateException(
                    "Duplicate supported expansion direct16 advancement/criterion key: " + key);
            }
        }
        long uniqueKeys = cases.stream()
            .map(caseDefinition -> caseDefinition.advancementId() + "#" + caseDefinition.criterion())
            .distinct()
            .count();
        if (uniqueKeys != 25) {
            throw new IllegalStateException(
                "Expected 25 unique supported expansion direct16 advancement/criterion keys but found "
                    + uniqueKeys);
        }
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
        String selectedItem,
        List<String> allowedItems,
        List<String> itemPredicateKeys,
        List<String> componentKeys,
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
