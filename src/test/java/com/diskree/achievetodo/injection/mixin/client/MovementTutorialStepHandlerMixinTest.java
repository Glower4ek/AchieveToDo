package com.diskree.achievetodo.injection.mixin.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementTutorialStepHandlerMixinTest {

    @Test
    void openingAdvancementsRecordsAcknowledgementEvenBeforeToastAppears() {
        MovementTutorialStepHandlerMixin handler = new MovementTutorialStepHandlerMixin();
        assertFalse(readAdvancementsOpened(handler));
        handler.achievetodo$onAdvancementsOpened();
        assertTrue(readAdvancementsOpened(handler));
    }

    @Test
    void openingAdvancementsRepeatedlyKeepsAcknowledgementRecorded() {
        MovementTutorialStepHandlerMixin handler = new MovementTutorialStepHandlerMixin();
        handler.achievetodo$onAdvancementsOpened();
        handler.achievetodo$onAdvancementsOpened();
        assertTrue(readAdvancementsOpened(handler));
    }

    private static boolean readAdvancementsOpened(MovementTutorialStepHandlerMixin handler) {
        try {
            Field field = MovementTutorialStepHandlerMixin.class.getDeclaredField("isAdvancementsOpened");
            field.setAccessible(true);
            return field.getBoolean(handler);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
