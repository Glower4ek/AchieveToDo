package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Real joined-player started_riding certification harness. */
public final class PhaseAEntityTypeTagStartedRidingGameTest implements CustomTestMethodInvoker {
    private static final int EXPECTED_CASE_COUNT = 5;
    private static final int MAX_POLL_TICKS = 10;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    private static final int CANARY_MAX_TICKS = 80;
    private static final int EXACT_MAX_TICKS = 260;

    private static final class JoinedPlayer {
        private final ServerPlayer player;
        private final Connection connection;
        private final EmbeddedChannel channel;
        private boolean cleanupComplete;

        private JoinedPlayer(ServerPlayer player, Connection connection, EmbeddedChannel channel) {
            this.player = player;
            this.connection = connection;
            this.channel = channel;
        }

        private ServerPlayer player() {
            return player;
        }

        private Connection connection() {
            return connection;
        }

        private EmbeddedChannel channel() {
            return channel;
        }

        private boolean cleanupComplete() {
            return cleanupComplete;
        }

        private void markCleanupComplete() {
            cleanupComplete = true;
        }
    }

    private record TagWitness(
        String tagId,
        Entity entity,
        int memberCount,
        boolean runtimeMembership
    ) {
    }

    private record RidingFixture(
        List<Entity> entities,
        Entity tagWitness,
        Entity finalVehicle,
        String requiredPassengerType,
        int requiredPassengerCount
    ) {
    }

    private record CaseRuntime(
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition,
        JoinedPlayer joinedPlayer,
        RidingFixture fixture,
        TagWitness tagWitness,
        AdvancementHolder advancement,
        boolean criterionBefore,
        boolean wasPassengerBefore,
        ProductionPreconditions productionPreconditions,
        boolean normalScheduler
    ) {
    }

