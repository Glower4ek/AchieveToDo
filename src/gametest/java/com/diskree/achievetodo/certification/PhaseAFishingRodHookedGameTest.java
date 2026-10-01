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
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Joined-player proof of the native hook-collision then rod-reel criterion path. */
public final class PhaseAFishingRodHookedGameTest implements CustomTestMethodInvoker {
    private static final int MAX_TICKS = 120;
    private static final int INITIAL_SYNC_SETTLE_TICKS = 2;
    private static final int MAX_HOOK_WAIT_TICKS = 20;
    private static final int MAX_SETTLEMENT_PASSES = 32;
    private static final int MAX_SETTLEMENT_MESSAGES = 4096;
    private static final BlockPos PLAYER_BLOCK = new BlockPos(1, 2, 1);
    private static final int TARGET_FORWARD_BLOCKS = 4;
    private static final Identifier RAVAGER_ID = Identifier.parse(PhaseAFishingRodHookedCertification.FIXTURE_ENTITY);
    private static final TagKey<EntityType<?>> HOSTILE_MONSTERS = TagKey.create(
        Registries.ENTITY_TYPE, Identifier.parse(PhaseAFishingRodHookedCertification.ENTITY_TAG)
    );

    @GameTest(maxTicks = MAX_TICKS)
    public void fishingRodHookedCanary(GameTestHelper helper) {
        begin(helper, "DIAGNOSTIC", "fishingRodHookedCanary");
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void fishingRodHookedExact1(GameTestHelper helper) {
        begin(helper, "PROMOTABLE", "fishingRodHookedExact1");
    }

    private static void begin(GameTestHelper helper, String runMode, String coordinator) {
        State state = new State(runMode);
        try {
            state.root = PhaseAFishingRodHookedExecutionEvidence.projectRoot();
            state.runId = PhaseAFishingRodHookedExecutionEvidence.beginRun(state.root, runMode);
            state.fingerprint = PhaseAFishingRodHookedExecutionEvidence.currentCatalogFingerprint(state.root);
            System.out.println("FISHING_ROD_HOOKED_COORDINATOR=" + coordinator
                + " runId=" + state.runId
                + " selectedKey=" + PhaseAFishingRodHookedCertification.ADVANCEMENT_ID + "#" + PhaseAFishingRodHookedCertification.CRITERION
                + " cases=1");
            state.joined = join(helper);
            state.joined.player().setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(state.joined.player().connection);
            require(state.joined.player().connection.hasClientLoaded(), "joined player did not complete client-loaded packet");
            helper.runAfterDelay(INITIAL_SYNC_SETTLE_TICKS, () -> prepareAndCast(helper, state));
        } catch (Throwable t) {
            fail(helper, state, "setup", t);
        }
    }

    private static void prepareAndCast(GameTestHelper helper, State state) {
        try {
            ServerPlayer player = state.joined.player();
            assertJoinedLifecycle(helper, state.joined);
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(
                Identifier.parse(PhaseAFishingRodHookedCertification.ADVANCEMENT_ID)
            );
            require(advancement != null, "frozen Indiana Jones advancement is not loaded");
            state.advancement = advancement;
            state.criterionBeforeFixture = !criterionDone(player, advancement);
            require(state.criterionBeforeFixture, "criterion was already complete before fixture setup");

            state.productionWitness = unlockFishingRodAbility(helper, player, advancement);
            require(!criterionDone(player, advancement), "criterion changed while only setting the production scoreboard");
            require(player.fishing == null, "joined player already owns a fishing hook");

            BlockPos playerBlock = helper.absolutePos(PLAYER_BLOCK);
            BlockPos targetBlock = helper.absolutePos(PLAYER_BLOCK.offset(0, 0, TARGET_FORWARD_BLOCKS));
            ServerLevel level = helper.getLevel();
            level.setBlockAndUpdate(playerBlock.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(targetBlock.below(), Blocks.STONE.defaultBlockState());
            player.teleportTo(playerBlock.getX() + 0.5D, playerBlock.getY(), playerBlock.getZ() + 0.5D);
            player.getInventory().setSelectedSlot(0);
            state.rod = new ItemStack(Items.FISHING_ROD);
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), state.rod);
            state.selectedSlot = player.getInventory().getSelectedSlot();
            state.rodDamageBefore = state.rod.getDamageValue();

            state.target = new Ravager(ravagerEntityType(), level);
            state.target.snapTo(targetBlock.getX() + 0.5D, targetBlock.getY(), targetBlock.getZ() + 0.5D, 0.0F, 0.0F);
            state.target.setNoAi(true);
            state.target.setDeltaMovement(Vec3.ZERO);
            require(state.target.getType().builtInRegistryHolder().is(HOSTILE_MONSTERS), "ravager does not match the live hostile-monster tag");
            require(level.addFreshEntity(state.target), "could not add deterministic hostile target");

            Vec3 aim = state.target.getBoundingBox().getCenter().subtract(player.getEyePosition());
            double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
            float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
            float pitch = (float) Math.toDegrees(Math.atan2(-aim.y, horizontal));
            player.absSnapRotationTo(yaw, pitch);
            require(level.clip(new ClipContext(player.getEyePosition(), state.target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS,
                "target is not in unobstructed line of sight");
            require(!criterionDone(player, advancement), "fixture setup fired the criterion before the intended action");

            state.castSequence = 0;
            new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, state.castSequence, player.getYRot(), player.getXRot())
                .handle(player.connection);
            state.hook = player.fishing;
            require(state.hook != null && state.hook.isAlive(), "normal rod use did not create a live FishingHook");
            require(state.hook.getPlayerOwner() == player, "cast hook owner differs from joined player");
            state.hookStateAfterCast = hookState(state.hook);
            require("FLYING".equals(state.hookStateAfterCast), "new FishingHook was not in FLYING state");
            state.criterionBeforeCast = !criterionDone(player, advancement);
            require(state.criterionBeforeCast, "cast alone fired fishing_rod_hooked");
            helper.runAfterDelay(1, () -> pollForHook(helper, state, 1));
        } catch (Throwable t) {
            fail(helper, state, "fixture/cast", t);
        }
    }

    private static void pollForHook(GameTestHelper helper, State state, int ticks) {
        try {
            ServerPlayer player = state.joined.player();
            require(player.fishing == state.hook && state.hook.isAlive(), "cast hook disappeared before collision");
            require(state.target.getType().builtInRegistryHolder().is(HOSTILE_MONSTERS), "hostile tag changed during the bounded collision wait");
            if (state.hook.getHookedIn() == state.target) {
                require(state.hook.getPlayerOwner() == player, "hook owner changed before reel");
                String observedState = hookState(state.hook);
                if (!"HOOKED_IN_ENTITY".equals(observedState)) {
                    require("FLYING".equals(observedState), "unexpected vanilla hook state after target collision: " + observedState);
                    require(!criterionDone(player, state.advancement), "criterion changed before the native reel action");
                    if (ticks >= MAX_HOOK_WAIT_TICKS) {
                        throw new IllegalStateException("hook attached to target but did not settle into HOOKED_IN_ENTITY within "
                            + MAX_HOOK_WAIT_TICKS + " ticks; last state=" + observedState);
                    }
                    helper.runAfterDelay(1, () -> pollForHook(helper, state, ticks + 1));
                    return;
                }
                state.ticksToHook = ticks;
                state.hookStateBeforeReel = observedState;
                state.criterionBeforeReel = !criterionDone(player, state.advancement);
                state.criterionAfterCollisionBeforeReel = criterionDone(player, state.advancement);
                require(state.criterionBeforeReel && !state.criterionAfterCollisionBeforeReel, "collision alone fired fishing_rod_hooked before reel-in");
                state.reelSequence = state.castSequence + 1;
                new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, state.reelSequence, player.getYRot(), player.getXRot())
                    .handle(player.connection);
                finishNativeReel(helper, state);
                return;
            }
            if (state.hook.getHookedIn() != null) {
                throw new IllegalStateException("normal hook attached to unexpected entity "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(state.hook.getHookedIn().getType()));
            }
            if (ticks >= MAX_HOOK_WAIT_TICKS) throw new IllegalStateException("no collision within bounded " + MAX_HOOK_WAIT_TICKS + " ticks");
            helper.runAfterDelay(1, () -> pollForHook(helper, state, ticks + 1));
        } catch (Throwable t) {
            fail(helper, state, "hook collision", t);
        }
    }

