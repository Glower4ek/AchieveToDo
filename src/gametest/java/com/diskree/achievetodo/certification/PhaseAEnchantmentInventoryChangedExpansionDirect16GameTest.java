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
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PhaseAEnchantmentInventoryChangedExpansionDirect16GameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET_POS = new BlockPos(1, 1, 1);
    private static final int MAX_PICKUP_ATTEMPTS = 20;

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record RuntimeCase(
        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition,
        ItemStack stack,
        List<PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.ConfiguredEnchantment> configuredEnchantments
    ) {
    }

    private record CaseRuntime(
        RuntimeCase runtimeCase,
        AdvancementHolder advancement,
        JoinedPlayer joinedPlayer,
        ItemEntity itemEntity,
        boolean criterionBeforeDone,
        String inventoryBefore,
        String itemEntityBefore,
        int attempt
    ) {
    }

    @GameTest(maxTicks = 12000)
    public void executeExpansionDirect16Cases(GameTestHelper helper) {
        List<CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition> cases =
            CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("Expansion direct16 enchantment catalog is empty");
            return;
        }
        prepareSafePlatform(helper, helper.absolutePos(TARGET_POS));
        runCase(helper, cases, 0);
    }

    private static void runCase(
        GameTestHelper helper,
        List<CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition> cases,
        int caseIndex
    ) {
        if (caseIndex >= cases.size()) {
            helper.succeed();
            return;
        }

        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition = cases.get(caseIndex);
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

            RuntimeCase runtimeCase = buildRuntimeCase(helper, caseDefinition);
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
            String inventoryBefore = describeInventory(player.getInventory());
            if (!"[]".equals(inventoryBefore)) {
                throw new IllegalStateException("Fresh joined player inventory was not empty for " + caseKey(caseDefinition) + ": " + inventoryBefore);
            }

            itemEntity = new ItemEntity(helper.getLevel(), targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, runtimeCase.stack().copy());
            itemEntity.setPickUpDelay(0);
            if (!helper.getLevel().addFreshEntity(itemEntity)) {
                throw new IllegalStateException("Failed to spawn natural pickup ItemEntity for " + caseKey(caseDefinition));
            }
            String itemEntityBefore = describeItemEntity(itemEntity);

            CaseRuntime runtime = new CaseRuntime(
                runtimeCase,
                advancement,
                joinedPlayer,
                itemEntity,
                criterionBeforeDone,
                inventoryBefore,
                itemEntityBefore,
                0
            );
            helper.runAfterDelay(1, () -> pollForPickup(helper, cases, caseIndex, runtime));
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
        List<CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition> cases,
        int caseIndex,
        CaseRuntime runtime
    ) {
        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition = runtime.runtimeCase().caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        CriterionProgress criterionAfterProgress = player.getAdvancements().getOrStartProgress(runtime.advancement()).getCriterion(caseDefinition.criterion());
        boolean criterionAfterDone = criterionAfterProgress != null && criterionAfterProgress.isDone();
        String inventoryAfter = describeInventory(player.getInventory());
        boolean entityConsumed = runtime.itemEntity() == null || !runtime.itemEntity().isAlive() || runtime.itemEntity().getItem().isEmpty();
        String itemEntityAfter = describeItemEntity(runtime.itemEntity());
        boolean matchingStackPresentAfter = inventoryContainsStack(player.getInventory(), runtime.runtimeCase().stack());
        if (criterionAfterDone && entityConsumed && matchingStackPresentAfter) {
            try {
                PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.recordGreen(
                    PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.projectRoot(),
                    new PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.DiagnosticReceipt(
                        caseDefinition.advancementId().toString(),
                        caseDefinition.criterion(),
                        caseDefinition.selectedItem(),
                        runtime.runtimeCase().configuredEnchantments(),
                        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.SOURCE,
                        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.GREEN,
                        runtime.criterionBeforeDone(),
                        criterionAfterDone,
                        entityConsumed,
                        matchingStackPresentAfter,
                        runtime.inventoryBefore(),
                        inventoryAfter,
                        runtime.itemEntityBefore(),
                        itemEntityAfter,
                        runtime.attempt() + 1
                    )
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

        if (runtime.attempt() >= MAX_PICKUP_ATTEMPTS) {
            cleanupItemEntity(runtime.itemEntity());
            cleanupJoinedServerPlayer(helper, runtime.joinedPlayer());
            helper.fail("Natural pickup proof failed for " + caseKey(caseDefinition)
                + " | criterionBefore=" + runtime.criterionBeforeDone()
                + " | criterionAfter=" + criterionAfterDone
                + " | itemEntityConsumed=" + entityConsumed
                + " | matchingStackPresentAfter=" + matchingStackPresentAfter
                + " | inventoryBefore=" + runtime.inventoryBefore()
                + " | inventoryAfter=" + inventoryAfter
                + " | itemEntityBefore=" + runtime.itemEntityBefore()
                + " | itemEntityAfter=" + itemEntityAfter);
            return;
        }

        helper.runAfterDelay(1, () -> pollForPickup(helper, cases, caseIndex, new CaseRuntime(
            runtime.runtimeCase(),
            runtime.advancement(),
            runtime.joinedPlayer(),
            runtime.itemEntity(),
            runtime.criterionBeforeDone(),
            runtime.inventoryBefore(),
            runtime.itemEntityBefore(),
            runtime.attempt() + 1
        )));
    }

    private static RuntimeCase buildRuntimeCase(
        GameTestHelper helper,
        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition
    ) {
        if (!"SUPPORTED".equals(caseDefinition.automationEligibility())) {
            throw new IllegalStateException("Refusing to execute non-SUPPORTED case " + caseKey(caseDefinition));
        }
        if (caseDefinition.enchantments().isEmpty()) {
            throw new IllegalStateException("SUPPORTED case is missing enchantment requirements: " + caseKey(caseDefinition));
        }
        if (!caseDefinition.componentKeys().isEmpty()) {
            throw new IllegalStateException("Component keys are forbidden for " + caseKey(caseDefinition));
        }
        if (!caseDefinition.allowedItems().isEmpty() && !caseDefinition.allowedItems().contains(caseDefinition.selectedItem())) {
            throw new IllegalStateException("Selected item " + caseDefinition.selectedItem() + " is not allowed for " + caseKey(caseDefinition));
        }
        ItemStack stack = new ItemStack(itemById(helper, caseDefinition.selectedItem()));
        List<PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.ConfiguredEnchantment> configured = new ArrayList<>();
        for (CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.EnchantmentRequirement requirement : caseDefinition.enchantments().stream()
            .sorted(Comparator.comparing(CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.EnchantmentRequirement::selector))
            .toList()) {
            if (!"ENCHANTMENTS".equals(requirement.storageType())) {
                throw new IllegalStateException("Expected ENCHANTMENTS storage type for " + caseKey(caseDefinition));
            }
            int selectedLevel = requirement.minLevel() != null ? requirement.minLevel() : 1;
            Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> holder = enchantmentHolder(helper, requirement.selector());
            stack.enchant(holder, selectedLevel);
            int actualLevel = stack.getEnchantments().getLevel(holder);
            if (actualLevel != selectedLevel) {
                throw new IllegalStateException("Configured enchantment level mismatch before pickup for " + caseKey(caseDefinition) + " | " + requirement.selector());
            }
            configured.add(new PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.ConfiguredEnchantment(
                requirement.selector(),
                selectedLevel,
                "ENCHANTMENTS"
            ));
        }
        return new RuntimeCase(caseDefinition, stack, List.copyOf(configured));
    }

    private static Item itemById(GameTestHelper helper, String itemId) {
        HolderGetter<Item> itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        return itemLookup.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ITEM, Identifier.parse(itemId))).value();
    }

    private static Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> enchantmentHolder(GameTestHelper helper, String selector) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(selector)));
    }

    private static boolean inventoryContainsStack(Inventory inventory, ItemStack expectedStack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == expectedStack.getItem() && expectedEnchantmentsMatch(stack, expectedStack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean expectedEnchantmentsMatch(ItemStack actualStack, ItemStack expectedStack) {
        if (!actualStack.getEnchantments().equals(expectedStack.getEnchantments())) {
            return false;
        }
        ItemEnchantments actualStored = actualStack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments expectedStored = expectedStack.get(DataComponents.STORED_ENCHANTMENTS);
        if (actualStored == null) {
            return expectedStored == null || expectedStored.isEmpty();
        }
        return actualStored.equals(expectedStored);
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition
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
        GameProfile profile = new GameProfile(playerId, "enchant-expansion-test");
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

    private static String caseKey(CertifiedEnchantmentInventoryChangedExpansionDirect16Catalog.CaseDefinition caseDefinition) {
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
