package com.diskree.achievetodo.ability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

public enum LandmarkType {

    DESERT_PYRAMID(
        BuiltinStructures.DESERT_PYRAMID
    ),
    DESERT_WELL(
        Feature.DESERT_WELL
    ),
    JUNGLE_PYRAMID(
        BuiltinStructures.JUNGLE_TEMPLE
    ),
    PILLAGER_OUTPOST(
        BuiltinStructures.PILLAGER_OUTPOST
    ),
    IGLOO(
        BuiltinStructures.IGLOO
    ),
    SWAMP_HUT(
        BuiltinStructures.SWAMP_HUT
    ),
    MANSION(
        BuiltinStructures.WOODLAND_MANSION
    ),
    VILLAGE(
        BuiltinStructures.VILLAGE_PLAINS,
        BuiltinStructures.VILLAGE_DESERT,
        BuiltinStructures.VILLAGE_SAVANNA,
        BuiltinStructures.VILLAGE_SNOWY,
        BuiltinStructures.VILLAGE_TAIGA
    ),
    RUINED_PORTAL(
        BuiltinStructures.RUINED_PORTAL_STANDARD,
        BuiltinStructures.RUINED_PORTAL_DESERT,
        BuiltinStructures.RUINED_PORTAL_JUNGLE,
        BuiltinStructures.RUINED_PORTAL_SWAMP,
        BuiltinStructures.RUINED_PORTAL_MOUNTAIN,
        BuiltinStructures.RUINED_PORTAL_OCEAN
    ),
    BURIED_TREASURE(
        BuiltinStructures.BURIED_TREASURE
    ),
    SHIPWRECK(
        BuiltinStructures.SHIPWRECK,
        BuiltinStructures.SHIPWRECK_BEACHED
    ),
    OCEAN_RUIN(
        BuiltinStructures.OCEAN_RUIN_COLD,
        BuiltinStructures.OCEAN_RUIN_WARM
    ),
    MONUMENT(
        BuiltinStructures.OCEAN_MONUMENT
    ),
    MONSTER_ROOM(
        Feature.MONSTER_ROOM
    ),
    MINESHAFT(
        BuiltinStructures.MINESHAFT,
        BuiltinStructures.MINESHAFT_MESA
    ),
    TRAIL_RUINS(
        BuiltinStructures.TRAIL_RUINS
    ),
    ANCIENT_CITY(
        BuiltinStructures.ANCIENT_CITY
    ),
    TRIAL_CHAMBERS(
        BuiltinStructures.TRIAL_CHAMBERS
    ),
    STRONGHOLD(
        BuiltinStructures.STRONGHOLD
    ),
    FORTRESS(
        BuiltinStructures.FORTRESS
    ),
    BASTION_REMNANT(
        BuiltinStructures.BASTION_REMNANT
    ),
    END_CITY(
        BuiltinStructures.END_CITY
    );

    public static final HashMap<Feature<?>, LandmarkType> FEATURES = new HashMap<>();

    private final Set<ResourceKey<Structure>> structureRegistryKeys;
    private final Feature<?> feature;

    static {
        for (LandmarkType landmarkType : values()) {
            if (landmarkType.isFeature()) {
                FEATURES.put(landmarkType.getFeature(), landmarkType);
            }
        }
    }

    @SafeVarargs
    LandmarkType(ResourceKey<Structure>... structures) {
        this(null, Arrays.stream(structures).collect(Collectors.toUnmodifiableSet()));
    }

    LandmarkType(Feature<?> feature) {
        this(feature, null);
    }

    LandmarkType(Feature<?> feature, Set<ResourceKey<Structure>> structureRegistryKeys) {
        this.structureRegistryKeys = structureRegistryKeys;
        this.feature = feature;
    }

    public boolean isStructure() {
        return structureRegistryKeys != null;
    }

    public boolean isFeature() {
        return feature != null;
    }

    public Set<ResourceKey<Structure>> getStructureRegistryKeys() {
        return structureRegistryKeys;
    }

    public Feature<?> getFeature() {
        return feature;
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable LandmarkType findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (LandmarkType landmarkType : values()) {
            if (landmarkType.name().equalsIgnoreCase(name)) {
                return landmarkType;
            }
        }
        return null;
    }

    public static @Nullable LandmarkType findByStructureRegistryKey(ResourceKey<Structure> structureRegistryKey) {
        if (structureRegistryKey == null) {
            return null;
        }
        for (LandmarkType type : values()) {
            if (type.isStructure() && type.getStructureRegistryKeys().contains(structureRegistryKey)) {
                return type;
            }
        }
        return null;
    }
}