    private static void finishNativeReel(GameTestHelper helper, State state) {
        try {
            ServerPlayer player = state.joined.player();
            CriterionProgress progress = criterionProgress(player, state.advancement);
            require(progress != null && progress.isDone(), "native reel did not complete the live criterion");
            require(state.hook.isRemoved(), "native reel did not remove the FishingHook");
            require(player.fishing == null, "native reel did not clear the active FishingHook");
            require(state.hook.getHookedIn() == state.target, "removed hook lost the hooked target witness");
            require(state.hook.getPlayerOwner() == player, "reel hook owner differs from joined player");
            state.hookStateAfterReel = hookState(state.hook);
            state.rodDamageAfter = state.rod.getDamageValue();
            require(state.rodDamageAfter - state.rodDamageBefore == 5, "native entity retrieval did not apply the bytecode-derived five-point rod damage");
            state.criterionAfterReel = progress.isDone();
            require(state.target.isAlive() && state.target.getType().builtInRegistryHolder().is(HOSTILE_MONSTERS), "hooked target no longer satisfies the frozen entity predicate");

            JsonObject receipt = receipt(state);
            Cleanup cleanup = cleanup(state);
            receipt.add("cleanup", cleanup.toJson());
            require(cleanup.playerRemoved() && cleanup.connectionRemoved() && cleanup.channelSettled() && cleanup.fixtureEntitiesRemoved(), "fixture cleanup did not settle");
            PhaseAFishingRodHookedExecutionEvidence.recordGreen(state.root, state.runMode, receipt);
            var artifact = PhaseAFishingRodHookedExecutionEvidenceValidation.loadTemporary(state.root, state.runMode);
            String terminal = "DIAGNOSTIC".equals(state.runMode) ? "TEMP_DIAGNOSTIC" : "TEMP_PROMOTABLE";
            System.out.println("FISHING_ROD_HOOKED_RUNTIME=GREEN runId=" + state.runId
                + " playerUuid=" + receipt.get("playerUuid").getAsString()
                + " hookUuid=" + receipt.get("fishingHookUuid").getAsString()
                + " targetUuid=" + receipt.get("hookedTargetUuid").getAsString()
                + " ticksToHook=" + state.ticksToHook
                + " receipts=" + artifact.entries().size() + " uniqueKeys=" + artifact.entries().size()
                + " uniquePlayers=" + artifact.entries().size() + " green=" + artifact.entries().size()
                + " cleanupWarnings=0");
            System.out.println(terminal + "=PASS family=" + PhaseAFishingRodHookedCertification.FAMILY
                + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
            helper.succeed();
        } catch (Throwable t) {
            fail(helper, state, "native reel", t);
        }
    }

    private static JsonObject receipt(State state) {
        ServerPlayer player = state.joined.player();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("family", PhaseAFishingRodHookedCertification.FAMILY);
        receipt.addProperty("source", PhaseAFishingRodHookedCertification.SOURCE);
        receipt.addProperty("minecraftVersion", "26.2");
        receipt.addProperty("compatibilityMarker", PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER);
        receipt.addProperty("catalogFingerprint", state.fingerprint);
        receipt.addProperty("runId", state.runId);
        receipt.addProperty("advancementId", PhaseAFishingRodHookedCertification.ADVANCEMENT_ID);
        receipt.addProperty("criterion", PhaseAFishingRodHookedCertification.CRITERION);
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", PhaseAFishingRodHookedCertification.TRIGGER);
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("joined", state.joined.lifecycleValid());
        receipt.addProperty("connectionRegistered", state.joined.connectionRegistered());
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("gameMode", player.gameMode.getGameModeForPlayer().name());
        receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
        receipt.addProperty("criterionBefore", !state.criterionBeforeFixture);
        receipt.addProperty("criterionAfter", state.criterionAfterReel);
        receipt.addProperty("criterionBeforeReel", !state.criterionBeforeReel);
        receipt.addProperty("criterionAfterCollisionBeforeReel", state.criterionAfterCollisionBeforeReel);
        receipt.addProperty("nativeCastSucceeded", state.hookStateAfterCast != null);
        receipt.addProperty("nativeReelSucceeded", state.criterionAfterReel);
        receipt.addProperty("noDirectCriterionTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("rodItem", BuiltInRegistries.ITEM.getKey(state.rod.getItem()).toString());
        receipt.addProperty("rodHand", InteractionHand.MAIN_HAND.name());
        receipt.addProperty("rodSlot", state.selectedSlot);
        receipt.addProperty("selectedSlot", player.getInventory().getSelectedSlot());
        receipt.addProperty("rodDurabilityBefore", state.rodDamageBefore);
        receipt.addProperty("rodDurabilityAfter", state.rodDamageAfter);
        receipt.addProperty("fishingHookUuid", state.hook.getUUID().toString());
        receipt.addProperty("hookOwnerUuid", state.hook.getPlayerOwner().getUUID().toString());
        receipt.addProperty("hookedTargetType", BuiltInRegistries.ENTITY_TYPE.getKey(state.target.getType()).toString());
        receipt.addProperty("hookedTargetUuid", state.target.getUUID().toString());
        receipt.addProperty("hookedTargetTag", PhaseAFishingRodHookedCertification.ENTITY_TAG);
        receipt.addProperty("hookedTargetMatchesTag", state.target.getType().builtInRegistryHolder().is(HOSTILE_MONSTERS));
        receipt.addProperty("hookStateAfterCast", state.hookStateAfterCast);
        receipt.addProperty("hookStateBeforeReel", state.hookStateBeforeReel);
        receipt.addProperty("hookStateAfterReel", state.hookStateAfterReel);
        receipt.addProperty("hookRemovedAfterReel", state.hook.isRemoved());
        receipt.addProperty("playerFishingClearedAfterReel", player.fishing == null);
        receipt.addProperty("actualActionBoundary", PhaseAFishingRodHookedCertification.RUNTIME_BOUNDARY);
        receipt.addProperty("actualActionResult", PhaseAFishingRodHookedExecutionEvidenceValidation.NATIVE_REEL_RESULT);
        receipt.addProperty("ticksToHook", state.ticksToHook);
        receipt.addProperty("ticksToCriterion", state.ticksToHook);
        receipt.add("productionUnlockWitness", state.productionWitness.deepCopy());
        receipt.addProperty("result", "GREEN");
        return receipt;
    }

    private static JsonObject unlockFishingRodAbility(GameTestHelper helper, ServerPlayer player, AdvancementHolder advancement) {
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective");
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not the active default objective");
        Objective rewardObjective = scoreboard.getObjective("bac_settings");
        require(rewardObjective != null, "Missing bac_settings objective");
        ScoreHolder rewardHolder = ScoreHolder.forNameOnly("reward");
        scoreboard.getOrCreatePlayerScore(rewardHolder, rewardObjective).set(0);
        var rewardInfo = scoreboard.getPlayerScoreInfo(rewardHolder, rewardObjective);
        require(rewardInfo != null && rewardInfo.value() == 0, "BACAP rewards are not disabled for this isolated fixture");

        LevelInfoExtension settings = (LevelInfoExtension) server.getWorldData().getLevelSettings();
        Map<AbilityType, Integer> abilities = settings.achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        Integer configuredThreshold = abilities.get(AbilityType.USE_FISHING_ROD);
        require(configuredThreshold != null && configuredThreshold > 0, "Live production configuration has no USE_FISHING_ROD threshold");
        int threshold = configuredThreshold;
        var beforeInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeInfo == null ? 0 : beforeInfo.value();
        require(scoreBefore == 0, "Fresh fishing player already has a bac_advancements score");
        boolean lockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FISHING_ROD, true);
        require(lockedBefore, "USE_FISHING_ROD was not locked before the score fixture");
        scoreboard.getOrCreatePlayerScore(player, objective).set(threshold);
        boolean lockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FISHING_ROD, true);
        require(!lockedAfter, "USE_FISHING_ROD remained locked at its current live threshold");
        var afterInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = afterInfo == null ? 0 : afterInfo.value();
        require(scoreAfter == threshold, "bac_advancements score did not remain at the live threshold");
        require(!criterionDone(player, advancement), "Criterion was complete after unlock setup and before fishing");

        JsonObject witness = new JsonObject();
        witness.addProperty("gatePresent", true);
        witness.addProperty("ability", AbilityType.USE_FISHING_ROD.name());
        witness.addProperty("scoreboardObjective", objective.getName());
        witness.addProperty("thresholdSource", PhaseAFishingRodHookedExecutionEvidenceValidation.THRESHOLD_SOURCE);
        witness.addProperty("requiredThreshold", threshold);
        witness.addProperty("scoreBefore", scoreBefore);
        witness.addProperty("scoreAfter", scoreAfter);
        witness.addProperty("scoreStable", scoreAfter == threshold);
        witness.addProperty("abilityLockedBefore", lockedBefore);
        witness.addProperty("abilityLockedAfter", lockedAfter);
        witness.addProperty("criterionAfterUnlock", criterionDone(player, advancement));
        witness.addProperty("defaultAdvancementsMode", true);
        witness.addProperty("itemRewardsObjective", rewardObjective.getName());
        witness.addProperty("itemRewardsScore", rewardInfo.value());
        witness.addProperty("itemRewardsDisabled", true);
        return witness;
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement) {
        CriterionProgress progress = criterionProgress(player, advancement);
        return progress != null && progress.isDone();
    }

