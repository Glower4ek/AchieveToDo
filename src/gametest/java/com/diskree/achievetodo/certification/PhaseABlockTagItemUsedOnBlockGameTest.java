package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A3 implements the closed WAX_SIGN canary, DYE_SIGN coordinator, and GLOW_SIGN canary. */
public final class PhaseABlockTagItemUsedOnBlockGameTest implements CustomTestMethodInvoker {
    private static final int BONE_MEAL_MAX_ATTEMPTS = 31;
    private static final double BONE_MEAL_SUCCESS_PROBABILITY = 0.45d;
    private static final int BONE_MEAL_CLEAR_RADIUS = 8;
    private static final int BONE_MEAL_CLEAR_HEIGHT = 20;

    @GameTest(maxTicks = 100)
    public void waxSignCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-canary");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            // Run interaction on the ordinary server scheduler after joining/loading.
            helper.runAfterDelay(2, () -> {
                try {
                    executeCanary(helper, player, connection);
                } catch (Exception e) {
                    helper.fail("BLOCK_TAG_ITEM_USED_ON_BLOCK canary: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("Canary join/setup: " + e);
        }
    }

    @GameTest(maxTicks = 100)
    public void whiteDyeCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-white-dye");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                try {
                    executeWhiteDye(helper, player, connection);
                } catch (Exception e) {
                    helper.fail("BLOCK_TAG_ITEM_USED_ON_BLOCK white_dye: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("White dye join/setup: " + e);
        }
    }

    @GameTest(maxTicks = 100)
    public void glowSignCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-glow-sign");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executeGlowSign(helper, player, connection);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("GLOW_SIGN case minecraft:husbandry/make_a_sign_glow#glow_ink_sac: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("GLOW_SIGN join/setup minecraft:husbandry/make_a_sign_glow#glow_ink_sac: " + e);
        }
    }

    @GameTest(maxTicks = 100)
    public void mapBannerCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-map-banner");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executeMapBanner(helper, player, connection);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("MAP_BANNER case blazeandcave:adventure/im_not_lost_anymore#map: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("MAP_BANNER join/setup blazeandcave:adventure/im_not_lost_anymore#map: " + e);
        }
    }

    @GameTest(maxTicks = 100)
    public void safeHoneyHarvestCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-safe-honey-harvest");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
                "minecraft:husbandry/safely_harvest_honey", "safely_harvest_honey");
            require("BOTTLE_HONEY".equals(definition.get("action").getAsString()), "Wrong honey action");
            require("minecraft:glass_bottle".equals(definition.get("heldItem").getAsString()), "Wrong honey held item");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executeHoneyHarvest(helper, player, connection, definition);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("BOTTLE_HONEY case minecraft:husbandry/safely_harvest_honey#safely_harvest_honey: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("BOTTLE_HONEY join/setup minecraft:husbandry/safely_harvest_honey#safely_harvest_honey: " + e);
        }
    }

    @GameTest(maxTicks = 100)
    public void placeFoodPorkchopCanary(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-place-food-porkchop");
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
                "blazeandcave:building/delicious_hot_schmoes", "porkchop");
            require("PLACE_FOOD".equals(definition.get("action").getAsString()), "Wrong porkchop action");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executePlaceFood(helper, player, connection, definition);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("PLACE_FOOD case blazeandcave:building/delicious_hot_schmoes#porkchop: " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.succeed();
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("PLACE_FOOD join/setup blazeandcave:building/delicious_hot_schmoes#porkchop: " + e);
        }
    }

    @GameTest(maxTicks = 400)
    public void placeFoodCoordinator(GameTestHelper helper) {
        java.util.List<JsonObject> definitions;
        try {
            definitions = PhaseABlockTagItemUsedOnBlockExecutionEvidence.placeFoodDefinitions();
            require(definitions.size() == 9, "Expected exactly 9 PLACE_FOOD definitions");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
        } catch (Throwable e) {
            helper.fail("PLACE_FOOD coordinator setup: " + e);
            return;
        }
        schedulePlaceFoodCase(helper, definitions, 0);
    }

    @GameTest(maxTicks = 400)
    public void boneMealTreeCanary(GameTestHelper helper) {
        JsonObject definition;
        try {
            definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
                "blazeandcave:farming/one_course_meal", "bone_meal");
            require("GROW_OAK_SAPLING".equals(definition.get("action").getAsString()), "Wrong bone-meal action");
            require("minecraft:bone_meal".equals(definition.get("heldItem").getAsString()), "Wrong bone-meal held item");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
        } catch (Throwable e) {
            helper.fail("GROW_OAK_SAPLING coordinator setup: " + e);
            return;
        }
        scheduleBoneMealAttempt(helper, definition, 1);
    }

    @GameTest(maxTicks = 400)
    public void dyeSignCoordinator(GameTestHelper helper) {
        java.util.List<JsonObject> definitions;
        try {
            definitions = PhaseABlockTagItemUsedOnBlockExecutionEvidence.dyeSignDefinitions();
            require(definitions.size() == 16, "Expected exactly 16 DYE_SIGN definitions");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
        } catch (Throwable e) {
            helper.fail("DYE_SIGN coordinator setup: " + e);
            return;
        }
        scheduleDyeCase(helper, definitions, 0);
    }

    /** Runs the complete catalog in one fresh TEMP run and owns the only terminal success. */
    @GameTest(maxTicks = 400)
    public void exact30Coordinator(GameTestHelper helper) {
        List<JsonObject> definitions;
        try {
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertExact30Catalog();
            definitions = exact30Definitions();
            require(definitions.size() == 30, "Expected exactly 30 exact30 definitions");
            PhaseABlockTagItemUsedOnBlockExecutionEvidence.beginRun();
        } catch (Throwable e) {
            helper.fail("EXACT30 coordinator setup: " + e);
            return;
        }
        scheduleExact30Case(helper, definitions, 0);
    }

    private static List<JsonObject> exact30Definitions() throws Exception {
        List<JsonObject> definitions = new ArrayList<>();

        JsonObject wax = PhaseABlockTagItemUsedOnBlockExecutionEvidence.canaryDefinition();
        require("WAX_SIGN".equals(wax.get("action").getAsString()), "Wrong WAX_SIGN action");
        definitions.add(wax);

        List<JsonObject> dye = PhaseABlockTagItemUsedOnBlockExecutionEvidence.dyeSignDefinitions();
        require(dye.size() == 16, "Expected exactly 16 DYE_SIGN definitions");
        definitions.addAll(dye);

        JsonObject glow = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
            "minecraft:husbandry/make_a_sign_glow", "glow_ink_sac");
        require("GLOW_SIGN".equals(glow.get("action").getAsString()), "Wrong GLOW_SIGN action");
        definitions.add(glow);

        JsonObject map = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
            "blazeandcave:adventure/im_not_lost_anymore", "map");
        require("MAP_BANNER".equals(map.get("action").getAsString()), "Wrong MAP_BANNER action");
        definitions.add(map);

        List<JsonObject> placeFood = PhaseABlockTagItemUsedOnBlockExecutionEvidence.placeFoodDefinitions();
        require(placeFood.size() == 9, "Expected exactly 9 PLACE_FOOD definitions");
        definitions.addAll(placeFood);

        JsonObject honey = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
            "minecraft:husbandry/safely_harvest_honey", "safely_harvest_honey");
        require("BOTTLE_HONEY".equals(honey.get("action").getAsString()), "Wrong BOTTLE_HONEY action");
        definitions.add(honey);

        JsonObject boneMeal = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
            "blazeandcave:farming/one_course_meal", "bone_meal");
        require("GROW_OAK_SAPLING".equals(boneMeal.get("action").getAsString()), "Wrong GROW_OAK_SAPLING action");
        definitions.add(boneMeal);

        Set<String> keys = new HashSet<>();
        Map<String, Integer> actionCounts = new HashMap<>();
        for (JsonObject definition : definitions) {
            String definitionKey = caseKey(definition);
            require(keys.add(definitionKey), "Duplicate exact30 definition: " + definitionKey);
            JsonObject checked = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
                definition.get("advancementId").getAsString(), definition.get("criterion").getAsString());
            require(caseKey(checked).equals(definitionKey), "Definition changed during exact30 preflight: " + definitionKey);
            String action = definition.get("action").getAsString();
            actionCounts.merge(action, 1, Integer::sum);
        }
        require(definitions.size() == 30 && keys.size() == 30, "Exact30 definition coverage is not 30 unique keys");
        require(actionCounts.getOrDefault("WAX_SIGN", 0) == 1, "Exact30 WAX_SIGN count is not 1");
        require(actionCounts.getOrDefault("DYE_SIGN", 0) == 16, "Exact30 DYE_SIGN count is not 16");
        require(actionCounts.getOrDefault("GLOW_SIGN", 0) == 1, "Exact30 GLOW_SIGN count is not 1");
        require(actionCounts.getOrDefault("MAP_BANNER", 0) == 1, "Exact30 MAP_BANNER count is not 1");
        require(actionCounts.getOrDefault("PLACE_FOOD", 0) == 9, "Exact30 PLACE_FOOD count is not 9");
        require(actionCounts.getOrDefault("BOTTLE_HONEY", 0) == 1, "Exact30 BOTTLE_HONEY count is not 1");
        require(actionCounts.getOrDefault("GROW_OAK_SAPLING", 0) == 1, "Exact30 GROW_OAK_SAPLING count is not 1");
        require(actionCounts.size() == 7, "Exact30 contains an unsupported action");
        return definitions;
    }

    private static void scheduleExact30Case(GameTestHelper helper, List<JsonObject> definitions, int index) {
        if (index == definitions.size()) {
            try {
                PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertExact30Coverage();
                helper.succeed();
            } catch (Throwable e) {
                helper.fail("EXACT30 coverage: " + e);
            }
            return;
        }
        if (index > definitions.size()) {
            helper.fail("EXACT30 case index exceeded catalog: " + index);
            return;
        }

        JsonObject definition = definitions.get(index);
        if ("GROW_OAK_SAPLING".equals(definition.get("action").getAsString())) {
            scheduleExact30BoneMealAttempt(helper, definitions, index, 1);
            return;
        }

        String caseKey = caseKey(definition);
        var server = helper.getLevel().getServer();
        UUID playerUuid = UUID.randomUUID();
        String profileName = "block-tag-exact30-" + playerUuid.toString().replace("-", "").substring(0, 12);
        var profile = new GameProfile(playerUuid, profileName);
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executeExact30Case(helper, player, connection, definition);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("EXACT30 case " + caseKey + ": " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) continueExact30(helper, definitions, index + 1);
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("EXACT30 case " + caseKey + " setup: " + e);
        }
    }

    private static void executeExact30Case(GameTestHelper helper, ServerPlayer player, Connection connection, JsonObject definition) throws Exception {
        switch (definition.get("action").getAsString()) {
            case "WAX_SIGN" -> {
                require("blazeandcave:building/sign_off#honeycomb".equals(caseKey(definition)), "Unexpected WAX_SIGN exact30 case");
                executeCanary(helper, player, connection);
            }
            case "DYE_SIGN" -> executeDyeCase(helper, player, connection, definition);
            case "GLOW_SIGN" -> executeGlowSign(helper, player, connection);
            case "MAP_BANNER" -> executeMapBanner(helper, player, connection);
            case "PLACE_FOOD" -> executePlaceFood(helper, player, connection, definition);
            case "BOTTLE_HONEY" -> executeHoneyHarvest(helper, player, connection, definition);
            default -> throw new IllegalStateException("Unsupported exact30 action: " + definition.get("action"));
        }
    }

    private static void scheduleExact30BoneMealAttempt(
        GameTestHelper helper, List<JsonObject> definitions, int catalogIndex, int attemptIndex) {
        JsonObject definition = definitions.get(catalogIndex);
        String caseKey = caseKey(definition);
        if (attemptIndex > BONE_MEAL_MAX_ATTEMPTS) {
            helper.fail("FAILED_EXACT30_BONE_MEAL_RANDOM_RETRY_EXHAUSTED: caseKey=" + caseKey
                + ", probability=" + BONE_MEAL_SUCCESS_PROBABILITY
                + ", maxAttempts=" + BONE_MEAL_MAX_ATTEMPTS
                + ", allFailureProbability=" + Math.pow(1.0d - BONE_MEAL_SUCCESS_PROBABILITY, BONE_MEAL_MAX_ATTEMPTS));
            return;
        }

        var server = helper.getLevel().getServer();
        UUID playerUuid = UUID.randomUUID();
        String profileName = "block-tag-exact30-bone-" + playerUuid.toString().replace("-", "").substring(0, 12);
        var profile = new GameProfile(playerUuid, profileName);
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean growth = false;
                boolean retry = false;
                try {
                    BoneMealAttemptResult result = executeBoneMealAttempt(helper, player, connection, definition, attemptIndex);
                    if (result == BoneMealAttemptResult.GROWTH) growth = true;
                    else retry = true;
                } catch (Throwable e) {
                    helper.fail("EXACT30 case " + caseKey + " attempt " + attemptIndex + ": " + e);
                } finally {
                    cleanup(helper, player, connection);
                    if (retry) {
                        try {
                            clearBoneMealFixture(helper.getLevel(), boneMealFixturePosition(helper));
                        } catch (Throwable e) {
                            retry = false;
                            helper.fail("EXACT30 bone-meal fixture cleanup attempt " + attemptIndex + ": " + e);
                        }
                    }
                }
                if (growth) continueExact30(helper, definitions, catalogIndex + 1);
                else if (retry) helper.runAfterDelay(1, () -> scheduleExact30BoneMealAttempt(helper, definitions, catalogIndex, attemptIndex + 1));
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("EXACT30 case " + caseKey + " attempt " + attemptIndex + " setup: " + e);
        }
    }

    private static void continueExact30(GameTestHelper helper, List<JsonObject> definitions, int nextIndex) {
        try {
            if (nextIndex > 0 && nextIndex < definitions.size()) {
                String previousAction = definitions.get(nextIndex - 1).get("action").getAsString();
                String nextAction = definitions.get(nextIndex).get("action").getAsString();
                if ("DYE_SIGN".equals(previousAction) && !"DYE_SIGN".equals(nextAction)) {
                    PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertDyeSignCoverageInExact30();
                }
                if ("PLACE_FOOD".equals(previousAction) && !"PLACE_FOOD".equals(nextAction)) {
                    PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertPlaceFoodCoverageInExact30();
                }
            }
            helper.runAfterDelay(1, () -> scheduleExact30Case(helper, definitions, nextIndex));
        } catch (Throwable e) {
            helper.fail("EXACT30 continuation after case " + (nextIndex - 1) + ": " + e);
        }
    }

    private static void executeWhiteDye(GameTestHelper helper, ServerPlayer player, Connection connection) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer");
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined");
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered");
        require(player.connection.hasClientLoaded(), "Client not loaded");
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof SignBlockEntity, "Missing sign");
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
        require(!sign.isWaxed(), "Fixture must be fresh and unwaxed");
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        boolean activeSideFront = sign.isFacingFrontText(player);
        require(sign.updateText(text -> text.setMessage(0, Component.literal("white dye canary")).setColor(DyeColor.BLACK), activeSideFront), "Could not set fixture text");
        SignText beforeText = sign.getText(activeSideFront);
        boolean textPresent = beforeText.hasMessage(player);
        require(textPresent, "Active sign side has no text");
        require(!sign.getText(!activeSideFront).hasMessage(player), "Text fixture leaked to inactive sign side");
        boolean waxedBefore = sign.isWaxed();
        DyeColor colorBefore = beforeText.getColor();
        require(!waxedBefore, "Fixture must remain unwaxed before dye");
        require(colorBefore == DyeColor.BLACK && colorBefore != DyeColor.WHITE, "Fixture must start BLACK, not WHITE");

        player.getInventory().clearContent();
        var whiteDye = Items.DYE.white();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(whiteDye, 1));
        JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(
            "blazeandcave:building/colors_of_the_wind", "white_dye");
        JsonObject receipt = new JsonObject();
        for (String field : java.util.List.of("advancementId", "criterion", "requirementGroupIndex", "trigger", "blockTag", "blockTagSampling", "heldItem", "action")) receipt.add(field, definition.get(field).deepCopy());
        var advancement = server.getAdvancements().get(Identifier.parse(receipt.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement");
        String criterionName = receipt.get("criterion").getAsString();
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterion != null && !criterion.isDone(), "Criterion must start false");
        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag");
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        var before = level.getBlockState(pos);
        require(before.is(tag), "Sign is outside runtime all_signs tag");
        ItemStack interactionStack = player.getMainHandItem();
        require(interactionStack.is(whiteDye) && interactionStack.getCount() == 1, "Wrong held stack");
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("activeSideFront", activeSideFront);
        receipt.addProperty("expectedColor", DyeColor.WHITE.getName());
        JsonObject proof = new JsonObject();
        proof.addProperty("textPresent", textPresent);
        proof.addProperty("waxedBefore", waxedBefore);
        proof.addProperty("colorBefore", colorBefore.getName());
        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(result.consumesAction(), "Interaction did not consume action");
        require(level.getBlockEntity(pos) == sign, "Sign block entity was replaced");
        require(!sign.isWaxed(), "Dye interaction waxed the sign");
        SignText afterText = sign.getText(activeSideFront);
        require(afterText.hasMessage(player), "Active sign side lost its text");
        DyeColor expectedColor = DyeColor.WHITE;
        DyeColor colorAfter = afterText.getColor();
        require(colorAfter == expectedColor && colorBefore != colorAfter, "Sign color did not change BLACK -> WHITE");
        require(afterCriterion != null && afterCriterion.isDone(), "Criterion did not become true");
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside runtime tag");

        // Read the selected hand only after useItemOn and all synchronous criterion callbacks have completed.
        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        String selectedHandItemAfter = selectedHandAfter.isEmpty() ? "minecraft:air" : selectedHandAfter.getItem().toString();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName()); receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild()); receipt.addProperty("productionPreconditions", true);
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag)); receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString()); receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", colorBefore != colorAfter);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        proof.addProperty("waxedAfter", sign.isWaxed());
        proof.addProperty("colorAfter", colorAfter.getName());
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }
    private static void executeGlowSign(GameTestHelper helper, ServerPlayer player, Connection connection) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String advancementId = "minecraft:husbandry/make_a_sign_glow";
        String criterionName = "glow_ink_sac";
        String caseKey = advancementId + "#" + criterionName;
        JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(advancementId, criterionName);
        require("GLOW_SIGN".equals(definition.get("action").getAsString()), "Wrong action for " + caseKey);
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer for " + caseKey);
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined for " + caseKey);
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered for " + caseKey);
        require(player.connection.hasClientLoaded(), "Client not loaded for " + caseKey);
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required for " + caseKey);

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof SignBlockEntity, "Missing sign for " + caseKey);
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
        require(!sign.isWaxed(), "Fixture must be fresh and unwaxed for " + caseKey);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        boolean activeSideFront = sign.isFacingFrontText(player);
        require(sign.updateText(text -> text.setMessage(0, Component.literal("glow sign canary")), activeSideFront), "Could not set fixture text for " + caseKey);
        SignText beforeText = sign.getText(activeSideFront);
        boolean textPresent = beforeText.hasMessage(player);
        require(textPresent, "Active sign side has no text for " + caseKey);
        require(!sign.getText(!activeSideFront).hasMessage(player), "Text fixture leaked to inactive sign side for " + caseKey);
        boolean waxedBefore = sign.isWaxed();
        boolean glowingBefore = beforeText.hasGlowingText();
        require(!waxedBefore && !glowingBefore, "Invalid glow-sign fixture state for " + caseKey);

        String heldItemId = definition.get("heldItem").getAsString();
        Identifier heldItemIdentifier = Identifier.parse(heldItemId);
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered: " + heldItemId);
        Item catalogItem = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        require(catalogItem instanceof GlowInkSacItem, "Catalog item is not a GlowInkSacItem: " + heldItemId);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catalogItem, 1));
        ItemStack interactionStack = player.getMainHandItem();
        require(interactionStack.getItem() == catalogItem && interactionStack.getCount() == 1, "Wrong catalog-bound held stack for " + caseKey);

        var advancement = server.getAdvancements().get(Identifier.parse(definition.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement for " + caseKey);
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterion != null, "Criterion must exist for " + caseKey);
        require(!criterion.isDone(), "Criterion must start false for " + caseKey);
        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag for " + caseKey);
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        var before = level.getBlockState(pos);
        require(before.is(tag), "Sign is outside catalog block tag for " + caseKey);
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        require(interactionStackCountBefore == 1, "Interaction stack must start at one for " + caseKey);

        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("activeSideFront", activeSideFront);
        JsonObject proof = new JsonObject();
        proof.addProperty("textPresent", textPresent);
        proof.addProperty("waxedBefore", waxedBefore);
        proof.addProperty("glowingBefore", glowingBefore);
        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(result.consumesAction(), "Interaction did not consume action for " + caseKey);
        require(level.getBlockEntity(pos) == sign, "Sign block entity was replaced for " + caseKey);
        require(!sign.isWaxed(), "Glow interaction waxed the sign for " + caseKey);
        SignText afterText = sign.getText(activeSideFront);
        require(afterText.hasMessage(player), "Active sign side lost its text for " + caseKey);
        boolean glowingAfter = afterText.hasGlowingText();
        require(!glowingBefore && glowingAfter, "Sign did not become glowing for " + caseKey);
        require(afterCriterion != null && afterCriterion.isDone(), "Criterion did not become true for " + caseKey);
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside catalog block tag for " + caseKey);

        // Read the selected hand only after useItemOn and all synchronous criterion callbacks have completed.
        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        require(interactionStackCountAfter == 0, "Glow ink interaction stack not consumed for " + caseKey);
        String selectedHandItemAfter = selectedHandAfter.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(selectedHandAfter.getItem()).toString();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild());
        receipt.addProperty("productionPreconditions", true);
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag));
        receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString());
        receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", !glowingBefore && glowingAfter);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        proof.addProperty("waxedAfter", sign.isWaxed());
        proof.addProperty("glowingAfter", glowingAfter);
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }

    private static void executeMapBanner(GameTestHelper helper, ServerPlayer player, Connection connection) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String advancementId = "blazeandcave:adventure/im_not_lost_anymore";
        String criterionName = "map";
        String caseKey = advancementId + "#" + criterionName;
        JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.definitionFor(advancementId, criterionName);
        require("MAP_BANNER".equals(definition.get("action").getAsString()), "Wrong action for " + caseKey);
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer for " + caseKey);
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined for " + caseKey);
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered for " + caseKey);
        require(player.connection.hasClientLoaded(), "Client not loaded for " + caseKey);
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required for " + caseKey);

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        Identifier whiteBannerId = Identifier.parse("minecraft:white_banner");
        require(BuiltInRegistries.BLOCK.containsKey(whiteBannerId), "minecraft:white_banner is not registered for " + caseKey);
        Block whiteBanner = BuiltInRegistries.BLOCK.getValue(whiteBannerId);
        require(whiteBanner != null, "Could not resolve minecraft:white_banner for " + caseKey);
        level.setBlockAndUpdate(pos, whiteBanner.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof BannerBlockEntity, "Missing banner for " + caseKey);
        MapBanner bannerBefore = MapBanner.fromWorld(level, pos);
        require(bannerBefore != null && bannerBefore.pos().equals(pos), "Missing live banner descriptor for " + caseKey);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);

        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag for " + caseKey);
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        var before = level.getBlockState(pos);
        require(before.is(tag), "Banner is outside catalog block tag for " + caseKey);

        player.getInventory().clearContent();
        ItemStack filledMap = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 0, true, false);
        require(filledMap.is(Items.FILLED_MAP) && filledMap.getCount() == 1, "MapItem.create did not create one filled map for " + caseKey);
        player.setItemInHand(InteractionHand.MAIN_HAND, filledMap);
        ItemStack interactionStack = player.getMainHandItem();
        require(interactionStack == filledMap && interactionStack.is(Items.FILLED_MAP) && interactionStack.getCount() == 1, "Wrong filled-map interaction stack for " + caseKey);
        var mapIdBefore = interactionStack.get(DataComponents.MAP_ID);
        require(mapIdBefore != null, "Filled map has no map identity for " + caseKey);
        MapItemSavedData mapDataBefore = MapItem.getSavedData(interactionStack, level);
        require(mapDataBefore != null && mapDataBefore.scale == 0, "Filled map saved data is missing or not scale 0 for " + caseKey);
        boolean mapCoversBanner = mapCoversBanner(mapDataBefore, pos);
        require(mapCoversBanner, "Filled map does not cover banner for " + caseKey);
        String markerId = bannerBefore.getId();
        MapBanner markerBefore = findMapBanner(mapDataBefore, markerId);
        require(markerBefore == null, "Banner marker already exists before interaction for " + caseKey);

        var advancement = server.getAdvancements().get(Identifier.parse(definition.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement for " + caseKey);
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterion != null, "Criterion must exist for " + caseKey);
        require(!criterion.isDone(), "Criterion must start false for " + caseKey);
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        require(interactionStackCountBefore == 1, "Interaction stack must start at one for " + caseKey);

        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        JsonObject proof = new JsonObject();
        proof.addProperty("mapCoversBanner", mapCoversBanner);
        proof.addProperty("markerBefore", markerBefore != null);
        proof.addProperty("mapId", mapIdBefore.key());
        proof.addProperty("markerId", markerId);
        proof.addProperty("bannerPosition", pos.toShortString());

        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(result.consumesAction(), "Interaction did not consume action for " + caseKey);

        MapItemSavedData mapDataAfter = MapItem.getSavedData(interactionStack, level);
        require(mapDataAfter != null && mapCoversBanner(mapDataAfter, pos), "Filled map saved data no longer covers banner for " + caseKey);
        var mapIdAfter = interactionStack.get(DataComponents.MAP_ID);
        require(mapIdAfter != null, "Filled map lost map identity for " + caseKey);
        boolean sameMapIdentity = mapIdBefore.equals(mapIdAfter);
        require(sameMapIdentity, "Filled map identity changed for " + caseKey);
        MapBanner bannerAfter = MapBanner.fromWorld(level, pos);
        require(bannerAfter != null && bannerAfter.pos().equals(pos), "Live banner descriptor changed for " + caseKey);
        MapBanner markerAfter = findMapBanner(mapDataAfter, markerId);
        boolean markerAfterPresent = markerAfter != null;
        require(markerAfterPresent, "Banner marker was not created for " + caseKey);
        require(markerAfter.pos().equals(pos) && markerId.equals(markerAfter.getId()), "Banner marker does not match clicked banner position for " + caseKey);
        boolean markerMatchesBanner = markerAfter.equals(bannerAfter);
        require(markerMatchesBanner, "Banner marker descriptor does not match clicked banner for " + caseKey);
        require(afterCriterion != null && afterCriterion.isDone(), "Criterion did not become true for " + caseKey);
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside catalog banner tag for " + caseKey);

        // Read the selected hand only after useItemOn and all synchronous criterion callbacks have completed.
        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        require(interactionStackCountAfter == 1, "Filled map interaction stack changed for " + caseKey);
        var selectedHandMapId = selectedHandAfter.get(DataComponents.MAP_ID);
        require(selectedHandAfter.is(Items.FILLED_MAP) && mapIdAfter.equals(selectedHandMapId), "Selected hand no longer holds the same filled map for " + caseKey);
        String selectedHandItemAfter = selectedHandAfter.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(selectedHandAfter.getItem()).toString();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        proof.addProperty("markerAfter", markerAfterPresent);
        proof.addProperty("markerPosition", markerAfter.pos().toShortString());
        proof.addProperty("markerMatchesBanner", markerMatchesBanner);
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild());
        receipt.addProperty("productionPreconditions", true);
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag));
        receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString());
        receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", markerBefore == null && markerAfterPresent);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", false);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("sameMapIdentity", sameMapIdentity);
        receipt.addProperty("mapIdAfter", mapIdAfter.key());
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }

    private static MapBanner findMapBanner(MapItemSavedData mapData, String markerId) {
        for (MapBanner marker : mapData.getBanners()) {
            if (markerId.equals(marker.getId())) return marker;
        }
        return null;
    }

    private static boolean mapCoversBanner(MapItemSavedData mapData, BlockPos pos) {
        int mapScale = 1 << mapData.scale;
        double mapX = (pos.getX() + 0.5 - mapData.centerX) / mapScale;
        double mapZ = (pos.getZ() + 0.5 - mapData.centerZ) / mapScale;
        return mapX >= -63.0 && mapX <= 63.0 && mapZ >= -63.0 && mapZ <= 63.0;
    }

    private static void executeHoneyHarvest(GameTestHelper helper, ServerPlayer player, Connection connection, JsonObject definition) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String advancementId = definition.get("advancementId").getAsString();
        String criterionName = definition.get("criterion").getAsString();
        String caseKey = advancementId + "#" + criterionName;
        require("BOTTLE_HONEY".equals(definition.get("action").getAsString()), "Wrong action for " + caseKey);
        require("minecraft:glass_bottle".equals(definition.get("heldItem").getAsString()), "Wrong held item for " + caseKey);
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer for " + caseKey);
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined for " + caseKey);
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered for " + caseKey);
        require(player.connection.hasClientLoaded(), "Client not loaded for " + caseKey);
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required for " + caseKey);

        BlockPos hivePos = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos campfirePos = hivePos.below();
        level.setBlockAndUpdate(hivePos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(campfirePos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(campfirePos.below(), Blocks.STONE.defaultBlockState());
        BlockState campfireState = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        level.setBlockAndUpdate(campfirePos, campfireState);
        int maxHoneyLevels = BeehiveBlock.MAX_HONEY_LEVELS;
        require(maxHoneyLevels == 5, "Unexpected max honey level for " + caseKey + ": " + maxHoneyLevels);
        BlockState hiveState = Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, maxHoneyLevels);
        level.setBlockAndUpdate(hivePos, hiveState);
        require(level.getBlockEntity(hivePos) instanceof BeehiveBlockEntity, "Missing beehive block entity for " + caseKey);
        BeehiveBlockEntity hive = (BeehiveBlockEntity) level.getBlockEntity(hivePos);
        require(hive.isEmpty(), "Honey fixture unexpectedly contains bees for " + caseKey);

        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag for " + caseKey);
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        BlockState before = level.getBlockState(hivePos);
        require(before.is(tag), "Beehive is outside catalog block tag for " + caseKey);
        int honeyLevelBefore = before.getValue(BeehiveBlock.HONEY_LEVEL);
        require(honeyLevelBefore == maxHoneyLevels, "Honey fixture did not start full for " + caseKey + ": " + honeyLevelBefore);
        BlockState campfireBefore = level.getBlockState(campfirePos);
        boolean litCampfireBelow = campfireBefore.getBlock() == Blocks.CAMPFIRE
            && campfireBefore.getValue(CampfireBlock.LIT);
        require(litCampfireBelow, "Campfire below hive is not lit for " + caseKey);
        boolean smokeyBefore = CampfireBlock.isSmokeyPos(level, hivePos);
        require(smokeyBefore, "Lit campfire is not smokey at hive for " + caseKey);

        player.teleportTo(hivePos.getX() + 0.5, hivePos.getY(), hivePos.getZ() - 1.5);
        player.getInventory().clearContent();
        Identifier heldItemIdentifier = Identifier.parse(definition.get("heldItem").getAsString());
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered for " + caseKey);
        Item catalogItem = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        require(catalogItem == Items.GLASS_BOTTLE, "Catalog honey item is not minecraft:glass_bottle for " + caseKey);
        require(BuiltInRegistries.ITEM.getKey(Items.HONEY_BOTTLE).equals(Identifier.parse("minecraft:honey_bottle")), "minecraft:honey_bottle is not registered for " + caseKey);
        ItemStack interactionStack = new ItemStack(catalogItem, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, interactionStack);
        require(player.getMainHandItem() == interactionStack, "Could not retain exact honey interaction stack for " + caseKey);
        require(interactionStack.is(catalogItem) && interactionStack.getCount() == 1, "Wrong catalog-bound honey stack for " + caseKey);

        var advancement = server.getAdvancements().get(Identifier.parse(advancementId));
        require(advancement != null, "Missing live advancement for " + caseKey);
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        if (criterion == null) throw new IllegalStateException("FAILED_HONEY_CRITERION_ABSENT: " + caseKey);
        if (criterion.isDone()) throw new IllegalStateException("FAILED_HONEY_CRITERION_ALREADY_DONE: " + caseKey);
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        require(interactionStackCountBefore == 1, "Honey interaction stack must start at one for " + caseKey);

        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        JsonObject proof = new JsonObject();
        proof.addProperty("litCampfireBelow", litCampfireBelow);
        proof.addProperty("smokeyBefore", smokeyBefore);
        proof.addProperty("honeyLevelBefore", honeyLevelBefore);
        proof.addProperty("maxHoneyLevels", maxHoneyLevels);
        proof.addProperty("hiveOccupantCountBefore", hive.getOccupantCount());
        proof.addProperty("hiveEmptyBefore", hive.isEmpty());

        var hit = new BlockHitResult(new Vec3(hivePos.getX() + 0.5, hivePos.getY() + 0.5, hivePos.getZ()), Direction.NORTH, hivePos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        require(result.consumesAction(), "Honey interaction did not consume action for " + caseKey);

        BlockState after = level.getBlockState(hivePos);
        require(level.getBlockEntity(hivePos) == hive, "Beehive block entity was replaced for " + caseKey);
        int honeyLevelAfter = after.getValue(BeehiveBlock.HONEY_LEVEL);
        require(honeyLevelAfter == 0, "Honey level did not reset to zero for " + caseKey + ": " + honeyLevelAfter);
        BlockState campfireAfter = level.getBlockState(campfirePos);
        require(campfireAfter.getBlock() == Blocks.CAMPFIRE, "Campfire below hive was replaced for " + caseKey);
        require(campfireAfter.getValue(CampfireBlock.LIT), "Campfire below hive went out for " + caseKey);
        boolean smokeyAfter = CampfireBlock.isSmokeyPos(level, hivePos);
        require(smokeyAfter, "Campfire smoke was lost after honey harvest for " + caseKey);
        require(after.is(tag), "Post-use hive is outside catalog block tag for " + caseKey);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(afterCriterion != null && afterCriterion.isDone(), "Honey criterion did not become true for " + caseKey);

        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        require(interactionStackCountAfter == 0, "Honey interaction stack was not consumed for " + caseKey);
        require(selectedHandAfter.is(Items.HONEY_BOTTLE) && selectedHandAfter.getCount() == 1, "Selected hand did not receive honey bottle for " + caseKey);
        boolean sameStackReference = interactionStack == selectedHandAfter;
        require(!sameStackReference, "Honey bottle replaced the retained interaction stack in place for " + caseKey);
        boolean semanticMutation = honeyLevelBefore == maxHoneyLevels
            && honeyLevelAfter == 0
            && selectedHandAfter.is(Items.HONEY_BOTTLE)
            && selectedHandAfter.getCount() == 1
            && interactionStackCountBefore == 1
            && interactionStackCountAfter == 0;
        require(semanticMutation, "Honey harvest did not perform the complete semantic mutation for " + caseKey);

        proof.addProperty("smokeyAfter", smokeyAfter);
        proof.addProperty("honeyLevelAfter", honeyLevelAfter);
        proof.addProperty("honeyBottleProduced", selectedHandAfter.is(Items.HONEY_BOTTLE) && selectedHandAfter.getCount() == 1);
        proof.addProperty("hiveBlockEntitySame", level.getBlockEntity(hivePos) == hive);
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild());
        receipt.addProperty("productionPreconditions", litCampfireBelow && smokeyBefore && smokeyAfter);
        receipt.addProperty("runtimeTagMembership", after.is(tag));
        receipt.addProperty("clickedPosition", hivePos.toShortString());
        receipt.addProperty("blockStateAfter", after.toString());
        receipt.addProperty("interactionResult", result.toString());
        receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", semanticMutation);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("selectedHandItemAfter", itemId(selectedHandAfter));
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }

    private static void executePlaceFood(GameTestHelper helper, ServerPlayer player, Connection connection, JsonObject definition) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String advancementId = definition.get("advancementId").getAsString();
        String criterionName = definition.get("criterion").getAsString();
        String caseKey = advancementId + "#" + criterionName;
        require("PLACE_FOOD".equals(definition.get("action").getAsString()), "Wrong action for " + caseKey);
        boolean authorizedCase = false;
        for (JsonObject placeFoodDefinition : PhaseABlockTagItemUsedOnBlockExecutionEvidence.placeFoodDefinitions()) {
            if (caseKey.equals(caseKey(placeFoodDefinition))) {
                authorizedCase = true;
                break;
            }
        }
        require(authorizedCase, "PLACE_FOOD case is outside the exact catalog-derived family: " + caseKey);
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer for " + caseKey);
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined for " + caseKey);
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered for " + caseKey);
        require(player.connection.hasClientLoaded(), "Client not loaded for " + caseKey);
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required for " + caseKey);

        var advancement = server.getAdvancements().get(Identifier.parse(definition.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement for " + caseKey);
        var criterionBeforeUnlock = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterionBeforeUnlock != null, "PLACE_FOOD criterion absent for " + caseKey);
        require(!criterionBeforeUnlock.isDone(), "PLACE_FOOD criterion already done before unlock fixture for " + caseKey);

        var scoreboard = server.getScoreboard();
        var objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective for " + caseKey);
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT, "bac_advancements is not the active default objective for " + caseKey);
        var scoreInfoBefore = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = scoreInfoBefore == null ? 0 : scoreInfoBefore.value();
        require(scoreBefore == 0, "Fresh bac_advancements score was not zero for " + caseKey + ": " + scoreBefore);
        boolean abilityLockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAMPFIRE);
        require(abilityLockedBefore, "USE_CAMPFIRE was not locked before unlock fixture for " + caseKey);

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        scoreboard.getOrCreatePlayerScore(player, objective).set(83);
        var scoreInfoAfter = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = scoreInfoAfter == null ? 0 : scoreInfoAfter.value();
        require(scoreAfter == 83, "Scoreboard unlock fixture did not produce score 83 for " + caseKey);
        boolean abilityLockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAMPFIRE);
        require(!abilityLockedAfter, "USE_CAMPFIRE remained locked after scoreboard unlock fixture for " + caseKey);
        var criterionAfterUnlock = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterionAfterUnlock != null, "PLACE_FOOD criterion disappeared after unlock fixture for " + caseKey);
        require(!criterionAfterUnlock.isDone(), "Scoreboard unlock fixture completed PLACE_FOOD criterion for " + caseKey);
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, level, pos);
        require(!lockedLandmark, "Campfire fixture position is inside a locked landmark for " + caseKey);

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        var campfireState = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
        level.setBlockAndUpdate(pos, campfireState);
        require(level.getBlockEntity(pos) instanceof CampfireBlockEntity, "Missing campfire block entity for " + caseKey);
        CampfireBlockEntity campfire = (CampfireBlockEntity) level.getBlockEntity(pos);
        var before = level.getBlockState(pos);
        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag for " + caseKey);
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        require(before.is(tag), "Campfire is outside catalog block tag for " + caseKey);
        boolean litBefore = before.getValue(CampfireBlock.LIT);
        require(!litBefore, "PLACE_FOOD fixture must use an unlit campfire for " + caseKey);

        int chosenSlot = -1;
        for (int slotIndex = 0; slotIndex < campfire.getItems().size(); slotIndex++) {
            require(campfire.getItems().get(slotIndex).isEmpty(), "Campfire slot " + slotIndex + " was not empty for " + caseKey);
            if (chosenSlot < 0) chosenSlot = slotIndex;
        }
        require(campfire.getItems().size() == 4 && chosenSlot >= 0, "Campfire did not expose four empty cooking slots for " + caseKey);
        String slotsBefore = campfireSlotSummary(campfire);

        String heldItemId = definition.get("heldItem").getAsString();
        Identifier heldItemIdentifier = Identifier.parse(heldItemId);
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered: " + heldItemId);
        Item catalogItem = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        player.getInventory().clearContent();
        ItemStack interactionStack = new ItemStack(catalogItem, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, interactionStack);
        require(player.getMainHandItem() == interactionStack, "Could not retain exact interaction stack for " + caseKey);
        require(interactionStack.is(catalogItem) && interactionStack.getCount() == 1, "Wrong catalog-bound held stack for " + caseKey);

        boolean placementAllowed = level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(interactionStack);
        require(placementAllowed, "CAMPFIRE_INPUT rejected " + heldItemId + " for " + caseKey);
        var recipeInput = new SingleRecipeInput(interactionStack);
        var recipeHolder = level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, recipeInput, level);
        boolean campfireRecipe = recipeHolder.isPresent();
        require(campfireRecipe, "No CAMPFIRE_COOKING recipe matched " + heldItemId + " for " + caseKey);
        var recipe = recipeHolder.get().value();
        String recipeId = recipeHolder.get().id().identifier().toString();
        require(recipe.getType() == RecipeType.CAMPFIRE_COOKING, "Wrong recipe type for " + caseKey + ": " + recipeId);

        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        var criterionBeforeInteraction = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterionBeforeInteraction != null, "PLACE_FOOD criterion absent before interaction for " + caseKey);
        require(!criterionBeforeInteraction.isDone(), "PLACE_FOOD criterion already done before interaction for " + caseKey);
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        require(interactionStackCountBefore == 1, "Interaction stack must start at one for " + caseKey);

        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterionBeforeInteraction.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("scoreboardObjective", "bac_advancements");
        receipt.addProperty("obtainedAdvancementsBefore", scoreBefore);
        receipt.addProperty("abilityLockedBefore", abilityLockedBefore);
        receipt.addProperty("obtainedAdvancementsAfter", scoreAfter);
        receipt.addProperty("abilityLockedAfter", abilityLockedAfter);
        receipt.addProperty("criterionAfterUnlock", criterionAfterUnlock.isDone());
        receipt.addProperty("lockedLandmark", lockedLandmark);
        receipt.addProperty("campfireLitBefore", litBefore);
        JsonObject proof = new JsonObject();
        proof.addProperty("placementAllowed", placementAllowed);
        proof.addProperty("campfireRecipe", campfireRecipe);
        proof.addProperty("recipeId", recipeId);
        proof.addProperty("recipeCookingTime", recipe.cookingTime());
        proof.addProperty("useCampfireUnlocked", !abilityLockedAfter);
        proof.addProperty("lockedLandmark", lockedLandmark);
        proof.addProperty("slotEmptyBefore", campfire.getItems().get(chosenSlot).isEmpty());
        proof.addProperty("slotIndex", chosenSlot);
        proof.addProperty("slotsBefore", slotsBefore);
        proof.addProperty("litBefore", litBefore);

        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(level.getBlockEntity(pos) == campfire, "Campfire block entity was replaced for " + caseKey);
        int populatedSlots = 0;
        int actualSlot = -1;
        for (int slotIndex = 0; slotIndex < campfire.getItems().size(); slotIndex++) {
            if (!campfire.getItems().get(slotIndex).isEmpty()) {
                populatedSlots++;
                actualSlot = slotIndex;
            }
        }
        ItemStack inserted = actualSlot < 0 ? ItemStack.EMPTY : campfire.getItems().get(actualSlot);
        boolean semanticMutation = campfire.getItems().size() == 4
            && populatedSlots == 1
            && actualSlot == chosenSlot
            && inserted.is(catalogItem)
            && inserted.getCount() == 1;
        if (result.consumesAction() && !semanticMutation) {
            throw new IllegalStateException("FAILED_PLACE_FOOD_CONSUME_WITHOUT_PLACEMENT: caseKey=" + caseKey
                + ", abilityUnlocked=" + !abilityLockedAfter
                + ", lockedLandmark=" + lockedLandmark
                + ", recipe=" + campfireRecipe
                + ", slotsBefore=" + slotsBefore
                + ", slotsAfter=" + campfireSlotSummary(campfire)
                + ", interactionStackBefore=" + interactionStackCountBefore
                + ", interactionStackAfter=" + interactionStack.getCount()
                + ", criterionBefore=" + criterionBeforeInteraction.isDone()
                + ", criterionAfter=" + (afterCriterion != null && afterCriterion.isDone()));
        }
        require(result.consumesAction(), "Interaction did not consume action for " + caseKey);
        require(semanticMutation, "Campfire food slot did not change from empty to " + heldItemId + " for " + caseKey);
        require(inserted.is(catalogItem) && inserted.getCount() == 1, "Inserted campfire food was not " + heldItemId + " x1 for " + caseKey);
        require(actualSlot == chosenSlot, "Unexpected campfire cooking slot changed for " + caseKey);
        for (int slotIndex = 0; slotIndex < campfire.getItems().size(); slotIndex++) {
            if (slotIndex != chosenSlot) require(campfire.getItems().get(slotIndex).isEmpty(), "Unexpected campfire slot populated: " + slotIndex + " for " + caseKey);
        }
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside catalog campfire tag for " + caseKey);
        require(afterCriterion != null, "PLACE_FOOD criterion disappeared after interaction for " + caseKey);
        require(afterCriterion.isDone(), "PLACE_FOOD criterion did not become true for " + caseKey);

        // Read the selected hand only after useItemOn and all synchronous criterion callbacks have completed.
        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        require(interactionStackCountAfter == 0, "PLACE_FOOD interaction stack not consumed for " + caseKey);
        String selectedHandItemAfter = selectedHandAfter.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(selectedHandAfter.getItem()).toString();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        String slotsAfter = campfireSlotSummary(campfire);
        proof.addProperty("slotItemAfter", itemId(inserted));
        proof.addProperty("slotCountAfter", inserted.getCount());
        proof.addProperty("slotsAfter", slotsAfter);
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild());
        receipt.addProperty("productionPreconditions", !abilityLockedAfter && !lockedLandmark && placementAllowed && campfireRecipe);
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag));
        receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString());
        receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", semanticMutation);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }

    private static String campfireSlotSummary(CampfireBlockEntity campfire) {
        StringBuilder summary = new StringBuilder("[");
        for (int slotIndex = 0; slotIndex < campfire.getItems().size(); slotIndex++) {
            if (slotIndex > 0) summary.append(", ");
            ItemStack stack = campfire.getItems().get(slotIndex);
            summary.append(slotIndex).append('=').append(itemId(stack)).append('x').append(stack.getCount());
        }
        return summary.append(']').toString();
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private enum BoneMealAttemptResult { GROWTH, RANDOM_MISS }

    private static void scheduleBoneMealAttempt(GameTestHelper helper, JsonObject definition, int attemptIndex) {
        if (attemptIndex > BONE_MEAL_MAX_ATTEMPTS) {
            helper.fail("BONE_MEAL_RANDOM_RETRY_EXHAUSTED: probability=" + BONE_MEAL_SUCCESS_PROBABILITY
                + ", maxAttempts=" + BONE_MEAL_MAX_ATTEMPTS
                + ", allFailureProbability=" + Math.pow(1.0d - BONE_MEAL_SUCCESS_PROBABILITY, BONE_MEAL_MAX_ATTEMPTS));
            return;
        }
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-bone-meal-" + attemptIndex);
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                boolean retry = false;
                try {
                    BoneMealAttemptResult result = executeBoneMealAttempt(helper, player, connection, definition, attemptIndex);
                    if (result == BoneMealAttemptResult.GROWTH) green = true;
                    else retry = true;
                } catch (Throwable e) {
                    helper.fail("GROW_OAK_SAPLING case " + caseKey(definition) + " attempt " + attemptIndex + ": " + e);
                } finally {
                    cleanup(helper, player, connection);
                    if (retry) {
                        try {
                            clearBoneMealFixture(helper.getLevel(), boneMealFixturePosition(helper));
                        } catch (Throwable e) {
                            retry = false;
                            helper.fail("GROW_OAK_SAPLING fixture cleanup attempt " + attemptIndex + ": " + e);
                        }
                    }
                }
                if (green) helper.succeed();
                else if (retry) helper.runAfterDelay(1, () -> scheduleBoneMealAttempt(helper, definition, attemptIndex + 1));
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("GROW_OAK_SAPLING case " + caseKey(definition) + " attempt " + attemptIndex + " setup: " + e);
        }
    }

    private static BoneMealAttemptResult executeBoneMealAttempt(
        GameTestHelper helper, ServerPlayer player, Connection connection, JsonObject definition, int attemptIndex) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String advancementId = definition.get("advancementId").getAsString();
        String criterionName = definition.get("criterion").getAsString();
        String caseKey = advancementId + "#" + criterionName;
        require("GROW_OAK_SAPLING".equals(definition.get("action").getAsString()), "Wrong action for " + caseKey);
        require("blazeandcave:farming/one_course_meal".equals(advancementId) && "bone_meal".equals(criterionName), "Unsupported bone-meal case: " + caseKey);
        require("minecraft:bone_meal".equals(definition.get("heldItem").getAsString()), "Wrong held item for " + caseKey);
        require("#minecraft:logs_that_burn".equals(definition.get("blockTag").getAsString()), "Wrong bone-meal block tag for " + caseKey);
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer for " + caseKey);
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined for " + caseKey);
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered for " + caseKey);
        require(player.connection.hasClientLoaded(), "Client not loaded for " + caseKey);
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required for " + caseKey);

        BlockPos pos = boneMealFixturePosition(helper);
        clearBoneMealFixture(level, pos);
        BlockState supportBefore = Blocks.DIRT.defaultBlockState();
        level.setBlockAndUpdate(pos.below(), supportBefore);
        BlockState saplingState = Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        level.setBlockAndUpdate(pos, saplingState);
        BlockState before = level.getBlockState(pos);
        require(before.is(Blocks.OAK_SAPLING), "Fixture is not an oak sapling for " + caseKey);
        require(before.getValue(SaplingBlock.STAGE) == 1, "Fixture oak sapling is not stage 1 for " + caseKey);
        require(level.getBlockState(pos.below()).is(Blocks.DIRT), "Missing ordinary survival support for " + caseKey);
        boolean planted = before.is(Blocks.OAK_SAPLING);
        boolean validSupport = before.canSurvive(level, pos);
        require(validSupport, "Oak sapling cannot survive on fixture support for " + caseKey);
        require(saplingState.getBlock() instanceof SaplingBlock, "Oak sapling is not bonemealable for " + caseKey);
        SaplingBlock sapling = (SaplingBlock) saplingState.getBlock();
        var minimumHeight = TreeGrower.OAK.getMinimumHeight(level);
        require(minimumHeight.isPresent(), "Oak grower has no minimum height for " + caseKey);
        require(level.isInsideBuildHeight(pos.above(minimumHeight.getAsInt())), "Oak growth exceeds world build height for " + caseKey);
        require(sapling.isValidBonemealTarget(level, pos, saplingState), "Oak sapling is not a valid bonemeal target for " + caseKey);
        boolean clearGrowthVolume = isBoneMealGrowthVolumeClear(level, pos);
        require(clearGrowthVolume, "Oak growth volume is not clear for " + caseKey);

        String blockTagName = definition.get("blockTag").getAsString();
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        var advancement = server.getAdvancements().get(Identifier.parse(advancementId));
        require(advancement != null, "Missing live advancement for " + caseKey);
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        if (criterion == null) throw new IllegalStateException("FAILED_BONE_MEAL_CRITERION_ABSENT: " + caseKey);
        if (criterion.isDone()) throw new IllegalStateException("FAILED_BONE_MEAL_CRITERION_ALREADY_DONE: " + caseKey);

        player.getInventory().clearContent();
        Identifier heldItemIdentifier = Identifier.parse(definition.get("heldItem").getAsString());
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered for " + caseKey);
        Item catalogItem = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        require(catalogItem == Items.BONE_MEAL, "Catalog bone-meal item is not minecraft:bone_meal for " + caseKey);
        ItemStack interactionStack = new ItemStack(catalogItem, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, interactionStack);
        require(player.getMainHandItem() == interactionStack, "Could not retain exact bone-meal interaction stack for " + caseKey);
        require(interactionStack.is(catalogItem) && interactionStack.getCount() == 1, "Wrong catalog-bound bone-meal stack for " + caseKey);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);

        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        boolean criterionBeforeInteraction = criterion.isDone();
        require(interactionStackCountBefore == 1, "Bone-meal interaction stack must start at one for " + caseKey);
        require(!criterionBeforeInteraction, "Bone-meal criterion was completed before interaction for " + caseKey);
        JsonObject proof = new JsonObject();
        proof.addProperty("planted", planted);
        proof.addProperty("validSupport", validSupport);
        proof.addProperty("clearGrowthVolume", clearGrowthVolume);
        proof.addProperty("mutationPosition", pos.toShortString());
        proof.addProperty("mutationBlockBefore", BuiltInRegistries.BLOCK.getKey(before.getBlock()).toString());
        proof.addProperty("saplingStageBefore", before.getValue(SaplingBlock.STAGE));
        proof.addProperty("treeMinimumHeight", minimumHeight.getAsInt());
        proof.addProperty("clearVolumeRadius", BONE_MEAL_CLEAR_RADIUS);
        proof.addProperty("clearVolumeHeight", BONE_MEAL_CLEAR_HEIGHT);
        proof.addProperty("supportBlockBefore", supportBefore.toString());

        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(result.consumesAction(), "Bone-meal interaction did not consume action for " + caseKey);
        require(afterCriterion != null, "Bone-meal criterion disappeared after interaction for " + caseKey);
        int interactionStackCountAfter = interactionStack.getCount();
        require(interactionStackCountAfter == 0, "Bone-meal interaction stack was not consumed for " + caseKey);

        BlockState after = level.getBlockState(pos);
        BlockState supportAfter = level.getBlockState(pos.below());
        boolean noGrowth = after.is(Blocks.OAK_SAPLING)
            && after.getValue(SaplingBlock.STAGE) == 1
            && isBoneMealGrowthVolumeClear(level, pos);
        int oakLogCount = countBlocksInBoneMealVolume(level, pos, Blocks.OAK_LOG);
        int oakLeafCount = countBlocksInBoneMealVolume(level, pos, Blocks.OAK_LEAVES);
        boolean runtimeTagMembership = after.is(tag);
        boolean successfulTreeGrowth = after.is(Blocks.OAK_LOG)
            && runtimeTagMembership
            && oakLogCount > 0
            && oakLeafCount > 0;
        if (noGrowth) {
            require(!successfulTreeGrowth, "Random-miss fixture unexpectedly contains tree growth for " + caseKey);
            return BoneMealAttemptResult.RANDOM_MISS;
        }
        require(afterCriterion.isDone(), "Bone-meal criterion did not become true for " + caseKey);
        require(successfulTreeGrowth, "Bone-meal interaction produced an unexpected partial tree state for " + caseKey
            + ": blockStateAfter=" + after + ", oakLogCount=" + oakLogCount + ", oakLeafCount=" + oakLeafCount);
        require(level.getBlockState(pos).is(tag), "Post-use tree origin is outside catalog runtime tag for " + caseKey);
        require(supportAfter.is(Blocks.DIRT), "Tree growth did not preserve ordinary support for " + caseKey);

        ItemStack selectedHandAfter = player.getMainHandItem();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        String selectedHandItemAfter = itemId(selectedHandAfter);
        boolean semanticMutation = before.is(Blocks.OAK_SAPLING)
            && before.getValue(SaplingBlock.STAGE) == 1
            && after.is(Blocks.OAK_LOG)
            && runtimeTagMembership
            && oakLogCount > 0
            && oakLeafCount > 0;
        require(semanticMutation, "Bone-meal interaction did not perform genuine oak tree growth for " + caseKey);

        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterionBeforeInteraction);
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("attemptIndex", attemptIndex);
        receipt.addProperty("maxAttempts", BONE_MEAL_MAX_ATTEMPTS);
        receipt.addProperty("randomSuccessProbability", BONE_MEAL_SUCCESS_PROBABILITY);
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild());
        receipt.addProperty("productionPreconditions", validSupport && clearGrowthVolume);
        receipt.addProperty("runtimeTagMembership", runtimeTagMembership);
        receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", after.toString());
        receipt.addProperty("interactionResult", result.toString());
        receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", semanticMutation);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        proof.addProperty("successfulTreeGrowth", successfulTreeGrowth);
        proof.addProperty("mutationBlockAfter", BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString());
        proof.addProperty("treeLogCount", oakLogCount);
        proof.addProperty("treeLeafCount", oakLeafCount);
        proof.addProperty("supportBlockAfter", supportAfter.toString());
        proof.addProperty("runtimeTagMembershipAfter", runtimeTagMembership);
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
        return BoneMealAttemptResult.GROWTH;
    }

    private static BlockPos boneMealFixturePosition(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(1, 2, 1));
    }

    private static void clearBoneMealFixture(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        for (int x = -BONE_MEAL_CLEAR_RADIUS; x <= BONE_MEAL_CLEAR_RADIUS; x++) {
            for (int y = 0; y <= BONE_MEAL_CLEAR_HEIGHT; y++) {
                for (int z = -BONE_MEAL_CLEAR_RADIUS; z <= BONE_MEAL_CLEAR_RADIUS; z++) {
                    BlockPos target = pos.offset(x, y, z);
                    require(level.isInsideBuildHeight(target), "Bone-meal clear volume exceeds world build height at " + target);
                    level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
    }

    private static boolean isBoneMealGrowthVolumeClear(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        for (int x = -BONE_MEAL_CLEAR_RADIUS; x <= BONE_MEAL_CLEAR_RADIUS; x++) {
            for (int y = 0; y <= BONE_MEAL_CLEAR_HEIGHT; y++) {
                for (int z = -BONE_MEAL_CLEAR_RADIUS; z <= BONE_MEAL_CLEAR_RADIUS; z++) {
                    BlockPos target = pos.offset(x, y, z);
                    if (target.equals(pos)) continue;
                    if (!level.getBlockState(target).isAir()) return false;
                }
            }
        }
        return true;
    }

    private static int countBlocksInBoneMealVolume(net.minecraft.server.level.ServerLevel level, BlockPos pos, Block block) {
        int count = 0;
        for (int x = -BONE_MEAL_CLEAR_RADIUS; x <= BONE_MEAL_CLEAR_RADIUS; x++) {
            for (int y = 0; y <= BONE_MEAL_CLEAR_HEIGHT; y++) {
                for (int z = -BONE_MEAL_CLEAR_RADIUS; z <= BONE_MEAL_CLEAR_RADIUS; z++) {
                    if (level.getBlockState(pos.offset(x, y, z)).is(block)) count++;
                }
            }
        }
        return count;
    }

    private static void schedulePlaceFoodCase(GameTestHelper helper, java.util.List<JsonObject> definitions, int index) {
        if (index == definitions.size()) {
            try {
                PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertPlaceFoodCoverage();
                helper.succeed();
            } catch (Throwable e) {
                helper.fail("PLACE_FOOD coverage: " + e);
            }
            return;
        }
        JsonObject definition = definitions.get(index);
        String caseKey = caseKey(definition);
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-" + definition.get("criterion").getAsString());
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executePlaceFood(helper, player, connection, definition);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("PLACE_FOOD case " + caseKey + ": " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.runAfterDelay(1, () -> schedulePlaceFoodCase(helper, definitions, index + 1));
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("PLACE_FOOD case " + caseKey + " setup: " + e);
        }
    }

    private static void scheduleDyeCase(GameTestHelper helper, java.util.List<JsonObject> definitions, int index) {
        if (index == definitions.size()) {
            try {
                PhaseABlockTagItemUsedOnBlockExecutionEvidence.assertDyeSignCoverage();
                helper.succeed();
            } catch (Throwable e) {
                helper.fail("DYE_SIGN coverage: " + e);
            }
            return;
        }
        JsonObject definition = definitions.get(index);
        String caseKey = caseKey(definition);
        var server = helper.getLevel().getServer();
        var profile = new GameProfile(UUID.randomUUID(), "block-tag-" + definition.get("criterion").getAsString());
        var player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        try {
            new EmbeddedChannel(connection);
            server.getConnection().getConnections().add(connection);
            server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            helper.runAfterDelay(2, () -> {
                boolean green = false;
                try {
                    executeDyeCase(helper, player, connection, definition);
                    green = true;
                } catch (Throwable e) {
                    helper.fail("DYE_SIGN case " + caseKey + ": " + e);
                } finally {
                    cleanup(helper, player, connection);
                }
                if (green) helper.runAfterDelay(1, () -> scheduleDyeCase(helper, definitions, index + 1));
            });
        } catch (Throwable e) {
            cleanup(helper, player, connection);
            helper.fail("DYE_SIGN case " + caseKey + " setup: " + e);
        }
    }

    private static void executeDyeCase(GameTestHelper helper, ServerPlayer player, Connection connection, JsonObject definition) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        String caseKey = caseKey(definition);
        require("DYE_SIGN".equals(definition.get("action").getAsString()), "Not a DYE_SIGN definition");
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer");
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined");
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered");
        require(player.connection.hasClientLoaded(), "Client not loaded");
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof SignBlockEntity, "Missing sign");
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
        require(!sign.isWaxed(), "Fixture must be fresh and unwaxed");
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        boolean activeSideFront = sign.isFacingFrontText(player);
        DyeColor expectedColor = catalogDyeColor(definition);
        DyeColor initialColor = expectedColor == DyeColor.BLACK ? DyeColor.WHITE : DyeColor.BLACK;
        require(initialColor != expectedColor, "Fixture initial color is not contrasting for " + caseKey);
        require(sign.updateText(text -> text.setMessage(0, Component.literal("dye sign " + definition.get("criterion").getAsString())).setColor(initialColor), activeSideFront), "Could not set fixture text");
        SignText beforeText = sign.getText(activeSideFront);
        boolean textPresent = beforeText.hasMessage(player);
        require(textPresent, "Active sign side has no text");
        require(!sign.getText(!activeSideFront).hasMessage(player), "Text fixture leaked to inactive sign side");
        boolean waxedBefore = sign.isWaxed();
        DyeColor colorBefore = beforeText.getColor();
        require(!waxedBefore, "Fixture must remain unwaxed before dye");
        require(colorBefore != expectedColor, "Fixture color unexpectedly matches expected dye");

        String heldItemId = definition.get("heldItem").getAsString();
        Identifier heldItemIdentifier = Identifier.parse(heldItemId);
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered: " + heldItemId);
        Item catalogItem = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        require(catalogItem instanceof DyeItem, "Catalog item is not a DyeItem: " + heldItemId);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(catalogItem, 1));
        ItemStack interactionStack = player.getMainHandItem();
        require(interactionStack.getItem() == catalogItem && interactionStack.getCount() == 1, "Wrong catalog-bound held stack");
        require(interactionStack.get(DataComponents.DYE) == expectedColor, "Held item dye component does not match catalog color");

        var advancement = server.getAdvancements().get(Identifier.parse(definition.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement");
        String criterionName = definition.get("criterion").getAsString();
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(criterion != null && !criterion.isDone(), "Criterion must start false");
        String blockTagName = definition.get("blockTag").getAsString();
        require(blockTagName.startsWith("#"), "Invalid catalog block tag");
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse(blockTagName.substring(1)));
        var before = level.getBlockState(pos);
        require(before.is(tag), "Sign is outside catalog block tag");
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = interactionStack.getCount();
        JsonObject receipt = new JsonObject();
        copyCatalogBinding(definition, receipt);
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        receipt.addProperty("activeSideFront", activeSideFront);
        receipt.addProperty("expectedColor", expectedColor.getName());
        JsonObject proof = new JsonObject();
        proof.addProperty("textPresent", textPresent);
        proof.addProperty("waxedBefore", waxedBefore);
        proof.addProperty("colorBefore", colorBefore.getName());
        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, interactionStack, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterionName);
        require(result.consumesAction(), "Interaction did not consume action");
        require(level.getBlockEntity(pos) == sign, "Sign block entity was replaced");
        require(!sign.isWaxed(), "Dye interaction waxed the sign");
        SignText afterText = sign.getText(activeSideFront);
        require(afterText.hasMessage(player), "Active sign side lost its text");
        DyeColor colorAfter = afterText.getColor();
        require(colorAfter == expectedColor && colorBefore != colorAfter, "Sign color did not change to the catalog dye");
        require(afterCriterion != null && afterCriterion.isDone(), "Criterion did not become true");
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside catalog block tag");

        // Read the selected hand only after useItemOn and all synchronous criterion callbacks have completed.
        ItemStack selectedHandAfter = player.getMainHandItem();
        int interactionStackCountAfter = interactionStack.getCount();
        String selectedHandItemAfter = selectedHandAfter.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(selectedHandAfter.getItem()).toString();
        boolean sameStackReference = interactionStack == selectedHandAfter;
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName()); receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild()); receipt.addProperty("productionPreconditions", true);
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag)); receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString()); receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", colorBefore != colorAfter);
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", sameStackReference);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", selectedHandAfter.getCount());
        proof.addProperty("waxedAfter", sign.isWaxed());
        proof.addProperty("colorAfter", colorAfter.getName());
        receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordGreen(receipt);
    }

    private static DyeColor catalogDyeColor(JsonObject definition) {
        String heldItemId = definition.get("heldItem").getAsString();
        Identifier heldItemIdentifier = Identifier.parse(heldItemId);
        require(BuiltInRegistries.ITEM.containsKey(heldItemIdentifier), "Catalog item is not registered: " + heldItemId);
        Item item = BuiltInRegistries.ITEM.getValue(heldItemIdentifier);
        require(item instanceof DyeItem, "Catalog item is not a DyeItem: " + heldItemId);
        DyeColor color = new ItemStack(item, 1).get(DataComponents.DYE);
        require(color != null, "Catalog DyeItem has no DYE component: " + heldItemId);
        String prefix = "minecraft:";
        String suffix = "_dye";
        require(heldItemId.startsWith(prefix) && heldItemId.endsWith(suffix), "Invalid catalog dye item: " + heldItemId);
        String idColor = heldItemId.substring(prefix.length(), heldItemId.length() - suffix.length());
        require(idColor.equals(color.getName()), "Catalog item/color mismatch: " + heldItemId);
        return color;
    }

    private static void copyCatalogBinding(JsonObject definition, JsonObject receipt) {
        for (String field : java.util.List.of("advancementId", "criterion", "requirementGroupIndex", "trigger", "blockTag", "blockTagSampling", "heldItem", "action")) {
            receipt.add(field, definition.get(field).deepCopy());
        }
    }

    private static String caseKey(JsonObject definition) {
        return definition.get("advancementId").getAsString() + "#" + definition.get("criterion").getAsString();
    }

    private static void executeCanary(GameTestHelper helper, ServerPlayer player, Connection connection) throws Exception {
        var level = helper.getLevel(); var server = level.getServer();
        require(player.getClass() == ServerPlayer.class, "Not plain ServerPlayer");
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Not joined");
        require(server.getConnection().getConnections().contains(connection), "Connection unregistered");
        require(player.connection.hasClientLoaded(), "Client not loaded");
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator() && player.mayBuild(), "SURVIVAL build permission required");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
        require(level.getBlockEntity(pos) instanceof SignBlockEntity, "Missing sign");
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
        require(!sign.isWaxed(), "Fixture must be fresh and unwaxed");
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 1));
        JsonObject definition = PhaseABlockTagItemUsedOnBlockExecutionEvidence.canaryDefinition();
        JsonObject receipt = new JsonObject();
        for (String field : java.util.List.of("advancementId", "criterion", "requirementGroupIndex", "trigger", "blockTag", "blockTagSampling", "heldItem", "action")) receipt.add(field, definition.get(field).deepCopy());
        var advancement = server.getAdvancements().get(Identifier.parse(receipt.get("advancementId").getAsString()));
        require(advancement != null, "Missing live advancement");
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion("honeycomb");
        require(criterion != null && !criterion.isDone(), "Criterion must start false");
        var tag = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:all_signs"));
        var before = level.getBlockState(pos);
        require(before.is(tag), "Sign is outside runtime all_signs tag");
        // This is the exact object passed to useItemOn; completion rewards may replace its now-empty slot afterward.
        var heldBefore = player.getMainHandItem();
        require(heldBefore.is(Items.HONEYCOMB) && heldBefore.getCount() == 1, "Wrong held stack");
        int selectedHandSlot = player.getInventory().getSelectedSlot();
        int interactionStackCountBefore = heldBefore.getCount();
        receipt.addProperty("blockStateBefore", before.toString());
        receipt.addProperty("criterionBefore", criterion.isDone());
        receipt.addProperty("heldCountBefore", interactionStackCountBefore);
        receipt.addProperty("interactionStackCountBefore", interactionStackCountBefore);
        receipt.addProperty("selectedHandSlot", selectedHandSlot);
        JsonObject proof = new JsonObject(); proof.addProperty("waxedBefore", sign.isWaxed());
        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, heldBefore, InteractionHand.MAIN_HAND, hit);
        var afterCriterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion("honeycomb");
        require(result.consumesAction(), "Interaction did not consume action");
        require(level.getBlockEntity(pos) == sign && sign.isWaxed(), "Sign did not become waxed");
        var heldAfter = player.getMainHandItem();
        int interactionStackCountAfter = heldBefore.getCount();
        String selectedHandItemAfter = heldAfter.isEmpty() ? "minecraft:air" : heldAfter.getItem().toString();
        require(interactionStackCountAfter == 0, "Honeycomb interaction stack not consumed: result=" + result
            + ", gameMode=" + player.gameMode()
            + ", instabuild=" + player.getAbilities().instabuild
            + ", hasInfiniteMaterials=" + player.hasInfiniteMaterials()
            + ", heldBeforeCountBefore=" + interactionStackCountBefore
            + ", interactionStackCountAfter=" + interactionStackCountAfter
            + ", heldAfterItem=" + selectedHandItemAfter
            + ", heldAfterCount=" + heldAfter.getCount()
            + ", sameStackReference=" + (heldBefore == heldAfter)
            + ", selectedHandSlot=" + selectedHandSlot
            + ", waxedBefore=" + proof.get("waxedBefore").getAsBoolean()
            + ", waxedAfter=" + sign.isWaxed()
            + ", criterionAfter=" + (afterCriterion != null && afterCriterion.isDone()));
        require(afterCriterion != null && afterCriterion.isDone(), "Criterion did not become true");
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", player.getClass().getName()); receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("gameMode", player.gameMode().getName().toUpperCase(java.util.Locale.ROOT));
        receipt.addProperty("joined", server.getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", server.getConnection().getConnections().contains(connection));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded()); receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", player.mayBuild()); receipt.addProperty("productionPreconditions", true);
        require(level.getBlockState(pos).is(tag), "Post-use clicked block is outside runtime tag");
        receipt.addProperty("runtimeTagMembership", level.getBlockState(pos).is(tag)); receipt.addProperty("clickedPosition", pos.toShortString());
        receipt.addProperty("blockStateAfter", level.getBlockState(pos).toString());
        receipt.addProperty("interactionResult", result.toString()); receipt.addProperty("interactionConsumesAction", result.consumesAction());
        receipt.addProperty("semanticMutation", !proof.get("waxedBefore").getAsBoolean() && sign.isWaxed());
        receipt.addProperty("criterionAfter", afterCriterion.isDone());
        receipt.addProperty("heldCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackCountAfter", interactionStackCountAfter);
        receipt.addProperty("interactionStackConsumed", interactionStackCountAfter == 0);
        receipt.addProperty("sameStackReference", heldBefore == heldAfter);
        receipt.addProperty("selectedHandItemAfter", selectedHandItemAfter);
        receipt.addProperty("selectedHandCountAfter", heldAfter.getCount());
        proof.addProperty("waxedAfter", sign.isWaxed()); receipt.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidence.recordCanaryGreen(receipt);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void cleanup(GameTestHelper helper, ServerPlayer player, Connection connection) {
        var server = helper.getLevel().getServer();
        try {
            if (server.getPlayerList().getPlayer(player.getUUID()) == player) server.getPlayerList().remove(player);
        } finally {
            try { connection.disconnect(Component.literal("GameTest cleanup")); }
            finally { server.getConnection().getConnections().remove(connection); }
        }
    }
    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, helper);
    }
}
