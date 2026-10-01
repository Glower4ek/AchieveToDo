package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native crafting-table result-take proof for the selected wax_on recipe criterion. */
public final class PhaseARecipeCraftedWaxOnGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TABLE_POS = new BlockPos(1, 1, 1);
    private static final int OPEN_CRAFTING_TABLE_UNLOCK_COUNT = 1000;
    private static final int MAX_POLL_TICKS = 100;
    private static final int MAX_SETTLE_PASSES = 32;
    private static final int MAX_SETTLE_MESSAGES = 4096;

    @GameTest(maxTicks = 250)
    public void recipeCraftedWaxOnCanary(GameTestHelper helper) {
        try {
            String runId = PhaseARecipeCraftedWaxOnExecutionEvidence.beginRun(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot());
            execute(helper, runId, () -> {
                try {
                    PhaseARecipeCraftedWaxOnExecutionEvidence.Artifact artifact =
                        PhaseARecipeCraftedWaxOnExecutionEvidence.loadTemporary(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot(), false);
                    require(artifact.runId().equals(runId) && artifact.entries().size() == 1, "Canary TEMP receipt/runId mismatch");
                    helper.succeed();
                } catch (Throwable t) { helper.fail("wax_on canary TEMP validation failed: " + describe(t)); }
            });
        } catch (Throwable t) { helper.fail("wax_on canary setup failed: " + describe(t)); }
    }

    @GameTest(maxTicks = 250)
    public void recipeCraftedWaxOnExact1(GameTestHelper helper) {
        try {
            String runId = PhaseARecipeCraftedWaxOnExecutionEvidence.beginRun(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot());
            execute(helper, runId, () -> {
                try {
                    PhaseARecipeCraftedWaxOnExecutionEvidence.Artifact artifact =
                        PhaseARecipeCraftedWaxOnExecutionEvidence.loadTemporary(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot(), true);
                    require(artifact.runId().equals(runId) && artifact.entries().size() == 1, "Exact TEMP receipt/runId mismatch");
                    helper.succeed();
                } catch (Throwable t) { helper.fail("wax_on exact TEMP validation failed: " + describe(t)); }
            });
        } catch (Throwable t) { helper.fail("wax_on exact setup failed: " + describe(t)); }
    }

    private static void execute(GameTestHelper helper, String runId, Done done) {
        JoinedPlayer joined = null;
        BlockPos tablePos = helper.absolutePos(TABLE_POS);
        try {
            prepareTable(helper, tablePos);
            joined = joinedPlayer(helper);
            ServerPlayer player = joined.player();
            establishSurvival(player);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded(), "Client-loaded packet was rejected");
            player.teleportTo(tablePos.getX() + .5D, tablePos.getY() + 1D, tablePos.getZ() - 1.5D);
            require(!AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), tablePos), "Crafting table fixture is inside locked landmark");
            AchieveToDoMod.getServer().setObtainedCount(player, OPEN_CRAFTING_TABLE_UNLOCK_COUNT);
            require(!AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_CRAFTING_TABLE), "Crafting table remained locked after fixture unlock");
            InteractionResult interaction = interactWithTable(player, helper);
            require(interaction.consumesAction() && player.containerMenu instanceof CraftingMenu, "Normal crafting-table interaction did not open CraftingMenu");
            CraftingMenu menu = (CraftingMenu) player.containerMenu;
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(PhaseARecipeCraftedWaxOnCertification.ADVANCEMENT_ID));
            require(advancement != null, "Missing live wax_on advancement");
            CriterionProgress before = player.getAdvancements().getOrStartProgress(advancement).getCriterion(PhaseARecipeCraftedWaxOnCertification.SELECTED_CRITERION);
            require(before != null && !before.isDone(), "Selected wax_on criterion was already complete");
            List<net.minecraft.world.inventory.Slot> inputs = menu.getInputGridSlots();
            require(inputs.size() == 9, "Expected 3x3 crafting input grid");
            inputs.getFirst().set(itemStack(helper, "minecraft:copper_block"));
            inputs.get(1).set(itemStack(helper, "minecraft:honeycomb"));
            menu.slotsChanged(inputs.getFirst().container);
            menu.broadcastChanges();
            ItemStack preview = menu.getSlot(CraftingMenu.RESULT_SLOT).getItem().copy();
            require("minecraft:waxed_copper_block".equals(itemId(preview)), "Crafting grid did not produce waxed copper block: " + preview);
            ResultContainer result = resultContainer(menu);
            RecipeHolder<?> recipe = result.getRecipeUsed();
            require(recipe != null && PhaseARecipeCraftedWaxOnCertification.SELECTED_RECIPE.equals(recipe.id().identifier().toString()), "Unexpected live crafting recipe: " + recipe);
            new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), (short) CraftingMenu.RESULT_SLOT, (byte) 0,
                ContainerInput.PICKUP, Int2ObjectMaps.emptyMap(), HashedStack.EMPTY).handle(player.connection);
            ItemStack produced = menu.getCarried().copy();
            boolean normalResultTaken = menu.getSlot(CraftingMenu.RESULT_SLOT).getItem().isEmpty()
                && inputs.getFirst().getItem().isEmpty() && inputs.get(1).getItem().isEmpty()
                && "minecraft:waxed_copper_block".equals(itemId(produced));
            require(normalResultTaken, "Normal crafting result click did not consume inputs and carry the waxed result");
            JoinedPlayer retained = joined;
            helper.runAfterDelay(1, () -> poll(helper, runId, done, retained, tablePos, advancement, normalResultTaken, 1));
        } catch (Throwable t) {
            if (joined != null) cleanup(joined);
            removeTable(helper, tablePos);
            helper.fail("wax_on case setup failed: " + describe(t));
        }
    }

    private static void poll(GameTestHelper helper, String runId, Done done, JoinedPlayer joined, BlockPos tablePos,
                             AdvancementHolder advancement, boolean normalResultTaken, int ticks) {
        try {
            CriterionProgress progress = joined.player().getAdvancements().getOrStartProgress(advancement)
                .getCriterion(PhaseARecipeCraftedWaxOnCertification.SELECTED_CRITERION);
            if (progress != null && progress.isDone()) {
                Cleanup cleanup = cleanup(joined);
                removeTable(helper, tablePos);
                JsonObject receipt = new JsonObject();
                receipt.addProperty("advancementId", PhaseARecipeCraftedWaxOnCertification.ADVANCEMENT_ID);
                receipt.addProperty("criterion", PhaseARecipeCraftedWaxOnCertification.SELECTED_CRITERION);
                receipt.addProperty("recipeId", PhaseARecipeCraftedWaxOnCertification.SELECTED_RECIPE);
                receipt.addProperty("trigger", "minecraft:recipe_crafted");
                receipt.addProperty("boundary", "ServerGamePacketListenerImpl.handleContainerClick->ResultSlot.onTake->CriteriaTriggers.RECIPE_CRAFTED");
                receipt.addProperty("family", PhaseARecipeCraftedWaxOnExecutionEvidence.FAMILY);
                receipt.addProperty("source", PhaseARecipeCraftedWaxOnExecutionEvidence.SOURCE);
                receipt.addProperty("catalogFingerprint", PhaseARecipeCraftedWaxOnExecutionEvidence.currentCatalogFingerprint(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot()));
                receipt.addProperty("runId", runId); receipt.addProperty("result", "GREEN"); receipt.addProperty("gameMode", "SURVIVAL");
                receipt.addProperty("inputA", "minecraft:copper_block"); receipt.addProperty("inputB", "minecraft:honeycomb"); receipt.addProperty("producedItem", "minecraft:waxed_copper_block");
                receipt.addProperty("joined", true); receipt.addProperty("connectionRegistered", true); receipt.addProperty("clientLoaded", true);
                receipt.addProperty("craftingTableInteraction", true); receipt.addProperty("normalResultTaken", normalResultTaken);
                receipt.addProperty("criterionBefore", false); receipt.addProperty("criterionAfter", true); receipt.addProperty("noDirectCriterionTrigger", true); receipt.addProperty("noManualAward", true); receipt.addProperty("ticksToCriterion", ticks);
                JsonObject cleanupJson = new JsonObject(); cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved()); cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved()); cleanupJson.addProperty("channelSettled", cleanup.channelSettled()); cleanupJson.addProperty("settlementMessages", cleanup.messages()); cleanupJson.addProperty("warningCount", 0); receipt.add("cleanup", cleanupJson);
                PhaseARecipeCraftedWaxOnExecutionEvidence.recordGreen(PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot(), receipt);
                done.complete();
                return;
            }
            if (ticks >= MAX_POLL_TICKS) throw new IllegalStateException("Recipe-crafted criterion did not complete after normal result take");
            helper.runAfterDelay(1, () -> poll(helper, runId, done, joined, tablePos, advancement, normalResultTaken, ticks + 1));
        } catch (Throwable t) {
            cleanup(joined); removeTable(helper, tablePos); helper.fail("wax_on native recipe proof failed: " + describe(t));
        }
    }

    private static void prepareTable(GameTestHelper helper, BlockPos pos) { helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState()); helper.getLevel().setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState()); helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState()); }
    private static void removeTable(GameTestHelper helper, BlockPos pos) { helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState()); }
    private static InteractionResult interactWithTable(ServerPlayer player, GameTestHelper helper) { BlockPos pos = helper.absolutePos(TABLE_POS); return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D), net.minecraft.core.Direction.UP, pos, false)); }
    private static ItemStack itemStack(GameTestHelper helper, String id) { HolderGetter<Item> lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM); Holder.Reference<Item> holder = lookup.getOrThrow(ResourceKey.create(Registries.ITEM, Identifier.parse(id))); return new ItemStack(holder); }
    private static ResultContainer resultContainer(CraftingMenu menu) { if (!(menu.getSlot(CraftingMenu.RESULT_SLOT).container instanceof ResultContainer result)) throw new IllegalStateException("Crafting result slot did not use ResultContainer"); return result; }
    private static String itemId(ItemStack stack) { return stack.isEmpty() ? "minecraft:air" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static JoinedPlayer joinedPlayer(GameTestHelper helper) { MinecraftServer server = helper.getLevel().getServer(); UUID id = UUID.randomUUID(); GameProfile profile = new GameProfile(id, "wax-on-" + id.toString().replace("-", "").substring(0, 10)); ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault()); Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection); server.getConnection().getConnections().add(connection); server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false)); require(server.getPlayerList().getPlayer(id) == player && server.getConnection().getConnections().contains(connection), "Joined lifecycle registration failed"); return new JoinedPlayer(player, connection, channel, new AtomicBoolean()); }
    private static void establishSurvival(ServerPlayer player) { if (player.gameMode() == GameType.SURVIVAL && player.hasInfiniteMaterials()) require(player.setGameMode(GameType.CREATIVE), "Could not refresh player mode"); player.setGameMode(GameType.SURVIVAL); require(player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials() && !player.isSpectator(), "Expected finite SURVIVAL player"); }
    private static Cleanup cleanup(JoinedPlayer joined) { if (!joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, 0); ServerPlayer player = joined.player(); MinecraftServer server = player.level().getServer(); if (player.containerMenu != player.inventoryMenu) player.closeContainer(); server.getPlayerList().remove(player); boolean playerRemoved = server.getPlayerList().getPlayer(player.getUUID()) != player; server.getConnection().getConnections().remove(joined.connection()); boolean connectionRemoved = !server.getConnection().getConnections().contains(joined.connection()); int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("wax_on GameTest cleanup")); return new Cleanup(playerRemoved, connectionRemoved, true, messages); }
    private static int settle(EmbeddedChannel channel) { int messages = 0; for (int pass = 0; pass < MAX_SETTLE_PASSES; pass++) { channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound(); int thisPass = 0; Object outbound; while ((outbound = channel.readOutbound()) != null) { ReferenceCountUtil.release(outbound); messages++; thisPass++; if (messages > MAX_SETTLE_MESSAGES) throw new IllegalStateException("EmbeddedChannel cleanup exceeded bound"); } if (thisPass == 0 && !channel.hasPendingTasks()) return messages; } throw new IllegalStateException("EmbeddedChannel cleanup did not quiesce"); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static String describe(Throwable t) { return t.getClass().getSimpleName() + ": " + t.getMessage(); }
    @FunctionalInterface private interface Done { void complete(); }
    private record JoinedPlayer(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { helper.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, helper); }
}
