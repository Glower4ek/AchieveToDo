package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Live vanilla mutations own the reconciliation regression; expected totals are fixture constants. */
public final class ProgressionReconciliationGameTest implements CustomTestMethodInvoker {
    private record JoinedPlayer(ServerPlayer player, Connection connection, EmbeddedChannel channel) { }
    @FunctionalInterface
    private interface Scenario { void run() throws Exception; }

    private GameTestHelper helper;
    private String scenario;
    private long sequence;
    private final List<JoinedPlayer> players = new ArrayList<>();
    private final List<PlayerTeam> teams = new ArrayList<>();
    private final List<Objective> createdObjectives = new ArrayList<>();
    private final Map<DisplaySlot, Objective> originalDisplays = new EnumMap<>(DisplaySlot.class);

    private AchieveToDoServer owner() { return AchieveToDoMod.getServer(); }
    private ServerScoreboard scoreboard() { return helper.getLevel().getServer().getScoreboard(); }

    private void trace(String operation, Object... values) {
        System.out.println("C5_PERMANENT " + new Gson().toJson(Map.of(
            "scenario", scenario, "seq", ++sequence, "tick", helper.getLevel().getServer().getTickCount(),
            "operation", operation, "values", Arrays.stream(values).map(String::valueOf).toList()
        )));
    }

    private void require(String assertion, boolean satisfied, Object observed) {
        trace("assert", assertion, satisfied, observed);
        if (!satisfied) throw new AssertionError(assertion + ": " + observed);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @SuppressWarnings("unchecked")
    private Map<AbilityType, Integer> configuration() throws ReflectiveOperationException {
        return (Map<AbilityType, Integer>) field(owner(), "abilitiesConfiguration");
    }

    @SuppressWarnings("unchecked")
    private int cached(ServerPlayer player) throws ReflectiveOperationException {
        return ((Map<UUID, Integer>) field(owner(), "obtainedAdvancementsCountByPlayers"))
            .getOrDefault(player.getUUID(), Integer.MIN_VALUE);
    }

    private AbilityType thresholdAbility() throws ReflectiveOperationException {
        Map<AbilityType, Integer> configuration = configuration();
        return Arrays.stream(AbilityType.values()).filter(a -> configuration.get(a) == 6)
            .findFirst().orElseGet(() -> Arrays.stream(AbilityType.values())
                .filter(a -> configuration.get(a) > 0 && configuration.get(a) < 200).findFirst().orElseThrow());
    }

    private void count(ServerPlayer player, int expected) throws ReflectiveOperationException {
        require("cache equals fixture total", cached(player) == expected, cached(player));
    }

    @SuppressWarnings("unchecked")
    private void accessAndIndex(ServerPlayer player, AbilityType ability, boolean locked) throws Exception {
        require("numeric access", owner().isAbilityLocked(player, ability, true) == locked, locked);
        var advancement = helper.getLevel().getServer().getAdvancements()
            .get(AbilityAdvancementsGenerator.buildAdvancementId(ability));
        require("ability definition loaded", advancement != null, ability);
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        var unlocked = progress.getCriterion(AbilityAdvancementsGenerator.UNLOCKED_CRITERION);
        require("unlocked criterion follows threshold", unlocked != null && unlocked.isDone() != locked,
            unlocked == null ? "absent" : unlocked.isDone());
        Map<LandmarkType, Set<UUID>> index = (Map<LandmarkType, Set<UUID>>) field(owner(), "playersByLockedLandmarkTypes");
        for (AbilityType landmarkAbility : AbilityType.values()) {
            LandmarkType type = landmarkAbility.getLandmarkType();
            if (type == null || configuration().get(landmarkAbility) <= 0) continue;
            boolean indexed = index.getOrDefault(type, Set.of()).contains(player.getUUID());
            require("landmark index " + type, indexed == owner().isAbilityLocked(player, landmarkAbility, true), indexed);
        }
    }

    private Objective objective(String name) {
        Objective objective = scoreboard().getObjective(name);
        if (objective == null) {
            objective = scoreboard().addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(name),
                ObjectiveCriteria.RenderType.INTEGER, false, null);
            createdObjectives.add(objective);
        }
        return objective;
    }

