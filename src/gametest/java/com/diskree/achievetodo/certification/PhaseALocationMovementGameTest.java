package com.diskree.achievetodo.certification;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTestHelper;
import io.netty.channel.embedded.EmbeddedChannel;

import java.lang.reflect.Method;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PhaseALocationMovementGameTest implements CustomTestMethodInvoker {
    private static final Identifier MOOSHROOM_KINGDOM_ID = Identifier.parse("blazeandcave:animal/mooshroom_kingdom");
    private static final Identifier COLD_FEET_ID = Identifier.parse("blazeandcave:biomes/cold_feet");
    private static final Identifier ENCHANTED_FOREST_ID = Identifier.parse("blazeandcave:biomes/enchanted_forest");
    private static final Identifier HIGH_FEET_ID = Identifier.parse("blazeandcave:biomes/high_feet");
    private static final Identifier ONE_WITH_THE_FOREST_ID = Identifier.parse("blazeandcave:biomes/one_with_the_forest");
    private static final Identifier OVERGROWN_ID = Identifier.parse("blazeandcave:biomes/overgrown");
    private static final Identifier PRETTY_IN_PINK_ID = Identifier.parse("blazeandcave:biomes/pretty_in_pink");
    private static final Identifier THE_GREAT_BLOCKY_REEF_ID = Identifier.parse("blazeandcave:biomes/the_great_blocky_reef");
    private static final Identifier THE_MIGHTY_JUNGLE_ID = Identifier.parse("blazeandcave:biomes/the_mighty_jungle");
    private static final Identifier THE_SEA_CALLS_YOU_ID = Identifier.parse("blazeandcave:biomes/the_sea_calls_you");
    private static final Identifier THERES_A_ZOMBIE_ON_THE_LAWN_ID = Identifier.parse("blazeandcave:biomes/theres_a_zombie_on_the_lawn");
    private static final Identifier WARM_FEET_ID = Identifier.parse("blazeandcave:biomes/warm_feet");
    private static final Identifier WET_FEET_ID = Identifier.parse("blazeandcave:biomes/wet_feet");
    private static final Identifier VOID_WALKER_ID = Identifier.parse("blazeandcave:end/void_walker");
    private static final Identifier UNENDING_HELL_ID = Identifier.parse("blazeandcave:end/unending_hell");
    private static final Identifier DEEP_SLATE_NINE_ID = Identifier.parse("blazeandcave:mining/deep_slate_nine");
    private static final Identifier ENTER_END_GATEWAY_ID = Identifier.parse("minecraft:end/enter_end_gateway");
    private static final Identifier ADVENTURING_TIME_ID = Identifier.parse("minecraft:adventure/adventuring_time");
    private static final Identifier EXPLORE_NETHER_ID = Identifier.parse("minecraft:nether/explore_nether");

    static {
        Set<Identifier> trackedAdvancements = new LinkedHashSet<>(CertifiedLocationMovementMatrix.automatedAdvancementIds());
        trackedAdvancements.add(DEEP_SLATE_NINE_ID);
        trackedAdvancements.add(ENTER_END_GATEWAY_ID);
        LocationTriggerProbe.setTrackedAdvancements(trackedAdvancements);
    }

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record DimensionProbeCase(
        Identifier advancementId,
        String criterion,
        String dimensionId,
        ResourceKey<Level> dimension,
        BlockPos target,
        boolean certifiedDimensionOnly
    ) {
    }

    // Static Fabric GameTest methods stay minimal; the certified matrix drives coverage underneath.
    @GameTest(maxTicks = 12000)
    public void biomeClusterMooshroomKingdom(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, MOOSHROOM_KINGDOM_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterColdFeet(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, COLD_FEET_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterEnchantedForest(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, ENCHANTED_FOREST_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterHighFeet(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, HIGH_FEET_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterOneWithTheForest(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, ONE_WITH_THE_FOREST_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterOvergrown(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, OVERGROWN_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterPrettyInPink(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, PRETTY_IN_PINK_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterTheGreatBlockyReef(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, THE_GREAT_BLOCKY_REEF_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterTheMightyJungle(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, THE_MIGHTY_JUNGLE_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterTheSeaCallsYou(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, THE_SEA_CALLS_YOU_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterTheresAZombieOnTheLawn(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, THERES_A_ZOMBIE_ON_THE_LAWN_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterWarmFeet(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, WARM_FEET_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterWetFeet(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, WET_FEET_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterVoidWalker(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, VOID_WALKER_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterAdventuringTime(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, ADVENTURING_TIME_ID);
    }

    @GameTest(maxTicks = 12000)
    public void biomeClusterExploreNether(GameTestHelper helper) {
        assertCertifiedAdvancement(helper, EXPLORE_NETHER_ID);
    }

    @GameTest(maxTicks = 12000)
    public void dimensionSliceDeepSlateNine(GameTestHelper helper) {
        runDimensionProbeCase(
            helper,
            new DimensionProbeCase(
                DEEP_SLATE_NINE_ID,
                "deepslate_level",
                "minecraft:overworld",
                Level.OVERWORLD,
                new BlockPos(0, 4, 0),
                false
            )
        );
    }

    @GameTest(maxTicks = 12000)
    public void dimensionSliceEnterEndGateway(GameTestHelper helper) {
        runDimensionProbeCase(
            helper,
            new DimensionProbeCase(
                ENTER_END_GATEWAY_ID,
                "outer_pos_x",
                "minecraft:the_end",
                Level.END,
                new BlockPos(751, 80, 0),
                false
            )
        );
    }

    private static void assertCertifiedAdvancement(GameTestHelper helper, Identifier advancementId) {
        List<CertifiedLocationMovementMatrix.CaseDefinition> cases = CertifiedLocationMovementMatrix.automatedSimpleCasesFor(advancementId);
        runCriterionCase(helper, cases, 0);
    }

    private static void runCriterionCase(
        GameTestHelper helper,
        List<CertifiedLocationMovementMatrix.CaseDefinition> cases,
        int caseIndex
    ) {
        if (caseIndex >= cases.size()) {
            helper.succeed();
            return;
        }
        CertifiedLocationMovementMatrix.CaseDefinition caseDefinition = cases.get(caseIndex);
        runBiomeVariant(helper, caseDefinition, 0, () -> {
            try {
                PhaseARuntimeExecutionEvidence.recordGreen(
                    PhaseARuntimeExecutionEvidence.projectRoot(),
                    caseDefinition.advancementId().toString(),
                    caseDefinition.criterion(),
                    PhaseARuntimeExecutionEvidence.LOCATION_MOVEMENT_FAMILY,
                    "PhaseALocationMovementGameTest"
                );
            } catch (Exception e) {
                helper.fail("Failed to record runtime evidence for " + caseDefinition.advancementId() + "#" + caseDefinition.criterion() + ": " + e.getMessage());
                return;
            }
            helper.runAfterDelay(1, () -> runCriterionCase(helper, cases, caseIndex + 1));
        });
    }

    private static void runBiomeVariant(
        GameTestHelper helper,
        CertifiedLocationMovementMatrix.CaseDefinition caseDefinition,
        int biomeIndex,
        Runnable onComplete
    ) {
        if (biomeIndex >= caseDefinition.biomeIds().size()) {
            onComplete.run();
            return;
        }
        String expectedBiomeId = caseDefinition.biomeIds().get(biomeIndex);
        ResourceKey<Biome> expectedBiome = ResourceKey.create(Registries.BIOME, Identifier.parse(expectedBiomeId));
        runCertifiedBiomeCase(helper, caseDefinition, expectedBiome, expectedBiomeId, () -> helper.runAfterDelay(1, () ->
            runBiomeVariant(helper, caseDefinition, biomeIndex + 1, onComplete)
        ));
    }

    private static void runCertifiedBiomeCase(
        GameTestHelper helper,
        CertifiedLocationMovementMatrix.CaseDefinition caseDefinition,
        ResourceKey<Biome> expectedBiome,
        String expectedBiomeId,
        Runnable onComplete
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            helper.setBiome(expectedBiome);

            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            if (!player.connection.hasClientLoaded()) {
                throw new IllegalStateException("Expected a production-equivalent loaded player lifecycle for " + describeCase(caseDefinition, expectedBiomeId));
            }
            if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
                throw new IllegalStateException("Expected a SURVIVAL joined player for " + describeCase(caseDefinition, expectedBiomeId));
            }
            if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
                throw new IllegalStateException("Expected joined ServerPlayer registration for " + describeCase(caseDefinition, expectedBiomeId));
            }
            if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
                throw new IllegalStateException("Expected scheduler connection registration for " + describeCase(caseDefinition, expectedBiomeId));
            }

            BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
            player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }

        JoinedPlayer scheduledJoinedPlayer = joinedPlayer;
        helper.runAfterDelay(61, () -> {
            String failure = null;
            try {
                ServerPlayer player = scheduledJoinedPlayer.player();
                LocationTriggerProbe.State probe = LocationTriggerProbe.get(player.getUUID());
                Holder<Biome> biome = helper.getLevel().getBiome(player.blockPosition());
                if (!biome.is(expectedBiome)) {
                    failure = "Expected biome " + expectedBiomeId + ", got " + biome.unwrapKey().map(Object::toString).orElse("unknown") +
                        " for " + describeCase(caseDefinition, expectedBiomeId);
                    return;
                }

                AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
                if (advancement == null) {
                    failure = "Missing advancement: " + caseDefinition.advancementId();
                    return;
                }

                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!probe.isLocationListenerRegistered(caseDefinition.advancementId())) {
                    failure = "LOCATION listener was not registered for " + describeCase(caseDefinition, expectedBiomeId) + " [" + probe.describe() + "]";
                    return;
                }
                if (probe.getLocationTriggerCalls() <= 0) {
                    failure = "CriteriaTriggers.LOCATION was never invoked for " + describeCase(caseDefinition, expectedBiomeId) + " [" + probe.describe() + "]";
                    return;
                }
                if (probe.getTickTriggerCalls() <= 0) {
                    failure = "Expected ordinary tick-driven scheduler lifecycle for " + describeCase(caseDefinition, expectedBiomeId) + " [" + probe.describe() + "]";
                    return;
                }
                if (progress.getCriterion(caseDefinition.criterion()) == null) {
                    failure = "Live AdvancementProgress did not expose criterion " + caseDefinition.criterion() + " for " + describeCase(caseDefinition, expectedBiomeId) + " [" + probe.describe() + "]";
                    return;
                }
                if (!progress.getCriterion(caseDefinition.criterion()).isDone()) {
                    failure = "Criterion " + caseDefinition.criterion() + " was not granted for " + describeCase(caseDefinition, expectedBiomeId) +
                        " on tick " + player.tickCount + " [" + probe.describe() + "]";
                }
            } finally {
                cleanupJoinedServerPlayer(helper, scheduledJoinedPlayer);
            }
            if (failure != null) {
                helper.fail(failure);
                return;
            }
            onComplete.run();
        });
    }

    private static void runDimensionProbeCase(GameTestHelper helper, DimensionProbeCase probeCase) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            if (!player.connection.hasClientLoaded()) {
                throw new IllegalStateException("Expected a production-equivalent loaded player lifecycle for " + describeDimensionCase(probeCase));
            }
            if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
                throw new IllegalStateException("Expected a SURVIVAL joined player for " + describeDimensionCase(probeCase));
            }
            if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
                throw new IllegalStateException("Expected joined ServerPlayer registration for " + describeDimensionCase(probeCase));
            }
            if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
                throw new IllegalStateException("Expected scheduler connection registration for " + describeDimensionCase(probeCase));
            }

            ServerLevel targetLevel = requiredLevel(helper.getLevel().getServer(), probeCase.dimension());
            prepareSafePlatform(targetLevel, probeCase.target());
            boolean teleported = player.teleportTo(
                targetLevel,
                probeCase.target().getX() + 0.5D,
                probeCase.target().getY(),
                probeCase.target().getZ() + 0.5D,
                Set.of(),
                0.0F,
                0.0F,
                false
            );
            if (!teleported) {
                throw new IllegalStateException("Cross-dimension teleport returned false for " + describeDimensionCase(probeCase));
            }
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }

        JoinedPlayer scheduledJoinedPlayer = joinedPlayer;
        helper.runAfterDelay(61, () -> {
            String failure = null;
            try {
                ServerPlayer player = scheduledJoinedPlayer.player();
                ServerLevel expectedLevel = requiredLevel(helper.getLevel().getServer(), probeCase.dimension());
                LocationTriggerProbe.State probe = LocationTriggerProbe.get(player.getUUID());
                ServerLevel actualLevel = (ServerLevel) player.level();
                if (actualLevel != expectedLevel) {
                    failure = "Expected player to remain in " + probeCase.dimensionId() + " for " + describeDimensionCase(probeCase) +
                        ", got " + String.valueOf(actualLevel.dimension());
                    return;
                }

                AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(probeCase.advancementId());
                if (advancement == null) {
                    failure = "Missing advancement: " + probeCase.advancementId();
                    return;
                }
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (!probe.isLocationListenerRegistered(probeCase.advancementId())) {
                    failure = "LOCATION listener was not registered for " + describeDimensionCase(probeCase) + " [" + probe.describe() + "]";
                    return;
                }
                if (probe.getLocationTriggerCalls() <= 0) {
                    failure = "CriteriaTriggers.LOCATION was never invoked for " + describeDimensionCase(probeCase) + " [" + probe.describe() + "]";
                    return;
                }
                if (probe.getTickTriggerCalls() <= 0) {
                    failure = "Expected ordinary tick-driven scheduler lifecycle for " + describeDimensionCase(probeCase) + " [" + probe.describe() + "]";
                    return;
                }
                if (progress.getCriterion(probeCase.criterion()) == null) {
                    failure = "Live AdvancementProgress did not expose criterion " + probeCase.criterion() + " for " + describeDimensionCase(probeCase) +
                        " [" + probe.describe() + "]";
                    return;
                }
                if (!progress.getCriterion(probeCase.criterion()).isDone()) {
                    failure = "Criterion " + probeCase.criterion() + " was not granted for " + describeDimensionCase(probeCase) +
                        " on tick " + player.tickCount + " [" + probe.describe() + "]";
                }
            } finally {
                cleanupJoinedServerPlayer(helper, scheduledJoinedPlayer);
            }
            if (failure != null) {
                helper.fail(failure);
                return;
            }
            helper.succeed();
        });
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ClientInformation clientInformation = ClientInformation.createDefault();
        UUID playerId = UUID.randomUUID();
        LocationTriggerProbe.reset(playerId);
        GameProfile profile = new GameProfile(playerId, "test-mock-player");
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
        server.getPlayerList().remove(joinedPlayer.player());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        server.getConnection().getConnections().remove(joinedPlayer.connection());
        LocationTriggerProbe.clear(joinedPlayer.player().getUUID());
    }

    private static ServerLevel requiredLevel(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            throw new IllegalStateException("Missing required test server level: " + dimension);
        }
        return level;
    }

    private static void prepareSafePlatform(ServerLevel targetLevel, BlockPos target) {
        targetLevel.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        targetLevel.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        targetLevel.setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
    }

    private static ResourceKey<Level> parseLevelKey(String dimensionId) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(dimensionId));
    }

    private static String describeCase(CertifiedLocationMovementMatrix.CaseDefinition caseDefinition, String expectedBiomeId) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion() + "@" + expectedBiomeId;
    }

    private static String describeDimensionCase(DimensionProbeCase probeCase) {
        return probeCase.advancementId() + "#" + probeCase.criterion() + "@" + probeCase.dimensionId();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, net.minecraft.world.level.block.Blocks.AIR);
        method.invoke(this, helper);
    }
}
