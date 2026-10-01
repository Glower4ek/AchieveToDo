package com.diskree.achievetodo.certification;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.gametest.framework.GameTestHelper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class PhaseAContainerLootGameTest implements CustomTestMethodInvoker {
    private static final BlockPos CHEST_POS = new BlockPos(1, 1, 1);
    private static final String SOURCE = "PhaseAContainerLootGameTest";

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    @GameTest(maxTicks = 12000)
    public void certifiedContainerLootCatalog(GameTestHelper helper) {
        runCase(helper, CertifiedContainerLootCatalog.allCases(), 0);
    }

    private static void runCase(GameTestHelper helper, List<CertifiedContainerLootCatalog.CaseDefinition> cases, int caseIndex) {
        if (caseIndex >= cases.size()) {
            helper.succeed();
            return;
        }
        CertifiedContainerLootCatalog.CaseDefinition caseDefinition = cases.get(caseIndex);
        JoinedPlayer joinedPlayer = null;
        try {
            prepareChest(helper, caseDefinition);
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, caseDefinition);
        } catch (Throwable t) {
            cleanupChest(helper);
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }

        JoinedPlayer scheduledJoinedPlayer = joinedPlayer;
        helper.runAfterDelay(1, () -> {
            String failure = null;
            try {
                failure = verifyCertifiedContainerLootCase(helper, scheduledJoinedPlayer.player(), caseDefinition);
            } catch (Exception e) {
                failure = "Failed to execute runtime container loot case for " + describeCase(caseDefinition) + ": " + e.getMessage();
            }
            try {
                cleanupChest(helper);
                cleanupJoinedServerPlayer(helper, scheduledJoinedPlayer);
            } catch (Exception cleanupFailure) {
                if (failure == null) {
                    failure = "Cleanup failed for " + describeCase(caseDefinition) + ": " + cleanupFailure.getMessage();
                } else {
                    failure = failure + " | cleanupFailure=" + cleanupFailure.getMessage();
                }
            }
            if (failure != null) {
                helper.fail(failure);
                return;
            }
            helper.runAfterDelay(1, () -> runCase(helper, cases, caseIndex + 1));
        });
    }

    private static void prepareChest(GameTestHelper helper, CertifiedContainerLootCatalog.CaseDefinition caseDefinition) {
        cleanupChest(helper);
        BlockPos chestPos = helper.absolutePos(CHEST_POS);
        helper.getLevel().setBlockAndUpdate(chestPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(chestPos.above(), Blocks.AIR.defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("Expected ChestBlockEntity at " + chestPos + " for " + describeCase(caseDefinition));
        }
        chest.setLootTable(caseDefinition.lootTable());
        chest.setLootTableSeed(1L);
        if (!caseDefinition.lootTable().equals(chest.getLootTable())) {
            throw new IllegalStateException("Chest lost assigned loot table before interaction for " + describeCase(caseDefinition));
        }
    }

    private static String verifyCertifiedContainerLootCase(
        GameTestHelper helper,
        ServerPlayer player,
        CertifiedContainerLootCatalog.CaseDefinition caseDefinition
    ) throws Exception {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            return "Missing advancement: " + caseDefinition.advancementId();
        }

        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionProgressBefore = progressBefore.getCriterion(caseDefinition.criterion());
        if (criterionProgressBefore == null) {
            return "Live AdvancementProgress did not expose criterion " + caseDefinition.criterion() + " for " + describeCase(caseDefinition);
        }
        boolean criterionBeforeDone = criterionProgressBefore.isDone();
        if (criterionBeforeDone) {
            return "Target criterion was already done before openMenu for " + describeCase(caseDefinition);
        }

        BlockPos chestPos = helper.absolutePos(CHEST_POS);
        player.teleportTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 2.0D);
        if (!(helper.getLevel().getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            return "Expected ChestBlockEntity before openMenu for " + describeCase(caseDefinition);
        }
        String lootTableBefore = stringifyLootTable(chest.getLootTable());
        if (!caseDefinition.lootTableId().equals(lootTableBefore)) {
            return "Expected lootTableBefore=" + caseDefinition.lootTableId() + " but got " + lootTableBefore + " for " + describeCase(caseDefinition);
        }
        var state = helper.getLevel().getBlockState(chestPos);
        MenuProvider menuProvider = state.getMenuProvider(helper.getLevel(), chestPos);
        if (menuProvider == null) {
            return "Expected non-null chest MenuProvider for " + describeCase(caseDefinition);
        }

        String containerMenuBefore = player.containerMenu.getClass().getName();
        boolean containerMenuBeforeWasInventory = player.containerMenu == player.inventoryMenu;
        var openResult = player.openMenu(menuProvider);
        String openResultString = String.valueOf(openResult);
        if (openResult.isEmpty()) {
            return "openMenu did not report success for " + describeCase(caseDefinition)
                + " | openMenuResult=" + openResultString
                + " | menuProviderClass=" + menuProvider.getClass().getName();
        }

        String lootTableAfter = stringifyLootTable(chest.getLootTable());
        if (!"null".equals(lootTableAfter)) {
            return "Loot table was not consumed for " + describeCase(caseDefinition)
                + " | lootTableBefore=" + lootTableBefore
                + " | lootTableAfter=" + lootTableAfter
                + " | openMenuResult=" + openResultString;
        }

        String containerMenuAfter = player.containerMenu.getClass().getName();
        if (player.containerMenu == player.inventoryMenu) {
            return "Expected a real container menu for " + describeCase(caseDefinition)
                + " | containerMenuBefore=" + containerMenuBefore
                + " | containerMenuBeforeWasInventory=" + containerMenuBeforeWasInventory
                + " | containerMenuAfter=" + containerMenuAfter
                + " | openMenuResult=" + openResultString;
        }

        CriterionProgress criterionProgressAfter = player.getAdvancements().getOrStartProgress(advancement).getCriterion(caseDefinition.criterion());
        if (criterionProgressAfter == null) {
            return "Target criterion disappeared after openMenu for " + describeCase(caseDefinition);
        }
        boolean criterionAfterDone = criterionProgressAfter.isDone();
        if (!criterionAfterDone) {
            return "Target criterion did not transition false->true for " + describeCase(caseDefinition)
                + " | criterionBefore=" + criterionBeforeDone
                + " | criterionAfter=" + criterionAfterDone
                + " | openMenuResult=" + openResultString
                + " | menuProviderClass=" + menuProvider.getClass().getName();
        }

        PhaseAContainerLootExecutionEvidence.recordGreen(
            PhaseAContainerLootExecutionEvidence.projectRoot(),
            caseDefinition.advancementId().toString(),
            caseDefinition.criterion(),
            caseDefinition.lootTableId(),
            PhaseAContainerLootExecutionEvidence.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY,
            SOURCE
        );
        return null;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedContainerLootCatalog.CaseDefinition caseDefinition
    ) {
        ServerPlayer player = joinedPlayer.player();
        if (!player.connection.hasClientLoaded()) {
            throw new IllegalStateException("Expected a production-equivalent loaded player lifecycle for " + describeCase(caseDefinition));
        }
        if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
            throw new IllegalStateException("Expected a SURVIVAL joined player for " + describeCase(caseDefinition));
        }
        if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
            throw new IllegalStateException("Expected joined ServerPlayer registration for " + describeCase(caseDefinition));
        }
        if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
            throw new IllegalStateException("Expected scheduler connection registration for " + describeCase(caseDefinition));
        }
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ClientInformation clientInformation = ClientInformation.createDefault();
        UUID playerId = UUID.randomUUID();
        GameProfile profile = new GameProfile(playerId, "container-loot-test-player");
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

    private static void cleanupChest(GameTestHelper helper) {
        BlockPos chestPos = helper.absolutePos(CHEST_POS);
        helper.getLevel().removeBlockEntity(chestPos);
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.AIR.defaultBlockState());
    }

    private static String describeCase(CertifiedContainerLootCatalog.CaseDefinition caseDefinition) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion() + "@" + caseDefinition.lootTableId();
    }

    private static String stringifyLootTable(net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable) {
        return lootTable == null ? "null" : lootTable.identifier().toString();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
