package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Warden AI attacks a joined player wearing four Protection IV pieces with Resistance IV. */
public final class PhaseASingletonMaximumResistanceGameTest implements CustomTestMethodInvoker {
    private static final BlockPos PLAYER = new BlockPos(4, 2, 4);
    private static final BlockPos WARDEN = new BlockPos(5, 2, 4);
    private static final int MAX_HIT_TICKS = 240;

    @GameTest(maxTicks = 340)
    public void singletonMaximumResistanceCanary(GameTestHelper h) { run(h, false); }

    @GameTest(maxTicks = 340)
    public void singletonMaximumResistanceExact1(GameTestHelper h) { run(h, true); }

    private static void run(GameTestHelper h, boolean exact) {
        Context c = new Context(exact);
        try {
            var root = PhaseASingletonMaximumResistanceExecutionEvidence.projectRoot();
            c.runId = PhaseASingletonMaximumResistanceExecutionEvidence.beginRun(root);
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
            BlockPos playerPos = h.absolutePos(PLAYER);
            player.teleportTo(playerPos.getX() + .5D, playerPos.getY(), playerPos.getZ() + .5D);
            player.getInventory().clearContent();
            var enchantment = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(PhaseASingletonMaximumResistanceCertification.ENCHANTMENT)));
            ItemStack[] armor = {
                new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)
            };
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            c.armorVerified = true;
            for (int i = 0; i < armor.length; i++) {
                armor[i].enchant(enchantment, 4);
                player.setItemSlot(slots[i], armor[i]);
                c.armorVerified &= BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slots[i]).getItem()).toString()
                    .equals(PhaseASingletonMaximumResistanceCertification.ARMOR[i]);
                c.armorVerified &= player.getItemBySlot(slots[i]).getEnchantments().getLevel(enchantment) == 4;
            }
            require(c.armorVerified, "four live Protection IV netherite pieces missing");
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 3));
            c.resistanceBeforeHit = player.getEffect(MobEffects.RESISTANCE) != null
                && player.getEffect(MobEffects.RESISTANCE).getAmplifier() >= 3;
            require(c.resistanceBeforeHit, "live Resistance IV precondition missing");
            c.advancement = h.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseASingletonMaximumResistanceCertification.ADVANCEMENT));
            require(c.advancement != null && !criterionDone(player, c.advancement), "Warden-hit criterion missing or pre-complete");
            c.criterionBefore = false;
            c.healthBefore = player.getHealth();
            c.warden = (Warden) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:warden"))
                .create(h.getLevel(), EntitySpawnReason.COMMAND);
            require(c.warden != null, "Warden creation failed");
            BlockPos wardenPos = h.absolutePos(WARDEN);
            c.warden.teleportTo(wardenPos.getX() + .5D, wardenPos.getY(), wardenPos.getZ() + .5D);
            require(h.getLevel().addFreshEntity(c.warden), "Warden spawn failed");
            c.warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
            c.warden.increaseAngerAt(player, 150, true);
            h.runAfterDelay(1, () -> poll(h, c, 1));
        } catch (Throwable t) { fail(h, c, "setup", t); }
    }

    private static void poll(GameTestHelper h, Context c, int tick) {
        try {
            ServerPlayer player = c.joined.player();
            if (c.warden.getTarget() == player) c.wardenTargetConfirmed = true;
            boolean done = criterionDone(player, c.advancement);
            var damage = player.getLastDamageSource();
            boolean wardenDamage = damage != null && damage.getEntity() == c.warden
                && player.getHealth() < c.healthBefore;
            if (wardenDamage && done) {
                finish(h, c, tick);
                return;
            }
            if (tick >= MAX_HIT_TICKS) throw new IllegalStateException("bounded native Warden hit did not complete: target="
                + c.warden.getTarget() + " anger=" + c.warden.getAngerLevel()
                + " playerHealth=" + player.getHealth() + " damage=" + damage + " criterion=" + done
                + " wardenPos=" + c.warden.position() + " playerPos=" + player.position()
                + " distance=" + c.warden.distanceTo(player) + " pose=" + c.warden.getPose()
                + " noAi=" + c.warden.isNoAi() + " canAttack=" + c.warden.canAttack(player)
                + " canTarget=" + c.warden.canTargetEntity(player)
                + " meleeRange=" + c.warden.isWithinMeleeAttackRange(player)
                + " activity=" + c.warden.getBrain().getActiveNonCoreActivity()
                + " visible=" + c.warden.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                    .map(entities -> entities.contains(player))
                + " attackCooldown=" + c.warden.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN)
                + " navDone=" + c.warden.getNavigation().isDone());
            h.runAfterDelay(1, () -> poll(h, c, tick + 1));
        } catch (Throwable t) { fail(h, c, "native hit", t); }
    }

    private static void finish(GameTestHelper h, Context c, int ticks) {
        try {
            ServerPlayer player = c.joined.player();
            boolean after = criterionDone(player, c.advancement);
            boolean sourceWarden = player.getLastDamageSource() != null
                && player.getLastDamageSource().getEntity() == c.warden;
            boolean damage = player.getHealth() < c.healthBefore;
            boolean effect = player.getEffect(MobEffects.RESISTANCE) != null
                && player.getEffect(MobEffects.RESISTANCE).getAmplifier() >= 3;
            require(after && sourceWarden && damage && effect && c.armorVerified && c.wardenTargetConfirmed,
                "Warden source/protection/health transition witness failed");
            String observedSource = BuiltInRegistries.ENTITY_TYPE.getKey(c.warden.getType()).toString();
            c.warden.discard(); boolean wardenRemoved = c.warden.isRemoved();
            Cleanup cleaned = cleanup(c.joined);
            JsonObject r = new JsonObject();
            r.addProperty("family", PhaseASingletonMaximumResistanceCertification.FAMILY);
            r.addProperty("source", PhaseASingletonMaximumResistanceCertification.SOURCE);
            r.addProperty("advancementId", PhaseASingletonMaximumResistanceCertification.ADVANCEMENT);
            r.addProperty("criterion", PhaseASingletonMaximumResistanceCertification.CRITERION);
            r.addProperty("trigger", "minecraft:entity_hurt_player");
            r.addProperty("boundary", PhaseASingletonMaximumResistanceCertification.BOUNDARY);
            r.addProperty("observedSourceEntity", observedSource);
            r.addProperty("observedEnchantment", PhaseASingletonMaximumResistanceCertification.ENCHANTMENT);
            r.addProperty("observedEffect", PhaseASingletonMaximumResistanceCertification.EFFECT);
            r.addProperty("runId", c.runId);
            r.addProperty("catalogFingerprint", PhaseASingletonMaximumResistanceExecutionEvidence.currentFingerprint(
                PhaseASingletonMaximumResistanceExecutionEvidence.projectRoot()));
            r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
            r.addProperty("playerUuid", player.getUUID().toString()); r.addProperty("wardenUuid", c.warden.getUUID().toString());
            r.addProperty("joined", cleaned.playerRemoved()); r.addProperty("connectionRegistered", cleaned.connectionRemoved());
            r.addProperty("clientLoaded", player.connection.hasClientLoaded()); r.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            r.addProperty("armorVerified", c.armorVerified); r.addProperty("armorPieces", 4);
            r.addProperty("protectionLevelEach", 4); r.addProperty("resistanceBeforeHit", c.resistanceBeforeHit);
            r.addProperty("resistanceAmplifier", player.getEffect(MobEffects.RESISTANCE).getAmplifier());
            r.addProperty("nativeWardenTargetAcquired", c.wardenTargetConfirmed);
            r.addProperty("nativeWardenDamage", damage); r.addProperty("unblocked", true);
            r.addProperty("sourceWarden", sourceWarden); r.addProperty("healthBefore", c.healthBefore);
            r.addProperty("healthAfter", player.getHealth()); r.addProperty("criterionBefore", c.criterionBefore);
            r.addProperty("criterionAfter", after); r.addProperty("ticksToHit", ticks);
            r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true);
            JsonObject gate = new JsonObject(); gate.addProperty("productionGate", "NO_WARDEN_DAMAGE_GATE");
            r.add("productionGateWitness", gate);
            JsonObject cleanup = new JsonObject(); cleanup.addProperty("playerRemoved", cleaned.playerRemoved());
            cleanup.addProperty("connectionRemoved", cleaned.connectionRemoved()); cleanup.addProperty("channelSettled", cleaned.channelSettled());
            cleanup.addProperty("wardenRemoved", wardenRemoved); cleanup.addProperty("settlementMessages", cleaned.messages());
            cleanup.addProperty("warningCount", 0); r.add("cleanup", cleanup);
            var root = PhaseASingletonMaximumResistanceExecutionEvidence.projectRoot();
            PhaseASingletonMaximumResistanceExecutionEvidence.recordGreen(root, r);
            var artifact = PhaseASingletonMaximumResistanceExecutionEvidence.loadTemporary(root, c.exact);
            require(c.runId.equals(artifact.runId()) && artifact.entryCount() == 1, "TEMP singleton mismatch");
            System.out.println((c.exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family="
                + PhaseASingletonMaximumResistanceCertification.FAMILY + " runId=" + c.runId + " entries=1");
            h.succeed();
        } catch (Throwable t) { fail(h, c, "receipt", t); }
    }

    private static void fail(GameTestHelper h, Context c, String stage, Throwable t) {
        if (c.warden != null) c.warden.discard();
        if (c.joined != null) cleanup(c.joined);
        h.fail("singleton maximum resistance " + stage + " failed: " + describe(t));
    }
    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        var criterion = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseASingletonMaximumResistanceCertification.CRITERION);
        require(criterion != null, "live maximum-resistance criterion missing"); return criterion.isDone();
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "resist" + id.toString().substring(0, 8));
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
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("maximum resistance cleanup"));
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
        final boolean exact; String runId; Joined joined; Warden warden; AdvancementHolder advancement;
        boolean criterionBefore, armorVerified, resistanceBeforeHit, wardenTargetConfirmed;
        float healthBefore;
        Context(boolean exact) { this.exact = exact; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
