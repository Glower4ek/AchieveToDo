package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.tracking.NearbyEntityProgressProvider;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

final class BukkitNearbyEntityProgressService implements NearbyEntityProgressProvider, AutoCloseable {

    private static final long SCAN_INTERVAL_TICKS = 20L;
    private static final long SCAN_WARN_NANOS = 15_000_000L;

    private final JavaPlugin plugin;
    private final BukkitNearbyEntityScanner scanner;
    private final Consumer<UUID> dirtyPlayerSink;
    private final Map<UUID, EnumMap<TrackedNearbyEntitiesType, Integer>> progressByPlayer = new HashMap<>();

    private BukkitTask scanTask;

    BukkitNearbyEntityProgressService(JavaPlugin plugin, Consumer<UUID> dirtyPlayerSink) {
        this(plugin, new BukkitNearbyEntityScanner(), dirtyPlayerSink);
    }

    BukkitNearbyEntityProgressService(
        JavaPlugin plugin,
        BukkitNearbyEntityScanner scanner,
        Consumer<UUID> dirtyPlayerSink
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.dirtyPlayerSink = Objects.requireNonNull(dirtyPlayerSink, "dirtyPlayerSink");
    }

    void start() {
        if (scanTask != null) {
            return;
        }
        scanTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::scanOnlinePlayers,
            SCAN_INTERVAL_TICKS,
            SCAN_INTERVAL_TICKS
        );
    }

    void refreshPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        updateSnapshot(player.getUniqueId(), scanner.scan(player).progressByType());
    }

    void removePlayer(UUID playerId) {
        progressByPlayer.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public int getNearbyEntityProgress(UUID playerId, TrackedNearbyEntitiesType type) {
        Objects.requireNonNull(type, "type");
        EnumMap<TrackedNearbyEntitiesType, Integer> progress = progressByPlayer.get(Objects.requireNonNull(playerId, "playerId"));
        return progress == null ? 0 : progress.getOrDefault(type, 0);
    }

    @Override
    public void close() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        progressByPlayer.clear();
    }

    private void scanOnlinePlayers() {
        long startedAt = System.nanoTime();
        long maxPlayerElapsed = 0L;
        int inspectedEntities = 0;
        int scannedPlayers = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            long playerStartedAt = System.nanoTime();
            BukkitNearbyEntityScanner.BukkitNearbyEntityScanResult scanResult = scanner.scan(player);
            long playerElapsed = System.nanoTime() - playerStartedAt;
            if (playerElapsed > maxPlayerElapsed) {
                maxPlayerElapsed = playerElapsed;
            }
            inspectedEntities += scanResult.inspectedEntities();
            scannedPlayers++;
            updateSnapshot(player.getUniqueId(), scanResult.progressByType());
        }
        long totalElapsed = System.nanoTime() - startedAt;
        if (totalElapsed >= SCAN_WARN_NANOS) {
            plugin.getLogger().fine(
                "Nearby entity scan for " + scannedPlayers + " player(s) took " +
                    (totalElapsed / 1_000_000.0d) + " ms; max single-player scan " +
                    (maxPlayerElapsed / 1_000_000.0d) + " ms; inspected " + inspectedEntities + " living entit" +
                    (inspectedEntities == 1 ? "y." : "ies.")
            );
        }
    }

    Map<TrackedNearbyEntitiesType, Integer> snapshot(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        EnumMap<TrackedNearbyEntitiesType, Integer> progress = progressByPlayer.get(playerId);
        return progress == null ? Map.of() : new EnumMap<>(progress);
    }

    boolean isRunning() {
        return scanTask != null;
    }

    private void updateSnapshot(UUID playerId, EnumMap<TrackedNearbyEntitiesType, Integer> newSnapshot) {
        EnumMap<TrackedNearbyEntitiesType, Integer> currentSnapshot = progressByPlayer.get(playerId);
        if (currentSnapshot != null && currentSnapshot.equals(newSnapshot)) {
            return;
        }
        progressByPlayer.put(playerId, new EnumMap<>(newSnapshot));
        dirtyPlayerSink.accept(playerId);
    }
}
