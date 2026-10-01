package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PhaseATrimPatternRecipeCraftedGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TABLE_POS = new BlockPos(1, 1, 1);
    private static final int OPEN_SMITHING_TABLE_UNLOCK_THRESHOLD = 294;
    private static final int MAX_CRITERION_POLL_ATTEMPTS = 20;
    private static final int EXPECTED_CASE_COUNT = 18;

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private static final class Exact18Runtime {
        private final List<CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition> cases;
        private final Set<String> completedReceiptKeys = new LinkedHashSet<>();
        private int nextCaseIndex;

        private Exact18Runtime(List<CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition> cases) {
            this.cases = List.copyOf(cases);
        }

        private CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition currentCase() {
            if (nextCaseIndex >= cases.size()) {
                throw new IllegalStateException("No remaining TRIM_PATTERN_RECIPE_CRAFTED case to execute");
            }
            return cases.get(nextCaseIndex);
        }

        private void markReceiptRecorded(CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition) {
            if (!completedReceiptKeys.add(caseKey(caseDefinition))) {
                throw new IllegalStateException("Duplicate exact18 receipt key: " + caseKey(caseDefinition));
            }
            nextCaseIndex++;
        }

        private boolean isComplete() {
            return nextCaseIndex == cases.size();
        }

        private void requireExactCoverage() {
            Set<String> expectedKeys = new LinkedHashSet<>();
            for (CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition : cases) {
                expectedKeys.add(caseKey(caseDefinition));
            }
            if (cases.size() != EXPECTED_CASE_COUNT
                || completedReceiptKeys.size() != EXPECTED_CASE_COUNT
                || !completedReceiptKeys.equals(expectedKeys)) {
                throw new IllegalStateException(
                    "Expected exactly 18 distinct TRIM_PATTERN_RECIPE_CRAFTED receipts"
                        + " | caseCount=" + cases.size()
                        + " | receiptCount=" + completedReceiptKeys.size()
                );
            }
        }
    }

    private record CanaryRuntime(
        Exact18Runtime familyRuntime,
        JoinedPlayer joinedPlayer,
        AdvancementHolder advancement,
        CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition,
        String recipeId,
        ItemStack producedItem,
        boolean criterionBefore,
        boolean normalResultTaken,
        boolean smithingTableInteraction,
        BlockPos tablePos
    ) {
    }

    @GameTest(maxTicks = 600)
    public void trimPatternRecipeCraftedExact18(GameTestHelper helper) {
        List<CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition> cases =
            CertifiedTrimPatternRecipeCraftedCatalog.allCases();
        if (cases.size() != EXPECTED_CASE_COUNT) {
            throw new IllegalStateException("Expected exact18 catalog but found " + cases.size() + " cases");
        }
        runNextCase(helper, new Exact18Runtime(cases));
    }

    private static void runNextCase(GameTestHelper helper, Exact18Runtime familyRuntime) {
        if (familyRuntime.isComplete()) {
            familyRuntime.requireExactCoverage();
            helper.succeed();
            return;
        }
        runCase(helper, familyRuntime, familyRuntime.currentCase());
    }

    private static void runCase(
        GameTestHelper helper,
        Exact18Runtime familyRuntime,
        CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition
    ) {
        JoinedPlayer joinedPlayer = null;
        BlockPos tablePos = helper.absolutePos(TABLE_POS);
        try {
            prepareTable(helper, tablePos);
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer);
            teleportBesideTable(player, tablePos);

            if (AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), tablePos)) {
                throw new IllegalStateException("Smithing table canary position is inside a locked landmark");
            }
            boolean lockedBeforeFixture = AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_SMITHING_TABLE);
            if (!lockedBeforeFixture) {
                throw new IllegalStateException("OPEN_SMITHING_TABLE was not locked for the fresh canary player");
            }

            InteractionResult lockedInteraction = interactWithTable(player, helper);
            if (lockedInteraction == InteractionResult.PASS) {
                throw new IllegalStateException("Locked Smithing Table interaction unexpectedly passed");
            }
            if (player.containerMenu != player.inventoryMenu) {
                throw new IllegalStateException("Locked Smithing Table interaction opened a menu");
            }

            AchieveToDoMod.getServer().setObtainedCount(player, OPEN_SMITHING_TABLE_UNLOCK_THRESHOLD);
            boolean lockedAfterFixture = AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_SMITHING_TABLE);
            if (lockedAfterFixture) {
                throw new IllegalStateException(
                    "OPEN_SMITHING_TABLE remained locked at accepted threshold " + OPEN_SMITHING_TABLE_UNLOCK_THRESHOLD
                );
            }

            InteractionResult openInteraction = interactWithTable(player, helper);
            if (openInteraction == InteractionResult.PASS || !(player.containerMenu instanceof SmithingMenu menu)) {
                throw new IllegalStateException("Unlocked normal Smithing Table interaction did not open SmithingMenu");
            }
            if (!openInteraction.consumesAction()) {
                throw new IllegalStateException("Unlocked Smithing Table interaction did not consume its action");
            }

            AdvancementHolder advancement = advancementOrThrow(helper, caseDefinition);
            AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
            CriterionProgress criterionProgressBefore = progressBefore.getCriterion(caseDefinition.criterion());
            if (criterionProgressBefore == null) {
                throw new IllegalStateException("Live AdvancementProgress did not expose " + caseKey(caseDefinition));
            }
            boolean criterionBefore = criterionProgressBefore.isDone();
            if (criterionBefore) {
                throw new IllegalStateException("Criterion was already complete before smithing result take");
            }

            ItemStack template = itemStack(helper, caseDefinition.expectedTemplateItem());
            ItemStack base = itemStack(helper, PhaseATrimPatternRecipeCraftedExecutionEvidence.BASE_ITEM);
            ItemStack addition = itemStack(helper, PhaseATrimPatternRecipeCraftedExecutionEvidence.ADDITION_ITEM);
            menu.getSlot(SmithingMenu.TEMPLATE_SLOT).set(template);
            menu.getSlot(SmithingMenu.BASE_SLOT).set(base);
            menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(addition);
            menu.slotsChanged(menu.getSlot(SmithingMenu.TEMPLATE_SLOT).container);
            menu.broadcastChanges();

            ItemStack preview = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().copy();
            if (preview.isEmpty()) {
                throw new IllegalStateException(
                    "SmithingMenu did not create a trim preview for " + caseKey(caseDefinition)
                );
            }
            ResultContainer resultContainer = resultContainer(menu);
            RecipeHolder<?> recipeUsed = resultContainer.getRecipeUsed();
            if (recipeUsed == null || !(recipeUsed.value() instanceof SmithingTrimRecipe)) {
                throw new IllegalStateException("Smithing result did not retain a SmithingTrimRecipe holder");
            }
            String recipeId = recipeUsed.id().identifier().toString();
            if (!caseDefinition.expectedRecipeId().equals(recipeId)) {
                throw new IllegalStateException("Unexpected Smithing recipe holder: " + recipeId);
            }
            validateProducedTrim(preview, caseDefinition.expectedTrimPattern());

            Int2ObjectMap<HashedStack> changedSlots = Int2ObjectMaps.emptyMap();
            new ServerboundContainerClickPacket(
                menu.containerId,
                menu.getStateId(),
                (short) SmithingMenu.RESULT_SLOT,
                (byte) 0,
                ContainerInput.PICKUP,
                changedSlots,
                HashedStack.EMPTY
            ).handle(player.connection);

            ItemStack producedItem = menu.getCarried().copy();
            boolean resultSlotEmpty = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem().isEmpty();
            boolean inputsConsumed = menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()
                && menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty()
                && menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty();
            boolean normalResultTaken = resultSlotEmpty && inputsConsumed && !producedItem.isEmpty();
            if (!normalResultTaken) {
                throw new IllegalStateException(
                    "Real container result click did not take the result normally"
                        + " | resultSlotEmpty=" + resultSlotEmpty
                        + " | inputsConsumed=" + inputsConsumed
                        + " | carried=" + producedItem
                );
            }
            validateProducedTrim(producedItem, caseDefinition.expectedTrimPattern());

            CanaryRuntime runtime = new CanaryRuntime(
                familyRuntime,
                joinedPlayer,
                advancement,
                caseDefinition,
                recipeId,
                producedItem,
                criterionBefore,
                normalResultTaken,
                true,
                tablePos
            );
            helper.runAfterDelay(1, () -> pollForCriterion(helper, runtime, 1));
        } catch (Throwable t) {
            cleanup(helper, joinedPlayer, tablePos);
            throw t;
        }
    }

    private static void pollForCriterion(GameTestHelper helper, CanaryRuntime runtime, int ticksToCriterion) {
        try {
            ServerPlayer player = runtime.joinedPlayer().player();
            AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(runtime.advancement());
            CriterionProgress criterionProgressAfter = progressAfter.getCriterion(runtime.caseDefinition().criterion());
            boolean criterionAfter = criterionProgressAfter != null && criterionProgressAfter.isDone();
            if (criterionAfter) {
                Path projectRoot = PhaseATrimPatternRecipeCraftedExecutionEvidence.projectRoot();
                String catalogFingerprint = PhaseATrimPatternRecipeCraftedExecutionEvidence.currentCatalogFingerprint(projectRoot);
                String runId = PhaseATrimPatternRecipeCraftedExecutionEvidence.currentRunId(projectRoot);
                ArmorTrim trim = runtime.producedItem().get(DataComponents.TRIM);
                if (trim == null) {
                    throw new IllegalStateException("Produced result lost DataComponents.TRIM before evidence recording");
                }
                PhaseATrimPatternRecipeCraftedExecutionEvidence.recordGreen(
                    projectRoot,
                    new PhaseATrimPatternRecipeCraftedExecutionEvidence.DiagnosticReceipt(
                        runtime.caseDefinition().advancementId().toString(),
                        runtime.caseDefinition().criterion(),
                        runtime.caseDefinition().trigger(),
                        runtime.recipeId(),
                        runtime.caseDefinition().expectedTrimPattern(),
                        runtime.caseDefinition().expectedTemplateItem(),
                        PhaseATrimPatternRecipeCraftedExecutionEvidence.BASE_ITEM,
                        PhaseATrimPatternRecipeCraftedExecutionEvidence.ADDITION_ITEM,
                        itemId(runtime.producedItem()),
                        trim.pattern().unwrapKey().orElseThrow().identifier().toString(),
                        trim.material().unwrapKey().orElseThrow().identifier().toString(),
                        runtime.criterionBefore(),
                        criterionAfter,
                        runtime.normalResultTaken(),
                        runtime.smithingTableInteraction(),
                        PhaseATrimPatternRecipeCraftedExecutionEvidence.FAMILY,
                        PhaseATrimPatternRecipeCraftedExecutionEvidence.SOURCE,
                        PhaseATrimPatternRecipeCraftedExecutionEvidence.GREEN,
                        ticksToCriterion,
                        catalogFingerprint,
                        runId
                    )
                );
                runtime.familyRuntime().markReceiptRecorded(runtime.caseDefinition());
                cleanup(helper, runtime.joinedPlayer(), runtime.tablePos());
                if (runtime.familyRuntime().isComplete()) {
                    runtime.familyRuntime().requireExactCoverage();
                    helper.succeed();
                } else {
                    helper.runAfterDelay(1, () -> runNextCase(helper, runtime.familyRuntime()));
                }
                return;
            }
            if (ticksToCriterion >= MAX_CRITERION_POLL_ATTEMPTS) {
                cleanup(helper, runtime.joinedPlayer(), runtime.tablePos());
                helper.fail(
                    "Recipe-crafted criterion did not complete after normal result take"
                        + " | criterionBefore=" + runtime.criterionBefore()
                        + " | criterionAfter=" + criterionAfter
                        + " | recipeId=" + runtime.recipeId()
                );
                return;
            }
            helper.runAfterDelay(1, () -> pollForCriterion(helper, runtime, ticksToCriterion + 1));
        } catch (IOException e) {
            cleanup(helper, runtime.joinedPlayer(), runtime.tablePos());
            throw new IllegalStateException(
                "Failed to record trim-pattern recipe-crafted canary evidence",
                e
            );
        } catch (RuntimeException | Error e) {
            cleanup(helper, runtime.joinedPlayer(), runtime.tablePos());
            throw e;
        }
    }

    private static void prepareTable(GameTestHelper helper, BlockPos tablePos) {
        helper.getLevel().setBlockAndUpdate(tablePos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(tablePos, Blocks.SMITHING_TABLE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(tablePos.above(), Blocks.AIR.defaultBlockState());
    }

    private static void teleportBesideTable(ServerPlayer player, BlockPos tablePos) {
        player.teleportTo(tablePos.getX() + 0.5D, tablePos.getY() + 1.0D, tablePos.getZ() - 1.5D);
    }

    private static InteractionResult interactWithTable(ServerPlayer player, GameTestHelper helper) {
        BlockPos tablePos = helper.absolutePos(TABLE_POS);
        BlockHitResult hit = new BlockHitResult(
            new Vec3(tablePos.getX() + 0.5D, tablePos.getY() + 0.5D, tablePos.getZ() + 0.5D),
            net.minecraft.core.Direction.UP,
            tablePos,
            false
        );
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static ItemStack itemStack(GameTestHelper helper, String itemId) {
        HolderGetter<Item> itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        Holder.Reference<Item> holder = itemLookup.getOrThrow(ResourceKey.create(Registries.ITEM, Identifier.parse(itemId)));
        return new ItemStack(holder);
    }

    private static ResultContainer resultContainer(SmithingMenu menu) {
        if (!(menu.getSlot(SmithingMenu.RESULT_SLOT).container instanceof ResultContainer resultContainer)) {
            throw new IllegalStateException("Smithing result slot did not use ResultContainer");
        }
        return resultContainer;
    }

    private static void validateProducedTrim(ItemStack stack, String expectedPattern) {
        if (!PhaseATrimPatternRecipeCraftedExecutionEvidence.BASE_ITEM.equals(itemId(stack))) {
            throw new IllegalStateException("Expected produced item " + PhaseATrimPatternRecipeCraftedExecutionEvidence.BASE_ITEM);
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim == null) {
            throw new IllegalStateException("Expected produced stack to contain DataComponents.TRIM");
        }
        String pattern = trim.pattern().unwrapKey().orElseThrow().identifier().toString();
        String material = trim.material().unwrapKey().orElseThrow().identifier().toString();
        if (!expectedPattern.equals(pattern)) {
            throw new IllegalStateException("Expected produced trim pattern " + expectedPattern + " but got " + pattern);
        }
        if (!PhaseATrimPatternRecipeCraftedExecutionEvidence.PRODUCED_TRIM_MATERIAL.equals(material)) {
            throw new IllegalStateException(
                "Expected produced trim material " + PhaseATrimPatternRecipeCraftedExecutionEvidence.PRODUCED_TRIM_MATERIAL
                    + " but got " + material
            );
        }
    }

    private static String itemId(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "trim-pattern-recipe-crafted-test-player");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new JoinedPlayer(player, connection);
    }

    private static void assertJoinedLifecycle(GameTestHelper helper, JoinedPlayer joinedPlayer) {
        ServerPlayer player = joinedPlayer.player();
        if (!player.connection.hasClientLoaded()) {
            throw new IllegalStateException("Expected client-loaded ServerPlayer lifecycle");
        }
        if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
            throw new IllegalStateException("Expected SURVIVAL non-spectator ServerPlayer lifecycle");
        }
        if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
            throw new IllegalStateException("Expected ServerPlayer registration in PlayerList");
        }
        if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
            throw new IllegalStateException("Expected Connection registration in ServerConnectionListener");
        }
    }

    private static void cleanup(GameTestHelper helper, JoinedPlayer joinedPlayer, BlockPos tablePos) {
        MinecraftServer server = helper.getLevel().getServer();
        if (joinedPlayer != null) {
            try {
                ServerPlayer player = joinedPlayer.player();
                if (player.containerMenu != player.inventoryMenu) {
                    player.closeContainer();
                }
                if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
                    server.getPlayerList().remove(player);
                }
            } finally {
                joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
                server.getConnection().getConnections().remove(joinedPlayer.connection());
            }
        }
        if (tablePos != null) {
            helper.getLevel().setBlockAndUpdate(tablePos, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(tablePos.below(), Blocks.AIR.defaultBlockState());
        }
    }

    private static String caseKey(CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
