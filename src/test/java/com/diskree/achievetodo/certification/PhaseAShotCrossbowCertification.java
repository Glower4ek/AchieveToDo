package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Deterministic frozen-definition catalog for the SHOT_CROSSBOW frontier family. */
public final class PhaseAShotCrossbowCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "shot_crossbow_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_shot_crossbow_case_catalog";
    public static final String FAMILY = "SHOT_CROSSBOW";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String TRIGGER = "minecraft:shot_crossbow";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String SEMANTICS_SOURCE = "frozenBacap";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleUseItem";
    public static final String PACKET_PATH =
        "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
            + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW";

    public static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:enchanting/machine_bow#shot_crossbow",
        "blazeandcave:enchanting/shotbow#shot_crossbow"
    );

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<ExpectedCase> EXPECTED_CASES = List.of(
        new ExpectedCase(
            "blazeandcave:enchanting/machine_bow",
            "data/blazeandcave/advancement/enchanting/machine_bow.json",
            "shot_crossbow",
            "minecraft:quick_charge",
            "min",
            3,
            "QUICK_CHARGE",
            1,
            1,
            1,
            1,
            "fd11c038282218fc928af42a39f4996df995fc799b80f69b1ddbf38a592e9e42"
        ),
        new ExpectedCase(
            "blazeandcave:enchanting/shotbow",
            "data/blazeandcave/advancement/enchanting/shotbow.json",
            "shot_crossbow",
            "minecraft:multishot",
            "present",
            1,
            "MULTISHOT",
            3,
            3,
            1,
            3,
            "3fe10408005e0a41c1e7274041ab2bd6ac52dc262631bd5017c1e010bf4a012d"
        )
    );

    private PhaseAShotCrossbowCertification() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected project root path");
        }
        writeSnapshot(Path.of(args[0]).toAbsolutePath().normalize());
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Path output = projectRoot.resolve(SNAPSHOT);
        Files.createDirectories(output.getParent());
        Files.writeString(output, generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        Path bacap = projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE);
        verifyFrozenBacap(bacap);
        List<CaseDefinition> cases = deriveCasesFromZip(bacap);
        assertExactCatalog(cases);

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", FAMILY);
        root.addProperty("canonicalAdvancementCount", PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        root.addProperty("semanticsSource", SEMANTICS_SOURCE);
        root.addProperty("frozenBacapSha1", FROZEN_BACAP_SHA1);
        root.addProperty("frozenBacapSha256", FROZEN_BACAP_SHA256);
        root.addProperty("sourceArtifactClassification", SOURCE_ARTIFACT_CLASSIFICATION);
        root.addProperty("runtimeBoundary", BOUNDARY);
        root.addProperty("runtimePacketPath", PACKET_PATH);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueKeys", cases.stream().map(CaseDefinition::key).distinct().count());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("requirementGroups", cases.size());
        summary.addProperty("automationSupported", cases.stream().filter(c -> SUPPORTED.equals(c.automationEligibility())).count());
        summary.addProperty("automationDeferred", cases.stream().filter(c -> !SUPPORTED.equals(c.automationEligibility())).count());
        JsonObject firingModes = new JsonObject();
        Map<String, Long> modeCounts = new LinkedHashMap<>();
        for (CaseDefinition definition : cases) {
            modeCounts.merge(definition.firingMode(), 1L, Long::sum);
        }
        modeCounts.forEach(firingModes::addProperty);
        summary.add("firingModes", firingModes);
        root.add("summary", summary);

        JsonArray casesJson = new JsonArray();
        cases.forEach(definition -> casesJson.add(definition.toJson()));
        root.add("cases", casesJson);
        return GSON.toJson(root) + "\n";
    }

    public static List<CaseDefinition> deriveCases(Path projectRoot) throws IOException {
        Path bacap = projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE);
        verifyFrozenBacap(bacap);
        return deriveCasesFromZip(bacap);
    }

    private static List<CaseDefinition> deriveCasesFromZip(Path bacap) throws IOException {
        List<CaseDefinition> cases = new ArrayList<>();
        try (ZipFile zip = new ZipFile(bacap.toFile(), StandardCharsets.UTF_8)) {
            for (ExpectedCase expected : EXPECTED_CASES) {
                ZipEntry advancementEntry = zip.getEntry(expected.sourcePath());
                if (advancementEntry == null) {
                    throw new IllegalStateException("Missing frozen advancement " + expected.advancementId());
                }
                byte[] sourceBytes;
                try (InputStream input = zip.getInputStream(advancementEntry)) {
                    sourceBytes = input.readAllBytes();
                }
                String sourceSha256 = sha256(sourceBytes);
                if (!expected.sourceSha256().equals(sourceSha256)) {
                    throw new IllegalStateException("Frozen source hash changed for " + expected.advancementId()
                        + ": expected=" + expected.sourceSha256() + ", actual=" + sourceSha256);
                }
                JsonObject advancement;
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(sourceBytes), StandardCharsets.UTF_8)) {
                    advancement = JsonParser.parseReader(reader).getAsJsonObject();
                }
                cases.add(deriveCase(expected, advancement, sourceSha256));
            }
        }
        return List.copyOf(cases);
    }

    private static CaseDefinition deriveCase(ExpectedCase expected, JsonObject advancement, String sourceSha256) {
        JsonObject criteria = requiredObject(advancement, "criteria", expected.key());
        if (criteria.size() != 1 || !criteria.has(expected.criterion())) {
            throw new IllegalStateException("Expected exactly criterion " + expected.criterion()
                + " for " + expected.key());
        }
        JsonObject criterion = criteria.getAsJsonObject(expected.criterion());
        requireString(criterion, "trigger", TRIGGER, expected.key());
        JsonObject conditions = requiredObject(criterion, "conditions", expected.key());
        if (conditions.size() != 1 || !conditions.has("item")) {
            throw new IllegalStateException("Expected only the item condition for " + expected.key());
        }
        JsonObject item = requiredObject(conditions, "item", expected.key());
        JsonArray items = requiredArray(item, "items", expected.key());
        if (items.size() != 1 || !"minecraft:crossbow".equals(requiredStringValue(items.get(0), expected.key()))) {
            throw new IllegalStateException("Expected the crossbow item predicate for " + expected.key());
        }
        JsonObject predicates = requiredObject(item, "predicates", expected.key());
        if (predicates.size() != 1 || !predicates.has("enchantments")) {
            throw new IllegalStateException("Expected only the enchantment predicate for " + expected.key());
        }
        JsonArray enchantments = requiredArray(predicates, "enchantments", expected.key());
        if (enchantments.size() != 1 || !enchantments.get(0).isJsonObject()) {
            throw new IllegalStateException("Expected exactly one enchantment predicate for " + expected.key());
        }
        JsonObject enchantment = enchantments.get(0).getAsJsonObject();
        requireString(enchantment, "enchantments", expected.enchantment(), expected.key());
        if ("min".equals(expected.levelRule())) {
            JsonObject levels = requiredObject(enchantment, "levels", expected.key());
            if (levels.size() != 1) {
                throw new IllegalStateException("Expected exactly one minimum level for " + expected.key());
            }
            requireInt(levels, "min", expected.enchantmentLevel(), expected.key());
        } else if (enchantment.has("levels")) {
            throw new IllegalStateException("Unexpected level constraint for " + expected.key());
        }
        verifySingletonRequirements(advancement, expected);
        return new CaseDefinition(
            expected.advancementId(),
            expected.sourcePath(),
            expected.criterion(),
            0,
            1,
            TRIGGER,
            "minecraft:crossbow",
            expected.enchantment(),
            expected.levelRule(),
            expected.enchantmentLevel(),
            expected.firingMode(),
            expected.loadedProjectiles(),
            expected.firedProjectiles(),
            expected.ammoConsumed(),
            expected.durabilityUse(),
            BOUNDARY,
            PACKET_PATH,
            SUPPORTED,
            SEMANTICS_SOURCE,
            SOURCE_ARTIFACT_CLASSIFICATION,
            sourceSha256
        );
    }

    private static void verifySingletonRequirements(JsonObject advancement, ExpectedCase expected) {
        if (!advancement.has("requirements")) {
            return;
        }
        JsonArray requirements = requiredArray(advancement, "requirements", expected.key());
        if (requirements.size() != 1 || !requirements.get(0).isJsonArray()) {
            throw new IllegalStateException("Expected one requirement group for " + expected.key());
        }
        JsonArray group = requirements.get(0).getAsJsonArray();
        if (group.size() != 1 || !expected.criterion().equals(requiredStringValue(group.get(0), expected.key()))) {
            throw new IllegalStateException("Requirement group is not the singleton criterion for " + expected.key());
        }
    }

    private static void assertExactCatalog(List<CaseDefinition> cases) {
        List<String> actual = cases.stream().map(CaseDefinition::key).toList();
        if (!actual.equals(EXPECTED_KEYS)) {
            Set<String> missing = new LinkedHashSet<>(EXPECTED_KEYS);
            missing.removeAll(actual);
            Set<String> extras = new LinkedHashSet<>(actual);
            extras.removeAll(EXPECTED_KEYS);
            throw new IllegalStateException("SHOT_CROSSBOW catalog mismatch | missing=" + missing
                + " | extras=" + extras + " | order=" + actual);
        }
        if (cases.size() != 2 || actual.stream().distinct().count() != 2
            || cases.stream().map(CaseDefinition::advancementId).distinct().count() != 2
            || cases.stream().anyMatch(c -> c.requirementGroupIndex() != 0 || c.requirementGroupCount() != 1)) {
            throw new IllegalStateException("SHOT_CROSSBOW catalog must contain two singleton cases");
        }
    }

    private static void verifyFrozenBacap(Path bacap) throws IOException {
        if (!Files.isRegularFile(bacap)) {
            throw new IllegalStateException("Missing frozen BACAP source: " + bacap);
        }
        requireHash(bacap, "SHA-1", FROZEN_BACAP_SHA1);
        requireHash(bacap, "SHA-256", FROZEN_BACAP_SHA256);
    }

    private static void requireHash(Path path, String algorithm, String expected) throws IOException {
        try {
            String actual = hex(MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(path)));
            if (!expected.equals(actual)) {
                throw new IllegalStateException("Frozen BACAP " + algorithm + " mismatch: " + actual);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest algorithm " + algorithm, e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in " + context);
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in " + context);
        }
        return value.getAsJsonArray();
    }

    private static String requiredStringValue(JsonElement value, String context) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank string in " + context);
        }
        return value.getAsString();
    }

    private static String requiredString(JsonObject parent, String key, String context) {
        return requiredStringValue(parent.get(key), context + "." + key);
    }

    private static void requireString(JsonObject parent, String key, String expected, String context) {
        if (!expected.equals(requiredString(parent, key, context))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in " + context);
        }
    }

    private static void requireInt(JsonObject parent, String key, int expected, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsInt() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in " + context);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    private record ExpectedCase(
        String advancementId,
        String sourcePath,
        String criterion,
        String enchantment,
        String levelRule,
        int enchantmentLevel,
        String firingMode,
        int loadedProjectiles,
        int firedProjectiles,
        int ammoConsumed,
        int durabilityUse,
        String sourceSha256
    ) {
        String key() {
            return advancementId + "#" + criterion;
        }
    }

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        int requirementGroupCount,
        String trigger,
        String selectedItem,
        String enchantment,
        String levelRule,
        int enchantmentLevel,
        String firingMode,
        int expectedLoadedProjectiles,
        int expectedFiredProjectiles,
        int expectedAmmoConsumed,
        int expectedDurabilityUse,
        String boundary,
        String packetPath,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification,
        String sourceJsonSha256
    ) {
        public String key() {
            return advancementId + "#" + criterion;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("criterion", criterion);
            json.addProperty("requirementGroupIndex", requirementGroupIndex);
            json.addProperty("requirementGroupCount", requirementGroupCount);
            json.addProperty("trigger", trigger);
            json.addProperty("selectedItem", selectedItem);
            JsonObject enchantmentPredicate = new JsonObject();
            enchantmentPredicate.addProperty("enchantment", enchantment);
            enchantmentPredicate.addProperty("levelRule", levelRule);
            enchantmentPredicate.addProperty("level", enchantmentLevel);
            json.add("enchantmentPredicate", enchantmentPredicate);
            json.addProperty("firingMode", firingMode);
            json.addProperty("expectedLoadedProjectileCount", expectedLoadedProjectiles);
            json.addProperty("expectedFiredProjectileCount", expectedFiredProjectiles);
            json.addProperty("expectedAmmoConsumed", expectedAmmoConsumed);
            json.addProperty("expectedDurabilityUse", expectedDurabilityUse);
            json.addProperty("boundary", boundary);
            json.addProperty("packetPath", packetPath);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("semanticsSource", semanticsSource);
            json.addProperty("sourceArtifactClassification", sourceArtifactClassification);
            json.addProperty("sourceJsonSha256", sourceJsonSha256);
            return json;
        }
    }
}
