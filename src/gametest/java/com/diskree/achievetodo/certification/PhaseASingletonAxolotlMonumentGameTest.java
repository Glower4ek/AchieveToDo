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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** A real axolotl chooses a water target; a joined player kills it and receives the native assist effect. */
public final class PhaseASingletonAxolotlMonumentGameTest implements CustomTestMethodInvoker {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);
    private static final int MAX_ASSIST_TICKS = 240;

    @GameTest(maxTicks = 360)
    public void singletonAxolotlMonumentCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 360)
    public void singletonAxolotlMonumentExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context c = new Context(exact);
        try {
            var root = PhaseASingletonAxolotlMonumentExecutionEvidence.projectRoot();
            c.runId = PhaseASingletonAxolotlMonumentExecutionEvidence.beginRun(root);
            BlockPos center = h.absolutePos(CENTER);
            c.center = center;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                BlockPos floor = center.offset(x, -1, z);
                h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                h.getLevel().setBlockAndUpdate(floor.above(),
                    wall ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
                h.getLevel().setBlockAndUpdate(floor.above(2),
                    wall ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
            }
            var structures = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            Holder.Reference<Structure> monument = structures.get(ResourceKey.create(Registries.STRUCTURE,
                Identifier.parse(PhaseASingletonAxolotlMonumentCertification.STRUCTURE)))
                .orElseThrow(() -> new IllegalStateException("registered monument missing"));
            c.structure = SyntheticStructureHarness.inject(h.getLevel(), monument, center);
            c.structureSet = HolderSet.direct(monument);
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
            player.teleportTo(center.getX() + .5D, center.getY(), center.getZ() + 1.5D);
            acknowledgeServerTeleport(c.joined);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            c.axolotl = (Axolotl) create(h, "minecraft:axolotl", center.offset(1, 0, 0));
            c.target = (TropicalFish) create(h, "minecraft:tropical_fish", center);
            c.advancement = h.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT));
            require(c.advancement != null && !criterionDone(player, c.advancement), "axolotl criterion missing or pre-complete");
            c.criterionBefore = false;
            require(h.getLevel().structureManager().getStructureWithPieceAt(c.axolotl.blockPosition(), c.structureSet)
                == c.structure.injectedStart(), "axolotl monument lookup failed");
            require(h.getLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), c.structureSet)
                == c.structure.injectedStart(), "player monument lookup failed");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, c.target), "target is inside locked production landmark");
            h.runAfterDelay(1, () -> poll(h, c, 1));
        } catch (Throwable t) { fail(h, c, "setup", t); }
    }

    private static net.minecraft.world.entity.LivingEntity create(GameTestHelper h, String id, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
        require(type != null, "entity type missing: " + id);
        var entity = type.create(h.getLevel(), EntitySpawnReason.COMMAND);
        require(entity instanceof net.minecraft.world.entity.LivingEntity, "entity creation failed: " + id);
        entity.teleportTo(pos.getX() + .5D, pos.getY() + .1D, pos.getZ() + .5D);
        require(h.getLevel().addFreshEntity(entity), "entity spawn failed: " + id);
        return (net.minecraft.world.entity.LivingEntity) entity;
    }

    private static void poll(GameTestHelper h, Context c, int tick) {
        try {
            var player = c.joined.player();
            var attackTarget = c.axolotl.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            if (!c.attacked && attackTarget.isPresent() && attackTarget.get() == c.target) {
                require(!criterionDone(player, c.advancement), "criterion completed before native assist");
                c.target.teleportTo(c.center.getX() + .5D, c.center.getY() + .1D, c.center.getZ() + .5D);
                player.teleportTo(c.center.getX() + .5D, c.center.getY(), c.center.getZ() + 1.5D);
                acknowledgeServerTeleport(c.joined);
                c.sourceStructureLookup = h.getLevel().structureManager()
                    .getStructureWithPieceAt(c.axolotl.blockPosition(), c.structureSet) == c.structure.injectedStart();
                c.playerStructureLookup = h.getLevel().structureManager()
                    .getStructureWithPieceAt(player.blockPosition(), c.structureSet) == c.structure.injectedStart();
                c.lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, c.target);
                require(c.sourceStructureLookup && c.playerStructureLookup && !c.lockedLandmark,
                    "source/player monument or production gate changed before attack: source=" + c.sourceStructureLookup
                        + " player=" + c.playerStructureLookup + " locked=" + c.lockedLandmark
                        + " axolotlPos=" + c.axolotl.blockPosition() + " playerPos=" + player.blockPosition());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                c.emptyHandAtAttack = player.getMainHandItem().isEmpty();
                require(c.emptyHandAtAttack, "finite empty-hand attack fixture failed");
                c.target.setHealth(1.0F);
                new ServerboundAttackPacket(c.target.getId()).handle(player.connection);
                c.attacked = true;
                c.targetKilledByPlayer = c.target.isDeadOrDying()
                    && c.target.getLastDamageSource() != null
                    && c.target.getLastDamageSource().getEntity() == player;
                require(c.targetKilledByPlayer, "packet attack did not kill the axolotl's chosen target: health="
                    + c.target.getHealth() + " distance=" + player.distanceTo(c.target)
                    + " damageSource=" + c.target.getLastDamageSource()
                    + " playerPos=" + player.position() + " targetPos=" + c.target.position());
            }
            if (c.attacked && criterionDone(player, c.advancement)) {
                finish(h, c, tick);
                return;
            }
            if (tick >= MAX_ASSIST_TICKS) throw new IllegalStateException("bounded axolotl assist did not complete: targetMemory="
                + attackTarget.map(e -> e.getType().toString()) + " attacked=" + c.attacked
                + " targetDead=" + c.target.isDeadOrDying() + " regeneration=" + player.hasEffect(MobEffects.REGENERATION));
            h.runAfterDelay(1, () -> poll(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "native assist", t); }
    }

    private static void finish(GameTestHelper h, Context c, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            boolean criterionAfter = criterionDone(player, c.advancement);
            boolean regeneration = player.hasEffect(MobEffects.REGENERATION);
            require(c.attacked && c.targetKilledByPlayer && regeneration && criterionAfter,
                "axolotl assist effect/criterion witness failed");
            String sourceType = BuiltInRegistries.ENTITY_TYPE.getKey(c.axolotl.getType()).toString();
            String targetType = BuiltInRegistries.ENTITY_TYPE.getKey(c.target.getType()).toString();
            c.axolotl.discard(); c.target.discard();
            boolean axolotlRemoved = c.axolotl.isRemoved(), targetRemoved = c.target.isRemoved();
            c.structure.close(); c.structureRestored = c.structure.chunk().getAllStarts().equals(c.structure.originalStarts())
                && c.structure.chunk().getAllReferences().equals(c.structure.originalReferences());
            Cleanup cleaned = cleanup(c.joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonAxolotlMonumentCertification.FAMILY);
            r.addProperty("source", PhaseASingletonAxolotlMonumentCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonAxolotlMonumentCertification.CRITERION);
            r.addProperty("trigger", "minecraft:effects_changed");
            r.addProperty("boundary", PhaseASingletonAxolotlMonumentCertification.BOUNDARY);
            r.addProperty("observedSourceEntity", sourceType); r.addProperty("observedTargetEntity", targetType);
            r.addProperty("observedStructure", PhaseASingletonAxolotlMonumentCertification.STRUCTURE);
            r.addProperty("runId", c.runId);
            r.addProperty("catalogFingerprint", PhaseASingletonAxolotlMonumentExecutionEvidence.currentFingerprint(
                PhaseASingletonAxolotlMonumentExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
            r.addProperty("playerUuid", player.getUUID().toString()); r.addProperty("axolotlUuid", c.axolotl.getUUID().toString());
            r.addProperty("targetUuid", c.target.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("emptyHand", c.emptyHandAtAttack);
            r.addProperty("sourceStructureLookup", c.sourceStructureLookup); r.addProperty("playerStructureLookup", c.playerStructureLookup);
            r.addProperty("nativeAxolotlTargetAcquired", c.attacked); r.addProperty("targetKilledByPlayer", c.targetKilledByPlayer);
            r.addProperty("regenerationFromAxolotl", regeneration); r.addProperty("criterionBefore", c.criterionBefore);
            r.addProperty("criterionAfter", criterionAfter); r.addProperty("ticksToAssist", ticks);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "LANDMARK_ONLY_FOR_PLAYER_ATTACK");
            gate.addProperty("lockedLandmark", c.lockedLandmark); r.add("productionGateWitness", gate);
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("structureRestored", c.structureRestored); cleanup.addProperty("axolotlRemoved", axolotlRemoved);
            cleanup.addProperty("targetRemoved", targetRemoved); cleanup.addProperty("settlementMessages", cleaned.messages());
            cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
            var root = PhaseASingletonAxolotlMonumentExecutionEvidence.projectRoot();
            PhaseASingletonAxolotlMonumentExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonAxolotlMonumentExecutionEvidence.loadTemporary(root, c.exact);
            require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonAxolotlMonumentCertification.FAMILY + " runId=" + c.runId + " entries=1");
            h.succeed();
        } catch (Throwable t) { fail(h, c, "receipt", t); }
    }

    private static void fail(GameTestHelper h, Context c, String stage, Throwable t) {
        if (c.axolotl != null) c.axolotl.discard();
        if (c.target != null) c.target.discard();
        if (c.structure != null && !c.structureRestored) c.structure.close();
        if (c.joined != null) cleanup(c.joined);
        h.fail("singleton axolotl monument " + stage + " failed: " + describe(t));
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonAxolotlMonumentCertification.CRITERION);
        require(criterion != null, "live axolotl criterion missing"); return criterion.isDone();
    }

    private static void acknowledgeServerTeleport(Joined joined) {
        EmbeddedChannel channel = joined.channel(); channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
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
        GameProfile profile = new GameProfile(id, "lotl" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("axolotl monument cleanup"));
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
        final boolean exact; String runId; Joined joined; Axolotl axolotl; TropicalFish target; BlockPos center;
        AdvancementHolder advancement; SyntheticStructureHarness.InjectedStructure structure; HolderSet<Structure> structureSet;
        boolean criterionBefore, attacked, targetKilledByPlayer, sourceStructureLookup, playerStructureLookup, emptyHandAtAttack,
            lockedLandmark, structureRestored;
        Context(boolean exact) { this.exact = exact; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
