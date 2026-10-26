package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record LandmarkQueryResult(
    LandmarkWorld world,
    BlockBox targetBox,
    Set<LandmarkRecord> matches
) {

    public LandmarkQueryResult {
        world = Objects.requireNonNull(world, "world");
        targetBox = Objects.requireNonNull(targetBox, "targetBox");
        matches = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(matches, "matches")));
    }

    public boolean isEmpty() {
        return matches.isEmpty();
    }
}
