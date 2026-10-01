package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runtime-only view of the exact frozen-derived SHOT_CROSSBOW catalog. */
public final class CertifiedShotCrossbowCatalog {
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "shot_crossbow_case_catalog.json"
    );
    private static final String SNAPSHOT = "phase_a_shot_crossbow_case_catalog";
    private static final String FAMILY = "SHOT_CROSSBOW";
    private static final String TRIGGER = "minecraft:shot_crossbow";
    private static final String BOUNDARY = "ServerGamePacketListenerImpl.handleUseItem";
    private static final String PACKET_PATH =
        "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
            + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW";
    private static final String SUPPORTED = "SUPPORTED";
    private static final String SEMANTICS_SOURCE = "frozenBacap";
    private static final String SOURCE_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    private static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:enchanting/machine_bow#shot_crossbow",
        "blazeandcave:enchanting/shotbow#shot_crossbow"
    );
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedShotCrossbowCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedShotCrossbowCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    public static CaseDefinition requiredCase(String advancementId, String criterion) {
        String key = advancementId + "#" + criterion;
        for (CaseDefinition definition : allCases()) {
            if (definition.key().equals(key)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Missing certified SHOT_CROSSBOW case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseAShotCrossbowExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", SEMANTICS_SOURCE);
            requireString(root, "sourceArtifactClassification", SOURCE_CLASSIFICATION);
            requireString(root, "runtimeBoundary", BOUNDARY);
            requireString(root, "runtimePacketPath", PACKET_PATH);

            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", 2);
            requireInt(summary, "uniqueKeys", 2);
            requireInt(summary, "uniqueAdvancements", 2);
            requireInt(summary, "requirementGroups", 2);
            requireInt(summary, "automationSupported", 2);
            requireInt(summary, "automationDeferred", 0);
            JsonObject modes = requiredObject(summary, "firingModes");
            requireInt(modes, "QUICK_CHARGE", 1);
            requireInt(modes, "MULTISHOT", 1);

            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_KEYS.size()) {
                throw new IllegalStateException("Expected exactly two SHOT_CROSSBOW cases but found " + casesJson.size());
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : casesJson) {
                if (!element.isJsonObject()) {
                    throw new IllegalStateException("SHOT_CROSSBOW catalog case is not an object");
                }
                CaseDefinition definition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(definition.key())) {
                    throw new IllegalStateException("Duplicate SHOT_CROSSBOW catalog key: " + definition.key());
                }
                cases.add(definition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException("SHOT_CROSSBOW catalog key/order mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SHOT_CROSSBOW catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in SHOT_CROSSBOW catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in SHOT_CROSSBOW catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in SHOT_CROSSBOW catalog");
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in SHOT_CROSSBOW catalog");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in SHOT_CROSSBOW catalog");
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in SHOT_CROSSBOW catalog");
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
        static CaseDefinition fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String criterion = requiredString(json, "criterion");
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            requireString(json, "selectedItem", "minecraft:crossbow");
            JsonObject predicate = requiredObject(json, "enchantmentPredicate");
            String enchantment = requiredString(predicate, "enchantment");
            String levelRule = requiredString(predicate, "levelRule");
            int enchantmentLevel = requiredInt(predicate, "level");
            String firingMode = requiredString(json, "firingMode");
            int loaded = requiredInt(json, "expectedLoadedProjectileCount");
            int fired = requiredInt(json, "expectedFiredProjectileCount");
            int ammo = requiredInt(json, "expectedAmmoConsumed");
            int durability = requiredInt(json, "expectedDurabilityUse");
            if ("QUICK_CHARGE".equals(firingMode)) {
                if (!"minecraft:quick_charge".equals(enchantment) || !"min".equals(levelRule)
                    || enchantmentLevel != 3 || loaded != 1 || fired != 1 || ammo != 1 || durability != 1) {
                    throw new IllegalStateException("Invalid QUICK_CHARGE catalog semantics for " + advancementId);
                }
            } else if ("MULTISHOT".equals(firingMode)) {
                if (!"minecraft:multishot".equals(enchantment) || !"present".equals(levelRule)
                    || enchantmentLevel != 1 || loaded != 3 || fired != 3 || ammo != 1 || durability != 3) {
                    throw new IllegalStateException("Invalid MULTISHOT catalog semantics for " + advancementId);
                }
            } else {
                throw new IllegalStateException("Unknown SHOT_CROSSBOW firing mode: " + firingMode);
            }
            requireString(json, "boundary", BOUNDARY);
            requireString(json, "packetPath", PACKET_PATH);
            requireString(json, "automationEligibility", SUPPORTED);
            requireString(json, "semanticsSource", SEMANTICS_SOURCE);
            requireString(json, "sourceArtifactClassification", SOURCE_CLASSIFICATION);
            String sourceSha256 = requiredString(json, "sourceJsonSha256");
            return new CaseDefinition(
                advancementId,
                requiredString(json, "sourcePath"),
                criterion,
                0,
                1,
                TRIGGER,
                "minecraft:crossbow",
                enchantment,
                levelRule,
                enchantmentLevel,
                firingMode,
                loaded,
                fired,
                ammo,
                durability,
                BOUNDARY,
                PACKET_PATH,
                SUPPORTED,
                SEMANTICS_SOURCE,
                SOURCE_CLASSIFICATION,
                sourceSha256
            );
        }

        public String key() {
            return advancementId + "#" + criterion;
        }
    }
}
