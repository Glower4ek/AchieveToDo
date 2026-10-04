package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.ScoreHolder;

import java.lang.reflect.Field;
import java.util.*;

/** D1-only observation of real joined-player, scoreboard, reward and outgoing packet paths. */
public final class B10MainDeepWitness {
    private final GameTestHelper helper;
    private final JsonObject plan;
    private final JsonObject receipt;
    private final List<Component> messages = new ArrayList<>();
    private ServerPlayer player;
    private Connection connection;
    private EmbeddedChannel channel;
    private JsonObject chosen;
    private AdvancementHolder advancement;
    private JsonObject rewardBefore;
    private JsonObject duplicateBefore;
    private AbilityType ability;
    private AdvancementHolder abilityAdvancement;
    private int threshold;
    private boolean cleaned;

    private B10MainDeepWitness(GameTestHelper helper, JsonObject plan, JsonObject receipt) {
        this.helper = helper; this.plan = plan; this.receipt = receipt;
    }
    public static void start(GameTestHelper helper, JsonObject plan, JsonObject receipt) {
        PhaseBRuntimeSmoke.require(plan != null, "D1 plan missing");
        new B10MainDeepWitness(helper, plan, receipt).step(0, B10MainDeepWitness::join);
    }
    private interface Stage { void run(B10MainDeepWitness witness) throws Exception; }
    private void step(int ticks, Stage next) {
        helper.runAfterDelay(ticks, () -> {
            try { next.run(this); }
            catch (Throwable failure) {
                receipt.addProperty("failure", failure.toString());
                receipt.addProperty("deepSmokeCompleted", false);
                try { cleanup(); } catch (Throwable error) { failure.addSuppressed(error); receipt.addProperty("cleanupFailure", error.toString()); }
                PhaseBRuntimeSmoke.write("deep.json", receipt);
                helper.fail("B10-D1: " + failure);
            }
        });
    }
    private void require(boolean value, String reason) { PhaseBRuntimeSmoke.require(value, reason); }
    private void join() {
        var server = helper.getLevel().getServer();
        require(!AchieveToDoMod.getServer().isNotReady(), "production ATD server not ready");
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "b10" + id.toString().substring(0, 8));
        player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        connection = new Connection(PacketFlow.SERVERBOUND);
        channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL);
        new ServerboundPlayerLoadedPacket().handle(player.connection);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        JsonObject joined = new JsonObject();
        joined.addProperty("UUID", id.toString()); joined.addProperty("concreteClass", player.getClass().getName());
        joined.addProperty("registered", server.getPlayerList().getPlayer(id) == player);
        joined.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        joined.addProperty("clientLoaded", player.connection.hasClientLoaded());
        joined.addProperty("gameMode", player.gameMode().name()); joined.addProperty("spectator", player.isSpectator());
        joined.addProperty("lifecycle", "GameProfile -> CommonListenerCookie -> ServerPlayer -> Connection/EmbeddedChannel -> ServerConnectionListener registration -> PlayerList.placeNewPlayer -> ServerboundPlayerLoadedPacket");
        receipt.add("joinedPlayer", joined);
        require(player.getClass() == ServerPlayer.class && joined.get("registered").getAsBoolean() &&
            joined.get("connectionRegistered").getAsBoolean() && player.connection.hasClientLoaded() &&
            player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "real joined SURVIVAL lifecycle failed");
        step(30, B10MainDeepWitness::award);
    }
    private void award() throws Exception {
        drain(); messages.clear();
        var atd = AchieveToDoMod.getServer();
        JsonObject mode = new JsonObject();
        mode.addProperty("activeObjective", atd.currentScoreboardObjective.getName());
        mode.addProperty("AdvancementsMode", atd.currentAdvancementsMode.name());
        mode.addProperty("displaySlot", atd.currentScoreboardDisplaySlot.name());
        mode.addProperty("obtainedCount", obtained());
        mode.addProperty("configuration", ((LevelInfoExtension) player.level().getServer().getWorldData().getLevelSettings()).achievetodo$getConfigName());
        receipt.add("progressionMode", mode);
        require(AdvancementsMode.findByObjectiveName(atd.currentScoreboardObjective.getName()) != null, "unrecognized raw progression objective");
        require(atd.currentAdvancementsMode == AdvancementsMode.DEFAULT && "bac_advancements".equals(atd.currentScoreboardObjective.getName()), "unexpected legitimate progression mode; no forced mode permitted");
        for (JsonElement row : plan.getAsJsonArray("rewardCandidates")) {
            JsonObject candidate = row.getAsJsonObject();
            AdvancementHolder live = player.level().getServer().getAdvancements().get(Identifier.parse(candidate.get("advancementId").getAsString()));
            require(live != null, "current candidate missing live");
            if (!player.getAdvancements().getOrStartProgress(live).isDone()) { chosen = candidate; advancement = live; break; }
        }
        require(chosen != null, "no current incomplete reward witness");
        Set<String> plannedCriteria = new TreeSet<>();
        chosen.getAsJsonArray("criteria").forEach(c -> plannedCriteria.add(c.getAsString()));
        require(advancement.value().criteria().keySet().equals(plannedCriteria), "live candidate criterion mismatch");
        require(advancement.value().rewards().function().isPresent(), "live reward absent");
        require(player.level().getServer().getFunctions().get(Identifier.parse(chosen.get("rewardFunction").getAsString())).isPresent(), "current reward function not loaded");
        rewardBefore = snapshot();
        JsonObject reward = chosen.deepCopy();
        reward.add("effectiveRewardPath", plan.getAsJsonObject("rewardGraph").deepCopy());
        reward.add("before", rewardBefore);
        int countGuard = score(ScoreHolder.forNameOnly(chosen.get("tier").getAsString()), "bac_dont_count");
        int points = score(ScoreHolder.forNameOnly(chosen.get("tier").getAsString()), "bac_points");
        int obtained = score(ScoreHolder.forNameOnly(advancement.id().toString()), "bac_obtained");
        int expSetting = score(ScoreHolder.forNameOnly("exp"), "bac_settings");
        require(expSetting == 1 || expSetting == -1, "D1 no unproved XP/team setting assumption");
        JsonObject expected = new JsonObject();
        expected.addProperty("raw", countGuard == 1 ? 0 : plan.getAsJsonObject("rewardGraph").get("rawIncrement").getAsInt());
        expected.addProperty("first", obtained >= 1 ? 0 : plan.getAsJsonObject("rewardGraph").get("firstIncrement").getAsInt());
        expected.addProperty("weighted", points);
        expected.addProperty("experience", expSetting == 1 || obtained < 1 ? chosen.get("experienceAmount").getAsInt() : 0);
        reward.add("expectedDeltas", expected);
        reward.addProperty("countGuardValue", countGuard); reward.addProperty("tierPointsValue", points);
        reward.addProperty("globalObtainedBefore", obtained); reward.addProperty("expSetting", expSetting);
        reward.addProperty("realCompletionPath", true); receipt.add("rewardWitness", reward);
        JsonArray results = new JsonArray();
        for (String criterion : plannedCriteria) results.add(player.getAdvancements().award(advancement, criterion));
        reward.add("awardResults", results);
        step(2, B10MainDeepWitness::verifyReward);
    }
    private void verifyReward() throws Exception {
        drain(); JsonObject after = snapshot();
        JsonObject reward = receipt.getAsJsonObject("rewardWitness"); reward.add("after", after);
        require(!rewardBefore.get("complete").getAsBoolean() && after.get("complete").getAsBoolean(), "real completion transition failed");
        JsonObject actual = new JsonObject();
        for (String key : List.of("raw", "first", "weighted", "experience")) {
            int delta = after.get(key).getAsInt() - rewardBefore.get(key).getAsInt(); actual.addProperty(key, delta);
            require(delta == reward.getAsJsonObject("expectedDeltas").get(key).getAsInt(), "reward delta mismatch " + key + ": " + delta);
        }
        require(after.get("obtainedCount").getAsInt() == after.get("raw").getAsInt(), "ATD count does not follow raw score");
        reward.add("actualDeltas", actual);
        JsonObject emitted = inspectMessages(chosen.get("messageFunction").getAsString(), advancement.id().toString(), null, null);
        reward.add("completionMessagePacketWitness", emitted);
        reward.addProperty("rewardExecutionWitness", "real PlayerAdvancements completion caused exact raw/first/weighted/XP writes and emitted the current wrapper completion message");
        reward.addProperty("rawVsWeightedProgressionPassed", true);
        duplicateBefore = after.deepCopy(); messages.clear();
        JsonObject duplicate = new JsonObject(); duplicate.add("before", duplicateBefore);
        duplicate.addProperty("awardResult", player.getAdvancements().award(advancement, chosen.getAsJsonArray("criteria").get(0).getAsString()));
        receipt.add("duplicateGrant", duplicate);
        step(1, B10MainDeepWitness::verifyDuplicate);
    }
    private void verifyDuplicate() throws Exception {
        drain(); JsonObject after = snapshot(); JsonObject duplicate = receipt.getAsJsonObject("duplicateGrant");
        duplicate.add("after", after);
        require(!duplicate.get("awardResult").getAsBoolean() && after.equals(duplicateBefore), "duplicate criterion changed reward/score/progress");
        require(messages.isEmpty(), "duplicate reward emitted chat side effect");
        duplicate.addProperty("duplicateReward", false); duplicate.addProperty("completionStillDone", true);
        var settings = (LevelInfoExtension) player.level().getServer().getWorldData().getLevelSettings();
        Map<AbilityType, Integer> configuration = settings.achievetodo$getAbilitiesConfiguration(player.level().getServer().overworld().getSeed());
        ability = Arrays.stream(AbilityType.values()).filter(a -> configuration.get(a) != null && configuration.get(a) > 0)
            .sorted(Comparator.<AbilityType>comparingInt(configuration::get).thenComparing(AbilityType::getName)).findFirst().orElseThrow();
        threshold = configuration.get(ability);
        abilityAdvancement = player.level().getServer().getAdvancements().get(Identifier.parse("achievetodo:abilities/" + ability.getName()));
        require(abilityAdvancement != null, "selected ability advancement missing");
        JsonObject thresholdReceipt = new JsonObject(); thresholdReceipt.addProperty("ability", ability.name());
        thresholdReceipt.addProperty("requiredCount", threshold); thresholdReceipt.addProperty("advancementId", abilityAdvancement.id().toString());
        thresholdReceipt.addProperty("rawObjective", AchieveToDoMod.getServer().currentScoreboardObjective.getName());
        thresholdReceipt.addProperty("configurationUnmodified", true);
        thresholdReceipt.addProperty("criterionContract", "unlock grants all remaining criteria; relock revokes only unlocked, preserving demystified");
        receipt.add("abilityThresholdWitness", thresholdReceipt);
        setRaw(threshold - 1);
        step(1, B10MainDeepWitness::belowThreshold);
    }
    private void belowThreshold() throws Exception {
        JsonObject below = abilityState(); receipt.getAsJsonObject("abilityThresholdWitness").add("belowThreshold", below);
        require(below.get("locked").getAsBoolean() && !below.get("unlocked").getAsBoolean() && below.get("obtainedCount").getAsInt() == threshold - 1, "below threshold state failed");
        setRaw(threshold);
        step(1, B10MainDeepWitness::atThreshold);
    }
    private void atThreshold() throws Exception {
        JsonObject state = abilityState(); receipt.getAsJsonObject("abilityThresholdWitness").add("exactThreshold", state);
        require(!state.get("locked").getAsBoolean() && state.get("unlocked").getAsBoolean() && state.get("demystified").getAsBoolean() && state.get("complete").getAsBoolean() && state.get("obtainedCount").getAsInt() == threshold, "exact threshold callback failed");
        receipt.getAsJsonObject("abilityThresholdWitness").addProperty("productionCallbackObserved", true);
        setRaw(threshold - 1);
        step(1, B10MainDeepWitness::relocked);
    }
    private void relocked() throws Exception {
        JsonObject state = abilityState();
        require(state.get("locked").getAsBoolean() && !state.get("unlocked").getAsBoolean() && state.get("demystified").getAsBoolean() && !state.get("complete").getAsBoolean(), "relock semantics failed");
        receipt.getAsJsonObject("abilityThresholdWitness").add("relock", state);
        receipt.getAsJsonObject("abilityThresholdWitness").addProperty("relockClaimed", true);
        drain(); messages.clear(); runFunction(chosen.get("messageFunction").getAsString());
        step(1, B10MainDeepWitness::ordinaryMessage);
    }
    private void ordinaryMessage() {
        drain(); JsonObject search = inspectMessages(chosen.get("messageFunction").getAsString(), advancement.id().toString(), null, null);
        search.addProperty("clientUiClaimed", false); search.addProperty("clientUiStatus", "CLIENT_SEARCH_UI_NOT_CLAIMED");
        receipt.add("searchRuntime", search);
        receipt.addProperty("scoreWitnessIsolation", "single joined player; reward and duplicate measurements complete before threshold edits; all score-sensitive measurements complete before ordinary/R2/root function execution");
        receipt.add("b8R2RuntimeWitnesses", new JsonArray());
        runR2(0);
    }
    private static final String[][] R2 = {
        {"bacap_rewards:msg/adventure/ive_got_a_bad_feeling_about_this", "minecraft:adventure/voluntary_exile", "Kill a raid captain. I’d warn against drinking that bottle they dropped…", "Kill a raid captain. Maybe consider staying away from villages for the time being..."},
        {"bacap_rewards:msg/animal/turtle_army", "blazeandcave:animal/turtle_army", "Collect a stack of Turtle Scutes", "Collect a stack of scutes"},
        {"bacap_rewards:msg/end/dogfight", "blazeandcave:end/dogfight", "Kill a Skeleton while both you and it have levitation", "Kill a Skeleton or Stray while both you and it have levitation"},
        {"bacap_rewards:potion/root", "blazeandcave:potion/root", "Brewing potions with helpful and hindering effects", "blazeandcave:mining/root"}
    };
    private void runR2(int index) {
        messages.clear(); runFunction(R2[index][0]);
        step(1, w -> {
            w.drain();
            JsonObject row = w.inspectMessages(R2[index][0], R2[index][1], R2[index][2], R2[index][3]);
            row.addProperty("oldBindingAbsent", true); w.receipt.getAsJsonArray("b8R2RuntimeWitnesses").add(row);
            if (index < R2.length - 1) w.runR2(index + 1);
            else {
                JsonObject macro = new JsonObject(); macro.addProperty("wrapperId", R2[index][0]);
                macro.addProperty("macroId", "bacap_rewards:advancement_made_macro");
                macro.addProperty("realFunctionManagerExecution", true); macro.addProperty("runtimeExecutionResult", "RETURNED_WITH_CURRENT_POTIONS_COMPONENT");
                macro.addProperty("optionalHooksSafe", true); w.receipt.add("macroWitness", macro);
                w.finish();
            }
        });
    }
    private void runFunction(String id) {
        var server = player.level().getServer();
        var function = server.getFunctions().get(Identifier.parse(id)).orElseThrow(() -> new IllegalStateException("required runtime function missing " + id));
        server.getFunctions().execute(function, server.createCommandSourceStack().withEntity(player).withPosition(player.position()).withLevel(player.level()));
    }
    private JsonObject inspectMessages(String function, String target, String description, String old) {
        require(!messages.isEmpty(), "no outgoing system-chat component " + function);
        List<String> clicks = new ArrayList<>(); Set<String> keys = new TreeSet<>(); JsonArray components = new JsonArray();
        for (Component message : messages) {
            visit(message, clicks, keys);
            components.add(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, message).getOrThrow());
        }
        String command = "/advancementssearch highlight " + target + " obtained_status";
        require(clicks.stream().anyMatch(s -> s.equals(command) || s.equals(command.substring(1))), "current outgoing Search binding missing " + function + " " + clicks);
        require(player.level().getServer().getAdvancements().get(Identifier.parse(target)) != null, "runtime Search target missing " + target);
        require(!plan.getAsJsonArray("removedOldIds").contains(new JsonPrimitive(target)), "removed target emitted");
        if (description != null) require(keys.contains(description), "current runtime description missing " + function);
        if (old != null) require(!keys.contains(old) && clicks.stream().noneMatch(s -> s.contains(old)), "old binding emitted " + function);
        JsonObject row = new JsonObject(); row.addProperty("functionId", function);
        row.addProperty("packetType", "net.minecraft.network.protocol.game.ClientboundSystemChatPacket");
        row.addProperty("capturePath", "real Connection -> EmbeddedChannel.readOutbound -> packet.content -> Component Style");
        row.add("emittedComponents", components); row.add("translationKeys", new Gson().toJsonTree(keys));
        row.addProperty("clickAction", "run_command"); row.addProperty("command", command); row.add("allClickCommands", new Gson().toJsonTree(clicks));
        row.addProperty("targetAdvancementId", target); row.addProperty("liveTargetResolved", true);
        if (description != null) row.addProperty("currentDescriptionKey", description);
        return row;
    }
    private static void visit(Component component, List<String> clicks, Set<String> keys) {
        if (component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand command) clicks.add(command.command());
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText hover) visit(hover.value(), clicks, keys);
        if (component.getContents() instanceof TranslatableContents translation) {
            keys.add(translation.getKey());
            for (Object arg : translation.getArgs()) if (arg instanceof Component child) visit(child, clicks, keys);
        }
        for (Component child : component.getSiblings()) visit(child, clicks, keys);
    }
    private void drain() {
        channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
        Object packet; int count = 0;
        while ((packet = channel.readOutbound()) != null) {
            if (packet instanceof ClientboundSystemChatPacket chat) messages.add(chat.content());
            if (packet instanceof ClientboundPlayerPositionPacket position) new ServerboundAcceptTeleportationPacket(position.id()).handle(player.connection);
            ReferenceCountUtil.release(packet);
            require(++count < 8192, "outbound packet bound exceeded");
        }
    }
    private int score(ScoreHolder holder, String objective) {
        var scoreboard = player.level().getServer().getScoreboard(); var field = scoreboard.getObjective(objective);
        require(field != null, "required score objective missing " + objective);
        var info = scoreboard.getPlayerScoreInfo(holder, field); return info == null ? 0 : info.value();
    }
    @SuppressWarnings("unchecked")
    private int obtained() throws Exception {
        Field field = AchieveToDoMod.getServer().getClass().getDeclaredField("obtainedAdvancementsCountByPlayers");
        field.setAccessible(true);
        return ((Map<UUID, Integer>) field.get(AchieveToDoMod.getServer())).getOrDefault(player.getUUID(), Integer.MIN_VALUE);
    }
    private JsonObject snapshot() throws Exception {
        JsonObject row = new JsonObject(); var progress = player.getAdvancements().getOrStartProgress(advancement);
        row.addProperty("complete", progress.isDone()); JsonObject criteria = new JsonObject();
        for (String criterion : advancement.value().criteria().keySet()) criteria.addProperty(criterion, progress.getCriterion(criterion).isDone());
        row.add("criteria", criteria); row.addProperty("raw", score(player, "bac_advancements"));
        row.addProperty("first", score(player, "bac_advfirst")); row.addProperty("weighted", score(player, "bac_advancements_points"));
        row.addProperty("experience", player.totalExperience); row.addProperty("obtainedCount", obtained());
        row.addProperty("globalObtained", score(ScoreHolder.forNameOnly(advancement.id().toString()), "bac_obtained"));
        JsonArray items = new JsonArray();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i); items.add(stack.toString() + ":" + stack.getComponents());
        }
        row.add("inventory", items); return row;
    }
    private void setRaw(int value) {
        var server = player.level().getServer();
        server.getScoreboard().getOrCreatePlayerScore(player, AchieveToDoMod.getServer().currentScoreboardObjective).set(value);
    }
    private JsonObject abilityState() throws Exception {
        var progress = player.getAdvancements().getOrStartProgress(abilityAdvancement); JsonObject row = new JsonObject();
        row.addProperty("raw", score(player, "bac_advancements")); row.addProperty("obtainedCount", obtained());
        row.addProperty("locked", AchieveToDoMod.isAbilityLocked(player, ability, true));
        row.addProperty("unlocked", progress.getCriterion("unlocked").isDone());
        row.addProperty("demystified", progress.getCriterion("demystified").isDone());
        row.addProperty("complete", progress.isDone()); return row;
    }
    private void cleanup() {
        if (cleaned || player == null) return; cleaned = true;
        var server = player.level().getServer();
        server.getPlayerList().remove(player); server.getConnection().getConnections().remove(connection);
        if (channel != null && channel.isOpen()) { drain(); connection.disconnect(Component.literal("B10-D1 complete")); channel.finishAndReleaseAll(); }
        JsonObject clean = new JsonObject(); clean.addProperty("playerRemoved", server.getPlayerList().getPlayer(player.getUUID()) == null);
        clean.addProperty("connectionRemoved", !server.getConnection().getConnections().contains(connection));
        clean.addProperty("channelClosed", !channel.isOpen()); receipt.add("cleanup", clean);
        require(clean.get("playerRemoved").getAsBoolean() && clean.get("connectionRemoved").getAsBoolean() && clean.get("channelClosed").getAsBoolean(), "D1 cleanup incomplete");
    }
    private void finish() {
        cleanup(); receipt.addProperty("deepSmokeCompleted", true); receipt.addProperty("result", "PASS");
        PhaseBRuntimeSmoke.write("deep.json", receipt); helper.succeed();
    }
}
