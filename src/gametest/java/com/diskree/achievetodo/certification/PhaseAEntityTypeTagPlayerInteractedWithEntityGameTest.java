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
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real joined-player packet-path certification for player_interacted_with_entity. */
public final class PhaseAEntityTypeTagPlayerInteractedWithEntityGameTest implements CustomTestMethodInvoker {
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_PASSES = 32;
    private static final int MAX_EMBEDDED_CHANNEL_SETTLEMENT_MESSAGES = 4096;
    private static final int UNLOCK_SCORE = 1000;
    private static final int CANARY_MAX_TICKS = 80;
    private static final int DIAGNOSTIC_MAX_TICKS = 80;
    // Each case has one scheduled interaction tick and one scheduled hand-off tick.
    // Nine cases require 18 coordinator ticks; 80 retains the established bounded test window.
    private static final int EXACT_MAX_TICKS = 80;
    private static final net.minecraft.core.BlockPos TARGET_POS = new net.minecraft.core.BlockPos(1, 2, 1);

    public PhaseAEntityTypeTagPlayerInteractedWithEntityGameTest() {
    }

    @GameTest(maxTicks = CANARY_MAX_TICKS)
    public void entityTypeTagPlayerInteractedWithEntityCanary(GameTestHelper helper) {
        List<CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition> cases =
            CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("Interaction catalog is empty");
            return;
        }
        try {
            String runId = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.beginRun(
                PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
            );
            executeCase(helper, cases.getFirst(), runId, completedHelper -> {
                try {
                    PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.RuntimeExecutionArtifact artifact =
                        PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.loadValidatedTemporaryArtifact(
                            PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
                        );
                    if (artifact.entries().size() != 1) {
                        throw new IllegalStateException("Canary expected exactly one TEMP receipt");
                    }
                    completedHelper.succeed();
                } catch (Throwable t) {
                    completedHelper.fail("Interaction canary TEMP_DIAGNOSTIC validation failed: " + describe(t));
                }
            });
        } catch (Throwable t) {
            helper.fail("Interaction canary setup failed: " + describe(t));
        }
    }

    /**
     * Non-promotable, one-case diagnostic for the first unfinished exact-family case.
     * It deliberately owns its own temporary run and must only ever be selected alone.
     */
    @GameTest(maxTicks = DIAGNOSTIC_MAX_TICKS)
    public void entityTypeTagPlayerInteractedWithEntityHayBlockDiagnostic(GameTestHelper helper) {
        executeDiagnosticCase(
            helper,
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "hay_block"
        );
    }

    /** Non-promotable, one-case diagnostic for the second unfinished exact-family case. */
    @GameTest(maxTicks = DIAGNOSTIC_MAX_TICKS)
    public void entityTypeTagPlayerInteractedWithEntityEnchantedGoldenAppleDiagnostic(GameTestHelper helper) {
        executeDiagnosticCase(
            helper,
            "blazeandcave:animal/so_hungry_i_could_eat_a_horse",
            "enchanted_golden_apple"
        );
    }

    /** Non-promotable, one-case diagnostic for the final unfinished exact-family case. */
    @GameTest(maxTicks = DIAGNOSTIC_MAX_TICKS)
    public void entityTypeTagPlayerInteractedWithEntityLlamaDiagnostic(GameTestHelper helper) {
        executeDiagnosticCase(helper, "blazeandcave:animal/you_lead_ill_follow", "lead");
    }

    @GameTest(maxTicks = EXACT_MAX_TICKS)
    public void entityTypeTagPlayerInteractedWithEntityExact9(GameTestHelper helper) {
        List<CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition> cases =
            CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.allCases();
        if (cases.isEmpty()) {
            helper.fail("Interaction catalog is empty");
            return;
        }
        try {
            String runId = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.beginRun(
                PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
            );
            executeCoordinatorCase(helper, cases, 0, runId);
        } catch (Throwable t) {
            helper.fail("Interaction exact-family setup failed: " + describe(t));
        }
    }

    private static void executeDiagnosticCase(GameTestHelper helper, String advancementId, String criterion) {
        try {
            CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition =
                CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.requiredCase(Identifier.parse(advancementId), criterion);
            String runId = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.beginRun(
                PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
            );
            executeCase(helper, definition, runId, completedHelper -> {
                try {
                    PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.RuntimeExecutionArtifact artifact =
                        PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.loadValidatedTemporaryArtifact(
                            PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
                        );
                    if (artifact.entries().size() != 1 || !runId.equals(artifact.runId())) {
                        throw new IllegalStateException("Diagnostic interaction artifact cardinality/runId mismatch");
                    }
                    completedHelper.succeed();
                } catch (Throwable t) {
                    completedHelper.fail("Interaction diagnostic validation failed: " + describe(t));
                }
            });
        } catch (Throwable t) {
            helper.fail("Interaction diagnostic setup failed: " + describe(t));
        }
    }

    private static void executeCoordinatorCase(
        GameTestHelper helper,
        List<CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition> cases,
        int index,
        String runId
    ) {
        if (index >= cases.size()) {
            try {
                PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.RuntimeExecutionArtifact artifact =
                    PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.loadValidatedPromotableTemporaryArtifact(
                        PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
                    );
                if (artifact.entries().size() != cases.size() || !runId.equals(artifact.runId())) {
                    throw new IllegalStateException("Exact interaction artifact cardinality/runId mismatch");
                }
                helper.succeed();
            } catch (Throwable t) {
                helper.fail("Interaction TEMP_PROMOTABLE validation failed: " + describe(t));
            }
            return;
        }
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition = cases.get(index);
        executeCase(helper, definition, runId, completedHelper -> {
            try {
                if (!runId.equals(PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.currentRunId(
                    PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
                ))) {
                    throw new IllegalStateException("Exact interaction coordinator runId changed");
                }
                executeCoordinatorCase(helper, cases, index + 1, runId);
            } catch (Throwable t) {
                completedHelper.fail("Interaction coordinator failed after " + definition.key() + ": " + describe(t));
            }
        });
    }

    private static void executeCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion
    ) {
        JoinedPlayer joinedPlayer = null;
        try {
            joinedPlayer = createJoinedServerPlayer(helper);
            establishFiniteSurvival(joinedPlayer.player());
            new ServerboundPlayerLoadedPacket().handle(joinedPlayer.player().connection);
            assertJoinedLifecycle(helper, joinedPlayer, definition);
            JoinedPlayer scheduledPlayer = joinedPlayer;
            helper.runAfterDelay(1, () -> executeScheduledCase(helper, definition, runId, completion, scheduledPlayer));
        } catch (Throwable t) {
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(joinedPlayer);
            }
            helper.fail("Interaction case setup failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void executeScheduledCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        String runId,
        CaseCompletion completion,
        JoinedPlayer joinedPlayer
    ) {
        Fixture[] fixtureReference = new Fixture[1];
        CaseExecution execution = null;
        CleanupResult cleanup = null;
        Throwable failure = null;
        try {
            assertCurrentRunId(runId);
            execution = executeJoinedCase(helper, definition, joinedPlayer, fixtureReference);
        } catch (Throwable t) {
            failure = t;
        } finally {
            if (fixtureReference[0] != null) {
                cleanupFixture(fixtureReference[0]);
            }
            try {
                cleanup = cleanupJoinedServerPlayer(joinedPlayer);
            } catch (Throwable t) {
                failure = failure == null ? t : new IllegalStateException("Interaction and cleanup both failed", failure);
            }
        }
        if (failure != null) {
            helper.fail("Interaction proof failed for " + definition.key() + ": " + describe(failure));
            return;
        }
        try {
            assertCurrentRunId(runId);
            JsonObject cleanupJson = new JsonObject();
            cleanupJson.addProperty("playerRemoved", cleanup.playerRemoved());
            cleanupJson.addProperty("connectionRemoved", cleanup.connectionRemoved());
            cleanupJson.addProperty("channelSettled", cleanup.channelSettled());
            cleanupJson.addProperty("settlementMessages", cleanup.settlementMessages());
            cleanupJson.addProperty("warningCount", 0);
            execution.receipt().add("cleanup", cleanupJson);
            var projectRoot = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot();
            execution.receipt().addProperty("family", PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.FAMILY);
            execution.receipt().addProperty("source", PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.SOURCE);
            execution.receipt().addProperty(
                "catalogFingerprint",
                PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.currentCatalogFingerprint(projectRoot)
            );
            execution.receipt().addProperty("runId", runId);
            execution.receipt().addProperty("minecraftVersion", PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.MINECRAFT_VERSION);
            execution.receipt().addProperty("compatibilityMarker", PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.COMPATIBILITY_MARKER);
            execution.receipt().addProperty("result", PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.GREEN);
            PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.recordGreen(
                projectRoot, execution.receipt()
            );
            System.out.println("ENTITY_TYPE_TAG_PLAYER_INTERACTED_WITH_ENTITY_CASE=" + definition.key()
                + " | action=" + definition.action()
                + " | itemTag=#" + definition.itemTag()
                + " | selectedItem=" + definition.selectedItem()
                + " | entity=" + definition.selectedEntityType()
                + " | boundary=" + definition.boundary()
                + " | criterionBefore=false | criterionAfter=true | cleanupWarnings=0");
            CaseCompletion next = completion;
            helper.runAfterDelay(1, () -> next.complete(helper));
        } catch (Throwable t) {
            helper.fail("Interaction evidence recording failed for " + definition.key() + ": " + describe(t));
        }
    }

    private static void assertCurrentRunId(String expectedRunId) throws IOException {
        String actualRunId = PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.currentRunId(
            PhaseAEntityTypeTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
        );
        if (!expectedRunId.equals(actualRunId)) {
            throw new IllegalStateException(
                "Interaction recorder runId changed during case execution: expected=" + expectedRunId + ", actual=" + actualRunId
            );
        }
    }

    private static CaseExecution executeJoinedCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        JoinedPlayer joinedPlayer,
        Fixture[] fixtureReference
    ) {
        if (!Thread.currentThread().equals(helper.getLevel().getServer().getRunningThread())) {
            throw new IllegalStateException("Interaction did not execute on the normal server scheduler thread");
        }
        ServerPlayer player = joinedPlayer.player();
        SelectedItem selected = resolveSelectedItem(definition);
        Fixture fixture = prepareFixture(helper, definition);
        fixtureReference[0] = fixture;
        Entity target = fixture.target();
        player.teleportTo(target.getX() - 2.0D, target.getY(), target.getZ());
        player.getInventory().clearContent();

        AdvancementHolder advancement = advancementOrThrow(helper, definition);
        AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionBeforeProgress = progressBefore.getCriterion(definition.criterion());
        require(criterionBeforeProgress != null, "Live AdvancementProgress did not expose " + definition.key());
        require(!criterionBeforeProgress.isDone(), "Criterion was complete before interaction fixture for " + definition.key());

        ProductionPreconditions preconditions = prepareProductionPreconditions(helper, player, target, definition);
        CriterionProgress afterPreconditions = player.getAdvancements().getOrStartProgress(advancement)
            .getCriterion(definition.criterion());
        require(afterPreconditions != null && !afterPreconditions.isDone(),
            "Production preconditions completed the target criterion for " + definition.key());

        ItemStack interactionStack = new ItemStack(selected.holder(), 1);
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), interactionStack);
        require(player.getInventory().getSelectedItem() == interactionStack
                && player.getItemInHand(InteractionHand.MAIN_HAND) == interactionStack
                && interactionStack.getItem() == selected.item(),
            "Selected ItemStack was not installed in the selected packet hand");
        int countBefore = interactionStack.getCount();
        int damageBefore = interactionStack.getDamageValue();
        String itemBefore = itemId(interactionStack);
        String stackSnapshotBefore = interactionStack.toString();
        JsonObject entityBefore = entityState(target);

        ServerboundInteractPacket packet = new ServerboundInteractPacket(
            target.getId(),
            InteractionHand.MAIN_HAND,
            target.position(),
            false
        );
        packet.handle(player.connection);

        AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionAfterProgress = progressAfter.getCriterion(definition.criterion());
        require(criterionAfterProgress != null && criterionAfterProgress.isDone(),
            "Criterion did not transition false->true through the normal interaction packet for " + definition.key());
        require(target.isAlive() && !target.isRemoved(), "Target was not alive after interaction for " + definition.key());

        ItemStack observedHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        int countAfter = interactionStack.getCount();
        int damageAfter = interactionStack.getDamageValue();
        String itemAfter = itemId(interactionStack);
        String expectedItemAfter = "IGNITE_CREEPER".equals(definition.action())
            ? definition.selectedItem() : "minecraft:air";
        require(itemAfter.equals(expectedItemAfter), "Used packet-hand item drifted for " + definition.key());
        boolean actualAction = verifyActionMutation(
            definition,
            player,
            target,
            interactionStack,
            countAfter,
            damageBefore,
            damageAfter,
            entityBefore.get("health").getAsFloat()
        );
        require(actualAction, "Expected action mutation was not observed for " + definition.key());
        JsonObject entityAfter = entityState(target);
        require(definition.selectedEntityType().equals(entityAfter.get("type").getAsString()),
            "Observed target type drifted for " + definition.key());

        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId().toString());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", definition.requirementGroupIndex());
        receipt.addProperty("trigger", definition.trigger());
        receipt.addProperty("entityTag", definition.itemTag());
        receipt.addProperty("selectedItem", definition.selectedItem());
        receipt.addProperty("runtimeEntityTagMembership", entityTagMembership(helper, definition, target));
        receipt.addProperty("runtimeEntityTagMemberCount", selected.memberCount());
        JsonArray members = new JsonArray();
        members.add(entityAfter.get("type").getAsString());
        receipt.add("runtimeEntityTagMembers", members);
        receipt.addProperty("selectedEntityType", definition.selectedEntityType());
        receipt.addProperty("observedEntityType", entityAfter.get("type").getAsString());
        receipt.addProperty("targetEntityId", target.getId());
        receipt.addProperty("targetEntityUuid", target.getUUID().toString());
        receipt.addProperty("targetAliveBefore", entityBefore.get("alive").getAsBoolean());
        receipt.addProperty("targetAliveAfter", target.isAlive());
        receipt.addProperty("entityPredicateSatisfied", predicateSatisfied(helper, definition, target));
        receipt.addProperty("boundary", definition.boundary());
        receipt.addProperty("packetPath", definition.packetPath());
        receipt.addProperty("hand", definition.interactionHand());
        receipt.addProperty("playerClass", player.getClass().getName());
        receipt.addProperty("playerUuid", player.getUUID().toString());
        receipt.addProperty("profileName", player.getGameProfile().name());
        receipt.addProperty("gameMode", player.gameMode().name());
        receipt.addProperty("finiteMaterials", !player.hasInfiniteMaterials());
        receipt.addProperty("joined", helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player);
        receipt.addProperty("connectionRegistered", helper.getLevel().getServer().getConnection().getConnections().contains(joinedPlayer.connection()));
        receipt.addProperty("clientLoaded", player.connection.hasClientLoaded());
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", criterionAfterProgress.isDone());
        receipt.addProperty("interactionPerformed", actualAction);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("interactionResult", definition.expectedInteractionResult());
        receipt.addProperty("interactionConsumesAction", true);
        receipt.addProperty("ticksToCriterion", 0);
        receipt.add("productionPreconditions", preconditions.toJson());
        receipt.add("interactionStack", interactionStackState(
            itemBefore, interactionStack, observedHand, countBefore, countAfter, damageBefore, damageAfter, stackSnapshotBefore
        ));
        receipt.add("entityStateBefore", entityBefore);
        receipt.add("entityStateAfter", entityAfter);

        JsonObject predicate = new JsonObject();
        predicate.addProperty("requiredEntityType", definition.selectedEntityType());
        predicate.addProperty("observedEntityType", entityAfter.get("type").getAsString());
        predicate.addProperty("satisfied", predicateSatisfied(helper, definition, target));
        receipt.add("entityPredicateObservation", predicate);

        JsonObject proof = new JsonObject();
        proof.addProperty("action", definition.action());
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("entityTagWitness", entityTagMembership(helper, definition, target));
        proof.addProperty("targetIdentityWitness", target.getId() > 0 && target.getUUID() != null);
        proof.addProperty("criterionBefore", false);
        proof.addProperty("criterionAfter", criterionAfterProgress.isDone());
        receipt.add("actionProof", proof);
        return new CaseExecution(fixture, receipt);
    }

    private static boolean verifyActionMutation(
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        ServerPlayer player,
        Entity target,
        ItemStack interactionStack,
        int countAfter,
        int damageBefore,
        int damageAfter,
        float targetHealthBefore
    ) {
        return switch (definition.action()) {
            case "ATTACH_LEAD" -> target instanceof Mob mob
                && mob.getLeashHolder() == player
                && countAfter == 0
                && damageAfter == damageBefore;
            case "FEED_HORSE" -> target instanceof AbstractHorse horse
            && countAfter == 0
                && damageAfter == damageBefore
                && horse.getHealth() > targetHealthBefore;
            case "FEED_DOLPHIN" -> target instanceof Dolphin dolphin
                && dolphin.gotFish()
                && countAfter == 0
                && damageAfter == damageBefore;
            case "FEED_OCELOT" -> target instanceof Ocelot
                && countAfter == 0
                && damageAfter == damageBefore;
            case "IGNITE_CREEPER" -> target instanceof Creeper creeper
                && creeper.isIgnited()
                && countAfter == 1
                && damageAfter == damageBefore + 1;
            case "FEED_BABY_SNIFFER" -> target instanceof Sniffer sniffer
                && sniffer.isBaby()
                && countAfter == 0
                && damageAfter == damageBefore;
            default -> false;
        };
    }

    private static JsonObject interactionStackState(
        String itemBefore,
        ItemStack interactionStack,
        ItemStack observedHand,
        int countBefore,
        int countAfter,
        int damageBefore,
        int damageAfter,
        String snapshotBefore
    ) {
        JsonObject stack = new JsonObject();
        stack.addProperty("itemBefore", itemBefore);
        stack.addProperty("itemAfter", itemId(interactionStack));
        stack.addProperty("countBefore", countBefore);
        stack.addProperty("countAfter", countAfter);
        stack.addProperty("damageBefore", damageBefore);
        stack.addProperty("damageAfter", damageAfter);
        stack.addProperty("selectedHandAfter", observedHand.isEmpty() ? "minecraft:air" : itemId(observedHand));
        stack.addProperty("selectedHandCountAfter", observedHand.getCount());
        stack.addProperty("snapshotBefore", snapshotBefore);
        stack.addProperty("snapshotAfter", interactionStack.toString());
        stack.addProperty("sameStackReference", observedHand == interactionStack);
        return stack;
    }

    private static JsonObject entityState(Entity target) {
        JsonObject state = new JsonObject();
        state.addProperty("type", entityId(target));
        state.addProperty("entityId", target.getId());
        state.addProperty("uuid", target.getUUID().toString());
        state.addProperty("alive", target.isAlive());
        if (target instanceof LivingEntity living) {
            state.addProperty("health", living.getHealth());
        }
        if (target instanceof Dolphin dolphin) {
            state.addProperty("gotFish", dolphin.gotFish());
        }
        if (target instanceof Creeper creeper) {
            state.addProperty("ignited", creeper.isIgnited());
        }
        if (target instanceof Sniffer sniffer) {
            state.addProperty("isBaby", sniffer.isBaby());
        }
        return state;
    }

    private static boolean predicateSatisfied(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        Entity target
    ) {
        if (!definition.selectedEntityType().equals(entityId(target))) {
            return false;
        }
        if (!entityTagMembership(helper, definition, target)
            && !"blazeandcave:dont_trigger_piwe".equals(definition.itemTag())) {
            return false;
        }
        if ("blazeandcave:dont_trigger_piwe".equals(definition.itemTag())
            && entityTagMembership(helper, definition, target)) {
            return false;
        }
        if (definition.entityFlags().containsKey("is_baby")) {
            return target instanceof Sniffer sniffer && sniffer.isBaby() == Boolean.parseBoolean(definition.entityFlags().get("is_baby"));
        }
        return definition.entityFlags().isEmpty();
    }

    private static ProductionPreconditions prepareProductionPreconditions(
        GameTestHelper helper,
        ServerPlayer player,
        Entity target,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition
    ) {
        MinecraftServer server = helper.getLevel().getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective("bac_advancements");
        require(objective != null, "Missing bac_advancements objective for " + definition.key());
        require(AchieveToDoMod.getServer().currentAdvancementsMode == AdvancementsMode.DEFAULT,
            "bac_advancements is not active default objective for " + definition.key());
        var beforeInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreBefore = beforeInfo == null ? 0 : beforeInfo.value();
        require(scoreBefore == 0, "Fresh interaction player score was not zero for " + definition.key());
        boolean lockedBefore = false;
        boolean lockedAfter = false;
        if ("USE_FLINT_AND_STEEL_UNLOCKED".equals(definition.productionPrecondition())) {
            lockedBefore = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL);
            require(lockedBefore, "Flint-and-steel ability was not locked before scoreboard unlock");
            scoreboard.getOrCreatePlayerScore(player, objective).set(UNLOCK_SCORE);
            lockedAfter = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL);
            require(!lockedAfter, "Flint-and-steel ability remained locked after scoreboard unlock");
        }
        var afterInfo = scoreboard.getPlayerScoreInfo(player, objective);
        int scoreAfter = afterInfo == null ? 0 : afterInfo.value();
        require(scoreAfter == ("USE_FLINT_AND_STEEL_UNLOCKED".equals(definition.productionPrecondition()) ? UNLOCK_SCORE : 0),
            "Unexpected interaction production score for " + definition.key());
        boolean lockedLandmark = AchieveToDoMod.isTargetInLockedLandmark(player, helper.getLevel(), target.blockPosition());
        require(!lockedLandmark, "Interaction target is in a locked landmark for " + definition.key());
        return new ProductionPreconditions(
            scoreBefore,
            scoreAfter,
            lockedBefore,
            lockedAfter,
            lockedLandmark,
            definition.productionPrecondition()
        );
    }

    private static SelectedItem resolveSelectedItem(
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition
    ) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(definition.selectedItem()));
        require(item != null, "Missing selected direct item " + definition.selectedItem());
        Holder.Reference<Item> holder = BuiltInRegistries.ITEM.get(Identifier.parse(definition.selectedItem())).orElseThrow();
        return new SelectedItem(item, holder, 1, true, List.of(definition.selectedItem()));
    }

    private static boolean entityTagMembership(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition,
        Entity target
    ) {
        HolderGetter<EntityType<?>> lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(definition.itemTag()));
        HolderSet.Named<EntityType<?>> members = lookup.getOrThrow(tag);
        Holder.Reference<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE
            .get(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())).orElseThrow();
        return members.contains(holder);
    }

    private static Fixture prepareFixture(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition
    ) {
        Level level = helper.getLevel();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(definition.selectedEntityType()));
        require(type != null, "Missing registered entity type " + definition.selectedEntityType());
        Entity target = type.create(level, EntitySpawnReason.COMMAND);
        require(target instanceof LivingEntity, "Selected witness is not a LivingEntity: " + definition.key());
        net.minecraft.core.BlockPos absolute = helper.absolutePos(TARGET_POS);
        target.teleportTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        if ("FEED_HORSE".equals(definition.action())) {
            require(target instanceof AbstractHorse, "Horse-food witness was not an AbstractHorse: " + definition.key());
            AbstractHorse horse = (AbstractHorse) target;
            float maxHealth = horse.getMaxHealth();
            require(maxHealth > 1.0F, "Horse witness has unusable maximum health for " + definition.key());
            horse.setHealth(maxHealth - 1.0F);
            require(horse.getHealth() < maxHealth && horse.getHealth() > 0.0F,
                "Horse-food witness did not enter a healable state for " + definition.key());
        }
        if (target instanceof Dolphin dolphin) {
            dolphin.setGotFish(false);
        }
        if (target instanceof Creeper creeper) {
            require(!creeper.isIgnited(), "Creeper fixture started ignited");
        }
        if (target instanceof Sniffer sniffer) {
            sniffer.setBaby(true);
        }
        require(level.addFreshEntity(target), "Could not add interaction witness to live level");
        require(target.isAlive() && !target.isRemoved(), "Interaction witness was not alive after spawn");
        require(entityId(target).equals(definition.selectedEntityType()), "Spawned witness type mismatch");
        return new Fixture(target);
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(definition.advancementId());
        require(advancement != null, "Missing live advancement " + definition.advancementId());
        return advancement;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedEntityTypeTagPlayerInteractedWithEntityCatalog.CaseDefinition definition
    ) {
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = helper.getLevel().getServer();
        require(player.getClass() == ServerPlayer.class, "Expected plain ServerPlayer for " + definition.key());
        require(server.getPlayerList().getPlayer(player.getUUID()) == player, "Player was not joined for " + definition.key());
        require(server.getConnection().getConnections().contains(joinedPlayer.connection()), "Connection was not registered");
        require(player.connection.hasClientLoaded(), "Client-loaded packet was not accepted");
        require(player.gameMode() == GameType.SURVIVAL && !player.isSpectator(), "Expected SURVIVAL");
        require(!player.hasInfiniteMaterials(), "Expected finite materials for survival interaction");
    }

    private static void establishFiniteSurvival(ServerPlayer player) {
        if (player.gameMode() == GameType.SURVIVAL && player.hasInfiniteMaterials()) {
            require(player.setGameMode(GameType.CREATIVE), "Could not refresh inconsistent survival abilities");
        }
        player.setGameMode(GameType.SURVIVAL);
        require(player.gameMode() == GameType.SURVIVAL && !player.hasInfiniteMaterials(),
            "Could not establish finite-material survival state");
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
        return new JoinedPlayer(player, connection, channel, new AtomicBoolean());
    }

    private static String profileNameForUuid(UUID playerId) {
        String compact = playerId.toString().replace("-", "");
        String result = "ient" + compact.substring(compact.length() - 12);
        require(result.length() <= 16, "Generated profile name exceeds 16 characters");
        return result;
    }

    private static CleanupResult cleanupJoinedServerPlayer(JoinedPlayer joinedPlayer) {
        if (!joinedPlayer.cleaned().compareAndSet(false, true)) {
            return new CleanupResult(true, true, true, 0);
        }
        ServerPlayer player = joinedPlayer.player();
        MinecraftServer server = player.level().getServer();
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        server.getPlayerList().remove(player);
        boolean playerRemoved = server.getPlayerList().getPlayer(player.getUUID()) != player;
        server.getConnection().getConnections().remove(joinedPlayer.connection());
        boolean connectionRemoved = !server.getConnection().getConnections().contains(joinedPlayer.connection());
        int released = settleOpenEmbeddedChannel(joinedPlayer.channel());
        joinedPlayer.connection().disconnect(Component.literal("GameTest cleanup"));
        return new CleanupResult(playerRemoved, connectionRemoved, true, released);
    }

    private static int settleOpenEmbeddedChannel(EmbeddedChannel channel) {
        if (!channel.isOpen()) {
            return 0;
        }
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
                    throw new IllegalStateException("EmbeddedChannel cleanup exceeded message bound");
                }
            }
            if (releasedThisPass == 0 && !channel.hasPendingTasks()) {
                return releasedMessages;
            }
        }
        throw new IllegalStateException("EmbeddedChannel cleanup did not quiesce within pass bound");
    }

    private static void cleanupFixture(Fixture fixture) {
        if (fixture != null && fixture.target() != null && !fixture.target().isRemoved()) {
            fixture.target().discard();
        }
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
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

    private record SelectedItem(
        Item item,
        Holder.Reference<Item> holder,
        int memberCount,
        boolean runtimeMembership,
        List<String> memberIds
    ) {
    }

    private record Fixture(Entity target) {
    }

    private record JoinedPlayer(
        ServerPlayer player,
        Connection connection,
        EmbeddedChannel channel,
        AtomicBoolean cleaned
    ) {
    }

    private record CaseExecution(Fixture fixture, JsonObject receipt) {
    }

    private record CleanupResult(boolean playerRemoved, boolean connectionRemoved, boolean channelSettled, int settlementMessages) {
    }

    private record ProductionPreconditions(
        int scoreBefore,
        int scoreAfter,
        boolean abilityLockedBefore,
        boolean abilityLockedAfter,
        boolean lockedLandmark,
        String ability
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("scoreboardObjective", "bac_advancements");
            json.addProperty("scoreBefore", scoreBefore);
            json.addProperty("scoreAfter", scoreAfter);
            json.addProperty("abilityLockedBefore", abilityLockedBefore);
            json.addProperty("abilityLockedAfter", abilityLockedAfter);
            json.addProperty("lockedLandmark", lockedLandmark);
            json.addProperty("ability", ability);
            return json;
        }
    }

    @FunctionalInterface
    private interface CaseCompletion {
        void complete(GameTestHelper helper);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, net.minecraft.world.level.block.Blocks.AIR);
        method.invoke(this, helper);
    }
}
