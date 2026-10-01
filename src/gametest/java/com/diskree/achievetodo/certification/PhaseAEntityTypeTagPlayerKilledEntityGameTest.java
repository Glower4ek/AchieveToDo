package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PhaseAEntityTypeTagPlayerKilledEntityGameTest implements CustomTestMethodInvoker {
    private static final BlockPos ARENA_FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos WITNESS_POS = new BlockPos(1, 2, 1);
    private static final BlockPos PLAYER_POS = new BlockPos(1, 2, 3);
    private static final int EXPECTED_SUPPORTED_CASE_COUNT = 2;
    private static final int MAX_POLL_ATTEMPTS = 20;
    private static final String COMBAT_BOUNDARY = "net.minecraft.server.level.ServerPlayer#attack(net.minecraft.world.entity.Entity)";

    private record JoinedPlayer(ServerPlayer player, Connection connection) {
    }

    private record SelectedWitness(
        Identifier tagId,
        Holder.Reference<EntityType<?>> holder,
        HolderSet.Named<EntityType<?>> holderSet,
        Identifier entityTypeId,
        int memberCount
    ) {
    }

    private record DiagnosticContext(
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition,
        int caseIndex,
        JoinedPlayer joinedPlayer,
        AdvancementHolder advancement,
        LivingEntity witness,
        SelectedWitness selectedWitness,
        boolean criterionBefore,
        boolean entityAliveBefore,
        String preparationProgression,
        int preparationTicks,
        CombatSnapshot preAttackSnapshot,
        CombatSnapshot immediatePostAttackSnapshot,
        CombatTrace trace
    ) {
    }

    private record CombatSnapshot(
        boolean clientLoaded,
        GameType gameMode,
        String mainHandItem,
        String playerPosition,
        float attackStrengthScale,
        String lastHurtMob,
        String witnessType,
        float witnessHealth,
        float witnessMaxHealth,
        boolean witnessAlive,
        boolean witnessRemoved,
        String witnessPosition,
        double distanceFromPlayer,
        boolean witnessOnFire,
        int witnessRemainingFireTicks,
        boolean canSeeSky,
        long gameTime,
        long dayTime,
        String difficulty,
        boolean tagExists,
        int tagMemberCount,
        boolean selectedWitnessInTag,
        String killCredit,
        String lastHurtByPlayer,
        boolean criterionDone,
        String lastDamageSource
    ) {
    }

    private static final class CombatTrace {
        private final CombatSnapshot preAttackSnapshot;
        private final CombatSnapshot immediatePostAttackSnapshot;
        private CombatSnapshot lastPolledSnapshot;

        private CombatTrace(CombatSnapshot preAttackSnapshot, CombatSnapshot immediatePostAttackSnapshot) {
            this.preAttackSnapshot = preAttackSnapshot;
            this.immediatePostAttackSnapshot = immediatePostAttackSnapshot;
            this.lastPolledSnapshot = immediatePostAttackSnapshot;
        }
    }

    private static final class PreparationTrace {
        private final StringBuilder attackStrengthProgression = new StringBuilder();

        private void append(int tick, float attackStrengthScale) {
            if (!attackStrengthProgression.isEmpty()) {
                attackStrengthProgression.append(", ");
            }
            attackStrengthProgression.append("tick=").append(tick).append(":").append(formatFloat(attackStrengthScale));
        }
    }

    @GameTest(maxTicks = 12000)
    public void entityTypeTagPlayerKilledEntityCertification(GameTestHelper helper) {
        List<CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition> cases = CertifiedEntityTypeTagPlayerKilledEntityCatalog.allCases();
        if (cases.size() != EXPECTED_SUPPORTED_CASE_COUNT) {
            throw new IllegalStateException(
                "Expected exactly " + EXPECTED_SUPPORTED_CASE_COUNT + " supported ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY cases but found " + cases.size()
            );
        }
        runCertifiedCase(helper, cases, 0);
    }

    private static void runCertifiedCase(
        GameTestHelper helper,
        List<CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition> cases,
        int caseIndex
    ) {
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition = cases.get(caseIndex);
        JoinedPlayer joinedPlayer = null;
        LivingEntity witness = null;
        try {
            prepareArena(helper);
            final JoinedPlayer createdJoinedPlayer = createJoinedServerPlayer(helper);
            joinedPlayer = createdJoinedPlayer;
            ServerPlayer player = createdJoinedPlayer.player();
            player.setGameMode(GameType.SURVIVAL);
            new ServerboundPlayerLoadedPacket().handle(player.connection);
            assertJoinedLifecycle(helper, createdJoinedPlayer, caseDefinition);

            SelectedWitness selectedWitness = resolveSelectedWitness(helper, caseDefinition);
            final LivingEntity spawnedWitness = spawnWitness(helper, selectedWitness);
            witness = spawnedWitness;
            helper.runAfterDelay(1, () -> continueCase(
                helper,
                caseDefinition,
                caseIndex,
                createdJoinedPlayer,
                spawnedWitness,
                selectedWitness,
                0,
                new PreparationTrace()
            ));
        } catch (Throwable t) {
            cleanupWitness(witness);
            if (joinedPlayer != null) {
                cleanupJoinedServerPlayer(helper, joinedPlayer);
            }
            throw t;
        }
    }

    private static void continueCase(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition,
        int caseIndex,
        JoinedPlayer joinedPlayer,
        LivingEntity witness,
        SelectedWitness selectedWitness,
        int preparationTick,
        PreparationTrace preparationTrace
    ) {
        try {
            ServerPlayer player = joinedPlayer.player();
            if (requiresShade(selectedWitness) && helper.getLevel().canSeeSky(witness.blockPosition())) {
                cleanupWitness(witness);
                cleanupJoinedServerPlayer(helper, joinedPlayer);
                helper.fail("Witness remained exposed to sky after arena preparation for " + caseKey(caseDefinition));
                return;
            }

            AdvancementHolder advancement = advancementOrThrow(helper, caseDefinition);
            AdvancementProgress progressBefore = player.getAdvancements().getOrStartProgress(advancement);
            CriterionProgress criterionProgressBefore = progressBefore.getCriterion(caseDefinition.criterion());
            if (criterionProgressBefore == null) {
                throw new IllegalStateException("Live AdvancementProgress did not expose criterion " + caseKey(caseDefinition));
            }
            boolean criterionBefore = criterionProgressBefore.isDone();
            if (criterionBefore) {
                throw new IllegalStateException("Criterion already complete before combat for " + caseKey(caseDefinition));
            }
            if (!witness.isAlive()) {
                throw new IllegalStateException("Witness was not alive before combat for " + caseKey(caseDefinition));
            }

            player.teleportTo(helper.absolutePos(PLAYER_POS).getX() + 0.5D, helper.absolutePos(PLAYER_POS).getY(), helper.absolutePos(PLAYER_POS).getZ() + 0.5D);
            player.getInventory().clearContent();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));

            waitForNaturalAttackReadiness(helper, caseDefinition, caseIndex, joinedPlayer, advancement, witness, selectedWitness, criterionBefore, preparationTick, preparationTrace);
        } catch (Throwable t) {
            cleanupWitness(witness);
            cleanupJoinedServerPlayer(helper, joinedPlayer);
            throw t;
        }
    }

    private static void waitForNaturalAttackReadiness(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition,
        int caseIndex,
        JoinedPlayer joinedPlayer,
        AdvancementHolder advancement,
        LivingEntity witness,
        SelectedWitness selectedWitness,
        boolean criterionBefore,
        int preparationTick,
        PreparationTrace preparationTrace
    ) {
        ServerPlayer player = joinedPlayer.player();
        AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionProgressAfter = progressAfter.getCriterion(caseDefinition.criterion());
        boolean criterionStillFalse = criterionProgressAfter != null && criterionProgressAfter.isDone();
        boolean playerLoaded = player.connection.hasClientLoaded();
        boolean survival = player.gameMode() == GameType.SURVIVAL && !player.isSpectator();
        boolean witnessAlive = witness.isAlive();
        boolean witnessRemoved = witness.isRemoved();
        float attackStrengthScale = player.getAttackStrengthScale(0.0F);
        preparationTrace.append(preparationTick, attackStrengthScale);

        if (!playerLoaded || !survival || !witnessAlive || witnessRemoved || criterionStillFalse) {
            cleanupWitness(witness);
            cleanupJoinedServerPlayer(helper, joinedPlayer);
            helper.fail(
                "Natural readiness preconditions failed for " + caseKey(caseDefinition)
                    + " | ticks=" + preparationTick
                    + " | attackStrengthProgression=" + preparationTrace.attackStrengthProgression
                    + " | playerLoaded=" + playerLoaded
                    + " | survival=" + survival
                    + " | witnessAlive=" + witnessAlive
                    + " | witnessRemoved=" + witnessRemoved
                    + " | criterionStillFalse=" + criterionStillFalse
            );
            return;
        }

        if (attackStrengthScale >= 1.0F) {
            AbilityType attackToolAbility = AbilityType.findToolMaterialUsageAbility(Items.NETHERITE_SWORD);
            if (attackToolAbility != AbilityType.USE_NETHERITE_TOOLS) {
                cleanupWitness(witness);
                cleanupJoinedServerPlayer(helper, joinedPlayer);
                helper.fail("Expected NETHERITE_SWORD to map to USE_NETHERITE_TOOLS for " + caseKey(caseDefinition));
                return;
            }
            boolean lockedBeforeFixture = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_NETHERITE_TOOLS);
            AchieveToDoMod.getServer().setObtainedCount(player, 422);
            boolean lockedAfterFixture = AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_NETHERITE_TOOLS);
            if (lockedAfterFixture) {
                cleanupWitness(witness);
                cleanupJoinedServerPlayer(helper, joinedPlayer);
                helper.fail(
                    "USE_NETHERITE_TOOLS remained locked at combat boundary for " + caseKey(caseDefinition)
                        + " | lockedBeforeFixture=" + lockedBeforeFixture
                );
                return;
            }
            CombatSnapshot preAttackSnapshot = captureSnapshot(player, witness, selectedWitness, criterionBefore);
            CombatSnapshot immediatePostAttackSnapshot;
            try {
                player.attack(witness);
                immediatePostAttackSnapshot = captureSnapshot(
                    player,
                    witness,
                    selectedWitness,
                    criterionDone(player, advancement, caseDefinition.criterion())
                );
            } catch (Throwable t) {
                cleanupWitness(witness);
                cleanupJoinedServerPlayer(helper, joinedPlayer);
                throw t;
            }
            DiagnosticContext context = new DiagnosticContext(
                caseDefinition,
                caseIndex,
                joinedPlayer,
                advancement,
                witness,
                selectedWitness,
                criterionBefore,
                true,
                preparationTrace.attackStrengthProgression.toString(),
                preparationTick,
                preAttackSnapshot,
                immediatePostAttackSnapshot,
                new CombatTrace(preAttackSnapshot, immediatePostAttackSnapshot)
            );
            pollForCompletion(helper, context, 0);
            return;
        }

        if (preparationTick >= 40) {
            cleanupWitness(witness);
            cleanupJoinedServerPlayer(helper, joinedPlayer);
            helper.fail(
                "Natural readiness was never reached for " + caseKey(caseDefinition)
                    + " | ticks=" + preparationTick
                    + " | attackStrengthProgression=" + preparationTrace.attackStrengthProgression
            );
            return;
        }

        helper.runAfterDelay(1, () -> waitForNaturalAttackReadiness(
            helper,
            caseDefinition,
            caseIndex,
            joinedPlayer,
            advancement,
            witness,
            selectedWitness,
            criterionBefore,
            preparationTick + 1,
            preparationTrace
        ));
    }

    private static void pollForCompletion(GameTestHelper helper, DiagnosticContext context, int tickDelay) {
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition = context.caseDefinition();
        ServerPlayer player = context.joinedPlayer().player();
        LivingEntity witness = context.witness();
        AdvancementProgress progressAfter = player.getAdvancements().getOrStartProgress(context.advancement());
        CriterionProgress criterionProgressAfter = progressAfter.getCriterion(caseDefinition.criterion());
        boolean criterionAfter = criterionProgressAfter != null && criterionProgressAfter.isDone();
        boolean entityAliveAfter = witness.isAlive();
        boolean entityRemovedAfter = witness.isRemoved();
        boolean playerKillAttributed = witness.getKillCredit() == player || witness.getLastHurtByPlayer() == player;
        if (tickDelay > 0) {
            context.trace().lastPolledSnapshot = captureSnapshot(player, witness, context.selectedWitness(), criterionAfter);
        }

        if (criterionAfter && (!entityAliveAfter || entityRemovedAfter) && playerKillAttributed) {
            try {
                PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.recordGreen(
                    PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.projectRoot(),
                    new PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.RuntimeExecutionEntry(
                        caseDefinition.advancementId().toString(),
                        caseDefinition.criterion(),
                        caseDefinition.trigger(),
                        caseDefinition.entityTypeTag(),
                        context.selectedWitness().entityTypeId().toString(),
                        true,
                        context.selectedWitness().memberCount(),
                        context.selectedWitness().holderSet().contains(context.selectedWitness().holder()),
                        context.criterionBefore(),
                        true,
                        context.entityAliveBefore(),
                        entityAliveAfter,
                        entityRemovedAfter,
                        COMBAT_BOUNDARY,
                        "ServerPlayer.attack(" + context.selectedWitness().entityTypeId() + ")",
                        true,
                        tickDelay,
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.FAMILY,
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.SOURCE,
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.GREEN,
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.currentCatalogFingerprint(
                            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.projectRoot()
                        ),
                        currentRunId(),
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.MINECRAFT_VERSION,
                        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.COMPATIBILITY_MARKER
                    )
                );
            } catch (Exception e) {
                cleanupWitness(witness);
                cleanupJoinedServerPlayer(helper, context.joinedPlayer());
                helper.fail("Failed to record GREEN runtime evidence for " + caseKey(caseDefinition) + ": " + e.getMessage());
                return;
            }
            cleanupWitness(witness);
            cleanupJoinedServerPlayer(helper, context.joinedPlayer());
            if (context.caseIndex() + 1 < CertifiedEntityTypeTagPlayerKilledEntityCatalog.allCases().size()) {
                helper.runAfterDelay(1, () -> runCertifiedCase(
                    helper,
                    CertifiedEntityTypeTagPlayerKilledEntityCatalog.allCases(),
                    context.caseIndex() + 1
                ));
            } else {
                helper.succeed();
            }
            return;
        }

        if (tickDelay >= MAX_POLL_ATTEMPTS) {
            cleanupWitness(witness);
            cleanupJoinedServerPlayer(helper, context.joinedPlayer());
            helper.fail(buildFailureReport(context, tickDelay));
            return;
        }
        helper.runAfterDelay(1, () -> pollForCompletion(helper, context, tickDelay + 1));
    }

    private static CombatSnapshot captureSnapshot(
        ServerPlayer player,
        LivingEntity witness,
        SelectedWitness selectedWitness,
        boolean criterionDone
    ) {
        DamageSource lastDamageSource = witness.getLastDamageSource();
        Level level = witness.level();
        return new CombatSnapshot(
            player.connection.hasClientLoaded(),
            player.gameMode(),
            BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString(),
            formatPosition(player),
            player.getAttackStrengthScale(0.0F),
            describeEntity(player.getLastHurtMob()),
            BuiltInRegistries.ENTITY_TYPE.getKey(witness.getType()).toString(),
            witness.getHealth(),
            witness.getMaxHealth(),
            witness.isAlive(),
            witness.isRemoved(),
            formatPosition(witness),
            player.distanceTo(witness),
            witness.isOnFire(),
            witness.getRemainingFireTicks(),
            level.canSeeSky(witness.blockPosition()),
            level.getGameTime(),
            level.getOverworldClockTime(),
            level.getDifficulty().name(),
            true,
            selectedWitness.memberCount(),
            selectedWitness.holderSet().contains(selectedWitness.holder()),
            describeEntity(witness.getKillCredit()),
            describeEntity(witness.getLastHurtByPlayer()),
            criterionDone,
            describeDamageSource(lastDamageSource)
        );
    }

    private static boolean criterionDone(
        ServerPlayer player,
        AdvancementHolder advancement,
        String criterion
    ) {
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        CriterionProgress criterionProgress = progress.getCriterion(criterion);
        return criterionProgress != null && criterionProgress.isDone();
    }

    private static String buildFailureReport(DiagnosticContext context, int tickDelay) {
        CombatSnapshot preAttack = context.preAttackSnapshot();
        CombatSnapshot immediate = context.immediatePostAttackSnapshot();
        CombatSnapshot lastSnapshot = context.trace().lastPolledSnapshot;
        return "Player-kill proof failed for " + caseKey(context.caseDefinition())
            + " | selectedEntityType=" + context.selectedWitness().entityTypeId()
            + " | tagExists=" + preAttack.tagExists()
            + " | tagMemberCount=" + preAttack.tagMemberCount()
            + " | selectedWitnessInTag=" + preAttack.selectedWitnessInTag()
            + " | prePlayerLoaded=" + preAttack.clientLoaded()
            + " | prePlayerGameMode=" + preAttack.gameMode()
            + " | preMainHand=" + preAttack.mainHandItem()
            + " | prePlayerPos=" + preAttack.playerPosition()
            + " | preAttackStrengthScale=" + formatFloat(preAttack.attackStrengthScale())
            + " | preLastHurtMob=" + preAttack.lastHurtMob()
            + " | preWitnessHealth=" + formatFloat(preAttack.witnessHealth()) + "/" + formatFloat(preAttack.witnessMaxHealth())
            + " | preWitnessAlive=" + preAttack.witnessAlive()
            + " | preWitnessRemoved=" + preAttack.witnessRemoved()
            + " | preWitnessPos=" + preAttack.witnessPosition()
            + " | preDistance=" + formatDouble(preAttack.distanceFromPlayer())
            + " | preWitnessOnFire=" + preAttack.witnessOnFire()
            + " | preRemainingFireTicks=" + preAttack.witnessRemainingFireTicks()
            + " | canSeeSky=" + preAttack.canSeeSky()
            + " | gameTime=" + preAttack.gameTime()
            + " | dayTime=" + preAttack.dayTime()
            + " | difficulty=" + preAttack.difficulty()
            + " | criterionBefore=" + preAttack.criterionDone()
            + " | preparationTicks=" + context.preparationTicks()
            + " | preparationProgression=" + context.preparationProgression()
            + " | immediateHealth=" + formatFloat(immediate.witnessHealth())
            + " | immediateAlive=" + immediate.witnessAlive()
            + " | immediateRemoved=" + immediate.witnessRemoved()
            + " | immediateOnFire=" + immediate.witnessOnFire()
            + " | immediateRemainingFireTicks=" + immediate.witnessRemainingFireTicks()
            + " | immediateKillCredit=" + immediate.killCredit()
            + " | immediateLastHurtByPlayer=" + immediate.lastHurtByPlayer()
            + " | immediateCriterion=" + immediate.criterionDone()
            + " | immediateLastDamageSource=" + immediate.lastDamageSource()
            + " | finalHealth=" + formatFloat(lastSnapshot.witnessHealth())
            + " | finalAlive=" + lastSnapshot.witnessAlive()
            + " | finalRemoved=" + lastSnapshot.witnessRemoved()
            + " | finalOnFire=" + lastSnapshot.witnessOnFire()
            + " | finalRemainingFireTicks=" + lastSnapshot.witnessRemainingFireTicks()
            + " | finalKillCredit=" + lastSnapshot.killCredit()
            + " | finalLastHurtByPlayer=" + lastSnapshot.lastHurtByPlayer()
            + " | finalCriterion=" + lastSnapshot.criterionDone()
            + " | finalLastDamageSource=" + lastSnapshot.lastDamageSource()
            + " | playerKillAttributedFinal=" + ("serverplayer:minecraft:player".equals(lastSnapshot.killCredit())
                || "serverplayer:minecraft:player".equals(lastSnapshot.lastHurtByPlayer()))
            + " | ticks=" + tickDelay;
    }

    private static String formatPosition(Entity entity) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", entity.getX(), entity.getY(), entity.getZ());
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String valueOrNone(Object value) {
        return value == null ? "none" : value.toString();
    }

    private static String describeEntity(Entity entity) {
        if (entity == null) {
            return "none";
        }
        return entity.getClass().getSimpleName().toLowerCase(Locale.ROOT)
            + ":" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    private static String describeDamageSource(DamageSource damageSource) {
        if (damageSource == null) {
            return "none";
        }
        return "type=" + damageSource.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("unkeyed")
            + ",direct=" + describeEntity(damageSource.getDirectEntity())
            + ",causing=" + describeEntity(damageSource.getEntity());
    }

    private static String currentRunId() throws IOException {
        JsonObject json = JsonParser.parseString(
            Files.readString(
                PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.projectRoot()
                    .resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.RUN_STATE_ARTIFACT),
                StandardCharsets.UTF_8
            )
        ).getAsJsonObject();
        return json.get("runId").getAsString();
    }

    private static AdvancementHolder advancementOrThrow(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition
    ) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(caseDefinition.advancementId());
        if (advancement == null) {
            throw new IllegalStateException("Missing advancement: " + caseDefinition.advancementId());
        }
        return advancement;
    }

    private static SelectedWitness resolveSelectedWitness(
        GameTestHelper helper,
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition
    ) {
        HolderGetter<EntityType<?>> entityTypeLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(caseDefinition.entityTypeTag()));
        HolderSet.Named<EntityType<?>> holderSet = entityTypeLookup.getOrThrow(tagKey);
        List<Holder.Reference<EntityType<?>>> members = holderSet.stream()
            .map(holder -> (Holder.Reference<EntityType<?>>) holder)
            .sorted(Comparator.comparing(holder -> holder.unwrapKey().orElseThrow().identifier().toString()))
            .toList();
        if (members.isEmpty()) {
            throw new IllegalStateException("Resolved empty entity-type tag " + caseDefinition.entityTypeTag() + " for " + caseKey(caseDefinition));
        }
        Identifier preferredWitnessId = Identifier.parse(
            "blazeandcave:piglins".equals(caseDefinition.entityTypeTag()) ? "minecraft:piglin" : "minecraft:zombie"
        );
        Holder.Reference<EntityType<?>> selected = members.stream()
            .filter(holder -> holder.unwrapKey().orElseThrow().identifier().equals(preferredWitnessId))
            .findFirst()
            .orElse(members.getFirst());
        if (!holderSet.contains(selected)) {
            throw new IllegalStateException("Selected witness holder was not contained in resolved tag " + caseDefinition.entityTypeTag());
        }
        return new SelectedWitness(
            Identifier.parse(caseDefinition.entityTypeTag()),
            selected,
            holderSet,
            selected.unwrapKey().orElseThrow().identifier(),
            members.size()
        );
    }

    private static LivingEntity spawnWitness(GameTestHelper helper, SelectedWitness selectedWitness) {
        Entity entity = selectedWitness.holder().value().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (!(entity instanceof LivingEntity livingEntity)) {
            throw new IllegalStateException("Selected witness type was not a LivingEntity: " + selectedWitness.entityTypeId());
        }
        livingEntity.teleportTo(helper.absolutePos(WITNESS_POS).getX() + 0.5D, helper.absolutePos(WITNESS_POS).getY(), helper.absolutePos(WITNESS_POS).getZ() + 0.5D);
        if (livingEntity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        if (livingEntity instanceof Zombie zombie) {
            zombie.setBaby(false);
        }
        livingEntity.setHealth(1.0F);
        if (!helper.getLevel().addFreshEntity(livingEntity)) {
            throw new IllegalStateException("Failed to spawn witness entity " + selectedWitness.entityTypeId());
        }
        return livingEntity;
    }

    private static void assertJoinedLifecycle(
        GameTestHelper helper,
        JoinedPlayer joinedPlayer,
        CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition
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
        GameProfile profile = new GameProfile(playerId, "entity-tag-kill-test-player");
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

    private static void cleanupWitness(LivingEntity witness) {
        if (witness != null && !witness.isRemoved()) {
            witness.discard();
        }
    }

    private static void prepareArena(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(ARENA_FLOOR);
        BlockPos witness = helper.absolutePos(WITNESS_POS);
        BlockPos player = helper.absolutePos(PLAYER_POS);
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(witness.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(player.below(), Blocks.STONE.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.getLevel().setBlockAndUpdate(witness.offset(dx, 4, dz), Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(witness.offset(dx, 5, dz), Blocks.STONE.defaultBlockState());
            }
        }
        helper.getLevel().setBlockAndUpdate(witness, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(player, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(witness.above(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(player.above(), Blocks.AIR.defaultBlockState());
    }

    private static String caseKey(CertifiedEntityTypeTagPlayerKilledEntityCatalog.CaseDefinition caseDefinition) {
        return caseDefinition.advancementId() + "#" + caseDefinition.criterion();
    }

    private static boolean requiresShade(SelectedWitness selectedWitness) {
        return "minecraft:zombie".equals(selectedWitness.entityTypeId().toString());
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
