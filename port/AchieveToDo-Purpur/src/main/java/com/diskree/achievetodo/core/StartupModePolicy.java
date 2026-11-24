package com.diskree.achievetodo.core;

import java.util.*;
import java.util.function.Function;

/**
 * Decides, before any service starts, whether this launch may proceed. Only the native guard path in
 * gameplay mode RELEASE is certified. The agents read their JVM properties in premain, before this
 * plugin exists, so the plugin cannot switch itself to that path. The agent can: with no explicit mode,
 * {@link ReleaseLaunchDefaults} (premain) sets the certified switches, and {@link ReleaseLaunchProfile} (bootstrap
 * and enable) fills in the per-installation values. A launch that still has no mode here therefore lacks the
 * agent (or contradicts a certified switch) and is refused (fail closed) with the one-line start command instead
 * of silently running the uncertified legacy Bukkit restriction path. The legacy path stays reachable only by
 * naming it explicitly.
 *
 * <p>Pure and Bukkit-free so it can be tested without a server. It never changes what the certified
 * composition does once admitted; it only adds earlier, named refusals for launches that would otherwise
 * run uncertified or fail later without saying which property is missing.
 */
public final class StartupModePolicy {
    public static final String MODE = "achievetodo.gameplay.mode";
    /** Boolean switches of the certified composition, in the order they are documented. */
    public static final List<String> CERTIFIED_SWITCHES = ReleaseLaunchDefaults.SWITCHES;
    /** Per-installation bindings the certified composition needs. On an explicit launch the plugin does not invent
     * them. On the release launch {@link ReleaseLaunchProfile} fills in only the absent ones, from files that are
     * written once and then verified on every later start, so they still are not copied from whatever world loaded. */
    public static final String BACAP_DIRECTORY = "achievetodo.bacap.directory";
    public static final String PROGRESSION_WORLD = "achievetodo.progression.world";
    public static final String PROGRESSION_WORLD_KEY = "achievetodo.progression.worldKey";
    public static final String BRIDGE_SERVER = "achievetodo.bridge.server";
    public static final List<String> PER_WORLD = List.of(BACAP_DIRECTORY, PROGRESSION_WORLD, PROGRESSION_WORLD_KEY, BRIDGE_SERVER);

    /** What the running server can tell an operator. Every field may be null when unknown. */
    public record Hints(String overworldUuid, String overworldKey, String worldGroupOverworldUuid,
                        String worldGroupOverworldKey, String datapacksDirectory, List<String> loadedWorlds,
                        String pluginJar) {
        public static final Hints NONE = new Hints(null, null, null, null, null, List.of(), null);
        public Hints {
            loadedWorlds = loadedWorlds == null ? List.of() : List.copyOf(loadedWorlds);
            if (pluginJar == null || pluginJar.isBlank()) pluginJar = DEFAULT_PLUGIN_JAR;
        }
        public Hints(String overworldUuid, String overworldKey, String worldGroupOverworldUuid,
                     String worldGroupOverworldKey, String datapacksDirectory, List<String> loadedWorlds) {
            this(overworldUuid, overworldKey, worldGroupOverworldUuid, worldGroupOverworldKey, datapacksDirectory, loadedWorlds, null);
        }
    }
    /** Shown when the real file name of the plugin jar is not known yet (bootstrap). */
    public static final String DEFAULT_PLUGIN_JAR = "plugins/achievetodo.jar";

    /** The release launch: the only flag an operator needs. */
    public static String releaseCommand(Hints hints) {
        return "java -javaagent:" + hints.pluginJar() + " -jar purpur-26.3-2660.jar --nogui";
    }

    public record Decision(GameplayAdmission.Mode mode, List<String> warnings) {
        public Decision { warnings = List.copyOf(warnings); }
    }

    /** A refused launch. {@link #guidance()} is meant to be logged line by line. */
    public static final class Refusal extends IllegalStateException {
        private final List<String> guidance;
        Refusal(String code, List<String> guidance) {
            super(code);
            this.guidance = List.copyOf(guidance);
        }
        public List<String> guidance() { return guidance; }
    }

