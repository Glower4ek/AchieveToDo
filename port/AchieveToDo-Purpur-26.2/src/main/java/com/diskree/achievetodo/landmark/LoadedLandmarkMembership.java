package com.diskree.achievetodo.landmark;

import java.util.*;

/** Loaded-source admission history, independent of player counts. Retires with the source chunk. */
public final class LoadedLandmarkMembership {
    public record Source(String identity, boolean hasHull, int threshold) {}
    private enum Admission { PENDING_HULL, ADMITTED, SKIPPED }
    private final Map<LandmarkChunkKey, Map<String, Admission>> chunks = new HashMap<>();

    public Set<String> publish(LandmarkChunkKey chunk, List<Source> sources) {
        var previous = chunks.getOrDefault(chunk, Map.of());
        Map<String, Admission> next = new HashMap<>();
        Set<String> admitted = new HashSet<>();
        for (Source source : sources) {
            Admission admission = previous.get(source.identity());
            // StructureStartMixin emits a load event for the first non-null hull, then resize events.
            if (admission == null || admission == Admission.PENDING_HULL)
                admission = !source.hasHull() ? Admission.PENDING_HULL
                    : source.threshold() == 0 ? Admission.SKIPPED : Admission.ADMITTED;
            // Config changes and later resizes cannot resurrect a source whose actual load was skipped.
            next.put(source.identity(), admission);
            if (admission == Admission.ADMITTED && source.hasHull()) admitted.add(source.identity());
        }
        chunks.put(chunk, Map.copyOf(next));
        return Set.copyOf(admitted);
    }
    public void unload(LandmarkChunkKey chunk) { chunks.remove(chunk); }
    public void unloadWorld(LandmarkWorld world) { chunks.keySet().removeIf(key -> key.world().equals(world)); }
    public void clear() { chunks.clear(); }
}
