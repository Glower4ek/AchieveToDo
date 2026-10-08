package com.diskree.achievetodo.server;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ObtainedCountReconciliationTest {

    @BeforeAll
    static void initializeMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static class Fixture {
        final Scoreboard scoreboard = new Scoreboard();
        final AchieveToDoServer owner = new AchieveToDoServer();
        final Objective selected;
        final Method reader;

        Fixture(AdvancementsMode mode) throws Exception {
            selected = objective("selected");
            owner.currentAdvancementsMode = mode;
            owner.currentScoreboardObjective = selected;
            reader = AchieveToDoServer.class.getDeclaredMethod(
                "computeCurrentObtainedCount", Scoreboard.class, String.class
            );
            reader.setAccessible(true);
        }

        Objective objective(String name) {
            return scoreboard.addObjective(
                name, ObjectiveCriteria.DUMMY, Component.literal(name),
                ObjectiveCriteria.RenderType.INTEGER, false, null
            );
        }

        void score(String name, int value) {
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), selected).set(value);
        }

        int count(String name) throws Exception {
            return (int) reader.invoke(owner, scoreboard, name);
        }

        PlayerTeam team(String name, String... members) {
            PlayerTeam team = scoreboard.addPlayerTeam(name);
            for (String member : members) {
                scoreboard.addPlayerToTeam(member, team);
            }
            return team;
        }
    }

    @Test
    void soloLastScoreRemovalAndRebuildReadCurrentTruth() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.DEFAULT);
        assertEquals(0, f.count("A"));
        assertNull(f.scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("A"), f.selected));
        f.score("A", 6);
        assertEquals(6, f.count("A"));
        f.scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly("A"), f.selected);
        assertEquals(0, f.count("A"));
        f.score("A", 6);
        assertEquals(6, f.count("A"));
    }

    @Test
    void soloRemovalWithOtherScoreAndAllHolderResetReadZero() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.FIRST_ADVANCEMENTS);
        Objective other = f.objective("other");
        f.scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("A"), other).set(9);
        f.score("A", 6);
        f.scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly("A"), f.selected);
        assertEquals(0, f.count("A"));
        assertEquals(9, f.scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("A"), other).value());
        f.score("A", 6);
        f.scoreboard.resetAllPlayerScores(ScoreHolder.forNameOnly("A"));
        assertEquals(0, f.count("A"));
    }

    @Test
    void allFourTeamModesConvergeOnMemberSumAndSurvivingContribution() throws Exception {
        int modes = 0;
        for (AdvancementsMode mode : AdvancementsMode.values()) {
            if (!mode.isTeamsMode()) {
                continue;
            }
            modes++;
            Fixture f = new Fixture(mode);
            f.team("team", "A", "B");
            f.score("A", 3);
            f.score("B", 1);
            assertEquals(4, f.count("A"), mode.name());
            f.score("B", 2);
            assertEquals(5, f.count("A"), mode.name());
            assertEquals(5, f.count("B"), mode.name());
            f.score("nonmember", 12);
            assertEquals(5, f.count("A"), mode.name());
            f.scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly("B"), f.selected);
            assertEquals(3, f.count("A"), mode.name());
            assertEquals(3, f.count("B"), mode.name());
        }
        assertEquals(4, modes);
    }

    @Test
    void actualFakeOfflineAndMissingMembersUseMembershipWithoutColorLookup() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.TEAM_ADVANCEMENTS);
        f.team("arbitrary", "A", "offline", "Black_Team", "missing");
        f.score("A", 3);
        f.score("offline", 7);
        f.score("Black_Team", 2);
        f.score("nonmember", 100);
        assertEquals(12, f.count("A"));
        assertEquals(12, f.count("missing"));
        assertNull(f.scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("missing"), f.selected));
    }

    @Test
    void teamMembershipMoveRemovalAndWholeTeamRemovalReadCurrentStructure() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.TOTAL_TEAM_ADVANCEMENTS);
        PlayerTeam oldTeam = f.team("old", "A", "B");
        PlayerTeam newTeam = f.team("new", "C");
        f.score("A", 3);
        f.score("B", 2);
        f.score("C", 7);
        assertEquals(5, f.count("A"));
        assertTrue(f.scoreboard.addPlayerToTeam("B", newTeam));
        assertEquals(3, f.count("A"));
        assertEquals(9, f.count("B"));
        assertEquals(9, f.count("C"));
        f.scoreboard.removePlayerFromTeam("B", newTeam);
        assertEquals(0, f.count("B"));
        assertEquals(7, f.count("C"));
        f.scoreboard.removePlayerTeam(oldTeam);
        assertTrue(oldTeam.getPlayers().contains("A"));
        assertEquals(0, f.count("A"));
    }

    @Test
    void noTeamIsZeroEvenWithSelectedScore() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.FIRST_ADVANCEMENTS_IN_TEAM);
        f.score("A", 19);
        assertEquals(0, f.count("A"));
    }

    @Test
    void nonselectedObjectiveMutationDoesNotChangeCanonicalScore() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.DEFAULT);
        Objective other = f.objective("other");
        f.score("A", 6);
        f.scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("A"), other).set(100);
        assertEquals(6, f.count("A"));
        f.scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly("A"), other);
        assertEquals(6, f.count("A"));
        f.owner.currentScoreboardObjective = other;
        assertEquals(0, f.count("A"));
    }

    @Test
    void exactScoresAndJavaIntArithmeticRemainUnnormalized() throws Exception {
        Fixture f = new Fixture(AdvancementsMode.DEFAULT);
        f.score("A", -4);
        assertEquals(-4, f.count("A"));
        f.owner.currentAdvancementsMode = AdvancementsMode.TEAM_FIRST_ADVANCEMENTS;
        f.team("team", "A", "B");
        f.score("A", Integer.MAX_VALUE);
        f.score("B", 1);
        assertEquals(Integer.MIN_VALUE, f.count("A"));
    }
}
