package com.diskree.achievetodo.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Same contract as the plugin's NativeCriterionContractTest: one-shot bounded marks per trigger kind. */
final class DeniedCriteriaTest {

    @Test
    void eachKindIsSuppressedOnceAfterADenial() {
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        for (var kind : DeniedCriteria.Kind.values()) assertFalse(DeniedCriteria.consume(player, kind));
        DeniedCriteria.markDenied(player);
        assertFalse(DeniedCriteria.consume(other, DeniedCriteria.Kind.ITEM_USED_ON_BLOCK));
        // A block denial fires DEFAULT_BLOCK_USE, then ANY_BLOCK_USE: both are suppressed once.
        assertTrue(DeniedCriteria.consume(player, DeniedCriteria.Kind.DEFAULT_BLOCK_USE));
        assertTrue(DeniedCriteria.consume(player, DeniedCriteria.Kind.ANY_BLOCK_USE));
        assertFalse(DeniedCriteria.consume(player, DeniedCriteria.Kind.DEFAULT_BLOCK_USE));
        assertTrue(DeniedCriteria.consume(player, DeniedCriteria.Kind.PLAYER_INTERACTED_WITH_ENTITY));
        assertTrue(DeniedCriteria.consume(player, DeniedCriteria.Kind.ITEM_USED_ON_BLOCK));
        for (var kind : DeniedCriteria.Kind.values()) assertFalse(DeniedCriteria.consume(player, kind));
    }

    @Test
    void staleMarksDoNotSuppress() {
        UUID player = UUID.randomUUID();
        DeniedCriteria.markDenied(player);
        assertFalse(DeniedCriteria.consume(player, DeniedCriteria.Kind.PLAYER_INTERACTED_WITH_ENTITY,
            System.nanoTime() + DeniedCriteria.WINDOW_NANOS + 1));
        assertFalse(DeniedCriteria.consume(player, DeniedCriteria.Kind.PLAYER_INTERACTED_WITH_ENTITY));
        DeniedCriteria.markDenied(null);
        assertFalse(DeniedCriteria.consume(null, DeniedCriteria.Kind.ANY_BLOCK_USE));
    }
}
