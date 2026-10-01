package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PhaseAEnchantmentInventoryChangedExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "enchantment_inventory_changed_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "enchantment_inventory_changed_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_execution_evidence.json");
    public static final String SNAPSHOT = "phase_a_enchantment_inventory_changed_execution_evidence";
    public static final String CATALOG_SNAPSHOT = "phase_a_enchantment_inventory_changed_case_catalog";
    public static final String FAMILY = "ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED";
    public static final String GREEN = "GREEN";
    public static final String SOURCE = "PhaseAEnchantmentInventoryChangedGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_case_catalog.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAEnchantmentInventoryChangedExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        System.setProperty(PROJECT_ROOT_PROPERTY, projectRoot.toString());
        if (args.length > 0 && "promote".equals(args[0])) {
            promote(projectRoot);
            return;
        }
        if (args.length > 0 && "reset".equals(args[0])) {
            resetRun(projectRoot);
            return;
        }
        throw new IllegalArgumentException("Expected promote or reset command");
    }

    public static Path projectRoot() {
        String configuredRoot = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY + " for GameTest runtime");
        }
        return Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    public static void resetRun(Path projectRoot) throws IOException {
        RuntimeRunState runState = new RuntimeRunState(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            currentCatalogFingerprint(projectRoot),
            UUID.randomUUID().toString(),
            Instant.now().toString()
        );
        writeJson(projectRoot.resolve(RUN_STATE_ARTIFACT), runState.toJson());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runState).toJson());
    }

    public static void recordGreen(Path projectRoot, DiagnosticReceipt receipt) throws IOException {
        requireNonBlank(receipt.advancementId(), "advancementId");
        requireNonBlank(receipt.criterion(), "criterion");
        requireNonBlank(receipt.selectedItem(), "selectedItem");
        requireNonBlank(receipt.runResult(), "result");
        if (!GREEN.equals(receipt.runResult())) {
            throw new IllegalStateException("Expected GREEN diagnostic result");
        }
        if (!SOURCE.equals(receipt.source())) {
            throw new IllegalStateException("Expected source=" + SOURCE + " but got " + receipt.source());
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        String currentFingerprint = currentCatalogFingerprint(projectRoot);
        validateRunAlignment(runState, artifact, currentFingerprint);

        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition =
            CertifiedEnchantmentInventoryChangedCatalog.requiredCase(Identifier.parse(receipt.advancementId()), receipt.criterion());
        if (!AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
            throw new IllegalStateException("Cannot record runtime evidence for ineligible case " + receipt.key());
        }
        validateReceiptAgainstCase(caseDefinition, receipt.enchantments(), receipt.selectedItem());

        if (artifact.containsReceiptKey(receipt.key())) {
            throw new IllegalStateException("Duplicate runtime evidence receipt attempt: " + receipt.key());
        }
        RuntimeExecutionArtifact updatedArtifact = artifact.append(new RuntimeExecutionEntry(
            receipt.advancementId(),
            receipt.criterion(),
            FAMILY,
            receipt.selectedItem(),
            receipt.enchantments(),
            SOURCE,
            GREEN,
            receipt.inventoryBefore(),
            receipt.inventoryAfter(),
            receipt.itemEntityBefore(),
            receipt.itemEntityAfter(),
            receipt.ticksToPickup()
        ));
        validateArtifact(projectRoot, runState, updatedArtifact, false);
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), updatedArtifact.toJson());
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact, true);
        writeJson(projectRoot.resolve(PERSISTENT_ARTIFACT), artifact.toJson());
        return artifact;
    }

    public static String currentCatalogFingerprint(Path projectRoot) throws IOException {
        byte[] bytes = Files.readAllBytes(projectRoot.resolve(CATALOG_PATH));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static void validateReceiptAgainstCase(
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition,
        List<ConfiguredEnchantment> configuredEnchantments,
        String selectedItem
    ) {
        if (!caseDefinition.allowedItems().isEmpty() && !caseDefinition.allowedItems().contains(selectedItem)) {
            throw new IllegalStateException("selectedItem is not allowed for " + key(caseDefinition.advancementId().toString(), caseDefinition.criterion()));
        }
        Map<String, ConfiguredEnchantment> configuredBySelector = new LinkedHashMap<>();
        for (ConfiguredEnchantment enchantment : configuredEnchantments) {
            String selectorKey = enchantment.selector() + "|" + enchantment.storageType();
            if (configuredBySelector.putIfAbsent(selectorKey, enchantment) != null) {
                throw new IllegalStateException("Duplicate configured enchantment selector in receipt: " + selectorKey);
            }
        }
        for (CertifiedEnchantmentInventoryChangedCatalog.EnchantmentRequirement requirement : caseDefinition.enchantments()) {
            String selectorKey = requirement.selector() + "|" + requirement.storageType();
            ConfiguredEnchantment configured = configuredBySelector.get(selectorKey);
            if (configured == null) {
                throw new IllegalStateException("Missing configured enchantment for " + selectorKey);
            }
            int actualLevel = configured.level();
            if (requirement.minLevel() != null && actualLevel < requirement.minLevel()) {
                throw new IllegalStateException("Configured enchantment below min level for " + selectorKey);
            }
            if (requirement.maxLevel() != null && actualLevel > requirement.maxLevel()) {
                throw new IllegalStateException("Configured enchantment above max level for " + selectorKey);
            }
        }
        if (configuredEnchantments.size() != caseDefinition.enchantments().size()) {
            throw new IllegalStateException("Configured enchantment set does not exactly match catalog case");
        }
    }

    private static void validateRunAlignment(RuntimeRunState runState, RuntimeExecutionArtifact artifact, String currentFingerprint) {
        requireEquals(runState.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(runState.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(runState.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(runState.catalogFingerprint(), currentFingerprint, "catalogFingerprint");
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), currentFingerprint, "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
    }

    private static void validateArtifact(Path projectRoot, RuntimeRunState runState, RuntimeExecutionArtifact artifact, boolean requireExactCoverage) throws IOException {
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), runState.catalogFingerprint(), "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Map<String, CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> catalogCases = loadCurrentCatalog();
        Set<String> expectedSupportedKeys = supportedCatalogKeys(catalogCases.values());
        Set<String> actualKeys = new LinkedHashSet<>();
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            requireEquals(entry.result(), GREEN, "result");
            requireEquals(entry.family(), FAMILY, "family");
            requireEquals(entry.source(), SOURCE, "source");
            requireNonBlank(entry.selectedItem(), "selectedItem");
            String receiptKey = key(entry.advancementId(), entry.criterion());
            if (!actualKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
            }
            CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition = catalogCases.get(receiptKey);
            if (caseDefinition == null) {
                throw new IllegalStateException("Runtime evidence references unsupported or unknown catalog case: " + receiptKey);
            }
            if (!AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
                throw new IllegalStateException("Runtime evidence references deferred catalog case: " + receiptKey);
            }
            validateReceiptAgainstCase(caseDefinition, entry.enchantments(), entry.selectedItem());
        }
        if (requireExactCoverage && !actualKeys.equals(expectedSupportedKeys)) {
            throw new IllegalStateException("Runtime evidence receipt keys do not exactly match current SUPPORTED catalog keys");
        }
    }

    private static RuntimeExecutionArtifact emptyArtifact(RuntimeRunState runState) {
        return new RuntimeExecutionArtifact(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            runState.catalogFingerprint(),
            runState.runId(),
            Instant.EPOCH.toString(),
            List.of()
        );
    }

    private static RuntimeRunState requireRunState(Path projectRoot) throws IOException {
        Path runStatePath = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (!Files.exists(runStatePath)) {
            throw new IllegalStateException("Missing active enchantment runtime RUN_STATE artifact");
        }
        JsonObject json = readJson(runStatePath);
        requireString(json, "snapshot", SNAPSHOT);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(json, "catalogFingerprint", currentCatalogFingerprint(projectRoot));
        return RuntimeRunState.fromJson(json);
    }

    private static RuntimeExecutionArtifact loadTemporaryArtifact(Path projectRoot, RuntimeRunState runState) throws IOException {
        Path artifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(artifactPath)) {
            return emptyArtifact(runState);
        }
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(artifactPath));
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    private static Map<String, CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> loadCurrentCatalog() {
        Map<String, CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> supportedCases = new LinkedHashMap<>();
        for (CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition : CertifiedEnchantmentInventoryChangedCatalog.allCases()) {
            supportedCases.put(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()), caseDefinition);
        }
        return supportedCases;
    }

    private static Set<String> supportedCatalogKeys(Collection<CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> caseDefinitions) {
        Set<String> keys = new LinkedHashSet<>();
        for (CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition : caseDefinitions) {
            if (AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
                keys.add(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()));
            }
        }
        return keys;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void requireString(JsonObject json, String key, String expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        requireEquals(json.get(key).getAsString(), expectedValue, key);
    }

    private static void requireEquals(String actualValue, String expectedValue, String fieldName) {
        if (!expectedValue.equals(actualValue)) {
            throw new IllegalStateException("Expected " + fieldName + "=" + expectedValue + " but got " + actualValue);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + fieldName);
        }
    }

    private static String key(String advancementId, String criterion) {
        return advancementId + "#" + criterion;
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

    public record DiagnosticReceipt(
        String advancementId,
        String criterion,
        String selectedItem,
        List<ConfiguredEnchantment> enchantments,
        String source,
        String runResult,
        String inventoryBefore,
        String inventoryAfter,
        String itemEntityBefore,
        String itemEntityAfter,
        int ticksToPickup
    ) {
        String key() {
            return advancementId + "#" + criterion;
        }
    }

    public record ConfiguredEnchantment(String selector, int level, String storageType) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("selector", selector);
            json.addProperty("level", level);
            json.addProperty("storageType", storageType);
            return json;
        }

        static ConfiguredEnchantment fromJson(JsonObject json) {
            return new ConfiguredEnchantment(
                json.get("selector").getAsString(),
                json.get("level").getAsInt(),
                json.get("storageType").getAsString()
            );
        }
    }

    public record RuntimeRunState(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String generatedAt
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("generatedAt", generatedAt);
            return json;
        }

        static RuntimeRunState fromJson(JsonObject json) {
            return new RuntimeRunState(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("catalogFingerprint").getAsString(),
                json.get("runId").getAsString(),
                json.get("generatedAt").getAsString()
            );
        }
    }

    public record RuntimeExecutionArtifact(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<RuntimeExecutionEntry> entries
    ) {
        boolean containsReceiptKey(String receiptKey) {
            for (RuntimeExecutionEntry entry : entries) {
                if (key(entry.advancementId(), entry.criterion()).equals(receiptKey)) {
                    return true;
                }
            }
            return false;
        }

        RuntimeExecutionArtifact append(RuntimeExecutionEntry entry) {
            List<RuntimeExecutionEntry> updated = new ArrayList<>(entries);
            updated.add(entry);
            updated.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion));
            return new RuntimeExecutionArtifact(snapshot, minecraftVersion, compatibilityMarker, catalogFingerprint, runId, Instant.now().toString(), List.copyOf(updated));
        }

        static RuntimeExecutionArtifact fromJson(JsonObject json) {
            List<RuntimeExecutionEntry> entries = new ArrayList<>();
            JsonArray entriesJson = json.getAsJsonArray("entries");
            if (entriesJson != null) {
                for (JsonElement element : entriesJson) {
                    JsonObject entryJson = element.getAsJsonObject();
                    List<ConfiguredEnchantment> enchantments = new ArrayList<>();
                    JsonArray enchantmentsJson = entryJson.getAsJsonArray("enchantments");
                    if (enchantmentsJson != null) {
                        for (JsonElement enchantmentElement : enchantmentsJson) {
                            enchantments.add(ConfiguredEnchantment.fromJson(enchantmentElement.getAsJsonObject()));
                        }
                    }
                    entries.add(new RuntimeExecutionEntry(
                        entryJson.get("advancementId").getAsString(),
                        entryJson.get("criterion").getAsString(),
                        entryJson.get("family").getAsString(),
                        entryJson.get("selectedItem").getAsString(),
                        List.copyOf(enchantments),
                        entryJson.get("source").getAsString(),
                        entryJson.get("result").getAsString(),
                        entryJson.get("inventoryBefore").getAsString(),
                        entryJson.get("inventoryAfter").getAsString(),
                        entryJson.get("itemEntityBefore").getAsString(),
                        entryJson.get("itemEntityAfter").getAsString(),
                        entryJson.get("ticksToPickup").getAsInt()
                    ));
                }
            }
            return new RuntimeExecutionArtifact(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("catalogFingerprint").getAsString(),
                json.get("runId").getAsString(),
                json.get("generatedAt").getAsString(),
                List.copyOf(entries)
            );
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("generatedAt", generatedAt);
            JsonArray entriesJson = new JsonArray();
            for (RuntimeExecutionEntry entry : entries) {
                JsonObject entryJson = new JsonObject();
                entryJson.addProperty("advancementId", entry.advancementId());
                entryJson.addProperty("criterion", entry.criterion());
                entryJson.addProperty("family", entry.family());
                entryJson.addProperty("selectedItem", entry.selectedItem());
                JsonArray enchantmentsJson = new JsonArray();
                for (ConfiguredEnchantment enchantment : entry.enchantments()) {
                    enchantmentsJson.add(enchantment.toJson());
                }
                entryJson.add("enchantments", enchantmentsJson);
                entryJson.addProperty("source", entry.source());
                entryJson.addProperty("result", entry.result());
                entryJson.addProperty("inventoryBefore", entry.inventoryBefore());
                entryJson.addProperty("inventoryAfter", entry.inventoryAfter());
                entryJson.addProperty("itemEntityBefore", entry.itemEntityBefore());
                entryJson.addProperty("itemEntityAfter", entry.itemEntityAfter());
                entryJson.addProperty("ticksToPickup", entry.ticksToPickup());
                entriesJson.add(entryJson);
            }
            json.add("entries", entriesJson);
            return json;
        }
    }

    public record RuntimeExecutionEntry(
        String advancementId,
        String criterion,
        String family,
        String selectedItem,
        List<ConfiguredEnchantment> enchantments,
        String source,
        String result,
        String inventoryBefore,
        String inventoryAfter,
        String itemEntityBefore,
        String itemEntityAfter,
        int ticksToPickup
    ) {
    }
}
