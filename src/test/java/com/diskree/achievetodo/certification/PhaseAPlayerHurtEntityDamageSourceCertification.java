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

/** Deterministic frozen-definition catalog for PLAYER_HURT_ENTITY_DAMAGE_SOURCE. */
public final class PhaseAPlayerHurtEntityDamageSourceCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "player_hurt_entity_damage_source_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_player_hurt_entity_damage_source_case_catalog";
    public static final String FAMILY = "PLAYER_HURT_ENTITY_DAMAGE_SOURCE";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA1 = "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String TRIGGER = "minecraft:player_hurt_entity";
    public static final String SEMANTICS_SOURCE = "frozenBacap";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String WIND_BOUNDARY = "ServerGamePacketListenerImpl.handleUseItem";
    public static final String ATTACK_BOUNDARY = "ServerGamePacketListenerImpl.handleAttack";
    public static final String WIND_PACKET_PATH =
        "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
            + "->WindChargeItem.use->AbstractWindCharge.onHitEntity"
            + "->CriteriaTriggers.PLAYER_HURT_ENTITY";
    public static final String ATTACK_PACKET_PATH =
        "ServerboundAttackPacket.handle->ServerGamePacketListenerImpl.handleAttack"
            + "->ServerPlayer.attack->LivingEntity.hurtServer"
            + "->CriteriaTriggers.PLAYER_HURT_ENTITY";

    public static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:adventure/from_under_your_feet#from_under_your_feet",
        "blazeandcave:weaponry/slapfish#slapfish",
        "blazeandcave:weaponry/viking#axe"
    );

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<ExpectedCase> EXPECTED_CASES = List.of(
        new ExpectedCase(
            "blazeandcave:adventure/from_under_your_feet",
            "data/blazeandcave/advancement/adventure/from_under_your_feet.json",
            "from_under_your_feet",
            "minecraft:wind_charge",
            List.of("minecraft:is_projectile"),
            "minecraft:wind_charge",
            "minecraft:player",
            "minecraft:zombie",
            "minecraft:trapdoors",
            "",
            -1,
            "minecraft:wind_charge",
            "THROW_WIND_CHARGE",
            319,
            "THROW_WIND_CHARGE",
            WIND_BOUNDARY,
            WIND_PACKET_PATH,
            "de042121fb2de397125de6a93a86ac69390b8418510768df4e9430ce5353cecb"
        ),
        new ExpectedCase(
            "blazeandcave:weaponry/slapfish",
            "data/blazeandcave/advancement/weaponry/slapfish.json",
            "slapfish",
            "minecraft:player_attack",
            List.of(),
            "minecraft:player",
            "minecraft:player",
            "minecraft:zombie",
            "",
            "minecraft:fishes",
            5,
            "minecraft:cod",
            "NONE",
            0,
            "ATTACK_WITH_FISH",
            ATTACK_BOUNDARY,
            ATTACK_PACKET_PATH,
            "941131a9af23a8d0fc966bb220b13eb2ff077d7b33d6dcfc9ac9eccc76859e95"
        ),
        new ExpectedCase(
            "blazeandcave:weaponry/viking",
            "data/blazeandcave/advancement/weaponry/viking.json",
            "axe",
            "minecraft:player_attack",
            List.of(),
            "minecraft:player",
            "minecraft:player",
            "minecraft:zombie",
            "",
            "minecraft:axes",
            5,
            "minecraft:wooden_axe",
            "USE_WOODEN_TOOLS",
            71,
            "ATTACK_WITH_AXE",
            ATTACK_BOUNDARY,
            ATTACK_PACKET_PATH,
            "0530c2b8218fee1350bcf0bee875d306733a8b158aa5848d921852a37a8a7858"
        )
    );

    private PhaseAPlayerHurtEntityDamageSourceCertification() {
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
        root.addProperty("runtimeBoundary", "ServerGamePacketListenerImpl.handleAttack / handleUseItem");
        root.addProperty("runtimePacketPath", "ServerboundAttackPacket / ServerboundUseItemPacket -> native damage");

        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", cases.size());
        summary.addProperty("uniqueKeys", cases.stream().map(CaseDefinition::key).distinct().count());
        summary.addProperty("uniqueAdvancements", cases.stream().map(CaseDefinition::advancementId).distinct().count());
        summary.addProperty("requirementGroups", cases.size());
        summary.addProperty("automationSupported", cases.stream()
            .filter(c -> SUPPORTED.equals(c.automationEligibility())).count());
        summary.addProperty("automationDeferred", cases.stream()
            .filter(c -> !SUPPORTED.equals(c.automationEligibility())).count());
        JsonObject actionCounts = new JsonObject();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (CaseDefinition definition : cases) {
            counts.merge(definition.action(), 1L, Long::sum);
        }
        counts.forEach(actionCounts::addProperty);
        summary.add("actions", actionCounts);
        JsonObject damageTypes = new JsonObject();
        Map<String, Long> damageCounts = new LinkedHashMap<>();
        for (CaseDefinition definition : cases) {
            damageCounts.merge(definition.expectedDamageType(), 1L, Long::sum);
        }
        damageCounts.forEach(damageTypes::addProperty);
        summary.add("damageTypes", damageTypes);
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
                ZipEntry entry = zip.getEntry(expected.sourcePath());
                if (entry == null) {
                    throw new IllegalStateException("Missing frozen advancement " + expected.advancementId());
                }
                byte[] sourceBytes;
                try (InputStream input = zip.getInputStream(entry)) {
                    sourceBytes = input.readAllBytes();
                }
                String sourceSha256 = sha256(sourceBytes);
                if (!expected.sourceSha256().equals(sourceSha256)) {
                    throw new IllegalStateException("Frozen source hash changed for " + expected.advancementId()
                        + ": expected=" + expected.sourceSha256() + ", actual=" + sourceSha256);
                }
                JsonObject advancement;
                try (Reader reader = new InputStreamReader(
                    new ByteArrayInputStream(sourceBytes), StandardCharsets.UTF_8)) {
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
        if ("minecraft:wind_charge".equals(expected.expectedDamageType())) {
            deriveWindConditions(expected, conditions);
        } else {
            deriveAttackConditions(expected, conditions);
        }
        verifySingletonRequirements(advancement, expected);
        return new CaseDefinition(
            expected.advancementId(), expected.sourcePath(), expected.criterion(), 0, 1, TRIGGER,
            expected.expectedDamageType(), expected.requiredDamageTypeTags(), expected.expectedDirectEntityType(),
            expected.expectedSourceEntityType(), expected.targetEntityType(), expected.targetBlockTag(),
            expected.sourceEquipmentTag(), expected.distanceMax(), expected.selectedItem(), expected.action(),
            expected.ability(), expected.abilityUnlockThreshold(), expected.action(), expected.boundary(),
            expected.packetPath(), SUPPORTED, SEMANTICS_SOURCE, SOURCE_ARTIFACT_CLASSIFICATION, sourceSha256
        );
    }

    private static void deriveWindConditions(ExpectedCase expected, JsonObject conditions) {
        if (conditions.size() != 2 || !conditions.has("entity") || !conditions.has("damage")) {
            throw new IllegalStateException("Wind-charge predicate shape changed for " + expected.key());
        }
        JsonObject entity = requiredObject(conditions, "entity", expected.key());
        JsonObject steppingOn = requiredObject(entity, "stepping_on", expected.key());
        JsonObject block = requiredObject(steppingOn, "block", expected.key());
        requireString(block, "blocks", "#minecraft:trapdoors", expected.key());
        JsonObject damage = requiredObject(conditions, "damage", expected.key());
        JsonObject type = requiredObject(damage, "type", expected.key());
        JsonArray tags = requiredArray(type, "tags", expected.key());
        if (tags.size() != 1 || !tags.get(0).isJsonObject()) {
            throw new IllegalStateException("Expected one wind-charge damage tag for " + expected.key());
        }
        JsonObject tag = tags.get(0).getAsJsonObject();
        requireString(tag, "id", "minecraft:is_projectile", expected.key());
        requireBoolean(tag, "expected", true, expected.key());
        JsonObject direct = requiredObject(type, "direct_entity", expected.key());
        requireString(direct, "type", "minecraft:wind_charge", expected.key());
        if (type.size() != 2 || damage.size() != 1) {
            throw new IllegalStateException("Unexpected wind-charge damage predicate fields for " + expected.key());
        }
    }

    private static void deriveAttackConditions(ExpectedCase expected, JsonObject conditions) {
        if (conditions.size() != 2 || !conditions.has("entity") || !conditions.has("damage")) {
            throw new IllegalStateException("Player-attack predicate shape changed for " + expected.key());
        }
        JsonObject entity = requiredObject(conditions, "entity", expected.key());
        JsonObject distance = requiredObject(entity, "distance", expected.key());
        if (distance.size() != 1) {
            throw new IllegalStateException("Expected only max attack distance for " + expected.key());
        }
        JsonObject absolute = requiredObject(distance, "absolute", expected.key());
        if (absolute.size() != 1) {
            throw new IllegalStateException("Expected only absolute max attack distance for " + expected.key());
        }
        requireNumber(absolute, "max", expected.distanceMax(), expected.key());
        JsonObject damage = requiredObject(conditions, "damage", expected.key());
        JsonObject type = requiredObject(damage, "type", expected.key());
        if (type.size() != 1) {
            throw new IllegalStateException("Expected only source entity for " + expected.key());
        }
        JsonObject source = requiredObject(type, "source_entity", expected.key());
        JsonObject equipment = requiredObject(source, "equipment", expected.key());
        JsonObject mainhand = requiredObject(equipment, "mainhand", expected.key());
        requireString(mainhand, "items", "#" + expected.sourceEquipmentTag(), expected.key());
        if (damage.size() != 1 || source.size() != 1 || equipment.size() != 1) {
            throw new IllegalStateException("Unexpected player-attack predicate fields for " + expected.key());
        }
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
            throw new IllegalStateException("Requirement group is not a singleton for " + expected.key());
        }
    }

    private static void assertExactCatalog(List<CaseDefinition> cases) {
        List<String> actual = cases.stream().map(CaseDefinition::key).toList();
        if (!actual.equals(EXPECTED_KEYS)) {
            Set<String> missing = new LinkedHashSet<>(EXPECTED_KEYS);
            missing.removeAll(actual);
            Set<String> extras = new LinkedHashSet<>(actual);
            extras.removeAll(EXPECTED_KEYS);
            throw new IllegalStateException("PLAYER_HURT_ENTITY_DAMAGE_SOURCE catalog mismatch"
                + " | missing=" + missing + " | extras=" + extras + " | order=" + actual);
        }
        if (cases.size() != 3 || actual.stream().distinct().count() != 3
            || cases.stream().map(CaseDefinition::advancementId).distinct().count() != 3
            || cases.stream().anyMatch(c -> c.requirementGroupIndex() != 0 || c.requirementGroupCount() != 1)) {
            throw new IllegalStateException("PLAYER_HURT_ENTITY_DAMAGE_SOURCE catalog must contain three singleton cases");
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

    private static void requireBoolean(JsonObject parent, String key, boolean expected, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in " + context);
        }
    }

    private static void requireNumber(JsonObject parent, String key, int expected, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsDouble() != expected) {
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
        String expectedDamageType,
        List<String> requiredDamageTypeTags,
        String expectedDirectEntityType,
        String expectedSourceEntityType,
        String targetEntityType,
        String targetBlockTag,
        String sourceEquipmentTag,
        int distanceMax,
        String selectedItem,
        String ability,
        int abilityUnlockThreshold,
        String action,
        String boundary,
        String packetPath,
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
        String expectedDamageType,
        List<String> requiredDamageTypeTags,
        String expectedDirectEntityType,
        String expectedSourceEntityType,
        String targetEntityType,
        String targetBlockTag,
        String sourceEquipmentTag,
        int distanceMax,
        String selectedItem,
        String action,
        String ability,
        int abilityUnlockThreshold,
        String firingMode,
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
            json.addProperty("expectedDamageType", expectedDamageType);
            JsonArray tags = new JsonArray();
            requiredDamageTypeTags.forEach(tags::add);
            json.add("requiredDamageTypeTags", tags);
            json.addProperty("expectedDirectEntityType", expectedDirectEntityType);
            json.addProperty("expectedSourceEntityType", expectedSourceEntityType);
            json.addProperty("targetEntityType", targetEntityType);
            json.addProperty("targetBlockTag", targetBlockTag);
            json.addProperty("sourceEquipmentTag", sourceEquipmentTag);
            json.addProperty("distanceMax", distanceMax);
            json.addProperty("selectedItem", selectedItem);
            json.addProperty("action", action);
            json.addProperty("ability", ability);
            json.addProperty("abilityUnlockThreshold", abilityUnlockThreshold);
            json.addProperty("firingMode", firingMode);
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
