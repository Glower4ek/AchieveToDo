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

/** Runtime-only view of the exact frozen-derived damage-source catalog. */
public final class CertifiedPlayerHurtEntityDamageSourceCatalog {
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "player_hurt_entity_damage_source_case_catalog.json"
    );
    private static final String SNAPSHOT = "phase_a_player_hurt_entity_damage_source_case_catalog";
    private static final String FAMILY = "PLAYER_HURT_ENTITY_DAMAGE_SOURCE";
    private static final String TRIGGER = "minecraft:player_hurt_entity";
    private static final List<String> EXPECTED_KEYS = List.of(
        "blazeandcave:adventure/from_under_your_feet#from_under_your_feet",
        "blazeandcave:weaponry/slapfish#slapfish",
        "blazeandcave:weaponry/viking#axe"
    );
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedPlayerHurtEntityDamageSourceCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedPlayerHurtEntityDamageSourceCatalog.class) {
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
        throw new IllegalArgumentException("Missing certified " + FAMILY + " case: " + key);
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path path = PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            requireString(root, "snapshot", SNAPSHOT);
            requireInt(root, "schemaVersion", 1);
            requireString(root, "family", FAMILY);
            requireInt(root, "canonicalAdvancementCount", 1152);
            requireString(root, "minecraftVersion", "26.2");
            requireString(root, "compatibilityMarker", "compat_26_2_r15");
            requireString(root, "semanticsSource", "frozenBacap");
            requireString(root, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");

            JsonObject summary = requiredObject(root, "summary");
            requireInt(summary, "totalCases", 3);
            requireInt(summary, "uniqueKeys", 3);
            requireInt(summary, "uniqueAdvancements", 3);
            requireInt(summary, "requirementGroups", 3);
            requireInt(summary, "automationSupported", 3);
            requireInt(summary, "automationDeferred", 0);

            JsonArray casesJson = requiredArray(root, "cases");
            if (casesJson.size() != EXPECTED_KEYS.size()) {
                throw new IllegalStateException("Expected exactly three " + FAMILY + " cases");
            }
            List<CaseDefinition> cases = new ArrayList<>();
            Set<String> keys = new LinkedHashSet<>();
            for (JsonElement element : casesJson) {
                if (!element.isJsonObject()) {
                    throw new IllegalStateException(FAMILY + " catalog case is not an object");
                }
                CaseDefinition definition = CaseDefinition.fromJson(element.getAsJsonObject());
                if (!keys.add(definition.key())) {
                    throw new IllegalStateException("Duplicate " + FAMILY + " catalog key: " + definition.key());
                }
                cases.add(definition);
            }
            if (!keys.equals(new LinkedHashSet<>(EXPECTED_KEYS))) {
                throw new IllegalStateException(FAMILY + " catalog key/order mismatch: " + keys);
            }
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + FAMILY + " catalog", e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in " + FAMILY + " catalog");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in " + FAMILY + " catalog");
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in " + FAMILY + " catalog");
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in " + FAMILY + " catalog");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in " + FAMILY + " catalog");
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in " + FAMILY + " catalog");
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
        static CaseDefinition fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String sourcePath = requiredString(json, "sourcePath");
            String criterion = requiredString(json, "criterion");
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            String damageType = requiredString(json, "expectedDamageType");
            JsonArray tagsJson = requiredArray(json, "requiredDamageTypeTags");
            List<String> tags = new ArrayList<>();
            for (JsonElement tag : tagsJson) {
                tags.add(tag.getAsString());
            }
            String direct = requiredStringAllowBlank(json, "expectedDirectEntityType");
            String source = requiredString(json, "expectedSourceEntityType");
            String target = requiredString(json, "targetEntityType");
            String targetBlockTag = requiredStringAllowBlank(json, "targetBlockTag");
            String sourceEquipmentTag = requiredStringAllowBlank(json, "sourceEquipmentTag");
            int distanceMax = requiredInt(json, "distanceMax");
            String selectedItem = requiredString(json, "selectedItem");
            String action = requiredString(json, "action");
            String ability = requiredString(json, "ability");
            int threshold = requiredInt(json, "abilityUnlockThreshold");
            String firingMode = requiredString(json, "firingMode");
            String boundary = requiredString(json, "boundary");
            String packetPath = requiredString(json, "packetPath");
            String eligibility = requiredString(json, "automationEligibility");
            requireString(json, "semanticsSource", "frozenBacap");
            requireString(json, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
            String sourceHash = requiredString(json, "sourceJsonSha256");
            if ("minecraft:wind_charge".equals(damageType)) {
                if (!tags.equals(List.of("minecraft:is_projectile"))
                    || !direct.equals("minecraft:wind_charge") || !source.equals("minecraft:player")
                    || !target.equals("minecraft:zombie") || !targetBlockTag.equals("minecraft:trapdoors")
                    || !sourceEquipmentTag.isBlank() || distanceMax != -1
                    || !selectedItem.equals("minecraft:wind_charge") || !action.equals("THROW_WIND_CHARGE")
                    || !ability.equals("THROW_WIND_CHARGE") || threshold != 319
                    || !boundary.equals("ServerGamePacketListenerImpl.handleUseItem")) {
                    throw new IllegalStateException("Invalid wind-charge catalog semantics for " + advancementId);
                }
            } else if ("minecraft:player_attack".equals(damageType)) {
                boolean axe = "minecraft:axes".equals(sourceEquipmentTag);
                if (!tags.isEmpty() || !direct.equals("minecraft:player") || !source.equals("minecraft:player")
                    || !target.equals("minecraft:zombie") || !targetBlockTag.isBlank()
                    || !sourceEquipmentTag.startsWith("minecraft:") || distanceMax != 5
                    || (!axe && (!ability.equals("NONE") || threshold != 0))
                    || (axe && (!ability.equals("USE_WOODEN_TOOLS") || threshold != 71))
                    || !boundary.equals("ServerGamePacketListenerImpl.handleAttack")) {
                    throw new IllegalStateException("Invalid player-attack catalog semantics for " + advancementId);
                }
            } else {
                throw new IllegalStateException("Unknown damage type in " + FAMILY + " catalog: " + damageType);
            }
            return new CaseDefinition(
                advancementId, sourcePath, criterion, 0, 1, TRIGGER, damageType, List.copyOf(tags), direct, source,
                target, targetBlockTag, sourceEquipmentTag, distanceMax, selectedItem, action, ability, threshold,
                firingMode, boundary, packetPath, eligibility, "frozenBacap", "FROZEN_BACAP_DEFINITION", sourceHash
            );
        }

        private static String requiredStringAllowBlank(JsonObject json, String key) {
            JsonElement value = json.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException("Missing string " + key + " in " + FAMILY + " catalog");
            }
            return value.getAsString();
        }

        public String key() {
            return advancementId + "#" + criterion;
        }
    }
}
