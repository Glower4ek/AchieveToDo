package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.tracking.BacapObjectiveDetector;
import com.diskree.achievetodo.tracking.DetectedAdvancementsObjective;
import com.diskree.achievetodo.tracking.NearbyEntityProgressProvider;
import com.diskree.achievetodo.tracking.ProgressionCountResolver;
import com.diskree.achievetodo.tracking.ProgressionDisplaySlot;
import com.diskree.achievetodo.tracking.ProgressionSource;
import com.diskree.achievetodo.tracking.ScoreAccess;
import com.diskree.achievetodo.tracking.TeamProgressionSnapshot;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;
import com.diskree.achievetodo.tracking.TrackedScoreType;
import com.diskree.achievetodo.tracking.TrackedStatisticKey;
import com.diskree.achievetodo.tracking.TrackedStatisticsDataType;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

final class BukkitBacapProgressionSource implements ProgressionSource {

    private static final Map<String, Statistic> CUSTOM_STATISTICS_BY_KEY = buildCustomStatisticsByKey();
    private static final Map<String, String> LEGACY_CUSTOM_STATISTIC_ALIASES = Map.of(
        "eat_cake_slice", "cake_slices_eaten",
        "enchant_item", "item_enchanted"
    );

    private final java.util.function.Supplier<Scoreboard> scoreboardSupplier;
    private final java.util.logging.Logger logger;
    private final NearbyEntityProgressProvider nearbyEntityProgressProvider;
    private final BacapObjectiveDetector objectiveDetector = new BacapObjectiveDetector();
    private final ProgressionCountResolver countResolver = new ProgressionCountResolver();
    private final Map<UUID, Integer> obtainedCounts = new HashMap<>();
    private final Map<UUID, EnumMap<TrackedScoreType, Integer>> trackedScores = new HashMap<>();
    private final Map<UUID, EnumMap<TrackedStatisticsDataType, Integer>> trackedStatistics = new HashMap<>();
    private final Map<UUID, TeamProgressionSnapshot> teamProgressByPlayer = new HashMap<>();
    private final Set<UUID> warnedAboutMissingTeam = new HashSet<>();
    private final Set<TrackedStatisticKey> warnedAboutMissingStatistics = new HashSet<>();

    private DetectedAdvancementsObjective detectedObjective;

    BukkitBacapProgressionSource(JavaPlugin plugin, NearbyEntityProgressProvider nearbyEntityProgressProvider) {
        this(() -> plugin.getServer().getScoreboardManager() == null ? null : plugin.getServer().getScoreboardManager().getMainScoreboard(),
            plugin.getLogger(), nearbyEntityProgressProvider);
    }

    BukkitBacapProgressionSource(java.util.function.Supplier<Scoreboard> scoreboardSupplier, java.util.logging.Logger logger,
                                NearbyEntityProgressProvider nearbyEntityProgressProvider) {
        this.scoreboardSupplier = Objects.requireNonNull(scoreboardSupplier, "scoreboardSupplier");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.nearbyEntityProgressProvider = Objects.requireNonNull(nearbyEntityProgressProvider, "nearbyEntityProgressProvider");
    }

    boolean refreshDetectedObjective() {
        DetectedAdvancementsObjective previous = detectedObjective;
        detectedObjective = detectObjective().orElse(null);
        return !Objects.equals(previous, detectedObjective);
    }

