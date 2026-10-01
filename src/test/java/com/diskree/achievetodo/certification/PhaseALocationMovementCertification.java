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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseALocationMovementCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "location_movement_matrix.json");

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private static final Path FROZEN_BACAP = Path.of("reference", "phase_a_preservation", "files", "final", "bacap.zip");
    private static final Path RUNTIME_COMPATIBLE_BACAP = Path.of("reference", "localization", "fixtures", "bacap_1.21.zip");
    // Immutable matrix provenance describes the original audited acquisition path.
    private static final String HISTORICAL_RUNTIME_SOURCE = "reference/localization/.official_audit_cache/bacap_1.21.zip";
    private static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final String AUTOMATION_DEFERRED = "DEFERRED";
    private static final Set<String> AUTOMATED_SIMPLE_ADVANCEMENTS = Set.of(
        "blazeandcave:animal/mooshroom_kingdom",
        "blazeandcave:biomes/cold_feet",
        "blazeandcave:biomes/enchanted_forest",
        "blazeandcave:biomes/high_feet",
        "blazeandcave:biomes/one_with_the_forest",
        "blazeandcave:biomes/overgrown",
        "blazeandcave:biomes/pretty_in_pink",
        "blazeandcave:biomes/the_great_blocky_reef",
        "blazeandcave:biomes/the_mighty_jungle",
        "blazeandcave:biomes/the_sea_calls_you",
        "blazeandcave:biomes/theres_a_zombie_on_the_lawn",
        "blazeandcave:biomes/warm_feet",
        "blazeandcave:biomes/wet_feet",
        "blazeandcave:end/void_walker",
        "minecraft:adventure/adventuring_time",
        "minecraft:nether/explore_nether"
    );

    private PhaseALocationMovementCertification() {
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
        Map<String, JsonObject> runtimeAdvancements = loadAdvancements(projectRoot.resolve(RUNTIME_COMPATIBLE_BACAP), canonicalInventory.values());
        Map<String, RuntimeSemanticComparison> runtimeComparisons = compareSimpleBiomeAdvancements(frozenAdvancements, runtimeAdvancements);

        List<LocationMovementCase> cases = new ArrayList<>();
        for (InventorySource inventorySource : canonicalInventory.values()) {
            JsonObject advancement = frozenAdvancements.get(inventorySource.id());
            if (advancement == null || !advancement.has("criteria")) {
                continue;
            }
            JsonObject criteria = advancement.getAsJsonObject("criteria");
            JsonArray requirements = advancement.has("requirements") && advancement.get("requirements").isJsonArray()
                ? advancement.getAsJsonArray("requirements")
                : null;
            for (Map.Entry<String, JsonElement> criterionEntry : criteria.entrySet()) {
                if (!criterionEntry.getValue().isJsonObject()) {
                    continue;
                }
                LocationMovementCase locationCase = classifyCase(
                    inventorySource,
                    criterionEntry.getKey(),
                    criterionEntry.getValue().getAsJsonObject(),
                    requirements,
                    runtimeComparisons
                );
                if (locationCase != null) {
                    cases.add(locationCase);
                }
            }
        }
        cases.sort(Comparator
            .comparing(LocationMovementCase::advancementId)
            .thenComparing(LocationMovementCase::criterion));

        Summary summary = summarize(cases);
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "phase_a_location_movement_matrix");
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", FROZEN_BACAP.toString().replace('\\', '/'));
        root.addProperty("runtimeCompatibleSource", HISTORICAL_RUNTIME_SOURCE);
        root.add("summary", summary.toJson());

        JsonArray caseArray = new JsonArray();
        for (LocationMovementCase locationCase : cases) {
            caseArray.add(locationCase.toJson());
        }
        root.add("cases", caseArray);
        return GSON.toJson(root) + System.lineSeparator();
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve(SNAPSHOT), generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    private static Summary summarize(List<LocationMovementCase> cases) {
        Map<String, Integer> byCategory = new TreeMap<>();
        Map<String, Integer> byStatus = new TreeMap<>();
        int simpleBiomeCases = 0;
        int automationSupportedCases = 0;
        int automationDeferredCases = 0;
        for (LocationMovementCase locationCase : cases) {
            byCategory.merge(locationCase.category(), 1, Integer::sum);
            byStatus.merge(locationCase.certificationStatus(), 1, Integer::sum);
            if (locationCase.category().equals("BIOME_ONLY") || locationCase.category().equals("BIOME_SET")) {
                simpleBiomeCases++;
            }
            if (AUTOMATION_SUPPORTED.equals(locationCase.automationEligibility())) {
                automationSupportedCases++;
            } else {
                automationDeferredCases++;
            }
        }
        return new Summary(
            cases.size(),
            simpleBiomeCases,
            byStatus.getOrDefault("AUTOMATED_GREEN", 0),
            byStatus.getOrDefault("AUTOMATED_RED", 0),
            byStatus.getOrDefault("RUNTIME_DEFERRED", 0),
            automationSupportedCases,
            automationDeferredCases,
            countCases(cases, locationCase -> "DIMENSION_LOCATION".equals(locationCase.category())),
            countCases(cases, locationCase -> "DIMENSION_ONLY".equals(locationCase.dimensionLocationType())),
            countCases(cases, locationCase -> "DIMENSION_PLUS_OTHER".equals(locationCase.dimensionLocationType())),
            countCases(cases, locationCase -> "POSITION_ONLY".equals(locationCase.dimensionLocationType())),
            countCases(cases, locationCase -> "AUTOMATED_GREEN".equals(locationCase.certificationStatus()) && "DIMENSION_ONLY".equals(locationCase.dimensionLocationType())),
            byCategory,
            byStatus
        );
    }

    private static int countCases(List<LocationMovementCase> cases, java.util.function.Predicate<LocationMovementCase> predicate) {
        int total = 0;
        for (LocationMovementCase locationCase : cases) {
            if (predicate.test(locationCase)) {
                total++;
            }
        }
        return total;
    }

    private static LocationMovementCase classifyCase(
        InventorySource inventorySource,
        String criterion,
        JsonObject criterionJson,
        JsonArray requirements,
        Map<String, RuntimeSemanticComparison> runtimeComparisons
    ) {
        String trigger = criterionJson.has("trigger") ? criterionJson.get("trigger").getAsString() : "";
        if (!"minecraft:location".equals(trigger)) {
            return null;
        }
        JsonObject conditions = criterionJson.has("conditions") && criterionJson.get("conditions").isJsonObject()
            ? criterionJson.getAsJsonObject("conditions")
            : new JsonObject();
        JsonElement playerConditions = conditions.get("player");
        LocationDetails details = extractLocationDetails(playerConditions);
        if (details.locationPredicate().entrySet().isEmpty()) {
            return null;
        }

        String certificationStatus;
        String certificationReason;
        String automationEligibility;
        RuntimeSemanticComparison runtimeComparison = runtimeComparisons.get(inventorySource.id());
        boolean simpleBiome = details.category().equals("BIOME_ONLY") || details.category().equals("BIOME_SET");
        boolean structureOnly = "STRUCTURE_LOCATION".equals(details.category()) && "STRUCTURE_ONLY".equals(details.structureLocationType());
        String dimensionCaseKey = inventorySource.id() + "#" + criterion;
        if (simpleBiome && runtimeComparison != null && runtimeComparison.equivalent() && AUTOMATED_SIMPLE_ADVANCEMENTS.contains(inventorySource.id())) {
            certificationStatus = "AUTOMATED_GREEN";
            automationEligibility = AUTOMATION_SUPPORTED;
            certificationReason = "PROVEN_EQUIVALENT_SIMPLE_BIOME";
        } else if (simpleBiome && runtimeComparison != null && !runtimeComparison.equivalent()) {
            certificationStatus = "RUNTIME_DEFERRED";
            automationEligibility = AUTOMATION_DEFERRED;
            certificationReason = "RUNTIME_SEMANTICS_DIVERGENT: " + runtimeComparison.reason();
        } else if (structureOnly) {
            certificationStatus = "RUNTIME_DEFERRED";
            automationEligibility = AUTOMATION_SUPPORTED;
            certificationReason = "RUNTIME_DEFERRED: STRUCTURE_ONLY";
        } else {
            certificationStatus = "RUNTIME_DEFERRED";
            automationEligibility = AUTOMATION_DEFERRED;
            String specialization = !details.dimensionLocationType().isEmpty()
                ? details.dimensionLocationType()
                : details.structureLocationTypeOrCategory();
            certificationReason = "RUNTIME_DEFERRED: " + specialization;
        }

        return new LocationMovementCase(
            inventorySource.id(),
            inventorySource.sourcePath(),
            criterion,
            trigger,
            details.category(),
            details.dimensionLocationType(),
            details.structureLocationType(),
            details.locationPredicate(),
            details.biomes(),
            details.dimensions(),
            details.structures(),
            details.blockOrFluidConditions(),
            details.playerPredicate(),
            details.additionalConditions(),
            requirementsToList(requirements),
            automationEligibility,
            certificationStatus,
            certificationReason
        );
    }

    private static List<List<String>> requirementsToList(JsonArray requirements) {
        if (requirements == null) {
            return List.of();
        }
        List<List<String>> values = new ArrayList<>();
        for (JsonElement requirement : requirements) {
            if (!requirement.isJsonArray()) {
                continue;
            }
            List<String> group = new ArrayList<>();
            for (JsonElement criterion : requirement.getAsJsonArray()) {
                group.add(criterion.getAsString());
            }
            values.add(group);
        }
        return values;
    }

    private static LocationDetails extractLocationDetails(JsonElement playerConditions) {
        JsonObject locationPredicate = new JsonObject();
        Set<String> biomes = new TreeSet<>();
        Set<String> dimensions = new TreeSet<>();
        Set<String> structures = new TreeSet<>();
        List<String> blockOrFluidConditions = new ArrayList<>();
        List<String> additionalConditions = new ArrayList<>();
        boolean spectatorExcluded = containsSpectatorExclusion(playerConditions);
        collectLocationTerms(playerConditions, locationPredicate, biomes, dimensions, structures, blockOrFluidConditions, additionalConditions);

        String category;
        String dimensionLocationType = "";
        String structureLocationType = "";
        boolean hasBiome = !biomes.isEmpty();
        boolean hasDimension = !dimensions.isEmpty();
        boolean hasStructure = !structures.isEmpty();
        boolean hasBlockOrFluid = !blockOrFluidConditions.isEmpty();
        boolean hasPosition = locationPredicate.has("position");
        boolean hasLight = locationPredicate.has("light");
        if (hasStructure) {
            category = "STRUCTURE_LOCATION";
            if (!hasBiome && !hasDimension && !hasBlockOrFluid && !hasPosition && !hasLight && additionalConditions.isEmpty()) {
                structureLocationType = "STRUCTURE_ONLY";
            } else if (hasBiome && !hasDimension && !hasBlockOrFluid && !hasPosition && !hasLight && additionalConditions.isEmpty()) {
                structureLocationType = "STRUCTURE_PLUS_BIOME";
            } else if (hasDimension && !hasBiome && !hasBlockOrFluid && !hasPosition && !hasLight && additionalConditions.isEmpty()) {
                structureLocationType = "STRUCTURE_PLUS_DIMENSION";
            } else if (!additionalConditions.isEmpty() && !hasBiome && !hasDimension && !hasBlockOrFluid && !hasPosition && !hasLight) {
                structureLocationType = "STRUCTURE_PLUS_PLAYER_STATE";
            } else {
                structureLocationType = "STRUCTURE_PLUS_OTHER";
            }
        } else if (hasBlockOrFluid || hasLight) {
            category = "BLOCK_OR_SURFACE_MOVEMENT";
        } else if (hasDimension || hasPosition) {
            category = "DIMENSION_LOCATION";
            if (hasDimension && !hasPosition && !hasBiome && additionalConditions.isEmpty()) {
                dimensionLocationType = "DIMENSION_ONLY";
            } else if (hasDimension) {
                dimensionLocationType = "DIMENSION_PLUS_OTHER";
            } else {
                dimensionLocationType = "POSITION_ONLY";
            }
        } else if (hasBiome) {
            category = biomes.size() == 1 ? "BIOME_ONLY" : "BIOME_SET";
        } else {
            category = "MIXED_OR_COMPLEX";
        }

        String playerPredicate = spectatorExcluded ? "SURVIVAL_OR_NON_SPECTATOR" : "NO_EXPLICIT_SPECTATOR_EXCLUSION";
        return new LocationDetails(
            category,
            dimensionLocationType,
            structureLocationType,
            locationPredicate,
            List.copyOf(biomes),
            List.copyOf(dimensions),
            List.copyOf(structures),
            List.copyOf(blockOrFluidConditions),
            playerPredicate,
            List.copyOf(additionalConditions)
        );
    }

    private static void collectLocationTerms(
        JsonElement playerConditions,
        JsonObject locationPredicate,
        Set<String> biomes,
        Set<String> dimensions,
        Set<String> structures,
        List<String> blockOrFluidConditions,
        List<String> additionalConditions
    ) {
        if (playerConditions == null || playerConditions.isJsonNull()) {
            return;
        }
        if (playerConditions.isJsonArray()) {
            for (JsonElement term : playerConditions.getAsJsonArray()) {
                if (!term.isJsonObject()) {
                    continue;
                }
                JsonObject condition = term.getAsJsonObject();
                String conditionType = condition.has("condition") ? condition.get("condition").getAsString() : "";
                if ("minecraft:entity_properties".equals(conditionType)) {
                    collectLocationPredicateFromEntityProperties(
                        condition,
                        locationPredicate,
                        biomes,
                        dimensions,
                        structures,
                        blockOrFluidConditions,
                        additionalConditions
                    );
                } else if ("minecraft:any_of".equals(conditionType) && condition.has("terms")) {
                    for (JsonElement nested : condition.getAsJsonArray("terms")) {
                        if (nested.isJsonObject()) {
                            collectLocationPredicateFromEntityProperties(
                                nested.getAsJsonObject(),
                                locationPredicate,
                                biomes,
                                dimensions,
                                structures,
                                blockOrFluidConditions,
                                additionalConditions
                            );
                        }
                    }
                } else if ("minecraft:inverted".equals(conditionType) && condition.has("term") && condition.get("term").isJsonObject()) {
                    JsonObject nestedTerm = condition.getAsJsonObject("term");
                    String nestedType = nestedTerm.has("condition") ? nestedTerm.get("condition").getAsString() : "";
                    if ("minecraft:entity_properties".equals(nestedType)) {
                        collectLocationPredicateFromEntityProperties(
                            nestedTerm,
                            locationPredicate,
                            biomes,
                            dimensions,
                            structures,
                            blockOrFluidConditions,
                            additionalConditions
                        );
                    } else if (!nestedType.isEmpty()) {
                        additionalConditions.add("inverted." + nestedType);
                    }
                } else if (!"minecraft:inverted".equals(conditionType)) {
                    additionalConditions.add(conditionType);
                }
            }
        }
    }

    private static void collectLocationPredicateFromEntityProperties(
        JsonObject entityProperties,
        JsonObject locationPredicate,
        Set<String> biomes,
        Set<String> dimensions,
        Set<String> structures,
        List<String> blockOrFluidConditions,
        List<String> additionalConditions
    ) {
        if (!entityProperties.has("predicate") || !entityProperties.get("predicate").isJsonObject()) {
            return;
        }
        JsonObject predicate = entityProperties.getAsJsonObject("predicate");
        if (!predicate.has("location") || !predicate.get("location").isJsonObject()) {
            return;
        }
        JsonObject location = predicate.getAsJsonObject("location");
        mergeLocationPredicate(locationPredicate, location);
        if (location.has("biomes")) {
            collectStringValues(location.get("biomes"), biomes);
        }
        if (location.has("dimension")) {
            dimensions.add(location.get("dimension").getAsString());
        }
        if (location.has("structures")) {
            collectStringValues(location.get("structures"), structures);
        }
        if (location.has("block")) {
            blockOrFluidConditions.add("block=" + location.get("block"));
        }
        if (location.has("fluid")) {
            blockOrFluidConditions.add("fluid=" + location.get("fluid"));
        }
        if (location.has("light")) {
            blockOrFluidConditions.add("light=" + location.get("light"));
        }
        for (Map.Entry<String, JsonElement> entry : predicate.entrySet()) {
            String key = entry.getKey();
            if ("location".equals(key)) {
                continue;
            }
            if ("type_specific".equals(key) && entry.getValue().isJsonObject()) {
                JsonObject typeSpecific = entry.getValue().getAsJsonObject();
                if (typeSpecific.has("advancements")) {
                    additionalConditions.add("type_specific.advancements");
                }
                for (Map.Entry<String, JsonElement> nestedEntry : typeSpecific.entrySet()) {
                    String nestedKey = nestedEntry.getKey();
                    if (!"advancements".equals(nestedKey)) {
                        additionalConditions.add("type_specific." + nestedKey);
                    }
                }
                continue;
            }
            additionalConditions.add(key);
        }
    }

    private static void mergeLocationPredicate(JsonObject target, JsonObject addition) {
        for (Map.Entry<String, JsonElement> entry : addition.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!target.has(key)) {
                target.add(key, value.deepCopy());
                continue;
            }
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                JsonArray values;
                if (target.get(key).isJsonArray()) {
                    values = target.getAsJsonArray(key);
                } else {
                    values = new JsonArray();
                    values.add(target.get(key).getAsString());
                    target.add(key, values);
                }
                String candidate = value.getAsString();
                boolean present = false;
                for (JsonElement existing : values) {
                    if (candidate.equals(existing.getAsString())) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    values.add(candidate);
                }
            }
        }
    }

    private static void collectStringValues(JsonElement element, Set<String> target) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                collectStringValues(entry, target);
            }
            return;
        }
        target.add(element.getAsString());
    }

    private static boolean containsSpectatorExclusion(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("gamemode")) {
                JsonElement gamemode = object.get("gamemode");
                if (gamemode.isJsonArray()) {
                    for (JsonElement entry : gamemode.getAsJsonArray()) {
                        if ("spectator".equals(entry.getAsString())) {
                            return true;
                        }
                    }
                } else if (gamemode.isJsonPrimitive() && "spectator".equals(gamemode.getAsString())) {
                    return true;
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (containsSpectatorExclusion(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (containsSpectatorExclusion(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, RuntimeSemanticComparison> compareSimpleBiomeAdvancements(
        Map<String, JsonObject> frozenAdvancements,
        Map<String, JsonObject> runtimeAdvancements
    ) {
        Map<String, RuntimeSemanticComparison> comparisons = new LinkedHashMap<>();
        for (Map.Entry<String, JsonObject> frozenEntry : frozenAdvancements.entrySet()) {
            String advancementId = frozenEntry.getKey();
            JsonObject frozen = frozenEntry.getValue();
            JsonObject runtime = runtimeAdvancements.get(advancementId);
            if (runtime == null) {
                continue;
            }
            if (!advancementContainsSimpleBiomeCriteria(frozen)) {
                continue;
            }
            JsonObject normalizedFrozen = normalizeCriterionShapes(extractSemanticPayload(frozen));
            JsonObject normalizedRuntime = normalizeCriterionShapes(extractSemanticPayload(runtime));
            if (!canonicalJson(normalizedFrozen).equals(canonicalJson(normalizedRuntime))) {
                comparisons.put(advancementId, new RuntimeSemanticComparison(false, explainRuntimeDifference(normalizedFrozen, normalizedRuntime)));
            } else {
                comparisons.put(advancementId, new RuntimeSemanticComparison(true, "SEMANTIC_EQUIVALENCE_PROVEN"));
            }
        }
        return comparisons;
    }

    private static boolean advancementContainsSimpleBiomeCriteria(JsonObject advancement) {
        if (!advancement.has("criteria")) {
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : advancement.getAsJsonObject("criteria").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            LocationMovementCase locationCase = classifyCase(
                new InventorySource("probe", "probe"),
                entry.getKey(),
                entry.getValue().getAsJsonObject(),
                null,
                Map.of()
            );
            if (locationCase != null && (locationCase.category().equals("BIOME_ONLY") || locationCase.category().equals("BIOME_SET"))) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject extractSemanticPayload(JsonObject advancement) {
        JsonObject payload = new JsonObject();
        if (advancement.has("criteria")) {
            payload.add("criteria", advancement.getAsJsonObject("criteria").deepCopy());
        }
        if (advancement.has("requirements")) {
            payload.add("requirements", advancement.get("requirements").deepCopy());
        }
        return payload;
    }

    private static JsonObject normalizeCriterionShapes(JsonObject element) {
        return normalizeObject(element.deepCopy());
    }

    private static JsonObject normalizeObject(JsonObject object) {
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if ("minecraft:type_specific/player".equals(key)) {
                JsonObject playerTypeSpecific = value.getAsJsonObject().deepCopy();
                playerTypeSpecific.addProperty("type", "player");
                normalized.add("type_specific", normalizeObject(playerTypeSpecific));
                continue;
            }
            normalized.add(key, normalizeElement(value));
        }
        return normalized;
    }

    private static JsonElement normalizeElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return element;
        }
        if (element.isJsonObject()) {
            return normalizeObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                array.add(normalizeElement(child));
            }
            return array;
        }
        return element.deepCopy();
    }

    private static String explainRuntimeDifference(JsonObject frozen, JsonObject runtime) {
        Set<String> frozenCriteria = frozen.has("criteria")
            ? frozen.getAsJsonObject("criteria").keySet()
            : Set.of();
        Set<String> runtimeCriteria = runtime.has("criteria")
            ? runtime.getAsJsonObject("criteria").keySet()
            : Set.of();
        if (!frozenCriteria.equals(runtimeCriteria)) {
            Set<String> added = new TreeSet<>(runtimeCriteria);
            added.removeAll(frozenCriteria);
            Set<String> removed = new TreeSet<>(frozenCriteria);
            removed.removeAll(runtimeCriteria);
            return "CRITERIA_KEYS_CHANGED added=" + added + " removed=" + removed;
        }
        if (frozen.has("requirements") || runtime.has("requirements")) {
            String frozenRequirements = canonicalJson(frozen.get("requirements"));
            String runtimeRequirements = canonicalJson(runtime.get("requirements"));
            if (!frozenRequirements.equals(runtimeRequirements)) {
                return "REQUIREMENTS_CHANGED";
            }
        }
        if (frozen.has("criteria")) {
            JsonObject frozenCriteriaObject = frozen.getAsJsonObject("criteria");
            JsonObject runtimeCriteriaObject = runtime.getAsJsonObject("criteria");
            for (String criterion : frozenCriteriaObject.keySet()) {
                if (!canonicalJson(frozenCriteriaObject.get(criterion)).equals(canonicalJson(runtimeCriteriaObject.get(criterion)))) {
                    return "CRITERION_PAYLOAD_CHANGED: " + criterion;
                }
            }
        }
        return "UNKNOWN_DIFFERENCE";
    }

    private static String canonicalJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "null";
        }
        if (element.isJsonObject()) {
            Map<String, String> ordered = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                ordered.put(entry.getKey(), canonicalJson(entry.getValue()));
            }
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : ordered.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(entry.getKey()).append('"').append(':').append(entry.getValue());
            }
            return builder.append('}').toString();
        }
        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (JsonElement child : element.getAsJsonArray()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(canonicalJson(child));
            }
            return builder.append(']').toString();
        }
        return GSON.toJson(element);
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

    private static JsonObject readJson(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private record InventorySource(String id, String sourcePath) {
    }

    private record RuntimeSemanticComparison(boolean equivalent, String reason) {
    }

    private record Summary(
        int totalFamilyCases,
        int simpleBiomeCases,
        int automatedGreen,
        int automatedRed,
        int deferred,
        int automationSupported,
        int automationDeferred,
        int dimensionLocationCases,
        int dimensionOnlyCases,
        int dimensionPlusOtherCases,
        int positionOnlyCases,
        int dimensionAutomatedGreen,
        Map<String, Integer> byCategory,
        Map<String, Integer> byStatus
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("totalFamilyCases", totalFamilyCases);
            json.addProperty("simpleBiomeCases", simpleBiomeCases);
            json.addProperty("automatedGreen", automatedGreen);
            json.addProperty("automatedRed", automatedRed);
            json.addProperty("deferredToSpecializedHarness", deferred);
            json.addProperty("automationSupported", automationSupported);
            json.addProperty("automationDeferred", automationDeferred);
            json.addProperty("dimensionLocationCases", dimensionLocationCases);
            json.addProperty("dimensionOnlyCases", dimensionOnlyCases);
            json.addProperty("dimensionPlusOtherCases", dimensionPlusOtherCases);
            json.addProperty("positionOnlyCases", positionOnlyCases);
            json.addProperty("dimensionAutomatedGreen", dimensionAutomatedGreen);
            json.add("byCategory", mapToJson(byCategory));
            json.add("byStatus", mapToJson(byStatus));
            return json;
        }
    }

    private record LocationDetails(
        String category,
        String dimensionLocationType,
        String structureLocationType,
        JsonObject locationPredicate,
        List<String> biomes,
        List<String> dimensions,
        List<String> structures,
        List<String> blockOrFluidConditions,
        String playerPredicate,
        List<String> additionalConditions
    ) {
        String dimensionLocationTypeOrCategory() {
            return dimensionLocationType.isEmpty() ? category : dimensionLocationType;
        }

        String structureLocationTypeOrCategory() {
            return structureLocationType.isEmpty() ? category : structureLocationType;
        }
    }

    private record LocationMovementCase(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String category,
        String dimensionLocationType,
        String structureLocationType,
        JsonObject relevantLocationPredicate,
        List<String> biomes,
        List<String> dimensions,
        List<String> structures,
        List<String> blockOrFluidConditions,
        String playerPredicate,
        List<String> additionalConditions,
        List<List<String>> completionRequirements,
        String automationEligibility,
        String certificationStatus,
        String certificationReason
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("criterion", criterion);
            json.addProperty("trigger", trigger);
            json.addProperty("category", category);
            json.addProperty("dimensionLocationType", dimensionLocationType);
            json.addProperty("structureLocationType", structureLocationType);
            json.add("relevantLocationPredicate", relevantLocationPredicate.deepCopy());
            json.add("biomes", stringsToJsonArray(biomes));
            json.add("dimensions", stringsToJsonArray(dimensions));
            json.add("structures", stringsToJsonArray(structures));
            json.add("blockOrFluidConditions", stringsToJsonArray(blockOrFluidConditions));
            json.addProperty("playerPredicate", playerPredicate);
            json.add("additionalConditions", stringsToJsonArray(additionalConditions));
            JsonArray requirements = new JsonArray();
            for (List<String> requirementGroup : completionRequirements) {
                requirements.add(stringsToJsonArray(requirementGroup));
            }
            json.add("completionRequirements", requirements);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("certificationStatus", certificationStatus);
            json.addProperty("certificationReason", certificationReason);
            return json;
        }
    }

    private static JsonObject mapToJson(Map<String, Integer> values) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private static JsonArray stringsToJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
