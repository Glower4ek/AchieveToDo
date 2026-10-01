package com.diskree.achievetodo.certification;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PhaseAItemTagInventoryChangedGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET_POS = new BlockPos(1, 1, 1);
    private static final int MAX_PICKUP_ATTEMPTS = 20;

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record SelectedItem(Item item, Identifier itemId, HolderSet.Named<Item> holderSet) {
    }

    private record CaseRuntime(
        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition,
        SelectedItem selectedItem,
        AdvancementHolder advancement,
        JoinedPlayer joinedPlayer,
        ItemEntity itemEntity,
        int requiredCount,
        int maxStackSize,
        boolean criterionBeforeDone,
        boolean tagMembership
    ) {
    }

    @GameTest(maxTicks = 12000)
    public void executeAllSupportedItemTagInventoryChangedCases(GameTestHelper helper) {
        List<CertifiedItemTagInventoryChangedCatalog.CaseDefinition> cases = CertifiedItemTagInventoryChangedCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("Certified ITEM_TAG_INVENTORY_CHANGED catalog is empty");
            return;
        }
        prepareSafePlatform(helper, helper.absolutePos(TARGET_POS));
        runCase(helper, cases, 0);
    }

    private static void runCase(GameTestHelper helper, List<CertifiedItemTagInventoryChangedCatalog.CaseDefinition> cases, int caseIndex) {
        if (caseIndex >= cases.size()) {
            helper.succeed();
            return;
        }

        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition = cases.get(caseIndex);
        JoinedPlayer joinedPlayer = null;
        ItemEntity itemEntity = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, caseDefinition);

            BlockPos targetPos = helper.absolutePos(TARGET_POS);
            player.teleportTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
            player.getInventory().clearContent();

            SelectedItem selectedItem = resolveSelectedItem(helper, caseDefinition);
            AdvancementHolder advancement = advancementOrThrow(helper, caseDefinition);
            AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
            CriterionProgress criterionBefore = progressBefore.getCriterion(caseDefinition.criterion());
            if (criterionBefore == null) {
                throw new IllegalStateException("Live AdvancementProgress did not expose criterion " + caseKey(caseDefinition));
            }
            boolean criterionBeforeDone = criterionBefore.isDone();
            if (criterionBeforeDone) {
                throw new IllegalStateException("Criterion already done before natural pickup for " + caseKey(caseDefinition));
            }
            int selectedItemCountBefore = countInventoryItems(player.getInventory(), selectedItem.item());
            if (selectedItemCountBefore != 0) {
                throw new IllegalStateException("Fresh joined player already possessed selected item " + selectedItem.itemId() + " for " + caseKey(caseDefinition));
            }

            int requiredCount = caseDefinition.requiredAcquisitionCount();
            if (requiredCount < 1) {
                throw new IllegalStateException("Expected requiredCount>=1 for " + caseKey(caseDefinition));
            }
            ItemStack stack = new ItemStack(selectedItem.item(), requiredCount);
            int maxStackSize = stack.getMaxStackSize();
            if (requiredCount > maxStackSize) {
                throw new IllegalStateException("Illegal pickup stack for " + caseKey(caseDefinition)
                    + " | itemTag=" + caseDefinition.itemTag()
                    + " | selectedItem=" + selectedItem.itemId()
                    + " | requiredCount=" + requiredCount
                    + " | maxStackSize=" + maxStackSize);
            }
            boolean tagMembership = selectedItem.holderSet().contains(holder(helper, selectedItem.itemId()));
            if (!tagMembership) {
                throw new IllegalStateException("Selected holder not contained in resolved tag for " + caseKey(caseDefinition));
            }

            itemEntity = new ItemEntity(helper.getLevel(), targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, stack);
            itemEntity.setPickUpDelay(0);
            if (!helper.getLevel().addFreshEntity(itemEntity)) {
                throw new IllegalStateException("Failed to spawn natural pickup ItemEntity for " + caseKey(caseDefinition));
            }

            System.out.println("ITEM_TAG_PICKUP_CASE=" + caseKey(caseDefinition)
                + " | itemTag=" + caseDefinition.itemTag()
                + " | selectedItem=" + selectedItem.itemId()
                + " | requiredCount=" + requiredCount
                + " | maxStackSize=" + maxStackSize);

            JoinedPlayer scheduledJoinedPlayer = joinedPlayer;
            ItemEntity scheduledItemEntity = itemEntity;
            CaseRuntime runtime = new CaseRuntime(
                caseDefinition,
                selectedItem,
                advancement,
                scheduledJoinedPlayer,
                scheduledItemEntity,
                requiredCount,
                maxStackSize,
                criterionBeforeDone,
                tagMembership
            );
            helper.runAfterDelay(1, () -> pollForPickup(helper, cases, caseIndex, runtime, 0));
        } catch (Throwable t) {
            cleanupItemEntity(itemEntity);
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }
    }

    private static void pollForPickup(
        GameTestHelper helper,
        List<CertifiedItemTagInventoryChangedCatalog.CaseDefinition> cases,
        int caseIndex,
        CaseRuntime runtime,
        int attempt
    ) {
        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition = runtime.caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        CriterionProgress criterionAfterProgress = player.getAdvancements().getOrStartProgress(runtime.advancement()).getCriterion(caseDefinition.criterion());
        boolean criterionAfterDone = criterionAfterProgress != null && criterionAfterProgress.isDone();
        int selectedItemCountAfter = countInventoryItems(player.getInventory(), runtime.selectedItem().item());
        int maxMatchingStackAfter = maxMatchingStackCount(player.getInventory(), runtime.selectedItem().item());
        boolean entityConsumed = runtime.itemEntity() == null || !runtime.itemEntity().isAlive() || runtime.itemEntity().getItem().isEmpty();

        if (criterionAfterDone
            && selectedItemCountAfter >= runtime.requiredCount()
            && maxMatchingStackAfter >= runtime.requiredCount()
            && entityConsumed
            && runtime.tagMembership()) {
            try {
                PhaseAItemTagInventoryChangedExecutionEvidence.recordGreen(
                    PhaseAItemTagInventoryChangedExecutionEvidence.projectRoot(),
                    caseDefinition.advancementId().toString(),
                    caseDefinition.criterion(),
                    caseDefinition.itemTag(),
                    runtime.selectedItem().itemId().toString(),
                    runtime.requiredCount(),
                    PhaseAItemTagInventoryChangedExecutionEvidence.SOURCE
                );
            } catch (Exception e) {
                cleanupItemEntity(runtime.itemEntity());
                cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
                helper.fail("Failed to record GREEN runtime evidence for " + caseKey(caseDefinition) + ": " + e.getMessage());
                return;
            }
            cleanupItemEntity(runtime.itemEntity());
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            helper.runAfterDelay(1, () -> runCase(helper, cases, caseIndex + 1));
            return;
        }

        if (attempt >= MAX_PICKUP_ATTEMPTS) {
            String failure = failureDetails(
                caseDefinition,
                runtime,
                player.getInventory(),
                criterionAfterDone,
                selectedItemCountAfter,
                maxMatchingStackAfter,
                runtime.itemEntity()
            );
            cleanupItemEntity(runtime.itemEntity());
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            helper.fail(failure);
            return;
        }

        helper.runAfterDelay(1, () -> pollForPickup(helper, cases, caseIndex, runtime, attempt + 1));
    }

    private static String failureDetails(
        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition,
        CaseRuntime runtime,
        Inventory inventory,
        boolean criterionAfterDone,
        int selectedItemCountAfter,
        int maxMatchingStackAfter,
        ItemEntity itemEntity
    ) {
        return "Natural pickup proof failed for " + caseKey(caseDefinition)
            + " | itemTag=" + caseDefinition.itemTag()
            + " | selectedItem=" + runtime.selectedItem().itemId()
            + " | requiredCount=" + runtime.requiredCount()
            + " | maxStackSize=" + runtime.maxStackSize()
            + " | criterionBefore=" + runtime.criterionBeforeDone()
            + " | criterionAfter=" + criterionAfterDone
            + " | selectedItemTotalCountAfter=" + selectedItemCountAfter
            + " | maxMatchingStackAfter=" + maxMatchingStackAfter
            + " | inventory=" + describeInventory(inventory)
            + " | itemEntity=" + describeItemEntity(itemEntity);
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static SelectedItem resolveSelectedItem(GameTestHelper helper, CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition) {
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
        if (!holderSet.contains(selectedHolder)) {
            throw new IllegalStateException("Selected item holder was not contained in resolved tag " + caseDefinition.itemTag());
        }
        return new SelectedItem(selectedHolder.value(), selectedHolder.unwrapKey().orElseThrow().identifier(), holderSet);
    }

    private static Holder.Reference<Item> holder(GameTestHelper helper, Identifier itemId) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM)
            .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ITEM, itemId));
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition
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
        GameProfile profile = new GameProfile(playerId, "item-tag-pickup-test-player");
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

    private static void cleanupItemEntity(ItemEntity itemEntity) {
        if (itemEntity != null && itemEntity.isAlive()) {
            itemEntity.discard();
        }
    }

    private static void prepareSafePlatform(GameTestHelper helper, BlockPos targetPos) {
        helper.getLevel().setBlockAndUpdate(targetPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(targetPos.above(), Blocks.AIR.defaultBlockState());
    }

    private static int countInventoryItems(Inventory inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int maxMatchingStackCount(Inventory inventory, Item item) {
        int maxCount = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                maxCount = Math.max(maxCount, stack.getCount());
            }
        }
        return maxCount;
    }

    private static String describeInventory(Inventory inventory) {
        List<String> entries = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                entries.add(slot + "=" + stack);
            }
        }
        return entries.isEmpty() ? "[]" : entries.toString();
    }

    private static String describeItemEntity(ItemEntity itemEntity) {
        if (itemEntity == null) {
            return "null";
        }
        return "alive=" + itemEntity.isAlive()
            + ",stack=" + itemEntity.getItem()
            + ",pickupDelay=" + itemEntity.hasPickUpDelay()
            + ",pos=" + formatPosition(itemEntity);
    }

    private static String caseKey(CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion();
    }

    private static String formatPosition(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f", entity.getX(), entity.getY(), entity.getZ());
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
