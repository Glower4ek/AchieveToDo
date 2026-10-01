package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real joined-player item_used_on_block certification harness.  The canary and
 * exact15 coordinator share only nonterminal case execution; each registered
 * test owns exactly one evidence run and one terminal result.
 */
public final class PhaseAItemTagItemUsedOnBlockGameTest implements CustomTestMethodInvoker {
    private static final int EXPECTED_CASE_COUNT = 15;
    private static final int MAX_POLL_TICKS = 10;
    private static final int TOOL_UNLOCK_SCORE = 1000;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    /* 15 x (2 lifecycle ticks + 10 observation ticks + 1 cleanup handoff) + 30 setup/final headroom + 15 reserve. */
    private static final int EXACT15_MAX_TICKS = 240;
    private static final int CANARY_MAX_TICKS = 60;
    private static final BlockPos TARGET_POS = new BlockPos(1, 2, 1);
    private record JoinedPlayer(ServerPlayer player, Connection connection, EmbeddedChannel channel) {
    }

    private record SelectedTool(
        Item item,
        Identifier itemId,
        Holder.Reference<Item> holder,
        HolderSet.Named<Item> holderSet,
        int memberCount,
        boolean runtimeMembership
    ) {
    }

    private record Fixture(
        BlockPos targetPos,
        BlockState before,
        String beforeId,
        String expectedAfterId,
        BlockState beforePropertiesState
    ) {
    }