    void refreshPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        refreshTrackedScores(player);
        refreshTrackedStatistics(player);
        refreshObtainedCount(player);
    }

    void refreshCount(Player player) { refreshObtainedCount(player); }

    void clear() {
        obtainedCounts.clear();
        trackedScores.clear();
        trackedStatistics.clear();
        teamProgressByPlayer.clear();
        warnedAboutMissingTeam.clear();
        detectedObjective = null;
    }

    void removePlayer(UUID playerId) {
        obtainedCounts.remove(Objects.requireNonNull(playerId, "playerId"));
        trackedScores.remove(playerId);
        trackedStatistics.remove(playerId);
        teamProgressByPlayer.remove(playerId);
        warnedAboutMissingTeam.remove(playerId);
    }

    @Override
    public boolean isReady() {
        return detectedObjective != null;
    }

    @Override
    public Optional<DetectedAdvancementsObjective> getDetectedObjective() {
        return Optional.ofNullable(detectedObjective);
    }

    @Override
    public int getObtainedAdvancementCount(UUID playerId) {
        return obtainedCounts.getOrDefault(Objects.requireNonNull(playerId, "playerId"), 0);
    }

    @Override
    public int getTrackedScore(UUID playerId, TrackedScoreType scoreType) {
        Objects.requireNonNull(scoreType, "scoreType");
        return trackedScores.getOrDefault(Objects.requireNonNull(playerId, "playerId"), new EnumMap<>(TrackedScoreType.class))
            .getOrDefault(scoreType, 0);
    }

    @Override
    public int getTrackedStatistic(UUID playerId, TrackedStatisticsDataType statisticType) {
        Objects.requireNonNull(statisticType, "statisticType");
        return trackedStatistics.getOrDefault(Objects.requireNonNull(playerId, "playerId"), new EnumMap<>(TrackedStatisticsDataType.class))
            .getOrDefault(statisticType, 0);
    }

    @Override
    public int getNearbyEntityProgress(UUID playerId, TrackedNearbyEntitiesType type) {
        return nearbyEntityProgressProvider.getNearbyEntityProgress(
            Objects.requireNonNull(playerId, "playerId"),
            Objects.requireNonNull(type, "type")
        );
    }

    @Override
    public Optional<TeamProgressionSnapshot> getTeamProgress(UUID playerId) {
        return Optional.ofNullable(teamProgressByPlayer.get(Objects.requireNonNull(playerId, "playerId")));
    }

    private Optional<DetectedAdvancementsObjective> detectObjective() {
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return Optional.empty();
        }
        Map<ProgressionDisplaySlot, String> slots = new EnumMap<>(ProgressionDisplaySlot.class);
        putObjectiveName(scoreboard, DisplaySlot.SIDEBAR, ProgressionDisplaySlot.SIDEBAR, slots);
        putObjectiveName(scoreboard, DisplaySlot.PLAYER_LIST, ProgressionDisplaySlot.LIST, slots);
        putObjectiveName(scoreboard, DisplaySlot.BELOW_NAME, ProgressionDisplaySlot.BELOW_NAME, slots);
        return objectiveDetector.detect(slots);
    }

    private void putObjectiveName(
        Scoreboard scoreboard,
        DisplaySlot displaySlot,
        ProgressionDisplaySlot trackingSlot,
        Map<ProgressionDisplaySlot, String> objectives
    ) {
        Objective objective = scoreboard.getObjective(displaySlot);
        if (objective != null) {
            objectives.put(trackingSlot, objective.getName());
        }
    }

    OptionalInt readCount(String entry) {
        Objects.requireNonNull(entry);
        Scoreboard board=getMainScoreboard();
        if(detectedObjective==null || board==null)return OptionalInt.empty();
        Team team=board.getEntryTeam(entry);
        return countResolver.resolveObtainedAdvancementCount(detectedObjective,entry,team==null?null:team.getName(),
            team==null?Set.of():team.getEntries(),(holder,objective)->lookupScore(board,holder,objective));
    }

    private void refreshObtainedCount(Player player) {
        if (detectedObjective == null) {
            return;
        }
        Scoreboard scoreboard = getMainScoreboard();
        if (scoreboard == null) {
            return;
        }
        String entry = player.getName();
        ScoreAccess scoreAccess = (holderEntry, objectiveName) -> lookupScore(scoreboard, holderEntry, objectiveName);
        if (!detectedObjective.mode().isTeamsMode()) {
            int obtainedCount = countResolver.resolvePersonalCount(detectedObjective.objectiveName(), entry, scoreAccess);
            obtainedCounts.put(player.getUniqueId(), obtainedCount);
            teamProgressByPlayer.remove(player.getUniqueId());
            warnedAboutMissingTeam.remove(player.getUniqueId());
            return;
        }
        Team team = scoreboard.getEntryTeam(entry);
        if (team == null) {
            obtainedCounts.put(player.getUniqueId(), 0);
            if (warnedAboutMissingTeam.add(player.getUniqueId())) {
                logger.warning("BACAP team objective is active, but player " + entry + " is not assigned to any scoreboard team");
            }
            teamProgressByPlayer.remove(player.getUniqueId());
            return;
        }
        warnedAboutMissingTeam.remove(player.getUniqueId());
        Optional<TeamProgressionSnapshot> teamProgress = countResolver.resolveTeamProgress(
            detectedObjective.objectiveName(),
            team.getName(),
            team.getEntries(),
            scoreAccess
        );
        if (teamProgress.isPresent()) {
            TeamProgressionSnapshot snapshot = teamProgress.get();
            obtainedCounts.put(player.getUniqueId(), snapshot.obtainedAdvancementCount());
            teamProgressByPlayer.put(player.getUniqueId(), snapshot);
        } else {
            obtainedCounts.put(player.getUniqueId(), 0);
            teamProgressByPlayer.remove(player.getUniqueId());
        }
    }

    private void refreshTrackedScores(Player player) {
        Scoreboard playerScoreboard = player.getScoreboard();
        Scoreboard fallbackScoreboard = getMainScoreboard();
        String entry = player.getName();
        ScoreAccess scoreAccess = (holderEntry, objectiveName) -> {
            OptionalInt primaryScore = lookupScore(playerScoreboard, holderEntry, objectiveName);
            if (primaryScore.isPresent()) {
                return primaryScore;
            }
            return fallbackScoreboard == null ? OptionalInt.empty() : lookupScore(fallbackScoreboard, holderEntry, objectiveName);
        };
        EnumMap<TrackedScoreType, Integer> progress = new EnumMap<>(TrackedScoreType.class);
        for (TrackedScoreType scoreType : TrackedScoreType.values()) {
            OptionalInt rawScore = scoreType.getObjectiveNames().stream()
                .mapToInt(objectiveName -> scoreAccess.getScore(entry, objectiveName).orElse(Integer.MIN_VALUE))
                .filter(value -> value != Integer.MIN_VALUE)
                .findFirst();
            if (rawScore.isPresent()) {
                int fixedScore = scoreType.fixScore(scoreAccess, entry, rawScore.getAsInt());
                progress.put(scoreType, scoreType.normalizeProgress(fixedScore));
            }
        }
        trackedScores.put(player.getUniqueId(), progress);
    }

    private void refreshTrackedStatistics(Player player) {
        EnumMap<TrackedStatisticsDataType, Integer> progress = new EnumMap<>(TrackedStatisticsDataType.class);
        for (TrackedStatisticsDataType statisticType : TrackedStatisticsDataType.values()) {
            int rawValue = readStatistic(player, statisticType.getStatisticKey());
            progress.put(statisticType, statisticType.normalizeProgress(rawValue));
        }
        trackedStatistics.put(player.getUniqueId(), progress);
    }

    private int readStatistic(Player player, TrackedStatisticKey statisticKey) {
        try {
            return switch (statisticKey.category()) {
                case CUSTOM -> player.getStatistic(resolveCustomStatistic(statisticKey));
                case ITEM_USED -> player.getStatistic(Statistic.USE_ITEM, resolveMaterial(statisticKey));
                case BLOCK_MINED -> player.getStatistic(Statistic.MINE_BLOCK, resolveMaterial(statisticKey));
                case ENTITY_KILLED -> player.getStatistic(Statistic.KILL_ENTITY, resolveEntityType(statisticKey));
            };
        } catch (IllegalArgumentException exception) {
            if (warnedAboutMissingStatistics.add(statisticKey)) {
                logger.log(
                    Level.WARNING,
                    "Unable to resolve tracked BACAP statistic " + statisticKey + " against Paper 26.2 API",
                    exception
                );
            }
            return 0;
        }
    }

    private Statistic resolveCustomStatistic(TrackedStatisticKey statisticKey) {
        String customKey = statisticKey.key().toLowerCase(Locale.ROOT);
        Statistic statistic = CUSTOM_STATISTICS_BY_KEY.get(customKey);
        if (statistic != null) {
            return statistic;
        }
        String aliasKey = LEGACY_CUSTOM_STATISTIC_ALIASES.get(customKey);
        if (aliasKey != null) {
            statistic = CUSTOM_STATISTICS_BY_KEY.get(aliasKey);
            if (statistic != null) {
                return statistic;
            }
        }
        throw new IllegalArgumentException("Unknown custom statistic " + statisticKey.key());
    }

    private Material resolveMaterial(TrackedStatisticKey statisticKey) {
        Material material = Material.matchMaterial(statisticKey.key());
        if (material == null) {
            throw new IllegalArgumentException("Unknown material " + statisticKey.key() + " for " + statisticKey.category());
        }
        return material;
    }

    private EntityType resolveEntityType(TrackedStatisticKey statisticKey) {
        return EntityType.valueOf(statisticKey.key().toUpperCase(java.util.Locale.ROOT));
    }

    private static Map<String, Statistic> buildCustomStatisticsByKey() {
        Map<String, Statistic> statisticsByKey = new HashMap<>();
        for (Statistic statistic : Statistic.values()) {
            if (statistic.getType() != Statistic.Type.UNTYPED) {
                continue;
            }
            statisticsByKey.put(statistic.name().toLowerCase(Locale.ROOT), statistic);
            statisticsByKey.put(statistic.getKey().getKey().toLowerCase(Locale.ROOT), statistic);
        }
        return Collections.unmodifiableMap(statisticsByKey);
    }

    private OptionalInt lookupScore(Scoreboard scoreboard, String holderEntry, String objectiveName) {
        if (scoreboard == null) {
            return OptionalInt.empty();
        }
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return OptionalInt.empty();
        }
        Score score = objective.getScore(holderEntry);
        if (!score.isScoreSet()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(score.getScore());
    }

    private Scoreboard getMainScoreboard() {
        return scoreboardSupplier.get();
    }
}
