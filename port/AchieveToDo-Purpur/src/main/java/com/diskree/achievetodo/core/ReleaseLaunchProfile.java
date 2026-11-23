package com.diskree.achievetodo.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Release launch, plugin half. Only used when premain applied the release defaults
 * ({@link ReleaseLaunchDefaults#active}); an explicit launch never reaches any of this.
 *
 * <p>It fills in the per-installation values the certified command passed by hand, and only those the operator
 * did not give. Explicit values always win and are never rewritten:
 * <ul>
 * <li>{@code achievetodo.bacap.directory}: {@code <level-name>/datapacks} from server.properties; checked again
 *     against the loaded overworld folder once worlds exist.</li>
 * <li>{@code achievetodo.bridge.server}: a random UUID created once and kept in
 *     {@code plugins/AchieveToDo/server-identity.properties}.</li>
 * <li>{@code progression/world-group.properties}: written once on a fresh install (no world group and no saved
 *     selection), binding the three vanilla dimensions by their fixed keys. From then on the file is the
 *     binding and every later start is verified against it exactly as before.</li>
 * <li>{@code achievetodo.progression.world/worldKey}: the overworld of that world group.</li>
 * <li>{@code achievetodo.progression.freshConfig=true}: only on the start that wrote the world group.</li>
 * </ul>
 * Pure (no Bukkit types) so it can be tested without a server.
 */
public final class ReleaseLaunchProfile {
    public static final String BACAP_DIRECTORY = StartupModePolicy.BACAP_DIRECTORY;
    public static final String PROGRESSION_WORLD = StartupModePolicy.PROGRESSION_WORLD;
    public static final String PROGRESSION_WORLD_KEY = StartupModePolicy.PROGRESSION_WORLD_KEY;
    public static final String BRIDGE_SERVER = StartupModePolicy.BRIDGE_SERVER;
    public static final String FRESH_CONFIG = "achievetodo.progression.freshConfig";
    /** Comma-separated names of the values this profile filled in; for the startup log and the cross-check. */
    public static final String DERIVED = "achievetodo.launch.derived";
    public static final String SERVER_IDENTITY_FILE = "server-identity.properties";
    public static final String WORLD_GROUP_FILE = "world-group.properties";
    public static final String SELECTION_FILE = "world-selection.properties";
    public static final String DEFAULT_GROUP_ID = "main";
    private static final Map<String, String> ROLE_KEYS = new LinkedHashMap<>();
    static {
        ROLE_KEYS.put("overworld", "minecraft:overworld");
        ROLE_KEYS.put("nether", "minecraft:the_nether");
        ROLE_KEYS.put("end", "minecraft:the_end");
    }

    /** A loaded world as the profile needs to see it. {@code role} is overworld, nether, end or other. */
    public record LoadedWorld(String role, UUID uuid, String key, Path folder) {}

    private final Function<String, String> get;
    private final BiConsumer<String, String> set;
    private final List<String> derived = new ArrayList<>();

    public ReleaseLaunchProfile(Function<String, String> get, BiConsumer<String, String> set) {
        this.get = Objects.requireNonNull(get);
        this.set = Objects.requireNonNull(set);
        String before = get.apply(DERIVED);
        if (before != null && !before.isBlank()) derived.addAll(List.of(before.split(",")));
    }

    /** Bootstrap step, before datapack discovery. */
    public void deriveBacapDirectory(Path serverRoot) {
        if (get.apply(BACAP_DIRECTORY) != null) return;
        String level = "world";
        Path properties = serverRoot.resolve("server.properties");
        if (Files.isRegularFile(properties)) {
            var p = new Properties();
            try (InputStream in = Files.newInputStream(properties)) { p.load(in); }
            catch (IOException e) { throw refusal("ATD_LAUNCH_SERVER_PROPERTIES", "server.properties could not be read: " + e.getMessage()); }
            String configured = p.getProperty("level-name");
            if (configured != null && !configured.isBlank()) level = configured.trim();
        }
        put(BACAP_DIRECTORY, serverRoot.resolve(level).resolve("datapacks").toAbsolutePath().normalize().toString());
    }

    /** Plugin step, after the worlds are loaded and the agent/plugin identity is verified. */
    public void deriveInstallation(Path dataFolder, List<LoadedWorld> worlds) {
        Path progression = dataFolder.resolve("progression");
        checkBacapAgainstWorld(worlds);
        deriveServerId(dataFolder.resolve(SERVER_IDENTITY_FILE));
        boolean created = bindWorldGroupOnFreshInstall(progression, worlds);
        deriveProgressionWorld(progression.resolve(WORLD_GROUP_FILE));
        if (created && get.apply(FRESH_CONFIG) == null) put(FRESH_CONFIG, "true");
    }

    public List<String> derived() { return List.copyOf(derived); }

    void checkBacapAgainstWorld(List<LoadedWorld> worlds) {
        if (!derived.contains(BACAP_DIRECTORY)) return; // an explicit directory is the operator's assertion
        LoadedWorld overworld = single(worlds, "overworld");
        Path expected = levelRoot(overworld.folder()).resolve("datapacks").toAbsolutePath().normalize();
        if (!expected.equals(Path.of(get.apply(BACAP_DIRECTORY)).toAbsolutePath().normalize()))
            throw refusal("ATD_LAUNCH_BACAP_DIRECTORY", "The BACAP directory taken from server.properties ("
                + get.apply(BACAP_DIRECTORY) + ") is not the loaded overworld's datapacks folder (" + expected + ").",
                "Start with -D" + BACAP_DIRECTORY + "=" + expected + " or fix level-name in server.properties.");
    }

    void deriveServerId(Path file) {
        if (get.apply(BRIDGE_SERVER) != null) return;
        UUID id;
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            var p = new Properties();
            try (InputStream in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) { p.load(in); }
            catch (IOException e) { throw refusal("ATD_LAUNCH_SERVER_IDENTITY", file + " could not be read: " + e.getMessage()); }
            try { id = UUID.fromString(String.valueOf(p.getProperty("serverId")).trim()); }
            catch (IllegalArgumentException e) { id = null; }
            if (id == null || id.equals(new UUID(0, 0)) || !p.stringPropertyNames().equals(Set.of("serverId")))
                throw refusal("ATD_LAUNCH_SERVER_IDENTITY", file + " is damaged. Restore it from a backup.",
                    "Do not delete it to get a new one: players' clients know this server by that id.");
        } else {
            id = UUID.randomUUID();
            var p = new Properties();
            p.setProperty("serverId", id.toString());
            writeOnce(file, p, "AchieveToDo server identity. Created once; keep it with the server and never edit it.");
        }
        put(BRIDGE_SERVER, id.toString());
    }

    boolean bindWorldGroupOnFreshInstall(Path progression, List<LoadedWorld> worlds) {
        Path group = progression.resolve(WORLD_GROUP_FILE);
        if (Files.exists(group, LinkOption.NOFOLLOW_LINKS)) return false;
        if (Files.exists(progression.resolve(SELECTION_FILE), LinkOption.NOFOLLOW_LINKS))
            throw refusal("ATD_LAUNCH_WORLD_GROUP_MISSING", group + " is missing but this server already has saved progression ("
                + progression.resolve(SELECTION_FILE).getFileName() + ").",
                "AchieveToDo will not guess the binding of an existing installation. Restore world-group.properties from a backup.");
        var p = new Properties();
        p.setProperty("formatVersion", "1");
        p.setProperty("groupId", DEFAULT_GROUP_ID);
        p.setProperty("configId", UUID.randomUUID().toString());
        for (var role : ROLE_KEYS.entrySet()) {
            LoadedWorld world = byKey(worlds, role.getKey(), role.getValue());
            if (world == null) {
                if (role.getKey().equals("overworld"))
                    throw refusal("ATD_LAUNCH_WORLD_GROUP_UNBOUND", "No loaded world has the key minecraft:overworld.",
                        "Create " + group + " by hand (see STARTING_THE_SERVER.md).");
                continue; // nether or end disabled on this server
            }
            p.setProperty(role.getKey() + ".uuid", world.uuid().toString());
            p.setProperty(role.getKey() + ".key", world.key());
        }
        writeOnce(group, p, "AchieveToDo world group. Written once on the first start; keep it with the world.");
        derived.add("world-group.properties");
        return true;
    }

    void deriveProgressionWorld(Path groupFile) {
        if (get.apply(PROGRESSION_WORLD) != null && get.apply(PROGRESSION_WORLD_KEY) != null) return;
        var p = new Properties();
        try (InputStream in = Files.newInputStream(groupFile, LinkOption.NOFOLLOW_LINKS)) { p.load(in); }
        catch (IOException e) { throw refusal("ATD_LAUNCH_WORLD_GROUP_UNREADABLE", groupFile + " could not be read: " + e.getMessage()); }
        String uuid = p.getProperty("overworld.uuid"), key = p.getProperty("overworld.key");
        if (uuid == null || key == null)
            throw refusal("ATD_LAUNCH_WORLD_GROUP_UNREADABLE", groupFile + " has no overworld.uuid/overworld.key.");
        if (get.apply(PROGRESSION_WORLD) == null) put(PROGRESSION_WORLD, uuid.trim());
        if (get.apply(PROGRESSION_WORLD_KEY) == null) put(PROGRESSION_WORLD_KEY, key.trim());
    }

    /**
     * The level folder that holds {@code level.dat} and {@code datapacks}. On 26.2 a dimension's world folder is
     * {@code <level>/dimensions/<namespace>/<path>}; older layouts used the level folder itself.
     */
    public static Path levelRoot(Path worldFolder) {
        Path folder = worldFolder.toAbsolutePath().normalize();
        int n = folder.getNameCount();
        if (n >= 3 && folder.getName(n - 3).toString().equals("dimensions")) return folder.getParent().getParent().getParent();
        return folder;
    }

    private static LoadedWorld byKey(List<LoadedWorld> worlds, String role, String key) {
        LoadedWorld found = null;
        for (LoadedWorld world : worlds) {
            if (!key.equals(world.key())) continue;
            if (!role.equals(world.role()))
                throw refusal("ATD_LAUNCH_WORLD_GROUP_UNBOUND", "The world with key " + key + " is not a " + role + " dimension.");
            found = world;
        }
        return found;
    }

    private static LoadedWorld single(List<LoadedWorld> worlds, String role) {
        LoadedWorld found = byKey(worlds, role, ROLE_KEYS.get(role));
        if (found == null) throw refusal("ATD_LAUNCH_WORLD_GROUP_UNBOUND", "No loaded world has the key " + ROLE_KEYS.get(role) + ".");
        return found;
    }

    private void put(String name, String value) {
        set.accept(name, value);
        if (!derived.contains(name)) derived.add(name);
        set.accept(DERIVED, String.join(",", derived));
    }

    /** Creates the file atomically and never replaces an existing one. */
    private static void writeOnce(Path file, Properties content, String comment) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(temporary)) { content.store(out, comment); }
                try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
                Files.move(temporary, file); // a same-directory rename; without REPLACE_EXISTING it never overwrites
            } finally { Files.deleteIfExists(temporary); }
        } catch (FileAlreadyExistsException raced) {
            throw refusal("ATD_LAUNCH_WRITE", file + " appeared while starting; start again.");
        } catch (IOException e) {
            throw refusal("ATD_LAUNCH_WRITE", file + " could not be written: " + e.getMessage());
        }
    }

    private static StartupModePolicy.Refusal refusal(String code, String... detail) {
        var lines = new ArrayList<String>();
        lines.add(code + ": " + detail[0]);
        lines.addAll(Arrays.asList(detail).subList(1, detail.length));
        return new StartupModePolicy.Refusal(code, lines);
    }
}
