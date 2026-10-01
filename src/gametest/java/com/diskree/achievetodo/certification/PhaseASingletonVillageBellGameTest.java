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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Packet-driven bell ring with a real vanilla structure lookup at the clicked block. */
public final class PhaseASingletonVillageBellGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 400)
    public void singletonVillageBellCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 400)
    public void singletonVillageBellExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Joined joined = null;
        SyntheticStructureHarness.InjectedStructure injected = null;
        try {
            var root = PhaseASingletonVillageBellExecutionEvidence.projectRoot();
            String runId = PhaseASingletonVillageBellExecutionEvidence.beginRun(root);
            BlockPos pos = h.absolutePos(TARGET);
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, Blocks.BELL.defaultBlockState());
            require(h.getLevel().getBlockState(pos).is(Blocks.BELL)
                && h.getLevel().getBlockEntity(pos) instanceof BellBlockEntity, "live bell fixture missing");
            var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            TagKey<Structure> villageTag = TagKey.create(Registries.STRUCTURE,
                Identifier.parse(PhaseASingletonVillageBellCertification.STRUCTURE_TAG));
            HolderSet<Structure> members = structures.getOrThrow(villageTag);
            Holder.Reference<Structure> plains = structures.get(ResourceKey.create(Registries.STRUCTURE,
                Identifier.parse(PhaseASingletonVillageBellCertification.SELECTED_STRUCTURE)))
                .orElseThrow(() -> new IllegalStateException("registered plains village missing"));
            require(members.contains(plains), "selected plains village is not in live BACAP village tag");
            injected = SyntheticStructureHarness.inject(h.getLevel(), plains, pos);
            joined = join(h); ServerPlayer player = joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
                "joined finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(joined.connection()), "joined registration failed");
            var settings = h.getLevel().getServer().getScoreboard().getObjective("bac_settings");
            require(settings != null, "BACAP settings objective missing");
            h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
            player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + 1.5D);
            acknowledgeServerTeleport(joined);
            player.getInventory().clearContent();
            AdvancementHolder advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseASingletonVillageBellCertification.ADVANCEMENT));
            require(advancement != null && !criterionDone(player, advancement), "bell criterion missing or pre-complete");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, h.getLevel(), pos), "bell is in a locked landmark");
            var visible = h.getLevel().structureManager().getStructureWithPieceAt(pos, members);
            require(visible == injected.injectedStart() && visible.isValid() && visible.getStructure() == plains.value()
                && injected.pieceBox().isInside(pos), "vanilla village structure lookup failed at bell origin");
            Joined kept = joined; SyntheticStructureHarness.InjectedStructure structure = injected;
            h.runAfterDelay(1, () -> ring(h, exact, runId, kept, advancement, pos, members, plains, structure));
        } catch (Throwable t) {
            if (injected != null) injected.close();
            if (joined != null) cleanup(joined);
            h.fail("singleton village bell setup failed: " + describe(t));
        }
    }

    private static void ring(GameTestHelper h, boolean exact, String runId, Joined joined, AdvancementHolder advancement,
                             BlockPos pos, HolderSet<Structure> members, Holder.Reference<Structure> plains,
                             SyntheticStructureHarness.InjectedStructure injected) {
        boolean structureRestored = false;
        try {
            ServerPlayer player = joined.player();
            String observedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(h.getLevel().getBlockState(pos).getBlock()).toString();
            boolean tagMember = members.contains(plains);
            boolean lookup = h.getLevel().structureManager().getStructureWithPieceAt(pos, members) == injected.injectedStart();
            boolean emptyHand = player.getMainHandItem().isEmpty();
            boolean criterionBefore = criterionDone(player, advancement);
            boolean landmarkLocked = AchieveToDoMod.isTargetInLockedLandmark(player, h.getLevel(), pos);
            require(PhaseASingletonVillageBellCertification.BLOCK.equals(observedBlock) && tagMember && lookup
                && emptyHand && !criterionBefore && !landmarkLocked, "bell pre-action witness failed");
            BellBlockEntity bell = (BellBlockEntity) h.getLevel().getBlockEntity(pos);
            require(bell != null && !bell.shaking, "bell already ringing");
            var hit = new BlockHitResult(new Vec3(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .8D), Direction.SOUTH, pos, false);
            new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 1).handle(player.connection);
            boolean bellRang = bell.shaking;
            boolean criterionAfter = criterionDone(player, advancement);
            require(bellRang && criterionAfter, "native packet bell ring did not satisfy criterion: bellRang="
                + bellRang + " criterionAfter=" + criterionAfter + " playerPos=" + player.blockPosition()
                + " bellPos=" + pos + " hit=" + hit.getLocation());
            injected.close(); structureRestored = injected.chunk().getAllStarts().equals(injected.originalStarts())
                && injected.chunk().getAllReferences().equals(injected.originalReferences());
            require(structureRestored, "structure fixture was not restored");
            Cleanup cleaned = cleanup(joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonVillageBellCertification.FAMILY);
            r.addProperty("source", PhaseASingletonVillageBellCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonVillageBellCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonVillageBellCertification.CRITERION);
            r.addProperty("trigger", "minecraft:any_block_use");
            r.addProperty("boundary", PhaseASingletonVillageBellCertification.BOUNDARY);
            r.addProperty("observedBlock", observedBlock);
            r.addProperty("checkedStructureTag", PhaseASingletonVillageBellCertification.STRUCTURE_TAG);
            r.addProperty("observedStructure", plains.unwrapKey().orElseThrow().identifier().toString());
            r.addProperty("runId", runId);
            r.addProperty("catalogFingerprint", PhaseASingletonVillageBellExecutionEvidence.currentFingerprint(PhaseASingletonVillageBellExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name()); r.addProperty("playerUuid", player.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("emptyHand", emptyHand); r.addProperty("structureTagMember", tagMember);
            r.addProperty("vanillaStructureLookup", lookup); r.addProperty("bellRang", bellRang);
            r.addProperty("criterionBefore", criterionBefore); r.addProperty("criterionAfter", criterionAfter);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "LANDMARK_ONLY_FOR_BELL_RING");
            gate.addProperty("lockedLandmark", landmarkLocked); r.add("productionGateWitness", gate);
            JsonObject c = new JsonObject(); c.addProperty("playerRemoved", cleaned.playerRemoved());
            c.addProperty("connectionRemoved", cleaned.connectionRemoved()); c.addProperty("channelSettled", cleaned.channelSettled());
            c.addProperty("structureRestored", structureRestored); c.addProperty("settlementMessages", cleaned.messages());
            c.addProperty("warningCount", 0); r.add("cleanup", c);
            var root = PhaseASingletonVillageBellExecutionEvidence.projectRoot();
            PhaseASingletonVillageBellExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonVillageBellExecutionEvidence.loadTemporary(root, exact);
            require(runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonVillageBellCertification.FAMILY + " runId=" + runId + " entries=1");
            h.succeed();
        } catch (Throwable t) {
            if (!structureRestored) injected.close();
            cleanup(joined); h.fail("singleton village bell native ring failed: " + describe(t));
        }
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(PhaseASingletonVillageBellCertification.CRITERION);
        require(criterion != null, "live click criterion missing"); return criterion.isDone();
    }
    private static void acknowledgeServerTeleport(Joined joined) {
        EmbeddedChannel channel = joined.channel();
        channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
        Integer teleportId = null; Object outgoing;
        while ((outgoing = channel.readOutbound()) != null) {
            if (outgoing instanceof ClientboundPlayerPositionPacket position) teleportId = position.id();
            ReferenceCountUtil.release(outgoing);
        }
        require(teleportId != null, "server did not send a teleport for client acknowledgement");
        new ServerboundAcceptTeleportationPacket(teleportId).handle(joined.player().connection);
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "click" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("village bell cleanup"));
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
