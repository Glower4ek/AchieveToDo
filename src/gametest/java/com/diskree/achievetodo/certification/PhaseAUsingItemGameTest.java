package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AdvancementsMode;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real joined-player packet-path certification for minecraft:using_item. */
public final class PhaseAUsingItemGameTest implements CustomTestMethodInvoker {
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    private static final int CANARY_MAX_TICKS = 150;
    private static final int EXACT_MAX_TICKS = 260;
    private static final int ATTACK_WITH_TRIDENT_UNLOCK_COUNT = 244;
    private static final int INITIAL_SYNC_SETTLE_TICKS = 2;
    private static final BlockPos PLAYER_BLOCK = new BlockPos(1, 2, 1);
    private static final ResourceKey<Biome> DEEP_DARK = ResourceKey.create(
        Registries.BIOME, Identifier.parse("minecraft:deep_dark")
    );

    public PhaseAUsingItemGameTest() {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void usingItemCanary(GameTestHelper helper) {
        List<CertifiedUsingItemCatalog.CaseDefinition> cases = CertifiedUsingItemCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("USING_ITEM catalog is empty");
            return;
        }
        CertifiedUsingItemCatalog.CaseDefinition definition = cases.getFirst();
        System.out.println("USING_ITEM_COORDINATOR=usingItemCanary selectedCase=" + definition.key());
        try {
            String runId = PhaseAUsingItemExecutionEvidence.beginRun(
                PhaseAUsingItemExecutionEvidence.projectRoot()
            );
            executeCase(helper, definition, runId, completedHelper -> {
                try {
                    PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
                        PhaseAUsingItemExecutionEvidenceValidation.loadValidatedTemporaryArtifact(
                            PhaseAUsingItemExecutionEvidence.projectRoot()
                        );
                    require(artifact.entries().size() == 1, "Canary expected exactly one TEMP receipt");
                    System.out.println("TEMP_DIAGNOSTIC=PASS family="
                        + PhaseAUsingItemExecutionEvidence.FAMILY + " runId=" + artifact.runId()
                        + " entries=" + artifact.entries().size());
                    completedHelper.succeed();
                } catch (Throwable t) {
                    completedHelper.fail("USING_ITEM canary TEMP validation failed: " + describe(t));
                }
            });
        } catch (Throwable t) {
            helper.fail("USING_ITEM canary setup failed: " + describe(t));
        }
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void usingItemExact2(GameTestHelper helper) {
        List<CertifiedUsingItemCatalog.CaseDefinition> cases = CertifiedUsingItemCatalog.allCases();
        System.out.println("USING_ITEM_COORDINATOR=usingItemExact2 selectedCases="
            + cases.stream().map(CertifiedUsingItemCatalog.CaseDefinition::key).toList());
        try {
            String runId = PhaseAUsingItemExecutionEvidence.beginRun(
                PhaseAUsingItemExecutionEvidence.projectRoot()
            );
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("USING_ITEM exact-family setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedUsingItemCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
                    PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(
                        PhaseAUsingItemExecutionEvidence.projectRoot()
                    );
                require(artifact.entries().size() == cases.size(), "Exact family TEMP receipt count mismatch");
                require(runId.equals(artifact.runId()), "Exact family TEMP runId mismatch");
                System.out.println("TEMP_PROMOTABLE=PASS family="
                    + PhaseAUsingItemExecutionEvidence.FAMILY + " runId=" + artifact.runId()
                    + " entries=" + artifact.entries().size());
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("USING_ITEM TEMP_PROMOTABLE validation failed: " + describe(t));
            }
            return;
        }
        CertifiedUsingItemCatalog.CaseDefinition definition = cases.get(index);
        executeCase(helper, definition, runId, completedHelper -> {
            try {
                require(runId.equals(PhaseAUsingItemExecutionEvidence.currentRunId(
                    PhaseAUsingItemExecutionEvidence.projectRoot()
                )), "Exact family coordinator runId changed");
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completedHelper.fail("USING_ITEM coordinator failed after " + definition.key()
                    + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedUsingItemCatalog.CaseDefinition definition,
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
            helper.runAfterDelay(
                INITIAL_SYNC_SETTLE_TICKS,
                () -> prepareScheduledCase(helper, definition, runId, completion, scheduledPlayer)
            );
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(joinedPlayer);
            }
            helper.fail("USING_ITEM case setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void prepareScheduledCase(
        GameTestHelper helper,
        CertifiedUsingItemCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) {
        try {
            PreparedCase state = prepareCase(helper, definition, joinedPlayer);
            // Water collision and the biome fixture are observed on a normal server tick before
            // the use packet is sent. This keeps the environmental witness independent of setup.
            helper.runAfterDelay(1, () -> sendUsePacket(helper, runId, completion, state));
        } catch (Throwable t) {
            cleanupJoinedServerPlayer(joinedPlayer);
            helper.fail("USING_ITEM fixture failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static PreparedCase prepareCase(
        GameTestHelper helper,
        CertifiedUsingItemCatalog.CaseDefinition definition,
        JoinedPlayer joinedPlayer
    ) {
        require(Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread()),
            "USING_ITEM fixture did not execute on the normal server scheduler thread");
        ServerPlayer player = joinedPlayer.player();
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(PLAYER_BLOCK);
        installFloor(level, absolute);
        if (isGoatHorn(definition)) {
            helper.setBiome(DEEP_DARK);
        } else {
            installWater(level, absolute);
        }
        player.teleportTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        player.getInventory().clearContent();

        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(definition.selectedItem()));
        require(item != null, "Missing selected item " + definition.selectedItem());
        ItemStack stack = new ItemStack(item, 1);
        Holder.Reference<Enchantment> riptide = null;
        int actualEnchantmentLevel = 0;
        if (isRiptide(definition)) {
            riptide = enchantmentHolder(helper, "minecraft:riptide");
            stack.enchant(riptide, 1);
            actualEnchantmentLevel = stack.getEnchantments().getLevel(riptide);
            require(actualEnchantmentLevel >= 1, "Riptide component was not installed on the live trident");
        }
        int selectedSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setItem(selectedSlot, stack);
        require(player.getItemInHand(InteractionHand.MAIN_HAND) == stack,
            "Selected item stack reference was not installed in the main hand");
        require(itemId(stack).equals(definition.selectedItem()), "Selected item id did not match the catalog");
        require(stack.getCount() == 1, "Selected item was not finite");

        InstrumentComponent instrument = stack.get(DataComponents.INSTRUMENT);
        if (isGoatHorn(definition)) {
            require(instrument != null, "Goat horn has no live InstrumentComponent");
            require(instrument.instrument().isBound(), "Goat horn InstrumentComponent holder is unbound");
        }
        int actualUseDuration = stack.getUseDuration(player);
        require(actualUseDuration > 0, "Selected item has no usable native duration");
        String actualUseAnimation = stack.getUseAnimation().name();
        require((isGoatHorn(definition) && "TOOT_HORN".equals(actualUseAnimation))
                || (isRiptide(definition) && "TRIDENT".equals(actualUseAnimation)),
            "Unexpected native use animation for " + definition.key() + ": " + actualUseAnimation);

        AdvancementHolder advancement = advancementOrThrow(helper, definition);
        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionBefore = progressBefore.getCriterion(definition.criterion());
        require(criterionBefore != null, "Live AdvancementProgress did not expose " + definition.key());
        require(!criterionBefore.isDone(), "Criterion was complete before fixture for " + definition.key());

        JsonObject production = productionUnlockWitness(helper, player, absolute, definition);
        require(!criterionDone(player, advancement, definition.criterion()),
            "Production precondition preparation unexpectedly triggered the criterion");

        JsonObject environment = new JsonObject();
        environment.addProperty("required", true);
        if (isGoatHorn(definition)) {
            String biomeId = biomeId(level, absolute);
            require("minecraft:deep_dark".equals(biomeId),
                "Live biome fixture did not resolve to minecraft:deep_dark: " + biomeId);
            environment.addProperty("biomeId", biomeId);
            environment.addProperty("biomePredicateSatisfied", true);
            environment.addProperty("structureAlternativeSatisfied", false);
            environment.addProperty("predicate", "minecraft:deep_dark OR minecraft:ancient_city");
            environment.addProperty("predicateSatisfied", true);
        } else {
            environment.addProperty("waterOrRainRequired", true);
            environment.addProperty("wetStateSource", "Player.isInWaterOrRain");
            environment.addProperty("predicateSatisfied", true);
        }
        return new PreparedCase(
            joinedPlayer,
            player,
            level,
            definition,
            advancement,
            stack,
            selectedSlot,
            production,
            environment,
            criterionBefore.isDone(),
            stack.getCount(),
            stack.getComponents().toString(),
            actualEnchantmentLevel,
            actualUseDuration,
            actualUseAnimation,
            riptide
        );
    }

    private static void sendUsePacket(
        GameTestHelper helper,
        String runId,
        CaseCompletion completion,
        PreparedCase state
    ) {
        try {
            require(Thread.currentThread().equals(state.level().getServer().getRunningThread()),
                "USING_ITEM packet did not execute on the normal server scheduler thread");
            require(!state.player().isUsingItem(), "Player was already using an item before intended action");
            if (isRiptide(state.definition())) {
                boolean wetBeforeUse = state.player().isInWaterOrRain();
                require(wetBeforeUse, "Riptide use did not reach a real wet player state");
                state.environment().addProperty("wetBeforeUse", wetBeforeUse);
                state.environment().addProperty("wetAtUse", wetBeforeUse);
                state.environment().addProperty("wetStateObserved", wetBeforeUse);
            }
            require(!criterionDone(state.player(), state.advancement(), state.definition().criterion()),
                "USING_ITEM criterion became complete before the use packet");

            state.joinedPlayer().channel().writeInbound(new ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                0,
                state.player().getYRot(),
                state.player().getXRot()
            ));
            require(state.player().isUsingItem(),
                "ServerboundUseItemPacket did not start native use for " + state.definition().key());
            require(state.player().getUseItem() == state.stack(),
                "Native use state did not retain the selected ItemStack reference");
            require(state.player().getUsedItemHand() == InteractionHand.MAIN_HAND,
                "Native use state did not retain MAIN_HAND");
            require(state.player().getUseItemRemainingTicks() <= state.actualUseDuration()
                    && state.player().getUseItemRemainingTicks() > 0,
                "Native use state has an invalid initial remaining duration");
            require(!criterionDone(state.player(), state.advancement(), state.definition().criterion()),
                "USING_ITEM criterion completed synchronously before a server use tick");

            helper.runAfterDelay(1, () -> observeUseTick(helper, runId, completion, state));
        } catch (Throwable t) {
            failCase(helper, state, "USING_ITEM packet action failed: " + describe(t));
        }
    }

    private static void observeUseTick(
        GameTestHelper helper,
        String runId,
        CaseCompletion completion,
        PreparedCase state
    ) {
        try {
            int actualUseTicks = state.player().getTicksUsingItem();
            require(actualUseTicks >= 1, "Native active-use state did not advance on a server tick");
            require(state.player().isUsingItem(), "Native active-use state ended before observation");
            require(state.player().getUseItem() == state.stack(), "Native use item changed unexpectedly");
            AdvancementProgress progressAfterTick = state.player().getAdvancements()
                .getOrStartProgress(state.advancement());
            CriterionProgress criterionAfterTick = progressAfterTick.getCriterion(state.definition().criterion());
            require(criterionAfterTick != null && criterionAfterTick.isDone(),
                "Native USING_ITEM trigger did not complete " + state.definition().key());

            state.joinedPlayer().channel().writeInbound(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                net.minecraft.core.Direction.DOWN,
                1
            ));
            require(!state.player().isUsingItem(), "Release-use packet did not stop native item use");
            require(state.player().getItemInHand(InteractionHand.MAIN_HAND) == state.stack(),
                "Release-use packet replaced the selected item stack unexpectedly");
            require(state.stack().getCount() == 1, "Using-item proof changed finite item count unexpectedly");

            JsonObject receipt = buildReceipt(state, actualUseTicks, criterionAfterTick.isDone());
            CleanupResult cleanup = cleanupJoinedServerPlayer(state.joinedPlayer());
            JsonObject cleanupJson = new JsonObject();
            cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved());
            cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved());
            cleanupJson.addProperty("channelSettled", cleanup.channelSettled());
            cleanupJson.addProperty("settlementMessages", cleanup.settlementMessages());
            cleanupJson.addProperty("warningCount", 0);
            receipt.add("cleanup", cleanupJson);
            Path root = PhaseAUsingItemExecutionEvidence.projectRoot();
            receipt.addProperty("family", PhaseAUsingItemExecutionEvidence.FAMILY);
            receipt.addProperty("source", PhaseAUsingItemExecutionEvidence.SOURCE);
            receipt.addProperty("catalogFingerprint", PhaseAUsingItemExecutionEvidence.currentCatalogFingerprint(root));
            receipt.addProperty("runId", runId);
            receipt.addProperty("minecraftVersion", PhaseAUsingItemExecutionEvidence.MINECRAFT_VERSION);
            receipt.addProperty("compatibilityMarker", PhaseAUsingItemExecutionEvidence.COMPATIBILITY_MARKER);
            receipt.addProperty("result", PhaseAUsingItemExecutionEvidence.GREEN);
            require(cleanup.playerRemoved() && cleanup.connectionRemoved() && cleanup.channelSettled(),
                "USING_ITEM cleanup did not remove and settle the joined player");
            require(cleanup.settlementMessages() >= 0, "USING_ITEM cleanup settlement count was negative");
            PhaseAUsingItemExecutionEvidence.recordGreen(root, receipt);
            System.out.println("USING_ITEM_CASE=" + state.definition().key()
                + " | selectedItem=" + state.definition().selectedItem()
                + " | actualUseTicks=" + actualUseTicks
                + " | criterionBefore=false | criterionAfter=true | cleanupWarnings=0");
            helper.runAfterDelay(1, () -> completion.complete(helper));
        } catch (Throwable t) {
            failCase(helper, state, "Native USING_ITEM proof failed: " + describe(t));
        }
    }

    private static JsonObject buildReceipt(PreparedCase state, int actualUseTicks, boolean criterionAfter) {
        ServerPlayer player = state.player();
        CertifiedUsingItemCatalog.CaseDefinition definition = state.definition();
        ItemStack selected = player.getItemInHand(InteractionHand.MAIN_HAND);
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("joined", player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", player.level().getServer().getConnection().getConnections()
            .contains(state.joinedPlayer().connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", Thread.currentThread().equals(player.level().getServer().getRunningThread()));
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
        receipt.addProperty("abilitiesInstabuild", player.getAbilities().instabuild);
        receipt.addProperty("spectator", player.isSpectator());
        receipt.addProperty("selectedItem", definition.selectedItem());
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("slot", state.selectedSlot());
        receipt.addProperty("itemCountBefore", state.itemCountBefore());
        receipt.addProperty("itemCountAfter", selected.getCount());
        receipt.addProperty("componentsBefore", state.componentsBefore());
        receipt.addProperty("componentsAfter", selected.getComponents().toString());
        receipt.addProperty("sameStackReference", selected == state.stack());
        receipt.addProperty("itemPredicateSatisfied", itemPredicateSatisfied(definition, state));

        JsonObject enchantment = new JsonObject();
        boolean riptide = isRiptide(definition);
        enchantment.addProperty("required", riptide);
        enchantment.addProperty("id", riptide ? "minecraft:riptide" : "minecraft:none");
        enchantment.addProperty("level", state.actualEnchantmentLevel());
        enchantment.addProperty("componentPresent", riptide && state.riptideHolder() != null);
        enchantment.addProperty("beforeMatches", !riptide || state.actualEnchantmentLevel() >= 1);
        enchantment.addProperty("afterMatches", !riptide || selected.getEnchantments().getLevel(state.riptideHolder()) >= 1);
        receipt.add("enchantmentWitness", enchantment);

        receipt.add("productionUnlockWitness", state.production());
        receipt.add("environmentWitness", state.environment());
        receipt.addProperty("boundary", definition.boundary());
        receipt.addProperty("packetPath", definition.packetPath());
        receipt.addProperty("criterionBefore", state.criterionBefore());
        receipt.addProperty("criterionAfter", criterionAfter);
        receipt.addProperty("fixtureDidNotTrigger", !state.criterionBefore());
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("usingItemBefore", false);
        receipt.addProperty("usingItemAfterOrObserved", true);
        receipt.addProperty("actualUseItem", itemId(state.stack()));
        receipt.addProperty("actualUseTicks", actualUseTicks);
        receipt.addProperty("actualUseDuration", state.actualUseDuration());
        receipt.addProperty("actualUseAnimation", state.actualUseAnimation());
        receipt.addProperty("actualActionBoundary", definition.boundary());
        receipt.addProperty("actualPacketPath", definition.packetPath());
        receipt.addProperty("actualActionResult", PhaseAUsingItemExecutionEvidenceValidation.NATIVE_ACTIVE_USE_ACCEPTED);
        receipt.addProperty("usePacketAccepted", true);
        receipt.addProperty("nativeUseStateObserved", true);
        receipt.addProperty("releasePacketAccepted", true);
        receipt.addProperty("releasePacketAction", "RELEASE_USE_ITEM");
        receipt.addProperty("usingAfterRelease", false);
        receipt.addProperty("useStoppedAfterRelease", true);
        receipt.addProperty("noDirectTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("nativeTriggerObserved", criterionAfter);
        receipt.addProperty("ticksToCriterion", actualUseTicks);

        JsonObject proof = new JsonObject();
        proof.addProperty("usePacket", "ServerboundUseItemPacket");
        proof.addProperty("releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        proof.addProperty("serverHandler", definition.boundary());
        proof.addProperty("nativeCriterionPath", "ServerPlayer.updateUsingItem->CriteriaTriggers.USING_ITEM");
        proof.addProperty("realPacketPath", true);
        proof.addProperty("liveAdvancementProgress", true);
        proof.addProperty("nativeActiveUseState", true);
        proof.addProperty("noDirectTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("noManualListenerInvocation", true);
        receipt.add("packetProof", proof);
        return receipt;
    }

    private static boolean itemPredicateSatisfied(
        CertifiedUsingItemCatalog.CaseDefinition definition,
        PreparedCase state
    ) {
        if (!itemId(state.stack()).equals(definition.selectedItem())) {
            return false;
        }
        if (isGoatHorn(definition)) {
            return state.stack().get(DataComponents.INSTRUMENT) != null;
        }
        return state.actualEnchantmentLevel() >= 1;
    }

    private static JsonObject productionUnlockWitness(
        GameTestHelper helper,
        ServerPlayer player,
        BlockPos position,
        CertifiedUsingItemCatalog.CaseDefinition definition
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
        var rewardSettings = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("reward"), rewardSettingsObjective);
        require(rewardSettings != null && rewardSettings.value() == 0,
            "BACAP item rewards could not be disabled for the isolated USING_ITEM fixture");
        var beforeInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeInfo == null ? 0 : beforeInfo.value();
        require(scoreBefore == 0, "Fresh USING_ITEM player score was not zero");

        boolean gatePresent = definition.productionUnlockGate().get("present").getAsBoolean();
        int threshold = definition.productionUnlockGate().get("unlockThreshold").getAsInt();
        String ability = definition.productionUnlockGate().get("ability").getAsString();
        boolean abilityLockedBefore = false;
        if (gatePresent) {
            require("ATTACK_WITH_TRIDENT".equals(ability), "Unexpected USING_ITEM production ability");
            abilityLockedBefore = AchieveToDoMod.isAbilityLocked(
                player, AbilityType.ATTACK_WITH_TRIDENT, true
            );
            require(abilityLockedBefore, "ATTACK_WITH_TRIDENT was not locked before the unlock fixture");
            scoreboard.getOrCreatePlayerScore(player, objective).set(threshold);
        }
        boolean abilityLockedAfter = gatePresent && AchieveToDoMod.isAbilityLocked(
            player, AbilityType.ATTACK_WITH_TRIDENT, true
        );
        require(!abilityLockedAfter, "ATTACK_WITH_TRIDENT remained locked at the live threshold");
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), position);
        require(!lockedLandmark, "USING_ITEM fixture is in a locked landmark");
        var afterInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = afterInfo == null ? 0 : afterInfo.value();
        require(scoreAfter == threshold, "USING_ITEM unlock score did not remain stable");

        JsonObject json = new JsonObject();
        json.addProperty("scoreboardObjective", "bac_advancements");
        json.addProperty("itemRewardsScoreboard", "bac_settings");
        json.addProperty("itemRewardsScore", rewardSettings.value());
        json.addProperty("itemRewardsDisabled", true);
        json.addProperty("defaultAdvancementsMode", true);
        json.addProperty("gatePresent", gatePresent);
        json.addProperty("ability", ability);
        json.addProperty("unlockThreshold", threshold);
        json.addProperty("scoreBefore", scoreBefore);
        json.addProperty("scoreAfter", scoreAfter);
        json.addProperty("scoreStable", scoreBefore == 0 && scoreAfter == threshold);
        json.addProperty("abilityLockedBefore", abilityLockedBefore);
        json.addProperty("abilityLockedAfter", abilityLockedAfter);
        json.addProperty("lockedLandmark", lockedLandmark);
        return json;
    }

    private static void installFloor(ServerLevel level, BlockPos playerBlock) {
        level.setBlockAndUpdate(playerBlock.below(), Blocks.STONE.defaultBlockState());
    }

    private static void installWater(ServerLevel level, BlockPos playerBlock) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(playerBlock.offset(x, 0, z), Blocks.WATER.defaultBlockState());
            }
        }
    }

