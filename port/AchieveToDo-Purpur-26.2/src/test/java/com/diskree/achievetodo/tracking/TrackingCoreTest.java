package com.diskree.achievetodo.tracking;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.AbilityRequirements;
import com.diskree.achievetodo.progression.PlayerProgressionState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class TrackingCoreTest {

    private TrackingCoreTest() {
    }

    public static void run() {
        testObjectiveDetectionPriority();
        testProgressionCountResolution();
        testTrackedScoreFixups();
        testTrackedStatisticsNormalization();
        testNearbyEntityModel();
        testUnavailablePlayerState();
        testProgressionSynchronization();
    }

    private static void testObjectiveDetectionPriority() {
        BacapObjectiveDetector detector = new BacapObjectiveDetector();
        Map<ProgressionDisplaySlot, String> slotObjectives = Map.of(
            ProgressionDisplaySlot.BELOW_NAME, "bac_advancements",
            ProgressionDisplaySlot.LIST, "bac_advfirst",
            ProgressionDisplaySlot.SIDEBAR, "bac_advancements_team"
        );
        Optional<DetectedAdvancementsObjective> detected = detector.detect(slotObjectives);
        assertTrue(detected.isPresent());
        assertEquals(AdvancementsMode.TEAM_ADVANCEMENTS, detected.get().mode());
        assertEquals(ProgressionDisplaySlot.SIDEBAR, detected.get().displaySlot());

        detected = detector.detect(Map.of(ProgressionDisplaySlot.LIST, "bac_advfirst_team"));
        assertTrue(detected.isPresent());
        assertEquals(AdvancementsMode.FIRST_ADVANCEMENTS_IN_TEAM, detected.get().mode());

        detected = detector.detect(Map.of(ProgressionDisplaySlot.SIDEBAR, "unrelated"));
        assertFalse(detected.isPresent());
    }

    private static void testProgressionCountResolution() {
        ProgressionCountResolver resolver = new ProgressionCountResolver();
        Map<String, Integer> scores = Map.of(
            "alice|bac_advancements", 37,
            "alice|bac_advancements_team", 10,
            "bob|bac_advancements_team", 12
        );
        ScoreAccess scoreAccess = (entry, objectiveName) -> {
            Integer score = scores.get(entry + "|" + objectiveName);
            return score == null ? OptionalInt.empty() : OptionalInt.of(score);
        };

        int personal = resolver.resolvePersonalCount("bac_advancements", "alice", scoreAccess);
        assertEquals(37, personal);

        Optional<TeamProgressionSnapshot> teamProgress = resolver.resolveTeamProgress(
            "bac_advancements_team",
            "team-red",
            Set.of("alice", "bob"),
            scoreAccess
        );
        assertTrue(teamProgress.isPresent());
        assertEquals(22, teamProgress.get().obtainedAdvancementCount());

        DetectedAdvancementsObjective teamObjective = new DetectedAdvancementsObjective(
            AdvancementsMode.TEAM_ADVANCEMENTS,
            "bac_advancements_team",
            ProgressionDisplaySlot.SIDEBAR
        );
        assertEquals(
            22,
            resolver.resolveObtainedAdvancementCount(teamObjective, "alice", "team-red", Set.of("alice", "bob"), scoreAccess).getAsInt()
        );
        assertEquals(0, resolver.resolveObtainedAdvancementCount(teamObjective, "alice", null, Set.of(), scoreAccess).getAsInt());
    }

    private static void testTrackedScoreFixups() {
        Map<String, Integer> scores = Map.of(
            "alice|bac_oar_eligible_x", 1,
            "alice|bac_oar_current_x", -640,
            "bob|bac_oar_eligible_x", 0,
            "bob|bac_oar_eligible_z", 1,
            "bob|bac_oar_current_z", 720
        );
        ScoreAccess scoreAccess = (entry, objectiveName) -> {
            Integer score = scores.get(entry + "|" + objectiveName);
            return score == null ? OptionalInt.empty() : OptionalInt.of(score);
        };

        assertEquals(640, TrackedScoreType.ON_A_RAIL.fixScore(scoreAccess, "alice", 0));
        assertEquals(720, TrackedScoreType.ON_A_RAIL.fixScore(scoreAccess, "bob", 0));
        assertEquals(1, TrackedScoreType.LOSER.fixScore(scoreAccess, "alice", 10));
        assertEquals(0, TrackedScoreType.LOSER.fixScore(scoreAccess, "alice", 11));
        assertEquals(50, TrackedScoreType.ROCKETMAN.normalizeProgress(1184));
        assertTrue(TrackedScoreType.findByObjectiveName("bac_underwater").contains(TrackedScoreType.FREE_DIVER));
        assertTrue(TrackedScoreType.findByObjectiveName("bac_underwater").contains(TrackedScoreType.SLEEP_WITH_THE_FISHES));
    }

    private static void testTrackedStatisticsNormalization() {
        Set<TrackedStatisticsDataType> walkStats = TrackedStatisticsDataType.findByStatisticKey(TrackedStatisticKey.custom("walk_one_cm"));
        assertEquals(3, walkStats.size());
        assertTrue(walkStats.contains(TrackedStatisticsDataType.OUT_FOR_A_STROLL));
        assertTrue(walkStats.contains(TrackedStatisticsDataType.WHO_NEEDS_CARS));
        assertTrue(walkStats.contains(TrackedStatisticsDataType.I_ENJOY_LONG_WALKS_AND_PLAYING_MINECRAFT));
        assertEquals(50, TrackedStatisticsDataType.BULLDOZER.normalizeProgress(5_000));
        assertEquals(250, TrackedStatisticsDataType.CUPID.normalizeProgress(250));
        assertEquals(Optional.of(TrackedStatisticsDataType.INSOMNIAC), TrackedStatisticsDataType.findByAdvancementId("blazeandcave:building/insomniac"));
    }

    private static void testNearbyEntityModel() {
        assertEquals(10, TrackedNearbyEntitiesType.FAMILY_REUNION.getEntitiesCount());
        assertTrue(TrackedNearbyEntitiesType.findByAdvancementId("blazeandcave:monsters/bone_to_party").isPresent());

        EnumMap<TrackedNearbyEntitiesType, Integer> progress = NearbyEntityProgressCalculator.calculate(List.of(
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("COW", false, 4.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("cow", false, 9.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("sheep", false, 32.0 * 32.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("frog", false, 32.0 * 32.0 + 1.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("zombie", false, 25.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("zombie", true, 25.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("husk", false, 26.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("skeleton", false, 16.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("skeleton", true, 4.0),
            new NearbyEntityProgressCalculator.ObservedNearbyEntity("wither_skeleton", false, 1.0)
        ));
        assertEquals(2, progress.get(TrackedNearbyEntitiesType.ANIMAL_KINGDOM));
        assertEquals(2, progress.get(TrackedNearbyEntitiesType.FAMILY_REUNION));
        assertEquals(2, progress.get(TrackedNearbyEntitiesType.BONE_TO_PARTY));
    }

    private static void testUnavailablePlayerState() {
        AbilityRequirements requirements = requirementsFor(AbilityType.OPEN_CHEST, 0);
        PlayerProgressionState unavailable = PlayerProgressionState.unavailable(UUID.randomUUID(), requirements);
        assertFalse(unavailable.isReady());
        assertFalse(unavailable.getAbilityState(AbilityType.OPEN_CHEST).isUnlocked());
    }

    private static void testProgressionSynchronization() {
        AbilityRequirements requirements = requirementsFor(AbilityType.OPEN_CHEST, 10);
        UUID playerId = UUID.randomUUID();
        PlayerProgressionState locked = PlayerProgressionState.create(playerId, 9, requirements);
        PlayerProgressionUpdate unlocked = ProgressionStateSynchronizer.synchronize(locked, 10, true);
        assertTrue(unlocked.unlockedAbilities().contains(AbilityType.OPEN_CHEST));
        assertFalse(unlocked.relockedAbilities().contains(AbilityType.OPEN_CHEST));

        PlayerProgressionState demystifiedUnlocked = unlocked.currentState().withDemystified(AbilityType.OPEN_CHEST);
        PlayerProgressionUpdate relocked = ProgressionStateSynchronizer.synchronize(demystifiedUnlocked, 8, true);
        assertTrue(relocked.relockedAbilities().contains(AbilityType.OPEN_CHEST));
        assertTrue(relocked.currentState().getAbilityState(AbilityType.OPEN_CHEST).demystified());
    }

    private static AbilityRequirements requirementsFor(AbilityType targetAbility, int requiredCount) {
        Map<AbilityType, Integer> requirements = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            requirements.put(abilityType, 0);
        }
        requirements.put(targetAbility, requiredCount);
        return AbilityRequirements.from(requirements);
    }
}
