package com.diskree.achievetodo.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateWorldContinuationGateTest {

    @Test
    void successfulPreparationResumesExactlyOnce() {
        CreateWorldContinuationGate gate = new CreateWorldContinuationGate();

        gate.beginUserCreate();
        assertTrue(gate.hasPendingUserCreate());
        assertFalse(gate.shouldResumeWhenCreateScreenReopens());

        gate.markDatapacksReady();
        assertTrue(gate.shouldResumeWhenCreateScreenReopens());

        gate.consumeResumedCreate();
        assertFalse(gate.hasPendingUserCreate());
        assertFalse(gate.shouldResumeWhenCreateScreenReopens());
    }

    @Test
    void failedOrCanceledPreparationDoesNotResumeCreate() {
        CreateWorldContinuationGate gate = new CreateWorldContinuationGate();

        gate.beginUserCreate();
        gate.clear();

        assertFalse(gate.hasPendingUserCreate());
        assertFalse(gate.shouldResumeWhenCreateScreenReopens());
    }

    @Test
    void nextIndependentAttemptStartsFresh() {
        CreateWorldContinuationGate gate = new CreateWorldContinuationGate();

        gate.beginUserCreate();
        gate.markDatapacksReady();
        gate.consumeResumedCreate();

        gate.beginUserCreate();
        assertTrue(gate.hasPendingUserCreate());
        assertFalse(gate.shouldResumeWhenCreateScreenReopens());
    }
}