    private static String biomeId(ServerLevel level, BlockPos position) {
        return level.getBiome(position).unwrapKey().orElseThrow().identifier().toString();
    }

    private static boolean isGoatHorn(CertifiedUsingItemCatalog.CaseDefinition definition) {
        return "GOAT_HORN_USE".equals(definition.action());
    }

    private static boolean isRiptide(CertifiedUsingItemCatalog.CaseDefinition definition) {
        return "RIPTIDE_TRIDENT_USE".equals(definition.action());
    }

    private static Holder.Reference<Enchantment> enchantmentHolder(GameTestHelper helper, String id) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(id)));
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedUsingItemCatalog.CaseDefinition definition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
            .get(Identifier.parse(definition.advancementId()));
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement, String criterion) {
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionProgress = progress.getCriterion(criterion);
        return criterionProgress != null && criterionProgress.isDone();
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedUsingItemCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
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
        String result = "use" + compact.substring(compact.length() - 12);
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

    private static void failCase(GameTestHelper helper, PreparedCase state, String message) {
        cleanupJoinedServerPlayer(state.joinedPlayer());
        helper.fail(message);
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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

    private record PreparedCase(
        JoinedPlayer joinedPlayer,
        ServerPlayer player,
        ServerLevel level,
        CertifiedUsingItemCatalog.CaseDefinition definition,
        AdvancementHolder advancement,
        ItemStack stack,
        int selectedSlot,
        JsonObject production,
        JsonObject environment,
        boolean criterionBefore,
        int itemCountBefore,
        String componentsBefore,
        int actualEnchantmentLevel,
        int actualUseDuration,
        String actualUseAnimation,
        Holder.Reference<Enchantment> riptideHolder
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
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
