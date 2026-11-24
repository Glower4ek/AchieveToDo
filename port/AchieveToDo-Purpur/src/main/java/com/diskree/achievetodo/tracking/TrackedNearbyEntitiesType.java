package com.diskree.achievetodo.tracking;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum TrackedNearbyEntitiesType {

    ANIMAL_KINGDOM(
        "blazeandcave:animal/animal_kingdom",
        32,
        false,
        "axolotl", "bat", "cat", "chicken", "cod", "cow", "donkey", "fox", "frog", "glow_squid",
        "horse", "mooshroom", "mule", "ocelot", "parrot", "pig", "pufferfish", "rabbit", "salmon", "sheep",
        "squid", "strider", "tropical_fish", "turtle", "bee", "dolphin", "goat", "llama", "panda", "polar_bear",
        "wolf", "camel", "sniffer", "skeleton_horse", "armadillo", "tadpole", "hoglin"
    ),
    FAMILY_REUNION(
        "blazeandcave:monsters/family_reunion",
        5,
        true,
        "husk", "zombie_villager", "drowned", "zombified_piglin", "zombie"
    ),
    BONE_TO_PARTY(
        "blazeandcave:monsters/bone_to_party",
        5,
        false,
        "skeleton_horse", "wither", "stray", "bogged", "wither_skeleton", "skeleton"
    );

    private final String advancementId;
    private final int radius;
    private final boolean babySeparated;
    private final Set<String> entityKeys;

    TrackedNearbyEntitiesType(String advancementId, int radius, boolean babySeparated, String... entityKeys) {
        this.advancementId = advancementId;
        this.radius = radius;
        this.babySeparated = babySeparated;
        this.entityKeys = Arrays.stream(entityKeys)
            .filter(Objects::nonNull)
            .map(key -> key.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    public String getAdvancementId() {
        return advancementId;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isBabySeparated() {
        return babySeparated;
    }

    public Set<String> getEntityKeys() {
        return entityKeys;
    }

    public int getEntitiesCount() {
        int entitiesCount = entityKeys.size();
        if (babySeparated) {
            entitiesCount *= 2;
        }
        return entitiesCount;
    }

    public static Optional<TrackedNearbyEntitiesType> findByAdvancementId(String advancementId) {
        if (advancementId == null || advancementId.isBlank()) {
            return Optional.empty();
        }
        String normalizedAdvancementId = advancementId.trim().toLowerCase(Locale.ROOT);
        for (TrackedNearbyEntitiesType type : values()) {
            if (type.advancementId.equals(normalizedAdvancementId)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
