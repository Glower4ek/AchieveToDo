package com.diskree.achievetodo.server;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.ability.DimensionalBlockBox;
import com.diskree.achievetodo.ability.LandmarkType;
import com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import com.diskree.achievetodo.client.InternalPack;
import com.diskree.achievetodo.client.Utils;
import com.diskree.achievetodo.injection.extension.main.ChunkExtension;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.injection.extension.main.StructureStartExtension;
import com.diskree.achievetodo.networking.c2s.DemystifyAbilityPayload;
import com.diskree.achievetodo.networking.s2c.*;
import com.diskree.achievetodo.tracking.TrackedScoreType;
import com.diskree.achievetodo.tracking.TrackedStatisticsDataType;
import com.diskree.achievetodo.util.MixinCasting;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AchieveToDoServer implements ServerModInitializer {

    private Map<AbilityType, Integer> abilitiesConfiguration;
    private final Map<UUID, Integer> obtainedAdvancementsCountByPlayers = new Object2IntOpenHashMap<>();
    private final Set<UUID> playersAwaitingInitialSync = new HashSet<>();
    private boolean scoreboardInitializationComplete;
    private boolean missingScoreboardReported;

    private final Map<ChunkPos, Map<LandmarkType, Set<DimensionalBlockBox>>> landmarksByChunks = new HashMap<>();
    private final Map<LandmarkType, Set<UUID>> playersByLockedLandmarkTypes = new HashMap<>();

    private final Map<TrackedScoreType, Map<UUID, Integer>> trackedScores = new HashMap<>();
    private final Map<TrackedStatisticsDataType, Map<UUID, Integer>> trackedStats = new HashMap<>();

    public AdvancementsMode currentAdvancementsMode;
    public Objective currentScoreboardObjective;
    public DisplaySlot currentScoreboardDisplaySlot;

    public void prepareScoreboard(ServerScoreboard scoreboard) {
        AdvancementsMode oldAdvancementsMode = currentAdvancementsMode;
        Objective oldScoreboardObjective = currentScoreboardObjective;
        DisplaySlot oldScoreboardDisplaySlot = currentScoreboardDisplaySlot;

        currentAdvancementsMode = null;
        currentScoreboardObjective = null;
        currentScoreboardDisplaySlot = null;

        DisplaySlot[] slotPriority = {
            DisplaySlot.SIDEBAR, DisplaySlot.LIST, DisplaySlot.BELOW_NAME
        };
        for (DisplaySlot displaySlot : slotPriority) {
            Objective objective = scoreboard.getDisplayObjective(displaySlot);
            if (objective != null) {
                AdvancementsMode advancementsMode = AdvancementsMode.findByObjectiveName(objective.getName());
                if (advancementsMode != null) {
                    currentAdvancementsMode = advancementsMode;
                    currentScoreboardObjective = objective;
                    currentScoreboardDisplaySlot = displaySlot;
                    break;
                }
            }
        }
        if (currentAdvancementsMode == null ||
            currentScoreboardObjective == null ||
            currentScoreboardDisplaySlot == null
        ) {
            if (scoreboardInitializationComplete && !missingScoreboardReported) {
                missingScoreboardReported = true;
                AchieveToDoMod.logger.error(
                "Can't find scoreboard objective with advancements counter! " +
                    "Please check that BACAP datapack is installed " +
                    "and enable advancements counter in the sidebar, tab list or below player names."
                );
            }
            return;
        }
        missingScoreboardReported = false;
        if (currentAdvancementsMode != oldAdvancementsMode ||
            currentScoreboardObjective != oldScoreboardObjective ||
            currentScoreboardDisplaySlot != oldScoreboardDisplaySlot
        ) {
            AchieveToDoMod.logger.info(
                "Scoreboard objective with advancements counter found: advancements mode = {}, objective name = {}, display slot = {}",
                currentAdvancementsMode,
                currentScoreboardObjective.getName(),
                currentScoreboardDisplaySlot
            );
            for (ServerPlayer serverPlayer : scoreboard.server.getPlayerList().getPlayers()) {
                updateObtainedCount(scoreboard, serverPlayer);
            }
        }
    }

    /** END_SERVER_TICK runs after the function manager has executed pending #load functions. */
    public void finishScoreboardInitialization(ServerScoreboard scoreboard) {
        scoreboardInitializationComplete = true;
        prepareScoreboard(scoreboard);
    }

    public void setObtainedCount(@NotNull ServerPlayer player, int obtainedCount) {
        if (isNotReady()) {
            return;
        }
        UUID playerUuid = player.getUUID();
        int oldCount = obtainedAdvancementsCountByPlayers.getOrDefault(playerUuid, Integer.MIN_VALUE);
        obtainedAdvancementsCountByPlayers.put(playerUuid, obtainedCount);
        Set<LandmarkType> unlockedLandmarkTypes = null;
        Map<LandmarkType, Set<DimensionalBlockBox>> lockedLandmarks = null;

        for (AbilityType abilityType : AbilityType.values()) {
            int requiredCount = abilitiesConfiguration.get(abilityType);
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
            ) {
                continue;
            }
            boolean isLock;
            if (obtainedCount >= requiredCount && oldCount < requiredCount) {
                isLock = false;
            } else if (oldCount == Integer.MIN_VALUE || obtainedCount < requiredCount && oldCount >= requiredCount) {
                isLock = true;
            } else {
                continue;
            }
            setAbilityLocked(player, abilityType, isLock);
            LandmarkType landmarkType = abilityType.getLandmarkType();
            if (landmarkType != null) {
                if (isLock) {
                    playersByLockedLandmarkTypes
                        .computeIfAbsent(landmarkType, k -> new HashSet<>())
                        .add(playerUuid);
                    for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                        Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                        if (dimensionalBlockBoxes != null) {
                            if (lockedLandmarks == null) {
                                lockedLandmarks = new HashMap<>();
                            }
                            lockedLandmarks.put(landmarkType, dimensionalBlockBoxes);
                        }
                    }
                } else {
                    Set<UUID> players = playersByLockedLandmarkTypes.get(landmarkType);
                    if (players != null && players.remove(playerUuid) && players.isEmpty()) {
                        playersByLockedLandmarkTypes.remove(landmarkType);
                    }
                    if (unlockedLandmarkTypes == null) {
                        unlockedLandmarkTypes = new HashSet<>();
                    }
                    unlockedLandmarkTypes.add(landmarkType);
                }
            }
        }
        ServerPlayNetworking.send(player, new SyncObtainedAdvancementsCountPayload(obtainedCount));
        if (unlockedLandmarkTypes != null) {
            ServerPlayNetworking.send(player, new LandmarkTypesUnlockedPayload(unlockedLandmarkTypes));
        }
        if (lockedLandmarks != null) {
            ServerPlayNetworking.send(player, new LandmarksLockedStatusChangedPayload(lockedLandmarks, true));
        }
    }

    public void setScore(@NotNull ServerPlayer player, @NotNull TrackedScoreType progressType, int progress) {
        if (progressType.isPercentage()) {
            progress = Math.max(0, Math.min(100, (int) ((progress * 100.0) / progressType.getFinalValue())));
        }
        var progressByPlayers = trackedScores.computeIfAbsent(progressType, k -> new Object2IntOpenHashMap<>());
        Integer currentProgress = progressByPlayers.get(player.getUUID());
        if (currentProgress == null || !currentProgress.equals(progress)) {
            progressByPlayers.put(player.getUUID(), progress);
            ServerPlayNetworking.send(player, new ScoreProgressChangedPayload(progressType, progress));
        }
    }

    public void setStat(@NotNull ServerPlayer player, @NotNull TrackedStatisticsDataType statType, int progress) {
        if (statType.isPercentage()) {
            progress = Math.max(0, Math.min(100, (int) ((progress * 100.0) / statType.getFinalValue())));
        }
        var progressByPlayers = trackedStats.computeIfAbsent(statType, k -> new Object2IntOpenHashMap<>());
        Integer currentProgress = progressByPlayers.get(player.getUUID());
        if (currentProgress == null || !currentProgress.equals(progress)) {
            progressByPlayers.put(player.getUUID(), progress);
            ServerPlayNetworking.send(player, new StatisticsDataProgressChangedPayload(statType, progress));
        }
    }

    public boolean isNotReady() {
        return abilitiesConfiguration == null ||
            currentAdvancementsMode == null ||
            currentScoreboardObjective == null ||
            currentScoreboardDisplaySlot == null;
    }

    public boolean isAbilityLocked(@NotNull ServerPlayer player, @NotNull AbilityType abilityType) {
        return isAbilityLocked(player, abilityType, false);
    }

    public boolean isAbilityLocked(
        @NotNull ServerPlayer player,
        @NotNull AbilityType abilityType,
        boolean checkOnly
    ) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (isNotReady()) {
            return true;
        }
        int obtainedCount = obtainedAdvancementsCountByPlayers.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
        int requiredCount = abilitiesConfiguration.get(abilityType);
        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
            requiredCount != Constants.Progression.PERMANENTLY_LOCKED_FLAG && obtainedCount >= requiredCount
        ) {
            return false;
        }
        if (!checkOnly) {
            Component lockedMessageText;
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                lockedMessageText = abilityType.buildPermanentlyLockedMessage();
            } else {
                int leftCount = requiredCount - obtainedCount;
                lockedMessageText = abilityType.buildUnlockProgressMessage(leftCount);
            }
            player.sendOverlayMessage(lockedMessageText);
            demystifyAbility(player, abilityType);
        }
        return true;
    }

    public boolean isTargetInLockedLandmark(
        @NotNull ServerPlayer actor,
        @NotNull DimensionType targetDimensionType,
        @NotNull AABB targetBox
    ) {
        BoundingBox targetBlockBox = Utils.toBlockBox(targetBox);
        for (var entry : playersByLockedLandmarkTypes.entrySet()) {
            if (entry.getValue().contains(actor.getUUID())) {
                LandmarkType landmarkType = entry.getKey();
                for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                    if (dimensionalBlockBoxes != null) {
                        for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                            if (dimensionalBlockBox.dimensionType() == targetDimensionType &&
                                dimensionalBlockBox.blockBox().intersects(targetBlockBox)
                            ) {
                                AbilityType abilityType = AbilityType.findByLandmarkType(landmarkType);
                                if (abilityType != null) {
                                    boolean isAbilityLocked = isAbilityLocked(actor, abilityType, true);
                                    if (isAbilityLocked) {
                                        ServerPlayNetworking.send(actor, new CheckTargetInLockedLandmarkPayload(
                                            targetDimensionType,
                                            targetBox
                                        ));
                                    }
                                    return isAbilityLocked;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void onLandmarksLoadedStatusChanged(
        @NotNull ServerLevel world,
        @NotNull ChunkPos chunkPos,
        @NotNull Map<LandmarkType, Set<DimensionalBlockBox>> landmarks,
        boolean isLoaded
    ) {
        if (isNotReady()) {
            return;
        }
        boolean isChanged = false;
        if (isLoaded) {
            var chunkMap = landmarksByChunks.computeIfAbsent(chunkPos, k -> new HashMap<>());
            for (var entry : landmarks.entrySet()) {
                LandmarkType landmarkType = entry.getKey();
                AbilityType abilityType = AbilityType.findByLandmarkType(landmarkType);
                if (abilitiesConfiguration.get(abilityType) == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
                    continue;
                }
                if (chunkMap
                    .computeIfAbsent(landmarkType, k -> new HashSet<>())
                    .addAll(entry.getValue())
                ) {
                    isChanged = true;
                }
            }
        } else {
            Map<LandmarkType, Set<DimensionalBlockBox>> chunkMap = landmarksByChunks.get(chunkPos);
            if (chunkMap != null) {
                for (var entry : landmarks.entrySet()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = chunkMap.get(entry.getKey());
                    if (dimensionalBlockBoxes != null && dimensionalBlockBoxes.removeAll(entry.getValue())) {
                        isChanged = true;
                        if (dimensionalBlockBoxes.isEmpty()) {
                            chunkMap.remove(entry.getKey());
                            if (chunkMap.isEmpty()) {
                                landmarksByChunks.remove(chunkPos);
                            }
                        }
                    }
                }
            }
        }
        if (!isChanged) {
            return;
        }
        HashMap<UUID, Set<LandmarkType>> landmarkTypesByPlayers = new HashMap<>();
        for (LandmarkType type : landmarks.keySet()) {
            Set<UUID> playerUuids = playersByLockedLandmarkTypes.get(type);
            if (playerUuids == null) {
                continue;
            }
            for (UUID playerUuid : playerUuids) {
                landmarkTypesByPlayers
                    .computeIfAbsent(playerUuid, k -> new HashSet<>())
                    .add(type);
            }
        }
        PlayerList playerManager = world.getServer().getPlayerList();
        for (UUID playerUuid : landmarkTypesByPlayers.keySet()) {
            ServerPlayer player = playerManager.getPlayer(playerUuid);
            if (player == null) {
                continue;
            }
            Set<LandmarkType> landmarkTypes = landmarkTypesByPlayers.get(playerUuid);
            HashMap<LandmarkType, Set<DimensionalBlockBox>> landmarksToSync = new HashMap<>();
            for (LandmarkType landmarkType : landmarkTypes) {
                landmarksToSync
                    .computeIfAbsent(landmarkType, k -> new HashSet<>())
                    .addAll(landmarks.get(landmarkType));
            }
            ServerPlayNetworking.send(player, new LandmarksLockedStatusChangedPayload(landmarksToSync, isLoaded));
        }
    }

    public void onLandmarkResized(
        @NotNull ServerLevel world,
        @NotNull ChunkPos chunkPos,
        LandmarkType landmarkType,
        DimensionalBlockBox oldDimensionalBlockBox,
        DimensionalBlockBox newDimensionalBlockBox
    ) {
        Map<LandmarkType, Set<DimensionalBlockBox>> chunkMap = landmarksByChunks.get(chunkPos);
        if (chunkMap == null) {
            return;
        }
        Set<DimensionalBlockBox> dimensionalBlockBoxes = chunkMap.get(landmarkType);
        if (dimensionalBlockBoxes == null) {
            return;
        }
        if (!dimensionalBlockBoxes.remove(oldDimensionalBlockBox)) {
            return;
        }
        dimensionalBlockBoxes.add(newDimensionalBlockBox);
        Set<UUID> playerUuids = playersByLockedLandmarkTypes.get(landmarkType);
        if (playerUuids == null) {
            return;
        }
        PlayerList playerManager = world.getServer().getPlayerList();
        for (UUID playerUuid : playerUuids) {
            ServerPlayer player = playerManager.getPlayer(playerUuid);
            if (player == null) {
                continue;
            }
            ServerPlayNetworking.send(player, new LockedLandmarkResizedPayload(
                landmarkType,
                oldDimensionalBlockBox.dimensionType(),
                oldDimensionalBlockBox.blockBox(),
                newDimensionalBlockBox.blockBox()
            ));
        }
    }

    @Override
    public void onInitializeServer() {
        registerInternalDataPacks();
        registerPayloads();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            scoreboardInitializationComplete = false;
            missingScoreboardReported = false;
            LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(server.getWorldData().getLevelSettings());
            abilitiesConfiguration = levelInfoExtension.achievetodo$getAbilitiesConfiguration(
                server.overworld().getSeed()
            );
            prepareScoreboard(server.getScoreboard());
            AchieveToDoMod.logger.info("Abilities configuration loaded");
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            scoreboardInitializationComplete = false;
            missingScoreboardReported = false;
            abilitiesConfiguration = null;
            obtainedAdvancementsCountByPlayers.clear();
            playersAwaitingInitialSync.clear();
            landmarksByChunks.clear();
            playersByLockedLandmarkTypes.clear();
            trackedScores.clear();
            trackedStats.clear();
            currentAdvancementsMode = null;
            currentScoreboardObjective = null;
            currentScoreboardDisplaySlot = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!scoreboardInitializationComplete) {
                finishScoreboardInitialization(server.getScoreboard());
            }
            if (playersAwaitingInitialSync.isEmpty()) {
                return;
            }
            prepareScoreboard(server.getScoreboard());
            if (isNotReady()) {
                Iterator<UUID> iterator = playersAwaitingInitialSync.iterator();
                while (iterator.hasNext()) {
                    UUID playerUuid = iterator.next();
                    iterator.remove();
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                    if (player != null) {
                        player.connection.disconnect(Component.literal(BuildConfig.MOD_NAME + " is not ready yet"));
                    }
                }
                return;
            }
            Iterator<UUID> iterator = playersAwaitingInitialSync.iterator();
            while (iterator.hasNext()) {
                UUID playerUuid = iterator.next();
                iterator.remove();
                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                    syncPlayer(server, player);
                }
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                scoreboardInitializationComplete = false;
                prepareScoreboard(server.getScoreboard());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    updateObtainedCount(server.getScoreboard(), player);
                }
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> onChunkLoadedStatusChanged(world, chunk, true));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> onChunkLoadedStatusChanged(world, chunk, false));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            prepareScoreboard(server.getScoreboard());
            if (isNotReady()) {
                playersAwaitingInitialSync.add(player.getUUID());
                return;
            }
            syncPlayer(server, player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUUID();
            obtainedAdvancementsCountByPlayers.remove(playerUuid);
            playersAwaitingInitialSync.remove(playerUuid);
            for (Map<UUID, Integer> players : trackedScores.values()) {
                players.remove(playerUuid);
            }
            for (Map<UUID, Integer> players : trackedStats.values()) {
                players.remove(playerUuid);
            }
            for (Set<UUID> players : playersByLockedLandmarkTypes.values()) {
                players.remove(playerUuid);
            }
        });
    }

    private static void registerInternalDataPacks() {
        FabricLoader.getInstance().getModContainer(BuildConfig.MOD_ID).ifPresent(modContainer -> {
            for (InternalPack internalPack : InternalPack.values()) {
                ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.parse(internalPack.getDatapackName()),
                    modContainer,
                    ResourcePackActivationType.NORMAL
                );
            }
        });
    }

    private void registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(DemystifyAbilityPayload.ID, (payload, context) ->
            context.player().level().getServer().execute(() -> demystifyAbility(context.player(), payload.abilityType()))
        );
    }

    private void onChunkLoadedStatusChanged(@NotNull ServerLevel world, @NotNull ChunkAccess chunk, boolean isLoaded) {
        DimensionType dimensionType = DimensionType.findByWorld(world.dimension());
        if (dimensionType == null) {
            return;
        }
        Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = null;
        for (StructureStart structureStart : chunk.getAllStarts().values()) {
            StructureStartExtension structureStartExtension = MixinCasting.structureStart(structureStart);
            LandmarkType landmarkType = structureStartExtension.achievetodo$getLandmarkType();
            BoundingBox landmarkBlockBox = structureStartExtension.achievetodo$getLandmarkBlockBox();
            if (landmarkType != null && landmarkBlockBox != null) {
                if (landmarks == null) {
                    landmarks = new HashMap<>();
                }
                landmarks
                    .computeIfAbsent(landmarkType, k -> new HashSet<>())
                    .add(new DimensionalBlockBox(dimensionType, landmarkBlockBox));
            }
        }
        if (chunk instanceof ChunkExtension chunkExtension) {
            Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks =
                chunkExtension.achievetodo$getFeatureLandmarks();
            if (featureLandmarks != null) {
                if (landmarks == null) {
                    landmarks = new HashMap<>();
                }
                landmarks.putAll(featureLandmarks);
            }
        }
        if (landmarks != null) {
            onLandmarksLoadedStatusChanged(world, chunk.getPos(), landmarks, isLoaded);
        }
    }

    private int computeCurrentObtainedCount(Scoreboard scoreboard, String playerName) {
        if (currentAdvancementsMode.isTeamsMode()) {
            PlayerTeam team = scoreboard.getPlayersTeam(playerName);
            if (team == null) {
                return 0;
            }
            int count = 0;
            for (String teamMemberName : team.getPlayers()) {
                ReadOnlyScoreInfo teamMemberScore = scoreboard.getPlayerScoreInfo(
                    ScoreHolder.forNameOnly(teamMemberName),
                    currentScoreboardObjective
                );
                if (teamMemberScore != null) {
                    count += teamMemberScore.value();
                }
            }
            return count;
        }
        ReadOnlyScoreInfo playerScore = scoreboard.getPlayerScoreInfo(
            ScoreHolder.forNameOnly(playerName),
            currentScoreboardObjective
        );
        return playerScore == null ? 0 : playerScore.value();
    }

    private void reconcileObtainedCount(ServerScoreboard scoreboard, @NotNull ServerPlayer player) {
        if (isNotReady()) {
            return;
        }
        setObtainedCount(player, computeCurrentObtainedCount(scoreboard, player.getScoreboardName()));
    }

    private void updateObtainedCount(ServerScoreboard scoreboard, @NotNull ServerPlayer player) {
        reconcileObtainedCount(scoreboard, player);
    }

    public void reconcileScoreHolder(ServerScoreboard scoreboard, String holderName) {
        if (isNotReady()) {
            return;
        }
        Set<String> affectedNames = new HashSet<>();
        affectedNames.add(holderName);
        if (currentAdvancementsMode.isTeamsMode()) {
            PlayerTeam team = scoreboard.getPlayersTeam(holderName);
            if (team != null) {
                affectedNames.addAll(team.getPlayers());
            }
        }
        reconcilePlayers(scoreboard, affectedNames);
    }

    public void reconcileTeamMembers(ServerScoreboard scoreboard, PlayerTeam team) {
        if (isNotReady() || !currentAdvancementsMode.isTeamsMode()) {
            return;
        }
        reconcilePlayers(scoreboard, new HashSet<>(team.getPlayers()));
    }

    private void reconcilePlayers(ServerScoreboard scoreboard, Set<String> playerNames) {
        PlayerList playerList = scoreboard.server.getPlayerList();
        for (String playerName : playerNames) {
            ServerPlayer player = playerList.getPlayerByName(playerName);
            if (player != null) {
                reconcileObtainedCount(scoreboard, player);
            }
        }
    }

    private void syncPlayer(@NotNull net.minecraft.server.MinecraftServer server, @NotNull ServerPlayer player) {
        ServerPlayNetworking.send(player, new SyncAbilitiesConfigurationPayload(abilitiesConfiguration));
        updateObtainedCount(server.getScoreboard(), player);
        ScoreHolder scoreHolder = ScoreHolder.forNameOnly(player.getScoreboardName());
        Scoreboard scoreboard = server.getScoreboard();
        for (var entry : TrackedScoreType.SCORES.entrySet()) {
            ReadOnlyScoreInfo scoreboardScore = scoreboard.getPlayerScoreInfo(
                scoreHolder, scoreboard.getObjective(entry.getKey())
            );
            if (scoreboardScore != null) {
                int score = scoreboardScore.value();
                for (TrackedScoreType type : entry.getValue()) {
                    setScore(player, type, type.fixScore(scoreboard, scoreHolder, score));
                }
            }
        }
        ServerStatsCounter serverStatHandler = player.getStats();
        for (var entry : TrackedStatisticsDataType.STATISTICS_DATA.entrySet()) {
            int statValue = serverStatHandler.getValue(entry.getKey());
            for (TrackedStatisticsDataType trackedStatisticsDataType : entry.getValue()) {
                setStat(player, trackedStatisticsDataType, statValue);
            }
        }
    }

    private void demystifyAbility(@NotNull ServerPlayer player, @NotNull AbilityType ability) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements()
            .get(AbilityAdvancementsGenerator.buildAdvancementId(ability));
        if (advancement == null) {
            AchieveToDoMod.logger.warn("Ability advancement is missing for demystify: {}", ability);
            return;
        }
        player.getAdvancements().award(
            advancement,
            AbilityAdvancementsGenerator.DEMYSTIFIED_CRITERION
        );
    }

    private void setAbilityLocked(@NotNull ServerPlayer player, @NotNull AbilityType ability, boolean isLocked) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements()
            .get(AbilityAdvancementsGenerator.buildAdvancementId(ability));
        if (advancement == null) {
            AchieveToDoMod.logger.warn("Ability advancement is missing for lock state update: {}", ability);
            return;
        }
        PlayerAdvancements advancementTracker = player.getAdvancements();
        if (isLocked) {
            advancementTracker.revoke(advancement, AbilityAdvancementsGenerator.UNLOCKED_CRITERION);
        } else {
            for (String criterion : advancementTracker.getOrStartProgress(advancement).getRemainingCriteria()) {
                advancementTracker.award(advancement, criterion);
            }
        }
    }

}
