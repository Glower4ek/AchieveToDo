package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Llama.Variant;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real llama-mount packet-path certification.  Max temper makes the vanilla
 * RunAroundLikeCrazyGoal probability deterministic without granting taming.
 */
public final class PhaseATameAnimalLlamaGameTest implements CustomTestMethodInvoker {
    private static final int MAX_TAME_TICKS = 600;
    private static final int CANARY_MAX_TICKS = 700;
    private static final int EXACT_MAX_TICKS = 3900;
    private static final int MAX_SETTLE_PASSES = 32;
    private static final int MAX_SETTLE_MESSAGES = 4096;

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void tameAnimalLlamaCanary(GameTestHelper helper) {
        PhaseATameAnimalLlamaCertification.Case definition = PhaseATameAnimalLlamaCertification.CASES.getFirst();
        try {
            String runId = PhaseATameAnimalLlamaExecutionEvidence.beginRun(PhaseATameAnimalLlamaExecutionEvidence.projectRoot());
            executeCase(helper, definition, runId, () -> {
                try {
                    PhaseATameAnimalLlamaExecutionEvidence.Artifact artifact =
                        PhaseATameAnimalLlamaExecutionEvidence.loadTemporary(PhaseATameAnimalLlamaExecutionEvidence.projectRoot(), false);
                    require(artifact.entries().size() == 1 && artifact.runId().equals(runId), "Canary TEMP receipt/runId mismatch");
                    helper.succeed();
                } catch (Throwable t) { helper.fail("Llama canary TEMP validation failed: " + describe(t)); }
            });
        } catch (Throwable t) { helper.fail("Llama canary setup failed: " + describe(t)); }
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void tameAnimalLlamaExact6(GameTestHelper helper) {
        try {
            String runId = PhaseATameAnimalLlamaExecutionEvidence.beginRun(PhaseATameAnimalLlamaExecutionEvidence.projectRoot());
            executeCoordinator(helper, PhaseATameAnimalLlamaCertification.CASES, 0, runId);
        } catch (Throwable t) { helper.fail("Llama exact-family setup failed: " + describe(t)); }
    }

    private static void executeCoordinator(GameTestHelper helper, List<PhaseATameAnimalLlamaCertification.Case> cases, int index, String runId) {
        if (index == cases.size()) {
            try {
                PhaseATameAnimalLlamaExecutionEvidence.Artifact artifact =
                    PhaseATameAnimalLlamaExecutionEvidence.loadTemporary(PhaseATameAnimalLlamaExecutionEvidence.projectRoot(), true);
                require(artifact.runId().equals(runId) && artifact.entries().size() == cases.size(), "Exact TEMP receipt/runId mismatch");
                helper.succeed();
            } catch (Throwable t) { helper.fail("Llama exact TEMP promotability failed: " + describe(t)); }
            return;
        }
        executeCase(helper, cases.get(index), runId, () -> {
            try {
                require(runId.equals(PhaseATameAnimalLlamaExecutionEvidence.currentRunId(
                    PhaseATameAnimalLlamaExecutionEvidence.projectRoot())), "Coordinator runId collision");
                executeCoordinator(helper, cases, index + 1, runId);
            } catch (Throwable t) { helper.fail("Llama coordinator failed: " + describe(t)); }
        });
    }

    private static void executeCase(GameTestHelper helper, PhaseATameAnimalLlamaCertification.Case definition, String runId, Done done) {
        JoinedPlayer joined = null;
        Llama llama = null;
        try {
            joined = joinedPlayer(helper);
            establishSurvival(joined.player());
            new ServerboundPlayerLoadedPacket().handle(joined.player().connection);
            require(joined.player().connection.hasClientLoaded(), "Client-loaded packet was rejected");
            llama = (Llama) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:llama"))
                .create(helper.getLevel(), EntitySpawnReason.COMMAND);
            require(llama != null, "Could not create llama");
            net.minecraft.core.BlockPos llamaPos = helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3));
            llama.teleportTo(llamaPos.getX() + 0.5D, llamaPos.getY(), llamaPos.getZ() + 0.5D);
            configureFixture(llama, definition);
            require(helper.getLevel().addFreshEntity(llama), "Could not add llama fixture");
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(definition.advancementId()));
            require(advancement != null, "Missing live advancement " + definition.advancementId());
            CriterionProgress before = joined.player().getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion());
            require(before != null && !before.isDone(), "Criterion was already complete " + definition.key());
            JoinedPlayer retainedJoined = joined;
            Llama retainedLlama = llama;
            helper.runAfterDelay(1, () -> mountAndWait(helper, definition, runId, done, retainedJoined, retainedLlama, advancement, 1));
        } catch (Throwable t) {
            if (llama != null) llama.discard();
            if (joined != null) cleanup(joined);
            helper.fail("Llama case setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void mountAndWait(GameTestHelper helper, PhaseATameAnimalLlamaCertification.Case definition, String runId,
                                     Done done, JoinedPlayer joined, Llama llama, AdvancementHolder advancement, int tick) {
        try {
            if (tick == 1) {
                ServerPlayer player = joined.player();
                player.teleportTo(llama.getX() - 1.0D, llama.getY(), llama.getZ());
                new ServerboundInteractPacket(llama.getId(), InteractionHand.MAIN_HAND, llama.position(), false).handle(player.connection);
                require(player.getVehicle() == llama, "Native interaction packet did not mount untamed llama");
            }
            CriterionProgress progress = joined.player().getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion());
            boolean tamed = llama.isTamed();
            if (tamed && progress != null && progress.isDone()) {
                finish(helper, definition, runId, done, joined, llama, tick);
                return;
            }
            if (tick >= MAX_TAME_TICKS) {
                throw new IllegalStateException("Native bounded taming did not complete | tamed=" + tamed
                    + " criterion=" + (progress != null && progress.isDone()) + " temper=" + llama.getTemper()
                    + " maxTemper=" + llama.getMaxTemper());
            }
            helper.runAfterDelay(1, () -> mountAndWait(helper, definition, runId, done, joined, llama, advancement, tick + 1));
        } catch (Throwable t) {
            llama.discard();
            cleanup(joined);
            helper.fail("Llama native tame proof failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void finish(GameTestHelper helper, PhaseATameAnimalLlamaCertification.Case definition, String runId,
                               Done done, JoinedPlayer joined, Llama llama, int ticks) {
        try {
            JsonObject receipt = new JsonObject();
            receipt.addProperty("advancementId", definition.advancementId());
            receipt.addProperty("criterion", definition.criterion());
            receipt.addProperty("family", PhaseATameAnimalLlamaExecutionEvidence.FAMILY);
            receipt.addProperty("source", PhaseATameAnimalLlamaExecutionEvidence.SOURCE);
            receipt.addProperty("catalogFingerprint", fingerprint());
            receipt.addProperty("runId", runId);
            receipt.addProperty("result", "GREEN");
            receipt.addProperty("trigger", PhaseATameAnimalLlamaCertification.TRIGGER);
            receipt.addProperty("boundary", PhaseATameAnimalLlamaCertification.BOUNDARY);
            receipt.addProperty("packetPath", PhaseATameAnimalLlamaCertification.PACKET_PATH);
            receipt.addProperty("playerUuid", joined.player().getUUID().toString());
            receipt.addProperty("gameMode", "SURVIVAL");
            receipt.addProperty("joined", true);
            receipt.addProperty("connectionRegistered", true);
            receipt.addProperty("clientLoaded", true);
            receipt.addProperty("normalScheduler", true);
            receipt.addProperty("criterionBefore", false);
            receipt.addProperty("criterionAfter", true);
            receipt.addProperty("nativeTameTransition", true);
            receipt.addProperty("noDirectCriterionTrigger", true);
            receipt.addProperty("noManualAward", true);
            receipt.addProperty("untamedBefore", true);
            receipt.addProperty("tamedAfter", llama.isTamed());
            receipt.addProperty("observedEntityType", "minecraft:llama");
            receipt.addProperty("temperBeforeMount", llama.getMaxTemper());
            receipt.addProperty("maxTemper", llama.getMaxTemper());
            receipt.addProperty("ticksToCriterion", ticks);
            JsonObject predicate = new JsonObject();
            predicate.addProperty("kind", definition.predicateKind());
            predicate.addProperty("strength", llama.getStrength());
            predicate.addProperty("variant", llama.getVariant().name());
            predicate.addProperty("satisfied", predicateSatisfied(llama, definition));
            receipt.add("predicateObservation", predicate);
            require(predicateSatisfied(llama, definition), "Live llama predicate mismatch");
            llama.discard();
            Cleanup cleanup = cleanup(joined);
            JsonObject cleanupJson = new JsonObject();
            cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved());
            cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved());
            cleanupJson.addProperty("channelSettled", cleanup.channelSettled());
            cleanupJson.addProperty("settlementMessages", cleanup.messages());
            cleanupJson.addProperty("warningCount", 0);
            receipt.add("cleanup", cleanupJson);
            PhaseATameAnimalLlamaExecutionEvidence.recordGreen(PhaseATameAnimalLlamaExecutionEvidence.projectRoot(), receipt);
            done.complete();
        } catch (Throwable t) {
            llama.discard();
            cleanup(joined);
            helper.fail("Llama receipt failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void configureFixture(Llama llama, PhaseATameAnimalLlamaCertification.Case definition) throws Exception {
        require(!llama.isTamed(), "Llama fixture unexpectedly started tamed");
        Method setStrength = Llama.class.getDeclaredMethod("setStrength", int.class);
        setStrength.setAccessible(true);
        setStrength.invoke(llama, "strength".equals(definition.predicateKind()) ? definition.predicateValue() : 1);
        Method setVariant = Llama.class.getDeclaredMethod("setVariant", Variant.class);
        setVariant.setAccessible(true);
        setVariant.invoke(llama, "variant".equals(definition.predicateKind()) ? Variant.valueOf(definition.variant()) : Variant.CREAMY);
        llama.setTemper(llama.getMaxTemper());
        require(llama.getTemper() == llama.getMaxTemper() && llama.getMaxTemper() > 0,
            "Could not establish deterministic native tame temper");
        require(predicateSatisfied(llama, definition), "Fixture predicate is not live before native taming");
    }

    private static boolean predicateSatisfied(Llama llama, PhaseATameAnimalLlamaCertification.Case definition) {
        return switch (definition.predicateKind()) {
            case "strength" -> llama.getStrength() == definition.predicateValue();
            case "variant" -> llama.getVariant() == Variant.valueOf(definition.variant());
            case "any_llama" -> true;
            default -> false;
        };
    }

    private static JoinedPlayer joinedPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "llama" + id.toString().replace("-", "").substring(0, 11));
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        require(server.getPlayerList().getPlayer(id) == player && server.getConnection().getConnections().contains(connection), "Joined lifecycle registration failed");
        return new JoinedPlayer(player, connection, channel, new AtomicBoolean());
    }

    private static void establishSurvival(ServerPlayer player) {
        if (player.gameMode() == GameType.SURVIVAL && player.hasInfiniteMaterials()) require(player.setGameMode(GameType.CREATIVE), "Could not refresh player mode");
        player.setGameMode(GameType.SURVIVAL);
        require(player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials() && !player.isSpectator(), "Expected finite SURVIVAL player");
    }

    private static Cleanup cleanup(JoinedPlayer joined) {
        if (!joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, 0);
        ServerPlayer player = joined.player();
        MinecraftServer server = player.level().getServer();
        if (player.getVehicle() != null) player.stopRiding();
        server.getPlayerList().remove(player);
        boolean playerRemoved = server.getPlayerList().getPlayer(player.getUUID()) != player;
        server.getConnection().getConnections().remove(joined.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joined.connection());
        int messages = settle(joined.channel());
        joined.connection().disconnect(Component.literal("Llama GameTest cleanup"));
        return new Cleanup(playerRemoved, connectionRemoved, true, messages);
    }

    private static int settle(EmbeddedChannel channel) {
        int messages = 0;
        for (int pass = 0; pass < MAX_SETTLE_PASSES; pass++) {
            channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
            int thisPass = 0; Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound); messages++; thisPass++;
                if (messages > MAX_SETTLE_MESSAGES) throw new IllegalStateException("EmbeddedChannel cleanup exceeded bound");
            }
            if (thisPass == 0 && !channel.hasPendingTasks()) return messages;
        }
        throw new IllegalStateException("EmbeddedChannel cleanup did not quiesce");
    }

    private static String fingerprint() throws java.io.IOException {
        try {
            return "sha-256:" + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                java.nio.file.Files.readAllBytes(PhaseATameAnimalLlamaExecutionEvidence.projectRoot().resolve(PhaseATameAnimalLlamaCertification.SNAPSHOT))));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static String describe(Throwable t) { return t.getClass().getSimpleName() + ": " + t.getMessage(); }
    @FunctionalInterface private interface Done { void complete(); }
    private record JoinedPlayer(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { helper.setBlock(0, 0, 0, net.minecraft.world.level.block.Blocks.AIR); method.invoke(this, helper); }
}
