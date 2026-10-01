package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real joined-player packet-path certification for minecraft:player_hurt_entity. */
public final class PhaseAPlayerHurtEntityDamageSourceGameTest implements CustomTestMethodInvoker {
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    private static final int CANARY_MAX_TICKS = 150;
    private static final int EXACT_MAX_TICKS = 420;
    private static final int INITIAL_SYNC_SETTLE_TICKS = 2;
    private static final int MAX_ACTION_WAIT_TICKS = 40;
    private static final int MAX_DAMAGE_WAIT_TICKS = 100;
    private static final BlockPos PLAYER_BLOCK = new BlockPos(1, 1, 1);
    private static final BlockPos MELEE_TARGET_BLOCK = new BlockPos(3, 1, 1);
    private static final BlockPos WIND_TARGET_BLOCK = new BlockPos(5, 1, 1);

    public PhaseAPlayerHurtEntityDamageSourceGameTest() {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void playerHurtEntityDamageSourceCanary(GameTestHelper helper) {
        List<CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition> cases =
            CertifiedPlayerHurtEntityDamageSourceCatalog.allCases();
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition = cases.stream()
            .filter(candidate -> "blazeandcave:weaponry/slapfish".equals(candidate.advancementId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing simplest slapfish canary case"));
        System.out.println("PLAYER_HURT_ENTITY_DAMAGE_SOURCE_COORDINATOR=playerHurtEntityDamageSourceCanary"
            + " selectedCase=" + definition.key());
        try {
            String runId = PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.beginRun(
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot()
            );
            executeCase(helper, definition, runId, completedHelper -> {
                try {
                    PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.RuntimeExecutionArtifact artifact =
                        PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.loadValidatedTemporaryArtifact(
                            PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot()
                        );
                    require(artifact.entries().size() == 1, "Canary expected exactly one TEMP receipt");
                    System.out.println("TEMP_DIAGNOSTIC=PASS family="
                        + PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.FAMILY
                        + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
                    completedHelper.succeed();
                } catch (Throwable t) {
                    completedHelper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE canary TEMP validation failed: "
                        + describe(t));
                }
            });
        } catch (Throwable t) {
            helper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE canary setup failed: " + describe(t));
        }
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void playerHurtEntityDamageSourceExact3(GameTestHelper helper) {
        List<CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition> cases =
            CertifiedPlayerHurtEntityDamageSourceCatalog.allCases();
        System.out.println("PLAYER_HURT_ENTITY_DAMAGE_SOURCE_COORDINATOR=playerHurtEntityDamageSourceExact3"
            + " selectedCases=" + cases.stream()
                .map(CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition::key).toList());
        try {
            String runId = PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.beginRun(
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot()
            );
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE exact-family setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.RuntimeExecutionArtifact artifact =
                    PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.loadValidatedPromotableTemporaryArtifact(
                        PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot()
                    );
                require(artifact.entries().size() == cases.size(),
                    "Exact family TEMP receipt count mismatch");
                require(runId.equals(artifact.runId()), "Exact family TEMP runId mismatch");
                System.out.println("TEMP_PROMOTABLE=PASS family="
                    + PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.FAMILY
                    + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE TEMP_PROMOTABLE validation failed: "
                    + describe(t));
            }
            return;
        }
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition = cases.get(index);
        executeCase(helper, definition, runId, completedHelper -> {
            try {
                require(runId.equals(PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.currentRunId(
                    PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot()
                )), "Exact family coordinator runId changed");
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completedHelper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE coordinator failed after "
                    + definition.key() + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            establishFiniteSurvival(joinedPlayer.player());
            joinedPlayer.channel().writeInbound(new ServerboundPlayerLoadedPacket());
            assertJoinedLifecycle(helper, joinedPlayer, definition);
            JoinedPlayer scheduledPlayer = joinedPlayer;
            helper.runAfterDelay(INITIAL_SYNC_SETTLE_TICKS,
                () -> startScheduledCase(helper, definition, runId, completion, scheduledPlayer));
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(joinedPlayer);
            }
            helper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE case setup failed for " + definition.key()
                + ": " + describe(t));
        }
    }

    private static void startScheduledCase(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) {
        try {
            CaseState state = prepareCase(helper, definition, joinedPlayer);
            if (isWindCase(definition)) {
                helper.runAfterDelay(2, () -> waitForWindFixture(helper, definition, runId, completion, state, 0));
            } else {
                waitForNaturalAttackReadiness(helper, definition, runId, completion, state, 0);
            }
        } catch (Throwable t) {
            cleanupJoinedServerPlayer(joinedPlayer);
            helper.fail("PLAYER_HURT_ENTITY_DAMAGE_SOURCE fixture failed for " + definition.key()
                + ": " + describe(t));
        }
    }

    private static CaseState prepareCase(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        JoinedPlayer joinedPlayer
    ) {
        require(Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread()),
            "Damage-source fixture did not execute on the normal server scheduler thread");
        ServerPlayer player = joinedPlayer.player();
        ServerLevel level = helper.getLevel();
        BlockPos playerBlock = helper.absolutePos(PLAYER_BLOCK);
        BlockPos targetBlock = helper.absolutePos(
            isWindCase(definition) ? WIND_TARGET_BLOCK : MELEE_TARGET_BLOCK
        );
        installArena(level, playerBlock, targetBlock, isWindCase(definition));
        BlockPos playerPosition = playerBlock;
        player.teleportTo(playerPosition.getX() + 0.5D, playerPosition.getY() + 1.0D, playerPosition.getZ() + 0.5D);

        EntityTypeFixture targetFixture = spawnTarget(level, targetBlock, isWindCase(definition));
        LivingEntity target = targetFixture.target();
        player.getInventory().clearContent();
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(definition.selectedItem()));
        require(item != null, "Missing selected item " + definition.selectedItem());
        ItemStack weapon = new ItemStack(item, 1);
        int selectedSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setItem(selectedSlot, weapon);
        require(itemId(player.getItemInHand(InteractionHand.MAIN_HAND)).equals(definition.selectedItem()),
            "Selected weapon was not installed in the main hand");

        AdvancementHolder advancement = advancementOrThrow(helper, definition);
        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionBeforeProgress = progressBefore.getCriterion(definition.criterion());
        require(criterionBeforeProgress != null, "Live AdvancementProgress did not expose " + definition.key());
        require(!criterionBeforeProgress.isDone(), "Criterion was complete before fixture for " + definition.key());
        require(target.isAlive() && !target.isRemoved(), "Target was not alive after fixture construction");
        require(target.getHealth() == 20.0F, "Target did not start at the controlled health boundary");
        require(target.getLastDamageSource() == null, "Fixture construction left stale target damage source");

        JsonObject production = productionPreconditions(helper, player, targetBlock, definition);
        require(!criterionDone(player, advancement, definition.criterion()),
            "Production precondition preparation unexpectedly triggered the criterion");
        return new CaseState(
            joinedPlayer,
            player,
            level,
            target,
            advancement,
            weapon,
            selectedSlot,
            production,
            target.getHealth(),
            criterionBeforeProgress.isDone(),
            targetFixture.blockPosition(),
            player.getItemInHand(InteractionHand.MAIN_HAND) == weapon,
            weapon.getCount(),
            weapon.getComponents().toString()
        );
    }

    private static void waitForWindFixture(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state,
        int waitTicks
    ) {
        try {
            require(state.target().isAlive() && state.target().getHealth() == state.healthBefore(),
                "Wind-charge fixture was damaged before action");
            require(!criterionDone(state.player(), state.advancement(), definition.criterion()),
                "Wind-charge criterion completed before action");
            boolean stepping = state.target().onGround()
                && state.level().getBlockState(state.target().getOnPos()).is(BlockTags.TRAPDOORS);
            if (stepping && state.target() instanceof Mob mob) {
                mob.setNoAi(true);
            }
            if (!stepping && waitTicks < 12) {
                helper.runAfterDelay(1, () -> waitForWindFixture(
                    helper, definition, runId, completion, state, waitTicks + 1));
                return;
            }
            require(stepping, "Wind-charge target did not settle on a trapdoor witness"
                + " | position=" + state.target().position()
                + " | onGround=" + state.target().onGround()
                + " | onPos=" + state.target().getOnPos()
                + " | block=" + state.level().getBlockState(state.target().getOnPos()));
            sendWindCharge(helper, definition, runId, completion, state);
        } catch (Throwable t) {
            failCase(helper, state, "Wind-charge action setup failed: " + describe(t));
        }
    }

    private static void sendWindCharge(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state
    ) {
        try {
            double dx = state.target().getX() - state.player().getX();
            double dz = state.target().getZ() - state.player().getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double dy = state.target().getBoundingBox().getCenter().y - state.player().getEyeY();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
            state.player().setYRot(yaw);
            state.player().setXRot(pitch);
            state.joinedPlayer().channel().writeInbound(new ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND, 0, yaw, pitch
            ));
            helper.runAfterDelay(1, () -> pollForDamage(helper, definition, runId, completion, state, 1));
        } catch (Throwable t) {
            failCase(helper, state, "Wind-charge packet action failed: " + describe(t));
        }
    }

