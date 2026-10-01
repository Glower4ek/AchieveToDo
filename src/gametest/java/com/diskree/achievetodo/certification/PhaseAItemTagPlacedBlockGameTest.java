package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PhaseAItemTagPlacedBlockGameTest implements CustomTestMethodInvoker {
    private static final int EXPECTED_SUPPORTED_CASE_COUNT = 5;
    private static final int MAX_POLL_ATTEMPTS = 10;

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record SelectedItem(BlockItem item, Identifier itemId, Holder.Reference<Item> holder, HolderSet.Named<Item> holderSet, int memberCount) {
    }

    private record PlacementPlan(
        BlockPos supportPos,
        BlockPos targetPos,
        Direction clickFace,
        Vec3 hitLocation
    ) {
    }

    private record RuntimeContext(
        CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition,
        JoinedPlayer joinedPlayer,
        SelectedItem selectedItem,
        PlacementPlan placementPlan,
        AdvancementHolder advancement,
        String supportPosDescription,
        String targetPosDescription,
        String placedBlockBefore,
        int handCountBefore,
        boolean criterionBefore,
        InteractionResult interactionResult
    ) {
    }

    @GameTest(maxTicks = 12000)
    public void executeSupportedPlacedBlockCatalog(GameTestHelper helper) {
        List<CertifiedItemTagPlacedBlockCatalog.CaseDefinition> cases = CertifiedItemTagPlacedBlockCatalog.allCases();
        if (cases.size() != EXPECTED_SUPPORTED_CASE_COUNT) {
            throw new IllegalStateException("Expected exactly " + EXPECTED_SUPPORTED_CASE_COUNT + " supported ITEM_TAG_PLACED_BLOCK cases but found " + cases.size());
        }
        runCase(helper, cases, 0);
    }

    private static void runCase(GameTestHelper helper, List<CertifiedItemTagPlacedBlockCatalog.CaseDefinition> cases, int index) {
        if (index >= cases.size()) {
            helper.succeed();
            return;
        }
        CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition = cases.get(index);
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, caseDefinition);

            SelectedItem selectedItem = resolveSelectedItem(helper, caseDefinition);
            PlacementPlan placementPlan = placementPlanFor(caseDefinition, selectedItem);
            preparePlacementPlatform(helper, placementPlan);

            BlockPos absoluteSupportPos = helper.absolutePos(placementPlan.supportPos());
            BlockPos absoluteTargetPos = helper.absolutePos(placementPlan.targetPos());
            player.teleportTo(absoluteTargetPos.getX() + 0.5D, absoluteTargetPos.getY() + 1.0D, absoluteTargetPos.getZ() - 1.5D);
            player.getInventory().clearContent();

            AdvancementHolder advancement = advancementOrThrow(helper, caseDefinition);
            AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
            CriterionProgress criterionProgressBefore = progressBefore.getCriterion(caseDefinition.criterion());
            if (criterionProgressBefore == null) {
                throw new IllegalStateException("Live AdvancementProgress did not expose criterion " + caseKey(caseDefinition));
            }
            boolean criterionBefore = criterionProgressBefore.isDone();
            if (criterionBefore) {
                throw new IllegalStateException("Criterion already complete before placement for " + caseKey(caseDefinition));
            }

            ItemStack heldStack = new ItemStack(selectedItem.item(), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
            int handCountBefore = player.getMainHandItem().getCount();
            if (handCountBefore != 1) {
                throw new IllegalStateException("Expected main-hand count 1 before placement for " + caseKey(caseDefinition) + " but got " + handCountBefore);
            }

            BlockState targetBeforeState = helper.getLevel().getBlockState(absoluteTargetPos);
            if (!targetBeforeState.isAir()) {
                throw new IllegalStateException("Expected placement target to start as air for " + caseKey(caseDefinition));
            }
            BlockState supportState = helper.getLevel().getBlockState(absoluteSupportPos);
            if (supportState.isAir()) {
                throw new IllegalStateException("Expected solid support block for " + caseKey(caseDefinition));
            }

            BlockHitResult hitResult = new BlockHitResult(absoluteHitLocation(helper, placementPlan), placementPlan.clickFace(), absoluteSupportPos, false);
            InteractionResult interactionResult = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hitResult);
            if (!interactionResult.consumesAction()) {
                throw new IllegalStateException("Expected placement interaction to consume action for " + caseKey(caseDefinition) + " but got " + interactionResult);
            }

            RuntimeContext runtime = new RuntimeContext(
                caseDefinition,
                joinedPlayer,
                selectedItem,
                placementPlan,
                advancement,
                absoluteSupportPos.toShortString(),
                absoluteTargetPos.toShortString(),
                blockId(targetBeforeState.getBlock()),
                handCountBefore,
                criterionBefore,
                interactionResult
            );
            verifyAfterInteraction(helper, runtime, cases, index, 0);
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }
    }

    private static void verifyAfterInteraction(
        GameTestHelper helper,
        RuntimeContext runtime,
        List<CertifiedItemTagPlacedBlockCatalog.CaseDefinition> cases,
        int index,
        int tickDelay
    ) {
        CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition = runtime.caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        BlockPos absoluteTargetPos = helper.absolutePos(runtime.placementPlan().targetPos());
        BlockState targetAfterState = helper.getLevel().getBlockState(absoluteTargetPos);
        boolean targetNoLongerAir = !targetAfterState.isAir();
        boolean blockMatches = targetAfterState.getBlock() == runtime.selectedItem().item().getBlock();
        AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(runtime.advancement());
        CriterionProgress criterionAfterProgress = progressAfter.getCriterion(caseDefinition.criterion());
        boolean criterionAfter = criterionAfterProgress != null && criterionAfterProgress.isDone();
        int handCountAfter = player.getMainHandItem().getCount();
        if (criterionAfter && targetNoLongerAir && blockMatches) {
            if (handCountAfter != 0) {
                cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
                helper.fail("Expected main hand to be consumed after placement for " + caseKey(caseDefinition) + " but got " + handCountAfter);
                return;
            }
            try {
                PhaseAItemTagPlacedBlockExecutionEvidence.recordGreen(
                    PhaseAItemTagPlacedBlockExecutionEvidence.projectRoot(),
                    new PhaseAItemTagPlacedBlockExecutionEvidence.RuntimeExecutionEntry(
                        caseDefinition.advancementId().toString(),
                        caseDefinition.criterion(),
                        caseDefinition.itemTag(),
                        runtime.selectedItem().itemId().toString(),
                        blockId(targetAfterState.getBlock()),
                        PhaseAItemTagPlacedBlockExecutionEvidence.FAMILY,
                        PhaseAItemTagPlacedBlockExecutionEvidence.SOURCE,
                        PhaseAItemTagPlacedBlockExecutionEvidence.GREEN,
                        runtime.criterionBefore(),
                        true,
                        runtime.handCountBefore(),
                        handCountAfter,
                        runtime.interactionResult().toString(),
                        runtime.targetPosDescription(),
                        runtime.supportPosDescription(),
                        runtime.placedBlockBefore(),
                        blockId(targetAfterState.getBlock()),
                        runtime.selectedItem().holderSet().contains(runtime.selectedItem().holder()),
                        runtime.selectedItem().memberCount(),
                        tickDelay,
                        PhaseAItemTagPlacedBlockExecutionEvidence.currentCatalogFingerprint(PhaseAItemTagPlacedBlockExecutionEvidence.projectRoot()),
                        currentRunId(),
                        PhaseAItemTagPlacedBlockExecutionEvidence.MINECRAFT_VERSION,
                        PhaseAItemTagPlacedBlockExecutionEvidence.COMPATIBILITY_MARKER
                    )
                );
            } catch (Exception e) {
                cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
                helper.fail("Failed to record GREEN runtime evidence for " + caseKey(caseDefinition) + ": " + e.getMessage());
                return;
            }
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            runCase(helper, cases, index + 1);
            return;
        }
        if (tickDelay >= MAX_POLL_ATTEMPTS) {
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            helper.fail("Placed-block proof failed for " + caseKey(caseDefinition)
                + " | criterionBefore=" + runtime.criterionBefore()
                + " | criterionAfter=" + criterionAfter
                + " | handCountBefore=" + runtime.handCountBefore()
                + " | handCountAfter=" + handCountAfter
                + " | placedBlockBefore=" + runtime.placedBlockBefore()
                + " | placedBlockAfter=" + blockId(targetAfterState.getBlock())
                + " | interactionResult=" + runtime.interactionResult()
                + " | ticks=" + tickDelay);
            return;
        }
        helper.runAfterDelay(1, () -> verifyAfterInteraction(helper, runtime, cases, index, tickDelay + 1));
    }

    private static String currentRunId() throws IOException {
        JsonObject json = com.google.gson.JsonParser.parseString(
            java.nio.file.Files.readString(
                PhaseAItemTagPlacedBlockExecutionEvidence.projectRoot().resolve(PhaseAItemTagPlacedBlockExecutionEvidence.RUN_STATE_ARTIFACT),
                java.nio.charset.StandardCharsets.UTF_8
            )
        ).getAsJsonObject();
        return json.get("runId").getAsString();
    }

    private static AdvancementHolder advancementOrThrow(GameTestHelper helper, CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static SelectedItem resolveSelectedItem(GameTestHelper helper, CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition) {
        HolderGetter<Item> itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(caseDefinition.itemTag()));
        HolderSet.Named<Item> holderSet = itemLookup.getOrThrow(tagKey);
        List<Holder.Reference<Item>> members = holderSet.stream()
            .map(holder -> (Holder.Reference<Item>) holder)
            .sorted(Comparator.comparing(holder -> holder.unwrapKey().orElseThrow().identifier().toString()))
            .toList();
        if (members.isEmpty()) {
            throw new IllegalStateException("Resolved empty item tag " + caseDefinition.itemTag() + " for " + caseKey(caseDefinition));
        }
        Holder.Reference<Item> selectedHolder = members.getFirst();
        Item selectedItem = selectedHolder.value();
        if (!(selectedItem instanceof BlockItem blockItem)) {
            throw new IllegalStateException("Selected tag member is not a BlockItem for " + caseKey(caseDefinition) + ": " + selectedHolder.unwrapKey().orElseThrow().identifier());
        }
        if (!holderSet.contains(selectedHolder)) {
            throw new IllegalStateException("Selected item holder was not contained in resolved tag " + caseDefinition.itemTag());
        }
        return new SelectedItem(blockItem, selectedHolder.unwrapKey().orElseThrow().identifier(), selectedHolder, holderSet, members.size());
    }

    private static PlacementPlan placementPlanFor(CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition, SelectedItem selectedItem) {
        String itemTag = caseDefinition.itemTag();
        if ("minecraft:fences".equals(itemTag) || "minecraft:signs".equals(itemTag) || "minecraft:banners".equals(itemTag)) {
            return new PlacementPlan(
                new BlockPos(1, 1, 1),
                new BlockPos(1, 2, 1),
                Direction.UP,
                new Vec3(1.5D, 2.0D, 1.5D)
            );
        }
        if ("minecraft:hanging_signs".equals(itemTag)) {
            return new PlacementPlan(
                new BlockPos(1, 3, 1),
                new BlockPos(1, 2, 1),
                Direction.DOWN,
                new Vec3(1.5D, 3.0D, 1.5D)
            );
        }
        if ("minecraft:trapdoors".equals(itemTag)) {
            return new PlacementPlan(
                new BlockPos(1, 2, 2),
                new BlockPos(1, 2, 1),
                Direction.NORTH,
                new Vec3(1.5D, 2.5D, 2.0D)
            );
        }
        throw new IllegalStateException("Unsupported ITEM_TAG_PLACED_BLOCK placement plan for " + caseKey(caseDefinition) + " via " + selectedItem.itemId());
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition
    ) {
        ServerPlayer player = joinedPlayer.player();
        if (!player.connection.hasClientLoaded()) {
            throw new IllegalStateException("Expected a production-equivalent loaded player lifecycle for " + caseKey(caseDefinition));
        }
        if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
            throw new IllegalStateException("Expected a SURVIVAL joined player for " + caseKey(caseDefinition));
        }
        if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
            throw new IllegalStateException("Expected joined ServerPlayer registration for " + caseKey(caseDefinition));
        }
        if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
            throw new IllegalStateException("Expected scheduler connection registration for " + caseKey(caseDefinition));
        }
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ClientInformation clientInformation = ClientInformation.createDefault();
        UUID playerId = UUID.randomUUID();
        GameProfile profile = new GameProfile(playerId, "item-tag-placed-block-test-player");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, clientInformation);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new JoinedPlayer(player, connection);
    }

    private static void cleanupJoinedServerPlayer(GameTestHelper helper, JoinedPlayer joinedPlayer) {
        MinecraftServer server = helper.getLevel().getServer();
        if (joinedPlayer.player().containerMenu != joinedPlayer.player().inventoryMenu) {
            joinedPlayer.player().closeContainer();
        }
        server.getPlayerList().remove(joinedPlayer.player());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        server.getConnection().getConnections().remove(joinedPlayer.connection());
    }

    private static void preparePlacementPlatform(GameTestHelper helper, PlacementPlan placementPlan) {
        Level level = helper.getLevel();
        List<BlockPos> clearPositions = new ArrayList<>();
        clearPositions.add(placementPlan.targetPos());
        clearPositions.add(placementPlan.targetPos().above());
        clearPositions.add(placementPlan.targetPos().below());
        clearPositions.add(placementPlan.targetPos().north());
        clearPositions.add(placementPlan.targetPos().south());
        clearPositions.add(placementPlan.targetPos().east());
        clearPositions.add(placementPlan.targetPos().west());
        clearPositions.add(placementPlan.supportPos().above());
        clearPositions.add(placementPlan.supportPos().below());
        for (BlockPos clearPos : clearPositions) {
            if (!clearPos.equals(placementPlan.supportPos())) {
                level.setBlockAndUpdate(helper.absolutePos(clearPos), Blocks.AIR.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(helper.absolutePos(placementPlan.supportPos()), Blocks.STONE.defaultBlockState());
    }

    private static Vec3 absoluteHitLocation(GameTestHelper helper, PlacementPlan placementPlan) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new Vec3(
            origin.getX() + placementPlan.hitLocation().x(),
            origin.getY() + placementPlan.hitLocation().y(),
            origin.getZ() + placementPlan.hitLocation().z()
        );
    }

    private static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String caseKey(CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
