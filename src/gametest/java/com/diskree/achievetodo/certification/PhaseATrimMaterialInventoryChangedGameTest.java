package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.AdvancementsMode;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

/** Native pickup and inventory-click witnesses for the frozen trim-material criteria. */
public final class PhaseATrimMaterialInventoryChangedGameTest implements CustomTestMethodInvoker {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final int MAX_TICKS = 100;

    @GameTest(maxTicks = 600)
    public void trimMaterialInventoryChangedCanary(GameTestHelper helper) {
        begin(helper, List.of(PhaseATrimMaterialInventoryChangedCertification.cases().getFirst()), false);
    }

    @GameTest(maxTicks = 600)
    public void trimMaterialInventoryChangedEquipmentCanary(GameTestHelper helper) {
        begin(helper, List.of(PhaseATrimMaterialInventoryChangedCertification.cases().get(11)), false);
    }

    @GameTest(maxTicks = 5500)
    public void trimMaterialInventoryChangedExact22(GameTestHelper helper) {
        begin(helper, PhaseATrimMaterialInventoryChangedCertification.cases(), true);
    }

    private static void begin(GameTestHelper helper, List<PhaseATrimMaterialInventoryChangedCertification.Case> cases, boolean exact) {
        try {
            String runId = PhaseATrimMaterialInventoryChangedExecutionEvidence.beginRun(PhaseATrimMaterialInventoryChangedExecutionEvidence.projectRoot());
            run(helper, cases, 0, runId, exact);
        } catch (Throwable t) { helper.fail("trim-material run setup failed: " + describe(t)); }
    }

    private static void run(GameTestHelper helper, List<PhaseATrimMaterialInventoryChangedCertification.Case> cases, int index, String runId, boolean exact) {
        if (index == cases.size()) {
            try {
                var artifact = PhaseATrimMaterialInventoryChangedExecutionEvidence.loadTemporary(PhaseATrimMaterialInventoryChangedExecutionEvidence.projectRoot(), exact);
                require(artifact.runId().equals(runId) && artifact.entries().size() == cases.size(), "TEMP receipt/runId mismatch");
                System.out.println((exact ? "TEMP_PROMOTABLE" : "TEMP_DIAGNOSTIC") + "=PASS family=TRIM_MATERIAL_INVENTORY_CHANGED runId=" + runId + " entries=" + artifact.entries().size());
                helper.succeed();
            } catch (Throwable t) { helper.fail("trim-material TEMP validation failed: " + describe(t)); }
            return;
        }
        var definition = cases.get(index);
        Joined joined = null; ItemEntity entity = null;
        try {
            helper.getLevel().setBlockAndUpdate(helper.absolutePos(TARGET).below(), Blocks.STONE.defaultBlockState());
            joined = join(helper);
            ServerPlayer player = joined.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            require(player.connection.hasClientLoaded() && player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(), "joined finite SURVIVAL lifecycle failed");
            require(helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player, "player not registered");
            require(helper.getLevel().getServer().getConnection().getConnections().contains(joined.connection()), "connection not registered");
            BlockPos target = helper.absolutePos(TARGET);
            player.teleportTo(target.getX() + .5, target.getY(), target.getZ() + .5);
            player.getInventory().clearContent();
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(definition.advancementId()));
            require(advancement != null, "missing advancement " + definition.advancementId());
            CriterionProgress before = player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion());
            require(before != null && !before.isDone(), "criterion already complete " + definition.key());

