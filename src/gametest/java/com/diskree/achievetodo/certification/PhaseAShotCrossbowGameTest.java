package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.diskree.achievetodo.ability.AbilityType;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real joined-player packet-path certification for minecraft:shot_crossbow. */
public final class PhaseAShotCrossbowGameTest implements CustomTestMethodInvoker {
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    private static final int CANARY_MAX_TICKS = 100;
    private static final int EXACT_MAX_TICKS = 260;
    private static final int SHOOT_CROSSBOW_UNLOCK_COUNT = 269;
    private static final int INITIAL_SYNC_SETTLE_TICKS = 2;
    private static final BlockPos PLAYER_POS = new BlockPos(1, 3, 1);

    public PhaseAShotCrossbowGameTest() {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void shotCrossbowCanary(GameTestHelper helper) {
        List<CertifiedShotCrossbowCatalog.CaseDefinition> cases = CertifiedShotCrossbowCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("SHOT_CROSSBOW catalog is empty");
            return;
        }
        try {
            String runId = PhaseAShotCrossbowExecutionEvidence.beginRun(
                PhaseAShotCrossbowExecutionEvidence.projectRoot()
            );
            executeCase(helper, cases.getFirst(), runId, completedHelper -> {
                try {
                    PhaseAShotCrossbowExecutionEvidence.RuntimeExecutionArtifact artifact =
                        PhaseAShotCrossbowExecutionEvidence.loadValidatedTemporaryArtifact(
                            PhaseAShotCrossbowExecutionEvidence.projectRoot()
                        );
                    if (artifact.entries().size() != 1) {
                        throw new IllegalStateException("Canary expected exactly one TEMP receipt");
                    }
                    completedHelper.succeed();
                } catch (Throwable t) {
                    completedHelper.fail("SHOT_CROSSBOW canary TEMP validation failed: " + describe(t));
                }
            });
        } catch (Throwable t) {
            helper.fail("SHOT_CROSSBOW canary setup failed: " + describe(t));
        }
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void shotCrossbowExact2(GameTestHelper helper) {
        List<CertifiedShotCrossbowCatalog.CaseDefinition> cases = CertifiedShotCrossbowCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("SHOT_CROSSBOW catalog is empty");
            return;
        }
        try {
            String runId = PhaseAShotCrossbowExecutionEvidence.beginRun(
                PhaseAShotCrossbowExecutionEvidence.projectRoot()
            );
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("SHOT_CROSSBOW exact-family setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedShotCrossbowCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAShotCrossbowExecutionEvidence.RuntimeExecutionArtifact artifact =
                    PhaseAShotCrossbowExecutionEvidence.loadValidatedPromotableTemporaryArtifact(
                        PhaseAShotCrossbowExecutionEvidence.projectRoot()
                    );
                if (artifact.entries().size() != cases.size() || !runId.equals(artifact.runId())) {
                    throw new IllegalStateException("Exact SHOT_CROSSBOW artifact cardinality/runId mismatch");
                }
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("SHOT_CROSSBOW TEMP_PROMOTABLE validation failed: " + describe(t));
            }
            return;
        }
        CertifiedShotCrossbowCatalog.CaseDefinition definition = cases.get(index);
        executeCase(helper, definition, runId, completedHelper -> {
            try {
                if (!runId.equals(PhaseAShotCrossbowExecutionEvidence.currentRunId(
                    PhaseAShotCrossbowExecutionEvidence.projectRoot()
                ))) {
                    throw new IllegalStateException("Exact SHOT_CROSSBOW coordinator runId changed");
                }
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completedHelper.fail("SHOT_CROSSBOW coordinator failed after " + definition.key() + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            establishFiniteSurvival(joinedPlayer.player());
            new ServerboundPlayerLoadedPacket().handle(joinedPlayer.player().connection);
            assertJoinedLifecycle(helper, joinedPlayer, definition);
            JoinedPlayer scheduledPlayer = joinedPlayer;
            // Let AchieveToDoServer finish the joined-player scoreboard sync before applying the
            // normal progression boundary used by the production crossbow lock.
            helper.runAfterDelay(
                INITIAL_SYNC_SETTLE_TICKS,
                () -> startScheduledCase(helper, definition, runId, completion, scheduledPlayer)
            );
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(joinedPlayer);
            }
            helper.fail("SHOT_CROSSBOW case setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void startScheduledCase(
        GameTestHelper helper,
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) {
        try {
            CaseState state = prepareAndStartUse(helper, definition, joinedPlayer);
            helper.runAfterDelay(state.chargeWaitTicks(),
                () -> releaseAndShoot(helper, definition, runId, completion, state));
        } catch (Throwable t) {
            cleanupJoinedServerPlayer(joinedPlayer);
            helper.fail("SHOT_CROSSBOW use setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static CaseState prepareAndStartUse(
        GameTestHelper helper,
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        JoinedPlayer joinedPlayer
    ) {
        if (!Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread())) {
            throw new IllegalStateException("SHOT_CROSSBOW did not execute on the normal server scheduler thread");
        }
        ServerPlayer player = joinedPlayer.player();
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(PLAYER_POS);
        player.teleportTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        player.getInventory().clearContent();

        Holder.Reference<Enchantment> enchantment = enchantmentHolder(helper, definition.enchantment());
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(enchantment, definition.enchantmentLevel());
        int actualLevel = crossbow.getEnchantments().getLevel(enchantment);
        require(actualLevel >= definition.enchantmentLevel(),
            "Configured enchantment level is below the catalog requirement for " + definition.key());
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), crossbow);
        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 1));
        require(countItem(player.getInventory(), Items.ARROW) == 1, "Finite arrow fixture was not installed");
        require(player.getProjectile(crossbow).is(Items.ARROW), "Crossbow could not resolve the finite arrow fixture");
        require(!CrossbowItem.isCharged(crossbow), "Crossbow fixture started charged");

        AdvancementHolder advancement = advancementOrThrow(helper, definition);
        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionBefore = progressBefore.getCriterion(definition.criterion());
        require(criterionBefore != null, "Live AdvancementProgress did not expose " + definition.key());
        require(!criterionBefore.isDone(), "Criterion was complete before the crossbow fixture for " + definition.key());

        JsonObject preconditions = productionPreconditions(helper, player, absolute);
        Set<UUID> projectileUuidsBeforeUse = projectileUuids(level, player);
        int ammoBefore = countItem(player.getInventory(), Items.ARROW);
        int damageBefore = crossbow.getDamageValue();
        String stackSnapshotBefore = crossbow.toString();
        int chargeTicks = Math.max(2, CrossbowItem.getChargeDuration(crossbow, player) + 4);

        new ServerboundUseItemPacket(
            InteractionHand.MAIN_HAND,
            0,
            player.getYRot(),
            player.getXRot()
        ).handle(player.connection);
        require(player.isUsingItem(), "ServerboundUseItemPacket did not start normal crossbow use");
        require(countItem(player.getInventory(), Items.ARROW) == ammoBefore,
            "Crossbow consumed ammo before the normal charge path");
        return new CaseState(
            joinedPlayer,
            player,
            level,
            advancement,
            crossbow,
            enchantment,
            actualLevel,
            preconditions,
            projectileUuidsBeforeUse,
            ammoBefore,
            damageBefore,
            stackSnapshotBefore,
            chargeTicks
        );
    }

    private static void releaseAndShoot(
        GameTestHelper helper,
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        CaseState state
    ) {
        try {
            require(Thread.currentThread().equals(state.level().getServer().getRunningThread()),
                "SHOT_CROSSBOW release did not execute on the normal server scheduler thread");
            ChargedProjectiles loaded = state.crossbow().get(DataComponents.CHARGED_PROJECTILES);
            require(CrossbowItem.isCharged(state.crossbow()) && loaded != null && !loaded.isEmpty(),
                "Normal server ticks did not load the crossbow");
            List<ItemStack> loadedItems = loaded.itemCopies();
            require(loadedItems.size() == definition.expectedLoadedProjectiles(),
                "Unexpected native loaded-projectile count for " + definition.key());
            require(loadedItems.stream().allMatch(item -> item.is(Items.ARROW)),
                "Loaded projectile witness was not an arrow for " + definition.key());
            require(countItem(state.player().getInventory(), Items.ARROW) == 0,
                "Finite survival did not consume the first arrow during loading");

            new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                net.minecraft.core.Direction.DOWN,
                0
            ).handle(state.player().connection);
            require(!state.player().isUsingItem(), "Release-use packet did not stop normal crossbow use");
            require(CrossbowItem.isCharged(state.crossbow()), "Release-use packet did not leave a charged crossbow");

            Set<UUID> projectileUuidsBeforeShoot = projectileUuids(state.level(), state.player());
            require(projectileUuidsBeforeShoot.containsAll(state.projectileUuidsBeforeUse()),
                "Projectile baseline changed unexpectedly before the shot");
            ItemStack firingStack = state.player().getItemInHand(InteractionHand.MAIN_HAND);
            require(firingStack == state.crossbow(),
                "Main-hand crossbow stack changed before the firing packet"
                    + " | sameReference=" + (firingStack == state.crossbow())
                    + " | actual=" + firingStack
                    + " | expected=" + state.crossbow());
            require(!AchieveToDoMod.isAbilityLocked(state.player(), AbilityType.SHOOT_CROSSBOW, true),
                "SHOOT_CROSSBOW became locked before the firing packet");
            require(state.player().connection.hasClientLoaded(),
                "Client-loaded state was lost before the firing packet");
            require(!state.player().getCooldowns().isOnCooldown(firingStack),
                "Crossbow was unexpectedly on cooldown before the firing packet");
            new ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                1,
                state.player().getYRot(),
                state.player().getXRot()
            ).handle(state.player().connection);
            require(!CrossbowItem.isCharged(firingStack),
                "Native shooting did not clear charged projectiles"
                    + " | sameReference=" + (firingStack == state.player().getItemInHand(InteractionHand.MAIN_HAND))
                    + " | actual=" + state.player().getItemInHand(InteractionHand.MAIN_HAND)
                    + " | useItem=" + state.player().getUseItem()
                    + " | using=" + state.player().isUsingItem());

            AdvancementProgress progressAfter = state.player().getAdvancements().getOrStartProgress(state.advancement());
            CriterionProgress criterionAfter = progressAfter.getCriterion(definition.criterion());
            require(criterionAfter != null && criterionAfter.isDone(),
                "Native SHOT_CROSSBOW trigger did not complete " + definition.key());
            List<Projectile> spawned = new ArrayList<>();
            for (Entity entity : state.level().getAllEntities()) {
                if (entity instanceof Projectile projectile
                    && !projectileUuidsBeforeShoot.contains(projectile.getUUID())
                    && state.player().equals(projectile.getOwner())) {
                    spawned.add(projectile);
                }
            }
            spawned.sort(Comparator.comparingInt(Entity::getId));
            require(spawned.size() == definition.expectedFiredProjectiles(),
                "Native projectile entity count mismatch for " + definition.key()
                    + ": expected=" + definition.expectedFiredProjectiles() + ", actual=" + spawned.size());
            int ammoAfter = countItem(state.player().getInventory(), Items.ARROW);
            require(ammoAfter == 0,
                "Native finite-ammo shot changed the arrow count unexpectedly"
                    + " | countBefore=" + state.ammoBefore()
                    + " | countAfter=" + ammoAfter
                    + " | inventory=" + inventorySnapshot(state.player().getInventory())
                    + " | advancements=" + advancementSnapshot(state.player(), state.level().getServer()));

            JsonObject receipt = buildReceipt(definition, state, loadedItems, spawned, criterionAfter.isDone());
            cleanupProjectiles(spawned);
            CleanupResult cleanup = cleanupJoinedServerPlayer(state.joinedPlayer());
            JsonObject cleanupJson = new JsonObject();
            cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved());
            cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved());
            cleanupJson.addProperty("channelSettled", cleanup.channelSettled());
            cleanupJson.addProperty("settlementMessages", cleanup.settlementMessages());
            cleanupJson.addProperty("warningCount", 0);
            receipt.add("cleanup", cleanupJson);
            Path root = PhaseAShotCrossbowExecutionEvidence.projectRoot();
            receipt.addProperty("family", PhaseAShotCrossbowExecutionEvidence.FAMILY);
            receipt.addProperty("source", PhaseAShotCrossbowExecutionEvidence.SOURCE);
            receipt.addProperty("catalogFingerprint", PhaseAShotCrossbowExecutionEvidence.currentCatalogFingerprint(root));
            receipt.addProperty("runId", runId);
            receipt.addProperty("minecraftVersion", PhaseAShotCrossbowExecutionEvidence.MINECRAFT_VERSION);
            receipt.addProperty("compatibilityMarker", PhaseAShotCrossbowExecutionEvidence.COMPATIBILITY_MARKER);
            receipt.addProperty("result", PhaseAShotCrossbowExecutionEvidence.GREEN);
            PhaseAShotCrossbowExecutionEvidence.recordGreen(root, receipt);
            System.out.println("SHOT_CROSSBOW_CASE=" + definition.key()
                + " | mode=" + definition.firingMode()
                + " | loadedProjectiles=" + loadedItems.size()
                + " | firedProjectiles=" + spawned.size()
                + " | criterionBefore=false | criterionAfter=true | cleanupWarnings=0");
            helper.runAfterDelay(1, () -> completion.complete(helper));
        } catch (Throwable t) {
            cleanupJoinedServerPlayer(state.joinedPlayer());
            helper.fail("SHOT_CROSSBOW runtime proof failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static JsonObject buildReceipt(
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        CaseState state,
        List<ItemStack> loadedItems,
        List<Projectile> spawned,
        boolean criterionAfter
    ) {
        ServerPlayer player = state.player();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("selectedItem", definition.selectedItem());
        receipt.addProperty("enchantment", definition.enchantment());
        receipt.addProperty("levelRule", definition.levelRule());
        receipt.addProperty("configuredEnchantmentLevel", definition.enchantmentLevel());
        receipt.addProperty("actualEnchantmentLevel", state.actualEnchantmentLevel());
        receipt.addProperty("itemPredicateSatisfied", predicateSatisfied(definition, state.crossbow(), state.enchantment()));
        receipt.addProperty("boundary", definition.boundary());
        receipt.addProperty("packetPath", definition.packetPath());
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
        receipt.addProperty("abilitiesInstabuild", player.getAbilities().instabuild);
        receipt.addProperty("abilitiesMayBuild", player.getAbilities().mayBuild);
        receipt.addProperty("abilitiesInvulnerable", player.getAbilities().invulnerable);
        receipt.addProperty("abilitiesMayFly", player.getAbilities().mayfly);
        receipt.addProperty("spectator", player.isSpectator());
        receipt.addProperty("joined", player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", player.level().getServer().getConnection().getConnections()
            .contains(state.joinedPlayer().connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", Thread.currentThread().equals(player.level().getServer().getRunningThread()));
        receipt.addProperty("usePacketAccepted", true);
        receipt.addProperty("releasePacketAccepted", true);
        receipt.addProperty("releasePacketAction", "RELEASE_USE_ITEM");
        receipt.addProperty("usingAfterRelease", false);
        receipt.addProperty("chargedBeforeRelease", true);
        receipt.addProperty("chargedBeforeShoot", true);
        receipt.addProperty("chargedAfterShoot", false);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", criterionAfter);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("noDirectCriterionTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("nativeTriggerObserved", criterionAfter);
        receipt.addProperty("ticksToCriterion", 0);
        receipt.add("productionPreconditions", state.productionPreconditions());

        JsonObject loaded = new JsonObject();
        loaded.addProperty("count", loadedItems.size());
        loaded.addProperty("expectedCount", definition.expectedLoadedProjectiles());
        loaded.addProperty("itemType", itemId(loadedItems.getFirst()));
        loaded.addProperty("chargedComponentNonEmpty", true);
        receipt.add("loadedProjectiles", loaded);
        receipt.addProperty("loadedProjectileWitness", loadedItems.size() == definition.expectedLoadedProjectiles()
            && loadedItems.stream().allMatch(item -> item.is(Items.ARROW)));

        int ammoAfter = countItem(player.getInventory(), Items.ARROW);
        JsonObject ammo = new JsonObject();
        ammo.addProperty("item", "minecraft:arrow");
        ammo.addProperty("countBefore", state.ammoBefore());
        ammo.addProperty("countAfter", ammoAfter);
        ammo.addProperty("consumed", state.ammoBefore() - ammoAfter);
        ammo.addProperty("additionalCopiesNotConsumed", Math.max(0, loadedItems.size() - 1));
        ammo.addProperty("finiteSurvivalConsumption", state.ammoBefore() - ammoAfter == definition.expectedAmmoConsumed());
        receipt.add("ammoState", ammo);

        JsonObject crossbow = new JsonObject();
        crossbow.addProperty("itemBefore", "minecraft:crossbow");
        crossbow.addProperty("itemAfter", itemId(state.crossbow()));
        crossbow.addProperty("damageBefore", state.damageBefore());
        crossbow.addProperty("damageAfter", state.crossbow().getDamageValue());
        crossbow.addProperty("durabilityUse", state.crossbow().getDamageValue() - state.damageBefore());
        crossbow.addProperty("chargedBeforeRelease", true);
        crossbow.addProperty("chargedAfterShoot", CrossbowItem.isCharged(state.crossbow()));
        crossbow.addProperty("sameStackReference", player.getItemInHand(InteractionHand.MAIN_HAND) == state.crossbow());
        receipt.add("crossbowState", crossbow);

        JsonObject projectiles = new JsonObject();
        projectiles.addProperty("expectedCount", definition.expectedFiredProjectiles());
        projectiles.addProperty("spawnedCount", spawned.size());
        projectiles.addProperty("firedProjectileCount", spawned.size());
        projectiles.addProperty("allAreArrowProjectiles", spawned.stream().allMatch(p -> "minecraft:arrow".equals(entityId(p))));
        projectiles.addProperty("allHavePlayerOwner", spawned.stream().allMatch(p -> p.getOwner() == player));
        projectiles.addProperty("ownerIdentityMatches", spawned.stream().allMatch(p -> p.getOwner() == player));
        projectiles.addProperty("nativeSpawnObserved", !spawned.isEmpty());
        JsonArray ids = new JsonArray();
        JsonArray uuids = new JsonArray();
        JsonArray types = new JsonArray();
        JsonArray owners = new JsonArray();
        for (Projectile projectile : spawned) {
            ids.add(projectile.getId());
            uuids.add(projectile.getUUID().toString());
            types.add(entityId(projectile));
            owners.add(projectile.getOwner() == null ? "minecraft:none" : projectile.getOwner().getUUID().toString());
        }
        projectiles.add("entityIds", ids);
        projectiles.add("entityUuids", uuids);
        projectiles.add("entityTypes", types);
        projectiles.add("owners", owners);
        receipt.add("projectileObservation", projectiles);

        JsonObject proof = new JsonObject();
        proof.addProperty("usePacket", "ServerboundUseItemPacket");
        proof.addProperty("releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        proof.addProperty("serverHandler", "ServerGamePacketListenerImpl.handleUseItem");
        proof.addProperty("nativeItemPath", "CrossbowItem.use->CrossbowItem.performShooting");
        proof.addProperty("nativeCriterionPath", "CriteriaTriggers.SHOT_CROSSBOW");
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("liveAdvancementProgress", true);
        receipt.add("packetProof", proof);
        return receipt;
    }

    private static JsonObject productionPreconditions(GameTestHelper helper, ServerPlayer player, BlockPos position) {
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
            "BACAP item rewards could not be disabled for the isolated native shot fixture");
        var beforeInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeInfo == null ? 0 : beforeInfo.value();
        require(scoreBefore == 0, "Fresh SHOT_CROSSBOW player score was not zero");
        boolean abilityLockedBefore = AchieveToDoMod.isAbilityLocked(
            player, AbilityType.SHOOT_CROSSBOW, true
        );
        require(abilityLockedBefore, "SHOOT_CROSSBOW was not locked before the native unlock fixture");
        scoreboard.getOrCreatePlayerScore(player, objective).set(SHOOT_CROSSBOW_UNLOCK_COUNT);
        boolean abilityLockedAfter = AchieveToDoMod.isAbilityLocked(
            player, AbilityType.SHOOT_CROSSBOW, true
        );
        require(!abilityLockedAfter, "SHOOT_CROSSBOW remained locked at the accepted normal threshold");
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), position);
        require(!lockedLandmark, "SHOT_CROSSBOW fixture is in a locked landmark");
        JsonObject json = new JsonObject();
        json.addProperty("scoreboardObjective", "bac_advancements");
        json.addProperty("itemRewardsScoreboard", "bac_settings");
        json.addProperty("itemRewardsScore", rewardSettings.value());
        json.addProperty("itemRewardsDisabled", true);
        json.addProperty("ability", "SHOOT_CROSSBOW");
        json.addProperty("abilityUnlockThreshold", SHOOT_CROSSBOW_UNLOCK_COUNT);
        json.addProperty("defaultAdvancementsMode", true);
        json.addProperty("scoreBefore", scoreBefore);
        var afterInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = afterInfo == null ? 0 : afterInfo.value();
        require(scoreAfter == SHOOT_CROSSBOW_UNLOCK_COUNT,
            "Scoreboard unlock did not persist the accepted SHOT_CROSSBOW threshold");
        json.addProperty("scoreAfter", scoreAfter);
        json.addProperty("abilityLockedBefore", abilityLockedBefore);
        json.addProperty("abilityLockedAfter", abilityLockedAfter);
        json.addProperty("lockedLandmark", lockedLandmark);
        return json;
    }

    private static boolean predicateSatisfied(
        CertifiedShotCrossbowCatalog.CaseDefinition definition,
        ItemStack crossbow,
        Holder.Reference<Enchantment> enchantment
    ) {
        if (!crossbow.is(Items.CROSSBOW)) {
            return false;
        }
        int level = crossbow.getEnchantments().getLevel(enchantment);
        return "min".equals(definition.levelRule())
            ? level >= definition.enchantmentLevel()
            : level >= 1;
    }

    private static Holder.Reference<Enchantment> enchantmentHolder(GameTestHelper helper, String id) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(id)));
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedShotCrossbowCatalog.CaseDefinition definition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
            .get(Identifier.parse(definition.advancementId()));
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedShotCrossbowCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Player was not joined");
        require(server.getConnection().getConnections().contains(joinedPlayer.connection()), "Connection was not registered");
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
        String profileName = profileNameForUuid(playerId);
        GameProfile profile = new GameProfile(playerId, profileName);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new JoinedPlayer(player, connection, channel, new AtomicBoolean());
    }

    private static String profileNameForUuid(UUID playerId) {
        String compact = playerId.toString().replace("-", "");
        String result = "shot" + compact.substring(compact.length() - 12);
        require(result.length() <= 16, "Generated profile name exceeds 16 characters");
        return result;
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

    private static void cleanupProjectiles(List<Projectile> projectiles) {
        projectiles.forEach(projectile -> {
            if (!projectile.isRemoved()) {
                projectile.discard();
            }
        });
    }

    private static Set<UUID> projectileUuids(ServerLevel level, ServerPlayer owner) {
        Set<UUID> result = new HashSet<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Projectile projectile && owner.equals(projectile.getOwner())) {
                result.add(projectile.getUUID());
            }
        }
        return result;
    }

    private static int countItem(Inventory inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static String inventorySnapshot(Inventory inventory) {
        List<String> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                slots.add(slot + "=" + itemId(stack) + "x" + stack.getCount());
            }
        }
        return slots.toString();
    }

    private static String advancementSnapshot(ServerPlayer player, MinecraftServer server) {
        List<String> ids = new ArrayList<>();
        for (String id : List.of(
            "minecraft:adventure/shoot_arrow",
            "blazeandcave:weaponry/point_blank",
            "blazeandcave:weaponry/bow_spammer",
            "blazeandcave:weaponry/ol_betsy",
            "blazeandcave:enchanting/machine_bow",
            "blazeandcave:enchanting/shotbow"
        )) {
            AdvancementHolder advancement = server.getAdvancements().get(Identifier.parse(id));
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                ids.add(id);
            }
        }
        return ids.toString();
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
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

    private record JoinedPlayer(
        ServerPlayer player,
        Connection connection,
        EmbeddedChannel channel,
        AtomicBoolean cleaned
    ) {
    }

    private record CaseState(
        JoinedPlayer joinedPlayer,
        ServerPlayer player,
        ServerLevel level,
        AdvancementHolder advancement,
        ItemStack crossbow,
        Holder.Reference<Enchantment> enchantment,
        int actualEnchantmentLevel,
        JsonObject productionPreconditions,
        Set<UUID> projectileUuidsBeforeUse,
        int ammoBefore,
        int damageBefore,
        String stackSnapshotBefore,
        int chargeWaitTicks
    ) {
    }

    private record CleanupResult(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int settlementMessages) {
    }

    @FunctionalInterface
    private interface CaseCompletion {
        void complete(GameTestHelper helper);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, net.minecraft.world.level.block.Blocks.AIR);
        method.invoke(this, helper);
    }
}
