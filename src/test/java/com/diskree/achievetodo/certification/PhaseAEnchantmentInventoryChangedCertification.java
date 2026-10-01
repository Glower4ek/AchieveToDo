package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAEnchantmentInventoryChangedCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_case_catalog.json");

    private static final String SNAPSHOT_ID = "phase_a_enchantment_inventory_changed_case_catalog";
    private static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String DEFERRED = "DEFERRED";
    private static final String INVENTORY_CHANGED_TRIGGER = "minecraft:inventory_changed";
    private static final String ENCHANTMENTS = "ENCHANTMENTS";
    private static final String STORED_ENCHANTMENTS = "STORED_ENCHANTMENTS";
    private static final Set<String> EXPECTED_SUPPORTED_KEYS = Set.of(
        "blazeandcave:enchanting/a_rather_pointy_fence_post#rather_pointy_fence_post",
        "blazeandcave:enchanting/boomerang#power",
        "blazeandcave:enchanting/like_a_cat#feather_falling",
        "blazeandcave:enchanting/like_a_ninja#swift_sneak_book",
        "blazeandcave:enchanting/mace_windu#mace_windu",
        "blazeandcave:enchanting/master_fisher#perfect_rod",
        "blazeandcave:enchanting/newtons_flaming_laser_sword#newton",
        "blazeandcave:enchanting/to_infinity_and_beyond#infinity",
        "blazeandcave:enchanting/unbreakable#mending",
        "blazeandcave:enchanting/zeus#power",
        "blazeandcave:mining/the_mistake#the_mistake"
    );
    private static final String EXPECTED_DEFERRED_KEY = "blazeandcave:enchanting/god_of_thunder#mjolnir";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final HolderLookup.Provider REGISTRY_LOOKUP;

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        REGISTRY_LOOKUP = VanillaRegistries.createLookup();
    }

    private PhaseAEnchantmentInventoryChangedCertification() {
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
        validateAcceptedFrontier(cases);
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
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
            if (caseDefinition.baseItemConstraint() == null) {
                caseJson.add("baseItemConstraint", null);
            } else {
                caseJson.addProperty("baseItemConstraint", caseDefinition.baseItemConstraint());
            }
            JsonArray allowedItems = new JsonArray();
            for (String allowedItem : caseDefinition.allowedItems()) {
                allowedItems.add(allowedItem);
            }
            caseJson.add("allowedItems", allowedItems);
            JsonArray itemPredicateKeys = new JsonArray();
            for (String itemPredicateKey : caseDefinition.itemPredicateKeys()) {
                itemPredicateKeys.add(itemPredicateKey);
            }
            caseJson.add("itemPredicateKeys", itemPredicateKeys);
            JsonArray componentKeys = new JsonArray();
            for (String componentKey : caseDefinition.componentKeys()) {
                componentKeys.add(componentKey);
            }
            caseJson.add("componentKeys", componentKeys);
            JsonArray enchantmentsJson = new JsonArray();
            for (EnchantmentPredicate predicate : caseDefinition.enchantmentPredicates()) {
                JsonObject predicateJson = new JsonObject();
                predicateJson.addProperty("selector", predicate.selector());
                predicateJson.addProperty("storageType", predicate.storageType());
                if (predicate.minLevel() == null) {
                    predicateJson.add("minLevel", null);
                } else {
                    predicateJson.addProperty("minLevel", predicate.minLevel());
                }
                if (predicate.maxLevel() == null) {
                    predicateJson.add("maxLevel", null);
                } else {
                    predicateJson.addProperty("maxLevel", predicate.maxLevel());
                }
                enchantmentsJson.add(predicateJson);
            }
            caseJson.add("enchantmentPredicates", enchantmentsJson);
            casesJson.add(caseJson);
        }
        root.add("cases", casesJson);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private static List<CaseDefinition> loadCases(Path projectRoot) throws IOException {
        Set<String> stableAdvancementIds = stableEnchantmentHolderSetAdvancementIds(projectRoot);
        Set<String> acceptedFrontierAdvancementIds = acceptedFrontierAdvancementIds();
        if (!stableAdvancementIds.containsAll(acceptedFrontierAdvancementIds)) {
            Set<String> missing = new java.util.TreeSet<>(acceptedFrontierAdvancementIds);
            missing.removeAll(stableAdvancementIds);
            throw new IllegalStateException("Accepted enchantment frontier fell outside current ENCHANTMENT_HOLDERSET family: " + missing);
        }
        List<InventoryEntry> inventoryEntries = loadCanonicalInventory(projectRoot.resolve(PhaseACertification.INVENTORY_SNAPSHOT));
        if (inventoryEntries.size() != PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS) {
            throw new IllegalStateException(
                "Expected canonical advancement inventory size " + PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS + " but found " + inventoryEntries.size()
            );
        }

        Map<String, JsonObject> rawAdvancements = loadFrozenAdvancements(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE), inventoryEntries);
        List<CaseDefinition> cases = new ArrayList<>();
        for (InventoryEntry inventoryEntry : inventoryEntries) {
            if (!acceptedFrontierAdvancementIds.contains(inventoryEntry.advancementId())) {
                continue;
            }
            JsonObject advancementJson = rawAdvancements.get(inventoryEntry.sourcePath());
            if (advancementJson == null) {
                throw new IllegalStateException("Missing frozen advancement JSON for " + inventoryEntry.advancementId() + " at " + inventoryEntry.sourcePath());
            }
            JsonObject criteria = advancementJson.getAsJsonObject("criteria");
            if (criteria == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> criterionEntry : criteria.entrySet()) {
                if (!criterionEntry.getValue().isJsonObject()) {
                    continue;
                }
                CaseDefinition caseDefinition = classifyCriterion(inventoryEntry, criterionEntry.getKey(), criterionEntry.getValue().getAsJsonObject());
                if (caseDefinition != null) {
                    cases.add(caseDefinition);
                }
            }
        }
        cases.sort(Comparator.comparing(CaseDefinition::advancementId).thenComparing(CaseDefinition::criterion));
        return List.copyOf(cases);
    }

    static Set<String> stableEnchantmentHolderSetAdvancementIds(Path projectRoot) throws IOException {
        PhaseACertification.bootstrapMinecraft();
        return PhaseACertification.analyzeStaticRules(projectRoot).entries().stream()
            .filter(entry -> entry.status() == PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED)
            .filter(entry -> entry.deferredReason() == PhaseACertification.DeferredReason.REGISTRY_CONTEXT_REQUIRED)
            .filter(entry -> entry.registryComponent() == PhaseACertification.RegistryDependentComponent.ENCHANTMENT_HOLDERSET)
            .map(PhaseACertification.StaticValidationEntry::id)
            .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    static Set<String> acceptedFrontierAdvancementIds() {
        Set<String> advancementIds = new java.util.TreeSet<>();
        for (String key : EXPECTED_SUPPORTED_KEYS) {
            advancementIds.add(advancementIdFromKey(key));
        }
        advancementIds.add(advancementIdFromKey(EXPECTED_DEFERRED_KEY));
        return Set.copyOf(advancementIds);
    }

    private static String advancementIdFromKey(String key) {
        int separatorIndex = key.indexOf('#');
        if (separatorIndex <= 0 || separatorIndex == key.length() - 1) {
            throw new IllegalStateException("Accepted frontier key must contain advancementId#criterion: " + key);
        }
        return key.substring(0, separatorIndex);
    }

    private static CaseDefinition classifyCriterion(InventoryEntry inventoryEntry, String criterion, JsonObject criterionJson) {
        String trigger = criterionJson.has("trigger") ? criterionJson.get("trigger").getAsString() : "";
        if (!INVENTORY_CHANGED_TRIGGER.equals(trigger)) {
            return null;
        }
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : null;
        if (conditions == null || !conditionKeys(conditions).equals(List.of("items"))) {
            return null;
        }
        JsonArray items = conditions.getAsJsonArray("items");
        if (items == null || items.size() != 1 || !items.get(0).isJsonObject()) {
            return null;
        }
        JsonObject itemPredicate = items.get(0).getAsJsonObject();
        List<String> itemPredicateKeys = conditionKeys(itemPredicate);
        JsonObject predicates = itemPredicate.has("predicates") && itemPredicate.get("predicates").isJsonObject()
            ? itemPredicate.getAsJsonObject("predicates")
            : null;
        List<EnchantmentPredicate> enchantmentPredicates = predicates == null ? List.of() : parseEnchantmentPredicates(predicates);
        if (enchantmentPredicates.isEmpty()) {
            return null;
        }
        List<String> componentKeys = componentKeys(itemPredicate);
        String baseItemConstraint = itemPredicate.has("items") && itemPredicate.get("items").isJsonPrimitive()
            ? itemPredicate.get("items").getAsString()
            : null;
        List<String> allowedItems = parseAllowedItems(itemPredicate.get("items"), enchantmentPredicates);
        boolean allowedItemResolvable = isResolvableAllowedItemSet(allowedItems);
        boolean selectorResolvable = enchantmentPredicates.stream().allMatch(PhaseAEnchantmentInventoryChangedCertification::enchantmentSelectorResolves);
        boolean hasMeaningfulComponentMatcher = !componentKeys.isEmpty();
        boolean onlySupportedKeys = itemPredicateKeys.stream().allMatch(key -> Objects.equals(key, "items") || Objects.equals(key, "predicates") || Objects.equals(key, "components"));
        String automationEligibility = SUPPORTED;
        String deferReason = "SIMPLE_ENCHANTMENT_HOLDERSET";
        if (!onlySupportedKeys) {
            automationEligibility = DEFERRED;
            deferReason = "UNSUPPORTED_ITEM_PREDICATE_KEYS";
        } else if (hasMeaningfulComponentMatcher) {
            automationEligibility = DEFERRED;
            deferReason = "MEANINGFUL_COMPONENT_MATCHER";
        } else if (!allowedItemResolvable) {
            automationEligibility = DEFERRED;
            deferReason = "UNRESOLVABLE_ALLOWED_ITEMS";
        } else if (!selectorResolvable) {
            automationEligibility = DEFERRED;
            deferReason = "UNRESOLVABLE_ENCHANTMENT_SELECTOR";
        }
        return new CaseDefinition(
            inventoryEntry.advancementId(),
            inventoryEntry.sourcePath(),
            criterion,
            trigger,
            automationEligibility,
            deferReason,
            baseItemConstraint,
            List.copyOf(allowedItems),
            List.copyOf(itemPredicateKeys),
            List.copyOf(componentKeys),
            List.copyOf(enchantmentPredicates)
        );
    }

    private static void validateAcceptedFrontier(List<CaseDefinition> cases) {
        Set<String> supportedKeys = new LinkedHashSet<>();
        Set<String> deferredKeys = new LinkedHashSet<>();
        for (CaseDefinition caseDefinition : cases) {
            String key = caseDefinition.advancementId() + "#" + caseDefinition.criterion();
            if (SUPPORTED.equals(caseDefinition.automationEligibility())) {
                supportedKeys.add(key);
            } else if (DEFERRED.equals(caseDefinition.automationEligibility())) {
                deferredKeys.add(key);
            }
        }
        if (!supportedKeys.equals(EXPECTED_SUPPORTED_KEYS)) {
            throw new IllegalStateException("Derived SUPPORTED set drifted from accepted enchantment frontier: " + supportedKeys);
        }
        if (!deferredKeys.equals(Set.of(EXPECTED_DEFERRED_KEY))) {
            throw new IllegalStateException("Derived DEFERRED set drifted from accepted enchantment frontier: " + deferredKeys);
        }
    }

    private static boolean isResolvableAllowedItemSet(List<String> allowedItems) {
        if (allowedItems.isEmpty()) {
            return true;
        }
        for (String allowedItem : allowedItems) {
            if (allowedItem.startsWith("#")) {
                return false;
            }
            if (REGISTRY_LOOKUP.lookupOrThrow(Registries.ITEM)
                .get(net.minecraft.resources.ResourceKey.create(Registries.ITEM, Identifier.parse(allowedItem)))
                .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean enchantmentSelectorResolves(EnchantmentPredicate predicate) {
        return REGISTRY_LOOKUP.lookupOrThrow(Registries.ENCHANTMENT)
            .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(predicate.selector())))
            .isPresent();
    }

    private static List<EnchantmentPredicate> parseEnchantmentPredicates(JsonObject predicates) {
        List<EnchantmentPredicate> result = new ArrayList<>();
        for (StorageType storageType : StorageType.values()) {
            if (!predicates.has(storageType.jsonKey()) || !predicates.get(storageType.jsonKey()).isJsonArray()) {
                continue;
            }
            JsonArray entries = predicates.getAsJsonArray(storageType.jsonKey());
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                JsonElement enchantmentsElement = entry.get("enchantments");
                String selector = enchantmentsElement != null && enchantmentsElement.isJsonPrimitive()
                    ? enchantmentsElement.getAsString()
                    : null;
                if (selector == null || selector.isBlank()) {
                    continue;
                }
                LevelBounds bounds = parseLevelBounds(entry.get("levels"));
                result.add(new EnchantmentPredicate(selector, storageType.serializedName(), bounds.min(), bounds.max()));
            }
        }
        result.sort(Comparator.comparing(EnchantmentPredicate::storageType).thenComparing(EnchantmentPredicate::selector));
        return List.copyOf(result);
    }

    private static LevelBounds parseLevelBounds(JsonElement levelsElement) {
        if (levelsElement == null || levelsElement.isJsonNull()) {
            return new LevelBounds(1, null);
        }
        if (levelsElement.isJsonPrimitive() && levelsElement.getAsJsonPrimitive().isNumber()) {
            int exact = levelsElement.getAsInt();
            return new LevelBounds(exact, exact);
        }
        if (!levelsElement.isJsonObject()) {
            return new LevelBounds(1, null);
        }
        JsonObject object = levelsElement.getAsJsonObject();
        Integer min = object.has("min") && object.get("min").isJsonPrimitive() ? object.get("min").getAsInt() : 1;
        Integer max = object.has("max") && object.get("max").isJsonPrimitive() ? object.get("max").getAsInt() : null;
        return new LevelBounds(min, max);
    }

    private static List<String> parseAllowedItems(JsonElement itemsElement, List<EnchantmentPredicate> enchantmentPredicates) {
        if (itemsElement == null || itemsElement.isJsonNull()) {
            boolean storedOnly = enchantmentPredicates.stream().allMatch(predicate -> STORED_ENCHANTMENTS.equals(predicate.storageType()));
            return storedOnly ? List.of("minecraft:enchanted_book") : List.of();
        }
        if (itemsElement.isJsonPrimitive() && itemsElement.getAsJsonPrimitive().isString()) {
            return List.of(itemsElement.getAsString());
        }
        if (itemsElement.isJsonArray()) {
            List<String> values = new ArrayList<>();
            for (JsonElement element : itemsElement.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    values.add(element.getAsString());
                }
            }
            values.sort(String::compareTo);
            return List.copyOf(values);
        }
        return List.of();
    }

    private static List<String> componentKeys(JsonObject itemPredicate) {
        List<String> keys = new ArrayList<>();
        JsonObject components = itemPredicate.has("components") && itemPredicate.get("components").isJsonObject()
            ? itemPredicate.getAsJsonObject("components")
            : null;
        if (components == null) {
            return List.of();
        }
        for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(String::compareTo);
        return List.copyOf(keys);
    }

    private static List<String> conditionKeys(JsonObject json) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(Comparator.comparingInt(PhaseAEnchantmentInventoryChangedCertification::conditionKeyRank).thenComparing(String::toString));
        return List.copyOf(keys);
    }

    private static int conditionKeyRank(String key) {
        return switch (key) {
            case "items" -> 0;
            case "predicates" -> 1;
            case "components" -> 2;
            default -> 10;
        };
    }

    private static List<InventoryEntry> loadCanonicalInventory(Path inventoryPath) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(inventoryPath, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray entries = root.getAsJsonArray("entries");
        List<InventoryEntry> inventory = new ArrayList<>();
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            inventory.add(new InventoryEntry(
                entry.get("id").getAsString(),
                entry.get("sourcePath").getAsString()
            ));
        }
        return List.copyOf(inventory);
    }

    private static Map<String, JsonObject> loadFrozenAdvancements(Path zipPath, List<InventoryEntry> inventoryEntries) throws IOException {
        Map<String, JsonObject> advancements = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            for (InventoryEntry entry : inventoryEntries) {
                ZipEntry zipEntry = zipFile.getEntry(entry.sourcePath());
                if (zipEntry == null) {
                    continue;
                }
                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    advancements.put(entry.sourcePath(), readJson(inputStream));
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

    private record InventoryEntry(String advancementId, String sourcePath) {
    }

    private record LevelBounds(Integer min, Integer max) {
    }

    private enum StorageType {
        NORMAL("enchantments", ENCHANTMENTS),
        STORED("stored_enchantments", STORED_ENCHANTMENTS);

        private final String jsonKey;
        private final String serializedName;

        StorageType(String jsonKey, String serializedName) {
            this.jsonKey = jsonKey;
            this.serializedName = serializedName;
        }

        String jsonKey() {
            return jsonKey;
        }

        String serializedName() {
            return serializedName;
        }
    }

    record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String automationEligibility,
        String deferReason,
        String baseItemConstraint,
        List<String> allowedItems,
        List<String> itemPredicateKeys,
        List<String> componentKeys,
        List<EnchantmentPredicate> enchantmentPredicates
    ) {
    }

    record EnchantmentPredicate(String selector, String storageType, Integer minLevel, Integer maxLevel) {
    }
}
