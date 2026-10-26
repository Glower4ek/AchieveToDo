package com.diskree.achievetodo.ability;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public enum LandmarkType {

    DESERT_PYRAMID(structure("minecraft:desert_pyramid")),
    DESERT_WELL(feature("minecraft:desert_well")),
    JUNGLE_PYRAMID(structure("minecraft:jungle_pyramid")),
    PILLAGER_OUTPOST(structure("minecraft:pillager_outpost")),
    IGLOO(structure("minecraft:igloo")),
    SWAMP_HUT(structure("minecraft:swamp_hut")),
    MANSION(structure("minecraft:mansion")),
    VILLAGE(structure(
        "minecraft:village_plains",
        "minecraft:village_desert",
        "minecraft:village_savanna",
        "minecraft:village_snowy",
        "minecraft:village_taiga"
    )),
    RUINED_PORTAL(structure(
        "minecraft:ruined_portal",
        "minecraft:ruined_portal_desert",
        "minecraft:ruined_portal_jungle",
        "minecraft:ruined_portal_swamp",
        "minecraft:ruined_portal_mountain",
        "minecraft:ruined_portal_ocean",
        "minecraft:ruined_portal_nether"
    )),
    BURIED_TREASURE(structure("minecraft:buried_treasure")),
    SHIPWRECK(structure("minecraft:shipwreck", "minecraft:shipwreck_beached")),
    OCEAN_RUIN(structure("minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm")),
    MONUMENT(structure("minecraft:monument")),
    MONSTER_ROOM(feature("minecraft:monster_room")),
    MINESHAFT(structure("minecraft:mineshaft", "minecraft:mineshaft_mesa")),
    TRAIL_RUINS(structure("minecraft:trail_ruins")),
    ANCIENT_CITY(structure("minecraft:ancient_city")),
    TRIAL_CHAMBERS(structure("minecraft:trial_chambers")),
    STRONGHOLD(structure("minecraft:stronghold")),
    FORTRESS(structure("minecraft:fortress")),
    BASTION_REMNANT(structure("minecraft:bastion_remnant")),
    END_CITY(structure("minecraft:end_city"));

    private static final Map<String, LandmarkType> BY_NAME;
    private static final Map<String, LandmarkType> BY_STRUCTURE_KEY;
    private static final Map<String, LandmarkType> BY_FEATURE_KEY;

    static {
        Map<String, LandmarkType> byName = new java.util.HashMap<>();
        Map<String, LandmarkType> byStructureKey = new java.util.HashMap<>();
        Map<String, LandmarkType> byFeatureKey = new java.util.HashMap<>();
        for (LandmarkType landmarkType : values()) {
            byName.put(landmarkType.getName(), landmarkType);
            for (String structureKey : landmarkType.structureKeys) {
                byStructureKey.put(normalizeRegistryKey(structureKey), landmarkType);
            }
            if (landmarkType.featureKey != null) {
                byFeatureKey.put(normalizeRegistryKey(landmarkType.featureKey), landmarkType);
            }
        }
        BY_NAME = Collections.unmodifiableMap(byName);
        BY_STRUCTURE_KEY = Collections.unmodifiableMap(byStructureKey);
        BY_FEATURE_KEY = Collections.unmodifiableMap(byFeatureKey);
    }

    private final LandmarkDiscoveryType discoveryType;
    private final Set<String> structureKeys;
    private final String featureKey;

    LandmarkType(LandmarkDefinition definition) {
        this.discoveryType = definition.discoveryType();
        this.structureKeys = definition.structureKeys();
        this.featureKey = definition.featureKey();
    }

    public static LandmarkDefinition structure(String... structureKeys) {
        if (structureKeys.length == 0) {
            throw new IllegalArgumentException("At least one structure key is required");
        }
        Set<String> normalizedKeys = new LinkedHashSet<>();
        Arrays.stream(structureKeys)
            .map(LandmarkType::normalizeRegistryKey)
            .forEach(normalizedKeys::add);
        return new LandmarkDefinition(LandmarkDiscoveryType.STRUCTURE, Collections.unmodifiableSet(normalizedKeys), null);
    }

    public static LandmarkDefinition feature(String featureKey) {
        return new LandmarkDefinition(LandmarkDiscoveryType.FEATURE, Set.of(), normalizeRegistryKey(featureKey));
    }

    public boolean isStructure() {
        return discoveryType == LandmarkDiscoveryType.STRUCTURE;
    }

    public boolean isFeature() {
        return discoveryType == LandmarkDiscoveryType.FEATURE;
    }

    public LandmarkDiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public Set<String> getStructureKeys() {
        return structureKeys;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<LandmarkType> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.trim().toLowerCase(Locale.ROOT)));
    }

    public static Optional<LandmarkType> findByStructureKey(String structureKey) {
        if (structureKey == null || structureKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_STRUCTURE_KEY.get(normalizeRegistryKey(structureKey)));
    }

    public static Optional<LandmarkType> findByFeatureKey(String featureKey) {
        if (featureKey == null || featureKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_FEATURE_KEY.get(normalizeRegistryKey(featureKey)));
    }

    private static String normalizeRegistryKey(String key) {
        String normalized = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    public record LandmarkDefinition(
        LandmarkDiscoveryType discoveryType,
        Set<String> structureKeys,
        String featureKey
    ) {

        public LandmarkDefinition {
            discoveryType = Objects.requireNonNull(discoveryType, "discoveryType");
            structureKeys = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(structureKeys, "structureKeys")));
        }
    }
}
