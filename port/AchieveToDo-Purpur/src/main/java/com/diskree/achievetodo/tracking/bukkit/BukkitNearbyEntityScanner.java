package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.tracking.NearbyEntityProgressCalculator;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

final class BukkitNearbyEntityScanner {

    private static final int MAX_SCAN_RADIUS = Arrays.stream(TrackedNearbyEntitiesType.values())
        .mapToInt(TrackedNearbyEntitiesType::getRadius)
        .max()
        .orElse(0);
    private static final double MAX_SCAN_RADIUS_SQUARED = (double) MAX_SCAN_RADIUS * MAX_SCAN_RADIUS;

    BukkitNearbyEntityScanResult scan(Player player) {
        EnumMap<TrackedNearbyEntitiesType, Integer> zeroProgress = zeroProgress();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return new BukkitNearbyEntityScanResult(zeroProgress, 0);
        }
        Location playerLocation = player.getLocation();
        List<NearbyEntityProgressCalculator.ObservedNearbyEntity> observedEntities = new ArrayList<>();
        int inspectedEntities = 0;
        for (Entity entity : player.getNearbyEntities(MAX_SCAN_RADIUS, MAX_SCAN_RADIUS, MAX_SCAN_RADIUS)) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }
            inspectedEntities++;
            double distanceSquared = playerLocation.distanceSquared(entity.getLocation());
            if (distanceSquared > MAX_SCAN_RADIUS_SQUARED) {
                continue;
            }
            observedEntities.add(new NearbyEntityProgressCalculator.ObservedNearbyEntity(
                entity.getType().name().toLowerCase(Locale.ROOT),
                isBaby(livingEntity),
                distanceSquared
            ));
        }
        return new BukkitNearbyEntityScanResult(
            NearbyEntityProgressCalculator.calculate(observedEntities),
            inspectedEntities
        );
    }

    private static boolean isBaby(LivingEntity entity) {
        if (entity instanceof Zombie zombie) {
            return zombie.isBaby();
        }
        if (entity instanceof Ageable ageable) {
            return !ageable.isAdult();
        }
        return false;
    }

    private static EnumMap<TrackedNearbyEntitiesType, Integer> zeroProgress() {
        EnumMap<TrackedNearbyEntitiesType, Integer> progress = new EnumMap<>(TrackedNearbyEntitiesType.class);
        for (TrackedNearbyEntitiesType type : TrackedNearbyEntitiesType.values()) {
            progress.put(type, 0);
        }
        return progress;
    }

    record BukkitNearbyEntityScanResult(
        EnumMap<TrackedNearbyEntitiesType, Integer> progressByType,
        int inspectedEntities
    ) {
    }
}