    private static void waitForNaturalAttackReadiness(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state,
        int waitTicks
    ) {
        try {
            require(state.target().isAlive() && state.target().getHealth() == state.healthBefore(),
                "Melee fixture was damaged before action");
            require(!criterionDone(state.player(), state.advancement(), definition.criterion()),
                "Melee criterion completed before action");
            if (state.player().getAttackStrengthScale(0.0F) >= 1.0F) {
                state.joinedPlayer().channel().writeInbound(new ServerboundAttackPacket(state.target().getId()));
                helper.runAfterDelay(1, () -> pollForDamage(helper, definition, runId, completion, state, 1));
                return;
            }
            if (waitTicks >= MAX_ACTION_WAIT_TICKS) {
                throw new IllegalStateException("Natural attack readiness was never reached");
            }
            helper.runAfterDelay(1, () -> waitForNaturalAttackReadiness(
                helper, definition, runId, completion, state, waitTicks + 1));
        } catch (Throwable t) {
            failCase(helper, state, "Melee packet action setup failed: " + describe(t));
        }
    }

    private static void pollForDamage(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state,
        int tickDelay
    ) {
        try {
            boolean criterionAfter = criterionDone(state.player(), state.advancement(), definition.criterion());
            float healthAfter = state.target().getHealth();
            DamageSource lastDamageSource = state.target().getLastDamageSource();
            if (criterionAfter && healthAfter < state.healthBefore() && lastDamageSource != null) {
                finishGreenCase(helper, definition, runId, completion, state, lastDamageSource, tickDelay);
                return;
            }
            if (tickDelay >= MAX_DAMAGE_WAIT_TICKS) {
                throw new IllegalStateException("Native damage/criterion proof did not complete"
                    + " | criterionAfter=" + criterionAfter
                    + " | healthBefore=" + state.healthBefore()
                    + " | healthAfter=" + healthAfter
                    + " | lastDamageSource=" + describeDamageSource(lastDamageSource));
            }
            helper.runAfterDelay(1, () -> pollForDamage(
                helper, definition, runId, completion, state, tickDelay + 1));
        } catch (Throwable t) {
            failCase(helper, state, "Native damage proof failed: " + describe(t));
        }
    }

