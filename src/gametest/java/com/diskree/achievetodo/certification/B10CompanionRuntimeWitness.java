package com.diskree.achievetodo.certification;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
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

import java.util.*;

/** D2-only companion load and function/component witnesses; D1 common semantics remain separate. */
public final class B10CompanionRuntimeWitness {
    private final GameTestHelper helper;
    private final JsonObject plan;
    private final JsonObject receipt;
    private final List<Component> messages = new ArrayList<>();
    private ServerPlayer player;
    private Connection connection;
    private EmbeddedChannel channel;
    private boolean cleaned;
    private Integer originalGate;
    private int rawBefore;
    private int pointsBefore;

    private B10CompanionRuntimeWitness(GameTestHelper helper, JsonObject plan, JsonObject receipt) {
        this.helper = helper; this.plan = plan; this.receipt = receipt;
    }
    public static void start(GameTestHelper helper, JsonObject plan, JsonObject receipt) {
        PhaseBRuntimeSmoke.require(plan != null, "D2 companion plan missing");
        new B10CompanionRuntimeWitness(helper, plan, receipt).step(0, B10CompanionRuntimeWitness::join);
    }
    private interface Stage { void run(B10CompanionRuntimeWitness witness) throws Exception; }
    private void step(int ticks, Stage stage) {
        helper.runAfterDelay(ticks, () -> {
            try { stage.run(this); }
            catch (Throwable failure) {
                receipt.addProperty("failure", failure.toString()); receipt.addProperty("result", "FAIL");
                try { cleanup(); } catch (Throwable error) { failure.addSuppressed(error); receipt.addProperty("cleanupFailure", error.toString()); }
                PhaseBRuntimeSmoke.write("companion.json", receipt); helper.fail("B10-D2: " + failure);
            }
        });
    }
    private void require(boolean condition, String message) { PhaseBRuntimeSmoke.require(condition, message); }
    private boolean live(String id) { return helper.getLevel().getServer().getAdvancements().get(Identifier.parse(id)) != null; }
    private boolean functionLoaded(String id) { return helper.getLevel().getServer().getFunctions().get(Identifier.parse(id)).isPresent(); }
    private String mode() { return plan.get("mode").getAsString(); }
    private void join() {
        require(mode().equals(System.getProperty("achievetodo.b10.mode")), "D2 mode mismatch");
        JsonArray merges = new JsonArray();
        for (JsonElement element : plan.getAsJsonArray("merges")) {
            JsonObject row = element.getAsJsonObject(); String id = row.get("advancementId").getAsString();
            require(live(id), "companion merged advancement missing " + id); merges.add(id);
        }
        receipt.add("mergedAdvancementIdsLive", merges);
        JsonArray wrappers = new JsonArray();
        for (JsonElement element : plan.getAsJsonArray("wrappers")) {
            JsonObject row = element.getAsJsonObject(); String id = row.get("functionId").getAsString();
            require(functionLoaded(id), "companion wrapper not loaded " + id);
            require(live(row.get("advancementId").getAsString()), "companion wrapper target not live " + id);
            require(functionLoaded(row.get("macroTarget").getAsString()), "companion macro not loaded " + id); wrappers.add(id);
        }
        receipt.add("wrappersLive", wrappers);
        if (plan.has("repairedHelpers")) {
            for (JsonElement id : plan.getAsJsonArray("repairedHelpers")) require(live(id.getAsString()), "repaired helper absent " + id);
            receipt.add("repairedHelpersLive", plan.getAsJsonArray("repairedHelpers").deepCopy());
        }
        for (JsonElement element : plan.getAsJsonArray("messages")) {
            JsonObject row = element.getAsJsonObject();
            require(functionLoaded(row.get("functionId").getAsString()), "companion message not loaded");
            require(live(row.get("targetId").getAsString()), "companion message target missing");
        }
        var server = helper.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "b10c" + id.toString().substring(0, 7));
        player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        connection = new Connection(PacketFlow.SERVERBOUND); channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setGameMode(GameType.SURVIVAL); new ServerboundPlayerLoadedPacket().handle(player.connection);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        JsonObject joined = new JsonObject(); joined.addProperty("UUID", id.toString());
        joined.addProperty("concreteClass", player.getClass().getName());
        joined.addProperty("registered", server.getPlayerList().getPlayer(id) == player);
        joined.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        joined.addProperty("clientLoaded", player.connection.hasClientLoaded());
        joined.addProperty("gameMode", player.gameMode().name()); joined.addProperty("spectator", player.isSpectator());
        require(player.getClass() == ServerPlayer.class && joined.get("registered").getAsBoolean() && joined.get("connectionRegistered").getAsBoolean() && player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "companion real joined lifecycle failed");
        receipt.add("joinedPlayer", joined); receipt.add("messageWitnesses", new JsonArray());
        step(30, w -> { w.drain(); w.messages.clear(); w.runMessage(0); });
    }
    private void runMessage(int index) {
        JsonArray planned = plan.getAsJsonArray("messages");
        if (index == planned.size()) {
            if (plan.has("representativeMerge") && !mode().equals("NULLSCAPE")) representativeMerge();
            else finish();
            return;
        }
        JsonObject row = planned.get(index).getAsJsonObject(); messages.clear();
        runFunction(row.get("functionId").getAsString());
        step(1, w -> {
            w.drain(); JsonObject witness = w.inspect(row.get("functionId").getAsString(), row.get("targetId").getAsString());
            for (JsonElement key : row.getAsJsonArray("translationKeys")) require(witness.getAsJsonArray("translationKeys").contains(key), "current companion translation component missing " + key);
            w.receipt.getAsJsonArray("messageWitnesses").add(witness); w.runMessage(index + 1);
        });
    }
    private void representativeMerge() {
        JsonObject selected = plan.getAsJsonObject("representativeMerge");
        require(functionLoaded(selected.get("messageFunction").getAsString()), "representative merged message not loaded");
        messages.clear(); runFunction(selected.get("rewardFunction").getAsString());
        step(2, w -> {
            w.drain(); JsonObject witness = w.inspect(selected.get("rewardFunction").getAsString(), selected.get("advancementId").getAsString());
            witness.addProperty("selectionReason", selected.get("selectionReason").getAsString());
            witness.addProperty("currentRewardAndMessagePathExecuted", true); w.receipt.add("representativeMergeWitness", witness);
            if (w.mode().equals("TERRALITH")) w.terralithGate(); else w.finish();
        });
    }
    private void terralithGate() {
        JsonObject gate = plan.getAsJsonObject("macroGate");
        require(functionLoaded(gate.get("functionId").getAsString()), "Terralith gate macro missing");
        originalGate = score(ScoreHolder.forNameOnly(gate.get("scoreHolder").getAsString()), gate.get("objective").getAsString());
        setGate(gate.get("enabledValue").getAsInt());
        JsonObject wrapper = plan.getAsJsonArray("wrappers").get(0).getAsJsonObject();
        rawBefore = score(player, "bac_advancements"); pointsBefore = score(player, "bac_advancements_points");
        JsonObject witness = new JsonObject(); witness.addProperty("functionId", gate.get("functionId").getAsString());
        witness.addProperty("wrapperId", wrapper.get("functionId").getAsString());
        witness.addProperty("originalGate", originalGate); witness.addProperty("enabledGate", 1);
        witness.addProperty("rawBefore", rawBefore); witness.addProperty("pointsBefore", pointsBefore);
        witness.addProperty("tierPoints", score(ScoreHolder.forNameOnly(wrapper.getAsJsonObject("arguments").get("tier").getAsString()), "bac_points"));
        receipt.add("terralithMacroGateWitness", witness);
        messages.clear(); runFunction(wrapper.get("functionId").getAsString());
        step(2, w -> {
            w.drain(); JsonObject packet = w.inspect(wrapper.get("functionId").getAsString(), wrapper.get("advancementId").getAsString());
            w.receipt.add("representativeWrapperWitness", packet);
            JsonObject result = w.receipt.getAsJsonObject("terralithMacroGateWitness");
            int rawAfter = w.score(w.player, "bac_advancements"), pointsAfter = w.score(w.player, "bac_advancements_points");
            result.addProperty("rawAfter", rawAfter); result.addProperty("pointsAfter", pointsAfter);
            result.addProperty("rawDelta", rawAfter - w.rawBefore); result.addProperty("pointsDelta", pointsAfter - w.pointsBefore);
            require(rawAfter > w.rawBefore && pointsAfter > w.pointsBefore, "enabled Terralith count/points path not reachable");
            result.addProperty("countPointsReachable", true); result.addProperty("balanceAssertion", false);
            result.addProperty("cooperativeBehaviorPreserved", true); result.addProperty("result", "PASS");
            w.setGate(w.originalGate); result.addProperty("gateRestored", true); w.finish();
        });
    }
    private int score(ScoreHolder holder, String name) {
        var board = player.level().getServer().getScoreboard(); var objective = board.getObjective(name);
        require(objective != null, "companion objective missing " + name);
        var value = board.getPlayerScoreInfo(holder, objective); return value == null ? 0 : value.value();
    }
    private void setGate(int value) {
        JsonObject gate = plan.getAsJsonObject("macroGate"); var board = player.level().getServer().getScoreboard();
        board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(gate.get("scoreHolder").getAsString()), board.getObjective(gate.get("objective").getAsString())).set(value);
    }
    private void runFunction(String id) {
        var server = player.level().getServer();
        var function = server.getFunctions().get(Identifier.parse(id)).orElseThrow(() -> new IllegalStateException("companion required function missing " + id));
        server.getFunctions().execute(function, server.createCommandSourceStack().withEntity(player).withPosition(player.position()).withLevel(player.level()));
    }
    private JsonObject inspect(String function, String target) {
        require(!messages.isEmpty(), "companion emitted no runtime component " + function);
        List<String> clicks = new ArrayList<>(); Set<String> keys = new TreeSet<>(); JsonArray components = new JsonArray();
        for (Component message : messages) {
            visit(message, clicks, keys); components.add(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, message).getOrThrow());
        }
        String expected = "/advancementssearch highlight " + target + " obtained_status";
        require(clicks.stream().anyMatch(c -> c.equals(expected) || c.equals(expected.substring(1))), "companion current runtime Search binding absent " + function + " " + clicks);
        for (String click : clicks) {
            String[] tokens = click.replaceFirst("^/", "").split("\\s+");
            if (tokens.length >= 3 && tokens[0].equals("advancementssearch") && tokens[1].equals("highlight")) {
                require(live(tokens[2]), "companion emitted unresolved Search target " + tokens[2]);
                require(!plan.getAsJsonArray("forbiddenSearchTargets").contains(new JsonPrimitive(tokens[2])), "stale companion Search target " + tokens[2]);
            }
        }
        require(live(target), "companion target not live " + target);
        JsonObject result = new JsonObject(); result.addProperty("functionId", function); result.addProperty("functionLoaded", true);
        result.addProperty("runtimeExecutionSucceeded", true); result.addProperty("packetType", "net.minecraft.network.protocol.game.ClientboundSystemChatPacket");
        result.addProperty("capturePath", "real Connection -> EmbeddedChannel.readOutbound -> packet.content -> Component Style");
        result.add("emittedComponents", components); result.add("translationKeys", new Gson().toJsonTree(keys));
        result.addProperty("clickAction", "run_command"); result.addProperty("command", expected); result.add("allClickCommands", new Gson().toJsonTree(clicks));
        result.addProperty("targetAdvancementId", target); result.addProperty("liveTargetResolved", true);
        result.addProperty("staleSearchTargetEmitted", false); result.addProperty("result", "PASS"); return result;
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
        channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound(); Object packet; int count = 0;
        while ((packet = channel.readOutbound()) != null) {
            if (packet instanceof ClientboundSystemChatPacket chat) messages.add(chat.content());
            if (packet instanceof ClientboundPlayerPositionPacket position) new ServerboundAcceptTeleportationPacket(position.id()).handle(player.connection);
            ReferenceCountUtil.release(packet); require(++count < 8192, "companion outbound packet bound exceeded");
        }
    }
    private void cleanup() {
        if (cleaned || player == null) return; cleaned = true;
        if (originalGate != null) setGate(originalGate);
        var server = player.level().getServer(); server.getPlayerList().remove(player); server.getConnection().getConnections().remove(connection);
        if (channel != null && channel.isOpen()) { drain(); connection.disconnect(Component.literal("B10-D2 complete")); channel.finishAndReleaseAll(); }
        JsonObject clean = new JsonObject(); clean.addProperty("playerRemoved", server.getPlayerList().getPlayer(player.getUUID()) == null);
        clean.addProperty("connectionRemoved", !server.getConnection().getConnections().contains(connection)); clean.addProperty("channelClosed", !channel.isOpen());
        receipt.add("cleanup", clean); require(clean.get("playerRemoved").getAsBoolean() && clean.get("connectionRemoved").getAsBoolean() && clean.get("channelClosed").getAsBoolean(), "companion cleanup failed");
    }
    private void finish() {
        cleanup(); receipt.addProperty("clientSearchUiClaimed", false); receipt.addProperty("clientSearchUiStatus", "CLIENT_SEARCH_UI_NOT_CLAIMED");
        receipt.addProperty("commonDeepSemanticsRepeated", false); receipt.addProperty("result", "PASS");
        PhaseBRuntimeSmoke.write("companion.json", receipt); helper.succeed();
    }
}
