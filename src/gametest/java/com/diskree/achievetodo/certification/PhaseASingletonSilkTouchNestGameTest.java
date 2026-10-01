package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Packet-driven SURVIVAL mining of a live three-bee nest. */
public final class PhaseASingletonSilkTouchNestGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);
    @GameTest(maxTicks = 180)
    public void singletonSilkTouchNestCanary(GameTestHelper h) { run(h, false); }
    @GameTest(maxTicks = 180)
    public void singletonSilkTouchNestExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context c = new Context(exact);
        try {
            c.runId = PhaseASingletonSilkTouchNestExecutionEvidence.beginRun(PhaseASingletonSilkTouchNestExecutionEvidence.projectRoot());
            c.pos = h.absolutePos(TARGET);
            h.getLevel().setBlockAndUpdate(c.pos.below(), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(c.pos, Blocks.BEE_NEST.defaultBlockState());
            c.hive = (BeehiveBlockEntity) h.getLevel().getBlockEntity(c.pos);
            require(c.hive != null, "live nest entity missing");
            for (int i = 0; i < 3; i++) c.hive.storeBee(BeehiveBlockEntity.Occupant.create(0));
            c.beesBefore = c.hive.getOccupantCount();
            require(c.beesBefore == 3, "three-bee nest fixture failed");
            c.joined = join(h); ServerPlayer player = c.joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && !player.hasInfiniteMaterials(), "finite client-loaded lifecycle failed");
            player.teleportTo(c.pos.getX() + .5D, c.pos.getY(), c.pos.getZ() + 1.5D);
            acknowledgeTeleport(c.joined);
            player.getInventory().clearContent(); c.tool = new ItemStack(Items.WOODEN_AXE);
            var silk = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(PhaseASingletonSilkTouchNestCertification.ENCHANTMENT)));
            c.tool.enchant(silk, 1); player.setItemSlot(EquipmentSlot.MAINHAND, c.tool);
            require(c.tool.getEnchantments().getLevel(silk) == 1, "live Silk Touch missing");
            c.advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseASingletonSilkTouchNestCertification.ADVANCEMENT));
            require(c.advancement != null && !criterionDone(player, c.advancement), "criterion missing or pre-complete");
            c.gates = unlock(h, player);
            c.damageBefore = c.tool.getDamageValue();
            require(!criterionDone(player, c.advancement) && c.hive.getOccupantCount() == 3, "pre-action fixture changed");
            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, c.pos, Direction.NORTH, 0).handle(player.connection);
            c.startPacket = true;
            h.runAfterDelay(10, () -> stop(h, c));
        } catch (Throwable failure) { fail(h, c, "setup", failure); }
    }
    private static JsonObject unlock(GameTestHelper h, ServerPlayer player) {
        var server = h.getLevel().getServer(); var scoreboard = server.getScoreboard();
        var score = scoreboard.getObjective("bac_advancements"); var settings = scoreboard.getObjective("bac_settings");
        require(score != null && settings != null, "production scoreboards missing");
        scoreboard.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
        var abilities = ((LevelInfoExtension) server.getWorldData().getLevelSettings()).achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        JsonObject gate = new JsonObject(); gate.addProperty("thresholdSource", "LIVE_OVERWORLD_SEED_CONFIGURATION");
        var before = scoreboard.getPlayerScoreInfo(player, score); int value = before == null ? 0 : before.value();
        gate.addProperty("scoreBefore", value); require(value == 0, "fresh score not zero");
        AbilityType[] required = {AbilityType.BREAK_BLOCKS, AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y, AbilityType.USE_WOODEN_TOOLS};
        int threshold = 0;
        for (var ability : required) {
            Integer cost = abilities.get(ability); require(cost != null && cost > 0, "live ability threshold missing");
            JsonObject witness = new JsonObject(); witness.addProperty("threshold", cost);
            boolean locked = AchieveToDoMod.isAbilityLocked(player, ability, true);
            require(locked, "fresh required ability already unlocked"); witness.addProperty("lockedBefore", locked);
            gate.add(ability.name(), witness); threshold = Math.max(threshold, cost);
        }
        scoreboard.getOrCreatePlayerScore(player, score).set(threshold);
        for (var ability : required) {
            boolean locked = AchieveToDoMod.isAbilityLocked(player, ability, true);
            gate.getAsJsonObject(ability.name()).addProperty("lockedAfter", locked); require(!locked, "ability remained locked");
        }
        gate.addProperty("requiredThreshold", threshold); gate.addProperty("scoreAfter", scoreboard.getPlayerScoreInfo(player, score).value());
        require(!player.blockActionRestricted(h.getLevel(), cpos(h), GameType.SURVIVAL), "production restricts the mining target");
        return gate;
    }
    private static BlockPos cpos(GameTestHelper h) { return h.absolutePos(TARGET); }
    private static void stop(GameTestHelper h, Context c) {
        try {
            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, c.pos, Direction.NORTH, 1).handle(c.joined.player().connection);
            c.stopPacket = true; poll(h, c, 10);
        } catch (Throwable failure) { fail(h, c, "stop", failure); }
    }
    private static void poll(GameTestHelper h, Context c, int ticks) {
        try {
            if (criterionDone(c.joined.player(), c.advancement)) { finish(h, c, ticks); return; }
            if (ticks >= 80) throw new IllegalStateException("native nest mining timed out: block=" + h.getLevel().getBlockState(c.pos));
            h.runAfterDelay(1, () -> poll(h, c, ticks + 1));
        } catch (Throwable failure) { fail(h, c, "mining", failure); }
    }
    private static void finish(GameTestHelper h, Context c, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            int bees = -1;
            for (var drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(c.pos).inflate(3))) {
                if (drop.getItem().is(Items.BEE_NEST)) bees = drop.getItem().getOrDefault(DataComponents.BEES, Bees.EMPTY).bees().size();
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.is(Items.BEE_NEST)) bees = stack.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees().size();
            }
            require(bees == 3 && h.getLevel().getBlockState(c.pos).isAir() && c.tool.getDamageValue() > c.damageBefore,
                "native bee-preserving drop/block/tool mutation missing: bees=" + bees);
            JsonObject receipt = new JsonObject();
            receipt.addProperty("family", PhaseASingletonSilkTouchNestCertification.FAMILY);
            receipt.addProperty("source", PhaseASingletonSilkTouchNestCertification.SOURCE);
            receipt.addProperty("advancementId", PhaseASingletonSilkTouchNestCertification.ADVANCEMENT);
            receipt.addProperty("criterion", PhaseASingletonSilkTouchNestCertification.CRITERION);
            receipt.addProperty("trigger", "minecraft:bee_nest_destroyed"); receipt.addProperty("boundary", PhaseASingletonSilkTouchNestCertification.BOUNDARY);
            receipt.addProperty("observedBlock", PhaseASingletonSilkTouchNestCertification.BLOCK);
            receipt.addProperty("observedEnchantment", PhaseASingletonSilkTouchNestCertification.ENCHANTMENT);
            receipt.addProperty("observedTool", "minecraft:wooden_axe"); receipt.addProperty("runId", c.runId);
            receipt.addProperty("catalogFingerprint", PhaseASingletonSilkTouchNestExecutionEvidence.currentFingerprint(PhaseASingletonSilkTouchNestExecutionEvidence.projectRoot()));
            receipt.addProperty("result", "GREEN"); receipt.addProperty("gameMode", player.gameMode().name()); receipt.addProperty("playerUuid", player.getUUID().toString());
            receipt.addProperty("joined", h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
            receipt.addProperty("connectionRegistered", h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection()));
            receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            receipt.addProperty("blockBefore", c.beesBefore == 3); receipt.addProperty("blockDestroyed", h.getLevel().getBlockState(c.pos).isAir());
            receipt.addProperty("nativeBeePreservingDrop", bees == 3); receipt.addProperty("startPacketSent", c.startPacket); receipt.addProperty("stopPacketSent", c.stopPacket);
            receipt.addProperty("beesBefore", c.beesBefore); receipt.addProperty("beesInDrop", bees); receipt.addProperty("silkTouchLevel", 1);
            receipt.addProperty("toolCountBefore", 1); receipt.addProperty("toolCountAfter", c.tool.getCount()); receipt.addProperty("toolDamageBefore", c.damageBefore); receipt.addProperty("toolDamageAfter", c.tool.getDamageValue());
            receipt.addProperty("criterionBefore", false); receipt.addProperty("criterionAfter", criterionDone(player, c.advancement)); receipt.addProperty("ticksToBreak", ticks);
            receipt.addProperty("noDirectCriterionTrigger", true); receipt.addProperty("noManualAward", true); receipt.add("productionGateWitness", c.gates);
            boolean dropsRemoved = removeDrops(h, c); Cleanup cleaned = cleanup(c.joined);
            JsonObject clean = new JsonObject(); clean.addProperty("playerRemoved", cleaned.playerRemoved()); clean.addProperty("connectionRemoved", cleaned.connectionRemoved());
            clean.addProperty("channelSettled", cleaned.channelSettled()); clean.addProperty("dropsRemoved", dropsRemoved); clean.addProperty("warningCount", 0); clean.addProperty("settlementMessages", cleaned.messages()); receipt.add("cleanup", clean);
            var root = PhaseASingletonSilkTouchNestExecutionEvidence.projectRoot(); PhaseASingletonSilkTouchNestExecutionEvidence.recordGreen(root, receipt);
            var artifact = PhaseASingletonSilkTouchNestExecutionEvidence.loadTemporary(root, c.exact); require(artifact.entryCount() == 1 && c.runId.equals(artifact.runId()), "TEMP mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=" + PhaseASingletonSilkTouchNestCertification.FAMILY + " runId=" + c.runId + " entries=1"); h.succeed();
        } catch (Throwable failure) { fail(h, c, "receipt", failure); }
    }
    private static boolean removeDrops(GameTestHelper h, Context c) {
        if (c.pos == null) return true;
        var area = new AABB(c.pos).inflate(3); for (var drop : h.getLevel().getEntitiesOfClass(ItemEntity.class, area)) drop.discard();
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty();
    }
    private static void fail(GameTestHelper h, Context c, String stage, Throwable failure) {
        removeDrops(h, c); if (c.joined != null) cleanup(c.joined);
        h.fail("singleton silk touch nest " + stage + " failed: " + describe(failure));
    }
    private static void acknowledgeTeleport(Joined joined) {
        var channel = joined.channel(); channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
        Integer id = null; Object packet;
        while ((packet = channel.readOutbound()) != null) { if (packet instanceof ClientboundPlayerPositionPacket position) id = position.id(); ReferenceCountUtil.release(packet); }
        require(id != null, "missing server teleport"); new ServerboundAcceptTeleportationPacket(id).handle(joined.player().connection);
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonSilkTouchNestCertification.CRITERION);
        require(criterion != null, "live silknest-restoration criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "silk" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("silknest restoration cleanup"));
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
        final boolean exact; String runId; Joined joined; AdvancementHolder advancement; BlockPos pos;
        BeehiveBlockEntity hive; ItemStack tool; int beesBefore, damageBefore; JsonObject gates; boolean startPacket, stopPacket;
        Context(boolean exact) { this.exact = exact; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