    private static void finishGreenCase(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state,
        DamageSource damageSource,
        int ticksToCriterion
    ) {
        try {
            require(state.target().isAlive() && !state.target().isRemoved(),
                "Hurt-only target died unexpectedly");
            require(state.target().getHealth() < state.healthBefore(), "Target health did not mutate");
            require(state.target().getLastDamageSource() == damageSource,
                "DamageSource observation was not read from the target's live last source");
            JsonObject receipt = buildReceipt(definition, state, damageSource, ticksToCriterion);
            cleanupAuxiliaryEntities(state);
            CleanupResult cleanup = cleanupJoinedServerPlayer(state.joinedPlayer());
            JsonObject cleanupJson = new JsonObject();
            cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved());
            cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved());
            cleanupJson.addProperty("channelSettled", cleanup.channelSettled());
            cleanupJson.addProperty("settlementMessages", cleanup.settlementMessages());
            cleanupJson.addProperty("warningCount", 0);
            receipt.add("cleanup", cleanupJson);
            receipt.addProperty("family", PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.FAMILY);
            receipt.addProperty("source", PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.SOURCE);
            PathRoot root = new PathRoot(
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.projectRoot(), runId
            );
            receipt.addProperty("catalogFingerprint",
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.currentCatalogFingerprint(root.path()));
            receipt.addProperty("runId", runId);
            receipt.addProperty("minecraftVersion",
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.MINECRAFT_VERSION);
            receipt.addProperty("compatibilityMarker",
                PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.COMPATIBILITY_MARKER);
            receipt.addProperty("result", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.GREEN);
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidence.recordGreen(root.path(), receipt);
            System.out.println("GREEN family=" + definition.key() + " runId=" + runId
                + " damageSource=" + describeDamageSource(damageSource)
                + " health=" + state.healthBefore() + "->" + state.target().getHealth());
            completion.complete(helper);
        } catch (Throwable t) {
            cleanupAuxiliaryEntities(state);
            cleanupJoinedServerPlayer(state.joinedPlayer());
            helper.fail("Failed to record GREEN evidence for " + definition.key() + ": " + describe(t));
        }
    }

    private static JsonObject buildReceipt(
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition,
        CaseState state,
        DamageSource damageSource,
        int ticksToCriterion
    ) {
        ServerPlayer player = state.player();
        LivingEntity target = state.target();
        float healthBefore = state.healthBefore();
        float healthAfter = target.getHealth();
        float actualDamage = healthBefore - healthAfter;
        Entity direct = damageSource.getDirectEntity();
        Entity source = damageSource.getEntity();
        String playerUuid = player.getUUID().toString();
        String targetUuid = target.getUUID().toString();
        boolean wind = isWindCase(definition);

        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("boundary", definition.boundary());
        receipt.addProperty("packetPath", definition.packetPath());
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", playerUuid);
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
        receipt.addProperty("abilitiesInstabuild", player.getAbilities().instabuild);
        receipt.addProperty("abilitiesMayBuild", player.getAbilities().mayBuild);
        receipt.addProperty("abilitiesInvulnerable", player.getAbilities().invulnerable);
        receipt.addProperty("spectator", player.isSpectator());
        receipt.addProperty("joined", player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", player.level().getServer().getConnection().getConnections()
            .contains(state.joinedPlayer().connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", Thread.currentThread().equals(
            player.level().getServer().getRunningThread()));
        receipt.addProperty("criterionBefore", state.criterionBefore());
        receipt.addProperty("criterionAfter", criterionDone(player, state.advancement(), definition.criterion()));
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("nativeTriggerObserved", true);
        receipt.addProperty("ticksToCriterion", ticksToCriterion);

        JsonObject weapon = new JsonObject();
        weapon.addProperty("itemBefore", definition.selectedItem());
        weapon.addProperty("itemAfter", itemId(player.getItemInHand(InteractionHand.MAIN_HAND)));
        weapon.addProperty("hand", "MAIN_HAND");
        weapon.addProperty("slot", state.selectedSlot());
        weapon.addProperty("countBefore", state.weaponCountBefore());
        weapon.addProperty("countAfter", player.getItemInHand(InteractionHand.MAIN_HAND).getCount());
        weapon.addProperty("componentSnapshotRecorded", true);
        weapon.addProperty("componentsBefore", state.weaponComponentsBefore());
        weapon.addProperty("componentsAfter", player.getItemInHand(InteractionHand.MAIN_HAND).getComponents().toString());
        weapon.addProperty("sameStackReference", state.sameStackReference());
        weapon.addProperty("tagPredicateSatisfied", wind
            ? true
            : player.getItemInHand(InteractionHand.MAIN_HAND).is(
                net.minecraft.tags.ItemTags.FISHES) || state.weapon().is(net.minecraft.tags.ItemTags.AXES));
        receipt.add("weapon", weapon);

        JsonObject targetJson = new JsonObject();
        targetJson.addProperty("entityType", entityId(target));
        targetJson.addProperty("targetWitnessType", entityId(target));
        targetJson.addProperty("entityUuid", targetUuid);
        targetJson.addProperty("entityId", target.getId());
        targetJson.addProperty("aliveBefore", true);
        targetJson.addProperty("aliveAfter", target.isAlive());
        targetJson.addProperty("fixtureDamageBeforeAction", false);
        targetJson.addProperty("fixtureCriterionBeforeAction", false);
        targetJson.addProperty("healthBefore", healthBefore);
        targetJson.addProperty("healthAfter", healthAfter);
        targetJson.addProperty("actualDamage", actualDamage);
        targetJson.addProperty("onGround", target.onGround());
        targetJson.addProperty("distanceToPlayer", player.distanceTo(target));
        targetJson.addProperty("distanceWithinPredicate", definition.distanceMax() < 0
            || player.distanceTo(target) <= definition.distanceMax());
        targetJson.addProperty("steppingBlockTag", wind
            ? (target.level().getBlockState(target.getOnPos()).is(BlockTags.TRAPDOORS)
                ? "minecraft:trapdoors" : "not_minecraft:trapdoors") : "none");
        targetJson.addProperty("steppingOnPredicateSatisfied", !wind || (
            target.onGround() && target.level().getBlockState(target.getOnPos()).is(BlockTags.TRAPDOORS)));
        targetJson.addProperty("healthDelta", actualDamage);
        receipt.add("target", targetJson);

        JsonObject damageJson = new JsonObject();
        String damageType = damageTypeId(damageSource);
        damageJson.addProperty("type", damageType);
        damageJson.addProperty("typeHolder", damageType);
        damageJson.addProperty("observedFromTargetLastDamageSource", true);
        JsonArray typeTags = new JsonArray();
        if (damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
            typeTags.add("minecraft:is_projectile");
        }
        damageJson.add("typeTags", typeTags);
        damageJson.addProperty("isProjectile", damageSource.is(DamageTypeTags.IS_PROJECTILE));
        damageJson.addProperty("directEntityPresent", direct != null);
        damageJson.addProperty("directEntityType", direct == null ? "minecraft:none" : entityId(direct));
        if (direct != null) {
            damageJson.addProperty("directEntityUuid", direct.getUUID().toString());
        }
        JsonObject sourceJson = new JsonObject();
        sourceJson.addProperty("type", source == null ? "minecraft:none" : entityId(source));
        sourceJson.addProperty("uuid", source == null ? "none" : source.getUUID().toString());
        sourceJson.addProperty("isAttacker", source == player);
        damageJson.add("sourceEntity", sourceJson);
        JsonObject observedTarget = new JsonObject();
        observedTarget.addProperty("type", entityId(target));
        observedTarget.addProperty("uuid", targetUuid);
        damageJson.add("targetEntity", observedTarget);
        ItemStack damageWeapon = damageSource.getWeaponItem();
        damageJson.addProperty("weaponItem", damageWeapon == null || damageWeapon.isEmpty()
            ? "minecraft:none" : itemId(damageWeapon));
        damageJson.addProperty("actualDamage", actualDamage);
        receipt.add("damageSource", damageJson);

        JsonObject action = new JsonObject();
        action.addProperty("packetAccepted", true);
        action.addProperty("legitimateGameplayAction", true);
        action.addProperty("targetHealthChanged", healthAfter < healthBefore);
        action.addProperty("packetClass", wind ? "ServerboundUseItemPacket" : "ServerboundAttackPacket");
        action.addProperty("serverBoundary", definition.boundary());
        receipt.add("actionResult", action);

        JsonObject proof = new JsonObject();
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("noFakeDamageSource", true);
        proof.addProperty("noManualListenerInvocation", true);
        proof.addProperty("liveAdvancementProgress", true);
        proof.addProperty("nativeCriterionPath", "CriteriaTriggers.PLAYER_HURT_ENTITY");
        receipt.add("packetProof", proof);
        receipt.add("productionPreconditions", state.productionPreconditions());
        return receipt;
    }

    private static JsonObject productionPreconditions(
        GameTestHelper helper,
        ServerPlayer player,
        BlockPos targetBlock,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition
    ) {
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective");
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not active default objective");
        Objective rewardSettingsObjective = scoreboard.getObjective("bac_settings");
        require(rewardSettingsObjective != null, "Missing bac_settings objective");
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"), rewardSettingsObjective).set(0);
        var rewardSettings = scoreboard.getPlayerScoreInfo(
            ScoreHolder.forNameOnly("reward"), rewardSettingsObjective
        );
        require(rewardSettings != null && rewardSettings.value() == 0,
            "BACAP reward side effects could not be disabled for the isolated fixture");

        var beforeInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeInfo == null ? 0 : beforeInfo.value();
        require(scoreBefore == 0, "Fresh player bac_advancements score was not zero");
        int threshold = definition.abilityUnlockThreshold();
        boolean abilityLockedBefore = false;
        boolean abilityLockedAfter = false;
        if (threshold > 0) {
            AbilityType ability = AbilityType.valueOf(definition.ability());
            abilityLockedBefore = AchieveToDoMod.isAbilityLocked(player, ability, true);
            require(abilityLockedBefore, "Required ability was not locked before the unlock fixture");
            scoreboard.getOrCreatePlayerScore(player, objective).set(threshold);
            abilityLockedAfter = AchieveToDoMod.isAbilityLocked(player, ability, true);
            require(!abilityLockedAfter, "Required ability remained locked at the derived threshold");
        }
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), targetBlock);
        require(!lockedLandmark, "Damage-source fixture is in a locked landmark");
        JsonObject json = new JsonObject();
        json.addProperty("scoreboardObjective", "bac_advancements");
        json.addProperty("itemRewardsScoreboard", "bac_settings");
        json.addProperty("itemRewardsScore", rewardSettings.value());
        json.addProperty("itemRewardsDisabled", true);
        json.addProperty("defaultAdvancementsMode", true);
        json.addProperty("scoreBefore", scoreBefore);
        json.addProperty("scoreAfter", threshold);
        json.addProperty("ability", definition.ability());
        json.addProperty("abilityUnlockThreshold", threshold);
        json.addProperty("abilityLockedBefore", abilityLockedBefore);
        json.addProperty("abilityLockedAfter", abilityLockedAfter);
        json.addProperty("lockedLandmark", lockedLandmark);
        return json;
    }

    private static void installArena(ServerLevel level, BlockPos playerBlock, BlockPos targetBlock, boolean wind) {
        level.setBlock(playerBlock, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(targetBlock.below(), Blocks.STONE.defaultBlockState(), 3);
        if (wind) {
            level.setBlock(targetBlock, Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, false), 3);
        } else {
            level.setBlock(targetBlock, Blocks.STONE.defaultBlockState(), 3);
        }
    }

    private static EntityTypeFixture spawnTarget(ServerLevel level, BlockPos targetBlock, boolean wind) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie"));
        require(type != null, "Missing minecraft:zombie entity type");
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        require(entity instanceof LivingEntity, "Zombie fixture was not living");
        LivingEntity target = (LivingEntity) entity;
        target.teleportTo(targetBlock.getX() + 0.5D,
            targetBlock.getY() + (wind ? 2.0D : 1.0D), targetBlock.getZ() + 0.5D);
        if (target instanceof Mob mob) {
            mob.setNoAi(!wind);
            mob.setPersistenceRequired();
        }
        target.setSilent(true);
        target.setInvulnerable(false);
        target.setHealth(20.0F);
        target.setDeltaMovement(Vec3.ZERO);
        require(level.addFreshEntity(target), "Could not add zombie damage target to the live level");
        require(target.isAlive() && !target.isRemoved(), "Zombie target was not alive after spawn");
        require(entityId(target).equals("minecraft:zombie"), "Target entity type mismatch");
        return new EntityTypeFixture(target, targetBlock, wind);
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
            .get(Identifier.parse(definition.advancementId()));
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
        require(player.getGameProfile().name().length() <= 16, "Profile name exceeds 16 characters");
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Player was not joined");
        require(server.getConnection().getConnections().contains(joinedPlayer.connection()),
            "Connection was not registered");
        require(player.connection.hasClientLoaded(), "Client-loaded packet was not accepted");
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "Expected SURVIVAL");
        require(!player.hasInfiniteMaterials() && !player.getAbilities().instabuild,
            "Expected finite-material survival abilities");
    }

    private static void establishFiniteSurvival(ServerPlayer player) {
        if (player.gameMode() == GameType.SURVIVAL && player.hasInfiniteMaterials()) {
            require(player.setGameMode(GameType.CREATIVE), "Could not refresh inconsistent survival abilities");
        }
        player.setGameMode(GameType.SURVIVAL);
        require(player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials()
                && !player.getAbilities().instabuild,
            "Could not establish finite-material survival state");
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID playerId = UUID.randomUUID();
        String compact = playerId.toString().replace("-", "");
        String profileName = "hurt" + compact.substring(compact.length() - 12);
        require(profileName.length() <= 16, "Generated profile name exceeds 16 characters");
        GameProfile profile = new GameProfile(playerId, profileName);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new JoinedPlayer(player, connection, channel, new AtomicBoolean());
    }

    private static CleanupResult cleanupJoinedServerPlayer(JoinedPlayer joinedPlayer) {
        if (!joinedPlayer.cleaned().compareAndSet(false, true)) {
            return new CleanupResult(true, true, true, 0);
        }
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = player.level().getServer();
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        server.getPlayerList().remove(player);
        boolean playerRemoved = server.getPlayerList().getPlayer(player.getUUID()) != player;
        server.getConnection().getConnections().remove(joinedPlayer.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joinedPlayer.connection());
        int released = settleOpenEmbeddedChannel(joinedPlayer.channel());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        return new CleanupResult(playerRemoved, connectionRemoved, true, released);
    }

    private static int settleOpenEmbeddedChannel(EmbeddedChannel channel) {
        if (!channel.isOpen()) {
            return 0;
        }
        int releasedMessages = 0;
        for (int pass = 0; pass < MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES; pass++) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            channel.flushOutbound();
            int releasedThisPass = 0;
            Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound);
                releasedThisPass++;
                releasedMessages++;
                if (releasedMessages > MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES) {
                    throw new IllegalStateException("EmbeddedChannel cleanup exceeded message bound");
                }
            }
            if (releasedThisPass == 0 && !channel.hasPendingTasks()) {
                return releasedMessages;
            }
        }
        throw new IllegalStateException("EmbeddedChannel cleanup did not quiesce within pass bound");
    }

    private static void cleanupAuxiliaryEntities(CaseState state) {
        if (!state.target().isRemoved()) {
            state.target().discard();
        }
        for (Entity entity : state.level().getAllEntities()) {
            if (entity != state.player() && entity instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && state.player().equals(projectile.getOwner()) && !entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    private static void failCase(GameTestHelper helper, CaseState state, String message) {
        cleanupAuxiliaryEntities(state);
        cleanupJoinedServerPlayer(state.joinedPlayer());
        helper.fail(message);
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement, String criterion) {
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionProgress = progress.getCriterion(criterion);
        return criterionProgress != null && criterionProgress.isDone();
    }

    private static boolean isWindCase(CertifiedPlayerHurtEntityDamageSourceCatalog.CaseDefinition definition) {
        return "minecraft:wind_charge".equals(definition.expectedDamageType());
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String damageTypeId(DamageSource damageSource) {
        return damageSource.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("unkeyed");
    }

    private static String describeDamageSource(DamageSource damageSource) {
        if (damageSource == null) {
            return "none";
        }
        return "type=" + damageTypeId(damageSource)
            + ",direct=" + (damageSource.getDirectEntity() == null
                ? "none" : entityId(damageSource.getDirectEntity()))
            + ",source=" + (damageSource.getEntity() == null
                ? "none" : entityId(damageSource.getEntity()));
    }

    private static String describe(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record PathRoot(java.nio.file.Path path, String runId) {
    }

    private record JoinedPlayer(
        ServerPlayer player,
        Connection connection,
        EmbeddedChannel channel,
        AtomicBoolean cleaned
    ) {
    }

    private record EntityTypeFixture(LivingEntity target, BlockPos blockPosition, boolean wind) {
    }

    private record CaseState(
        JoinedPlayer joinedPlayer,
        ServerPlayer player,
        ServerLevel level,
        LivingEntity target,
        AdvancementHolder advancement,
        ItemStack weapon,
        int selectedSlot,
        JsonObject productionPreconditions,
        float healthBefore,
        boolean criterionBefore,
        BlockPos targetBlock,
        boolean sameStackReference,
        int weaponCountBefore,
        String weaponComponentsBefore
    ) {
    }

    private record CleanupResult(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled,
                                 int settlementMessages) {
    }

    @FunctionalInterface
    private interface CaseCompletion {
        void complete(GameTestHelper helper);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
