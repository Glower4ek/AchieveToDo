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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PhaseAEnchantmentInventoryChangedGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET_POS = new BlockPos(1, 1, 1);
    private static final int MAX_PICKUP_ATTEMPTS = 20;
    private static final Map<String, String> DETERMINISTIC_BASE_ITEMS = Map.ofEntries(
        Map.entry("blazeandcave:enchanting/a_rather_pointy_fence_post#rather_pointy_fence_post", "minecraft:wooden_sword"),
        Map.entry("blazeandcave:enchanting/boomerang#power", "minecraft:trident"),
        Map.entry("blazeandcave:enchanting/like_a_cat#feather_falling", "minecraft:iron_boots"),
        Map.entry("blazeandcave:enchanting/like_a_ninja#swift_sneak_book", "minecraft:enchanted_book"),
        Map.entry("blazeandcave:enchanting/mace_windu#mace_windu", "minecraft:mace"),
        Map.entry("blazeandcave:enchanting/master_fisher#perfect_rod", "minecraft:fishing_rod"),
        Map.entry("blazeandcave:enchanting/newtons_flaming_laser_sword#newton", "minecraft:diamond_sword"),
        Map.entry("blazeandcave:enchanting/to_infinity_and_beyond#infinity", "minecraft:bow"),
        Map.entry("blazeandcave:enchanting/unbreakable#mending", "minecraft:diamond_pickaxe"),
        Map.entry("blazeandcave:enchanting/zeus#power", "minecraft:trident"),
        Map.entry("blazeandcave:mining/the_mistake#the_mistake", "minecraft:golden_helmet")
    );

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record RuntimeCase(
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition,
        ItemStack stack,
        String selectedItem,
        List<PhaseAEnchantmentInventoryChangedExecutionEvidence.ConfiguredEnchantment> configuredEnchantments,
        String componentSemantics,
        String proof
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
    public void executeAllSupportedEnchantmentInventoryChangedCases(GameTestHelper helper) {
        List<CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> cases = CertifiedEnchantmentInventoryChangedCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("Certified ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED catalog is empty");
            return;
        }
        prepareSafePlatform(helper, helper.absolutePos(TARGET_POS));
        runCase(helper, cases, 0);
    }

    private static void runCase(GameTestHelper helper, List<CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> cases, int caseIndex) {
        if (caseIndex >= cases.size()) {
            helper.succeed();
            return;
        }

        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition = cases.get(caseIndex);
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

            System.out.println("ENCHANTMENT_PICKUP_CASE=" + caseKey(caseDefinition)
                + " | selectedItem=" + runtimeCase.selectedItem()
                + " | componentSemantics=" + runtimeCase.componentSemantics()
                + " | proof=" + runtimeCase.proof());

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

    private static void pollForPickup(GameTestHelper helper, List<CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition> cases, int caseIndex, CaseRuntime runtime) {
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition = runtime.runtimeCase().caseDefinition();
        ServerPlayer player = runtime.joinedPlayer().player();
        CriterionProgress criterionAfterProgress = player.getAdvancements().getOrStartProgress(runtime.advancement()).getCriterion(caseDefinition.criterion());
        boolean criterionAfterDone = criterionAfterProgress != null && criterionAfterProgress.isDone();
        String inventoryAfter = describeInventory(player.getInventory());
        boolean entityConsumed = runtime.itemEntity() == null || !runtime.itemEntity().isAlive() || runtime.itemEntity().getItem().isEmpty();
        String itemEntityAfter = describeItemEntity(runtime.itemEntity());
        if (criterionAfterDone
            && entityConsumed
            && inventoryContainsStack(player.getInventory(), runtime.runtimeCase().stack())) {
            try {
                PhaseAEnchantmentInventoryChangedExecutionEvidence.recordGreen(
                    PhaseAEnchantmentInventoryChangedExecutionEvidence.projectRoot(),
                    new PhaseAEnchantmentInventoryChangedExecutionEvidence.DiagnosticReceipt(
                        caseDefinition.advancementId().toString(),
                        caseDefinition.criterion(),
                        runtime.runtimeCase().selectedItem(),
                        runtime.runtimeCase().configuredEnchantments(),
                        PhaseAEnchantmentInventoryChangedExecutionEvidence.SOURCE,
                        PhaseAEnchantmentInventoryChangedExecutionEvidence.GREEN,
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
                + " | componentSemantics=" + runtime.runtimeCase().componentSemantics()
                + " | proof=" + runtime.runtimeCase().proof()
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

    private static RuntimeCase buildRuntimeCase(GameTestHelper helper, CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition) {
        String receiptKey = caseKey(caseDefinition);
        if (!"SUPPORTED".equals(caseDefinition.automationEligibility())) {
            throw new IllegalStateException("Refusing to execute non-SUPPORTED case " + receiptKey);
        }
        if (caseDefinition.enchantments().isEmpty()) {
            throw new IllegalStateException("SUPPORTED case is missing enchantment requirements: " + receiptKey);
        }
        String itemId = deterministicItemId(caseDefinition);
        verifyAllowedItem(caseDefinition, itemId);

        boolean storedOnly = caseDefinition.enchantments().stream()
            .allMatch(requirement -> "STORED_ENCHANTMENTS".equals(requirement.storageType()));
        boolean normalOnly = caseDefinition.enchantments().stream()
            .allMatch(requirement -> "ENCHANTMENTS".equals(requirement.storageType()));
        if (!storedOnly && !normalOnly) {
            throw new IllegalStateException("Mixed storage types are unsupported for runtime construction: " + receiptKey);
        }
        if (storedOnly) {
            return buildStoredRuntimeCase(helper, caseDefinition, itemId);
        }
        return buildNormalRuntimeCase(helper, caseDefinition, itemId);
    }

    private static RuntimeCase buildNormalRuntimeCase(
        GameTestHelper helper,
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition,
        String itemId
    ) {
        Item item = itemById(helper, itemId);
        ItemStack stack = new ItemStack(item);
        List<PhaseAEnchantmentInventoryChangedExecutionEvidence.ConfiguredEnchantment> configured = new ArrayList<>();
        List<String> actualLevels = new ArrayList<>();
        List<String> selectors = new ArrayList<>();
        for (CertifiedEnchantmentInventoryChangedCatalog.EnchantmentRequirement requirement : caseDefinition.enchantments().stream()
            .sorted(Comparator.comparing(CertifiedEnchantmentInventoryChangedCatalog.EnchantmentRequirement::selector))
            .toList()) {
            if (!"ENCHANTMENTS".equals(requirement.storageType())) {
                throw new IllegalStateException("Expected ENCHANTMENTS storage type for " + caseKey(caseDefinition) + " but got " + requirement.storageType());
            }
            int selectedLevel = selectedLevel(requirement, caseDefinition);
            Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> holder = enchantmentHolder(helper, requirement.selector());
            stack.enchant(holder, selectedLevel);
            int actualLevel = stack.getEnchantments().getLevel(holder);
            if (actualLevel != selectedLevel) {
                throw new IllegalStateException("Configured enchantment level mismatch before pickup for " + caseKey(caseDefinition) + " | " + requirement.selector());
            }
            configured.add(new PhaseAEnchantmentInventoryChangedExecutionEvidence.ConfiguredEnchantment(requirement.selector(), selectedLevel, "ENCHANTMENTS"));
            selectors.add(requirement.selector());
            actualLevels.add(requirement.selector() + "=" + actualLevel);
        }
        return new RuntimeCase(
            caseDefinition,
            stack,
            itemId,
            List.copyOf(configured),
            "ENCHANTMENTS",
            "itemConstraint=true,selectors=" + String.join("|", selectors) + ",actualLevels=" + actualLevels
        );
    }

    private static RuntimeCase buildStoredRuntimeCase(
        GameTestHelper helper,
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition,
        String itemId
    ) {
        if (!"minecraft:enchanted_book".equals(itemId)) {
            throw new IllegalStateException("STORED_ENCHANTMENTS runtime currently requires enchanted_book for " + caseKey(caseDefinition));
        }
        if (caseDefinition.enchantments().size() != 1) {
            throw new IllegalStateException("STORED_ENCHANTMENTS runtime expected exactly one requirement for " + caseKey(caseDefinition));
        }
        CertifiedEnchantmentInventoryChangedCatalog.EnchantmentRequirement requirement = caseDefinition.enchantments().getFirst();
        if (!"STORED_ENCHANTMENTS".equals(requirement.storageType())) {
            throw new IllegalStateException("Expected STORED_ENCHANTMENTS storage type for " + caseKey(caseDefinition));
        }
        int selectedLevel = selectedLevel(requirement, caseDefinition);
        Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> holder = enchantmentHolder(helper, requirement.selector());
        ItemStack stack = EnchantmentHelper.createBook(new EnchantmentInstance(holder, selectedLevel));
        ItemEnchantments storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments normalEnchantments = stack.get(DataComponents.ENCHANTMENTS);
        int storedLevel = storedEnchantments == null ? 0 : storedEnchantments.getLevel(holder);
        int normalLevel = normalEnchantments == null ? 0 : normalEnchantments.getLevel(holder);
        if (storedLevel != selectedLevel) {
            throw new IllegalStateException("Expected STORED_ENCHANTMENTS " + requirement.selector() + "=" + selectedLevel + " before pickup for " + caseKey(caseDefinition));
        }
        if (normalLevel != 0) {
            throw new IllegalStateException("Expected ENCHANTMENTS " + requirement.selector() + "=0 on enchanted book before pickup for " + caseKey(caseDefinition));
        }
        return new RuntimeCase(
            caseDefinition,
            stack,
            itemId,
            List.of(new PhaseAEnchantmentInventoryChangedExecutionEvidence.ConfiguredEnchantment(requirement.selector(), selectedLevel, "STORED_ENCHANTMENTS")),
            "STORED_ENCHANTMENTS",
            "itemConstraint=true,selector=" + requirement.selector() + ",storedLevel=" + storedLevel + ",normalLevel=" + normalLevel
        );
    }

    private static String deterministicItemId(CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition) {
        String receiptKey = caseKey(caseDefinition);
        String itemId = DETERMINISTIC_BASE_ITEMS.get(receiptKey);
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalStateException("Missing deterministic base-item mapping for " + receiptKey);
        }
        return itemId;
    }

    private static int selectedLevel(
        CertifiedEnchantmentInventoryChangedCatalog.EnchantmentRequirement requirement,
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition
    ) {
        int selectedLevel = requirement.minLevel() != null ? requirement.minLevel() : 1;
        if (requirement.minLevel() != null && selectedLevel < requirement.minLevel()) {
            throw new IllegalStateException("Selected level fell below minLevel for " + caseKey(caseDefinition) + " | " + requirement.selector());
        }
        if (requirement.maxLevel() != null && selectedLevel > requirement.maxLevel()) {
            throw new IllegalStateException("Selected level exceeded maxLevel for " + caseKey(caseDefinition) + " | " + requirement.selector());
        }
        return selectedLevel;
    }

    private static Item itemById(GameTestHelper helper, String itemId) {
        HolderGetter<Item> itemLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM);
        return itemLookup.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ITEM, Identifier.parse(itemId))).value();
    }

    private static Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> enchantmentHolder(GameTestHelper helper, String selector) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(selector)));
    }

    private static void verifyAllowedItem(CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition, String itemId) {
        if (!caseDefinition.allowedItems().isEmpty() && !caseDefinition.allowedItems().contains(itemId)) {
            throw new IllegalStateException("Selected item " + itemId + " is not allowed for " + caseKey(caseDefinition));
        }
    }

    private static boolean inventoryContainsStack(Inventory inventory, ItemStack expectedStack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()
                && stack.getItem() == expectedStack.getItem()
                && expectedEnchantmentsMatch(stack, expectedStack)) {
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

    private static AdvancementHolder advancementOrThrow(GameTestHelper helper, CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition
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
        GameProfile profile = new GameProfile(playerId, "enchantment-pickup-test");
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

    private static String caseKey(CertifiedEnchantmentInventoryChangedCatalog.CaseDefinition caseDefinition) {
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