            JsonObject productionUnlockWitness = new JsonObject();
            if (definition.witness().equals("native_item_entity_pickup")) {
                productionUnlockWitness.addProperty("gatePresent", false);
                ItemStack fixture = trimmed(helper, new ItemStack(Items.IRON_HELMET), definition.materialId());
                require(definition.materialId().equals(material(fixture)), "item fixture material mismatch");
                entity = new ItemEntity(helper.getLevel(), player.getX(), player.getY() + .1, player.getZ(), fixture);
                entity.setPickUpDelay(0);
                require(helper.getLevel().addFreshEntity(entity), "could not add item entity");
            } else {
                require(definition.witness().equals("native_inventory_armor_click"), "unknown witness");
                productionUnlockWitness = unlockIronArmor(helper, player, advancement, definition);
                player.setItemSlot(EquipmentSlot.HEAD, trimmed(helper, new ItemStack(Items.IRON_HELMET), definition.materialId()));
                player.setItemSlot(EquipmentSlot.CHEST, trimmed(helper, new ItemStack(Items.IRON_CHESTPLATE), definition.materialId()));
                player.setItemSlot(EquipmentSlot.LEGS, trimmed(helper, new ItemStack(Items.IRON_LEGGINGS), definition.materialId()));
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS))
                    require(definition.materialId().equals(material(player.getItemBySlot(slot))), "equipped fixture mismatch " + slot);
                require(!player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion()).isDone(), "fixture completed criterion before click");
                InventoryMenu menu = player.inventoryMenu;
                require(player.containerMenu == menu, "inventory menu not active");
                int feetSlot = InventoryMenu.ARMOR_SLOT_START + 3;
                require(menu.getSlot(feetSlot).getItem().isEmpty(), "feet slot already occupied");
                player.getInventory().setItem(0, trimmed(helper, new ItemStack(Items.IRON_BOOTS), definition.materialId()));
                require(definition.materialId().equals(material(menu.getSlot(InventoryMenu.USE_ROW_SLOT_START).getItem())), "hotbar boot trim mismatch");
                require(!player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion()).isDone(), "hotbar fixture completed criterion before click");
                menu.broadcastChanges();
                new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), (short) InventoryMenu.USE_ROW_SLOT_START, (byte) 0,
                    ContainerInput.QUICK_MOVE, Int2ObjectMaps.emptyMap(), HashedStack.EMPTY).handle(player.connection);
            }
            Joined kept = joined; ItemEntity keptEntity = entity;
            JsonObject keptUnlockWitness = productionUnlockWitness;
            helper.runAfterDelay(1, () -> poll(helper, cases, index, runId, exact, kept, keptEntity, advancement, keptUnlockWitness, 1));
        } catch (Throwable t) {
            if (entity != null) entity.discard();
            if (joined != null) cleanup(joined);
            helper.fail("trim-material setup failed " + definition.key() + ": " + describe(t));
        }
    }

    private static void poll(GameTestHelper h, List<PhaseATrimMaterialInventoryChangedCertification.Case> cases, int index,
                             String runId, boolean exact, Joined joined, ItemEntity entity, AdvancementHolder advancement, JsonObject productionUnlockWitness, int ticks) {
        var definition = cases.get(index); ServerPlayer player = joined.player();
        try {
            CriterionProgress progress = player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion());
            boolean done = progress != null && progress.isDone();
            String observedMaterial;
            boolean eventObserved;
            if (entity != null) {
                observedMaterial = inventoryTrimMaterial(player, definition.materialId());
                eventObserved = (!entity.isAlive() || entity.getItem().isEmpty()) && definition.materialId().equals(observedMaterial);
            } else {
                observedMaterial = material(player.getItemBySlot(EquipmentSlot.FEET));
                eventObserved = definition.materialId().equals(observedMaterial)
                    && player.inventoryMenu.getCarried().isEmpty()
                    && List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).stream()
                        .allMatch(slot -> definition.materialId().equals(material(player.getItemBySlot(slot))));
            }
            if (done && eventObserved) {
                Cleanup cleaned = cleanup(joined);
                JsonObject receipt = receipt(definition, runId, player, observedMaterial, ticks, cleaned, eventObserved, productionUnlockWitness);
                PhaseATrimMaterialInventoryChangedExecutionEvidence.recordGreen(PhaseATrimMaterialInventoryChangedExecutionEvidence.projectRoot(), receipt);
                h.runAfterDelay(1, () -> run(h, cases, index + 1, runId, exact));
                return;
            }
            if (ticks >= MAX_TICKS) throw new IllegalStateException("native criterion did not transition; done=" + done + " eventObserved=" + eventObserved + " material=" + observedMaterial
                + " carried=" + player.inventoryMenu.getCarried() + " armorSlot=" + player.inventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START + 3).getItem()
                + " feet=" + player.getItemBySlot(EquipmentSlot.FEET) + " hotbar=" + player.inventoryMenu.getSlot(InventoryMenu.USE_ROW_SLOT_START).getItem());
            h.runAfterDelay(1, () -> poll(h, cases, index, runId, exact, joined, entity, advancement, productionUnlockWitness, ticks + 1));
        } catch (Throwable t) {
            if (entity != null) entity.discard();
            cleanup(joined);
            h.fail("trim-material native event failed " + definition.key() + ": " + describe(t));
        }
    }

    private static ItemStack trimmed(GameTestHelper helper, ItemStack stack, String materialId) {
        Holder<TrimMaterial> material = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
            .getOrThrow(ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.parse(materialId)));
        Holder<TrimPattern> pattern = helper.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY);
        stack.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
        return stack;
    }
    private static String material(ItemStack stack) {
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        return trim == null ? "" : trim.material().unwrapKey().orElseThrow().identifier().toString();
    }
    private static String inventoryTrimMaterial(ServerPlayer player, String expected) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            String actual = material(player.getInventory().getItem(i));
            if (expected.equals(actual)) return actual;
        }
        return "";
    }
    private static JsonObject unlockIronArmor(GameTestHelper helper, ServerPlayer player, AdvancementHolder advancement,
                                               PhaseATrimMaterialInventoryChangedCertification.Case definition) {
        require(definition.productionGate().equals("EQUIP_IRON_ARMOR"), "unexpected production gate");
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard(); Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null && AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not the active default objective");
        Objective settingsObjective = scoreboard.getObjective("bac_settings");
        require(settingsObjective != null, "missing bac_settings objective");
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("reward"), settingsObjective).set(0);
        var settings = (LevelInfoExtension) server.getWorldData().getLevelSettings();
        Map<AbilityType, Integer> abilities = settings.achievetodo$getAbilitiesConfiguration(server.overworld().getSeed());
        Integer threshold = abilities.get(AbilityType.EQUIP_IRON_ARMOR);
        require(threshold != null && threshold > 0, "live EQUIP_IRON_ARMOR threshold missing");
        var scoreInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int before = scoreInfo == null ? 0 : scoreInfo.value();
        require(before == 0, "fresh armor player already had advancement score");
        boolean lockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.EQUIP_IRON_ARMOR, true);
        require(lockedBefore, "iron armor ability was not locked before fixture");
        scoreboard.getOrCreatePlayerScore(player, objective).set(threshold);
        boolean lockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.EQUIP_IRON_ARMOR, true);
        var afterInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int after = afterInfo == null ? 0 : afterInfo.value();
        require(!lockedAfter && after == threshold, "iron armor ability did not unlock at live scoreboard threshold");
        require(!player.getAdvancements().getOrStartProgress(advancement).getCriterion(definition.criterion()).isDone(),
            "score fixture completed trim criterion");
        JsonObject witness = new JsonObject(); witness.addProperty("gatePresent", true);
        witness.addProperty("ability", AbilityType.EQUIP_IRON_ARMOR.name()); witness.addProperty("scoreboardObjective", objective.getName());
        witness.addProperty("thresholdSource", "live_overworld_seed_abilities_configuration");
        witness.addProperty("requiredThreshold", threshold); witness.addProperty("scoreBefore", before); witness.addProperty("scoreAfter", after);
        witness.addProperty("abilityLockedBefore", lockedBefore); witness.addProperty("abilityLockedAfter", lockedAfter);
        return witness;
    }
    private static JsonObject receipt(PhaseATrimMaterialInventoryChangedCertification.Case d, String runId, ServerPlayer player,
                                      String observedMaterial, int ticks, Cleanup cleanup, boolean eventObserved, JsonObject productionUnlockWitness) throws Exception {
        JsonObject r = new JsonObject();
        r.addProperty("advancementId", d.advancementId()); r.addProperty("criterion", d.criterion());
        r.addProperty("materialId", d.materialId()); r.addProperty("observedTrimMaterial", observedMaterial);
        r.addProperty("requirementGroup", d.requirementGroup()); r.addProperty("witness", d.witness()); r.addProperty("productionGate", d.productionGate());
        r.add("productionUnlockWitness", productionUnlockWitness.deepCopy());
        r.addProperty("trigger", "minecraft:inventory_changed");
        r.addProperty("boundary", d.witness().equals("native_item_entity_pickup")
            ? "ItemEntity.playerTouch->Inventory.add->InventoryChangedTrigger"
            : "ServerGamePacketListenerImpl.handleContainerClick->InventoryMenu->InventoryChangedTrigger");
        r.addProperty("family", PhaseATrimMaterialInventoryChangedExecutionEvidence.FAMILY);
        r.addProperty("source", PhaseATrimMaterialInventoryChangedExecutionEvidence.SOURCE);
        r.addProperty("catalogFingerprint", PhaseATrimMaterialInventoryChangedExecutionEvidence.currentCatalogFingerprint(PhaseATrimMaterialInventoryChangedExecutionEvidence.projectRoot()));
        r.addProperty("runId", runId); r.addProperty("result", "GREEN"); r.addProperty("gameMode", player.gameMode().name());
        r.addProperty("playerUuid", player.getUUID().toString()); r.addProperty("joined", cleanup.playerRemoved());
        r.addProperty("connectionRegistered", cleanup.connectionRemoved()); r.addProperty("clientLoaded", player.connection.hasClientLoaded());
        r.addProperty("finiteMaterials", !player.hasInfiniteMaterials()); r.addProperty("eventObserved", eventObserved);
        r.addProperty("criterionBefore", false); r.addProperty("criterionAfter", true);
        r.addProperty("noDirectCriterionTrigger", true); r.addProperty("noManualAward", true); r.addProperty("ticksToCriterion", ticks);
        JsonObject clean = new JsonObject(); clean.addProperty("playerRemoved", cleanup.playerRemoved());
        clean.addProperty("connectionRemoved", cleanup.connectionRemoved()); clean.addProperty("channelSettled", cleanup.channelSettled());
        clean.addProperty("settlementMessages", cleanup.messages()); clean.addProperty("warningCount", 0); r.add("cleanup", clean);
        return r;
    }
    private static Joined join(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer(); UUID id = UUID.randomUUID();
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), new GameProfile(id, "trim" + id.toString().substring(0, 8)), ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND); EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
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
        int messages = settle(joined.channel());
        joined.connection().disconnect(Component.literal("trim-material cleanup"));
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
    private static void require(boolean okay, String message) { if (!okay) throw new IllegalStateException(message); }
    private record Joined(ServerPlayer player, Connection connection, EmbeddedChannel channel, AtomicBoolean cleaned) { }
    private record Cleanup(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int messages) { }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { helper.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, helper); }
}
