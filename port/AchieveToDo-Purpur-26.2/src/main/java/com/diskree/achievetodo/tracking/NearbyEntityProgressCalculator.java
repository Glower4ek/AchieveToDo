package com.diskree.achievetodo.tracking;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NearbyEntityProgressCalculator {

    private NearbyEntityProgressCalculator() {
    }

    public static EnumMap<TrackedNearbyEntitiesType, Integer> calculate(Iterable<ObservedNearbyEntity> observedEntities) {
        Objects.requireNonNull(observedEntities, "observedEntities");
        EnumMap<TrackedNearbyEntitiesType, Integer> progress = new EnumMap<>(TrackedNearbyEntitiesType.class);
        Map<TrackedNearbyEntitiesType, Set<String>> seenSlots = new EnumMap<>(TrackedNearbyEntitiesType.class);
        for (TrackedNearbyEntitiesType type : TrackedNearbyEntitiesType.values()) {
            progress.put(type, 0);
            seenSlots.put(type, new HashSet<>());
        }
        for (ObservedNearbyEntity observedEntity : observedEntities) {
            ObservedNearbyEntity normalizedEntity = Objects.requireNonNull(observedEntity, "observedEntity").normalized();
            for (TrackedNearbyEntitiesType type : TrackedNearbyEntitiesType.values()) {
                if (!type.getEntityKeys().contains(normalizedEntity.entityKey())) {
                    continue;
                }
                if (normalizedEntity.distanceSquared() > squared(type.getRadius())) {
                    continue;
                }
                String slotKey = normalizedEntity.slotKey(type.isBabySeparated());
                Set<String> slots = seenSlots.get(type);
                if (slots.add(slotKey)) {
                    progress.put(type, progress.get(type) + 1);
                }
            }
        }
        return progress;
    }

    private static double squared(int value) {
        return (double) value * value;
    }

    public record ObservedNearbyEntity(String entityKey, boolean baby, double distanceSquared) {

        public ObservedNearbyEntity {
            entityKey = Objects.requireNonNull(entityKey, "entityKey");
        }

        private ObservedNearbyEntity normalized() {
            return new ObservedNearbyEntity(entityKey.trim().toLowerCase(Locale.ROOT), baby, distanceSquared);
        }

        private String slotKey(boolean babySeparated) {
            return babySeparated ? entityKey + ":" + baby : entityKey;
        }
    }
}
