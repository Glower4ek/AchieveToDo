package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.landmark.LandmarkWorld;

import java.util.Objects;
import java.util.UUID;

public record RestrictionTarget(
    RestrictionTargetKind kind,
    LandmarkWorld world,
    BlockBox blockBox,
    UUID entityId,
    String targetKey
) {

    public RestrictionTarget {
        kind = kind == null ? RestrictionTargetKind.NONE : kind;
        targetKey = normalize(targetKey);
        if (kind == RestrictionTargetKind.NONE) {
            world = null;
            blockBox = null;
            entityId = null;
            targetKey = null;
        } else if (world == null || blockBox == null) {
            throw new IllegalArgumentException("Non-empty targets require world and blockBox");
        }
    }

    public static RestrictionTarget none() {
        return new RestrictionTarget(RestrictionTargetKind.NONE, null, null, null, null);
    }

    public static RestrictionTarget block(LandmarkWorld world, BlockBox blockBox, String blockKey) {
        return new RestrictionTarget(RestrictionTargetKind.BLOCK, world, blockBox, null, blockKey);
    }

    public static RestrictionTarget area(LandmarkWorld world, BlockBox blockBox, String targetKey) {
        return new RestrictionTarget(RestrictionTargetKind.AREA, world, blockBox, null, targetKey);
    }

    public static RestrictionTarget entity(LandmarkWorld world, BlockBox blockBox, UUID entityId, String entityTypeKey) {
        return new RestrictionTarget(RestrictionTargetKind.ENTITY, world, blockBox, entityId, entityTypeKey);
    }

    public boolean hasLandmarkQueryableRegion() {
        return world != null && blockBox != null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
