package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.AdvancementsTutorialProgress;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MovementTutorialStepHandlerMixinTest {

    @TempDir Path directory;
    private AdvancementsTutorialProgress progress() throws Exception {
        return new AdvancementsTutorialProgress(directory.resolve("early-completion"));
    }
    @Test void incompleteTutorialShowsOnlyOneToastAndCompletionHidesExactlyOnce() throws Exception {
        var state = progress();
        var shown = new AtomicInteger();
        var hidden = new AtomicInteger();
        assertTrue(state.mayShowToast());
        state.showToast(shown::incrementAndGet, hidden::incrementAndGet);
        state.showToast(shown::incrementAndGet, hidden::incrementAndGet);
        assertEquals(1, shown.get());
        state.opened(true, () -> {});
        state.clear();
        assertEquals(1, hidden.get());
        assertFalse(state.mayShowToast());
    }
    @Test void clearHidesToastWithoutAcknowledgingAnUnfinishedTutorial() throws Exception {
        var state = progress();
        var hidden = new AtomicInteger();
        state.showToast(() -> {}, hidden::incrementAndGet);
        state.clear();
        state.clear();
        assertEquals(1, hidden.get());
        assertTrue(state.mayShowToast());
        assertTrue(progress().mayShowToast());
    }
    @Test void earlyOpenSurvivesReconstructionWithoutSkippingMovementAndLook() throws Exception {
        var advances = new AtomicInteger();
        progress().opened(false, advances::incrementAndGet);
        assertEquals(0, advances.get());
        var reconstructed = progress();
        assertFalse(reconstructed.mayShowToast());
        reconstructed.advanceIfReady(false, advances::incrementAndGet);
        assertEquals(0, advances.get());
        reconstructed.advanceIfReady(true, advances::incrementAndGet);
        for (int tick = 0; tick < 100; tick++) reconstructed.advanceIfReady(true, advances::incrementAndGet);
        assertEquals(1, advances.get());
    }
    @Test void normalOpenCallsVanillaPersistenceImmediatelyAndDoesNotCreateDedicatedReceipt() throws Exception {
        var tutorial = new PersistentTutorialFixture(directory.resolve("options.txt"));
        var handler = handler(progress(), tutorial, 41, 42);
        handler.achievetodo$onAdvancementsOpened();
        handler.achievetodo$onAdvancementsOpened();
        assertEquals(1, tutorial.saves);
        assertEquals(TutorialSteps.FIND_TREE, new PersistentTutorialFixture(tutorial.options).step);
        assertFalse(Files.exists(directory.resolve("early-completion")));
    }
    @Test void mixinDoesNotAdvanceIncompleteVanillaPrerequisitesButPersistsAcknowledgement() throws Exception {
        var tutorial = new PersistentTutorialFixture(directory.resolve("options.txt"));
        handler(progress(), tutorial, -1, 42).achievetodo$onAdvancementsOpened();
        assertEquals(0, tutorial.saves);
        assertEquals(TutorialSteps.MOVEMENT, tutorial.step);
        assertFalse(progress().mayShowToast());
    }
    @Test void vanillaTransitionAndClearDoNotRepeatOnSubsequentTicks() throws Exception {
        var state = progress();
        var hides = new AtomicInteger();
        var advances = new AtomicInteger();
        state.showToast(() -> {}, hides::incrementAndGet);
        state.opened(true, advances::incrementAndGet);
        for (int tick = 0; tick < 100; tick++) state.advanceIfReady(true, advances::incrementAndGet);
        assertEquals(1, advances.get());
        assertEquals(1, hides.get());
    }
    private MovementTutorialStepHandlerMixin handler(AdvancementsTutorialProgress state, Tutorial tutorial,
        int moved, int looked) throws Exception {
        var handler = new MovementTutorialStepHandlerMixin();
        for (var entry : java.util.Map.of("advancementsProgress", state, "tutorial", tutorial,
            "moveCompleted", moved, "lookCompleted", looked).entrySet()) {
            var field = MovementTutorialStepHandlerMixin.class.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(handler, entry.getValue());
        }
        return handler;
    }
    // Models audited vanilla setStep -> options.save. Graphical restart/rendering remains manual.
    private static final class PersistentTutorialFixture extends Tutorial {
        final Path options;
        TutorialSteps step;
        int saves;
        PersistentTutorialFixture(Path options) throws Exception {
            super(null, null);
            this.options = options;
            step = Files.exists(options) ? TutorialSteps.getByName(Files.readString(options)) : TutorialSteps.MOVEMENT;
        }
        @Override public boolean isSurvival() { return true; }
        @Override public void setStep(TutorialSteps step) {
            this.step = step;
            saves++;
            try { Files.writeString(options, step.getName()); }
            catch (Exception exception) { throw new AssertionError(exception); }
        }
    }
}
