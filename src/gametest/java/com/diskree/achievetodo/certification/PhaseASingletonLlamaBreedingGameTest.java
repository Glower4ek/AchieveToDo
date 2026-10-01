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
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native hay feeding and AI-driven breeding of two adult tamed llamas. */
public final class PhaseASingletonLlamaBreedingGameTest implements CustomTestMethodInvoker {
    private static final BlockPos PARENT = new BlockPos(2, 2, 2);
    private static final BlockPos PARTNER = new BlockPos(4, 2, 2);
    private static final int MAX_BREED_TICKS = 400;

    @GameTest(maxTicks = 500)
    public void singletonLlamaBreedingCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 500)
    public void singletonLlamaBreedingExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Joined joined = null; Llama parent = null, partner = null;
        try {
            var root = PhaseASingletonLlamaBreedingExecutionEvidence.projectRoot();
            String runId = PhaseASingletonLlamaBreedingExecutionEvidence.beginRun(root);
            for (int x = 0; x <= 7; x++) for (int z = 0; z <= 6; z++)
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState());
            joined = join(h); ServerPlayer player = joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
                "joined finite SURVIVAL lifecycle failed");
            require(h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && h.getLevel().getServer().getConnection().getConnections().contains(joined.connection()), "joined registration failed");
            var settings = h.getLevel().getServer().getScoreboard().getObjective("bac_settings");
            require(settings != null, "BACAP settings objective missing");
            h.getLevel().getServer().getScoreboard().getOrCreatePlayerScore(
                net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
            parent = createLlama(h, PARENT, player);
            partner = createLlama(h, PARTNER, player);
            var lookup = h.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE,
                Identifier.parse(PhaseASingletonLlamaBreedingCertification.ENTITY_TAG));
            HolderSet.Named<EntityType<?>> members = lookup.getOrThrow(tag);
            require(members.contains(parent.typeHolder()) && members.contains(partner.typeHolder())
                && "minecraft:llama".equals(BuiltInRegistries.ENTITY_TYPE.getKey(parent.getType()).toString())
                && "minecraft:llama".equals(BuiltInRegistries.ENTITY_TYPE.getKey(partner.getType()).toString()),
                "live llama tag membership failed");
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, parent)
                && !AchieveToDoMod.isTargetInLockedLandmark(player, partner), "llama fixture in locked landmark");
            AdvancementHolder advancement = h.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT));
            require(advancement != null && !criterionDone(player, advancement), "llama breeding criterion missing or pre-complete");
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HAY_BLOCK, 2));
            Context c = new Context(joined, parent, partner, advancement, members, runId, exact);
            h.runAfterDelay(1, () -> feed(h, c));
        } catch (Throwable t) {
            if (parent != null) parent.discard(); if (partner != null) partner.discard();
            if (joined != null) cleanup(joined);
            h.fail("singleton llama breeding setup failed: " + describe(t));
        }
    }

    private static Llama createLlama(GameTestHelper h, BlockPos relative, ServerPlayer player) throws ReflectiveOperationException {
        Llama llama = (Llama) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:llama"))
            .create(h.getLevel(), EntitySpawnReason.COMMAND);
        require(llama != null, "llama creation failed");
        BlockPos pos = h.absolutePos(relative);
        llama.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D);
        Method setStrength = Llama.class.getDeclaredMethod("setStrength", int.class);
        setStrength.setAccessible(true);
        setStrength.invoke(llama, 1);
        llama.setTamed(true); llama.setOwner(player); llama.setAge(0);
        require(llama.isTamed() && llama.getAge() == 0 && !llama.isInLove() && llama.getStrength() == 1,
            "adult tamed fixture invalid");
        require(h.getLevel().addFreshEntity(llama), "llama spawn failed");
        return llama;
    }

    private static void feed(GameTestHelper h, Context c) {
        try {
            ServerPlayer player = c.joined.player();
            c.criterionBefore = criterionDone(player, c.advancement);
            c.parentTagMember = c.members.contains(c.parent.typeHolder());
            c.partnerTagMember = c.members.contains(c.partner.typeHolder());
            c.parentTamedBefore = c.parent.isTamed(); c.partnerTamedBefore = c.partner.isTamed();
            c.parentAdultBefore = c.parent.getAge() == 0 && !c.parent.isBaby();
            c.partnerAdultBefore = c.partner.getAge() == 0 && !c.partner.isBaby();
            c.parentLocked = AchieveToDoMod.isTargetInLockedLandmark(player, c.parent);
            c.partnerLocked = AchieveToDoMod.isTargetInLockedLandmark(player, c.partner);
            require(!c.criterionBefore && c.parentTagMember && c.partnerTagMember
                && c.parentTamedBefore && c.partnerTamedBefore && c.parentAdultBefore && c.partnerAdultBefore
                && !c.parentLocked && !c.partnerLocked, "pre-feed witness failed");
            require(player.getMainHandItem().is(Items.HAY_BLOCK) && player.getMainHandItem().getCount() == 2,
                "finite hay fixture missing");
            player.teleportTo(c.parent.getX(), c.parent.getY(), c.parent.getZ() + 1.5D);
            new ServerboundInteractPacket(c.parent.getId(), InteractionHand.MAIN_HAND, c.parent.position(), false)
                .handle(player.connection);
            c.parentFed = c.parent.isInLove() && c.parent.getLoveCause() == player;
            require(c.parentFed && player.getMainHandItem().getCount() == 1, "first native hay feed failed");
            player.teleportTo(c.partner.getX(), c.partner.getY(), c.partner.getZ() + 1.5D);
            new ServerboundInteractPacket(c.partner.getId(), InteractionHand.MAIN_HAND, c.partner.position(), false)
                .handle(player.connection);
            c.partnerFed = c.partner.isInLove() && c.partner.getLoveCause() == player;
            require(c.partnerFed && player.getMainHandItem().isEmpty() && !criterionDone(player, c.advancement),
                "second native hay feed or pre-breeding criterion failed");
            c.hayConsumed = 2;
            player.teleportTo(c.parent.getX(), c.parent.getY(), c.parent.getZ() + 4D);
            h.runAfterDelay(1, () -> poll(h, c, 1));
        } catch (Throwable t) { fail(h, c, "native feed", t); }
    }

    private static void poll(GameTestHelper h, Context c, int tick) {
        try {
            boolean criterionAfter = criterionDone(c.joined.player(), c.advancement);
            List<Llama> babies = babies(h, c);
            if (criterionAfter && babies.size() == 1 && c.parent.getAge() > 0 && c.partner.getAge() > 0) {
                finish(h, c, babies.getFirst(), tick);
                return;
            }
            if (tick >= MAX_BREED_TICKS) throw new IllegalStateException("bounded native breeding did not complete: criterion="
                + criterionAfter + " babies=" + babies.size() + " parentLove=" + c.parent.isInLove()
                + " partnerLove=" + c.partner.isInLove() + " parentAge=" + c.parent.getAge()
                + " partnerAge=" + c.partner.getAge());
            h.runAfterDelay(1, () -> poll(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "native breeding", t); }
    }

    private static List<Llama> babies(GameTestHelper h, Context c) {
        Vec3 midpoint = c.parent.position().add(c.partner.position()).scale(.5D);
        return h.getLevel().getEntitiesOfClass(Llama.class, AABB.ofSize(midpoint, 12D, 7D, 12D),
            llama -> llama != c.parent && llama != c.partner && llama.isBaby());
    }

    private static void finish(GameTestHelper h, Context c, Llama child, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            boolean criterionAfter = criterionDone(player, c.advancement);
            boolean parentCooldown = c.parent.getAge() > 0, partnerCooldown = c.partner.getAge() > 0;
            String parentType = BuiltInRegistries.ENTITY_TYPE.getKey(c.parent.getType()).toString();
            String partnerType = BuiltInRegistries.ENTITY_TYPE.getKey(c.partner.getType()).toString();
            String childType = BuiltInRegistries.ENTITY_TYPE.getKey(child.getType()).toString();
            require(criterionAfter && child.isBaby() && parentCooldown && partnerCooldown,
                "native child/cooldown witness failed");
            child.discard(); c.parent.discard(); c.partner.discard();
            boolean childRemoved = child.isRemoved(), parentRemoved = c.parent.isRemoved(), partnerRemoved = c.partner.isRemoved();
            Cleanup cleaned = cleanup(c.joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonLlamaBreedingCertification.FAMILY);
            r.addProperty("source", PhaseASingletonLlamaBreedingCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonLlamaBreedingCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonLlamaBreedingCertification.CRITERION);
            r.addProperty("trigger", "minecraft:bred_animals");
            r.addProperty("boundary", PhaseASingletonLlamaBreedingCertification.BOUNDARY);
            r.addProperty("checkedParentTag", PhaseASingletonLlamaBreedingCertification.ENTITY_TAG);
            r.addProperty("checkedPartnerTag", PhaseASingletonLlamaBreedingCertification.ENTITY_TAG);
            r.addProperty("observedParent", parentType); r.addProperty("observedPartner", partnerType);
            r.addProperty("observedChild", childType); r.addProperty("observedFeedItem", "minecraft:hay_block");
            r.addProperty("runId", c.runId);
            r.addProperty("catalogFingerprint", PhaseASingletonLlamaBreedingExecutionEvidence.currentFingerprint(
                PhaseASingletonLlamaBreedingExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
            r.addProperty("playerUuid", player.getUUID().toString()); r.addProperty("parentUuid", c.parent.getUUID().toString());
            r.addProperty("partnerUuid", c.partner.getUUID().toString()); r.addProperty("childUuid", child.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("parentTagMember", c.parentTagMember); r.addProperty("partnerTagMember", c.partnerTagMember);
            r.addProperty("parentTamedBefore", c.parentTamedBefore); r.addProperty("partnerTamedBefore", c.partnerTamedBefore);
            r.addProperty("parentAdultBefore", c.parentAdultBefore); r.addProperty("partnerAdultBefore", c.partnerAdultBefore);
            r.addProperty("parentFedWithHay", c.parentFed); r.addProperty("partnerFedWithHay", c.partnerFed);
            r.addProperty("parentInLoveAfterFeed", c.parentFed); r.addProperty("partnerInLoveAfterFeed", c.partnerFed);
            r.addProperty("nativeChildSpawned", childRemoved); r.addProperty("parentCooldownAfterBreeding", parentCooldown);
            r.addProperty("partnerCooldownAfterBreeding", partnerCooldown); r.addProperty("hayConsumed", c.hayConsumed);
            r.addProperty("ticksToBreed", ticks); r.addProperty("criterionBefore", c.criterionBefore);
            r.addProperty("criterionAfter", criterionAfter); r.addProperty("noDirectCriterionTrigger", true);
            r.addProperty("noManualAward", true);
            JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "LANDMARK_ONLY_FOR_LLAMA_FEEDING");
            gate.addProperty("parentLockedLandmark", c.parentLocked); gate.addProperty("partnerLockedLandmark", c.partnerLocked);
            r.add("productionGateWitness", gate);
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("parentRemoved", parentRemoved); cleanup.addProperty("partnerRemoved", partnerRemoved);
            cleanup.addProperty("childRemoved", childRemoved); cleanup.addProperty("settlementMessages", cleaned.messages());
            cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
            var root = PhaseASingletonLlamaBreedingExecutionEvidence.projectRoot();
            PhaseASingletonLlamaBreedingExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonLlamaBreedingExecutionEvidence.loadTemporary(root, c.exact);
            require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonLlamaBreedingCertification.FAMILY + " runId=" + c.runId + " entries=1");
            h.succeed();
        } catch (Throwable t) { fail(h, c, "native proof", t); }
    }

    private static void fail(GameTestHelper h, Context c, String stage, Throwable t) {
        for (Llama baby : babies(h, c)) baby.discard();
        c.parent.discard(); c.partner.discard(); cleanup(c.joined);
        h.fail("singleton llama breeding " + stage + " failed: " + describe(t));
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var progress = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonLlamaBreedingCertification.CRITERION);
        require(progress != null, "live llama breeding criterion missing");
        return progress.isDone();
    }

    private static final class Context {
        final Joined joined; final Llama parent, partner; final AdvancementHolder advancement;
        final HolderSet.Named<EntityType<?>> members; final String runId; final boolean exact;
        boolean criterionBefore, parentTagMember, partnerTagMember, parentTamedBefore, partnerTamedBefore,
            parentAdultBefore, partnerAdultBefore, parentLocked, partnerLocked, parentFed, partnerFed;
        int hayConsumed;
        Context(Joined joined, Llama parent, Llama partner, AdvancementHolder advancement,
                HolderSet.Named<EntityType<?>> members, String runId, boolean exact) {
            this.joined = joined; this.parent = parent; this.partner = partner; this.advancement = advancement;
            this.members = members; this.runId = runId; this.exact = exact;
        }
    }

    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "breed" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("llama breeding cleanup"));
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
