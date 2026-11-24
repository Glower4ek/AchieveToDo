package com.diskree.achievetodo.restriction;

import java.util.UUID;

public record RestrictionProjectileContext(
    UUID projectileId,
    String projectileTypeKey,
    String launcherTypeKey
) {

    public RestrictionProjectileContext {
        projectileTypeKey = normalize(projectileTypeKey);
        launcherTypeKey = normalize(launcherTypeKey);
    }

    public static RestrictionProjectileContext none() {
        return new RestrictionProjectileContext(null, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
