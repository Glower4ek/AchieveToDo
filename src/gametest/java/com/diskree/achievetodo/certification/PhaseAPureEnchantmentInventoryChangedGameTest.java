package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.List;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Sequential native item pickups and one inventory equipment click. */
public final class PhaseAPureEnchantmentInventoryChangedGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);
    @GameTest(maxTicks = 1800)
    public void pureEnchantmentInventoryChangedCanary(GameTestHelper h) { run(h, false); }
    @GameTest(maxTicks = 1800)
    public void pureEnchantmentInventoryChangedExact211(GameTestHelper h) { run(h, true); }
    private static void run(GameTestHelper h, boolean exact) {
        try {
            var root = PhaseAPureEnchantmentInventoryChangedExecutionEvidence.root();
            String run = PhaseAPureEnchantmentInventoryChangedExecutionEvidence.begin(root, exact);
            var pos = h.absolutePos(TARGET);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) h.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            runCase(h, PhaseAPureEnchantmentInventoryChangedExecutionEvidence.selected(), 0, run, exact);
        } catch (Throwable failure) { h.fail("pure enchantment start failed: " + describe(failure)); }
    }
    private static void runCase(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact) {
        if (index == cases.size()) {
            try {
                var artifact = PhaseAPureEnchantmentInventoryChangedEvidenceValidation.load(PhaseAPureEnchantmentInventoryChangedExecutionEvidence.root(), PhaseAPureEnchantmentInventoryChangedEvidenceValidation.TEMP, exact, true);
                require(artifact.get("runId").getAsString().equals(run), "run ownership changed");
                System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=PURE_ENCHANTMENT_INVENTORY_CHANGED runId=" + run + " entries=" + cases.size()); h.succeed();
            } catch (Throwable failure) { h.fail("pure enchantment exact-set audit failed: " + describe(failure)); }
            return;
        }
        Context c = new Context(cases.get(index));
        try {
            c.joined = join(h); ServerPlayer player = c.joined.player(); player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && !player.hasInfiniteMaterials(), "joined finite loaded lifecycle failed");
            var server = h.getLevel().getServer(); var settings = server.getScoreboard().getObjective("bac_settings");
            require(settings != null, "reward settings missing"); server.getScoreboard().getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("reward"), settings).set(0);
            var pos = h.absolutePos(TARGET); player.teleportTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D); player.getInventory().clearContent();
            c.advancement = server.getAdvancements().get(Identifier.parse(c.row.get("advancementId").getAsString()));
            require(c.advancement != null && !done(player, c), "criterion missing or pre-complete");
            c.stack = construct(h, c.row); c.before = false;
            boolean equip = c.row.get("action").getAsString().equals("EQUIP_HEAD");
            if (equip) {
                require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "head fixture not empty");
                player.getInventory().setItem(0, c.stack.copy()); require(!done(player, c), "hotbar fixture completed equipment criterion");
                var ability = com.diskree.achievetodo.ability.AbilityType.findEquipmentEquipAbility(c.stack.getItem());
                require(ability == null, "unexpected production ability gate on carved pumpkin");
                InventoryMenu menu = player.inventoryMenu; menu.broadcastChanges();
                new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), (short) InventoryMenu.USE_ROW_SLOT_START, (byte) 0,
                    ContainerInput.QUICK_MOVE, Int2ObjectMaps.emptyMap(), HashedStack.EMPTY).handle(player.connection);
                c.packet = true;
            } else {
                c.entity = new ItemEntity(h.getLevel(), pos.getX() + .5D, pos.getY() + .1D, pos.getZ() + .5D, c.stack.copy()); c.entity.setPickUpDelay(0);
                require(h.getLevel().addFreshEntity(c.entity), "item entity spawn failed");
            }
            h.runAfterDelay(1, () -> poll(h, cases, index, run, exact, c, 1));
        } catch (Throwable failure) { fail(h, c, "setup", failure); }
    }
    private static ItemStack construct(GameTestHelper h, JsonObject row) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        boolean stored = row.get("storage").getAsString().equals("STORED_ENCHANTMENTS");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(row.get("selectedItem").getAsString())));
        for (var entry : row.getAsJsonObject("configuredEnchantments").entrySet()) {
            var holder = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(entry.getKey()))); int level = entry.getValue().getAsInt();
            require(level > 0 && level <= holder.value().getMaxLevel(), "non-gameplay enchantment level");
            if (stored) { require(row.getAsJsonObject("configuredEnchantments").size() == 1, "unexpected compound stored case"); stack = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level)); }
            else { require(holder.value().canEnchant(stack), "unsupported direct enchantment witness item"); stack.enchant(holder, level); }
        }
        String name = row.get("customName").getAsString(); if (!name.isEmpty()) stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        require(observed(h, stack, row).equals(row.getAsJsonObject("configuredEnchantments")), "constructed live components differ");
        require(!stored || stack.getEnchantments().isEmpty(), "stored-book fixture contains direct enchantments");
        return stack;
    }
    private static JsonObject observed(GameTestHelper h, ItemStack stack, JsonObject row) {
        var component = row.get("storage").getAsString().equals("STORED_ENCHANTMENTS") ? stack.get(DataComponents.STORED_ENCHANTMENTS) : stack.get(DataComponents.ENCHANTMENTS);
        require(component != null, "expected enchantment component missing"); JsonObject observed = new JsonObject();
        for (var entry : component.entrySet()) observed.addProperty(entry.getKey().unwrapKey().orElseThrow().identifier().toString(), entry.getIntValue());
        return observed;
    }
    private static boolean done(ServerPlayer player, Context c) {
        var criterion = player.getAdvancements().getOrStartProgress(c.advancement).getCriterion(c.row.get("criterion").getAsString());
        require(criterion != null, "missing live criterion"); return criterion.isDone();
    }
    private static void poll(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact, Context c, int ticks) {
        try {
            var player = c.joined.player(); boolean equip = c.row.get("action").getAsString().equals("EQUIP_HEAD");
            ItemStack live = ItemStack.EMPTY;
            if (equip) live = player.getItemBySlot(EquipmentSlot.HEAD);
            else for (int i = 0; i < player.getInventory().getContainerSize(); i++) { var candidate = player.getInventory().getItem(i); if (ItemStack.isSameItemSameComponents(candidate, c.stack)) { live = candidate; break; } }
            boolean event = equip ? c.packet && player.getInventory().getItem(0).isEmpty() : !c.entity.isAlive() || c.entity.getItem().isEmpty();
            if (done(player, c) && event && !live.isEmpty()) { finish(h, cases, index, run, exact, c, live, ticks); return; }
            if (ticks >= 80) throw new IllegalStateException("native inventory event timed out: " + PhaseAPureEnchantmentInventoryChangedCertification.key(c.row) + " event=" + event + " head=" + player.getItemBySlot(EquipmentSlot.HEAD));
            h.runAfterDelay(1, () -> poll(h, cases, index, run, exact, c, ticks + 1));
        } catch (Throwable failure) { fail(h, c, "event", failure); }
    }
    private static void finish(GameTestHelper h, List<JsonObject> cases, int index, String run, boolean exact, Context c, ItemStack live, int ticks) {
        try {
            var player = c.joined.player(); boolean equip = c.row.get("action").getAsString().equals("EQUIP_HEAD");
            require(ItemStack.isSameItemSameComponents(c.stack, live) && live.getCount() == 1, "finite live witness mismatch");
            JsonObject receipt = new JsonObject();
            for (String field : new String[]{"advancementId", "criterion", "requirementGroup", "selectedItem", "action", "storage"}) receipt.add(field, c.row.get(field).deepCopy());
            receipt.addProperty("family", PhaseAPureEnchantmentInventoryChangedCertification.FAMILY); receipt.addProperty("source", PhaseAPureEnchantmentInventoryChangedCertification.SOURCE);
            receipt.addProperty("runId", run); receipt.addProperty("catalogFingerprint", PhaseAPureEnchantmentInventoryChangedEvidenceValidation.fingerprint(PhaseAPureEnchantmentInventoryChangedExecutionEvidence.root()));
            receipt.addProperty("result", "GREEN"); receipt.addProperty("gameMode", player.gameMode().name()); receipt.addProperty("playerUuid", player.getUUID().toString());
            receipt.addProperty("joined", h.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
            receipt.addProperty("connectionRegistered", h.getLevel().getServer().getConnection().getConnections().contains(c.joined.connection())); receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
            receipt.addProperty("trigger", "minecraft:inventory_changed"); receipt.addProperty("boundary", equip ? PhaseAPureEnchantmentInventoryChangedCertification.EQUIP : PhaseAPureEnchantmentInventoryChangedCertification.PICKUP);
            receipt.add("observedEnchantments", observed(h, live, c.row)); var name = live.get(DataComponents.CUSTOM_NAME); receipt.addProperty("observedCustomName", name == null ? "" : name.getString());
            receipt.addProperty("eventObserved", true); receipt.addProperty("matchingStackAfter", true); receipt.addProperty("componentStorageVerified", !c.row.get("storage").getAsString().equals("STORED_ENCHANTMENTS") || live.getEnchantments().isEmpty());
            receipt.addProperty("criterionBefore", c.before); receipt.addProperty("criterionAfter", done(player, c)); receipt.addProperty("ticksToComplete", ticks); receipt.addProperty("noDirectCriterionTrigger", true); receipt.addProperty("noManualAward", true);
            if (equip) { receipt.addProperty("headEmptyBefore", true); receipt.addProperty("headOccupiedAfter", player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)); receipt.addProperty("packetSent", c.packet); receipt.addProperty("noProductionAbilityGate", true); }
            else { receipt.addProperty("itemEntityConsumed", !c.entity.isAlive() || c.entity.getItem().isEmpty()); receipt.addProperty("pickedUpCount", live.getCount()); }
            if (c.entity != null) c.entity.discard(); var clean = cleanup(c.joined); JsonObject cleanup = new JsonObject();
            cleanup.addProperty("playerRemoved", clean.playerRemoved()); cleanup.addProperty("connectionRemoved", clean.connectionRemoved()); cleanup.addProperty("channelSettled", clean.channelSettled()); cleanup.addProperty("itemEntityRemoved", c.entity == null || !c.entity.isAlive()); cleanup.addProperty("warningCount", 0); cleanup.addProperty("settlementMessages", clean.messages()); receipt.add("cleanup", cleanup);
            PhaseAPureEnchantmentInventoryChangedExecutionEvidence.append(PhaseAPureEnchantmentInventoryChangedExecutionEvidence.root(), receipt);
            h.runAfterDelay(1, () -> runCase(h, cases, index + 1, run, exact));
        } catch (Throwable failure) { fail(h, c, "receipt", failure); }
    }
    private static void fail(GameTestHelper h, Context c, String stage, Throwable failure) {
        if (c.entity != null) c.entity.discard(); if (c.joined != null) cleanup(c.joined);
        h.fail("pure enchantment " + stage + " failed " + PhaseAPureEnchantmentInventoryChangedCertification.key(c.row) + ": " + describe(failure));
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, "pure" + id.toString().substring(0, 8));
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        return new Joined(player, connection, channel, new AtomicBoolean());
    }
    private static Cleanup cleanup(Joined joined) {
        if (!joined.cleaned().compareAndSet(false, true)) return new Cleanup(true, true, true, 0);
        MinecraftServer server = joined.player().level().getServer();
        if (joined.player().containerMenu != joined.player().inventoryMenu) joined.player().closeContainer();
        server.getPlayerList().remove(joined.player());
        boolean playerRemoved = server.getPlayerList().getPlayer(joined.player().getUUID()) != joined.player();
        server.getConnection().getConnections().remove(joined.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joined.connection());
        int messages = settle(joined.channel()); joined.connection().disconnect(Component.literal("pure enchantment cleanup"));
        return new Cleanup(playerRemoved, connectionRemoved, true, messages);
    }
    private static int settle(EmbeddedChannel channel) {
        if (!channel.isOpen()) return 0; int released = 0;
        for (int pass = 0; pass < 32; pass++) {
            channel.runPendingTasks(); channel.runScheduledPendingTasks(); channel.flushOutbound();
            int current = 0; Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound); current++;
                if (++released > 4096) throw new IllegalStateException("channel cleanup exceeded message bound");
            }
            if (current == 0 && !channel.hasPendingTasks()) return released;
        }
        throw new IllegalStateException("channel cleanup did not quiesce");
    }
    private static String describe(Throwable t) { Throwable c = t; while (c.getCause() != null && c.getMessage() == null) c = c.getCause(); return c.getClass().getSimpleName() + ": " + c.getMessage(); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    private static final class Context {
        final JsonObject row; Joined joined; AdvancementHolder advancement; ItemStack stack; ItemEntity entity; boolean before, packet;
        Context(JsonObject row) { this.row = row; }
    }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper h, Method method) throws ReflectiveOperationException { h.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, h); }
}
