package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.BlockBox;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.landmark.ChunkCoordinate;
import com.diskree.achievetodo.landmark.LandmarkChunkKey;
import com.diskree.achievetodo.landmark.LandmarkEngine;
import com.diskree.achievetodo.landmark.LandmarkPersistence;
import com.diskree.achievetodo.landmark.LandmarkRecord;
import com.diskree.achievetodo.landmark.LandmarkSource;
import com.diskree.achievetodo.landmark.LandmarkSourceKind;
import com.diskree.achievetodo.landmark.LandmarkWorld;
import com.diskree.achievetodo.progression.AbilityRequirements;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ProgressionConstants;
import com.diskree.achievetodo.tests.TestSupport;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RestrictionEngineTest {

    private RestrictionEngineTest() {
    }

    public static void run() {
        verifyAllowDecision();
        verifyAbilityDenyDecision();
        verifyLandmarkDenyDecision();
        verifyBothDenyDecision();
        verifyInitiallyUnlockedAbility();
        verifyPermanentlyLockedAbility();
        verifyRelockAfterProgressLoss();
        verifyDifferentDimensionsDoNotCrossMatch();
        verifyOverlappingLandmarksCollectAllTypes();
        verifyMultipleLandmarksSameTargetStayStable();
        verifyOverlapPrimaryLandmarkIsDeterministic();
        verifyDifferentPlayersCanObserveDifferentRestrictionStates();
    }

    private static void verifyAllowDecision() {
        RestrictionHarness harness = new RestrictionHarness();
        PlayerProgressionState state = harness.playerState(AbilityType.SHOOT_BOW, 10, 10);
        RestrictionDecision decision = harness.engine.check(
            state,
            RestrictionContext.builder(state.getPlayerId(), RestrictionActionKind.ITEM_USE)
                .actorMode(RestrictionActorMode.SURVIVAL)
                .abilityCandidate(AbilityType.SHOOT_BOW)
                .build()
        );
        TestSupport.assertEquals(RestrictionDecisionKind.ALLOW, decision.kind());
    }

    private static void verifyAbilityDenyDecision() {
        RestrictionHarness harness = new RestrictionHarness();
        PlayerProgressionState state = harness.playerState(AbilityType.SHOOT_BOW, 3, 8).withDemystified(AbilityType.SHOOT_BOW);
        RestrictionDecision decision = harness.engine.check(
            state,
            RestrictionContext.builder(state.getPlayerId(), RestrictionActionKind.ITEM_USE)
                .actorMode(RestrictionActorMode.SURVIVAL)
                .abilityCandidate(AbilityType.SHOOT_BOW)
                .build()
        );
        TestSupport.assertEquals(RestrictionDecisionKind.DENY_ABILITY, decision.kind());
        TestSupport.assertEquals(AbilityType.SHOOT_BOW, decision.ability());
    }

    private static void verifyLandmarkDenyDecision() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = harness.playerState(AbilityType.INTERACT_INSIDE_VILLAGE, 1, 4);

        RestrictionDecision decision = harness.engine.check(
            state,
            RestrictionContext.builder(state.getPlayerId(), RestrictionActionKind.BLOCK_INTERACT)
                .actorMode(RestrictionActorMode.SURVIVAL)
                .target(RestrictionTarget.block(overworld, new BlockBox(1, 64, 1, 1, 64, 1), "chest"))
                .build()
        );

        TestSupport.assertEquals(RestrictionDecisionKind.DENY_LANDMARK, decision.kind());
        TestSupport.assertEquals(LandmarkType.VILLAGE, decision.landmark());
        TestSupport.assertEquals(Set.of(LandmarkType.VILLAGE), decision.deniedLandmarks());
    }

    private static void verifyBothDenyDecision() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = harness.playerState(
            Map.of(
                AbilityType.OPEN_CHEST, 10,
                AbilityType.INTERACT_INSIDE_VILLAGE, 10
            ),
            2
        );

        RestrictionDecision decision = harness.engine.check(
            state,
            RestrictionContext.builder(state.getPlayerId(), RestrictionActionKind.BLOCK_INTERACT)
                .actorMode(RestrictionActorMode.SURVIVAL)
                .abilityCandidate(AbilityType.OPEN_CHEST)
                .target(RestrictionTarget.block(overworld, new BlockBox(1, 64, 1, 1, 64, 1), "chest"))
                .build()
        );

        TestSupport.assertEquals(RestrictionDecisionKind.DENY_BOTH, decision.kind());
        TestSupport.assertEquals(AbilityType.OPEN_CHEST, decision.ability());
        TestSupport.assertEquals(LandmarkType.VILLAGE, decision.landmark());
    }

    private static void verifyInitiallyUnlockedAbility() {
        RestrictionHarness harness = new RestrictionHarness();
        PlayerProgressionState state = harness.playerState(AbilityType.OPEN_CHEST, 0, ProgressionConstants.INITIALLY_UNLOCKED_FLAG);
        RestrictionDecision decision = harness.engine.checkAbility(state, AbilityType.OPEN_CHEST, RestrictionActorMode.SURVIVAL);
        TestSupport.assertEquals(RestrictionDecisionKind.ALLOW, decision.kind());
    }

    private static void verifyPermanentlyLockedAbility() {
        RestrictionHarness harness = new RestrictionHarness();
        PlayerProgressionState state = harness.playerState(
            AbilityType.OPEN_CHEST,
            999,
            ProgressionConstants.PERMANENTLY_LOCKED_FLAG
        );
        RestrictionDecision decision = harness.engine.checkAbility(state, AbilityType.OPEN_CHEST, RestrictionActorMode.SURVIVAL);
        TestSupport.assertEquals(RestrictionDecisionKind.DENY_ABILITY, decision.kind());
    }

    private static void verifyRelockAfterProgressLoss() {
        RestrictionHarness harness = new RestrictionHarness();
        PlayerProgressionState unlockedState = harness.playerState(AbilityType.OPEN_CHEST, 12, 10);
        TestSupport.assertEquals(
            RestrictionDecisionKind.ALLOW,
            harness.engine.checkAbility(unlockedState, AbilityType.OPEN_CHEST, RestrictionActorMode.SURVIVAL).kind()
        );

        PlayerProgressionState relockedState = unlockedState.withObtainedAdvancementCount(2);
        TestSupport.assertEquals(
            RestrictionDecisionKind.DENY_ABILITY,
            harness.engine.checkAbility(relockedState, AbilityType.OPEN_CHEST, RestrictionActorMode.SURVIVAL).kind()
        );
    }

    private static void verifyDifferentDimensionsDoNotCrossMatch() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        LandmarkWorld nether = harness.nether();
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = harness.playerState(AbilityType.INTERACT_INSIDE_VILLAGE, 0, 4);

        RestrictionDecision decision = harness.engine.checkLandmark(
            state,
            nether,
            new BlockBox(1, 64, 1, 1, 64, 1),
            RestrictionActorMode.SURVIVAL
        );
        TestSupport.assertEquals(RestrictionDecisionKind.ALLOW, decision.kind());
    }

    private static void verifyOverlappingLandmarksCollectAllTypes() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        BlockBox overlap = new BlockBox(4, 60, 4, 6, 72, 6);
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        harness.register(overworld, LandmarkType.SHIPWRECK, new BlockBox(4, 60, 4, 12, 72, 12), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = harness.playerState(
            Map.of(
                AbilityType.INTERACT_INSIDE_VILLAGE, 5,
                AbilityType.INTERACT_INSIDE_SHIPWRECK, 5
            ),
            0
        );

        RestrictionDecision decision = harness.engine.checkLandmark(state, overworld, overlap, RestrictionActorMode.SURVIVAL);
        TestSupport.assertEquals(RestrictionDecisionKind.DENY_LANDMARK, decision.kind());
        TestSupport.assertEquals(Set.of(LandmarkType.VILLAGE, LandmarkType.SHIPWRECK), decision.deniedLandmarks());
    }

    private static void verifyMultipleLandmarksSameTargetStayStable() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        BlockBox target = new BlockBox(2, 64, 2, 2, 64, 2);
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(1, 60, 1, 6, 72, 6), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = harness.playerState(AbilityType.INTERACT_INSIDE_VILLAGE, 1, 3);

        RestrictionDecision decision = harness.engine.checkLandmark(state, overworld, target, RestrictionActorMode.SURVIVAL);
        TestSupport.assertEquals(RestrictionDecisionKind.DENY_LANDMARK, decision.kind());
        TestSupport.assertEquals(Set.of(LandmarkType.VILLAGE), decision.deniedLandmarks());
    }

    private static void verifyOverlapPrimaryLandmarkIsDeterministic() {
        RestrictionHarness firstHarness = new RestrictionHarness();
        LandmarkWorld overworld = firstHarness.overworld();
        BlockBox target = new BlockBox(4, 64, 4, 4, 64, 4);
        firstHarness.register(overworld, LandmarkType.SHIPWRECK, new BlockBox(2, 60, 2, 8, 72, 8), new ChunkCoordinate(0, 0));
        firstHarness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        PlayerProgressionState state = firstHarness.playerState(
            Map.of(
                AbilityType.INTERACT_INSIDE_VILLAGE, 5,
                AbilityType.INTERACT_INSIDE_SHIPWRECK, 5
            ),
            0
        );
        RestrictionDecision firstDecision = firstHarness.engine.checkLandmark(state, overworld, target, RestrictionActorMode.SURVIVAL);

        RestrictionHarness secondHarness = new RestrictionHarness();
        secondHarness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));
        secondHarness.register(overworld, LandmarkType.SHIPWRECK, new BlockBox(2, 60, 2, 8, 72, 8), new ChunkCoordinate(0, 0));
        RestrictionDecision secondDecision = secondHarness.engine.checkLandmark(state, overworld, target, RestrictionActorMode.SURVIVAL);

        TestSupport.assertEquals(firstDecision.landmark(), secondDecision.landmark());
        TestSupport.assertEquals(firstDecision.deniedLandmarks(), secondDecision.deniedLandmarks());
    }

    private static void verifyDifferentPlayersCanObserveDifferentRestrictionStates() {
        RestrictionHarness harness = new RestrictionHarness();
        LandmarkWorld overworld = harness.overworld();
        harness.register(overworld, LandmarkType.VILLAGE, new BlockBox(0, 60, 0, 8, 72, 8), new ChunkCoordinate(0, 0));

        PlayerProgressionState lockedState = harness.playerState(AbilityType.INTERACT_INSIDE_VILLAGE, 0, 4);
        PlayerProgressionState unlockedState = harness.playerState(AbilityType.INTERACT_INSIDE_VILLAGE, 4, 4);

        RestrictionContext context = RestrictionContext.builder(null, RestrictionActionKind.ENTITY_INTERACT)
            .actorMode(RestrictionActorMode.SURVIVAL)
            .target(RestrictionTarget.area(overworld, new BlockBox(1, 64, 1, 2, 65, 2), "villager"))
            .build();

        TestSupport.assertEquals(RestrictionDecisionKind.DENY_LANDMARK, harness.engine.check(lockedState, context).kind());
        TestSupport.assertEquals(RestrictionDecisionKind.ALLOW, harness.engine.check(unlockedState, context).kind());
    }

    private static final class RestrictionHarness {

        private final InMemoryPersistence persistence = new InMemoryPersistence();
        private final LandmarkEngine landmarkEngine = new LandmarkEngine(persistence);
        private final RestrictionEngine engine = new RestrictionEngine(landmarkEngine);

        private LandmarkWorld overworld() {
            return new LandmarkWorld("world", DimensionType.OVERWORLD);
        }

        private LandmarkWorld nether() {
            return new LandmarkWorld("world_nether", DimensionType.NETHER);
        }

        private PlayerProgressionState playerState(AbilityType abilityType, int obtainedCount, int requiredCount) {
            return playerState(Map.of(abilityType, requiredCount), obtainedCount);
        }

        private PlayerProgressionState playerState(Map<AbilityType, Integer> overrides, int obtainedCount) {
            EnumMap<AbilityType, Integer> requirements = new EnumMap<>(AbilityType.class);
            for (AbilityType abilityType : AbilityType.values()) {
                requirements.put(abilityType, ProgressionConstants.INITIALLY_UNLOCKED_FLAG);
            }
            requirements.putAll(overrides);
            return PlayerProgressionState.create(UUID.randomUUID(), obtainedCount, AbilityRequirements.from(requirements));
        }

        private void register(
            LandmarkWorld world,
            LandmarkType landmarkType,
            BlockBox box,
            ChunkCoordinate sourceChunk
        ) {
            LandmarkRecord record = new LandmarkRecord(
                landmarkType,
                world,
                box,
                new LandmarkSource(LandmarkSourceKind.STRUCTURE, "test:" + landmarkType.getName(), sourceChunk)
            );
            try {
                landmarkEngine.registerLandmark(record);
                for (ChunkCoordinate chunkCoordinate : record.intersectingChunks()) {
                    landmarkEngine.loadChunk(world, chunkCoordinate);
                }
            } catch (IOException exception) {
                throw new AssertionError("Unexpected test IO failure", exception);
            }
        }
    }

    private static final class InMemoryPersistence implements LandmarkPersistence {

        private final Map<LandmarkChunkKey, Set<LandmarkRecord>> slices = new LinkedHashMap<>();

        @Override
        public Map<ChunkCoordinate, Set<LandmarkRecord>> loadWorld(LandmarkWorld world) {
            Map<ChunkCoordinate, Set<LandmarkRecord>> results = new LinkedHashMap<>();
            for (Map.Entry<LandmarkChunkKey, Set<LandmarkRecord>> entry : slices.entrySet()) {
                if (entry.getKey().world().equals(world)) {
                    results.put(entry.getKey().chunk(), Set.copyOf(entry.getValue()));
                }
            }
            return results;
        }

        @Override
        public void saveWorld(LandmarkWorld world, Map<ChunkCoordinate, ? extends Collection<LandmarkRecord>> chunks) {
            slices.entrySet().removeIf(entry -> entry.getKey().world().equals(world));
            for (Map.Entry<ChunkCoordinate, ? extends Collection<LandmarkRecord>> entry : chunks.entrySet()) {
                saveChunk(world, entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Set<LandmarkRecord> loadChunk(LandmarkWorld world, ChunkCoordinate chunk) {
            return Set.copyOf(slices.getOrDefault(new LandmarkChunkKey(world, chunk), Set.of()));
        }

        @Override
        public void saveChunk(LandmarkWorld world, ChunkCoordinate chunk, Collection<LandmarkRecord> records) {
            LandmarkChunkKey key = new LandmarkChunkKey(world, chunk);
            if (records.isEmpty()) {
                slices.remove(key);
                return;
            }
            slices.put(key, new LinkedHashSet<>(records));
        }
    }
}