    private record CaseRuntime(
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition,
        JoinedPlayer joinedPlayer,
        SelectedTool selectedTool,
        Fixture fixture,
        AdvancementHolder advancement,
        boolean criterionBefore,
        boolean abilityLockedBefore,
        int scoreBefore,
        int scoreAfter,
        boolean abilityLockedAfter,
        boolean lockedLandmark,
        int damageBefore,
        int countBefore,
        ItemStack interactionStack,
        InteractionResult interactionResult,
        BlockState blockStateBefore,
        boolean normalScheduler
    ) {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void itemTagItemUsedOnBlockCanary(GameTestHelper helper) {
        List<CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition> cases = CertifiedItemTagItemUsedOnBlockCatalog.allCases();
        if (cases.size() != EXPECTED_CASE_COUNT
            || !cases.getFirst().key().equals("blazeandcave:building/lost_its_bark#stripped_wood")) {
            helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK catalog is not exact15 with the accepted lost_its_bark canary first");
            return;
        }
        try {
            String runId = PhaseAItemTagItemUsedOnBlockExecutionEvidence.beginRun(PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot());
            executeCase(helper, cases.getFirst(), runId, (completedHelper, runtime) -> {
                try {
                    PhaseAItemTagItemUsedOnBlockExecutionEvidence.loadValidatedTemporaryArtifact(
                        PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot()
                    );
                    completedHelper.succeed();
                } catch (Exception e) {
                    completedHelper.fail("Canary TEMP_DIAGNOSTIC validation failed: " + e.getMessage());
                }
            });
        } catch (Throwable t) {
            helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK canary setup failed: " + describe(t));
        }
    }

    @GameTest(maxTicks = EXACT15_MAX_TICKS)
    public void itemTagItemUsedOnBlockExact15(GameTestHelper helper) {
        List<CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition> cases = CertifiedItemTagItemUsedOnBlockCatalog.allCases();
        if (cases.size() != EXPECTED_CASE_COUNT) {
            helper.fail("Expected exactly 15 ITEM_TAG_ITEM_USED_ON_BLOCK catalog cases but found " + cases.size());
            return;
        }
        try {
            String runId = PhaseAItemTagItemUsedOnBlockExecutionEvidence.beginRun(PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot());
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK exact15 setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAItemTagItemUsedOnBlockExecutionEvidence.RuntimeExecutionArtifact artifact =
                    PhaseAItemTagItemUsedOnBlockExecutionEvidence.loadValidatedPromotableTemporaryArtifact(
                        PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot()
                    );
                if (artifact.entries().size() != EXPECTED_CASE_COUNT) {
                    throw new IllegalStateException("Expected exact15 TEMP entries but found " + artifact.entries().size());
                }
                if (!runId.equals(artifact.runId())) {
                    throw new IllegalStateException("Exact15 runId changed during coordinator");
                }
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("Exact15 TEMP_PROMOTABLE validation failed: " + describe(t));
            }
            return;
        }
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition = cases.get(index);
        executeCase(helper, caseDefinition, runId, (completedHelper, runtime) -> {
            try {
                if (!runId.equals(PhaseAItemTagItemUsedOnBlockExecutionEvidence.currentRunId(
                    PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot()
                ))) {
                    throw new IllegalStateException("Coordinator runId changed before recording " + caseDefinition.key());
                }
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completedHelper.fail("Exact15 coordinator failed after " + caseDefinition.key() + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition,
        String runId,
        CaseCompletion completion
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, caseDefinition);
            JoinedPlayer scheduledPlayer = joinedPlayer;
            helper.runAfterDelay(2, () -> {
                try {
                    executeJoinedCase(helper, caseDefinition, runId, completion, scheduledPlayer);
                } catch (Throwable t) {
                    cleanupJoinedServerPlayer(helper, scheduledPlayer);
                    helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK scheduled case failed for " + caseDefinition.key() + ": " + describe(t));
                }
            });
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK case setup failed for " + caseDefinition.key() + ": " + describe(t));
        }
    }

    private static void executeJoinedCase(
        GameTestHelper helper,
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) {
        if (!Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread())) {
            throw new IllegalStateException("Case did not execute on the normal server scheduler thread");
        }
        boolean normalScheduler = true;
        ServerPlayer player = joinedPlayer.player();
        SelectedTool selectedTool = resolveSelectedTool(helper, caseDefinition);
        Fixture fixture = prepareFixture(helper, caseDefinition);
        BlockPos targetPos = helper.absolutePos(fixture.targetPos());
        player.teleportTo(targetPos.getX() + 0.5D, targetPos.getY() + 1.0D, targetPos.getZ() - 1.5D);
        player.getInventory().clearContent();

        AdvancementHolder advancement = advancementOrThrow(helper, caseDefinition);
        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionBeforeProgress = progressBefore.getCriterion(caseDefinition.criterion());
        require(criterionBeforeProgress != null, "Live AdvancementProgress did not expose " + caseDefinition.key());
        boolean criterionBefore = criterionBeforeProgress.isDone();
        require(!criterionBefore, "Criterion was already complete before production preconditions: " + caseDefinition.key());

        ProductionPreconditions preconditions = unlockThroughScoreboard(helper, player, selectedTool.item(), targetPos, caseDefinition);
        CriterionProgress criterionAfterUnlock = player.getAdvancements().getOrStartProgress(advancement).getCriterion(caseDefinition.criterion());
        require(criterionAfterUnlock != null && !criterionAfterUnlock.isDone(),
            "Scoreboard unlock completed target criterion before useItemOn: " + caseDefinition.key());

        ItemStack interactionStack = new ItemStack(selectedTool.holder(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, interactionStack);
        require(player.getMainHandItem() == interactionStack, "Could not retain interaction stack reference: " + caseDefinition.key());
        int countBefore = interactionStack.getCount();
        int damageBefore = interactionStack.getDamageValue();
        require(countBefore == 1, "Expected durable tool count 1 before useItemOn: " + caseDefinition.key());
        BlockState blockStateBefore = helper.getLevel().getBlockState(targetPos);
        require(blockStateBefore.getBlock() == blockById(fixture.beforeId()), "Fixture changed before useItemOn: " + caseDefinition.key());

        BlockHitResult hit = new BlockHitResult(
            new Vec3(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D),
            Direction.UP,
            targetPos,
            false
        );
        InteractionResult interactionResult = player.gameMode.useItemOn(
            player,
            helper.getLevel(),
            interactionStack,
            InteractionHand.MAIN_HAND,
            hit
        );
        require(interactionResult.consumesAction(), "useItemOn did not consume action: " + caseDefinition.key() + " -> " + interactionResult);
        require(interactionResult instanceof InteractionResult.Success, "Expected 26.2 Success result: " + caseDefinition.key());
        require(itemId(interactionStack).toString().equals(caseDefinition.preferredToolItem()),
            "Interaction stack item changed unexpectedly: " + caseDefinition.key());
        require(interactionStack.getCount() == countBefore,
            "Interaction stack count changed unexpectedly: " + caseDefinition.key());
        require(interactionStack.getDamageValue() == damageBefore + 1,
            "Interaction stack did not receive one ordinary durability damage: " + caseDefinition.key());
        require(!AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(selectedTool.item())),
            "Tool ability became locked during real interaction: " + caseDefinition.key());

        CaseRuntime runtime = new CaseRuntime(
            caseDefinition,
            joinedPlayer,
            selectedTool,
            fixture,
            advancement,
            criterionBefore,
            preconditions.abilityLockedBefore(),
            preconditions.scoreBefore(),
            preconditions.scoreAfter(),
            preconditions.abilityLockedAfter(),
            preconditions.lockedLandmark(),
            damageBefore,
            countBefore,
            interactionStack,
            interactionResult,
            blockStateBefore,
            normalScheduler
        );
        pollForProof(helper, runtime, runId, 0, completion);
    }

    private static void pollForProof(
        GameTestHelper helper,
        CaseRuntime runtime,
        String runId,
        int ticksToCriterion,
        CaseCompletion completion
    ) {
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition = runtime.caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        try {
            Level level = helper.getLevel();
            BlockPos targetPos = helper.absolutePos(runtime.fixture().targetPos());
            BlockState after = level.getBlockState(targetPos);
            AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(runtime.advancement());
            CriterionProgress criterionAfterProgress = progressAfter.getCriterion(caseDefinition.criterion());
            boolean criterionAfter = criterionAfterProgress != null && criterionAfterProgress.isDone();
            boolean blockMatches = after.getBlock() == blockById(caseDefinition.expectedPostBlock());
            boolean stateMatches = stateMatches(after, caseDefinition.expectedPostState());
            ItemStack selectedStack = player.getMainHandItem();
            ItemStack interactionStack = runtime.interactionStack();
            boolean stackMatches = itemId(interactionStack).toString().equals(caseDefinition.preferredToolItem())
                && interactionStack.getCount() == runtime.countBefore()
                && interactionStack.getDamageValue() == runtime.damageBefore() + 1;

            if (criterionAfter && blockMatches && stateMatches && stackMatches) {
                JsonObject receipt = buildReceipt(helper, runtime, after, ticksToCriterion);
                PhaseAItemTagItemUsedOnBlockExecutionEvidence.recordGreen(
                    PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot(),
                    receipt
                );
                System.out.println("ITEM_TAG_ITEM_USED_ON_BLOCK_CASE=" + caseDefinition.key()
                    + " | action=" + caseDefinition.action()
                    + " | itemTag=#" + caseDefinition.itemTag()
                    + " | selectedItem=" + caseDefinition.preferredToolItem()
                    + " | before=" + runtime.blockStateBefore()
                    + " | after=" + after
                    + " | criterionBefore=" + runtime.criterionBefore()
                    + " | criterionAfter=" + criterionAfter
                    + " | damage=" + runtime.damageBefore() + "->" + interactionStack.getDamageValue()
                    + " | ticks=" + ticksToCriterion);
                cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
                helper.runAfterDelay(1, () -> completion.complete(helper, runtime));
                return;
            }
            if (ticksToCriterion >= MAX_POLL_TICKS) {
                throw new IllegalStateException(
                    "semantic proof failed | criterionBefore=" + runtime.criterionBefore()
                        + " | criterionAfter=" + criterionAfter
                        + " | before=" + runtime.blockStateBefore()
                        + " | after=" + after
                        + " | expectedPost=" + caseDefinition.expectedPostBlock()
                        + " | interactionResult=" + runtime.interactionResult()
                        + " | item=" + itemId(selectedStack)
                        + " | count=" + selectedStack.getCount()
                        + " | damage=" + selectedStack.getDamageValue()
                );
            }
            helper.runAfterDelay(1, () -> pollForProof(helper, runtime, runId, ticksToCriterion + 1, completion));
        } catch (Throwable t) {
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            helper.fail("ITEM_TAG_ITEM_USED_ON_BLOCK proof failed for " + caseDefinition.key() + ": " + describe(t));
        }
    }

    private static JsonObject buildReceipt(GameTestHelper helper, CaseRuntime runtime, BlockState after, int ticksToCriterion) {
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition definition = runtime.caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        ItemStack stack = runtime.interactionStack();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId().toString());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("itemTag", definition.itemTag());
        receipt.addProperty("selectedItem", definition.preferredToolItem());
        receipt.addProperty("runtimeItemTagMembership", runtime.selectedTool().runtimeMembership());
        receipt.addProperty("runtimeItemTagMemberCount", runtime.selectedTool().memberCount());
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("joined", helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", helper.getLevel().getServer().getConnection().getConnections().contains(runtime.joinedPlayer().connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", runtime.normalScheduler());
        receipt.addProperty("buildPermission", player.mayBuild());
        BlockPos targetPos = helper.absolutePos(runtime.fixture().targetPos());
        receipt.addProperty("clickedPosition", targetPos.toShortString());
        receipt.addProperty("blockStateBefore", runtime.blockStateBefore().toString());
        receipt.addProperty("blockStateAfter", after.toString());
        receipt.addProperty("blockIdBefore", runtime.fixture().beforeId());
        receipt.addProperty("blockIdAfter", blockId(after.getBlock()));
        receipt.addProperty("interactionResult", runtime.interactionResult().toString());
        receipt.addProperty("interactionConsumesAction", runtime.interactionResult().consumesAction());
        receipt.addProperty("semanticMutation", true);
        receipt.addProperty("criterionBefore", runtime.criterionBefore());
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("ticksToCriterion", ticksToCriterion);
        receipt.add(
            "blockStateBeforeProperties",
            observedStateProperties(runtime.blockStateBefore(), definition.fixtureStateBefore(), definition.key())
        );
        receipt.add(
            "blockStateAfterProperties",
            observedStateProperties(after, definition.expectedPostState(), definition.key())
        );

        JsonObject preconditions = new JsonObject();
        preconditions.addProperty("scoreboardObjective", "bac_advancements");
        preconditions.addProperty("scoreBefore", runtime.scoreBefore());
        preconditions.addProperty("scoreAfter", runtime.scoreAfter());
        preconditions.addProperty("abilityLockedBefore", runtime.abilityLockedBefore());
        preconditions.addProperty("abilityLockedAfter", runtime.abilityLockedAfter());
        preconditions.addProperty("lockedLandmark", runtime.lockedLandmark());
        preconditions.addProperty("ability", "USE_DIAMOND_TOOLS");
        receipt.add("productionPreconditions", preconditions);

        JsonObject interactionStack = new JsonObject();
        interactionStack.addProperty("itemBefore", definition.preferredToolItem());
        interactionStack.addProperty("itemAfter", itemId(stack).toString());
        interactionStack.addProperty("countBefore", runtime.countBefore());
        interactionStack.addProperty("countAfter", stack.getCount());
        interactionStack.addProperty("damageBefore", runtime.damageBefore());
        interactionStack.addProperty("damageAfter", stack.getDamageValue());
        interactionStack.addProperty("sameStackReference", player.getMainHandItem() == runtime.interactionStack());
        receipt.add("interactionStack", interactionStack);

        JsonObject proof = new JsonObject();
        proof.addProperty("action", definition.action());
        proof.addProperty("realUseOn", true);
        proof.addProperty("triggerPath", "ServerPlayerGameMode.useItemOn->ItemUsedOnLocationTrigger");
        proof.addProperty("preBlock", definition.fixtureBlockBefore());
        proof.addProperty("postBlock", definition.expectedPostBlock());
        proof.addProperty("interactionConsumesAction", runtime.interactionResult().consumesAction());
        proof.addProperty("toolDamageDelta", stack.getDamageValue() - runtime.damageBefore());
        proof.addProperty("sameStackReference", player.getMainHandItem() == runtime.interactionStack());
        proof.addProperty("criterionBefore", runtime.criterionBefore());
        proof.addProperty("criterionAfter", true);
        switch (definition.action()) {
            case "STRIP_WOOD", "STRIP_LOG" -> {
                proof.addProperty("operation", "STRIP");
                proof.addProperty("vanillaMethod", "AxeItem.evaluateNewBlockState");
            }
            case "CREATE_PATH" -> {
                proof.addProperty("operation", "FLATTEN");
                proof.addProperty("vanillaMethod", "ShovelItem.useOn");
                proof.addProperty("clickedFace", "UP");
                proof.addProperty("aboveBlock", blockId(helper.getLevel().getBlockState(targetPos.above()).getBlock()));
            }
            case "AXE_COPPER_MUTATION" -> {
                proof.addProperty("operation", "DEWAX");
                proof.addProperty("vanillaMethod", "AxeItem.evaluateNewBlockState");
                proof.addProperty("litBefore", propertyValue(runtime.blockStateBefore(), true));
                proof.addProperty("litAfter", propertyValue(after, true));
                proof.addProperty("waxedBefore", runtime.blockStateBefore().getBlock() == blockById("minecraft:waxed_oxidized_copper_bulb"));
                proof.addProperty("waxedAfter", after.getBlock() == blockById("minecraft:waxed_oxidized_copper_bulb"));
            }
            default -> throw new IllegalStateException("Unsupported action category: " + definition.action());
        }
        receipt.add("actionProof", proof);
        return receipt;
    }

