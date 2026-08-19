package com.diskree.achievetodo.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityJoinGateTest {

    @Test
    void initialJoinRunsPreparationButPreparedJoinDoesNotReenter() {
        CompatibilityJoinGate gate = new CompatibilityJoinGate();

        assertTrue(gate.shouldRunPreparation());

        int[] joinCount = {0};
        gate.runPreparedJoin(() -> {
            joinCount[0]++;
            assertFalse(gate.shouldRunPreparation());
        });

        assertEquals(1, joinCount[0]);
        assertTrue(gate.shouldRunPreparation());
    }

    @Test
    void failedPreparationDoesNotArmPreparedJoin() {
        CompatibilityJoinGate gate = new CompatibilityJoinGate();

        assertTrue(gate.shouldRunPreparation());
        gate.reset();

        assertTrue(gate.shouldRunPreparation());
    }

    @Test
    void preparedJoinGuardClearsWhenInternalJoinThrows() {
        CompatibilityJoinGate gate = new CompatibilityJoinGate();

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            gate.runPreparedJoin(() -> {
                throw new RuntimeException("boom");
            })
        );

        assertEquals("boom", thrown.getMessage());
        assertTrue(gate.shouldRunPreparation());
    }
}
