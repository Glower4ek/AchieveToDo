package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native milk-bucket consumption while poisoned at half a heart. */
public final class PhaseASingletonMiracleDrinkGameTest implements CustomTestMethodInvoker {
    private static final BlockPos PLAYER = new BlockPos(4, 2, 4);
    @GameTest(maxTicks = 240)
    public void singletonMiracleDrinkCanary(GameTestHelper h) { run(h, false); }
    @GameTest(maxTicks = 240)
    public void singletonMiracleDrinkExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context c = new Context(exact);
        try {
            var root = PhaseASingletonMiracleDrinkExecutionEvidence.projectRoot();
            c.runId = PhaseASingletonMiracleDrinkExecutionEvidence.beginRun(root);
            for (int x = 0; x <= 9; x++) for (int z = 0; z <= 8; z++)
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState());
            c.joined = join(h);
            ServerPlayer player = c.joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
                "joined finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()),
                "joined registration failed");
            var settings = h.getLevel().getServer().getScoreboard().getObjective("bac_settings");
            require(settings != null, "BACAP settings objective missing");
            h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(
                net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
            BlockPos pos = h.absolutePos(PLAYER);
            player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D);
            player.getInventory().clearContent();
            c.stack = new ItemStack(Items.MILK_BUCKET);
            player.setItemSlot(EquipmentSlot.MAINHAND, c.stack);
            player.getFoodData().setFoodLevel(10);
            var tag = TagKey.create(Registries.ITEM, Identifier.parse(PhaseASingletonMiracleDrinkCertification.ITEM_TAG));
            require(c.stack.is(tag), "live poison-cures tag does not contain milk bucket");
            c.tagVerified = true;
            player.setHealth(1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0));
            c.advancement = h.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT));
            require(c.advancement != null && !criterionDone(player, c.advancement), "miracle-drink criterion missing or pre-complete");
            h.runAfterDelay(2, () -> waitForScore(h, c, 2));
        } catch (Throwable t) { fail(h, c, "setup", t); }
    }
    private static void waitForScore(GameTestHelper h, Context c, int tick) {
        try {
            ServerPlayer player = c.joined.player();
            var scoreboard = h.getLevel().getServer().getScoreboard();
            var objective = scoreboard.getObjective(PhaseASingletonMiracleDrinkCertification.SCORE);
            require(objective != null && objective.getCriteria() == ObjectiveCriteria.HEALTH, "live health objective missing or wrong");
            var info = scoreboard.getPlayerScoreInfo(player, objective);
            int score = info == null ? -1 : info.value();
            if (score == 1) {
                require(player.getHealth() == 1.0F && player.hasEffect(MobEffects.POISON),
                    "half-heart poisoned precondition missing");
                c.scoreBefore = score;
                c.healthBefore = player.getHealth();
                c.poisonBefore = true;
                c.criterionBefore = criterionDone(player, c.advancement);
                require(!c.criterionBefore, "criterion completed before intended drink");
                c.joined.channel().writeInbound(new ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, 0, player.getYRot(), player.getXRot()));
                require(player.isUsingItem() && player.getUseItem() == c.stack, "native milk use did not start: using="
                    + player.isUsingItem() + " held=" + player.getMainHandItem() + " use=" + player.getUseItem()
                    + " sameStack=" + (player.getUseItem() == c.stack) + " food=" + player.getFoodData().getFoodLevel()
                    + " health=" + player.getHealth() + " loaded=" + player.connection.hasClientLoaded());
                c.packetAccepted = true;
                h.runAfterDelay(1, () -> poll(h, c, 1));
                return;
            }
            if (tick >= 40) throw new IllegalStateException("health scoreboard did not reach 1: score=" + score
                + " health=" + player.getHealth() + " criterion=" + criterionDone(player, c.advancement));
            h.runAfterDelay(1, () -> waitForScore(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "score", t); }
    }
    private static void poll(GameTestHelper h, Context c, int tick) {
        try {
            ServerPlayer player = c.joined.player();
            if (criterionDone(player, c.advancement) && !player.isUsingItem()) { finish(h, c, tick); return; }
            if (tick >= 100) throw new IllegalStateException("native milk consumption did not complete: using="
                + player.isUsingItem() + " remaining=" + player.getUseItemRemainingTicks()
                + " criterion=" + criterionDone(player, c.advancement)
                + " poison=" + player.hasEffect(MobEffects.POISON));
            h.runAfterDelay(1, () -> poll(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "consume", t); }
    }
    private static void finish(GameTestHelper h, Context c, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            boolean after = criterionDone(player, c.advancement);
            boolean poisonRemoved = !player.hasEffect(MobEffects.POISON);
            require(after && poisonRemoved && c.packetAccepted && c.tagVerified && c.poisonBefore
                && c.scoreBefore == 1 && c.healthBefore == 1.0F, "milk/poison/score transition failed");
            Cleanup cleaned = cleanup(c.joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonMiracleDrinkCertification.FAMILY);
            r.addProperty("source", PhaseASingletonMiracleDrinkCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonMiracleDrinkCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonMiracleDrinkCertification.CRITERION);
            r.addProperty("trigger", "minecraft:consume_item");
            r.addProperty("boundary", PhaseASingletonMiracleDrinkCertification.BOUNDARY);
            r.addProperty("observedItem", "minecraft:milk_bucket");
            r.addProperty("observedTag", PhaseASingletonMiracleDrinkCertification.ITEM_TAG);
            r.addProperty("observedEffect", "minecraft:poison");
            r.addProperty("runId", c.runId);
            r.addProperty("catalogFingerprint", PhaseASingletonMiracleDrinkExecutionEvidence.currentFingerprint(
                PhaseASingletonMiracleDrinkExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
            r.addProperty("playerUuid", player.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("tagVerified", c.tagVerified); r.addProperty("poisonBefore", c.poisonBefore);
            r.addProperty("healthBefore", c.healthBefore); r.addProperty("scoreObjective", PhaseASingletonMiracleDrinkCertification.SCORE);
            r.addProperty("scoreBefore", c.scoreBefore); r.addProperty("usePacketAccepted", c.packetAccepted);
            r.addProperty("poisonRemoved", poisonRemoved); r.addProperty("criterionBefore", c.criterionBefore);
            r.addProperty("criterionAfter", after); r.addProperty("ticksToConsume", ticks);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("settlementMessages", cleaned.messages()); cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
            var root = PhaseASingletonMiracleDrinkExecutionEvidence.projectRoot();
            PhaseASingletonMiracleDrinkExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonMiracleDrinkExecutionEvidence.loadTemporary(root, c.exact);
            require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonMiracleDrinkCertification.FAMILY + " runId=" + c.runId + " entries=1");
            h.succeed();
        } catch (Throwable t) { fail(h, c, "receipt", t); }
    }
    private static void fail(GameTestHelper h, Context c, String stage, Throwable t) {
        if (c.joined != null) cleanup(c.joined);
        h.fail("singleton miracle drink " + stage + " failed: " + describe(t));
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonMiracleDrinkCertification.CRITERION);
        require(criterion != null, "live miracle-drink criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "miracle" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("maximum miracleance cleanup"));
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
    private static final class Context {
        final boolean exact; String runId; Joined joined; AdvancementHolder advancement; ItemStack stack;
        boolean tagVerified, poisonBefore, criterionBefore, packetAccepted; int scoreBefore; float healthBefore;
        Context(boolean exact) { this.exact = exact; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
