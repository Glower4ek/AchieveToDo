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
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseAAdvancementRollup {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "phase_a_advancement_rollup.json");

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    private PhaseAAdvancementRollup() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 0
            ? Path.of(args[0]).toAbsolutePath().normalize()
            : Path.of("").toAbsolutePath().normalize();
        writeSnapshot(projectRoot);
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Files.writeString(projectRoot.resolve(SNAPSHOT), generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        return generateSnapshot(projectRoot, loadRuntimeEvidence(projectRoot));
    }

    // Explicit evidence input lets closure tests compare family additions without editing project artifacts.
    static String generateSnapshot(Path projectRoot, Map<String, RuntimeEvidence> runtimeEvidenceById) throws IOException {
        PhaseACertification.CertificationArtifacts artifacts = PhaseACertification.generate(projectRoot);
        PhaseACertification.StaticCertificationReport staticReport = PhaseACertification.analyzeStaticRules(projectRoot);
        Map<String, PhaseACertification.StaticValidationEntry> staticById = indexStaticReport(staticReport);

        List<RollupEntry> entries = new ArrayList<>();
        for (PhaseACertification.InventoryEntry inventoryEntry : artifacts.inventory()) {
            PhaseACertification.StaticValidationEntry staticEntry = staticById.get(inventoryEntry.id());
            if (staticEntry == null) {
                throw new IllegalStateException("Missing static validation entry for " + inventoryEntry.id());
            }
            RawAdvancement rawAdvancement = readRawAdvancement(projectRoot, inventoryEntry.sourcePath());
            RuntimeEvidence runtimeEvidence = runtimeEvidenceById.getOrDefault(inventoryEntry.id(), RuntimeEvidence.empty(inventoryEntry.id()));
            RollupStatus status = classify(staticEntry.status(), rawAdvancement.json(), runtimeEvidence);
            entries.add(buildEntry(inventoryEntry, staticEntry, rawAdvancement, runtimeEvidence, status));
        }

        entries.sort(Comparator.comparing(RollupEntry::id));
        Summary summary = summarize(entries, runtimeEvidenceById.keySet());

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "phase_a_advancement_rollup");
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("compatibilityMarker", "compat_26_2_r15");
        root.add("summary", summary.toJson());
        root.add("locationMovementImpact", buildLocationMovementImpact(entries));

        JsonArray entryArray = new JsonArray();
        for (RollupEntry entry : entries) {
            entryArray.add(entry.toJson());
        }
        root.add("entries", entryArray);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private static Summary summarize(List<RollupEntry> entries, Set<String> runtimeTouchedIds) {
        Map<String, Integer> statusTotals = new TreeMap<>();
        for (RollupEntry entry : entries) {
            statusTotals.merge(entry.advancementStatus().name(), 1, Integer::sum);
        }
        long staticCertifiedWithRuntimeEvidence = entries.stream()
            .filter(entry -> entry.advancementStatus() == RollupStatus.STATIC_CERTIFIED)
            .filter(entry -> !entry.runtimeGreenCriteria().isEmpty())
            .count();
        long runtimeCertified = entries.stream().filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_CERTIFIED).count();
        long runtimePartial = entries.stream().filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_PARTIAL).count();
        long runtimeDeferred = entries.stream().filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_DEFERRED).count();
        long staticFailUnresolved = entries.stream().filter(entry -> entry.advancementStatus() == RollupStatus.STATIC_FAIL_UNRESOLVED).count();
        long totalCertified = statusTotals.getOrDefault(RollupStatus.STATIC_CERTIFIED.name(), 0)
            + statusTotals.getOrDefault(RollupStatus.RUNTIME_CERTIFIED.name(), 0);
        return new Summary(
            entries.size(),
            totalCertified,
            runtimeCertified + runtimePartial,
            runtimeCertified,
            runtimePartial,
            runtimeDeferred + staticFailUnresolved,
            staticFailUnresolved,
            staticCertifiedWithRuntimeEvidence,
            statusTotals,
            runtimeTouchedIds.size()
        );
    }

    private static JsonObject buildLocationMovementImpact(List<RollupEntry> entries) {
        JsonObject impact = new JsonObject();
        long touched = entries.stream().filter(entry -> !entry.locationMovementCriteria().isEmpty()).count();
        long runtimeCertified = entries.stream()
            .filter(entry -> !entry.locationMovementCriteria().isEmpty())
            .filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_CERTIFIED)
            .count();
        long runtimePartial = entries.stream()
            .filter(entry -> !entry.locationMovementCriteria().isEmpty())
            .filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_PARTIAL)
            .count();
        long runtimeDeferred = entries.stream()
            .filter(entry -> !entry.locationMovementCriteria().isEmpty())
            .filter(entry -> entry.advancementStatus() == RollupStatus.RUNTIME_DEFERRED)
            .count();
        long staticCertified = entries.stream()
            .filter(entry -> !entry.locationMovementCriteria().isEmpty())
            .filter(entry -> entry.advancementStatus() == RollupStatus.STATIC_CERTIFIED)
            .count();
        long staticFailUnresolved = entries.stream()
            .filter(entry -> !entry.locationMovementCriteria().isEmpty())
            .filter(entry -> entry.advancementStatus() == RollupStatus.STATIC_FAIL_UNRESOLVED)
            .count();
        impact.addProperty("uniqueAdvancementsTouched", touched);
        impact.addProperty("runtimeCertified", runtimeCertified);
        impact.addProperty("runtimePartial", runtimePartial);
        impact.addProperty("runtimeDeferred", runtimeDeferred);
        impact.addProperty("staticCertified", staticCertified);
        impact.addProperty("staticFailUnresolved", staticFailUnresolved);
        return impact;
    }

    private static RollupEntry buildEntry(
        PhaseACertification.InventoryEntry inventoryEntry,
        PhaseACertification.StaticValidationEntry staticEntry,
        RawAdvancement rawAdvancement,
        RuntimeEvidence runtimeEvidence,
        RollupStatus status
    ) {
        List<List<String>> requirements = completionRequirements(rawAdvancement.json());
        boolean requirementsSatisfied = requirementsSatisfied(requirements, runtimeEvidence.greenCriteria());
        return new RollupEntry(
            inventoryEntry.id(),
            inventoryEntry.sourcePath(),
            staticEntry.status().name(),
            status,
            inventoryEntry.criteriaCount(),
            requirements,
            new ArrayList<>(runtimeEvidence.greenCriteria()),
            new ArrayList<>(runtimeEvidence.criteriaByFamily().getOrDefault(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY, Set.of())),
            runtimeEvidence.families(),
            runtimeEvidence.sources(),
            requirementsSatisfied,
            buildReason(staticEntry.status(), status, requirementsSatisfied, runtimeEvidence)
        );
    }

    static RollupStatus classify(
        PhaseACertification.StaticValidationStatus staticStatus,
        JsonObject rawAdvancement,
        RuntimeEvidence runtimeEvidence
    ) {
        boolean hasRuntimeGreen = !runtimeEvidence.greenCriteria().isEmpty();
        boolean requirementsSatisfied = requirementsSatisfied(completionRequirements(rawAdvancement), runtimeEvidence.greenCriteria());
        return switch (staticStatus) {
            case STATIC_FAIL -> RollupStatus.STATIC_FAIL_UNRESOLVED;
            case STATIC_PASS -> RollupStatus.STATIC_CERTIFIED;
            case RUNTIME_DEFERRED -> requirementsSatisfied
                ? RollupStatus.RUNTIME_CERTIFIED
                : hasRuntimeGreen
                    ? RollupStatus.RUNTIME_PARTIAL
                    : RollupStatus.RUNTIME_DEFERRED;
        };
    }

    private static String buildReason(
        PhaseACertification.StaticValidationStatus staticStatus,
        RollupStatus status,
        boolean requirementsSatisfied,
        RuntimeEvidence runtimeEvidence
    ) {
        return switch (status) {
            case STATIC_CERTIFIED -> "STATIC_CERTIFIED baseline; runtime evidence is supplemental only";
            case RUNTIME_CERTIFIED -> "Runtime evidence satisfied completion semantics after " + staticStatus;
            case RUNTIME_PARTIAL -> "Runtime evidence present but completion semantics still incomplete after " + staticStatus;
            case RUNTIME_DEFERRED -> "No runtime evidence yet after " + staticStatus;
            case STATIC_FAIL_UNRESOLVED -> requirementsSatisfied
                ? "Static failure remains unresolved even though runtime path is complete"
                : runtimeEvidence.greenCriteria().isEmpty()
                    ? "Static failure remains unresolved with no runtime evidence"
                    : "Static failure remains unresolved with only partial runtime evidence";
        };
    }

    private static Map<String, PhaseACertification.StaticValidationEntry> indexStaticReport(PhaseACertification.StaticCertificationReport report) {
        Map<String, PhaseACertification.StaticValidationEntry> byId = new LinkedHashMap<>();
        for (PhaseACertification.StaticValidationEntry entry : report.entries()) {
            byId.put(entry.id(), entry);
        }
        return byId;
    }

    static Map<String, RuntimeEvidence> loadRuntimeEvidence(Path projectRoot) throws IOException {
        Map<String, RuntimeEvidence> merged = new LinkedHashMap<>();
        mergeRuntimeEvidence(
            merged,
            adaptRuntimeEvidence(PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)),
            "location/structure"
        );
        mergeRuntimeEvidence(
            merged,
            adaptContainerEvidence(PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)),
            "container-loot"
        );
        mergeRuntimeEvidence(
            merged,
            adaptItemTagEvidence(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)),
            "item-tag-inventory_changed"
        );
        mergeRuntimeEvidence(
            merged,
            adaptEnchantmentEvidence(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)),
            "enchantment-holderset-inventory_changed"
        );
        mergeRuntimeEvidence(
            merged,
            adaptExpansionDirect16Evidence(
                PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
            ),
            "enchantment-expansion-direct16-inventory_changed"
        );
        mergeRuntimeEvidence(
            merged,
            adaptEntityTypeTagPlayerKilledEntityEvidence(
                PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
            ),
            "entity-type-tag-player_killed_entity"
        );
        if (Files.exists(projectRoot.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptPlacedBlockEvidence(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)),
                "item-tag-placed_block"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptTrimPatternRecipeCraftedEvidence(
                    PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "trim-pattern-recipe-crafted"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptBlockTagItemUsedOnBlockEvidence(
                    PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "block-tag-item_used_on_block"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptItemTagItemUsedOnBlockEvidence(
                    PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "item-tag-item_used_on_block"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptEntityTypeTagStartedRidingEvidence(
                    PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "entity-type-tag-started_riding"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptItemTagPlayerInteractedWithEntityEvidence(
                    PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "item-tag-player_interacted_with_entity"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptEntityTypeTagPlayerInteractedWithEntityEvidence(
                    PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "entity-type-tag-player_interacted_with_entity"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAShotCrossbowExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptShotCrossbowEvidence(
                    PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "shot-crossbow"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAUsingItemExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptUsingItemEvidence(
                    PhaseAUsingItemExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "using-item"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptPlayerHurtEntityDamageSourceEvidence(
                    PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "player-hurt-entity-damage-source"
            );
        }
        // New family evidence is optional so historical roll-up fixtures retain their frozen snapshot boundary.
        if (Files.exists(projectRoot.resolve(PhaseATameAnimalLlamaExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptTameAnimalLlamaEvidence(
                    PhaseATameAnimalLlamaExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "tame-animal-llama"
            );
        }
        // Optional evidence preserves older frozen roll-up fixture boundaries.
        if (Files.exists(projectRoot.resolve(PhaseARecipeCraftedWaxOnExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptRecipeCraftedWaxOnEvidence(
                    PhaseARecipeCraftedWaxOnExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "recipe-crafted-wax-on"
            );
        }
        // Optional evidence preserves every historical roll-up snapshot before this family existed.
        if (Files.exists(projectRoot.resolve(PhaseAItemTagInventoryContainmentExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptItemTagInventoryContainmentEvidence(
                    PhaseAItemTagInventoryContainmentExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "item-tag-inventory-containment"
            );
        }
        // This family is optional so all earlier accepted snapshots keep their original evidence boundary.
        if (Files.exists(projectRoot.resolve(PhaseAFishingRodHookedCertification.PERSISTENT_EVIDENCE))) {
            mergeRuntimeEvidence(
                merged,
                adaptFishingRodHookedEvidence(
                    PhaseAFishingRodHookedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "fishing-rod-hooked"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptTrimMaterialInventoryChangedEvidence(
                    PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "trim-material-inventory-changed"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptItemUsedOnBlockContextualEvidence(
                    PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "item-used-on-block-contextual"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonRedstoneClickExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonRedstoneClickEvidence(
                    PhaseASingletonRedstoneClickExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-redstone-click"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonMangroveGroveExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonMangroveGroveEvidence(
                    PhaseASingletonMangroveGroveExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-mangrove-grove"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonDiagonAllayExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonDiagonAllayEvidence(
                    PhaseASingletonDiagonAllayExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-diagon-allay"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonVillageBellExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonVillageBellEvidence(
                    PhaseASingletonVillageBellExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-village-bell"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonLlamaBreedingExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonLlamaBreedingEvidence(
                    PhaseASingletonLlamaBreedingExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-llama-breeding"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonAxolotlMonumentExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonAxolotlMonumentEvidence(
                    PhaseASingletonAxolotlMonumentExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-axolotl-monument"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonMaximumResistanceExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonMaximumResistanceEvidence(
                    PhaseASingletonMaximumResistanceExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-maximum-resistance"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonMiracleDrinkExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonMiracleDrinkEvidence(
                    PhaseASingletonMiracleDrinkExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-miracle-drink"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonAncientRestorationExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonAncientRestorationEvidence(
                    PhaseASingletonAncientRestorationExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-ancient-restoration"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseASingletonSilkTouchNestExecutionEvidenceValidation.PERSISTENT_ARTIFACT))) {
            mergeRuntimeEvidence(
                merged,
                adaptSingletonSilkTouchNestEvidence(
                    PhaseASingletonSilkTouchNestExecutionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot)
                ),
                "singleton-silk-touch-nest"
            );
        }
        if (Files.exists(projectRoot.resolve(PhaseAPureEnchantmentInventoryChangedCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAPureEnchantmentInventoryChangedEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "pure-enchantment-inventory-changed");
        }
        if (Files.exists(projectRoot.resolve(PhaseALocationHolderSetWorldgenCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseALocationHolderSetWorldgenEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "player-killed-entity-remainder");
        }
        if (Files.exists(projectRoot.resolve(PhaseAMixedPerfectRunCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "mixed-perfect-run");
        }
        if (Files.exists(projectRoot.resolve(PhaseAStackAllItemsCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAStackAllItemsEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "stack-all-items-inventory-backlog");
        }
        if (Files.exists(projectRoot.resolve(PhaseAMixedWeaponryMulticlassedCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAMixedWeaponryMulticlassedEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "mixed-weaponry-multiclassed");
        }
        if (Files.exists(projectRoot.resolve(PhaseAMixedPiglinDistractionCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAMixedPiglinDistractionEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "mixed-piglin-distraction");
        }
        if (Files.exists(projectRoot.resolve(PhaseAPlayerKilledEntityRemainderCertification.PERSISTENT))) {
            mergeRuntimeEvidence(merged, PhaseAPlayerKilledEntityRemainderEvidenceValidation.loadValidatedRuntimeEvidence(projectRoot), "player-killed-entity-remainder");
        }
        return merged;
    }

    static Map<String, RuntimeEvidence> adaptItemTagPlayerInteractedWithEntityEvidence(
        Map<String, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptEntityTypeTagPlayerInteractedWithEntityEvidence(
        Map<String, PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()), copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()), new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptShotCrossbowEvidence(
        Map<String, PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptUsingItemEvidence(
        Map<String, PhaseAUsingItemExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAUsingItemExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAUsingItemExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptPlayerHurtEntityDamageSourceEvidence(
        Map<String, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptTameAnimalLlamaEvidence(
        Map<String, PhaseATameAnimalLlamaExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptRecipeCraftedWaxOnEvidence(
        Map<String, PhaseARecipeCraftedWaxOnExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptItemTagInventoryContainmentEvidence(
        Map<String, PhaseAItemTagInventoryContainmentExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptFishingRodHookedEvidence(
        Map<String, PhaseAFishingRodHookedExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptTrimMaterialInventoryChangedEvidence(
        Map<String, PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptItemUsedOnBlockContextualEvidence(
        Map<String, PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonRedstoneClickEvidence(
        Map<String, PhaseASingletonRedstoneClickExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonMangroveGroveEvidence(
        Map<String, PhaseASingletonMangroveGroveExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonDiagonAllayEvidence(
        Map<String, PhaseASingletonDiagonAllayExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonVillageBellEvidence(
        Map<String, PhaseASingletonVillageBellExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonLlamaBreedingEvidence(
        Map<String, PhaseASingletonLlamaBreedingExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonAxolotlMonumentEvidence(
        Map<String, PhaseASingletonAxolotlMonumentExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonMaximumResistanceEvidence(
        Map<String, PhaseASingletonMaximumResistanceExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonMiracleDrinkEvidence(
        Map<String, PhaseASingletonMiracleDrinkExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonAncientRestorationEvidence(
        Map<String, PhaseASingletonAncientRestorationExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptSingletonSilkTouchNestEvidence(
        Map<String, PhaseASingletonSilkTouchNestExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptBlockTagItemUsedOnBlockEvidence(
        Map<String, PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            var evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(), new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()), copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()), new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptRuntimeEvidence(Map<String, PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData> source) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptEntityTypeTagPlayerKilledEntityEvidence(
        Map<String, PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptEntityTypeTagStartedRidingEvidence(
        Map<String, PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptItemTagItemUsedOnBlockEvidence(
        Map<String, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptContainerEvidence(Map<String, PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData> source) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptItemTagEvidence(Map<String, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> source) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptEnchantmentEvidence(Map<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> source) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptExpansionDirect16Evidence(
        Map<String, PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptPlacedBlockEvidence(Map<String, PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData> source) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static Map<String, RuntimeEvidence> adaptTrimPatternRecipeCraftedEvidence(
        Map<String, PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData> source
    ) {
        Map<String, RuntimeEvidence> adapted = new LinkedHashMap<>();
        for (Map.Entry<String, PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData> entry : source.entrySet()) {
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData evidence = entry.getValue();
            adapted.put(entry.getKey(), new RuntimeEvidence(
                evidence.advancementId(),
                new TreeSet<>(evidence.greenCriteria()),
                copyCriteriaMap(evidence.criteriaByFamily()),
                copyCriteriaMap(evidence.criteriaBySource()),
                new LinkedHashSet<>(evidence.families()),
                new LinkedHashSet<>(evidence.sources())
            ));
        }
        return adapted;
    }

    static void mergeRuntimeEvidence(Map<String, RuntimeEvidence> merged, Map<String, RuntimeEvidence> incoming, String incomingLabel) {
        for (Map.Entry<String, RuntimeEvidence> entry : incoming.entrySet()) {
            RuntimeEvidence target = merged.computeIfAbsent(entry.getKey(), RuntimeEvidence::empty);
            mergeInto(target, entry.getValue(), incomingLabel);
        }
    }

    private static void mergeInto(RuntimeEvidence target, RuntimeEvidence incoming, String incomingLabel) {
        for (String criterion : incoming.greenCriteria()) {
            if (target.greenCriteria().contains(criterion)) {
                throw new IllegalStateException(
                    "Cross-source runtime evidence criterion collision for " + target.advancementId() + "#" + criterion
                        + " while merging " + incomingLabel
                        + "; existing families=" + target.families()
                        + ", existing sources=" + target.sources()
                        + ", incoming families=" + incoming.families()
                        + ", incoming sources=" + incoming.sources()
                );
            }
        }
        target.greenCriteria().addAll(incoming.greenCriteria());
        mergeCriteriaMaps(target.criteriaByFamily(), incoming.criteriaByFamily());
        mergeCriteriaMaps(target.criteriaBySource(), incoming.criteriaBySource());
        target.families().addAll(incoming.families());
        target.sources().addAll(incoming.sources());
    }

    private static Map<String, Set<String>> copyCriteriaMap(Map<String, Set<String>> source) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new TreeSet<>(entry.getValue()));
        }
        return copy;
    }

    private static void mergeCriteriaMaps(Map<String, Set<String>> target, Map<String, Set<String>> incoming) {
        for (Map.Entry<String, Set<String>> entry : incoming.entrySet()) {
            target.computeIfAbsent(entry.getKey(), ignored -> new TreeSet<>()).addAll(entry.getValue());
        }
    }

    private static RawAdvancement readRawAdvancement(Path projectRoot, String sourcePath) throws IOException {
        Path bacapZip = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final").resolve("bacap.zip");
        try (ZipFile zipFile = new ZipFile(bacapZip.toFile())) {
            ZipEntry entry = zipFile.getEntry(sourcePath);
            if (entry == null) {
                throw new IllegalStateException("Missing frozen advancement entry: " + sourcePath);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry); Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                return new RawAdvancement(JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
    }

    static List<List<String>> completionRequirements(JsonObject advancementJson) {
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
                requirements.add(group);
            }
            return requirements;
        }
        JsonObject criteria = advancementJson.getAsJsonObject("criteria");
        if (criteria != null) {
            for (Map.Entry<String, JsonElement> criterion : criteria.entrySet()) {
                requirements.add(List.of(criterion.getKey()));
            }
        }
        return requirements;
    }

    static boolean requirementsSatisfied(List<List<String>> requirements, Set<String> greenCriteria) {
        if (requirements.isEmpty()) {
            return false;
        }
        for (List<String> requirementGroup : requirements) {
            boolean satisfied = false;
            for (String criterion : requirementGroup) {
                if (greenCriteria.contains(criterion)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    private record RawAdvancement(JsonObject json) {
    }

    static record RuntimeEvidence(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> sources
    ) {
        static RuntimeEvidence empty(String advancementId) {
            return new RuntimeEvidence(
                advancementId,
                new TreeSet<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
            );
        }
    }

    private record RollupEntry(
        String id,
        String sourcePath,
        String staticStatus,
        RollupStatus advancementStatus,
        int criteriaCount,
        List<List<String>> completionRequirements,
        List<String> runtimeGreenCriteria,
        List<String> locationMovementCriteria,
        Set<String> runtimeEvidenceFamilies,
        Set<String> runtimeEvidenceSources,
        boolean requirementsSatisfied,
        String reason
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("staticStatus", staticStatus);
            json.addProperty("advancementStatus", advancementStatus.name());
            json.addProperty("criteriaCount", criteriaCount);
            json.addProperty("requirementsSatisfied", requirementsSatisfied);

            JsonArray requirementsJson = new JsonArray();
            for (List<String> group : completionRequirements) {
                JsonArray groupJson = new JsonArray();
                for (String criterion : group) {
                    groupJson.add(criterion);
                }
                requirementsJson.add(groupJson);
            }
            json.add("completionRequirements", requirementsJson);

            JsonArray runtimeGreenCriteriaJson = new JsonArray();
            for (String criterion : runtimeGreenCriteria) {
                runtimeGreenCriteriaJson.add(criterion);
            }
            json.add("runtimeGreenCriteria", runtimeGreenCriteriaJson);

            JsonArray locationMovementCriteriaJson = new JsonArray();
            for (String criterion : locationMovementCriteria) {
                locationMovementCriteriaJson.add(criterion);
            }
            json.add("locationMovementCriteria", locationMovementCriteriaJson);

            JsonArray familiesJson = new JsonArray();
            for (String family : runtimeEvidenceFamilies) {
                familiesJson.add(family);
            }
            json.add("runtimeEvidenceFamilies", familiesJson);

            JsonArray sourcesJson = new JsonArray();
            for (String source : runtimeEvidenceSources) {
                sourcesJson.add(source);
            }
            json.add("runtimeEvidenceSources", sourcesJson);
            json.addProperty("reason", reason);
            return json;
        }
    }

    private record Summary(
        int totalAdvancements,
        long totalCertified,
        long totalStrengthenedByRuntime,
        long runtimeCertified,
        long runtimePartial,
        long unresolvedOrDeferred,
        long staticFailUnresolved,
        long staticCertifiedWithRuntimeEvidence,
        Map<String, Integer> statusTotals,
        long uniqueRuntimeTouchedAdvancements
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("totalAdvancements", totalAdvancements);
            json.addProperty("totalCertified", totalCertified);
            json.addProperty("totalStrengthenedByRuntime", totalStrengthenedByRuntime);
            json.addProperty("runtimeCertified", runtimeCertified);
            json.addProperty("runtimePartial", runtimePartial);
            json.addProperty("unresolvedOrDeferred", unresolvedOrDeferred);
            json.addProperty("staticFailUnresolved", staticFailUnresolved);
            json.addProperty("staticCertifiedWithRuntimeEvidence", staticCertifiedWithRuntimeEvidence);
            json.addProperty("uniqueRuntimeTouchedAdvancements", uniqueRuntimeTouchedAdvancements);
            JsonObject totals = new JsonObject();
            for (Map.Entry<String, Integer> entry : statusTotals.entrySet()) {
                totals.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("statusTotals", totals);
            return json;
        }
    }

    enum RollupStatus {
        STATIC_CERTIFIED,
        RUNTIME_CERTIFIED,
        RUNTIME_PARTIAL,
        RUNTIME_DEFERRED,
        STATIC_FAIL_UNRESOLVED
    }
}


