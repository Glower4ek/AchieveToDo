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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PhaseAStructureLocationGameTest implements CustomTestMethodInvoker {
    private static final BlockPos CANONICAL_TARGET = new BlockPos(32, 80, 32);

    static {
        LocationTriggerProbe.addTrackedAdvancements(
            StructureOnlyLocationInventory.allCases().stream().map(StructureOnlyLocationInventory.StructureOnlyCase::advancementId).distinct().toList()
        );
    }

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record RequiredStructureSelection(HolderSet<Structure> holderSet, Holder.Reference<Structure> holder) {
    }

    @GameTest(maxTicks = 12000)
    public void structureSliceVerifiedSubset(GameTestHelper helper) {
        runStructureProbeCases(helper, StructureOnlyLocationInventory.allCases(), 0);
    }

    private static void runStructureProbeCases(
        GameTestHelper helper,
        List<StructureOnlyLocationInventory.StructureOnlyCase> cases,
        int index
    ) {
        if (index >= cases.size()) {
            helper.succeed();
            return;
        }

        StructureOnlyLocationInventory.StructureOnlyCase structureCase = cases.get(index);
        JoinedPlayer joinedPlayer = null;
        SyntheticStructureHarness.InjectedStructure injectedStructure = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, structureCase);

            ServerLevel targetLevel = requiredLevel(helper.getLevel().getServer(), Level.OVERWORLD);
            prepareSafePlatform(targetLevel, CANONICAL_TARGET);
            RequiredStructureSelection requiredStructure = requiredStructure(targetLevel, structureCase);
            injectedStructure = SyntheticStructureHarness.inject(targetLevel, requiredStructure.holder(), CANONICAL_TARGET);

            boolean teleported = player.teleportTo(
                targetLevel,
                CANONICAL_TARGET.getX() + 0.5D,
                CANONICAL_TARGET.getY(),
                CANONICAL_TARGET.getZ() + 0.5D,
                Set.of(),
                0.0F,
                0.0F,
                false
            );
            if (!teleported) {
                throw new IllegalStateException("Cross-dimension teleport returned false for " + describeStructureCase(structureCase));
            }
            assertVanillaStructurePrecondition(player, targetLevel, requiredStructure, injectedStructure, structureCase);
        } catch (Throwable t) {
            closeInjectedStructure(injectedStructure);
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }

        JoinedPlayer scheduledJoinedPlayer = joinedPlayer;
        SyntheticStructureHarness.InjectedStructure scheduledInjectedStructure = injectedStructure;
        helper.runAfterDelay(61, () -> {
            String failure = null;
            try {
                failure = verifyGameplayGrant(helper, scheduledJoinedPlayer.player(), structureCase);
                if (failure == null) {
                    PhaseARuntimeExecutionEvidence.recordGreen(
                        PhaseARuntimeExecutionEvidence.projectRoot(),
                        structureCase.advancementId().toString(),
                        structureCase.criterion(),
                        PhaseARuntimeExecutionEvidence.STRUCTURE_ONLY_FAMILY,
                        "PhaseAStructureLocationGameTest"
                    );
                }
            } catch (Exception e) {
                failure = "Failed to record runtime evidence for " + describeStructureCase(structureCase) + ": " + e.getMessage();
            } finally {
                closeInjectedStructure(scheduledInjectedStructure);
                cleanupJoinedServerPlayer(helper, scheduledJoinedPlayer);
            }
            if (failure != null) {
                helper.fail(failure);
                return;
            }
            runStructureProbeCases(helper, cases, index + 1);
        });
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        StructureOnlyLocationInventory.StructureOnlyCase structureCase
    ) {
        ServerPlayer player = joinedPlayer.player();
        if (!player.connection.hasClientLoaded()) {
            throw new IllegalStateException("Expected a production-equivalent loaded player lifecycle for " + describeStructureCase(structureCase));
        }
        if (player.isSpectator() || player.gameMode() != GameType.SURVIVAL) {
            throw new IllegalStateException("Expected a SURVIVAL joined player for " + describeStructureCase(structureCase));
        }
        if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
            throw new IllegalStateException("Expected joined ServerPlayer registration for " + describeStructureCase(structureCase));
        }
        if (!helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection())) {
            throw new IllegalStateException("Expected scheduler connection registration for " + describeStructureCase(structureCase));
        }
    }

    private static void assertVanillaStructurePrecondition(
        ServerPlayer player,
        ServerLevel targetLevel,
        RequiredStructureSelection requiredStructure,
        SyntheticStructureHarness.InjectedStructure injectedStructure,
        StructureOnlyLocationInventory.StructureOnlyCase structureCase
    ) {
        StructureStart visibleStart = targetLevel.structureManager().getStructureWithPieceAt(
            player.blockPosition(),
            requiredStructure.holderSet()
        );
        if (!visibleStart.isValid()) {
            throw new IllegalStateException("Vanilla structure lookup did not return a valid start for " + describeStructureCase(structureCase));
        }
        if (visibleStart != injectedStructure.injectedStart()) {
            throw new IllegalStateException("Vanilla structure lookup returned a different StructureStart instance for " + describeStructureCase(structureCase));
        }
        if (visibleStart.getStructure() != requiredStructure.holder().value()) {
            throw new IllegalStateException("Vanilla structure lookup returned the wrong structure for " + describeStructureCase(structureCase));
        }
        if (!injectedStructure.pieceBox().isInside(player.blockPosition())) {
            throw new IllegalStateException("Injected piece bounding box did not contain the player for " + describeStructureCase(structureCase));
        }
    }

    private static String verifyGameplayGrant(
        GameTestHelper helper,
        ServerPlayer player,
        StructureOnlyLocationInventory.StructureOnlyCase structureCase
    ) {
        ServerLevel expectedLevel = requiredLevel(helper.getLevel().getServer(), Level.OVERWORLD);
        ServerLevel actualLevel = (ServerLevel) player.level();
        if (actualLevel != expectedLevel) {
            return "Expected player to remain in " + Level.OVERWORLD.identifier() + " for " + describeStructureCase(structureCase) +
                ", got " + String.valueOf(actualLevel.dimension());
        }

        LocationTriggerProbe.State probe = LocationTriggerProbe.get(player.getUUID());
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(structureCase.advancementId());
        if (advancement == null) {
            return "Missing advancement: " + structureCase.advancementId();
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!probe.isLocationListenerRegistered(structureCase.advancementId())) {
            return "LOCATION listener was not registered for " + describeStructureCase(structureCase) + " [" + probe.describe() + "]";
        }
        if (probe.getLocationTriggerCalls() <= 0) {
            return "CriteriaTriggers.LOCATION was never invoked for " + describeStructureCase(structureCase) + " [" + probe.describe() + "]";
        }
        if (probe.getTickTriggerCalls() <= 0) {
            return "Expected ordinary tick-driven scheduler lifecycle for " + describeStructureCase(structureCase) + " [" + probe.describe() + "]";
        }
        CriterionProgress criterionProgress = progress.getCriterion(structureCase.criterion());
        if (criterionProgress == null) {
            return "Live AdvancementProgress did not expose criterion " + structureCase.criterion() + " for " + describeStructureCase(structureCase) +
                " [" + probe.describe() + "]";
        }
        if (!criterionProgress.isDone()) {
            return "Criterion " + structureCase.criterion() + " was not granted for " + describeStructureCase(structureCase) +
                " on tick " + player.tickCount + " [" + probe.describe() + "]";
        }
        return null;
    }

    private static RequiredStructureSelection requiredStructure(ServerLevel level, StructureOnlyLocationInventory.StructureOnlyCase structureCase) {
        if (structureCase.isDirect()) {
            Holder.Reference<Structure> requiredStructure = requiredStructure(level, structureId(structureCase));
            return new RequiredStructureSelection(HolderSet.direct(requiredStructure), requiredStructure);
        }

        var structureLookup = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, structureCase.tagId());
        HolderSet<Structure> requiredHolderSet = structureLookup.getOrThrow(tagKey);
        List<Holder.Reference<Structure>> members = requiredHolderSet.stream().map(holder -> (Holder.Reference<Structure>) holder).toList();
        List<Identifier> memberIds = members.stream().map(PhaseAStructureLocationGameTest::holderId).toList();
        if (memberIds.size() != structureCase.requiredTagMembers().size()) {
            throw new IllegalStateException("Expected " + structureCase.requiredTagMembers().size() + " members in tag " + tagKey.location() + " but found " + memberIds.size());
        }
        if (!new LinkedHashSet<>(memberIds).equals(new LinkedHashSet<>(structureCase.requiredTagMembers()))) {
            throw new IllegalStateException("Unexpected tag members for " + tagKey.location() + ": " + memberIds);
        }
        Holder.Reference<Structure> selectedHolder = members.stream()
            .filter(holder -> holderId(holder).equals(structureCase.requiredTagMembers().get(0)))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unable to select a member holder from tag " + tagKey.location()));
        return new RequiredStructureSelection(requiredHolderSet, selectedHolder);
    }

    private static Holder.Reference<Structure> requiredStructure(ServerLevel level, Identifier structureId) {
        ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureId);
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structureKey)
            .orElseThrow(() -> new IllegalStateException("Missing registered structure: " + structureId));
    }

    private static Identifier holderId(Holder.Reference<Structure> holder) {
        return holder.unwrapKey().orElseThrow().identifier();
    }

    private static Identifier structureId(StructureOnlyLocationInventory.StructureOnlyCase structureCase) {
        String predicate = structureCase.frozenStructurePredicate();
        return predicate.contains(":") ? Identifier.parse(predicate) : Identifier.parse("minecraft:" + predicate);
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

    private static void closeInjectedStructure(SyntheticStructureHarness.InjectedStructure injectedStructure) {
        if (injectedStructure != null) {
            injectedStructure.close();
        }
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

    private static String describeStructureCase(StructureOnlyLocationInventory.StructureOnlyCase structureCase) {
        return structureCase.advancementId() + "#" + structureCase.criterion() + "@" + structureCase.frozenStructurePredicate();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}

