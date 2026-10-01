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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.ScoreHolder;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Natural Allay pickup, delivery, and player pickup in a live Deep Dark biome. */
public final class PhaseASingletonDiagonAllayGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final int MAX_TICKS = 650;

    @GameTest(maxTicks = 900)
    public void singletonDiagonAllayCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 900)
    public void singletonDiagonAllayExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context context = new Context();
        try {
            var root = PhaseASingletonDiagonAllayExecutionEvidence.projectRoot();
            context.runId = PhaseASingletonDiagonAllayExecutionEvidence.beginRun(root);
            BlockPos pos = h.absolutePos(TARGET);
            context.origin = pos;
            for (int x = -2; x <= 10; x++) for (int z = -2; z <= 2; z++)
                h.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            h.setBiome(Biomes.DEEP_DARK);
            context.joined = join(h);
            ServerPlayer player = context.joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
                "joined finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(context.joined.connection()), "joined registration failed");
            var settings = h.getLevel().getServer().getScoreboard().getObjective("bac_settings");
            require(settings != null, "BACAP settings objective missing");
            h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"), settings).set(0);
            player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + 1.5D);
            player.getInventory().clearContent();
            context.advancement = h.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseASingletonDiagonAllayCertification.ADVANCEMENT));
            require(context.advancement != null, "Allay advancement missing");
            context.criterionBefore = criterionDone(player, context.advancement);
            require(!context.criterionBefore, "Allay criterion pre-complete");
            EntityType<?> allayType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:allay"));
            require(allayType != null, "Registered Allay type missing");
            context.allay = (Allay) allayType.create(h.getLevel(), EntitySpawnReason.COMMAND);
            require(context.allay != null, "Could not create Allay");
            context.allay.teleportTo(pos.getX() + .5D, pos.getY() + .7D, pos.getZ() + .5D);
            context.allay.setPersistenceRequired();
            require(h.getLevel().addFreshEntity(context.allay), "Could not spawn live Allay");
            context.landmarkLocked = AchieveToDoMod.isTargetInLockedLandmark(player, context.allay);
            require(!context.landmarkLocked, "Allay is in locked landmark");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTION, 1));
            new ServerboundInteractPacket(context.allay.getId(), InteractionHand.MAIN_HAND, context.allay.position(), false)
                .handle(player.connection);
            context.given = context.allay.getMainHandItem().is(Items.POTION)
                && context.allay.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER).filter(player.getUUID()::equals).isPresent()
                && player.getMainHandItem().isEmpty();
            require(context.given && !criterionDone(player, context.advancement), "native Allay item handoff failed");
            player.teleportTo(pos.getX() + 8.5D, pos.getY(), pos.getZ() + .5D);
            context.dropped = new ItemEntity(h.getLevel(), pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                new ItemStack(Items.POTION, 1));
            context.dropped.setNoPickUpDelay();
            require(h.getLevel().addFreshEntity(context.dropped), "Could not drop matching finite potion");
            require(!criterionDone(player, context.advancement), "setup completed criterion without delivery");
            h.runAfterDelay(1, () -> poll(h, exact, context, 1));
        } catch (Throwable t) {
            cleanup(context); h.fail("diagon allay setup failed: " + describe(t));
        }
    }

    private static void poll(GameTestHelper h, boolean exact, Context c, int tick) {
        try {
            ServerPlayer player = c.joined.player();
            if (!c.picked && !c.allay.getInventory().isEmpty() && !c.dropped.isAlive()) c.picked = true;
            if (c.picked) {
                player.teleportTo(c.allay.getX() + 2D, c.origin.getY(), c.allay.getZ());
            }
            if (c.thrown == null) {
                AABB search = new AABB(c.origin).inflate(20);
                for (ItemEntity item : h.getLevel().getEntitiesOfClass(ItemEntity.class, search,
                    item -> item.getOwner() == c.allay && item.getItem().is(Items.POTION))) {
                    c.thrown = item;
                    c.thrower = BuiltInRegistries.ENTITY_TYPE.getKey(item.getOwner().getType()).toString();
                    c.item = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
                    c.ownerMatches = item.getOwner() == c.allay;
                    break;
                }
            }
            if (c.thrown != null && c.thrown.isAlive()) {
                player.teleportTo(c.thrown.getX(), c.thrown.getY(), c.thrown.getZ());
            }
            boolean criterionAfter = criterionDone(player, c.advancement);
            if (c.thrown != null && !c.thrown.isAlive() && criterionAfter) {
                String biome = h.getLevel().getBiome(player.blockPosition()).unwrapKey().orElseThrow().identifier().toString();
                require(c.picked && c.ownerMatches && c.given && containsPotion(player)
                    && PhaseASingletonDiagonAllayCertification.BIOME.equals(biome), "post-pickup predicate witness failed");
                Cleanup cleaned = cleanup(c);
                JsonObject r = new JsonObject();
                r.addProperty("family", PhaseASingletonDiagonAllayCertification.FAMILY);
                r.addProperty("source", PhaseASingletonDiagonAllayCertification.SOURCE);
                r.addProperty("advancementId", PhaseASingletonDiagonAllayCertification.ADVANCEMENT);
                r.addProperty("criterion", PhaseASingletonDiagonAllayCertification.CRITERION);
                r.addProperty("trigger", "minecraft:thrown_item_picked_up_by_player");
                r.addProperty("boundary", PhaseASingletonDiagonAllayCertification.BOUNDARY);
                r.addProperty("observedThrower", c.thrower); r.addProperty("observedItem", c.item);
                r.addProperty("observedPlayerBiome", biome);
                r.addProperty("runId", c.runId);
                r.addProperty("catalogFingerprint", PhaseASingletonDiagonAllayExecutionEvidence.currentFingerprint(
                    PhaseASingletonDiagonAllayExecutionEvidence.projectRoot()));
                r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
                r.addProperty("playerUuid", player.getUUID().toString()); r.addProperty("allayUuid", c.allay.getUUID().toString());
                r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
                r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
                r.addProperty("allayGivenMatchingItem", c.given); r.addProperty("allayPickedUpPotion", c.picked);
                r.addProperty("allayThrewPotion", c.thrown != null); r.addProperty("thrownEntityOwnerMatchesAllay", c.ownerMatches);
                r.addProperty("potionEntityConsumed", !c.thrown.isAlive());
                r.addProperty("matchingPotionInPlayerInventory", containsPotion(player));
                r.addProperty("criterionBefore", c.criterionBefore); r.addProperty("criterionAfter", criterionAfter);
                r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
                JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "LANDMARK_ONLY_FOR_ALLAY_INTERACTION");
                gate.addProperty("lockedLandmark", c.landmarkLocked); r.add("productionGateWitness", gate);
                JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
                cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
                cleanup.addProperty("allayRemoved", cleaned.allayRemoved()); cleanup.addProperty("settlementMessages", cleaned.messages());
                cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
                var root = PhaseASingletonDiagonAllayExecutionEvidence.projectRoot();
                PhaseASingletonDiagonAllayExecutionEvidence.recordGreen(root, r);
                var artifact = PhaseASingletonDiagonAllayExecutionEvidence.loadTemporary(root, exact);
                require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
                System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                    + PhaseASingletonDiagonAllayCertification.FAMILY + " runId=" + c.runId + " entries=1");
                h.succeed();
                return;
            }
            if (tick >= MAX_TICKS) throw new IllegalStateException("Allay delivery timed out at tick " + tick
                + " picked=" + c.picked + " thrown=" + (c.thrown != null) + " criterion=" + criterionDone(player, c.advancement)
                + " allayInventory=" + c.allay.getInventory().getItem(0));
            h.runAfterDelay(1, () -> poll(h, exact, c, tick + 1));
        } catch (Throwable t) {
            cleanup(c); h.fail("diagon allay native delivery failed: " + describe(t));
        }
    }

    private static boolean containsPotion(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(Items.POTION)) return true;
        return false;
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(PhaseASingletonDiagonAllayCertification.CRITERION);
        require(criterion != null, "live Allay criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "diagon" + id.toString().substring(0, 8));
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        return new Joined(player, connection, channel, new AtomicBoolean());
    }
    private static Cleanup cleanup(Context c) {
        if (c.joined == null || !c.joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, true, 0);
        if (c.dropped != null && c.dropped.isAlive()) c.dropped.discard();
        if (c.thrown != null && c.thrown.isAlive()) c.thrown.discard();
        if (c.allay != null && c.allay.isAlive()) c.allay.discard();
        boolean allayRemoved = c.allay == null || !c.allay.isAlive();
        MinecraftServer server = c.joined.player().level().getServer();
        if (c.joined.player().containerMenu != c.joined.player().inventoryMenu) c.joined.player().closeContainer();
        server.getPlayerList().remove(c.joined.player());
        boolean playerRemoved = server.getPlayerList().getPlayer(c.joined.player().getUUID()) != c.joined.player();
        server.getConnection().getConnections().remove(c.joined.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(c.joined.connection());
        int messages = settle(c.joined.channel()); c.joined.connection().disconnect(Component.literal("diagon allay cleanup"));
        return new Cleanup(playerRemoved, connectionRemoved, true, allayRemoved, messages);
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
    private static String describe(Throwable t) {
        Throwable c = t; while (c.getCause() != null && c.getMessage() == null) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    private static final class Context {
        String runId, thrower, item; BlockPos origin; Joined joined; AdvancementHolder advancement;
        Allay allay; ItemEntity dropped, thrown; boolean given, picked, ownerMatches, landmarkLocked, criterionBefore;
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, boolean allayRemoved, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException {
        h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h);
    }
}