    public static Decision decide(Function<String, String> properties, Hints hints) {
        Objects.requireNonNull(properties);
        Hints known = hints == null ? Hints.NONE : hints;
        String value = properties.apply(MODE);
        if (value == null) {
            // With the agent loaded the release defaults set the mode in premain, so a missing mode means either
            // no -javaagent at all, or an explicit switch that contradicts the certified value.
            var lines = new ArrayList<String>();
            String profile = properties.apply(ReleaseLaunchDefaults.PROFILE);
            if (profile != null && profile.startsWith(ReleaseLaunchDefaults.REFUSED_PREFIX)) {
                String conflict = profile.substring(ReleaseLaunchDefaults.REFUSED_PREFIX.length());
                lines.add("ATD_GAMEPLAY_MODE_REQUIRED: the start command sets -D" + conflict + ", which the certified launch does not allow.");
                lines.add("Remove that -D option. AchieveToDo refuses to start enforcement until then.");
            } else {
                lines.add("ATD_GAMEPLAY_MODE_REQUIRED: the AchieveToDo agent is not loaded. AchieveToDo refuses to start enforcement.");
                lines.add("The plugin jar must also be given to Java as an agent, before -jar. Keep your other options (-Xmx and so on).");
            }
            lines.add("Start command:");
            lines.add("  " + releaseCommand(known));
            lines.add("The jar after -javaagent: must be the same file as the one in plugins/. Nothing else is needed;");
            lines.add("see STARTING_THE_SERVER.md. The older explicit certified command (all -Dachievetodo.* options) also works.");
            throw new Refusal("ATD_GAMEPLAY_MODE_REQUIRED", lines);
        }
        var mode = GameplayAdmission.mode(value);
        return switch (mode) {
            case RELEASE -> {
                var missing = new ArrayList<String>();
                for (String name : CERTIFIED_SWITCHES)
                    if (!"true".equalsIgnoreCase(properties.apply(name))) missing.add("-D" + name + "=true");
                for (String name : PER_WORLD)
                    if (!validPerWorld(name, properties.apply(name))) missing.add("-D" + name + "=" + placeholder(name, known));
                if (!missing.isEmpty()) {
                    var lines = new ArrayList<String>();
                    lines.add("ATD_GAMEPLAY_MODE_PRECONDITIONS: release mode is missing or has invalid values for:");
                    for (String flag : missing) lines.add("  " + flag);
                    lines.addAll(guidance(properties, known));
                    throw new Refusal("ATD_GAMEPLAY_MODE_PRECONDITIONS", lines);
                }
                yield new Decision(mode, List.of());
            }
        };
    }

    static boolean validPerWorld(String name, String value) {
        if (value == null || value.isBlank()) return false;
        if (name.equals(PROGRESSION_WORLD) || name.equals(BRIDGE_SERVER)) {
            // Same parser as the runtime (UUID.fromString); the zero UUID never admits a bridge session.
            try {
                return !UUID.fromString(value).equals(new UUID(0, 0));
            } catch (IllegalArgumentException malformed) { return false; }
        }
        return true;
    }

    private static String placeholder(String name, Hints hints) {
        return switch (name) {
            case BACAP_DIRECTORY -> hints.datapacksDirectory() != null ? hints.datapacksDirectory()
                : "<absolute path of the world's datapacks directory with bacap.zip and bacap_override.zip>";
            case PROGRESSION_WORLD -> hints.worldGroupOverworldUuid() != null ? hints.worldGroupOverworldUuid()
                : hints.overworldUuid() != null ? hints.overworldUuid() : "<overworld UUID>";
            case PROGRESSION_WORLD_KEY -> hints.worldGroupOverworldKey() != null ? hints.worldGroupOverworldKey()
                : hints.overworldKey() != null ? hints.overworldKey() : "minecraft:overworld";
            case BRIDGE_SERVER -> "<stable server UUID; choose once and never change it>";
            default -> "<value>";
        };
    }

    /** The certified start command, with every value the server could observe already filled in. */
    static List<String> guidance(Function<String, String> properties, Hints hints) {
        var lines = new ArrayList<String>();
        lines.add("Explicit certified start (all on one line, before -jar; see the operator note STARTING_THE_SERVER.md):");
        lines.add("  java -javaagent:" + hints.pluginJar() + " -D" + MODE + "=release \\");
        for (String name : CERTIFIED_SWITCHES) lines.add("    -D" + name + "=true \\");
        for (String name : PER_WORLD) {
            String given = properties.apply(name);
            String shown = validPerWorld(name, given) ? given : placeholder(name, hints);
            lines.add("    -D" + name + "=" + shown + " \\");
        }
        lines.add("    -jar purpur-26.3-2660.jar --nogui");
        if (!hints.loadedWorlds().isEmpty()) {
            lines.add("Worlds loaded now (bind them in plugins/AchieveToDo/progression/world-group.properties):");
            for (String world : hints.loadedWorlds()) lines.add("  " + world);
        }
        if (!hints.equals(Hints.NONE) && hints.worldGroupOverworldUuid() == null)
            lines.add("plugins/AchieveToDo/progression/world-group.properties is missing or unreadable; create it first (see the operator note).");
        return lines;
    }

    private StartupModePolicy() {}
}
