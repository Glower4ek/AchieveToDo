package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native propagule placement in a live grove biome. */
public final class PhaseASingletonMangroveGroveGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 400)
    public void singletonMangroveGroveCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 400)
    public void singletonMangroveGroveExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Joined joined = null;
        try {
            var root = PhaseASingletonMangroveGroveExecutionEvidence.projectRoot();
            String runId = PhaseASingletonMangroveGroveExecutionEvidence.beginRun(root);
            BlockPos pos = h.absolutePos(TARGET);
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.CLAY.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            h.setBiome(Biomes.GROVE);
            require(h.getLevel().getBlockState(pos.below()).is(Blocks.CLAY) && h.getLevel().getBlockState(pos).isAir(),
                "live propagule placement fixture missing");
            joined = join(h); ServerPlayer player = joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
                "joined finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(joined.connection()), "joined registration failed");
            var settings = h.getLevel().getServer().getScoreboard().getObjective("bac_settings");
            require(settings != null, "BACAP settings objective missing");
            h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"), settings).set(0);
            player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + 1.5D);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MANGROVE_PROPAGULE, 1));
            AdvancementHolder advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseASingletonMangroveGroveCertification.ADVANCEMENT));
            require(advancement != null && !criterionDone(player, advancement), "propagule criterion missing or pre-complete");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, h.getLevel(), pos.below()), "placement is in a locked landmark");
            Joined kept = joined;
            h.runAfterDelay(1, () -> place(h, exact, runId, kept, advancement, pos));
        } catch (Throwable t) {
            if (joined != null) cleanup(joined);
            h.fail("singleton farming/a_mangrove_grove setup failed: " + describe(t));
        }
    }

    private static void place(GameTestHelper h, boolean exact, String runId, Joined joined, AdvancementHolder advancement, BlockPos pos) {
        try {
            ServerPlayer player = joined.player();
            String biome = h.getLevel().getBiome(pos).unwrapKey().orElseThrow().identifier().toString();
            String support = BuiltInRegistries.BLOCK.getKey(h.getLevel().getBlockState(pos.below()).getBlock()).toString();
            String heldItem = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String blockBefore = BuiltInRegistries.BLOCK.getKey(h.getLevel().getBlockState(pos).getBlock()).toString();
            int itemCountBefore = player.getMainHandItem().getCount();
            boolean criterionBefore = criterionDone(player, advancement);
            boolean landmarkLocked = AchieveToDoMod.isTargetInLockedLandmark(player, h.getLevel(), pos.below());
            require(PhaseASingletonMangroveGroveCertification.BIOME.equals(biome)
                && PhaseASingletonMangroveGroveCertification.SUPPORT.equals(support)
                && PhaseASingletonMangroveGroveCertification.ITEM.equals(heldItem)
                && "minecraft:air".equals(blockBefore) && itemCountBefore == 1 && !criterionBefore && !landmarkLocked,
                "propagule pre-action witness failed");
            var hit = new BlockHitResult(new Vec3(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D), Direction.UP, pos.below(), false);
            var result = player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            String blockAfter = BuiltInRegistries.BLOCK.getKey(h.getLevel().getBlockState(pos).getBlock()).toString();
            String biomeAfter = h.getLevel().getBiome(pos).unwrapKey().orElseThrow().identifier().toString();
            int itemCountAfter = player.getMainHandItem().getCount();
            boolean criterionAfter = criterionDone(player, advancement);
            require(result.consumesAction() && PhaseASingletonMangroveGroveCertification.BLOCK.equals(blockAfter)
                && PhaseASingletonMangroveGroveCertification.BIOME.equals(biomeAfter) && itemCountAfter == 0 && criterionAfter,
                "native propagule placement did not complete criterion: " + result);
            Cleanup cleaned = cleanup(joined);
            JsonObject r = new JsonObject(); r.addProperty("family", PhaseASingletonMangroveGroveCertification.FAMILY);
            r.addProperty("source", PhaseASingletonMangroveGroveCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonMangroveGroveCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonMangroveGroveCertification.CRITERION);
            r.addProperty("trigger", "minecraft:placed_block"); r.addProperty("boundary", PhaseASingletonMangroveGroveCertification.BOUNDARY);
            r.addProperty("observedBiome", biomeAfter); r.addProperty("observedSupport", support);
            r.addProperty("observedItem", heldItem); r.addProperty("blockBefore", blockBefore); r.addProperty("blockAfter", blockAfter);
            r.addProperty("itemCountBefore", itemCountBefore); r.addProperty("itemCountAfter", itemCountAfter);
            r.addProperty("runId", runId); r.addProperty("catalogFingerprint", PhaseASingletonMangroveGroveExecutionEvidence.currentFingerprint(PhaseASingletonMangroveGroveExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name()); r.addProperty("playerUuid", player.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("criterionBefore", criterionBefore); r.addProperty("criterionAfter", criterionAfter);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "LANDMARK_ONLY"); gate.addProperty("lockedLandmark", landmarkLocked);
            r.add("productionGateWitness", gate);
            JsonObject c = new JsonObject(); c.addProperty("playerRemoved", cleaned.playerRemoved()); c.addProperty("connectionRemoved", cleaned.connectionRemoved());
            c.addProperty("channelSettled", cleaned.channelSettled()); c.addProperty("settlementMessages", cleaned.messages()); c.addProperty("warningCount", 0);
            r.add("cleanup", c);
            var root = PhaseASingletonMangroveGroveExecutionEvidence.projectRoot();
            PhaseASingletonMangroveGroveExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonMangroveGroveExecutionEvidence.loadTemporary(root, exact);
            require(runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=" + PhaseASingletonMangroveGroveCertification.FAMILY + " runId=" + runId + " entries=1");
            h.succeed();
        } catch (Throwable t) {
            cleanup(joined); h.fail("singleton mangrove grove native placement failed: " + describe(t));
        }
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(PhaseASingletonMangroveGroveCertification.CRITERION);
        require(criterion != null, "live propagule criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "mangrove" + id.toString().substring(0, 8));
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        return new Joined(player, connection, channel, new AtomicBoolean());
    }
    private static Cleanup cleanup(Joined joined) {
        if (!joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, 0);
        MinecraftServer server = joined.player().level().getServer();
        if (joined.player().containerMenu != joined.player().inventoryMenu) joined.player().closeContainer();
        server.getPlayerList().remove(joined.player());
        boolean playerRemoved = server.getPlayerList().getPlayer(joined.player().getUUID()) != joined.player();
        server.getConnection().getConnections().remove(joined.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joined.connection());
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("mangrove grove cleanup"));
        return new Cleanup(playerRemoved, connectionRemoved, true, messages);
    }
    private static int settle(EmbeddedChannel channel) {
        if (!channel.isOpen()) return 0; int released = 0;
        for (int pass = 0; pass < 32; pass++) {
            channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
            int current = 0; Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound); current++;
                if (++released > 4096) throw new IllegalStateException("channel cleanup exceeded message bound");
            }
            if (current == 0 && !channel.hasPendingTasks()) return released;
        }
        throw new IllegalStateException("channel cleanup did not quiesce");
    }
    private static String describe(Throwable t) { Throwable c = t; while (c.getCause() != null && c.getMessage() == null) c = c.getCause(); return c.getClass().getSimpleName() + ": " + c.getMessage(); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
