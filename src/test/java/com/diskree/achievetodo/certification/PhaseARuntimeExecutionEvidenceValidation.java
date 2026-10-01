package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class PhaseARuntimeExecutionEvidenceValidation {
    static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "runtime_execution_evidence.json");
    static final Path MATRIX_PATH = Path.of("src", "test", "resources", "phase_a_certification", "location_movement_matrix.json");
    static final String SNAPSHOT = "phase_a_runtime_execution_evidence";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String AUTOMATED_GREEN = "AUTOMATED_GREEN";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    static final String AUTOMATION_DEFERRED = "DEFERRED";
    static final String LOCATION_MOVEMENT_FAMILY = "LOCATION_MOVEMENT";
    static final String STRUCTURE_ONLY_FAMILY = "STRUCTURE_ONLY";

    private PhaseARuntimeExecutionEvidenceValidation() {
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        Path evidencePath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        if (!Files.exists(evidencePath)) {
            return evidence;
        }
        Path matrixPath = projectRoot.resolve(MATRIX_PATH);
        String matrixFingerprint = fingerprint(matrixPath);
        Map<String, MatrixCase> matrix = loadMatrix(matrixPath);
        JsonObject root = readJson(evidencePath);
        requireString(root, "snapshot", SNAPSHOT);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "matrixFingerprint", matrixFingerprint);
        JsonArray entries = root.getAsJsonArray("entries");
        if (entries == null) {
            return evidence;
        }
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            String advancementId = entry.get("advancementId").getAsString();
            String criterion = entry.get("criterion").getAsString();
            String family = entry.get("family").getAsString();
            MatrixCase matrixCase = matrix.get(key(advancementId, criterion, family));
            if (matrixCase == null) {
                throw new IllegalStateException("Runtime evidence references missing matrix case: " + advancementId + "#" + criterion + "#" + family);
            }
            requireString(entry, "result", GREEN);
            requireString(entry, "matrixCertificationStatus", matrixCase.certificationStatus());
            if (!isAutomationEligible(matrixCase.automationEligibility())) {
                throw new IllegalStateException("Runtime evidence references ineligible matrix case: " + advancementId + "#" + criterion + "#" + family);
            }
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            runtimeEvidence.greenCriteria().add(criterion);
            runtimeEvidence.families().add(family);
            runtimeEvidence.sources().add(entry.get("source").getAsString());
            runtimeEvidence.criteriaByFamily()
                .computeIfAbsent(family, ignored -> new TreeSet<>())
                .add(criterion);
            runtimeEvidence.criteriaBySource()
                .computeIfAbsent(entry.get("source").getAsString(), ignored -> new TreeSet<>())
                .add(criterion);
        }
        return evidence;
    }

    static Map<String, MatrixCase> loadMatrix(Path matrixPath) throws IOException {
        Map<String, MatrixCase> matrix = new LinkedHashMap<>();
        JsonObject root = readJson(matrixPath);
        JsonArray cases = root.getAsJsonArray("cases");
        for (JsonElement element : cases) {
            JsonObject caseJson = element.getAsJsonObject();
            String structureLocationType = caseJson.has("structureLocationType") && !caseJson.get("structureLocationType").isJsonNull()
                ? caseJson.get("structureLocationType").getAsString()
                : null;
            String family = familyFor(caseJson.get("category").getAsString(), structureLocationType);
            if (family == null) {
                continue;
            }
            String advancementId = caseJson.get("advancementId").getAsString();
            String criterion = caseJson.get("criterion").getAsString();
            matrix.putIfAbsent(key(advancementId, criterion, family), new MatrixCase(
                advancementId,
                criterion,
                family,
                caseJson.get("automationEligibility").getAsString(),
                caseJson.get("certificationStatus").getAsString()
            ));
        }
        return matrix;
    }

    static String fingerprint(Path matrixPath) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(matrixPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    static record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily, Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) {
        static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(advancementId, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }

    static String familyFor(String category, String structureLocationType) {
        if ("BIOME_ONLY".equals(category) || "BIOME_SET".equals(category)) {
            return LOCATION_MOVEMENT_FAMILY;
        }
        if ("STRUCTURE_LOCATION".equals(category) && "STRUCTURE_ONLY".equals(structureLocationType)) {
            return STRUCTURE_ONLY_FAMILY;
        }
        return null;
    }

    static boolean isAutomationEligible(String automationEligibility) {
        return AUTOMATION_SUPPORTED.equals(automationEligibility);
    }

    static record MatrixCase(String advancementId, String criterion, String family, String automationEligibility, String certificationStatus) {
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void requireString(JsonObject json, String key, String expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (!expectedValue.equals(json.get(key).getAsString())) {
            throw new IllegalStateException("Expected " + key + "=" + expectedValue + " but got " + json.get(key).getAsString());
        }
    }

    private static String key(String advancementId, String criterion, String family) {
        return advancementId + "#" + criterion + "#" + family;
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
}
