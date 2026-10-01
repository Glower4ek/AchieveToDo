package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

public final class PhaseAEnchantmentInventoryChangedExpansionDirect16Certification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_expansion_direct16_case_catalog.json"
    );

    private static final String SNAPSHOT_ID = "phase_a_enchantment_inventory_changed_expansion_direct16_case_catalog";
    private static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String CLOSED_CATALOG_SHA_256 = "D14B6727FFEEF6885D774F0239C9F940EE228A451EC9402E3BEF2D1720275C34";
    private static final String CLOSED_CATALOG_PATH = "src/test/resources/phase_a_certification/enchantment_inventory_changed_case_catalog.json";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String INVENTORY_CHANGED_TRIGGER = "minecraft:inventory_changed";
    private static final String ENCHANTMENTS = "ENCHANTMENTS";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().setPrettyPrinting().create();
    private static final HolderLookup.Provider REGISTRY_LOOKUP;
    private static final List<AcceptedCaseSpec> ACCEPTED_CASES = List.of(
        new AcceptedCaseSpec("blazeandcave:enchanting/armor_for_the_masses", "protection", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/armor_for_the_masses", "fire_protection", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/armor_for_the_masses", "blast_protection", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/armor_for_the_masses", "projectile_protection", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/bane_of_one_shotting_spiders", "bane_of_arthropods", "minecraft:diamond_sword"),
        new AcceptedCaseSpec("blazeandcave:enchanting/bow_down_to_me", "power", "minecraft:bow"),
        new AcceptedCaseSpec("blazeandcave:enchanting/curses", "binding_curse", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/curses", "vanishing_curse", "minecraft:diamond_sword"),
        new AcceptedCaseSpec("blazeandcave:enchanting/fiery", "fire_protection", "minecraft:iron_chestplate"),
        new AcceptedCaseSpec("blazeandcave:enchanting/fiery", "fire_aspect", "minecraft:diamond_sword"),
        new AcceptedCaseSpec("blazeandcave:enchanting/fiery", "flame", "minecraft:bow"),
        new AcceptedCaseSpec("blazeandcave:enchanting/fortunate_son", "fortune", "minecraft:diamond_pickaxe"),
        new AcceptedCaseSpec("blazeandcave:enchanting/gotta_go_fast", "soul_speed_boots", "minecraft:iron_boots"),
        new AcceptedCaseSpec("blazeandcave:enchanting/knocking_your_socks_off", "knockback", "minecraft:diamond_sword"),
        new AcceptedCaseSpec("blazeandcave:enchanting/knocking_your_socks_off", "punch", "minecraft:bow"),
        new AcceptedCaseSpec("blazeandcave:enchanting/master_arbalist", "perfect_bow_multishot", "minecraft:crossbow"),
        new AcceptedCaseSpec("blazeandcave:enchanting/master_macerator", "perfect_mace_smite", "minecraft:mace"),
        new AcceptedCaseSpec("blazeandcave:enchanting/master_sniper", "perfect_bow_infinity", "minecraft:bow"),
        new AcceptedCaseSpec("blazeandcave:enchanting/master_tridenteer", "perfect_trident_loyalty", "minecraft:trident"),
        new AcceptedCaseSpec("blazeandcave:enchanting/needle_sharp", "sharpness", "minecraft:diamond_sword"),
        new AcceptedCaseSpec("blazeandcave:enchanting/scuba_gear", "respiration", "minecraft:iron_helmet"),
        new AcceptedCaseSpec("blazeandcave:enchanting/scuba_gear", "aqua_affinity", "minecraft:iron_helmet"),
        new AcceptedCaseSpec("blazeandcave:enchanting/scuba_gear", "depth_strider", "minecraft:iron_boots"),
        new AcceptedCaseSpec("blazeandcave:enchanting/super_efficient", "efficiency", "minecraft:diamond_pickaxe"),
        new AcceptedCaseSpec("blazeandcave:enchanting/undead_slayer", "smite", "minecraft:diamond_sword")
    );

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        REGISTRY_LOOKUP = VanillaRegistries.createLookup();
        // Minecraft 26.2 separates vanilla registry creation from default component binding.
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(REGISTRY_LOOKUP).forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
    }

    private PhaseAEnchantmentInventoryChangedExpansionDirect16Certification() {
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
        validateClosedCatalogSha(projectRoot);
        List<CaseDefinition> cases = loadCases(projectRoot);
        int selectedRequirementGroups = validateCaseSet(cases, projectRoot);

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        root.addProperty("closedCatalogSha256", CLOSED_CATALOG_SHA_256);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("selectedRequirementGroups", selectedRequirementGroups);
        summary.addProperty("storageType", ENCHANTMENTS);
        summary.addProperty("automationSupported", cases.size());
        summary.addProperty("automationDeferred", 0);
        JsonObject byEligibility = new JsonObject();
        byEligibility.addProperty(SUPPORTED, cases.size());
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
            caseJson.addProperty("selectedItem", caseDefinition.selectedItem());
            JsonArray allowedItemsJson = new JsonArray();
            for (String allowedItem : caseDefinition.allowedItems()) {
                allowedItemsJson.add(allowedItem);
            }
            caseJson.add("allowedItems", allowedItemsJson);
            JsonArray itemPredicateKeysJson = new JsonArray();
            for (String key : caseDefinition.itemPredicateKeys()) {
                itemPredicateKeysJson.add(key);
            }
            caseJson.add("itemPredicateKeys", itemPredicateKeysJson);
            JsonArray componentKeysJson = new JsonArray();
            for (String key : caseDefinition.componentKeys()) {
                componentKeysJson.add(key);
            }
            caseJson.add("componentKeys", componentKeysJson);
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

    private static void validateClosedCatalogSha(Path projectRoot) throws IOException {
        String actual = sha256(projectRoot.resolve(CLOSED_CATALOG_PATH));
        if (!CLOSED_CATALOG_SHA_256.equals(actual)) {
            throw new IllegalStateException("Closed enchantment catalog SHA drifted: " + actual);
        }
    }

    private static List<CaseDefinition> loadCases(Path projectRoot) throws IOException {
        Map<String, InventoryEntry> inventoryById = loadCanonicalInventory(projectRoot);
        Map<String, JsonObject> rawAdvancements = loadFrozenAdvancements(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE), inventoryById.values());
        List<CaseDefinition> cases = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (AcceptedCaseSpec spec : ACCEPTED_CASES) {
            InventoryEntry inventoryEntry = inventoryById.get(spec.advancementId());
            if (inventoryEntry == null) {
                throw new IllegalStateException("Missing canonical inventory entry for " + spec.advancementId());
            }
            JsonObject advancementJson = rawAdvancements.get(inventoryEntry.sourcePath());
            if (advancementJson == null) {
                throw new IllegalStateException("Missing frozen advancement JSON for " + spec.advancementId() + " at " + inventoryEntry.sourcePath());
            }
            JsonObject criteria = advancementJson.getAsJsonObject("criteria");
            if (criteria == null || !criteria.has(spec.criterion()) || !criteria.get(spec.criterion()).isJsonObject()) {
                throw new IllegalStateException("Missing frozen criterion " + spec.key());
            }
            if (!seenKeys.add(spec.key())) {
                throw new IllegalStateException("Duplicate accepted case spec " + spec.key());
            }
            cases.add(classifyCriterion(spec, inventoryEntry, advancementJson, criteria.getAsJsonObject(spec.criterion())));
        }
        cases.sort(Comparator.comparing(CaseDefinition::advancementId).thenComparing(CaseDefinition::criterion));
        return List.copyOf(cases);
    }

    private static CaseDefinition classifyCriterion(
        AcceptedCaseSpec spec,
        InventoryEntry inventoryEntry,
        JsonObject advancementJson,
        JsonObject criterionJson
    ) {
        String trigger = requiredString(criterionJson, "trigger");
        if (!INVENTORY_CHANGED_TRIGGER.equals(trigger)) {
            throw new IllegalStateException("Expected inventory_changed for " + spec.key() + " but got " + trigger);
        }
        JsonObject conditions = requiredObject(criterionJson, "conditions", spec.key());
        if (!sortedKeys(conditions).equals(List.of("items"))) {
            throw new IllegalStateException("Expected only conditions.items for " + spec.key() + " but found " + sortedKeys(conditions));
        }
        JsonArray items = requiredArray(conditions, "items", spec.key());
        if (items.size() != 1 || !items.get(0).isJsonObject()) {
            throw new IllegalStateException("Expected exactly one item predicate for " + spec.key());
        }
        JsonObject itemPredicate = items.get(0).getAsJsonObject();
        List<String> itemPredicateKeys = sortedKeys(itemPredicate);
        if (!itemPredicateKeys.equals(List.of("predicates")) && !itemPredicateKeys.equals(List.of("items", "predicates"))) {
            throw new IllegalStateException("Unsupported item predicate keys for " + spec.key() + ": " + itemPredicateKeys);
        }
        if (itemPredicate.has("components")) {
            throw new IllegalStateException("Meaningful component matcher is forbidden for " + spec.key());
        }
        JsonObject predicates = requiredObject(itemPredicate, "predicates", spec.key());
        if (predicates.has("stored_enchantments")) {
            throw new IllegalStateException("Stored enchantments are forbidden for " + spec.key());
        }
        if (!sortedKeys(predicates).equals(List.of("enchantments"))) {
            throw new IllegalStateException("Expected only predicates.enchantments for " + spec.key() + " but found " + sortedKeys(predicates));
        }
        List<EnchantmentPredicate> enchantments = parseDirectEnchantmentPredicates(predicates, spec.key());
        if (enchantments.isEmpty()) {
            throw new IllegalStateException("Expected direct enchantment predicates for " + spec.key());
        }
        List<String> allowedItems = parseAllowedItems(itemPredicate.get("items"));
        validateSelectedItem(spec, allowedItems, enchantments);
        validateRequirementGroupCoverage(advancementJson, spec);

        return new CaseDefinition(
            spec.advancementId(),
            inventoryEntry.sourcePath(),
            spec.criterion(),
            trigger,
            SUPPORTED,
            "SIMPLE_DIRECT_ENCHANTMENT_PICKUP",
            spec.selectedItem(),
            List.copyOf(allowedItems),
            List.copyOf(itemPredicateKeys),
            List.of(),
            List.copyOf(enchantments)
        );
    }

    private static int validateCaseSet(List<CaseDefinition> cases, Path projectRoot) throws IOException {
        if (cases.size() != 25) {
            throw new IllegalStateException("Expected 25 expansion cases but found " + cases.size());
        }
        long uniqueAdvancements = cases.stream().map(CaseDefinition::advancementId).distinct().count();
        if (uniqueAdvancements != 16) {
            throw new IllegalStateException("Expected 16 unique advancements but found " + uniqueAdvancements);
        }
        if (!CLOSED_CATALOG_SHA_256.equals(sha256(projectRoot.resolve(CLOSED_CATALOG_PATH)))) {
            throw new IllegalStateException("Closed enchantment catalog SHA drifted during catalog derivation");
        }
        Map<String, List<CaseDefinition>> byAdvancement = casesByAdvancement(cases);
        if (byAdvancement.size() != 16) {
            throw new IllegalStateException("Expected 16 grouped advancements but found " + byAdvancement.size());
        }
        int requirementGroupCount = deriveSelectedRequirementGroupCount(byAdvancement, projectRoot);
        if (requirementGroupCount != 25) {
            throw new IllegalStateException("Expected 25 requirement groups covered but found " + requirementGroupCount);
        }
        return requirementGroupCount;
    }

    private static int deriveSelectedRequirementGroupCount(
        Map<String, List<CaseDefinition>> byAdvancement,
        Path projectRoot
    ) throws IOException {
        int requirementGroupCount = 0;
        for (Map.Entry<String, List<CaseDefinition>> entry : byAdvancement.entrySet()) {
            Set<String> selectedCriteria = new TreeSet<>();
            String sourcePath = null;
            for (CaseDefinition caseDefinition : entry.getValue()) {
                selectedCriteria.add(caseDefinition.criterion());
                sourcePath = caseDefinition.sourcePath();
            }
            if (sourcePath == null) {
                throw new IllegalStateException("Missing sourcePath for " + entry.getKey());
            }
            JsonObject advancementJson = loadSingleFrozenAdvancement(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE), sourcePath);
            List<List<String>> requirements = completionRequirements(advancementJson);
            requirementGroupCount += requirements.size();
            for (List<String> requirementGroup : requirements) {
                boolean satisfied = false;
                for (String criterion : requirementGroup) {
                    if (selectedCriteria.contains(criterion)) {
                        satisfied = true;
                        break;
                    }
                }
                if (!satisfied) {
                    throw new IllegalStateException("Selected criteria do not cover requirement group for " + entry.getKey() + ": " + requirementGroup);
                }
            }
        }
        return requirementGroupCount;
    }
    private static Map<String, List<CaseDefinition>> casesByAdvancement(List<CaseDefinition> cases) {
        Map<String, List<CaseDefinition>> byAdvancement = new LinkedHashMap<>();
        for (CaseDefinition caseDefinition : cases) {
            byAdvancement.computeIfAbsent(caseDefinition.advancementId(), ignored -> new ArrayList<>()).add(caseDefinition);
        }
        return byAdvancement;
    }

    private static void validateRequirementGroupCoverage(JsonObject advancementJson, AcceptedCaseSpec spec) {
        JsonObject criteria = requiredObject(advancementJson, "criteria", spec.advancementId());
        if (!criteria.has(spec.criterion())) {
            throw new IllegalStateException("Criterion drifted out of advancement requirements for " + spec.key());
        }
    }

    private static void validateSelectedItem(
        AcceptedCaseSpec spec,
        List<String> allowedItems,
        List<EnchantmentPredicate> enchantments
    ) {
        if (!allowedItems.isEmpty() && !allowedItems.contains(spec.selectedItem())) {
            throw new IllegalStateException("Selected item " + spec.selectedItem() + " is not allowed for " + spec.key());
        }
        Holder.Reference<Item> item = REGISTRY_LOOKUP.lookupOrThrow(Registries.ITEM)
            .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ITEM, Identifier.parse(spec.selectedItem())));
        ItemStack stack = new ItemStack(item);
        for (EnchantmentPredicate enchantment : enchantments) {
            if (!ENCHANTMENTS.equals(enchantment.storageType())) {
                throw new IllegalStateException("Expected ENCHANTMENTS storage for " + spec.key());
            }
            int selectedLevel = enchantment.minLevel() != null ? enchantment.minLevel() : 1;
            Holder.Reference<Enchantment> holder = REGISTRY_LOOKUP.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(enchantment.selector())));
            stack.enchant(holder, selectedLevel);
            int actualLevel = stack.getEnchantments().getLevel(holder);
            if (actualLevel != selectedLevel) {
                throw new IllegalStateException(
                    "Selected item " + spec.selectedItem() + " cannot carry " + enchantment.selector() + "=" + selectedLevel + " for " + spec.key()
                );
            }
        }
    }

    private static List<EnchantmentPredicate> parseDirectEnchantmentPredicates(JsonObject predicates, String caseKey) {
        JsonArray enchantmentsJson = requiredArray(predicates, "enchantments", caseKey);
        List<EnchantmentPredicate> result = new ArrayList<>();
        for (JsonElement element : enchantmentsJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Expected enchantment predicate object for " + caseKey);
            }
            JsonObject entry = element.getAsJsonObject();
            JsonElement selectorElement = entry.get("enchantments");
            if (selectorElement == null || !selectorElement.isJsonPrimitive()) {
                throw new IllegalStateException("Expected primitive enchantment selector for " + caseKey);
            }
            String selector = selectorElement.getAsString();
            LevelBounds bounds = parseLevelBounds(entry.get("levels"));
            result.add(new EnchantmentPredicate(selector, ENCHANTMENTS, bounds.min(), bounds.max()));
        }
        result.sort(Comparator.comparing(EnchantmentPredicate::selector));
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

    private static List<String> parseAllowedItems(JsonElement itemsElement) {
        if (itemsElement == null || itemsElement.isJsonNull()) {
            return List.of();
        }
        if (itemsElement.isJsonPrimitive() && itemsElement.getAsJsonPrimitive().isString()) {
            return List.of(itemsElement.getAsString());
        }
        if (!itemsElement.isJsonArray()) {
            throw new IllegalStateException("Expected allowed items array or string");
        }
        List<String> items = new ArrayList<>();
        for (JsonElement element : itemsElement.getAsJsonArray()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException("Expected string item id in allowed items");
            }
            items.add(element.getAsString());
        }
        items.sort(String::compareTo);
        return List.copyOf(items);
    }

    private static Map<String, InventoryEntry> loadCanonicalInventory(Path projectRoot) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(projectRoot.resolve(PhaseACertification.INVENTORY_SNAPSHOT), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray entries = root.getAsJsonArray("entries");
        Map<String, InventoryEntry> inventory = new LinkedHashMap<>();
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            InventoryEntry inventoryEntry = new InventoryEntry(
                entry.get("id").getAsString(),
                entry.get("sourcePath").getAsString()
            );
            inventory.put(inventoryEntry.advancementId(), inventoryEntry);
        }
        return Map.copyOf(inventory);
    }

    private static Map<String, JsonObject> loadFrozenAdvancements(Path zipPath, Iterable<InventoryEntry> inventoryEntries) throws IOException {
        Map<String, JsonObject> advancements = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
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
        return Map.copyOf(advancements);
    }

    private static JsonObject loadSingleFrozenAdvancement(Path zipPath, String sourcePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry zipEntry = zipFile.getEntry(sourcePath);
            if (zipEntry == null) {
                throw new IllegalStateException("Missing frozen advancement entry " + sourcePath);
            }
            try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                return readJson(inputStream);
            }
        }
    }

    private static JsonObject readJson(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static List<List<String>> completionRequirements(JsonObject advancementJson) {
        JsonArray requirementsJson = advancementJson.has("requirements") && advancementJson.get("requirements").isJsonArray()
            ? advancementJson.getAsJsonArray("requirements")
            : null;
        List<List<String>> requirements = new ArrayList<>();
        if (requirementsJson != null) {
            for (JsonElement groupElement : requirementsJson) {
                if (!groupElement.isJsonArray()) {
                    continue;
                }
                List<String> group = new ArrayList<>();
                for (JsonElement criterionElement : groupElement.getAsJsonArray()) {
                    group.add(criterionElement.getAsString());
                }
                requirements.add(List.copyOf(group));
            }
            return List.copyOf(requirements);
        }
        JsonObject criteria = advancementJson.getAsJsonObject("criteria");
        if (criteria != null) {
            for (Map.Entry<String, JsonElement> criterion : criteria.entrySet()) {
                requirements.add(List.of(criterion.getKey()));
            }
        }
        return List.copyOf(requirements);
    }

    private static JsonObject requiredObject(JsonObject json, String key, String context) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalStateException("Expected object " + key + " for " + context);
        }
        return json.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key, String context) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalStateException("Expected array " + key + " for " + context);
        }
        return json.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Expected string " + key);
        }
        return json.get(key).getAsString();
    }

    private static List<String> sortedKeys(JsonObject json) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }
        keys.sort(String::compareTo);
        return List.copyOf(keys);
    }

    static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(Files.readAllBytes(path))).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            int b = value & 0xFF;
            builder.append(Character.forDigit((b >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    private record InventoryEntry(String advancementId, String sourcePath) {
    }

    private record AcceptedCaseSpec(String advancementId, String criterion, String selectedItem) {
        String key() {
            return advancementId + "#" + criterion;
        }
    }

    private record LevelBounds(Integer min, Integer max) {
    }

    record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String automationEligibility,
        String deferReason,
        String selectedItem,
        List<String> allowedItems,
        List<String> itemPredicateKeys,
        List<String> componentKeys,
        List<EnchantmentPredicate> enchantmentPredicates
    ) {
    }

    record EnchantmentPredicate(String selector, String storageType, Integer minLevel, Integer maxLevel) {
    }
}
