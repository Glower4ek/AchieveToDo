package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.datapack.BacapPacks;
import com.diskree.achievetodo.progression.ProgressionWorldIdentity;
import com.diskree.achievetodo.progression.ProgressionWorldGroup;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import com.diskree.achievetodo.progression.AbilityRequirements;
import com.diskree.achievetodo.progression.DemystificationSink;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ProgressionConfigService;
import com.diskree.achievetodo.progression.ProgressionModeType;
import com.diskree.achievetodo.progression.ResolvedProgressionConfig;
import com.diskree.achievetodo.progression.WorldProgressionConfig;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import com.diskree.achievetodo.tracking.PlayerProgressionUpdate;
import com.diskree.achievetodo.tracking.ProgressionStateSynchronizer;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BukkitProgressionRuntime implements Listener, PlayerProgressionLookup, DemystificationSink, AutoCloseable {

    private static final long RECONCILIATION_INTERVAL_TICKS = 1L;
    private static final int FULL_RECONCILIATION_PASSES = 20;

    private final JavaPlugin plugin;
    private ProgressionWorldGroup worldGroup;
    private long configurationGeneration;
    private boolean registryReady;
    private long validatedGeneration;
    private ResolvedProgressionConfig resolvedProgressionConfig;
    private AbilityRequirements requirements;
    private final BukkitNearbyEntityProgressService nearbyEntityProgressService;
    private final BukkitBacapProgressionSource progressionSource;
    private final BukkitSyntheticAdvancementSynchronizer advancementSynchronizer = new BukkitSyntheticAdvancementSynchronizer();
    private final Map<UUID, PlayerProgressionState> statesByPlayer = new HashMap<>();
    private final Set<UUID> dirtyPlayers = new LinkedHashSet<>();
    private final Map<String, PlayerProgressionListener> listenersById = new LinkedHashMap<>();

    private BukkitTask reconciliationTask;
    private int reconciliationPasses;

    public BukkitProgressionRuntime(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.worldGroup = loadWorldGroup();
        this.resolvedProgressionConfig = resolveProgressionConfig(worldGroup, Boolean.getBoolean("achievetodo.progression.freshConfig"));
        this.configurationGeneration = 1;
        this.requirements = resolvedProgressionConfig.definition().requirements();
        this.nearbyEntityProgressService = new BukkitNearbyEntityProgressService(plugin, dirtyPlayers::add);
        this.progressionSource = new BukkitBacapProgressionSource(plugin, nearbyEntityProgressService);
    }

    public void start() {
        if (reconciliationTask != null) throw new IllegalStateException("Progression already started");
        validateRegistry();
        progressionSource.refreshDetectedObjective();
        nearbyEntityProgressService.start();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        reconciliationTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::runReconciliation,
            RECONCILIATION_INTERVAL_TICKS,
            RECONCILIATION_INTERVAL_TICKS
        );
        synchronizePlayers(plugin.getServer().getOnlinePlayers(), true);
        plugin.getLogger().info("Resolved progression config " + resolvedProgressionConfig.fileName() + " (" + resolvedProgressionConfig.definition().id() + ")");
        logObjectiveState();
    }

    public ResolvedProgressionConfig getResolvedProgressionConfig() {
        return resolvedProgressionConfig;
    }

    public boolean isReady() {
        return registryReady && BacapPacks.discoveryReady() && validatedGeneration == BacapPacks.generation() && progressionSource.isReady();
    }

    public Optional<String> getDetectedObjectiveDescription() {
        return progressionSource.getDetectedObjective()
            .map(detected -> detected.mode().name() + " via " + detected.objectiveName() + " in " + detected.displaySlot());
    }

    public Map<TrackedNearbyEntitiesType, Integer> getNearbySnapshot(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Map<TrackedNearbyEntitiesType, Integer> snapshot = nearbyEntityProgressService.snapshot(playerId);
        if (snapshot.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new EnumMap<>(snapshot));
    }

    public boolean isNearbyScannerRunning() {
        return nearbyEntityProgressService.isRunning();
    }

    public void addListener(String id, PlayerProgressionListener listener) {
        listenersById.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(String id) {
        listenersById.remove(Objects.requireNonNull(id, "id"));
    }

    @Override
    public PlayerProgressionState getProgressionState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return PlayerProgressionState.unavailable(playerId, requirements);
        try {
            // The same read-only S3C capture used by the Bridge, sampled at action time.
            // Never synchronize here: that would write synthetic criteria/rewards during permission checks.
            var capture = capturePresentation(playerId, player.getName(), player.getWorld().getUID(), player.getWorld().getKey().toString());
            var state = PlayerProgressionState.create(playerId, capture.count(), capture.config().definition().requirements());
            var previous = statesByPlayer.get(playerId);
            return previous == null ? state : state.withDemystifiedAbilities(previous.getDemystifiedAbilities());
        } catch (IllegalStateException unavailable) {
            return PlayerProgressionState.unavailable(playerId, requirements);
        }
    }

    @Override
    public void demystify(UUID playerId, AbilityType abilityType) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(abilityType, "abilityType");
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !contextReady(player)) {
            return;
        }
        PlayerProgressionState currentState = statesByPlayer.getOrDefault(
            playerId,
            progressionSource.isReady()
                ? PlayerProgressionState.create(playerId, progressionSource.getObtainedAdvancementCount(playerId), requirements)
                : PlayerProgressionState.unavailable(playerId, requirements)
        );
        if (currentState.getDemystifiedAbilities().contains(abilityType)) {
            return;
        }
        PlayerProgressionState updatedState = currentState.withDemystified(abilityType);
        statesByPlayer.put(playerId, updatedState);
        advancementSynchronizer.demystify(player, abilityType);
        notifyListeners(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!isReady()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                "AchieveToDo BACAP content/advancement data/counter is not ready."
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        dirtyPlayers.add(event.getPlayer().getUniqueId());
        synchronizePlayer(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        statesByPlayer.remove(playerId);
        nearbyEntityProgressService.removePlayer(playerId);
        progressionSource.removePlayer(playerId);
        dirtyPlayers.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        dirtyPlayers.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
        dirtyPlayers.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        statesByPlayer.remove(id);
        progressionSource.removePlayer(id);
        nearbyEntityProgressService.removePlayer(id);
        synchronizePlayer(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        registryReady = false;
        progressionSource.clear();
        // Rebind only after the resource reload and pending #load functions have completed.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try { validateRegistry(); progressionSource.refreshDetectedObjective(); reloadConfiguration(false); }
            catch (RuntimeException failure) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "BACAP_RELOAD_NOT_READY", failure); }
        });
    }

    private void validateRegistry() {
        registryReady = false;
        try { BukkitBacapRegistry.validate(plugin); }
        catch (java.io.IOException failure) { throw new IllegalStateException("BACAP_NOT_READY", failure); }
        validatedGeneration = BacapPacks.generation();
        registryReady = true;
    }

    private boolean contextReady(Player player) {
        World world = player.getWorld();
        return isReady() && worldGroup.contains(world.getUID(), world.getKey().toString());
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
        if (reconciliationTask != null) {
            reconciliationTask.cancel();
            reconciliationTask = null;
        }
        nearbyEntityProgressService.close();
        dirtyPlayers.clear();
        statesByPlayer.clear();
        listenersById.clear();
        progressionSource.clear();
        registryReady = false;
    }

    private void runReconciliation() {
        boolean objectiveChanged = progressionSource.refreshDetectedObjective();
        if (objectiveChanged) logObjectiveState();
        reconciliationPasses++;
        // Paper has no complete scoreboard/revoke event stream. Read the authoritative scores
        // every tick instead of adding event deltas, including offline team holders and resets.
        synchronizePlayers(plugin.getServer().getOnlinePlayers(), false);
        dirtyPlayers.clear();
    }

    private void synchronizePlayers(Collection<? extends Player> players, boolean initialSync) {
        for (Player player : players) {
            synchronizePlayer(player, initialSync);
        }
    }

    private void synchronizePlayer(Player player, boolean initialSync) {
        if (!contextReady(player)) {
            UUID id = player.getUniqueId();
            // Temporary content unavailability is not a permission transition or a criteria-repair trigger.
            if (worldGroup.contains(player.getWorld().getUID(), player.getWorld().getKey().toString())) return;
            boolean changed = statesByPlayer.remove(id) != null;
            progressionSource.removePlayer(id);
            if (changed) notifyListeners(id);
            return;
        }
        if (initialSync) {
            nearbyEntityProgressService.refreshPlayer(player);
        }
        if (initialSync || reconciliationPasses % FULL_RECONCILIATION_PASSES == 0) progressionSource.refreshPlayer(player);
        else progressionSource.refreshCount(player);
        UUID playerId = player.getUniqueId();
        PlayerProgressionState previousState = statesByPlayer.get(playerId);
        if (previousState == null || initialSync) {
            PlayerProgressionState seededState = seedState(player);
            statesByPlayer.put(playerId, seededState);
            advancementSynchronizer.fullSync(player, seededState);
            notifyListeners(playerId);
            return;
        }
        int obtainedCount = progressionSource.getObtainedAdvancementCount(playerId);
        if (previousState.isReady() && previousState.getObtainedAdvancementCount() == obtainedCount) {
            if (dirtyPlayers.contains(playerId) || reconciliationPasses % FULL_RECONCILIATION_PASSES == 0) notifyListeners(playerId);
            return;
        }
        PlayerProgressionUpdate update = ProgressionStateSynchronizer.synchronize(previousState, obtainedCount, progressionSource.isReady());
        statesByPlayer.put(playerId, update.currentState());
        advancementSynchronizer.applyUpdate(player, update);
        notifyListeners(playerId);
    }

    private PlayerProgressionState seedState(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerProgressionState state = progressionSource.isReady()
            ? PlayerProgressionState.create(playerId, progressionSource.getObtainedAdvancementCount(playerId), requirements)
            : PlayerProgressionState.unavailable(playerId, requirements);
        return state.withDemystifiedAbilities(advancementSynchronizer.readDemystifiedAbilities(player));
    }

    private void logObjectiveState() {
        Optional<String> objectiveMessage = progressionSource.getDetectedObjective()
            .map(detected -> detected.mode().name() + " via " + detected.objectiveName() + " in " + detected.displaySlot());
        if (objectiveMessage.isPresent()) {
            plugin.getLogger().info("Detected active BACAP objective: " + objectiveMessage.get());
        } else {
            plugin.getLogger().severe(
                "BACAP objective not detected. AchieveToDo progression is unavailable until an objective appears in SIDEBAR, LIST, or BELOW_NAME."
            );
        }
    }

    /** Immutable read-only capture, including pre-play profiles; never awards/recounts or seeds a player cache. */
    public record Presentation(UUID player,ProgressionWorldIdentity world,ProgressionWorldGroup group,long generation,
                               ResolvedProgressionConfig config,String objective,int count) {}
    public Presentation capturePresentation(UUID player,String name,UUID worldId,String key) {
        if(!plugin.getServer().isPrimaryThread())throw new IllegalStateException("Progression capture requires server thread");
        progressionSource.refreshDetectedObjective();
        World loaded=plugin.getServer().getWorld(worldId);
        if(!isReady() || name==null || name.isBlank() || loaded==null || !loaded.getKey().toString().equals(key) || !worldGroup.contains(worldId,key))
            throw new IllegalStateException("ATD_PROGRESSION_UNAVAILABLE_OR_WORLD_UNBOUND");
        int count=progressionSource.readCount(name).orElseThrow(()->new IllegalStateException("ATD_PROGRESSION_SOURCE_UNAVAILABLE"));
        return new Presentation(player,new ProgressionWorldIdentity(worldId,key),worldGroup,configurationGeneration,resolvedProgressionConfig,
            progressionSource.getDetectedObjective().orElseThrow().objectiveName(),count);
    }

    public ProgressionWorldGroup getWorldGroup() { return worldGroup; }
    public long getConfigurationGeneration() { return configurationGeneration; }

    /** Server-thread transaction; caller must explicitly identify a new selection before allowing generation. */
    public boolean reloadConfiguration(boolean freshSelection) {
        if (!plugin.getServer().isPrimaryThread()) throw new IllegalStateException("Configuration reload requires server thread");
        try {
            if (!isReady()) throw new IllegalStateException("Progression content/source unavailable during configuration transaction");
            var candidateGroup = loadWorldGroup();
            if (!candidateGroup.groupId().equals(worldGroup.groupId()) || !candidateGroup.configId().equals(worldGroup.configId())) throw new IllegalStateException("CONFIG_OWNER_MISMATCH");
            var candidate = resolveProgressionConfig(candidateGroup, freshSelection);
            var candidateRequirements = candidate.definition().requirements();
            // Everything above may fail; publish only a completely validated generation.
            worldGroup = candidateGroup;
            resolvedProgressionConfig = candidate;
            requirements = candidateRequirements;
            configurationGeneration++;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                var previous = statesByPlayer.get(id);
                if (previous != null && contextReady(player)) {
                    progressionSource.refreshCount(player);
                    var current = PlayerProgressionState.create(id, progressionSource.getObtainedAdvancementCount(id), requirements)
                        .withDemystifiedAbilities(previous.getDemystifiedAbilities());
                    statesByPlayer.put(id,current);
                    // Only actual permission transitions; equal-count/config readback does not repair criteria.
                    advancementSynchronizer.applyUpdate(player, ProgressionStateSynchronizer.diff(previous,current));
                    notifyListeners(id);
                } else synchronizePlayer(player,false);
            }
            plugin.getLogger().info("PROGRESSION_CONFIG_READY generation=" + configurationGeneration + " file=" + candidate.fileName());
            return true;
        } catch (RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,"PROGRESSION_CONFIG_REJECTED: retaining last known good generation=" + configurationGeneration,failure);
            return false;
        }
    }

    private Path progressionDirectory() { return plugin.getDataFolder().toPath().resolve("progression"); }

    private ProgressionWorldGroup loadWorldGroup() {
        var group = ProgressionWorldGroup.load(progressionDirectory().resolve("world-group.properties"));
        for (var entry : group.worlds().entrySet()) {
            var identity = entry.getValue();
            World world = plugin.getServer().getWorld(identity.uuid());
            World.Environment expected = switch(entry.getKey()) {
                case "overworld" -> World.Environment.NORMAL;
                case "nether" -> World.Environment.NETHER;
                case "end" -> World.Environment.THE_END;
                default -> throw new AssertionError();
            };
            if (world == null || !identity.matches(world.getUID(),world.getKey().toString()) || world.getEnvironment() != expected) throw new IllegalStateException("WORLD_UNBOUND: UUID/key/role must match a loaded dimension");
        }
        var main = group.worlds().get("overworld");
        // Preserve the explicit S3A main-world assertion; it cannot redirect ownership.
        if (!main.matches(UUID.fromString(System.getProperty("achievetodo.progression.world", "")),System.getProperty("achievetodo.progression.worldKey", ""))) throw new IllegalStateException("WORLD_IDENTITY_MISMATCH");
        return group;
    }

    private ResolvedProgressionConfig resolveProgressionConfig(ProgressionWorldGroup group, boolean freshSelection) {
        World main = plugin.getServer().getWorld(group.worlds().get("overworld").uuid());
        var store = new BukkitWorldProgressionConfigStore(progressionDirectory().resolve("world-selection.properties"),main,group);
        var saved = store.load();
        if (saved.isEmpty() && !freshSelection) throw new IllegalStateException("Missing saved selection; explicit freshConfig required for initialization");
        var selection = saved.orElseGet(() -> new WorldProgressionConfig(System.getProperty("achievetodo.progression.initialConfig","normal")));
        var candidate = new ProgressionConfigService().resolve(selection,main.getSeed(),new BukkitProgressionConfigRepository(progressionDirectory().resolve("configs")),freshSelection);
        if (saved.isEmpty()) store.save(selection); // resolved table is durable before pointer publication
        return candidate;
    }

    private void notifyListeners(UUID playerId) {
        for (PlayerProgressionListener listener : listenersById.values()) {
            listener.onPlayerProgressionChanged(playerId);
        }
    }
}