    private Objective select(String name) {
        Objective objective = objective(name);
        scoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        require("selected exact objective", owner().currentScoreboardObjective == objective, name);
        return objective;
    }

    private PlayerTeam team() {
        PlayerTeam team = scoreboard().addPlayerTeam("c5" + UUID.randomUUID().toString().substring(0, 10));
        teams.add(team);
        return team;
    }

    private void score(ScoreHolder holder, Objective objective, int value) {
        trace("beforeScoreSet", holder.getScoreboardName(), objective.getName(), value);
        scoreboard().getOrCreatePlayerScore(holder, objective).set(value);
        trace("afterScoreSet", holder.getScoreboardName(), objective.getName(), value);
    }

    private JoinedPlayer join() throws Exception {
        var server = helper.getLevel().getServer();
        UUID uuid = UUID.randomUUID();
        GameProfile profile = new GameProfile(uuid, "c5" + uuid.toString().substring(0, 10));
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        JoinedPlayer joined = new JoinedPlayer(player, connection, channel);
        players.add(joined);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL);
        new ServerboundPlayerLoadedPacket().handle(player.connection);
        BlockPos position = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(position.below(), Blocks.STONE.defaultBlockState());
        player.teleportTo(position.getX() + .5, position.getY(), position.getZ() + .5);
        drain(joined);
        require("normal survival fixture", !player.isSpectator() && !player.isCreative()
            && server.getPlayerList().getPlayer(uuid) == player
            && server.getConnection().getConnections().contains(connection) && player.connection.hasClientLoaded(), uuid);
        trace("join", uuid, player.getScoreboardName());
        return joined;
    }

    private void drain(JoinedPlayer joined) {
        joined.channel().runPendingTasks();
        joined.channel().runScheduledPendingTasks();
        joined.channel().flushOutbound();
        Object packet;
        int drained = 0;
        while ((packet = joined.channel().readOutbound()) != null) {
            if (packet instanceof ClientboundPlayerPositionPacket position) {
                new ServerboundAcceptTeleportationPacket(position.id()).handle(joined.player().connection);
            }
            ReferenceCountUtil.release(packet);
            if (++drained > 20000) throw new IllegalStateException("Outbound fixture drain exceeded bound");
        }
    }

    private void cleanup() throws Exception {
        Throwable failure = null;
        for (JoinedPlayer joined : new ArrayList<>(players)) {
            try {
                joined.player().getAdvancements().save();
                if (joined.channel().isOpen()) drain(joined);
                joined.connection().disconnect(Component.literal("C5 fixture complete"));
                joined.channel().runPendingTasks();
                joined.channel().finishAndReleaseAll();
                if (!(Boolean) field(joined.connection(), "disconnectionHandled")) joined.connection().handleDisconnection();
                var server = helper.getLevel().getServer();
                server.getConnection().getConnections().remove(joined.connection());
                require("fixture player removed", server.getPlayerList().getPlayer(joined.player().getUUID()) == null,
                    joined.player().getUUID());
                require("fixture channel closed", !joined.channel().isOpen(), joined.player().getUUID());
                players.remove(joined);
            } catch (Throwable error) {
                if (failure == null) failure = error; else failure.addSuppressed(error);
            }
        }
        for (PlayerTeam team : teams) if (scoreboard().getPlayerTeam(team.getName()) == team) scoreboard().removePlayerTeam(team);
        teams.clear();
        for (var display : originalDisplays.entrySet()) {
            Objective objective = display.getValue();
            scoreboard().setDisplayObjective(display.getKey(), objective != null && scoreboard().getObjective(objective.getName()) == objective ? objective : null);
        }
        for (Objective objective : createdObjectives) {
            if (scoreboard().getObjective(objective.getName()) == objective) scoreboard().removeObjective(objective);
        }
        createdObjectives.clear();
        if (failure != null) throw new IllegalStateException("Fixture cleanup failed", failure);
        trace("cleanupComplete");
    }

    private void run(GameTestHelper helper, String name, Scenario body) {
        this.helper = helper;
        scenario = name;
        for (DisplaySlot slot : DisplaySlot.values()) originalDisplays.put(slot, scoreboard().getDisplayObjective(slot));
        helper.runAfterDelay(5, () -> {
            try {
                require("loaded ATD configuration", configuration() != null, name);
                body.run();
                cleanup();
                trace("PASS");
                helper.succeed();
            } catch (Throwable error) {
                trace("FAIL", error);
                error.printStackTrace();
                try { cleanup(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); cleanup.printStackTrace(); }
                helper.fail(name + ": " + error);
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void selectedSoloScoreRemoval(GameTestHelper helper) {
        run(helper, "C5-02", () -> {
            Objective selected = select("bac_advancements");
            ServerPlayer player = join().player();
            AbilityType ability = thresholdAbility();
            int threshold = configuration().get(ability);
            Objective spare = objective("c5_spare");
            score(player, spare, 1);
            score(player, selected, threshold);
            count(player, threshold);
            accessAndIndex(player, ability, false);
            trace("beforeSelectedRemoval", threshold);
            scoreboard().resetSinglePlayerScore(player, selected);
            require("selected raw score absent", scoreboard().getPlayerScoreInfo(player, selected) == null, player.getUUID());
            count(player, 0);
            accessAndIndex(player, ability, true);
            score(player, selected, threshold);
            scoreboard().resetSinglePlayerScore(player, spare);
            count(player, threshold);
            accessAndIndex(player, ability, false);
        });
    }

    @GameTest(maxTicks = 200)
    public void removalRoutesAndSurvivors(GameTestHelper helper) {
        run(helper, "C5-03", () -> {
            Objective solo = select("bac_advancements");
            ServerPlayer first = join().player();
            score(first, solo, 6);
            scoreboard().resetAllPlayerScores(first);
            count(first, 0);
            Objective selected = select("bac_advancements_team");
            ServerPlayer second = join().player();
            PlayerTeam team = team();
            scoreboard().addPlayerToTeam(first.getScoreboardName(), team);
            scoreboard().addPlayerToTeam(second.getScoreboardName(), team);
            score(first, selected, 3); score(second, selected, 1);
            count(first, 4); count(second, 4);
            score(first, objective("c5_spare"), 1);
            scoreboard().resetSinglePlayerScore(first, selected);
            count(first, 1); count(second, 1);
            score(first, selected, 3);
            scoreboard().resetAllPlayerScores(first);
            count(first, 1); count(second, 1);
            scoreboard().resetAllPlayerScores(second);
            count(first, 0); count(second, 0);
        });
    }

    @GameTest(maxTicks = 200)
    public void ordinaryThresholdCrossings(GameTestHelper helper) {
        run(helper, "C5-04", () -> {
            Objective selected = select("bac_advancements");
            ServerPlayer player = join().player();
            AbilityType ability = thresholdAbility(); int threshold = configuration().get(ability);
            score(player, selected, threshold - 1); count(player, threshold - 1); accessAndIndex(player, ability, true);
            score(player, selected, threshold); count(player, threshold); accessAndIndex(player, ability, false);
            score(player, selected, threshold - 1); count(player, threshold - 1); accessAndIndex(player, ability, true);
        });
    }

    @GameTest(maxTicks = 200)
    public void allTeamModes(GameTestHelper helper) {
        run(helper, "C5-06", () -> {
            select("bac_advancements");
            ServerPlayer first = join().player(), second = join().player();
            for (String mode : List.of("bac_advancements_team", "bac_advfirst_team_sum", "bac_advfirst_sum", "bac_advfirst_team")) {
                Objective selected = select(mode); PlayerTeam team = team();
                scoreboard().addPlayerToTeam(first.getScoreboardName(), team);
                scoreboard().addPlayerToTeam(second.getScoreboardName(), team);
                score(first, selected, 3); score(second, selected, 1);
                count(first, 4); count(second, 4);
                score(second, selected, 2); count(first, 5); count(second, 5);
                String fake = "c5fake" + UUID.randomUUID().toString().substring(0, 8);
                score(ScoreHolder.forNameOnly(fake), selected, 12); count(first, 5); count(second, 5);
                scoreboard().addPlayerToTeam(fake, team); count(first, 17); count(second, 17);
                scoreboard().addPlayerToTeam("c5missing" + UUID.randomUUID().toString().substring(0, 8), team);
                count(first, 17); count(second, 17);
                scoreboard().removePlayerFromTeam(fake, team); count(first, 5); count(second, 5);
                scoreboard().resetAllPlayerScores(ScoreHolder.forNameOnly(fake));
                scoreboard().removePlayerTeam(team); count(first, 0); count(second, 0);
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void structuralTeamChanges(GameTestHelper helper) {
        run(helper, "C5-07", () -> {
            Objective selected = select("bac_advancements_team");
            ServerPlayer first = join().player(), second = join().player(), third = join().player();
            score(first, selected, 3); score(second, selected, 1); score(third, selected, 2);
            count(first, 0); count(second, 0); count(third, 0);
            PlayerTeam old = team(), target = team();
            scoreboard().addPlayerToTeam(first.getScoreboardName(), old); count(first, 3);
            scoreboard().addPlayerToTeam(second.getScoreboardName(), old); count(first, 4); count(second, 4);
            scoreboard().addPlayerToTeam(third.getScoreboardName(), target); count(third, 2);
            scoreboard().addPlayerToTeam(second.getScoreboardName(), target);
            count(first, 3); count(second, 3); count(third, 3);
            scoreboard().removePlayerFromTeam(second.getScoreboardName(), target);
            count(second, 0); count(third, 2);
            scoreboard().removePlayerTeam(old); count(first, 0);
            scoreboard().removePlayerTeam(target); count(third, 0);
        });
    }

    @GameTest(maxTicks = 200)
    public void objectiveReplacement(GameTestHelper helper) {
        run(helper, "C5-17", () -> {
            Objective original = select("bac_advancements");
            Objective fallback = objective("bac_advfirst");
            scoreboard().setDisplayObjective(DisplaySlot.LIST, fallback);
            ServerPlayer player = join().player(); score(player, original, 6); score(player, fallback, 3); count(player, 6);
            scoreboard().removeObjective(original);
            require("fallback selected", owner().currentScoreboardObjective == fallback, owner().currentScoreboardObjective);
            count(player, 3);
            Objective replacement = objective("bac_advancements");
            require("replacement identity differs", replacement != original, replacement.getName());
            score(player, replacement, 4);
            scoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, replacement); count(player, 4);
            scoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, null);
            scoreboard().setDisplayObjective(DisplaySlot.LIST, null);
            scoreboard().setDisplayObjective(DisplaySlot.BELOW_NAME, null);
            require("no selected tuple not ready", owner().isNotReady(), owner().currentScoreboardObjective);
            scoreboard().setDisplayObjective(DisplaySlot.SIDEBAR, replacement); count(player, 4);
        });
    }

    @GameTest(maxTicks = 200)
    public void resetAndRebuild(GameTestHelper helper) {
        run(helper, "C5-19", () -> {
            Objective selected = select("bac_advancements_team");
            ServerPlayer player = join().player(); PlayerTeam team = team();
            scoreboard().addPlayerToTeam(player.getScoreboardName(), team);
            AbilityType ability = thresholdAbility(); int threshold = configuration().get(ability);
            String fake = "c5rebuild" + UUID.randomUUID().toString().substring(0, 6);
            scoreboard().addPlayerToTeam(fake, team);
            score(player, selected, threshold - 1); score(ScoreHolder.forNameOnly(fake), selected, 1);
            count(player, threshold); accessAndIndex(player, ability, false);
            trace("resetBegin"); scoreboard().resetAllPlayerScores(ScoreHolder.forNameOnly(fake));
            count(player, threshold - 1); accessAndIndex(player, ability, true);
            trace("rebuildBegin"); score(ScoreHolder.forNameOnly(fake), selected, 1);
            count(player, threshold); accessAndIndex(player, ability, false);
            score(ScoreHolder.forNameOnly(fake), selected, 1); count(player, threshold);
            scoreboard().resetAllPlayerScores(ScoreHolder.forNameOnly(fake));
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