    private record ProductionPreconditions(
        int scoreBefore,
        int scoreAfter,
        boolean abilityLockedBefore,
        boolean abilityLockedAfter,
        boolean lockedLandmark
    ) {
    }

    private static ProductionPreconditions unlockThroughScoreboard(
        GameTestHelper helper,
        ServerPlayer player,
        Item item,
        BlockPos targetPos,
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition
    ) {
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective for " + caseDefinition.key());
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not the active default objective for " + caseDefinition.key());
        AbilityType toolAbility = AbilityType.findToolMaterialUsageAbility(item);
        require(toolAbility == AbilityType.USE_DIAMOND_TOOLS, "Expected diamond tool ability for " + caseDefinition.key());
        var scoreInfoBefore = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = scoreInfoBefore == null ? 0 : scoreInfoBefore.value();
        require(scoreBefore == 0, "Fresh bac_advancements score was not zero for " + caseDefinition.key() + ": " + scoreBefore);
        boolean abilityLockedBefore = AchieveToDoMod.isAbilityLocked(player, toolAbility);
        require(abilityLockedBefore, "Diamond tool ability was not locked before ordinary scoreboard unlock for " + caseDefinition.key());

        scoreboard.getOrCreatePlayerScore(player, objective).set(TOOL_UNLOCK_SCORE);
        var scoreInfoAfter = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = scoreInfoAfter == null ? 0 : scoreInfoAfter.value();
        require(scoreAfter == TOOL_UNLOCK_SCORE, "Scoreboard unlock did not produce score " + TOOL_UNLOCK_SCORE + " for " + caseDefinition.key());
        boolean abilityLockedAfter = AchieveToDoMod.isAbilityLocked(player, toolAbility);
        require(!abilityLockedAfter, "Diamond tool ability remained locked after ordinary scoreboard unlock for " + caseDefinition.key());
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), targetPos);
        require(!lockedLandmark, "Fixture lies in a locked landmark for " + caseDefinition.key());
        return new ProductionPreconditions(scoreBefore, scoreAfter, abilityLockedBefore, abilityLockedAfter, lockedLandmark);
    }

    private static Fixture prepareFixture(GameTestHelper helper, CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition definition) {
        BlockPos targetPos = TARGET_POS;
        Level level = helper.getLevel();
        Block sourceBlock = blockById(definition.fixtureBlockBefore());
        Block expectedPostBlock = blockById(definition.expectedPostBlock());
        BlockState before = sourceBlock.defaultBlockState();
        if (!definition.fixtureStateBefore().isEmpty()) {
            require(sourceBlock instanceof CopperBulbBlock, "Only copper bulb fixture carries a state predicate: " + definition.key());
            require(definition.fixtureStateBefore().get("lit").equals("true"), "Unexpected fixture state for " + definition.key());
            before = before.setValue(CopperBulbBlock.LIT, true);
        }
        level.setBlockAndUpdate(helper.absolutePos(targetPos.below()), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(targetPos), before);
        level.setBlockAndUpdate(helper.absolutePos(targetPos.above()), Blocks.AIR.defaultBlockState());
        require(level.getBlockState(helper.absolutePos(targetPos)).getBlock() == sourceBlock, "Could not place fixture for " + definition.key());
        return new Fixture(targetPos, before, definition.fixtureBlockBefore(), blockId(expectedPostBlock), before);
    }

    private static SelectedTool resolveSelectedTool(GameTestHelper helper, CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition definition) {
        HolderGetter<Item> itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(definition.itemTag()));
        HolderSet.Named<Item> holderSet = itemLookup.getOrThrow(tagKey);
        List<Holder.Reference<Item>> members = holderSet.stream()
            .map(holder -> (Holder.Reference<Item>) holder)
            .sorted(Comparator.comparing(holder -> holder.unwrapKey().orElseThrow().identifier().toString()))
            .toList();
        require(!members.isEmpty(), "Resolved empty item tag " + definition.itemTag() + " for " + definition.key());
        Holder.Reference<Item> selectedHolder = null;
        for (Holder.Reference<Item> member : members) {
            if (member.unwrapKey().orElseThrow().identifier().toString().equals(definition.preferredToolItem())) {
                selectedHolder = member;
                break;
            }
        }
        require(selectedHolder != null, "Preferred runtime tag member is absent from " + definition.itemTag() + ": " + definition.preferredToolItem());
        boolean runtimeMembership = holderSet.contains(selectedHolder);
        require(runtimeMembership, "Selected runtime holder is not a member of " + definition.itemTag());
        return new SelectedTool(
            selectedHolder.value(),
            selectedHolder.unwrapKey().orElseThrow().identifier(),
            selectedHolder,
            holderSet,
            members.size(),
            runtimeMembership
        );
    }

    private static AdvancementHolder advancementOrThrow(GameTestHelper helper, CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition definition) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(definition.advancementId());
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Player was not joined for " + definition.key());
        require(server.getConnection().getConnections().contains(joinedPlayer.connection()), "Connection was not registered for " + definition.key());
        require(player.connection.hasClientLoaded(), "Client-loaded packet was not accepted for " + definition.key());
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "Expected SURVIVAL for " + definition.key());
        require(player.mayBuild(), "Expected build permission for " + definition.key());
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
        return new JoinedPlayer(player, connection, channel);
    }

    private static String profileNameForUuid(UUID playerId) {
        String compactUuid = playerId.toString().replace("-", "");
        String profileName = "itag" + compactUuid.substring(compactUuid.length() - 12);
        require(profileName.length() <= 16, "Generated player profile name exceeds Minecraft's 16-character limit: " + profileName);
        return profileName;
    }

    private static void cleanupJoinedServerPlayer(GameTestHelper helper, JoinedPlayer joinedPlayer) {
        MinecraftServer server = helper.getLevel().getServer();
        if (joinedPlayer.player().containerMenu != joinedPlayer.player().inventoryMenu) {
            joinedPlayer.player().closeContainer();
        }
        server.getPlayerList().remove(joinedPlayer.player());
        server.getConnection().getConnections().remove(joinedPlayer.connection());
        settleOpenEmbeddedChannel(joinedPlayer.channel());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(TARGET_POS), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(TARGET_POS.above()), Blocks.AIR.defaultBlockState());
    }

    private static void settleOpenEmbeddedChannel(EmbeddedChannel channel) {
        require(channel.isOpen(), "EmbeddedChannel closed before GameTest cleanup settlement");
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
                    throw new IllegalStateException("EmbeddedChannel cleanup exceeded "
                        + MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES + " outbound messages");
                }
            }

            if (releasedThisPass == 0 && !channel.hasPendingTasks()) {
                return;
            }
        }
        throw new IllegalStateException("EmbeddedChannel cleanup did not quiesce within "
            + MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES + " passes");
    }

    private static boolean stateMatches(BlockState state, Map<String, String> expected) {
        if (expected.isEmpty()) {
            return true;
        }
        if (!(state.getBlock() instanceof CopperBulbBlock)) {
            return false;
        }
        return expected.size() == 1
            && "true".equals(expected.get("lit"))
            && state.getValue(CopperBulbBlock.LIT);
    }

    private static JsonObject observedStateProperties(
        BlockState observed,
        Map<String, String> relevantProperties,
        String caseKey
    ) {
        JsonObject json = new JsonObject();
        for (String property : relevantProperties.keySet().stream().sorted().toList()) {
            switch (property) {
                case "lit" -> {
                    require(observed.getBlock() instanceof CopperBulbBlock, "Observed state for " + caseKey + " is not a copper bulb");
                    json.addProperty(property, Boolean.toString(observed.getValue(CopperBulbBlock.LIT)));
                }
                default -> throw new IllegalStateException("Unsupported observed block-state property for " + caseKey + ": " + property);
            }
        }
        return json;
    }

    private static boolean propertyValue(BlockState state, boolean expected) {
        return state.getBlock() instanceof CopperBulbBlock && state.getValue(CopperBulbBlock.LIT) == expected;
    }

    private static Block blockById(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        require(block != null, "Missing registered block " + id);
        return block;
    }

    private static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static Identifier itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
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

    @FunctionalInterface
    private interface CaseCompletion {
        void complete(GameTestHelper helper, CaseRuntime runtime);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
