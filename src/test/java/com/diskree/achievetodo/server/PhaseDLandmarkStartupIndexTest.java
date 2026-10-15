package com.diskree.achievetodo.server;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Executes the production pending-state algorithm. Opaque native identities are not gameplay fixtures. */
class PhaseDLandmarkStartupIndexTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private final AchieveToDoServer.PendingLandmarkChunks pending = new AchieveToDoServer.PendingLandmarkChunks();
    private final ServerLevel world = opaque(ServerLevel.class);
    private final ChunkAccess chunk = opaque(LevelChunk.class);
    private final ChunkPos pos = new ChunkPos(15, 15);
    private final List<ChunkAccess> admitted = new ArrayList<>();

    private static <T> T opaque(Class<T> type) {
        try {
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field singleton = unsafe.getDeclaredField("theUnsafe");
            singleton.setAccessible(true);
            return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void observedChunkWaitsForLegitimateAvailabilityAndAdmitsOnce() {
        pending.observe(world, pos, chunk);
        pending.replay((w, p) -> null, (w, c) -> admitted.add(c));
        assertTrue(admitted.isEmpty());
        pending.replay((w, p) -> { assertSame(world, w); assertEquals(pos, p); return chunk; },
            (w, c) -> admitted.add(c));
        pending.replay((w, p) -> fail("Consumed observation must not be revisited"), (w, c) -> admitted.add(c));
        assertEquals(List.of(chunk), admitted);
    }

    @Test
    void unloadBeforeAdmissionCancelsObservation() {
        pending.observe(world, pos, chunk);
        pending.remove(world, pos);
        pending.replay((w, p) -> fail("Unloaded observation retained"), (w, c) -> admitted.add(c));
        assertTrue(admitted.isEmpty());
    }

    @Test
    void repeatedLoadAndReplayDoNotDuplicateAdmission() {
        pending.observe(world, pos, chunk);
        pending.observe(world, new ChunkPos(15, 15), chunk);
        pending.replay((w, p) -> chunk, (w, c) -> admitted.add(c));
        pending.replay((w, p) -> chunk, (w, c) -> admitted.add(c));
        assertEquals(List.of(chunk), admitted);
    }

    @Test
    void samePositionInDistinctWorldsIsIndependent() {
        ServerLevel other = opaque(ServerLevel.class);
        ChunkAccess otherChunk = opaque(LevelChunk.class);
        pending.observe(world, pos, chunk);
        pending.observe(other, pos, otherChunk);
        pending.remove(world, pos);
        pending.replay((w, p) -> { assertSame(other, w); return otherChunk; }, (w, c) -> admitted.add(c));
        assertEquals(List.of(otherChunk), admitted);
    }

    @Test
    void replacedNativeChunkDiscardsStaleIdentityWithoutAdmission() {
        pending.observe(world, pos, chunk);
        pending.replay((w, p) -> opaque(LevelChunk.class), (w, c) -> admitted.add(c));
        pending.replay((w, p) -> fail("Stale observation retained"), (w, c) -> admitted.add(c));
        assertTrue(admitted.isEmpty());
    }

    @Test
    void latestObservedLoadReplacesPreviousObject() {
        ChunkAccess replacement = opaque(LevelChunk.class);
        pending.observe(world, pos, chunk);
        pending.observe(world, pos, replacement);
        pending.replay((w, p) -> replacement, (w, c) -> admitted.add(c));
        assertEquals(List.of(replacement), admitted);
    }

    @Test
    void observationIsConsumedBeforeAdmissionCallback() {
        pending.observe(world, pos, chunk);
        pending.replay((w, p) -> chunk, (w, c) -> {
            pending.replay((otherWorld, otherPos) -> fail("Reentrant duplicate"), (otherWorld, otherChunk) -> fail());
            admitted.add(c);
        });
        assertEquals(List.of(chunk), admitted);
    }

    @Test
    void stopClearAndRepeatedLifecycleDoNotRetainOldWorlds() {
        for (int i = 0; i < 3; i++) {
            pending.observe(opaque(ServerLevel.class), pos, opaque(LevelChunk.class));
            pending.clear();
            pending.replay((w, p) -> fail("Stopped world retained"), (w, c) -> fail());
        }
        pending.observe(world, pos, chunk);
        pending.replay((w, p) -> chunk, (w, c) -> admitted.add(c));
        assertEquals(List.of(chunk), admitted);
    }

    @Test
    void unrelatedUnloadCannotCancelAnotherWorldOrPosition() {
        pending.observe(world, pos, chunk);
        pending.remove(opaque(ServerLevel.class), pos);
        pending.remove(world, new ChunkPos(16, 15));
        pending.replay((w, p) -> chunk, (w, c) -> admitted.add(c));
        assertEquals(List.of(chunk), admitted);
    }
}