    private record ProductionPreconditions(
        int scoreBefore,
        int scoreAfter,
        boolean boatAbilityLockedBefore,
        boolean boatAbilityLockedAfter,
        boolean minecartAbilityLockedBefore,
        boolean minecartAbilityLockedAfter,
        boolean lockedLandmark
    ) {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void entityTypeTagStartedRidingCanary(GameTestHelper helper) {
        List<CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition> cases = CertifiedEntityTypeTagStartedRidingCatalog.allCases();
        if (cases.size() != EXPECTED_CASE_COUNT) {
            helper.fail("Expected exactly five ENTITY_TYPE_TAG_STARTED_RIDING cases but found " + cases.size());
            return;
        }
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition canary = cases.stream()
            .filter(value -> value.key().equals("blazeandcave:biomes/boaty_mcboatface#boat"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing accepted boaty_mcboatface canary"));
        try {
            String runId = PhaseAEntityTypeTagStartedRidingExecutionEvidence.beginRun(
                PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot()
            );
            executeCase(helper, canary, runId, (completed, ignored) -> {
                try {
                    PhaseAEntityTypeTagStartedRidingExecutionEvidence.loadValidatedTemporaryArtifact(
                        PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot()
                    );
                    completed.succeed();
                } catch (Exception e) {
                    completed.fail("Started-riding canary TEMP_DIAGNOSTIC validation failed: " + describe(e));
                }
            });
        } catch (Throwable t) {
            helper.fail("ENTITY_TYPE_TAG_STARTED_RIDING canary setup failed: " + describe(t));
        }
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void entityTypeTagStartedRidingExact5(GameTestHelper helper) {
        List<CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition> cases = CertifiedEntityTypeTagStartedRidingCatalog.allCases();
        if (cases.size() != EXPECTED_CASE_COUNT) {
            helper.fail("Expected exactly five ENTITY_TYPE_TAG_STARTED_RIDING catalog cases but found " + cases.size());
            return;
        }
        try {
            String runId = PhaseAEntityTypeTagStartedRidingExecutionEvidence.beginRun(
                PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot()
            );
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("ENTITY_TYPE_TAG_STARTED_RIDING exact5 setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAEntityTypeTagStartedRidingExecutionEvidence.RuntimeExecutionArtifact artifact =
                    PhaseAEntityTypeTagStartedRidingExecutionEvidence.loadValidatedPromotableTemporaryArtifact(
                        PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot()
                    );
                require(artifact.entries().size() == EXPECTED_CASE_COUNT, "Exact5 TEMP artifact did not contain five receipts");
                require(runId.equals(artifact.runId()), "Exact5 coordinator runId changed");
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("Started-riding exact5 TEMP_PROMOTABLE validation failed: " + describe(t));
            }
            return;
        }
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition = cases.get(index);
        executeCase(helper, definition, runId, (completed, ignored) -> {
            try {
                require(runId.equals(PhaseAEntityTypeTagStartedRidingExecutionEvidence.currentRunId(
                    PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot()
                )), "Exact5 runId changed before recording " + definition.key());
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completed.fail("Started-riding exact5 coordinator failed after " + definition.key() + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            ServerPlayer player = joinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, joinedPlayer, definition);
            JoinedPlayer scheduledPlayer = joinedPlayer;
            helper.runAfterDelay(1, () -> {
                try {
                    executeJoinedCase(helper, definition, runId, completion, scheduledPlayer);
                } catch (Throwable t) {
                    cleanupJoinedServerPlayer(helper, scheduledPlayer);
                    helper.fail("Started-riding scheduled case failed for " + definition.key() + ": " + describe(t));
                }
            });
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            helper.fail("Started-riding case setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void executeJoinedCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) throws IOException {
        require(Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread()),
            "Started-riding case did not execute on the normal server scheduler thread");
        ServerPlayer player = joinedPlayer.player();
        AdvancementHolder advancement = advancementOrThrow(helper, definition);
        RidingFixture fixture = null;
        try {
            fixture = prepareFixture(helper, definition);
            TagWitness tagWitness = observeTagWitness(helper, definition, fixture.tagWitness());
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            CriterionProgress beforeProgress = progress.getCriterion(definition.criterion());
            require(beforeProgress != null, "Live AdvancementProgress did not expose " + definition.key());
            boolean criterionBefore = beforeProgress.isDone();
            require(!criterionBefore, "Criterion already complete before riding event: " + definition.key());
            boolean wasPassengerBefore = player.isPassenger();
            require(!wasPassengerBefore, "Player was already a passenger before riding event: " + definition.key());
            require(tagWitness.entity().isAlive() && !tagWitness.entity().isRemoved(), "Tag witness is not live before riding event");
            ProductionPreconditions preconditions = unlockRideAbilities(helper, player, tagWitness.entity(), definition);
            require(!criterionDone(player, advancement, definition.criterion()),
                "Production preconditions completed the started-riding criterion before the riding event: " + definition.key());

            boolean started = player.startRiding(fixture.finalVehicle());
            require(started, "Entity.startRiding returned false for " + definition.key());
            boolean criterionAfter = criterionDone(player, advancement, definition.criterion());
            boolean isPassengerAfter = player.isPassenger();
            require(isPassengerAfter, "Player did not become a passenger for " + definition.key());
            String actualVehicleTypeAfter = entityId(player.getVehicle());
            require(definition.directVehicleType().equals(actualVehicleTypeAfter),
                "Unexpected direct vehicle after riding for " + definition.key() + ": " + actualVehicleTypeAfter);
            List<String> actualVehicleChainAfter = observeVehicleChainAfter(player, definition);
            require(expectedVehicleChain(definition).equals(actualVehicleChainAfter),
                "Unexpected live vehicle chain after riding for " + definition.key()
                    + ": " + actualVehicleChainAfter);
            require(criterionAfter, "started_riding criterion did not transition false -> true for " + definition.key());

            CaseRuntime runtime = new CaseRuntime(
                definition,
                joinedPlayer,
                fixture,
                tagWitness,
                advancement,
                criterionBefore,
                wasPassengerBefore,
                preconditions,
                true
            );
            JsonObject receipt = buildReceipt(runtime, actualVehicleTypeAfter, actualVehicleChainAfter, 0);
            PhaseAEntityTypeTagStartedRidingExecutionEvidence.recordGreen(
                PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot(), receipt
            );
            System.out.println("ENTITY_TYPE_TAG_STARTED_RIDING_CASE=" + definition.key()
                + " | action=" + definition.action()
                + " | entityTag=#" + definition.entityTypeTag()
                + " | selectedEntity=" + definition.selectedEntityType()
                + " | beforePassenger=" + wasPassengerBefore
                + " | afterVehicle=" + actualVehicleTypeAfter
                + " | criterionBefore=" + criterionBefore
                + " | criterionAfter=" + criterionAfter
                + " | ticks=" + 0);
            cleanupFixture(fixture, player);
            cleanupJoinedServerPlayer(helper, joinedPlayer);
            helper.runAfterDelay(1, () -> completion.complete(helper, runtime));
        } catch (Throwable t) {
            if (fixture != null) {
                cleanupFixture(fixture, player);
            }
            cleanupJoinedServerPlayer(helper, joinedPlayer);
            throw t;
        }
    }

    private static JsonObject buildReceipt(
        CaseRuntime runtime,
        String actualVehicleTypeAfter,
        List<String> actualVehicleChainAfter,
        int ticksToCriterion
    ) {
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition = runtime.definition();
        ServerPlayer player = runtime.joinedPlayer().player();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId().toString());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("entityTypeTag", definition.entityTypeTag());
        receipt.addProperty("selectedEntityType", definition.selectedEntityType());
        receipt.addProperty("runtimeEntityTypeTagMembership", runtime.tagWitness().runtimeMembership());
        receipt.addProperty("runtimeEntityTypeTagMemberCount", runtime.tagWitness().memberCount());
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("joined", helperServer(player).getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", helperServer(player).getConnection().getConnections().contains(runtime.joinedPlayer().connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", runtime.normalScheduler());
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("buildPermission", player.mayBuild());
        JsonObject preconditions = new JsonObject();
        preconditions.addProperty("scoreboardObjective", "bac_advancements");
        preconditions.addProperty("scoreBefore", runtime.productionPreconditions().scoreBefore());
        preconditions.addProperty("scoreAfter", runtime.productionPreconditions().scoreAfter());
        preconditions.addProperty("boatAbilityLockedBefore", runtime.productionPreconditions().boatAbilityLockedBefore());
        preconditions.addProperty("boatAbilityLockedAfter", runtime.productionPreconditions().boatAbilityLockedAfter());
        preconditions.addProperty("minecartAbilityLockedBefore", runtime.productionPreconditions().minecartAbilityLockedBefore());
        preconditions.addProperty("minecartAbilityLockedAfter", runtime.productionPreconditions().minecartAbilityLockedAfter());
        preconditions.addProperty("lockedLandmark", runtime.productionPreconditions().lockedLandmark());
        receipt.add("productionPreconditions", preconditions);
        receipt.addProperty("wasPassengerBefore", runtime.wasPassengerBefore());
        receipt.addProperty("isPassengerAfter", true);
        receipt.addProperty("actualVehicleTypeAfter", actualVehicleTypeAfter);
        receipt.addProperty("criterionBefore", runtime.criterionBefore());
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("ticksToCriterion", ticksToCriterion);
        receipt.addProperty("boundary", "Entity.startRiding");
        receipt.addProperty("action", definition.action());
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("interactionResult", "SUCCESS");
        receipt.addProperty("semanticMutation", true);
        receipt.addProperty("sourceArtifactClassification", definition.sourceArtifactClassification());
        JsonArray chain = new JsonArray();
        for (String actualVehicleType : actualVehicleChainAfter) {
            chain.add(actualVehicleType);
        }
        receipt.add("vehicleChainAfter", chain);
        receipt.addProperty("requiredPassengerCount", runtime.fixture().requiredPassengerCount());
        if (!definition.requiredPassengerType().isBlank()) {
            receipt.addProperty("requiredPassengerTypeAfter", definition.requiredPassengerType());
        }
        receipt.addProperty("family", PhaseAEntityTypeTagStartedRidingExecutionEvidence.FAMILY);
        receipt.addProperty("source", PhaseAEntityTypeTagStartedRidingExecutionEvidence.SOURCE);
        receipt.addProperty("result", PhaseAEntityTypeTagStartedRidingExecutionEvidence.GREEN);
        receipt.addProperty("catalogFingerprint", "pending");
        receipt.addProperty("runId", "pending");
        receipt.addProperty("minecraftVersion", PhaseAEntityTypeTagStartedRidingExecutionEvidence.MINECRAFT_VERSION);
        receipt.addProperty("compatibilityMarker", PhaseAEntityTypeTagStartedRidingExecutionEvidence.COMPATIBILITY_MARKER);
        try {
            java.nio.file.Path root = PhaseAEntityTypeTagStartedRidingExecutionEvidence.projectRoot();
            receipt.addProperty("catalogFingerprint", PhaseAEntityTypeTagStartedRidingExecutionEvidence.currentCatalogFingerprint(root));
            receipt.addProperty("runId", PhaseAEntityTypeTagStartedRidingExecutionEvidence.currentRunId(root));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to align started-riding receipt with active run", e);
        }
        return receipt;
    }

    private static List<String> observeVehicleChainAfter(
        ServerPlayer player,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        List<String> chain = new ArrayList<>();
        Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity current = player.getVehicle(); current != null; current = current.getVehicle()) {
            require(visited.add(current), "Vehicle graph contains a cycle after riding for " + definition.key());
            require(current.isAlive() && !current.isRemoved(),
                "Observed vehicle graph contains a non-live entity after riding for " + definition.key());
            chain.add(entityId(current));
        }
        require(!chain.isEmpty(), "Player has no live vehicle after riding for " + definition.key());
        return List.copyOf(chain);
    }

    private static List<String> expectedVehicleChain(
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        return definition.vehiclePredicateChain().stream()
            .map(predicate -> predicate.startsWith("#") ? definition.selectedEntityType() : predicate)
            .toList();
    }

    private static RidingFixture prepareFixture(
        GameTestHelper helper,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        List<Entity> entities = new ArrayList<>();
        try {
            Entity tagWitness = spawnEntity(helper, definition.selectedEntityType(), 1, 2, 1, entities);
            Entity finalVehicle;
            switch (definition.action()) {
                case "START_RIDING_BOAT", "START_RIDING_CHEST_BOAT" -> finalVehicle = tagWitness;
                case "START_RIDING_PIG_ON_BOAT_IN_MINECART" -> {
                    Entity minecart = spawnEntity(helper, "minecraft:minecart", 1, 2, 1, entities);
                    require(tagWitness.startRiding(minecart), "Boat could not start riding minecart");
                    Entity pig = spawnEntity(helper, "minecraft:pig", 1, 2, 1, entities);
                    require(pig.startRiding(tagWitness), "Pig could not start riding boat");
                    finalVehicle = pig;
                }
                case "START_RIDING_STRIDER_STACK_ON_BOAT_IN_MINECART" -> {
                    Entity minecart = spawnEntity(helper, "minecraft:minecart", 1, 2, 1, entities);
                    require(tagWitness.startRiding(minecart), "Boat could not start riding minecart");
                    Entity inner = spawnEntity(helper, "minecraft:strider", 1, 2, 1, entities);
                    require(inner.startRiding(tagWitness), "Inner strider could not start riding boat");
                    Entity outer = spawnEntity(helper, "minecraft:strider", 1, 2, 1, entities);
                    require(outer.startRiding(inner), "Outer strider could not start riding inner strider");
                    finalVehicle = outer;
                }
                case "START_RIDING_BOAT_WITH_GOAT" -> {
                    Entity goat = spawnEntity(helper, "minecraft:goat", 1, 2, 1, entities);
                    require(goat.startRiding(tagWitness), "Goat could not start riding boat");
                    finalVehicle = tagWitness;
                }
                default -> throw new IllegalStateException("Unsupported started-riding action: " + definition.action());
            }
            int passengerCount = 0;
            if (!definition.requiredPassengerType().isBlank()) {
                passengerCount = (int) tagWitness.getPassengers().stream()
                    .filter(passenger -> definition.requiredPassengerType().equals(entityId(passenger)))
                    .count();
                require(passengerCount > 0, "Required passenger was not present after fixture setup for " + definition.key());
            }
            return new RidingFixture(List.copyOf(entities), tagWitness, finalVehicle, definition.requiredPassengerType(), passengerCount);
        } catch (Throwable t) {
            for (Entity entity : entities) {
                if (entity.isPassenger()) {
                    entity.stopRiding();
                }
            }
            for (Entity entity : entities) {
                if (!entity.isRemoved()) {
                    entity.discard();
                }
            }
            throw t;
        }
    }

    private static ProductionPreconditions unlockRideAbilities(
        GameTestHelper helper,
        ServerPlayer player,
        Entity fixtureEntity,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective for " + definition.key());
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not the active default objective for " + definition.key());
        var beforeScoreInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeScoreInfo == null ? 0 : beforeScoreInfo.value();
        require(scoreBefore == 0, "Fresh bac_advancements score was not zero for " + definition.key() + ": " + scoreBefore);
        boolean boatLockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_BOAT);
        boolean minecartLockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_MINECART);
        require(boatLockedBefore, "GET_INTO_BOAT was not locked before ordinary scoreboard unlock for " + definition.key());
        require(minecartLockedBefore, "GET_INTO_MINECART was not locked before ordinary scoreboard unlock for " + definition.key());
        scoreboard.getOrCreatePlayerScore(player, objective).set(1000);
        var afterScoreInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = afterScoreInfo == null ? 0 : afterScoreInfo.value();
        require(scoreAfter == 1000, "Scoreboard unlock did not produce score 1000 for " + definition.key());
        boolean boatLockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_BOAT);
        boolean minecartLockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_MINECART);
        require(!boatLockedAfter, "GET_INTO_BOAT remained locked at riding boundary for " + definition.key());
        require(!minecartLockedAfter, "GET_INTO_MINECART remained locked at riding boundary for " + definition.key());
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), fixtureEntity.blockPosition());
        require(!lockedLandmark, "Riding fixture lies in a locked landmark for " + definition.key());
        return new ProductionPreconditions(
            scoreBefore,
            scoreAfter,
            boatLockedBefore,
            boatLockedAfter,
            minecartLockedBefore,
            minecartLockedAfter,
            lockedLandmark
        );
    }

    private static MinecraftServer helperServer(ServerPlayer player) {
        return player.level().getServer();
    }

    private static Entity spawnEntity(GameTestHelper helper, String id, int x, int y, int z, List<Entity> entities) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
        require(type != null, "Missing registered entity type " + id);
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        require(entity != null, "Could not create entity " + id);
        entity.teleportTo(helper.absolutePos(new net.minecraft.core.BlockPos(x, y, z)).getX() + 0.5D,
            helper.absolutePos(new net.minecraft.core.BlockPos(x, y, z)).getY(),
            helper.absolutePos(new net.minecraft.core.BlockPos(x, y, z)).getZ() + 0.5D);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        require(helper.getLevel().addFreshEntity(entity), "Could not add entity " + id + " to the live level");
        entities.add(entity);
        return entity;
    }

    private static TagWitness observeTagWitness(
        GameTestHelper helper,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition,
        Entity entity
    ) {
        HolderGetter<EntityType<?>> lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(definition.entityTypeTag()));
        HolderSet.Named<EntityType<?>> holderSet = lookup.getOrThrow(tag);
        int memberCount = (int) holderSet.stream().count();
        boolean membership = holderSet.contains(entity.typeHolder());
        require(memberCount > 0, "Required entity-type tag is empty: " + definition.entityTypeTag());
        require(membership, "Selected entity is not a runtime member of " + definition.entityTypeTag());
        require(entityId(entity).equals(definition.selectedEntityType()), "Selected entity type drifted from catalog");
        return new TagWitness(definition.entityTypeTag(), entity, memberCount, membership);
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(definition.advancementId());
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static boolean criterionDone(ServerPlayer player, AdvancementHolder advancement, String criterion) {
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress value = progress.getCriterion(criterion);
        return value != null && value.isDone();
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Player was not joined for " + definition.key());
        require(server.getConnection().getConnections().contains(joinedPlayer.connection()), "Connection was not registered for " + definition.key());
        require(player.connection.hasClientLoaded(), "Client-loaded packet was not accepted for " + definition.key());
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "Expected SURVIVAL for " + definition.key());
        require(player.mayBuild(), "Expected build permission for " + definition.key());
    }

    private static JoinedPlayer createJoinedServerPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID playerId = UUID.randomUUID();
        String profileName = profileNameForUuid(playerId);
        GameProfile profile = new GameProfile(playerId, profileName);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getConnection().getConnections().add(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new JoinedPlayer(player, connection, channel);
    }

    private static String profileNameForUuid(UUID playerId) {
        String compact = playerId.toString().replace("-", "");
        String result = "ride" + compact.substring(compact.length() - 12);
        require(result.length() <= 16, "Generated started-riding profile name exceeds 16 characters: " + result);
        return result;
    }

    private static void cleanupFixture(RidingFixture fixture, ServerPlayer player) {
        if (player != null && player.isPassenger()) {
            player.stopRiding();
        }
        for (Entity entity : fixture.entities()) {
            if (entity.isPassenger()) {
                entity.stopRiding();
            }
        }
        for (Entity entity : fixture.entities()) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    private static void cleanupJoinedServerPlayer(GameTestHelper helper, JoinedPlayer joinedPlayer) {
        if (joinedPlayer.cleanupComplete()) {
            return;
        }
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = joinedPlayer.player();
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        server.getPlayerList().remove(player);
        server.getConnection().getConnections().remove(joinedPlayer.connection());
        settleOpenEmbeddedChannel(joinedPlayer.channel());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        joinedPlayer.markCleanupComplete();
    }

    private static void settleOpenEmbeddedChannel(EmbeddedChannel channel) {
        require(channel.isOpen(), "EmbeddedChannel closed before started-riding cleanup settlement");
        int releasedMessages = 0;
        for (int pass = 0; pass < MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES; pass++) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            channel.flushOutbound();
            int releasedThisPass = 0;
            Object outbound;
            while ((outbound = channel.readOutbound()) != null) {
                ReferenceCountUtil.release(outbound);
                releasedThisPass++;
                releasedMessages++;
                if (releasedMessages > MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES) {
                    throw new IllegalStateException("Started-riding cleanup exceeded " + MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES + " outbound messages");
                }
            }
            if (releasedThisPass == 0 && !channel.hasPendingTasks()) {
                return;
            }
        }
        throw new IllegalStateException("Started-riding EmbeddedChannel cleanup did not quiesce within "
            + MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES + " passes");
    }

    private static String entityId(Entity entity) {
        return entity == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String describe(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @FunctionalInterface
    private interface CaseCompletion {
        void complete(GameTestHelper helper, CaseRuntime runtime);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
