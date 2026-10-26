package com.diskree.achievetodo.landmark.bukkit;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkCapture;
import com.diskree.achievetodo.landmark.*;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.world.*;
import org.bukkit.event.player.PlayerQuitEvent;
import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.lang.reflect.Method;
import java.util.*;

/** Controlled opt-in native generation hulls and canonical action enforcement; no disk sidecar. */
public final class BukkitExactLandmarkRuntime implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final PlayerProgressionLookup progression;
    private final LandmarkIndex index = new LandmarkIndex();
    private final CanonicalLandmarkMembership membership = new CanonicalLandmarkMembership();
    private final LoadedLandmarkMembership loadedMembership = new LoadedLandmarkMembership();
    private record Presentation(Set<LandmarkType> types,com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot snapshot) {}
    private final Map<UUID,Presentation> presentations=new HashMap<>();
    private static final String MEMBERSHIP_LISTENER = "s4gr1-landmark-membership";
    private final Set<LandmarkChunkKey> invalid = new HashSet<>();
    private final Set<LandmarkChunkKey> pending = new HashSet<>();
    private final Object token = new Object();
    private Method retire, drain;
    private BukkitTask task;

    public BukkitExactLandmarkRuntime(JavaPlugin plugin, PlayerProgressionLookup progression) {
        this.plugin = plugin; this.progression = progression;
    }
    public void start() {
        try {
            if (!(progression instanceof BukkitProgressionRuntime authority))
                throw new IllegalStateException("Exact membership requires S3C authority lifecycle");
            authority.addListener(MEMBERSHIP_LISTENER, id -> membership.observe(progression.getProgressionState(id)));
            for (Player player : Bukkit.getOnlinePlayers()) membership.observe(progression.getProgressionState(player.getUniqueId()));
            Class<?> nativePlayer = Class.forName("net.minecraft.world.entity.player.Player");
            Class<?> agent = Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkAgent", false, null);
            retire = agent.getMethod("retire", Object.class); drain = agent.getMethod("drain");
            agent.getMethod("install", Object.class, java.util.function.BiPredicate.class, ClassLoader.class)
                .invoke(null, token, (java.util.function.BiPredicate<Object, Object>) this::blockDenied, nativePlayer.getClassLoader());
            for (World world : Bukkit.getWorlds()) for (Chunk chunk : world.getLoadedChunks()) refresh(chunk);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::drain, 1L, 1L);
            plugin.getLogger().warning("S4G exact provenance + conditional actions controlled opt-in active; PRODUCT_GAMEPLAY_READY=false");
        } catch (ReflectiveOperationException failure) { close(); throw new IllegalStateException("S4G native prototype unavailable", failure); }
    }
    private static Object nativeLevel(World world) { return NativeLandmarkCapture.call(world, "getHandle"); }
    private static Object nativeChunk(Chunk chunk) {
        // getChunkNow is a pinned cache lookup; CraftChunk.getHandle(FULL) could load a chunk.
        Object loaded = NativeLandmarkCapture.call(NativeLandmarkCapture.call(nativeLevel(chunk.getWorld()), "getChunkSource"),
            "getChunkNow", chunk.getX(), chunk.getZ());
        if (loaded == null) throw new IllegalStateException("Native chunk not yet published");
        return loaded;
    }
    private void drain() {
        requireMainThread();
        for (var key : Set.copyOf(pending)) {
            pending.remove(key);
            World world = Bukkit.getWorld(UUID.fromString(key.world().worldId()));
            if (world != null && world.isChunkLoaded(key.chunk().x(), key.chunk().z())) refresh(world.getChunkAt(key.chunk().x(), key.chunk().z(), false));
        }
        try {
            Object[] batch = (Object[]) drain.invoke(null);
            if ((boolean) batch[0]) {
                for (World world : Bukkit.getWorlds()) for (Chunk chunk : world.getLoadedChunks()) refresh(chunk);
                return;
            }
            for (Object[] update : castRows(batch[1])) {
                for (World world : Bukkit.getWorlds()) {
                    if (nativeLevel(world) != update[0]) continue;
                    int x = (int) update[1], z = (int) update[2];
                    if (world.isChunkLoaded(x, z)) {
                        Chunk chunk = world.getChunkAt(x, z, false);
                        NativeLandmarkCapture.call(nativeChunk(chunk), "markUnsaved");
                        refresh(chunk);
                    }
                    break;
                }
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("S4G generation updates unavailable", e); }
    }
    @SuppressWarnings("unchecked") private static List<Object[]> castRows(Object rows) { return (List<Object[]>) rows; }
    private static void requireMainThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("S4G async world access"); }
    private void refresh(Chunk chunk) {
        requireMainThread();
        presentations.clear();
        var world = BukkitLandmarkAdapters.toLandmarkWorld(chunk.getWorld());
        if (world.isEmpty()) return;
        var coordinate = new ChunkCoordinate(chunk.getX(), chunk.getZ());
        var key = new LandmarkChunkKey(world.get(), coordinate);
        if (!((BukkitProgressionRuntime) progression).isReady()) { pending.add(key); return; }
        var previous = index.all().stream().filter(r -> r.world().equals(key.world()) && r.source().sourceChunk().equals(coordinate)).toList();
        index.removeAll(previous);
        try {
            Object handle = nativeChunk(chunk);
            var records = NativeLandmarkCapture.records(handle, nativeLevel(chunk.getWorld()));
            var publications = new ArrayList<LoadedLandmarkMembership.Source>();
            var identities = new IdentityHashMap<Object[], String>();
            for (Object[] row : records) {
                String id = (String) row[0]; int[] h = (int[]) row[1];
                var type = LandmarkType.findByStructureKey(id).or(() -> LandmarkType.findByFeatureKey(id)).orElseThrow();
                String identity = type.isStructure() ? id : id + Arrays.toString(h);
                identities.put(row, identity);
                int threshold = ((BukkitProgressionRuntime) progression).getResolvedProgressionConfig().definition().requirements()
                    .getRequiredCount(AbilityResolvers.findLandmarkAbility(type).orElseThrow());
                publications.add(new LoadedLandmarkMembership.Source(identity, h != null, threshold));
            }
            var admitted = loadedMembership.publish(key, publications);
            for (Object[] row : records) {
                String id = (String) row[0]; int[] h = (int[]) row[1];
                if (!admitted.contains(identities.get(row))) continue;
                var type = LandmarkType.findByStructureKey(id).or(() -> LandmarkType.findByFeatureKey(id)).orElseThrow();
                index.add(new LandmarkRecord(type, world.get(), new BlockBox(h[0], h[1], h[2], h[3], h[4], h[5]),
                    new LandmarkSource(type.isFeature() ? LandmarkSourceKind.FEATURE : LandmarkSourceKind.STRUCTURE, id, coordinate)));
            }
            invalid.remove(key);
        } catch (RuntimeException failure) {
            invalid.add(key);
            plugin.getLogger().warning("S4G unavailable native provenance at " + key + ": " + failure);
        }
    }
    private boolean blockDenied(Object actor, Object target) {
        requireMainThread(); drain();
        Object[] tuple = (Object[]) target;
        Object position = tuple[1];
        Player player = (Player) NativeLandmarkCapture.call(actor, "getBukkitEntity");
        boolean bypass = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
        if (bypass) return false;
        World targetWorld = Bukkit.getWorlds().stream().filter(w -> nativeLevel(w) == tuple[0]).findFirst().orElse(null);
        if (targetWorld == null) return true;
        var world = BukkitLandmarkAdapters.toLandmarkWorld(targetWorld);
        if (world.isEmpty()) return true;
        if (invalid.stream().anyMatch(key -> key.world().equals(world.get()))) return true;
        BlockBox targetBox;
        if (position instanceof double[] box) targetBox = TargetBlockBox.fromAabb(box);
        else {
            int x = (int) NativeLandmarkCapture.call(position, "getX");
            int y = (int) NativeLandmarkCapture.call(position, "getY");
            int z = (int) NativeLandmarkCapture.call(position, "getZ");
            targetBox = new BlockBox(x, y, z, x, y, z);
        }
        var state = progression.getProgressionState(player.getUniqueId());
        membership.observe(state);
        var result = membership.evaluate(state, player.getUniqueId(), false, index.findIntersecting(world.get(), targetBox));
        AbilityType additional = tuple.length > 2 && tuple[2] != null ? AbilityType.valueOf((String) tuple[2]) : null;
        return ExactLandmarkGate.denied(result, state, player.getUniqueId(), additional);
    }
    public Set<LandmarkRecord> records() { requireMainThread(); drain(); return index.all(); }
    public com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot presentation(UUID actor) {
        requireMainThread();drain();membership.observe(progression.getProgressionState(actor));
        var types=membership.types(actor);var previous=presentations.get(actor);
        if(previous!=null && previous.types().equals(types))return previous.snapshot();
        var boxes=new HashSet<com.diskree.achievetodo.bridge.BridgeLandmarks.Box>();
        for(var record:index.all()) {
            if(!membership.contains(actor,record.landmarkType()))continue;
            var box=record.blockBox();
            boxes.add(new com.diskree.achievetodo.bridge.BridgeLandmarks.Box(
                AbilityResolvers.findLandmarkAbility(record.landmarkType()).orElseThrow().getName(),
                UUID.fromString(record.world().worldId()),box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()));
        }
        var sorted=new ArrayList<>(boxes);
        sorted.sort(Comparator.comparing(com.diskree.achievetodo.bridge.BridgeLandmarks.Box::ability)
            .thenComparing(b->b.world().toString()).thenComparingInt(b->b.minX()).thenComparingInt(b->b.minY())
            .thenComparingInt(b->b.minZ()).thenComparingInt(b->b.maxX()).thenComparingInt(b->b.maxY()).thenComparingInt(b->b.maxZ()));
        var snapshot=new com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot(true,sorted);
        presentations.put(actor,new Presentation(types,snapshot));return snapshot;
    }
    @EventHandler public void quit(PlayerQuitEvent event) { membership.disconnect(event.getPlayer().getUniqueId());presentations.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void load(ChunkLoadEvent event) {
        BukkitLandmarkAdapters.toLandmarkWorld(event.getWorld()).ifPresent(world -> pending.add(new LandmarkChunkKey(world,
            new ChunkCoordinate(event.getChunk().getX(), event.getChunk().getZ()))));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void unload(ChunkUnloadEvent event) {
        presentations.clear();
        var world = BukkitLandmarkAdapters.toLandmarkWorld(event.getWorld());
        if (world.isEmpty()) return;
        var chunk = new ChunkCoordinate(event.getChunk().getX(), event.getChunk().getZ());
        index.removeIf(r -> r.world().equals(world.get()) && r.source().sourceChunk().equals(chunk));
        loadedMembership.unload(new LandmarkChunkKey(world.get(), chunk));
        invalid.remove(new LandmarkChunkKey(world.get(), chunk));
        pending.remove(new LandmarkChunkKey(world.get(), chunk));
    }
    @EventHandler public void unloadWorld(WorldUnloadEvent event) {
        presentations.clear();
        var world = BukkitLandmarkAdapters.toLandmarkWorld(event.getWorld());
        world.ifPresent(w -> { index.removeIf(r -> r.world().equals(w)); loadedMembership.unloadWorld(w); invalid.removeIf(k -> k.world().equals(w)); pending.removeIf(k -> k.world().equals(w)); });
    }
    @Override public void close() {
        if (task != null) task.cancel();
        HandlerList.unregisterAll(this);
        index.removeIf(r -> true); invalid.clear(); pending.clear();
        membership.clear();
        loadedMembership.clear();
        if (progression instanceof BukkitProgressionRuntime authority) authority.removeListener(MEMBERSHIP_LISTENER);
        if (retire != null) try { retire.invoke(null, token); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("S4G retirement failed", e); }
    }
}
