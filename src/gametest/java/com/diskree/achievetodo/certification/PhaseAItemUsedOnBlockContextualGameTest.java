package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native item-on-block witness for the frozen Meadows jukebox singleton. */
public final class PhaseAItemUsedOnBlockContextualGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 400)
    public void itemUsedOnBlockContextualCanary(GameTestHelper helper) { run(helper, false); }

    @GameTest(maxTicks = 400)
    public void itemUsedOnBlockContextualExact1(GameTestHelper helper) { run(helper, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Joined joined = null;
        try {
            var root = PhaseAItemUsedOnBlockContextualExecutionEvidence.projectRoot();
            String runId = PhaseAItemUsedOnBlockContextualExecutionEvidence.beginRun(root);
            BlockPos pos = h.absolutePos(TARGET);
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            h.setBiome(Biomes.MEADOW);
            h.getLevel().setBlockAndUpdate(pos, Blocks.JUKEBOX.defaultBlockState());
            require(h.getLevel().getBlockEntity(pos) instanceof JukeboxBlockEntity, "live jukebox block entity missing");
            joined = join(h);
            ServerPlayer player = joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(), "finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(joined.connection()), "joined registration failed");
            player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + 1.5D);
            player.getInventory().clearContent();
            AdvancementHolder advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID));
            require(advancement != null, "live jukebox advancement missing");
            require(!criterionDone(player, advancement), "criterion done before native interaction");
            JsonObject gate = unlockJukebox(h, player, advancement);
            ItemStack disc = new ItemStack(Items.MUSIC_DISC_13, 1);
            require(disc.has(DataComponents.JUKEBOX_PLAYABLE), "selected disc has no live jukebox-playable component");
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), disc);
            require(player.getMainHandItem() == disc && disc.getCount() == 1, "finite selected disc fixture failed");
            require(!criterionDone(player, advancement), "disc fixture completed criterion");
            Joined kept = joined;
            h.runAfterDelay(1, () -> interact(h, exact, runId, kept, advancement, gate, pos));
        } catch (Throwable t) {
            if (joined != null) cleanup(joined);
            h.fail("contextual jukebox setup failed: " + describe(t));
        }
    }

    private static void interact(GameTestHelper h, boolean exact, String runId, Joined joined,
                                 AdvancementHolder advancement, JsonObject gate, BlockPos pos) {
        try {
            ServerPlayer player = joined.player();
            String biome = h.getLevel().getBiome(pos).unwrapKey().orElseThrow().identifier().toString();
            String block = BuiltInRegistries.BLOCK.getKey(h.getLevel().getBlockState(pos).getBlock()).toString();
            String disc = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            require(PhaseAItemUsedOnBlockContextualCertification.BIOME.equals(biome)
                && PhaseAItemUsedOnBlockContextualCertification.BLOCK.equals(block)
                && PhaseAItemUsedOnBlockContextualCertification.DISC.equals(disc), "live biome/block/disc mismatch");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, h.getLevel(), pos), "jukebox fixture is in a locked landmark");
            require(!criterionDone(player, advancement), "criterion done before useItemOn");
            boolean playable = player.getMainHandItem().has(DataComponents.JUKEBOX_PLAYABLE);
            require(playable && player.getMainHandItem().getCount() == 1, "playable disc precondition failed");
            var hit = new BlockHitResult(new Vec3(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D), Direction.UP, pos, false);
            var result = player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            require(result.consumesAction(), "jukebox interaction did not consume action: " + result);
            JukeboxBlockEntity jukebox = (JukeboxBlockEntity) h.getLevel().getBlockEntity(pos);
            boolean accepted = jukebox != null && jukebox.getTheItem().is(Items.MUSIC_DISC_13);
            require(accepted && player.getMainHandItem().isEmpty() && criterionDone(player, advancement),
                "native jukebox action did not insert disc and complete criterion");
            Cleanup cleaned = cleanup(joined);
            JsonObject receipt = new JsonObject();
            receipt.addProperty("family", PhaseAItemUsedOnBlockContextualCertification.FAMILY);
            receipt.addProperty("source", PhaseAItemUsedOnBlockContextualCertification.SOURCE);
            receipt.addProperty("advancementId", PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID);
            receipt.addProperty("criterion", PhaseAItemUsedOnBlockContextualCertification.CRITERION);
            receipt.addProperty("trigger", "minecraft:item_used_on_block");
            receipt.addProperty("boundary", PhaseAItemUsedOnBlockContextualCertification.BOUNDARY);
            receipt.addProperty("observedBiome", biome); receipt.addProperty("observedBlock", block); receipt.addProperty("observedDisc", disc);
            receipt.addProperty("runId", runId);
            receipt.addProperty("catalogFingerprint", PhaseAItemUsedOnBlockContextualExecutionEvidence.currentFingerprint(PhaseAItemUsedOnBlockContextualExecutionEvidence.projectRoot()));
            receipt.addProperty("result", "GREEN"); receipt.addProperty("gameMode", player.gameMode().name());
            receipt.addProperty("playerUuid", player.getUUID().toString());
            receipt.addProperty("joined", cleaned.playerRemoved()); receipt.addProperty("connectionRegistered", cleaned.connectionRemoved());
            receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            receipt.addProperty("toolPlayable", playable); receipt.addProperty("jukeboxAcceptedDisc", accepted);
            receipt.addProperty("criterionBefore", false); receipt.addProperty("criterionAfter", true);
            receipt.addProperty("discCountBefore", 1); receipt.addProperty("discCountAfter", player.getMainHandItem().getCount());
            receipt.addProperty("noDirectCriterionTrigger", true); receipt.addProperty("noManualAward", true);
            receipt.add("productionUnlockWitness", gate.deepCopy());
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("settlementMessages", cleaned.messages()); cleanup.addProperty("warningCount", 0); receipt.add("cleanup", cleanup);
            var root = PhaseAItemUsedOnBlockContextualExecutionEvidence.projectRoot();
            PhaseAItemUsedOnBlockContextualExecutionEvidence.recordGreen(root, receipt);
            var artifact = PhaseAItemUsedOnBlockContextualExecutionEvidence.loadTemporary(root, exact);
            require(artifact.runId().equals(runId) && artifact.entryCount() == 1, "TEMP singleton run mismatch");
            System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=ITEM_USED_ON_BLOCK_CONTEXTUAL runId=" + runId + " entries=1");
            h.succeed();
        } catch (Throwable t) {
            cleanup(joined);
            h.fail("contextual jukebox native action failed: " + describe(t));
        }
    }

    private static JsonObject unlockJukebox(GameTestHelper h, ServerPlayer player, AdvancementHolder advancement) {
        MinecraftServer server = h.getLevel().getServer(); Scoreboard board = server.getScoreboard();
        Objective objective = board.getObjective("bac_advancements");
        require(objective != null && AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT, "default advancement objective missing");
        Objective settings = board.getObjective("bac_settings"); require(settings != null, "BACAP settings objective missing");
        board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"), settings).set(0);
        Map<AbilityType, Integer> abilities = ((LevelInfoExtension) server.getWorldData().getLevelSettings())
            .achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        Integer threshold = abilities.get(AbilityType.USE_JUKEBOX); require(threshold != null && threshold > 0, "live jukebox threshold missing");
        var beforeInfo = board.getPlayerScoreInfo(player, objective); int before = beforeInfo == null ? 0 : beforeInfo.value();
        require(before == 0 && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_JUKEBOX, true), "fresh jukebox gate was not locked");
        board.getOrCreatePlayerScore(player, objective).set(threshold);
        var afterInfo = board.getPlayerScoreInfo(player, objective); int after = afterInfo == null ? 0 : afterInfo.value();
        require(after == threshold && !AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_JUKEBOX, true), "jukebox gate did not unlock at live threshold");
        require(!criterionDone(player, advancement), "unlock fixture completed criterion");
        JsonObject gate = new JsonObject(); gate.addProperty("ability", AbilityType.USE_JUKEBOX.name());
        gate.addProperty("scoreboardObjective", objective.getName()); gate.addProperty("thresholdSource", "live_overworld_seed_abilities_configuration");
        gate.addProperty("requiredThreshold", threshold); gate.addProperty("scoreBefore", before); gate.addProperty("scoreAfter", after);
        gate.addProperty("abilityLockedBefore", true); gate.addProperty("abilityLockedAfter", false); return gate;
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(PhaseAItemUsedOnBlockContextualCertification.CRITERION);
        require(criterion != null, "live jukebox criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "jukebox" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("contextual jukebox cleanup"));
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
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { helper.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, helper); }
}
