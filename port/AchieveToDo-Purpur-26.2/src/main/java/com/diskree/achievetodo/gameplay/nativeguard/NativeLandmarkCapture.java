package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Purpur2620 generation receipts. No Bukkit calls, world reads, or progression on worker threads. */
public final class NativeLandmarkCapture {
    public static final String SOURCE = "achievetodo:s4g_source_v1";
    public static final String HULL = "achievetodo:s4g_hull_v1";
    public static final String FEATURES = "achievetodo:s4g_features_v1";
    private static final String FIELD = "atd$s4gCapture";
    private static final Set<String> DIMENSIONS = Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final ClassValue<Map<String, Method>> METHODS = new ClassValue<>() {
        @Override protected Map<String, Method> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    private static final ClassValue<Field> FRAMES = new ClassValue<>() {
        @Override protected Field computeValue(Class<?> type) {
            try { return type.getField(FIELD); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
    };
    private static final ClassValue<Map<Object, String>> FEATURE_TYPES = new ClassValue<>() {
        @Override protected Map<Object, String> computeValue(Class<?> type) {
            try { return Map.of(type.getField("MONSTER_ROOM").get(null), "minecraft:monster_room",
                type.getField("DESERT_WELL").get(null), "minecraft:desert_well"); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
    };

    public static Object call(Object target, String name, Object... args) {
        String key = name + Arrays.toString(Arrays.stream(args).map(a -> a == null ? null : a.getClass()).toArray());
        Method method = METHODS.get(target.getClass()).computeIfAbsent(key, ignored -> {
            for (Method candidate : target.getClass().getMethods()) {
                if (!candidate.getName().equals(name) || candidate.getParameterCount() != args.length) continue;
                var types = candidate.getParameterTypes(); boolean matches = true;
                for (int i = 0; i < types.length; i++) {
                    Class<?> type = types[i] == int.class ? Integer.class : types[i];
                    if (args[i] != null && !type.isInstance(args[i])) { matches = false; break; }
                }
                if (matches) {
                    // Use a public declaring interface for private native Holder implementations.
                    if (!Modifier.isPublic(candidate.getDeclaringClass().getModifiers())) candidate.setAccessible(true);
                    return candidate;
                }
            }
            throw new IllegalStateException("Missing native method " + target.getClass() + "." + name);
        });
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            if (e.getCause() instanceof Error error) throw error;
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static Object pdc(Object owner) {
        try { return owner.getClass().getField("persistentDataContainer").get(owner); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static Object tag(Object owner, String name) { return call(pdc(owner), "getTag", name); }
    private static Object newTag(Object owner, String name, Class<?> argument, Object value) {
        try {
            return Class.forName("net.minecraft.nbt." + name, true, owner.getClass().getClassLoader())
                .getConstructor(argument).newInstance(value);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void putInts(Object owner, String key, int[] value) {
        call(pdc(owner), "put", key, newTag(owner, "IntArrayTag", int[].class, value));
    }
    private static String source(Object owner) {
        Object value = tag(owner, SOURCE);
        return value == null ? "" : (String) call(value, "value");
    }
    private static boolean region(Object world) {
        return world.getClass().getName().equals("net.minecraft.server.level.WorldGenRegion")
            || world.getClass().getName().equals("net.minecraft.server.level.ServerLevel");
    }
    private static boolean dimension(Object world) {
        return DIMENSIONS.contains(call(call(call(world, "getLevel"), "dimension"), "identifier").toString());
    }
    private static Frame frame(Object world) {
        try { return (Frame) FRAMES.get(world.getClass()).get(world); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void frame(Object world, Frame frame) {
        try { FRAMES.get(world.getClass()).set(world, frame); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** Same successful Structure.generate return as canonical StructureMixin; no retrofit of old starts. */
    public static void generated(Object start, Object holder, Object dimension) {
        if (!NativeLandmarkAgent.captureAvailable()) return;
        if (!DIMENSIONS.contains(call(dimension, "identifier").toString())) return;
        var key = (Optional<?>) call(holder, "unwrapKey");
        if (key.isEmpty()) return;
        String id = call(key.get(), "identifier").toString();
        if (!NativeLandmarkMappings.STRUCTURES.containsKey(id)) return;
        try {
            Class<?> stringTag = Class.forName("net.minecraft.nbt.StringTag", true, start.getClass().getClassLoader());
            call(pdc(start), "put", SOURCE, stringTag.getMethod("valueOf", String.class).invoke(null, id));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    public static void beginStructure(Object start, Object world) {
        if (!NativeLandmarkAgent.captureAvailable()) return;
        if (!region(world) || !dimension(world) || source(start).isEmpty()) return;
        int[] old;
        synchronized (pdc(start)) { old = readHull(start); }
        frame(world, new Frame(start, false, old));
    }

    /** Only the last setBlock return: canonical @TAIL excludes the early ensureCanWrite failure. */
    public static void block(Object world, Object position) {
        Frame f = frame(world);
        if (f == null) return;
        int x = (int) call(position, "getX"), y = (int) call(position, "getY"), z = (int) call(position, "getZ");
        f.hull = union(f.hull, new int[]{x, y, z, x, y, z});
    }

    /** Canonical ServerWorldMixin records successful writes after super.setBlock, including command placement. */
    public static boolean levelBlock(boolean success, Object world, Object position) {
        if (success && world.getClass().getName().equals("net.minecraft.server.level.ServerLevel")) block(world,position);
        return success;
    }

    public static void endStructure(Object start, Object world) {
        if (!region(world)) return;
        Frame f = frame(world); frame(world, null);
        if (f == null || f.feature || f.owner != start || f.hull == null) return;
        synchronized (pdc(start)) { putInts(start, HULL, union(readHull(start), f.hull)); }
        changed(world, call(start, "getChunkPos"));
    }

    public static void abandon(Object world) { if (region(world)) frame(world, null); }

    /** Wraps the exact Feature.place(FeaturePlaceContext) call with exception-safe retirement. */
    public static boolean feature(Object feature, Object context) {
        if (!NativeLandmarkAgent.captureAvailable()) return (boolean) call(feature, "place", context);
        String id = FEATURE_TYPES.get(feature.getClass()).get(feature);
        if (id == null) return (boolean) call(feature, "place", context);
        Object world = call(context, "level");
        if (!region(world) || !dimension(world)) return (boolean) call(feature, "place", context);
        Frame capture = new Frame(id, true, null);
        frame(world, capture);
        boolean completed = false;
        try { boolean result = (boolean) call(feature, "place", context); completed = true; return result; }
        finally {
            // Canonical has one tracker, not a stack. A nested tracked feature consumes/resets the outer tracker.
            boolean stillOwned = frame(world) == capture;
            frame(world, null);
            if (completed && stillOwned && capture.hull != null) {
                // Origin chunk only, as canonical FeatureMixin. No ServerLevel chunk requests.
                Object chunk = call(world, "getChunk", call(context, "origin"));
                synchronized (pdc(chunk)) {
                    Object existing = tag(chunk, FEATURES);
                    Object list = existing == null ? newList(chunk) : existing;
                    Object row;
                    try { row = Class.forName("net.minecraft.nbt.CompoundTag", true, chunk.getClass().getClassLoader()).getConstructor().newInstance(); }
                    catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                    call(row, "putString", "id", id);
                    call(row, "putIntArray", "hull", capture.hull);
                    call(list, "add", row);
                    call(pdc(chunk), "put", FEATURES, list);
                }
                changed(world, call(chunk, "getPos"));
            }
        }
    }

    private static Object newList(Object owner) {
        try { return Class.forName("net.minecraft.nbt.ListTag", true, owner.getClass().getClassLoader()).getConstructor().newInstance(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void changed(Object world, Object pos) {
        NativeLandmarkAgent.changed(call(world, "getLevel"), (int) call(pos, "x"), (int) call(pos, "z"));
    }
    public static int[] readHull(Object owner) {
        Object value = tag(owner, HULL);
        if (value == null || !value.getClass().getName().equals("net.minecraft.nbt.IntArrayTag")) return null;
        int[] hull = (int[]) call(value, "getAsIntArray");
        return valid(hull) ? hull.clone() : null;
    }
    /** Called only by the main-thread reader on an already loaded native chunk. */
    public static List<Object[]> records(Object chunk, Object level) {
        List<Object[]> rows = new ArrayList<>();
        Object registryKey;
        try { registryKey = Class.forName("net.minecraft.core.registries.Registries", false, level.getClass().getClassLoader()).getField("STRUCTURE").get(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        Object registry = call(call(level, "registryAccess"), "lookupOrThrow", registryKey);
        Map<?, ?> starts = (Map<?, ?>) call(chunk, "getAllStarts");
        for (var entry : starts.entrySet()) {
            Object start = entry.getValue(); String id = source(start);
            if (id.isEmpty()) continue; // Missing original generation provenance is never synthesized.
            if (!NativeLandmarkMappings.STRUCTURES.containsKey(id)
                || !id.equals(String.valueOf(call(registry, "getKey", entry.getKey()))))
                throw new IllegalStateException("Landmark source/registry mismatch");
            int[] hull = readHull(start);
            if (hull == null && tag(start, HULL) != null) throw new IllegalStateException("Malformed landmark hull");
            // Observe tagged starts even before their first write: later resizing must not manufacture a load event.
            rows.add(new Object[]{id, hull});
        }
        Object features = tag(chunk, FEATURES);
        if (features != null) {
            int size = (int) call(features, "size");
            for (int i = 0; i < size; i++) {
                Object row = call(features, "getCompoundOrEmpty", i);
                String id = (String) call(row, "getStringOr", "id", "");
                int[] hull = (int[]) ((Optional<?>) call(row, "getIntArray", "hull")).orElse(null);
                if (!(id.equals("minecraft:monster_room") || id.equals("minecraft:desert_well")) || !valid(hull))
                    throw new IllegalStateException("Malformed feature provenance");
                rows.add(new Object[]{id, hull.clone()});
            }
        }
        return List.copyOf(rows);
    }
    public static boolean valid(int[] h) {
        return h != null && h.length == 6 && h[0] <= h[3] && h[1] <= h[4] && h[2] <= h[5]
            && (long) h[3] - h[0] <= 4096 && (long) h[5] - h[2] <= 4096 && (long) h[4] - h[1] <= 4096;
    }
    public static int[] union(int[] a, int[] b) {
        if (a == null) return b == null ? null : b.clone();
        if (b == null) return a.clone();
        return new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
            Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])};
    }
    private static final class Frame {
        final Object owner; final boolean feature; int[] hull;
        Frame(Object owner, boolean feature, int[] hull) { this.owner = owner; this.feature = feature; this.hull = hull; }
    }
    private NativeLandmarkCapture() {}
}
