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

/**
 * Deterministic frozen-definition catalog for the current item-tag/entity
 * interaction frontier family.  The catalog intentionally derives only the
 * four durable-map cases; it is not a source-wide advancement inventory.
 */
public final class PhaseAEntityTypeTagPlayerInteractedWithEntityCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_player_interacted_with_entity_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_entity_type_tag_player_interacted_with_entity_case_catalog";
    public static final String FAMILY = "ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String TRIGGER = "minecraft:player_interacted_with_entity";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String SEMANTICS_SOURCE = "frozenBacap";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleInteract";
    public static final String PACKET_PATH = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY";

    public static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:animal/lead_the_way#lead",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#sugar",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#wheat",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#apple",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_apple",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_carrot",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#hay_block",
        "blazeandcave:animal/so_hungry_i_could_eat_a_horse#enchanted_golden_apple",
        "blazeandcave:animal/you_lead_ill_follow#lead"
    );

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<ExpectedCase> EXPECTED_CASES = List.of(
        new ExpectedCase(
            "blazeandcave:animal/lead_the_way",
            "data/blazeandcave/advancement/animal/lead_the_way.json",
            "lead",
            "blazeandcave:dont_trigger_piwe",
            "minecraft:lead",
            "minecraft:pig",
            Map.of(),
            "ATTACH_LEAD",
            "NONE",
            "data/blazeandcave/tags/entity_type/dont_trigger_piwe.json",
            "f7dc77103a22595fc8dad39f681a5dd29cbfa45390a683248eca07be71a866a7"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "sugar",
            "blazeandcave:horses",
            "minecraft:sugar",
            "minecraft:horse",
            Map.of(),
            "FEED_HORSE",
            "NONE",
            "data/blazeandcave/tags/entity_type/horses.json",
            "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "wheat",
            "blazeandcave:horses",
            "minecraft:wheat",
            "minecraft:horse",
            Map.of(),
            "FEED_HORSE",
            "NONE",
            "data/blazeandcave/tags/entity_type/horses.json",
            "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "apple",
            "blazeandcave:horses",
            "minecraft:apple",
            "minecraft:horse",
            Map.of(),
            "FEED_HORSE",
            "NONE",
            "data/blazeandcave/tags/entity_type/horses.json",
            "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "golden_apple", "blazeandcave:horses", "minecraft:golden_apple", "minecraft:horse", Map.of(), "FEED_HORSE", "NONE",
            "data/blazeandcave/tags/entity_type/horses.json", "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "golden_carrot", "blazeandcave:horses", "minecraft:golden_carrot", "minecraft:horse", Map.of(), "FEED_HORSE", "NONE",
            "data/blazeandcave/tags/entity_type/horses.json", "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "hay_block", "blazeandcave:horses", "minecraft:hay_block", "minecraft:horse", Map.of(), "FEED_HORSE", "NONE",
            "data/blazeandcave/tags/entity_type/horses.json", "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "data/blazeandcave/advancement/animal/so_hungry_i_could_eat_a_horse.json",
            "enchanted_golden_apple", "blazeandcave:horses", "minecraft:enchanted_golden_apple", "minecraft:horse", Map.of(), "FEED_HORSE", "NONE",
            "data/blazeandcave/tags/entity_type/horses.json", "77fc77bdc0bcf45370c8fe68fbfe576044a0b05ed69ac7aa117156c0694bfaeb"
        ),
        new ExpectedCase(
            "blazeandcave:animal/you_lead_ill_follow",
            "data/blazeandcave/advancement/animal/you_lead_ill_follow.json",
            "lead", "blazeandcave:llamas", "minecraft:lead", "minecraft:llama", Map.of(), "ATTACH_LEAD", "NONE",
            "data/blazeandcave/tags/entity_type/llamas.json", "502c2a965bf6bdb960388a945f12e26d8505a778a3f5e9afc20a8bbee56cca3d"
        )
    );

    private PhaseAEntityTypeTagPlayerInteractedWithEntityCertification() {
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
        JsonObject actions = new JsonObject();
        Map<String, Long> actionCounts = new LinkedHashMap<>();
        for (CaseDefinition definition : cases) {
            actionCounts.merge(definition.action(), 1L, Long::sum);
        }
        actionCounts.forEach(actions::addProperty);
        summary.add("actionCategories", actions);
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
                JsonObject advancement;
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(sourceBytes), StandardCharsets.UTF_8)) {
                    advancement = JsonParser.parseReader(reader).getAsJsonObject();
                }
                cases.add(deriveCase(zip, expected, advancement, sourceSha256));
            }
        }
        return List.copyOf(cases);
    }

    private static CaseDefinition deriveCase(
        ZipFile zip,
        ExpectedCase expected,
        JsonObject advancement,
        String sourceSha256
    ) throws IOException {
        if (!expected.sourceSha256().equals(sourceSha256)) {
            throw new IllegalStateException("Frozen source hash changed for " + expected.advancementId()
                + ": expected=" + expected.sourceSha256() + ", actual=" + sourceSha256);
        }
        JsonObject criteria = requiredObject(advancement, "criteria", expected.advancementId());
        if (!criteria.has(expected.criterion())) {
            throw new IllegalStateException("Missing criterion " + expected.criterion() + " for " + expected.advancementId());
        }
        JsonObject criterion = criteria.getAsJsonObject(expected.criterion());
        requireString(criterion, "trigger", TRIGGER, expected.key());
        JsonObject conditions = requiredObject(criterion, "conditions", expected.key());

        JsonObject item = requiredObject(conditions, "item", expected.key());
        JsonArray items = requiredArray(item, "items", expected.key());
        if (items.size() != 1 || !expected.selectedItem().equals(normalizeId(items.get(0).getAsString()))) {
            throw new IllegalStateException("Unexpected direct item witness for " + expected.key());
        }

        JsonArray entities = requiredArray(conditions, "entity", expected.key());
        if (entities.size() != 1 || !entities.get(0).isJsonObject()) {
            throw new IllegalStateException("Expected one entity predicate for " + expected.key());
        }
        JsonObject entityCondition = entities.get(0).getAsJsonObject();
        JsonObject properties = entityCondition;
        boolean inverted = "blazeandcave:dont_trigger_piwe".equals(expected.itemTag());
        if (inverted) {
            requireString(entityCondition, "condition", "minecraft:inverted", expected.key());
            properties = requiredObject(entityCondition, "term", expected.key());
        }
        requireString(properties, "condition", "minecraft:entity_properties", expected.key());
        requireString(properties, "entity", "this", expected.key());
        JsonObject predicate = requiredObject(properties, "predicate", expected.key());
        String entityTag = selector(predicate, "minecraft:entity_type", "type", expected.key());
        if (!expected.itemTag().equals(entityTag)) {
            throw new IllegalStateException("Unexpected entity tag for " + expected.key()
                + ": expected=" + expected.itemTag() + ", actual=" + entityTag);
        }
        verifySingletonRequirements(advancement, expected);

        String tagSource = expected.tagSourcePath().isBlank() ? "minecraftRuntime" : "frozenBacap";
        String tagSha256 = "";
        if (!expected.tagSourcePath().isBlank()) {
            ZipEntry tagEntry = zip.getEntry(expected.tagSourcePath());
            if (tagEntry == null) {
                throw new IllegalStateException("Missing frozen item tag " + expected.itemTag());
            }
            byte[] tagBytes;
            try (InputStream input = zip.getInputStream(tagEntry)) {
                tagBytes = input.readAllBytes();
            }
            tagSha256 = sha256(tagBytes);
            if (!expected.tagSourceSha256().equals(tagSha256)) {
                throw new IllegalStateException("Frozen tag hash changed for " + expected.itemTag());
            }
            requiredArray(JsonParser.parseString(new String(tagBytes, StandardCharsets.UTF_8)).getAsJsonObject(), "values", expected.itemTag());
        }

        return new CaseDefinition(
            expected.advancementId(),
            expected.sourcePath(),
            expected.criterion(),
            0,
            TRIGGER,
            expected.itemTag(),
            expected.selectedItem(),
            expected.selectedEntityType(),
            expected.flags(),
            expected.action(),
            "MAIN_HAND",
            BOUNDARY,
            PACKET_PATH,
            "SUCCESS",
            expected.productionPrecondition(),
            SUPPORTED,
            SEMANTICS_SOURCE,
            SOURCE_ARTIFACT_CLASSIFICATION,
            sourceSha256,
            tagSource,
            tagSha256
        );
    }

    private static Map<String, String> parseFlags(JsonObject predicate, String key) {
        JsonElement flagsElement = predicate.has("minecraft:flags") ? predicate.get("minecraft:flags") : predicate.get("flags");
        if (flagsElement == null || flagsElement.isJsonNull()) {
            return Map.of();
        }
        if (!flagsElement.isJsonObject()) {
            throw new IllegalStateException("Entity flags are not an object for " + key);
        }
        Map<String, String> flags = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : flagsElement.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                throw new IllegalStateException("Entity flag is not primitive for " + key);
            }
            flags.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(flags);
    }

    private static String selector(JsonObject object, String currentKey, String legacyKey, String context) {
        JsonElement current = object.get(currentKey);
        JsonElement legacy = object.get(legacyKey);
        if (current != null && legacy != null && !current.equals(legacy)) {
            throw new IllegalStateException("Conflicting entity selector keys for " + context);
        }
        JsonElement value = current != null ? current : legacy;
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException("Missing entity selector for " + context);
        }
        return normalizeId(value.getAsString());
    }

    private static String normalizeId(String value) {
        String withoutHash = value.startsWith("#") ? value.substring(1) : value;
        return withoutHash.contains(":") ? withoutHash : "minecraft:" + withoutHash;
    }

    private static void verifySingletonRequirements(JsonObject advancement, ExpectedCase expected) {
        JsonElement requirementsElement = advancement.get("requirements");
        if (requirementsElement == null || requirementsElement.isJsonNull()) {
            return;
        }
        if (!requirementsElement.isJsonArray()) {
            throw new IllegalStateException("requirements is not an array for " + expected.key());
        }
        JsonArray requirements = requirementsElement.getAsJsonArray();
        if (requirements.size() != 1 || !requirements.get(0).isJsonArray()) {
            throw new IllegalStateException("Expected one requirement group for " + expected.key());
        }
        JsonArray group = requirements.get(0).getAsJsonArray();
        if (group.size() != 1 || !expected.criterion().equals(group.get(0).getAsString())) {
            throw new IllegalStateException("Requirement group 0 is not the accepted singleton for " + expected.key());
        }
    }

    private static void assertExactCatalog(List<CaseDefinition> cases) {
        List<String> actual = cases.stream().map(CaseDefinition::key).toList();
        if (!actual.equals(EXPECTED_KEYS)) {
            Set<String> missing = new LinkedHashSet<>(EXPECTED_KEYS);
            missing.removeAll(actual);
            Set<String> extras = new LinkedHashSet<>(actual);
            extras.removeAll(EXPECTED_KEYS);
            throw new IllegalStateException("ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY catalog mismatch"
                + " | missing=" + missing + " | extras=" + extras + " | order=" + actual);
        }
        if (cases.size() != 9 || actual.stream().distinct().count() != 9
            || cases.stream().map(CaseDefinition::advancementId).distinct().count() != 3
            || cases.stream().anyMatch(c -> c.requirementGroupIndex() != 0)) {
            throw new IllegalStateException("ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY catalog must contain exactly nine singleton cases across three advancements");
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

    private static String requiredString(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank string " + key + " in " + context);
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject parent, String key, String expected, String context) {
        if (!expected.equals(requiredString(parent, key, context))) {
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
        String itemTag,
        String selectedItem,
        String selectedEntityType,
        Map<String, String> flags,
        String action,
        String productionPrecondition,
        String tagSourcePath,
        String tagSourceSha256
    ) {
        String key() {
            return advancementId + "#" + criterion;
        }

        String sourceSha256() {
            return switch (key()) {
                case "blazeandcave:animal/lead_the_way#lead" -> "930343ed32eebbc5607041eb792c0297e9a7b7aac54cdd715a7c71fe1c01ae24";
                case "blazeandcave:animal/so_hungry_i_could_eat_a_horse#sugar",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#wheat",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#apple",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_apple",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#golden_carrot",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#hay_block",
                    "blazeandcave:animal/so_hungry_i_could_eat_a_horse#enchanted_golden_apple" -> "8fbe46bffbb8102e251d7bc8663c0ed8d37899a3e652abbcad09c162052a2e7a";
                case "blazeandcave:animal/you_lead_ill_follow#lead" -> "66748aa92dc555ab29fc0b77f4c1eba3b0eef2885f0df1edf656f2b4692871f2";
                default -> throw new IllegalStateException("Unknown expected case " + key());
            };
        }
    }

    public record CaseDefinition(
        String advancementId,
        String sourcePath,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String itemTag,
        String selectedItem,
        String selectedEntityType,
        Map<String, String> entityFlags,
        String action,
        String interactionHand,
        String boundary,
        String packetPath,
        String expectedInteractionResult,
        String productionPrecondition,
        String automationEligibility,
        String semanticsSource,
        String sourceArtifactClassification,
        String sourceJsonSha256,
        String itemTagSource,
        String itemTagSourceSha256
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
            json.addProperty("itemTag", itemTag);
            json.addProperty("selectedItem", selectedItem);
            JsonObject entityPredicate = new JsonObject();
            entityPredicate.addProperty("entityType", selectedEntityType);
            JsonObject flags = new JsonObject();
            entityFlags.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> flags.addProperty(entry.getKey(), entry.getValue()));
            entityPredicate.add("flags", flags);
            json.add("entityPredicate", entityPredicate);
            json.addProperty("selectedEntityType", selectedEntityType);
            json.addProperty("action", action);
            json.addProperty("interactionHand", interactionHand);
            json.addProperty("boundary", boundary);
            json.addProperty("packetPath", packetPath);
            json.addProperty("expectedInteractionResult", expectedInteractionResult);
            json.addProperty("productionPrecondition", productionPrecondition);
            json.addProperty("automationEligibility", automationEligibility);
            json.addProperty("semanticsSource", semanticsSource);
            json.addProperty("sourceArtifactClassification", sourceArtifactClassification);
            json.addProperty("sourceJsonSha256", sourceJsonSha256);
            json.addProperty("itemTagSource", itemTagSource);
            if (!itemTagSourceSha256.isBlank()) {
                json.addProperty("itemTagSourceSha256", itemTagSourceSha256);
            }
            return json;
        }
    }
}
