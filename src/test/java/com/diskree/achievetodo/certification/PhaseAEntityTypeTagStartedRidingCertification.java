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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Frozen-definition catalog for the accepted ENTITY_TYPE_TAG_STARTED_RIDING
 * frontier family.  The five expected records are deliberately explicit: a
 * change to any trigger, predicate, tag, or requirement group fails closed.
 */
public final class PhaseAEntityTypeTagStartedRidingCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_started_riding_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_entity_type_tag_started_riding_case_catalog";
    public static final String FAMILY = "ENTITY_TYPE_TAG_STARTED_RIDING";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String TRIGGER = "minecraft:started_riding";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String SEMANTICS_SOURCE = "frozenBacap";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";

    public static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:animal/swine_sailing#swine_sailing",
        "blazeandcave:biomes/boaty_mcboatface#boat",
        "blazeandcave:biomes/cargo_carrier#boat",
        "blazeandcave:nether/jenga#jenga",
        "minecraft:husbandry/ride_a_boat_with_a_goat#ride_a_boat_with_a_goat"
    );

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<ExpectedCase> EXPECTED_CASES = List.of(
        new ExpectedCase(
            "blazeandcave:animal/swine_sailing",
            "data/blazeandcave/advancement/animal/swine_sailing.json",
            "swine_sailing",
            "blazeandcave:all_boats",
            "minecraft:oak_boat",
            "minecraft:pig",
            List.of("minecraft:pig", "#blazeandcave:all_boats", "minecraft:minecart"),
            "",
            "START_RIDING_PIG_ON_BOAT_IN_MINECART"
        ),
        new ExpectedCase(
            "blazeandcave:biomes/boaty_mcboatface",
            "data/blazeandcave/advancement/biomes/boaty_mcboatface.json",
            "boat",
            "blazeandcave:all_boats",
            "minecraft:oak_boat",
            "minecraft:oak_boat",
            List.of("#blazeandcave:all_boats"),
            "",
            "START_RIDING_BOAT"
        ),
        new ExpectedCase(
            "blazeandcave:biomes/cargo_carrier",
            "data/blazeandcave/advancement/biomes/cargo_carrier.json",
            "boat",
            "blazeandcave:chest_boats",
            "minecraft:oak_chest_boat",
            "minecraft:oak_chest_boat",
            List.of("#blazeandcave:chest_boats"),
            "",
            "START_RIDING_CHEST_BOAT"
        ),
        new ExpectedCase(
            "blazeandcave:nether/jenga",
            "data/blazeandcave/advancement/nether/jenga.json",
            "jenga",
            "blazeandcave:all_boats",
            "minecraft:oak_boat",
            "minecraft:strider",
            List.of("minecraft:strider", "minecraft:strider", "#blazeandcave:all_boats", "minecraft:minecart"),
            "",
            "START_RIDING_STRIDER_STACK_ON_BOAT_IN_MINECART"
        ),
        new ExpectedCase(
            "minecraft:husbandry/ride_a_boat_with_a_goat",
            "data/minecraft/advancement/husbandry/ride_a_boat_with_a_goat.json",
            "ride_a_boat_with_a_goat",
            "minecraft:boat",
            "minecraft:oak_boat",
            "minecraft:oak_boat",
            List.of("#minecraft:boat"),
            "minecraft:goat",
            "START_RIDING_BOAT_WITH_GOAT"
        )
    );

    private PhaseAEntityTypeTagStartedRidingCertification() {
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

        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", FAMILY);
        root.addProperty("canonicalAdvancementCount", 1152);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        root.addProperty("semanticsSource", SEMANTICS_SOURCE);
        root.addProperty("frozenBacapSha1", FROZEN_BACAP_SHA1);
        root.addProperty("frozenBacapSha256", FROZEN_BACAP_SHA256);
        root.addProperty("sourceArtifactClassification", SOURCE_ARTIFACT_CLASSIFICATION);

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueKeys", cases.stream().map(CaseDefinition::key).distinct().count());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("requirementGroups", cases.size());
        summary.addProperty("automationSupported", cases.stream().filter(c -> SUPPORTED.equals(c.automationEligibility())).count());
        summary.addProperty("automationDeferred", 0);
        root.add("summary", summary);

        JsonArray casesJson = new JsonArray();
        cases.forEach(c -> casesJson.add(c.toJson()));
        root.add("cases", casesJson);
        return GSON.toJson(root) + "\n";
    }

    public static List<CaseDefinition> deriveCases(Path projectRoot) throws IOException {
        return deriveCasesFromZip(projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE));
    }

    private static List<CaseDefinition> deriveCasesFromZip(Path bacap) throws IOException {
        List<CaseDefinition> cases = new ArrayList<>();
        try (ZipFile zip = new ZipFile(bacap.toFile(), StandardCharsets.UTF_8)) {
            for (int index = 0; index < EXPECTED_CASES.size(); index++) {
                ExpectedCase expected = EXPECTED_CASES.get(index);
                ZipEntry entry = zip.getEntry(expected.sourcePath());
                if (entry == null) {
                    throw new IllegalStateException("Missing frozen advancement " + expected.advancementId());
                }
                byte[] bytes;
                try (InputStream input = zip.getInputStream(entry)) {
                    bytes = input.readAllBytes();
                }
                JsonObject advancement;
                try (Reader reader = new InputStreamReader(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                    advancement = JsonParser.parseReader(reader).getAsJsonObject();
                }
                cases.add(deriveCase(expected, advancement));
            }
        }
        assertExactCatalog(cases);
        return List.copyOf(cases);
    }

    private static CaseDefinition deriveCase(ExpectedCase expected, JsonObject advancement) {
        JsonObject criteria = requiredObject(advancement, "criteria", expected.advancementId());
        if (criteria.size() != 1 || !criteria.has(expected.criterion())) {
            throw new IllegalStateException("Expected exactly criterion " + expected.criterion() + " for " + expected.advancementId());
        }
        JsonObject criterion = criteria.getAsJsonObject(expected.criterion());
        requireString(criterion, "trigger", TRIGGER, expected.key());
        JsonObject conditions = requiredObject(criterion, "conditions", expected.key());
        JsonArray playerConditions = requiredArray(conditions, "player", expected.key());
        if (playerConditions.size() != 1) {
            throw new IllegalStateException("Expected one player condition for " + expected.key());
        }
        JsonObject playerCondition = playerConditions.get(0).getAsJsonObject();
        requireString(playerCondition, "condition", "minecraft:entity_properties", expected.key());
        requireString(playerCondition, "entity", "this", expected.key());
        JsonObject predicate = requiredObject(playerCondition, "predicate", expected.key());
        JsonObject vehicle = requiredObject(predicate, "vehicle", expected.key());
        List<String> vehicleChain = parseVehicleChain(vehicle, expected.key());
        if (!vehicleChain.equals(expected.vehiclePredicateChain())) {
            throw new IllegalStateException("Started-riding vehicle predicate changed for " + expected.key()
                + " | expected=" + expected.vehiclePredicateChain() + " | actual=" + vehicleChain);
        }
        String tag = vehicleChain.stream()
            .filter(value -> value.startsWith("#"))
            .map(value -> value.substring(1))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Expected an entity-type tag predicate for " + expected.key()));
        if (!expected.entityTypeTag().equals(tag)) {
            throw new IllegalStateException("Unexpected entity-type tag for " + expected.key()
                + " | expected=" + expected.entityTypeTag() + " | actual=" + tag);
        }

        String passenger = extractPassengerType(vehicle, expected.key());
        if (!expected.requiredPassengerType().equals(passenger)) {
            throw new IllegalStateException("Unexpected passenger predicate for " + expected.key()
                + " | expected=" + expected.requiredPassengerType() + " | actual=" + passenger);
        }
        verifySingletonRequirements(advancement, expected);
        return new CaseDefinition(
            expected.advancementId(),
            expected.sourcePath(),
            expected.criterion(),
            0,
            TRIGGER,
            expected.entityTypeTag(),
            expected.selectedEntityType(),
            expected.directVehicleType(),
            vehicleChain,
            passenger,
            expected.action(),
            SUPPORTED,
            SEMANTICS_SOURCE,
            SOURCE_ARTIFACT_CLASSIFICATION
        );
    }

    private static List<String> parseVehicleChain(JsonObject vehicle, String key) {
        List<String> chain = new ArrayList<>();
        JsonObject current = vehicle;
        while (true) {
            JsonElement type = current.get("type");
            if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException("Missing string vehicle type in " + key);
            }
            String value = type.getAsString();
            if (!value.contains(":")) {
                value = (value.startsWith("#") ? "#" : "") + "minecraft:" + (value.startsWith("#") ? value.substring(1) : value);
            }
            chain.add(value);
            JsonElement nested = current.get("vehicle");
            if (nested == null) {
                break;
            }
            if (!nested.isJsonObject()) {
                throw new IllegalStateException("Vehicle predicate is not an object in " + key);
            }
            current = nested.getAsJsonObject();
        }
        return List.copyOf(chain);
    }

    private static String extractPassengerType(JsonObject vehicle, String key) {
        JsonElement passenger = vehicle.get("passenger");
        if (passenger == null) {
            return "";
        }
        if (!passenger.isJsonObject()) {
            throw new IllegalStateException("Passenger predicate is not an object in " + key);
        }
        JsonElement type = passenger.getAsJsonObject().get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException("Passenger predicate has no type in " + key);
        }
        String value = type.getAsString();
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private static void verifySingletonRequirements(JsonObject advancement, ExpectedCase expected) {
        JsonElement requirementsElement = advancement.get("requirements");
        if (requirementsElement == null || requirementsElement.isJsonNull()) {
            return;
        }
        if (!requirementsElement.isJsonArray()) {
            throw new IllegalStateException("requirements is not an array for " + expected.advancementId());
        }
        JsonArray requirements = requirementsElement.getAsJsonArray();
        if (requirements.size() != 1 || !requirements.get(0).isJsonArray()) {
            throw new IllegalStateException("Expected one requirement group for " + expected.advancementId());
        }
        JsonArray group = requirements.get(0).getAsJsonArray();
        if (group.size() != 1 || !expected.criterion().equals(group.get(0).getAsString())) {
            throw new IllegalStateException("Requirement group 0 is not the accepted singleton for " + expected.advancementId());
        }
    }

    private static void assertExactCatalog(List<CaseDefinition> cases) {
        List<String> actual = cases.stream().map(CaseDefinition::key).toList();
        if (!actual.equals(EXPECTED_KEYS)) {
            Set<String> missing = new LinkedHashSet<>(EXPECTED_KEYS);
            missing.removeAll(actual);
            Set<String> extras = new LinkedHashSet<>(actual);
            extras.removeAll(EXPECTED_KEYS);
            throw new IllegalStateException("ENTITY_TYPE_TAG_STARTED_RIDING catalog mismatch | missing=" + missing + " | extras=" + extras);
        }
        if (cases.size() != 5 || actual.stream().distinct().count() != 5
            || cases.stream().map(CaseDefinition::advancementId).distinct().count() != 5
            || cases.stream().anyMatch(c -> c.requirementGroupIndex() != 0)) {
            throw new IllegalStateException("ENTITY_TYPE_TAG_STARTED_RIDING catalog must contain exactly five singleton cases");
        }
    }

    private static void verifyFrozenBacap(Path bacap) throws IOException {
        if (!Files.exists(bacap)) {
            throw new IllegalStateException("Missing frozen BACAP source: " + bacap);
        }
        requireHash(bacap, "SHA-1", FROZEN_BACAP_SHA1);
        requireHash(bacap, "SHA-256", FROZEN_BACAP_SHA256);
    }

    private static void requireHash(Path path, String algorithm, String expected) throws IOException {
        try {
            byte[] actual = MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(path));
            String actualHex = hex(actual);
            if (!expected.equals(actualHex)) {
                throw new IllegalStateException("Frozen BACAP " + algorithm + " mismatch: " + actualHex);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest algorithm " + algorithm, e);
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

    private static void requireString(JsonObject json, String key, String expected, String context) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || !expected.equals(value.getAsString())) {
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

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String entityTypeTag,
        String selectedEntityType,
        String directVehicleType,
        List<String> vehiclePredicateChain,
        String requiredPassengerType,
        String action,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification
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
            json.addProperty("trigger", trigger);
            json.addProperty("entityTypeTag", entityTypeTag);
            json.addProperty("selectedEntityType", selectedEntityType);
            json.addProperty("directVehicleType", directVehicleType);
            JsonArray chain = new JsonArray();
            vehiclePredicateChain.forEach(chain::add);
            json.add("vehiclePredicateChain", chain);
            json.addProperty("requiredPassengerType", requiredPassengerType);
            json.addProperty("action", action);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("semanticsSource", semanticsSource);
            json.addProperty("sourceArtifactClassification", sourceArtifactClassification);
            return json;
        }
    }

    private record ExpectedCase(
        String advancementId,
        String sourcePath,
        String criterion,
        String entityTypeTag,
        String selectedEntityType,
        String directVehicleType,
        List<String> vehiclePredicateChain,
        String requiredPassengerType,
        String action
    ) {
        String key() {
            return advancementId + "#" + criterion;
        }
    }
}
