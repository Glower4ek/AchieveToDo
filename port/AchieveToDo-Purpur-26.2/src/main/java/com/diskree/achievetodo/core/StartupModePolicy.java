package com.diskree.achievetodo.core;

import java.util.*;
import java.util.function.Function;

/**
 * Decides, before any service starts, whether this launch may proceed. Only the native guard path in
 * gameplay mode CERTIFICATION is certified. The agents read their JVM properties in premain, before this
 * plugin exists, so the plugin cannot switch itself to that path: a launch without an explicit gameplay
 * mode is refused (fail closed) with the exact certified start command instead of silently running the
 * uncertified legacy Bukkit restriction path. The legacy path stays reachable only by naming it explicitly.
 *
 * <p>Pure and Bukkit-free so it can be tested without a server. It never changes what the certified
 * composition does once admitted; it only adds earlier, named refusals for launches that would otherwise
 * run uncertified or fail later without saying which property is missing.
 */
public final class StartupModePolicy {
    public static final String MODE = "achievetodo.gameplay.mode";
    /** Boolean switches of the certified composition, in the order they are documented. */
    public static final List<String> CERTIFIED_SWITCHES = List.of(
        "achievetodo.bridge.real",
        "achievetodo.progression.foundation",
        "achievetodo.bridge.groupScope",
        "achievetodo.landmarks.exactPrototype",
        "achievetodo.bridge.spike");
    /** Per-installation bindings the certified composition needs. The plugin does not invent them: they are
     * operator assertions checked against the world, so deriving them from the running server would defeat them. */
    public static final String BACAP_DIRECTORY = "achievetodo.bacap.directory";
    public static final String PROGRESSION_WORLD = "achievetodo.progression.world";
    public static final String PROGRESSION_WORLD_KEY = "achievetodo.progression.worldKey";
    public static final String BRIDGE_SERVER = "achievetodo.bridge.server";
    public static final List<String> PER_WORLD = List.of(BACAP_DIRECTORY, PROGRESSION_WORLD, PROGRESSION_WORLD_KEY, BRIDGE_SERVER);

    /** What the running server can tell an operator. Every field may be null when unknown. */
    public record Hints(String overworldUuid, String overworldKey, String worldGroupOverworldUuid,
                        String worldGroupOverworldKey, String datapacksDirectory, List<String> loadedWorlds) {
        public static final Hints NONE = new Hints(null, null, null, null, null, List.of());
        public Hints { loadedWorlds = loadedWorlds == null ? List.of() : List.copyOf(loadedWorlds); }
    }

    public record Decision(ProductGameplayAdmission.Mode mode, List<String> warnings) {
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
            var lines = new ArrayList<String>();
            lines.add("ATD_GAMEPLAY_MODE_REQUIRED: no -D" + MODE + " was given. AchieveToDo refuses to start enforcement.");
            lines.add("Only the native guard path in certification mode is certified. Without the mode the plugin used to fall");
            lines.add("back to the uncertified legacy Bukkit restriction path, which has known gaps (for example copper armor).");
            lines.addAll(guidance(properties, known));
            lines.add("To run the uncertified legacy path on purpose (testing only), start with -D" + MODE + "=diagnostic.");
            throw new Refusal("ATD_GAMEPLAY_MODE_REQUIRED", lines);
        }
        var mode = ProductGameplayAdmission.mode(value);
        return switch (mode) {
            case DIAGNOSTIC -> new Decision(mode, List.of(
                "ATD_UNCERTIFIED_LEGACY_PATH: -D" + MODE + "=diagnostic was given explicitly.",
                "This start runs the legacy Bukkit restriction path, which is NOT certified and has known gaps.",
                "Do not use it for real players. The certified start uses -D" + MODE + "=certification."));
            case CERTIFICATION -> {
                var missing = new ArrayList<String>();
                for (String name : CERTIFIED_SWITCHES)
                    if (!"true".equalsIgnoreCase(properties.apply(name))) missing.add("-D" + name + "=true");
                for (String name : PER_WORLD)
                    if (!validPerWorld(name, properties.apply(name))) missing.add("-D" + name + "=" + placeholder(name, known));
                if (!missing.isEmpty()) {
                    var lines = new ArrayList<String>();
                    lines.add("ATD_GAMEPLAY_MODE_PRECONDITIONS: certification mode is missing or has invalid values for:");
                    for (String flag : missing) lines.add("  " + flag);
                    lines.addAll(guidance(properties, known));
                    throw new Refusal("ATD_GAMEPLAY_MODE_PRECONDITIONS", lines);
                }
                yield new Decision(mode, List.of());
            }
            // PRODUCT keeps its own, unchanged admission checks in AchieveToDoPlugin/ProductGameplayAdmission.
            case PRODUCT -> new Decision(mode, List.of());
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
        lines.add("Certified start (all on one line, before -jar; see the operator note STARTING_THE_SERVER.md):");
        lines.add("  java -javaagent:plugins/achievetodo.jar -D" + MODE + "=certification \\");
        for (String name : CERTIFIED_SWITCHES) lines.add("    -D" + name + "=true \\");
        for (String name : PER_WORLD) {
            String given = properties.apply(name);
            String shown = validPerWorld(name, given) ? given : placeholder(name, hints);
            lines.add("    -D" + name + "=" + shown + " \\");
        }
        lines.add("    -jar purpur-26.2-2620.jar --nogui");
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
