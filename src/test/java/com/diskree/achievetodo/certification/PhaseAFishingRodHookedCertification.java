package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** Frozen-source and catalog invariant for the singleton FISHING_ROD_HOOKED family. */
public final class PhaseAFishingRodHookedCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "fishing_rod_hooked_case_catalog.json");
    public static final Path PERSISTENT_EVIDENCE = Path.of("src", "test", "resources", "phase_a_certification", "fishing_rod_hooked_execution_evidence.json");
    public static final String FAMILY = "FISHING_ROD_HOOKED";
    public static final String SOURCE = "PhaseAFishingRodHookedGameTest";
    public static final String SNAPSHOT_ID = "phase_a_fishing_rod_hooked_case_catalog";
    public static final String ADVANCEMENT_ID = "blazeandcave:weaponry/indiana_jones";
    public static final String CRITERION = "indiana_jones";
    public static final String SOURCE_PATH = "data/blazeandcave/advancement/weaponry/indiana_jones.json";
    public static final String ENTITY_TAG_PATH = "data/blazeandcave/tags/entity_type/hostile_monsters.json";
    public static final String ENTITY_TAG = "blazeandcave:hostile_monsters";
    public static final String TRIGGER = "minecraft:fishing_rod_hooked";
    public static final String FIXTURE_ENTITY = "minecraft:ravager";
    public static final String ROD_ITEM = "minecraft:fishing_rod";
    public static final String RUNTIME_BOUNDARY = "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem->ServerPlayerGameMode.useItem->FishingRodItem.use->FishingHook.retrieve->CriteriaTriggers.FISHING_ROD_HOOKED";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final Path BACAP = Path.of("reference", "phase_a_preservation", "files", "final", "bacap.zip");
    private static final String BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final String SOURCE_SHA256 = "efcc25197096eb690266225ad524c7a46582a5c93cce173b99fd047a5ecf5f7a";
    private static final String ENTITY_TAG_SHA256 = "0a34f7342288773c229c3e6fa6be7aae25d431a71db9cf5b21d685068d697e2b";
    private static final List<List<String>> REQUIREMENTS = List.of(List.of(CRITERION));

    private PhaseAFishingRodHookedCertification() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of("").toAbsolutePath().normalize() : Path.of(args[0]).toAbsolutePath().normalize();
        validate(root);
        System.out.println("FISHING_ROD_HOOKED_CATALOG=PASS cases=1 fingerprint=sha-256:" + catalogFingerprint(root));
    }

    public static void validate(Path root) throws Exception {
        require(BACAP_SHA256.equals(sha(Files.readAllBytes(root.resolve(BACAP)))), "Frozen BACAP hash changed");
        JsonObject advancement;
        JsonObject hostileTag;
        try (ZipFile zip = new ZipFile(root.resolve(BACAP).toFile(), StandardCharsets.UTF_8)) {
            advancement = readZipJson(zip, SOURCE_PATH);
            hostileTag = readZipJson(zip, ENTITY_TAG_PATH);
        }
        String sourceHash = zipEntrySha256(root.resolve(BACAP), SOURCE_PATH);
        String tagHash = zipEntrySha256(root.resolve(BACAP), ENTITY_TAG_PATH);
        require(SOURCE_SHA256.equals(sourceHash), "Frozen Indiana Jones source changed");
        require(ENTITY_TAG_SHA256.equals(tagHash), "Frozen hostile-monster tag changed");
        validateFrozenDefinition(advancement, hostileTag);

        JsonObject catalog = JsonParser.parseString(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)).getAsJsonObject();
        require(SNAPSHOT_ID.equals(string(catalog, "snapshot")), "catalog snapshot mismatch");
        require(integer(catalog, "schemaVersion") == 1, "catalog schema mismatch");
        require(FAMILY.equals(string(catalog, "family")), "catalog family mismatch");
        require("Frozen BACAP".equals(string(catalog, "familySource")), "catalog source mismatch");
        require("26.2".equals(string(catalog, "minecraftVersion")), "catalog Minecraft version mismatch");
        require(COMPATIBILITY_MARKER.equals(string(catalog, "compatibilityMarker")), "catalog compatibility marker mismatch");
        require(BACAP_SHA256.equals(string(catalog, "frozenBacapSha256")), "catalog frozen BACAP fingerprint mismatch");
        require(TRIGGER.equals(string(catalog, "trigger")), "catalog trigger mismatch");
        require(RUNTIME_BOUNDARY.equals(string(catalog, "runtimeBoundary")), "catalog runtime boundary mismatch");
        require(integer(catalog, "advancementCount") == 1, "catalog advancement count mismatch");
        require(integer(catalog, "criterionCount") == 1, "catalog criterion count mismatch");
        require(integer(catalog, "requirementGroupCount") == 1, "catalog requirement group count mismatch");
        require(REQUIREMENTS.equals(requirements(catalog.getAsJsonArray("requirementGroups"))), "catalog requirement structure mismatch");
        require("NONE".equals(string(catalog, "requiredRodPredicate")), "unexpected rod predicate");
        require(ROD_ITEM.equals(string(catalog, "selectedRodWitness")), "catalog rod witness mismatch");
        require("NONE".equals(string(catalog, "requiredItemPredicate")), "unexpected caught-item predicate");
        requireEntityPredicate(catalog.getAsJsonObject("requiredHookedEntityPredicate"), "catalog hooked-entity predicate mismatch");
        require(FIXTURE_ENTITY.equals(string(catalog, "selectedHookedEntityWitness")), "catalog hooked-entity witness mismatch");

        JsonObject gate = catalog.getAsJsonObject("productionGate");
        require(gate != null && "ABILITY_UNLOCK".equals(string(gate, "kind")), "production gate kind mismatch");
        require("USE_FISHING_ROD".equals(string(gate, "ability")), "production ability mismatch");
        require("bac_advancements".equals(string(gate, "scoreboardObjective")), "production objective mismatch");
        require("LevelInfoExtension.achievetodo$getAbilitiesConfiguration(overworldSeed)".equals(string(gate, "thresholdSource")), "production threshold source mismatch");

        JsonArray cases = catalog.getAsJsonArray("cases");
        require(cases != null && cases.size() == 1, "catalog must contain exactly one case");
        JsonObject definition = cases.get(0).getAsJsonObject();
        require(ADVANCEMENT_ID.equals(string(definition, "advancementId")), "catalog advancement mismatch");
        require(CRITERION.equals(string(definition, "criterion")), "catalog criterion mismatch");
        require(integer(definition, "requirementGroupIndex") == 0, "catalog requirement group index mismatch");
        require(TRIGGER.equals(string(definition, "trigger")), "catalog case trigger mismatch");
        require("NONE".equals(string(definition, "rodPredicate")), "catalog case rod predicate mismatch");
        require(ROD_ITEM.equals(string(definition, "selectedRodWitness")), "catalog case rod witness mismatch");
        require("NONE".equals(string(definition, "itemPredicate")), "catalog case item predicate mismatch");
        require("NONE".equals(string(definition, "playerPredicate")), "catalog case player predicate mismatch");
        requireEntityPredicate(definition.getAsJsonObject("entityPredicate"), "catalog case entity predicate mismatch");
        require(ENTITY_TAG.equals(string(definition, "entityTag")), "catalog case entity tag mismatch");
        require(FIXTURE_ENTITY.equals(string(definition, "fixtureEntity")), "catalog case fixture mismatch");
        require(SOURCE_SHA256.equals(string(definition, "sourceJsonSha256")), "catalog source JSON hash mismatch");
        require(ENTITY_TAG_SHA256.equals(string(definition, "entityTagSourceSha256")), "catalog entity tag hash mismatch");
    }

    public static String catalogFingerprint(Path root) throws Exception {
        return sha(Files.readAllBytes(root.resolve(SNAPSHOT)));
    }

    public static List<List<String>> requirements() { return REQUIREMENTS; }

    private static void validateFrozenDefinition(JsonObject advancement, JsonObject hostileTag) {
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1 && criteria.has(CRITERION), "frozen advancement criterion shape changed");
        require(!advancement.has("requirements"), "frozen requirement semantics changed");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(TRIGGER.equals(string(criterion, "trigger")), "frozen trigger changed");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 1 && conditions.has("entity"), "frozen trigger conditions changed");
        JsonObject entity = conditions.getAsJsonObject("entity");
        require(entity.size() == 1 && ("#" + ENTITY_TAG).equals(string(entity, "type")), "frozen hooked entity predicate changed");
        JsonArray values = hostileTag.getAsJsonArray("values");
        require(values != null && values.asList().stream().map(JsonElement::getAsString).anyMatch(FIXTURE_ENTITY::equals), "ravager is not a frozen hostile-monster tag member");
    }

    private static JsonObject readZipJson(ZipFile zip, String path) throws IOException {
        var entry = zip.getEntry(path);
        require(entry != null, "Missing frozen BACAP entry " + path);
        try (InputStream input = zip.getInputStream(entry)) {
            return JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String zipEntrySha256(Path zipPath, String path) throws Exception {
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            var entry = zip.getEntry(path);
            require(entry != null, "Missing frozen BACAP entry " + path);
            try (InputStream input = zip.getInputStream(entry)) {
                return sha(input.readAllBytes());
            }
        }
    }

    private static List<List<String>> requirements(JsonArray groups) {
        require(groups != null, "Missing requirement groups");
        List<List<String>> result = new ArrayList<>();
        for (JsonElement groupElement : groups) {
            require(groupElement.isJsonArray(), "Invalid requirement group");
            List<String> group = new ArrayList<>();
            for (JsonElement criterion : groupElement.getAsJsonArray()) group.add(criterion.getAsString());
            result.add(List.copyOf(group));
        }
        return List.copyOf(result);
    }

    private static void requireEntityPredicate(JsonObject predicate, String message) {
        require(predicate != null && predicate.size() == 1 && ("#" + ENTITY_TAG).equals(string(predicate, "type")), message);
    }

    private static String string(JsonObject json, String field) {
        require(json != null && json.has(field) && json.get(field).isJsonPrimitive() && !json.get(field).getAsString().isBlank(), "Missing " + field);
        return json.get(field).getAsString();
    }

    private static int integer(JsonObject json, String field) {
        require(json != null && json.has(field) && json.get(field).isJsonPrimitive(), "Missing integer " + field);
        return json.get(field).getAsInt();
    }

    private static String sha(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
