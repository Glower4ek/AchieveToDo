package com.diskree.achievetodo.progression;

import java.util.*;
import java.nio.file.*;
import java.io.*;

/** One operator-bound global-scoreboard group. No inference from names, keys, or load order. */
public record ProgressionWorldGroup(String groupId, UUID configId, Map<String, ProgressionWorldIdentity> worlds) {
    public ProgressionWorldGroup {
        if (groupId == null || !groupId.matches("[A-Za-z0-9_-]+") || configId == null || configId.equals(new UUID(0,0))) throw new IllegalArgumentException("Explicit group/config identity required");
        worlds = Map.copyOf(worlds);
        if (!worlds.containsKey("overworld") || !Set.of("overworld","nether","end").containsAll(worlds.keySet())) throw new IllegalArgumentException("Invalid dimension roles");
        Set<UUID> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (var world : worlds.values()) if (!ids.add(world.uuid()) || !keys.add(world.key())) throw new IllegalArgumentException("Duplicate world binding");
    }
    public boolean contains(UUID uuid, String key) { return worlds.values().stream().anyMatch(w -> w.matches(uuid,key)); }
    public static ProgressionWorldGroup load(Path file) {
        Properties p = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate group property: " + key);
                return super.put(key,value);
            }
        };
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) { p.load(input); }
        catch (IOException e) { throw new IllegalStateException("WORLD_GROUP_UNBOUND: explicit world-group.properties required",e); }
        if (!"1".equals(p.getProperty("formatVersion"))) throw new IllegalArgumentException("Unsupported world group format");
        Set<String> allowed = new HashSet<>(Set.of("formatVersion","groupId","configId"));
        Map<String,ProgressionWorldIdentity> bindings = new LinkedHashMap<>();
        for (String role : List.of("overworld","nether","end")) {
            String uid = role + ".uuid", key = role + ".key";
            allowed.add(uid); allowed.add(key);
            if (p.containsKey(uid) || p.containsKey(key)) bindings.put(role,new ProgressionWorldIdentity(UUID.fromString(p.getProperty(uid)),p.getProperty(key)));
        }
        if (!allowed.containsAll(p.stringPropertyNames())) throw new IllegalArgumentException("Unknown world group fields or independent groups");
        return new ProgressionWorldGroup(p.getProperty("groupId"),UUID.fromString(p.getProperty("configId")),bindings);
    }
}