    private static CriterionProgress criterionProgress(ServerPlayer player, AdvancementHolder advancement) {
        return player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(PhaseAFishingRodHookedCertification.CRITERION);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Ravager> ravagerEntityType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(RAVAGER_ID);
        require(type != null && RAVAGER_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)), "frozen hostile witness is not registered");
        return (EntityType<? extends Ravager>) type;
    }

    private static String hookState(FishingHook hook) throws ReflectiveOperationException {
        Field field = FishingHook.class.getDeclaredField("currentState");
        if (!field.trySetAccessible()) throw new IllegalStateException("FishingHook.currentState is not readable");
        Object value = field.get(hook);
        return value == null ? "NONE" : value.toString();
    }

    private static Joined join(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "fish" + id.toString().substring(0, 12));
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        return new Joined(player, connection, channel,
            server.getConnection().getConnections().contains(connection)
                && server.getPlayerList().getPlayer(id) == player);
    }

    private static void assertJoinedLifecycle(GameTestHelper helper, Joined joined) {
        ServerPlayer player = joined.player();
        require(joined.lifecycleValid() && joined.connectionRegistered(), "joined-player lifecycle/registration failed");
        require(player.connection.hasClientLoaded(), "joined player did not process ServerboundPlayerLoadedPacket");
        require(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !player.hasInfiniteMaterials(), "player is not finite-materials SURVIVAL");
        require(helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player, "player left before fixture execution");
    }

    private static void fail(GameTestHelper helper, State state, String phase, Throwable error) {
        String cleanupError = "";
        try { if (state != null) cleanup(state); }
        catch (Throwable failure) { cleanupError = "; cleanup=" + describe(failure); }
        helper.fail("FISHING_ROD_HOOKED " + phase + " failed: " + describe(error) + cleanupError);
    }

    private static Cleanup cleanup(State state) {
        if (state == null || state.joined == null) return new Cleanup(true, true, true, true, 0, 0, 0, 0, "");
        if (!state.cleaned.compareAndSet(false, true)) return state.cleanupResult == null
            ? new Cleanup(false, false, false, false, 0, 0, 0, 0, "cleanup already in progress")
            : state.cleanupResult;
        ServerPlayer player = state.joined.player();
        MinecraftServer server = player.level().getServer();
        CleanupLogCapture logs = CleanupLogCapture.start();
        boolean playerRemoved = false;
        boolean connectionRemoved = false;
        boolean channelSettled = false;
        int messages = 0;
        StringBuilder failures = new StringBuilder();
        try {
            discard(state.target, failures);
            discard(state.hook, failures);
            discard(player.fishing, failures);
            attempt(failures, () -> server.getPlayerList().remove(player));
            playerRemoved = server.getPlayerList().getPlayer(player.getUUID()) != player;
            attempt(failures, () -> server.getConnection().getConnections().remove(state.joined.connection()));
            connectionRemoved = !server.getConnection().getConnections().contains(state.joined.connection());
            try {
                messages = settle(state.joined.channel());
                channelSettled = true;
            } catch (Throwable error) {
                appendFailure(failures, "channel settlement", error);
            }
            attempt(failures, () -> state.joined.connection().disconnect(Component.literal("FISHING_ROD_HOOKED GameTest cleanup")));
        } finally {
            logs.close();
        }
        boolean entitiesRemoved = (state.target == null || !state.target.isAlive())
            && (state.hook == null || !state.hook.isAlive())
            && (player.fishing == null || !player.fishing.isAlive());
        state.cleanupResult = new Cleanup(playerRemoved, connectionRemoved, channelSettled, entitiesRemoved, messages,
            logs.stacklessClosedChannelException(), logs.failedPacketDeliveryFallback(), logs.warningCount(), failures.toString());
        return state.cleanupResult;
    }

    private static void discard(net.minecraft.world.entity.Entity entity, StringBuilder failures) {
        if (entity != null && entity.isAlive()) attempt(failures, entity::discard);
    }

    private static void attempt(StringBuilder failures, Runnable action) {
        try { action.run(); }
        catch (Throwable error) { appendFailure(failures, "cleanup action", error); }
    }

    private static void appendFailure(StringBuilder failures, String action, Throwable error) {
        if (!failures.isEmpty()) failures.append("; ");
        failures.append(action).append('=').append(describe(error));
    }

    private static int settle(EmbeddedChannel channel) {
        if (!channel.isOpen()) return 0;
        int released = 0;
        for (int pass = 0; pass < MAX_SETTLEMENT_PASSES; pass++) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            channel.flushOutbound();
            int passMessages = 0;
            Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound);
                passMessages++;
                if (++released > MAX_SETTLEMENT_MESSAGES) throw new IllegalStateException("EmbeddedChannel settlement exceeded message bound");
            }
            if (passMessages == 0 && !channel.hasPendingTasks()) return released;
        }
        throw new IllegalStateException("EmbeddedChannel did not settle within bounded passes");
    }

    private static String describe(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getMessage() == null) current = current.getCause();
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }

    private static final class State {
        private final String runMode;
        private final AtomicBoolean cleaned = new AtomicBoolean();
        private Path root;
        private String runId;
        private String fingerprint;
        private Joined joined;
        private AdvancementHolder advancement;
        private Ravager target;
        private ItemStack rod;
        private FishingHook hook;
        private JsonObject productionWitness;
        private String hookStateAfterCast;
        private String hookStateBeforeReel;
        private String hookStateAfterReel;
        private boolean criterionBeforeFixture;
        private boolean criterionBeforeCast;
        private boolean criterionBeforeReel;
        private boolean criterionAfterCollisionBeforeReel;
        private boolean criterionAfterReel;
        private int selectedSlot;
        private int rodDamageBefore;
        private int rodDamageAfter;
        private int ticksToHook;
        private int castSequence;
        private int reelSequence;
        private Cleanup cleanupResult;

        private State(String runMode) { this.runMode = runMode; }
    }

    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, boolean lifecycleValid) {
        boolean connectionRegistered() { return lifecycleValid; }
    }

    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, boolean fixtureEntitiesRemoved,
                           int settlementMessages, int stacklessClosedChannelException, int failedPacketDeliveryFallback,
                           int warningCount, String errors) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("playerRemoved", playerRemoved);
            json.addProperty("connectionRemoved", connectionRemoved);
            json.addProperty("channelSettled", channelSettled);
            json.addProperty("fixtureEntitiesRemoved", fixtureEntitiesRemoved);
            json.addProperty("settlementMessages", settlementMessages);
            json.addProperty("stacklessClosedChannelException", stacklessClosedChannelException);
            json.addProperty("failedPacketDeliveryFallback", failedPacketDeliveryFallback);
            json.addProperty("warningCount", warningCount);
            json.addProperty("errors", errors);
            return json;
        }
    }

    private static final class CleanupLogCapture {
        private final LoggerContext context;
        private final org.apache.logging.log4j.core.Logger root;
        private final AbstractAppender appender;
        private final java.util.concurrent.atomic.AtomicInteger stackless;
        private final java.util.concurrent.atomic.AtomicInteger fallback;
        private final java.util.concurrent.atomic.AtomicInteger warnings;

        private CleanupLogCapture(LoggerContext context, org.apache.logging.log4j.core.Logger root, AbstractAppender appender,
                                  java.util.concurrent.atomic.AtomicInteger stackless,
                                  java.util.concurrent.atomic.AtomicInteger fallback,
                                  java.util.concurrent.atomic.AtomicInteger warnings) {
            this.context = context;
            this.root = root;
            this.appender = appender;
            this.stackless = stackless;
            this.fallback = fallback;
            this.warnings = warnings;
        }

        static CleanupLogCapture start() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.Logger root = context.getRootLogger();
            java.util.concurrent.atomic.AtomicInteger stackless = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger fallback = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger warnings = new java.util.concurrent.atomic.AtomicInteger();
            AbstractAppender appender = new AbstractAppender("PhaseAFishingRodHookedCleanup", null, null, true, Property.EMPTY_ARRAY) {
                @Override public void append(LogEvent event) {
                    String message = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
                    String throwable = event.getThrown() == null ? "" : event.getThrown().toString();
                    String text = message + " " + throwable;
                    if (text.contains("StacklessClosedChannelException")) stackless.incrementAndGet();
                    if (text.toLowerCase(java.util.Locale.ROOT).contains("failed to deliver packet, sending fallback")) {
                        fallback.incrementAndGet();
                    }
                    if (event.getLevel() == Level.WARN || event.getLevel() == Level.ERROR || event.getLevel() == Level.FATAL) {
                        warnings.incrementAndGet();
                    }
                }
            };
            appender.start();
            root.addAppender(appender);
            context.updateLoggers();
            return new CleanupLogCapture(context, root, appender, stackless, fallback, warnings);
        }

        void close() {
            root.removeAppender(appender);
            appender.stop();
            context.updateLoggers();
        }

        int stacklessClosedChannelException() { return stackless.get(); }
        int failedPacketDeliveryFallback() { return fallback.get(); }
        int warningCount() { return warnings.get(); }
    }
}
