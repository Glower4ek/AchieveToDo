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
import net.minecraft.world.level.portal.PortalShape;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** A real portal shape enters a registered ruined-portal structure lookup. */
public final class PhaseASingletonAncientRestorationGameTest implements CustomTestMethodInvoker {
    private static final BlockPos INNER = new BlockPos(5, 2, 5);
    @GameTest(maxTicks = 180)
    public void singletonAncientRestorationCanary(GameTestHelper h) { run(h, false); }
    @GameTest(maxTicks = 180)
    public void singletonAncientRestorationExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context c = new Context(exact);
        try {
            var root = PhaseASingletonAncientRestorationExecutionEvidence.projectRoot();
            c.runId = PhaseASingletonAncientRestorationExecutionEvidence.beginRun(root);
            BlockPos inner = h.absolutePos(INNER);
            for (int x = -1; x <= 2; x++) for (int y = -1; y <= 3; y++)
                h.getLevel().setBlockAndUpdate(inner.offset(x, y, 0),
                    x == -1 || x == 2 || y == -1 || y == 3 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
            c.frameBuilt = true;
            var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            Holder.Reference<Structure> ruined = structures.get(ResourceKey.create(Registries.STRUCTURE,
                Identifier.parse(PhaseASingletonAncientRestorationCertification.STRUCTURES[0])))
                .orElseThrow(() -> new IllegalStateException("registered ruined portal missing"));
            c.structure = SyntheticStructureHarness.inject(h.getLevel(), ruined, inner);
            c.structureSet = HolderSet.direct(ruined);
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
            player.teleportTo(inner.getX() + .5D, inner.getY(), inner.getZ() + 1.5D);
            c.advancement = h.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseASingletonAncientRestorationCertification.ADVANCEMENT));
            require(c.advancement != null && !criterionDone(player, c.advancement), "ancient-restoration criterion missing or pre-complete");
            c.criterionBefore = false;
            var shape = PortalShape.findEmptyPortalShape(h.getLevel(), inner, Direction.Axis.X)
                .orElseThrow(() -> new IllegalStateException("rebuilt obsidian frame is not a valid empty portal shape"));
            shape.createPortalBlocks(h.getLevel());
            c.nativePortalCreated = h.getLevel().getBlockState(inner).is(Blocks.NETHER_PORTAL);
            require(c.nativePortalCreated, "vanilla portal-shape activation failed");
            c.structureLookup = h.getLevel().structureManager().getStructureWithPieceAt(inner, c.structureSet)
                == c.structure.injectedStart();
            require(c.structureLookup, "ruined portal lookup failed at entry position");
            player.teleportTo(inner.getX() + .5D, inner.getY(), inner.getZ() + .5D);
            h.runAfterDelay(1, () -> poll(h, c, 1));
        } catch (Throwable t) { fail(h, c, "setup", t); }
    }
    private static void poll(GameTestHelper h, Context c, int tick) {
        try {
            ServerPlayer player = c.joined.player();
            if (criterionDone(player, c.advancement)) { finish(h, c, tick); return; }
            if (tick >= 80) throw new IllegalStateException("native portal entry did not complete: pos="
                + player.position() + " block=" + h.getLevel().getBlockState(player.blockPosition())
                + " lookup=" + (h.getLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), c.structureSet)
                    == c.structure.injectedStart()));
            h.runAfterDelay(1, () -> poll(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "entry", t); }
    }
    private static void finish(GameTestHelper h, Context c, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            boolean after = criterionDone(player, c.advancement);
            boolean blockWitness = h.getLevel().getBlockState(player.blockPosition()).is(Blocks.NETHER_PORTAL);
            boolean lookup = h.getLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), c.structureSet)
                == c.structure.injectedStart();
            require(after && blockWitness && lookup && c.frameBuilt && c.nativePortalCreated && c.structureLookup,
                "portal block/ruined-structure/criterion transition failed");
            boolean loaded = player.connection.hasClientLoaded();
            boolean registered = h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection());
            boolean joined = h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
            var structure = c.structure;
            structure.close();
            boolean restored = structure.chunk().getAllStarts().equals(structure.originalStarts())
                && structure.chunk().getAllReferences().equals(structure.originalReferences());
            require(restored, "structure state did not restore");
            c.structure = null;
            Cleanup cleaned = cleanup(c.joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonAncientRestorationCertification.FAMILY);
            r.addProperty("source", PhaseASingletonAncientRestorationCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonAncientRestorationCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonAncientRestorationCertification.CRITERION);
            r.addProperty("trigger", "minecraft:enter_block");
            r.addProperty("boundary", PhaseASingletonAncientRestorationCertification.BOUNDARY);
            r.addProperty("observedBlock", PhaseASingletonAncientRestorationCertification.BLOCK);
            r.addProperty("observedStructure", PhaseASingletonAncientRestorationCertification.STRUCTURES[0]);
            r.addProperty("runId", c.runId);
            r.addProperty("catalogFingerprint", PhaseASingletonAncientRestorationExecutionEvidence.currentFingerprint(
                PhaseASingletonAncientRestorationExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
            r.addProperty("playerUuid", player.getUUID().toString());
            r.addProperty("joined", joined); r.addProperty("connectionRegistered", registered);
            r.addProperty("clientLoaded", loaded); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("frameBuilt", c.frameBuilt); r.addProperty("nativePortalCreated", c.nativePortalCreated);
            r.addProperty("structureLookupBeforeEntry", c.structureLookup); r.addProperty("structureLookupAtEntry", lookup);
            r.addProperty("portalBlockAtEntry", blockWitness); r.addProperty("criterionBefore", c.criterionBefore);
            r.addProperty("criterionAfter", after); r.addProperty("ticksToEnter", ticks);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("structureRestored", restored);
            cleanup.addProperty("settlementMessages", cleaned.messages()); cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
            var root = PhaseASingletonAncientRestorationExecutionEvidence.projectRoot();
            PhaseASingletonAncientRestorationExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonAncientRestorationExecutionEvidence.loadTemporary(root, c.exact);
            require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonAncientRestorationCertification.FAMILY + " runId=" + c.runId + " entries=1");
            h.succeed();
        } catch (Throwable t) { fail(h, c, "receipt", t); }
    }
    private static void fail(GameTestHelper h, Context c, String stage, Throwable t) {
        if (c.structure != null) c.structure.close();
        if (c.joined != null) cleanup(c.joined);
        h.fail("singleton ancient restoration " + stage + " failed: " + describe(t));
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonAncientRestorationCertification.CRITERION);
        require(criterion != null, "live ancient-restoration criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "ancient" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("ancient restoration cleanup"));
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
        final boolean exact; String runId; Joined joined; AdvancementHolder advancement;
        SyntheticStructureHarness.InjectedStructure structure; HolderSet<Structure> structureSet;
        boolean frameBuilt, nativePortalCreated, structureLookup, criterionBefore;
        Context(boolean exact) { this.exact = exact; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
