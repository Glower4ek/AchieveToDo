package com.diskree.achievetodo.server;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A denied use or interaction answers with a consuming result so vanilla does not fall back to the item, and vanilla
 * then fires the use criteria as if the use had happened (0.4.0, known issue 4 audit: a map put into an item frame or
 * a golden dandelion given to a tadpole inside a locked landmark granted BACAP criteria). Every effectful denial marks
 * the player; the very next trigger of each kind for that player within a short window does not count. Same rule as
 * the Purpur plugin's NativeCriterionAgent.
 */
public final class DeniedCriteria {
    public enum Kind { ITEM_USED_ON_BLOCK, DEFAULT_BLOCK_USE, ANY_BLOCK_USE, PLAYER_INTERACTED_WITH_ENTITY }

    /** Guard -> trigger is a direct call chain on the server thread; the window only bounds stale marks. */
    static final long WINDOW_NANOS = 50_000_000L;
    private static final ConcurrentHashMap<UUID, long[]> DENIED = new ConcurrentHashMap<>();

    public static void markDenied(UUID player) {
        if (player == null) return;
        long[] marks = new long[Kind.values().length];
        Arrays.fill(marks, System.nanoTime());
        DENIED.put(player, marks);
    }

    /** One-shot per kind: a trigger of that kind clears its mark; only a fresh mark suppresses. */
    public static boolean consume(UUID player, Kind kind) {
        return consume(player, kind, System.nanoTime());
    }

    static boolean consume(UUID player, Kind kind, long now) {
        if (player == null) return false;
        boolean[] result = {false};
        DENIED.computeIfPresent(player, (id, marks) -> {
            long at = marks[kind.ordinal()];
            marks[kind.ordinal()] = Long.MIN_VALUE;
            result[0] = at != Long.MIN_VALUE && now - at < WINDOW_NANOS;
            return Arrays.stream(marks).allMatch(m -> m == Long.MIN_VALUE) ? null : marks;
        });
        return result[0];
    }

    public static void forget(UUID player) {
        DENIED.remove(player);
    }

    private DeniedCriteria() {}
}
