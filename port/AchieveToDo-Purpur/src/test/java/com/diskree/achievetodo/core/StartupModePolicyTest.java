package com.diskree.achievetodo.core;

import java.nio.file.*;
import java.util.*;

/** A start without the agent is refused with the one-line command; only the release mode exists. */
public final class StartupModePolicyTest {
    private static final String WORLD = "3f2b8a4e-1c2d-4e5f-8a9b-0c1d2e3f4a5b";
    private static final String SERVER = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
    private static int cases;

    public static void main(String[] args) throws Exception {
        // 1. No mode at all (no agent): refused, with the one-line release command in the guidance.
        var refusal = refuse(Map.of(), StartupModePolicy.Hints.NONE, "ATD_GAMEPLAY_MODE_REQUIRED");
        String text = String.join("\n", refusal.guidance());
        check(text.contains("java -javaagent:plugins/achievetodo.jar -jar purpur-26.3-2660.jar --nogui"), "guidance names the one-line start");
        check(text.contains("agent is not loaded"), "guidance says the agent is missing");
        check(!text.contains("-Dachievetodo.bridge.spike"), "release guidance needs no -D switches");
        check(!text.contains("diagnostic"), "the removed legacy path is not offered");
        var named = new StartupModePolicy.Hints(null, null, null, null, null, null, "plugins/AchieveToDo-Purpur-mc26.3+0.4.0.jar");
        check(String.join("\n", refuse(Map.of(), named, "ATD_GAMEPLAY_MODE_REQUIRED").guidance())
            .contains("-javaagent:plugins/AchieveToDo-Purpur-mc26.3+0.4.0.jar -jar"), "guidance uses the real jar name");
        // 1b. Agent loaded but a certified switch contradicted: the conflict is named.
        text = String.join("\n", refuse(Map.of(ReleaseLaunchDefaults.PROFILE, "refused:achievetodo.bridge.spike=false"),
            null, "ATD_GAMEPLAY_MODE_REQUIRED").guidance());
        check(text.contains("-Dachievetodo.bridge.spike=false") && !text.contains("agent is not loaded"), "conflict named");
        check(StartupModePolicy.CERTIFIED_SWITCHES.equals(ReleaseLaunchDefaults.SWITCHES), "one switch list");
        // 1c. The explicit full command is still printed when an explicit certification launch lacks a value.
        var partial = new HashMap<>(certified()); partial.remove(StartupModePolicy.BRIDGE_SERVER);
        text = String.join("\n", refuse(partial, StartupModePolicy.Hints.NONE, "ATD_GAMEPLAY_MODE_PRECONDITIONS").guidance());
        check(text.contains("-javaagent:plugins/achievetodo.jar"), "explicit guidance names the agent");
        check(text.contains("-Dachievetodo.gameplay.mode=release"), "explicit guidance names the mode");
        for (String flag : StartupModePolicy.CERTIFIED_SWITCHES) check(text.contains("-D" + flag + "=true"), "guidance names " + flag);
        for (String flag : StartupModePolicy.PER_WORLD) check(text.contains("-D" + flag + "="), "guidance names " + flag);
        check(!text.contains("world-group.properties is missing"), "no world-group claim without hints");

        // 2. Every other certified flag present but the mode absent is still refused (old silent legacy start).
        var noMode = new HashMap<>(certified()); noMode.remove(StartupModePolicy.MODE);
        refuse(noMode, StartupModePolicy.Hints.NONE, "ATD_GAMEPLAY_MODE_REQUIRED");

        // 3. Hints from the running server are filled into the command; world-group binding wins.
        var hints = new StartupModePolicy.Hints(WORLD, "minecraft:overworld", null, null, "/srv/world/datapacks",
            List.of("overworld.uuid=" + WORLD + "  overworld.key=minecraft:overworld", "nether.uuid=x  nether.key=minecraft:the_nether"));
        var unbound = new HashMap<>(certified());
        unbound.remove(StartupModePolicy.PROGRESSION_WORLD); unbound.remove(StartupModePolicy.BACAP_DIRECTORY);
        text = String.join("\n", refuse(unbound, hints, "ATD_GAMEPLAY_MODE_PRECONDITIONS").guidance());
        check(text.contains("-Dachievetodo.progression.world=" + WORLD), "observed overworld uuid suggested");
        check(text.contains("-Dachievetodo.bacap.directory=/srv/world/datapacks"), "observed datapacks dir suggested");
        check(text.contains("world-group.properties is missing"), "missing world group reported");
        check(text.contains("  nether.uuid=x  nether.key=minecraft:the_nether"), "all loaded worlds listed");
        String bound = "11111111-2222-4333-8444-555555555555";
        text = String.join("\n", refuse(unbound, new StartupModePolicy.Hints(WORLD, "minecraft:overworld", bound,
            "minecraft:overworld", null, null), "ATD_GAMEPLAY_MODE_PRECONDITIONS").guidance());
        check(text.contains("-Dachievetodo.progression.world=" + bound), "world-group overworld preferred over observed");

        // 4. The legacy diagnostic path was removed in 0.3.0: naming it is an unknown mode.
        try { StartupModePolicy.decide(props(Map.of(StartupModePolicy.MODE, "diagnostic")), null); throw new AssertionError("diagnostic accepted"); }
        catch (IllegalArgumentException expected) { check(expected.getMessage().equals("ATD_UNKNOWN_GAMEPLAY_MODE"), "diagnostic removed"); }

        // 5. The full certified composition is admitted without warnings.
        var ok = StartupModePolicy.decide(props(certified()), StartupModePolicy.Hints.NONE);
        check(ok.mode() == GameplayAdmission.Mode.RELEASE && ok.warnings().isEmpty(), "certified admitted");
        var oldName = new HashMap<>(certified()); oldName.put(StartupModePolicy.MODE, "certification");
        check(StartupModePolicy.decide(props(oldName), null).mode() == GameplayAdmission.Mode.RELEASE, "pre-0.3.0 mode name accepted");
        var upper = new HashMap<>(certified()); upper.put("achievetodo.bridge.spike", "TRUE");
        check(StartupModePolicy.decide(props(upper), null).mode() == GameplayAdmission.Mode.RELEASE,
            "Boolean.getBoolean parity for TRUE");

        // 6. Each missing or false switch, and each missing per-world value, is named.
        for (String flag : StartupModePolicy.CERTIFIED_SWITCHES) {
            for (String bad : Arrays.asList(null, "false", "yes", "")) {
                var config = new HashMap<>(certified());
                if (bad == null) config.remove(flag); else config.put(flag, bad);
                var r = refuse(config, null, "ATD_GAMEPLAY_MODE_PRECONDITIONS");
                check(r.guidance().get(1).contains(flag), "names missing " + flag);
            }
        }
        for (String flag : StartupModePolicy.PER_WORLD) {
            var config = new HashMap<>(certified()); config.remove(flag);
            check(refuse(config, null, "ATD_GAMEPLAY_MODE_PRECONDITIONS").guidance().get(1).contains(flag), "names missing " + flag);
            config.put(flag, "  ");
            refuse(config, null, "ATD_GAMEPLAY_MODE_PRECONDITIONS");
        }
        for (String flag : List.of(StartupModePolicy.PROGRESSION_WORLD, StartupModePolicy.BRIDGE_SERVER)) {
            for (String bad : List.of("not-a-uuid", "00000000-0000-0000-0000-000000000000")) {
                var config = new HashMap<>(certified()); config.put(flag, bad);
                refuse(config, null, "ATD_GAMEPLAY_MODE_PRECONDITIONS");
            }
        }

        // 7. Unknown modes keep the existing error; the removed PRODUCT mode is unknown now.
        try { StartupModePolicy.decide(props(Map.of(StartupModePolicy.MODE, "Certification")), null); throw new AssertionError("case-changed mode accepted"); }
        catch (IllegalArgumentException expected) { check(expected.getMessage().equals("ATD_UNKNOWN_GAMEPLAY_MODE"), "unknown mode"); }
        try { StartupModePolicy.decide(props(Map.of(StartupModePolicy.MODE, "product")), null); throw new AssertionError("product accepted"); }
        catch (IllegalArgumentException expected) { check(expected.getMessage().equals("ATD_UNKNOWN_GAMEPLAY_MODE"), "product removed"); }

        // 8. Source wiring: the plugin no longer defaults to diagnostic, and its own unchanged precondition
        //    check covers the same switches the policy documents.
        Path plugin = Path.of("src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java");
        String source = Files.readString(plugin);
        check(!source.contains("getProperty(\"achievetodo.gameplay.mode\", \"diagnostic\")"), "no silent diagnostic default");
        check(source.contains("StartupModePolicy.decide(System::getProperty"), "policy decides the mode");
        check(source.contains("throw new IllegalStateException(\"ATD_GAMEPLAY_MODE_PRECONDITIONS\")"), "original check retained");
        for (String flag : StartupModePolicy.CERTIFIED_SWITCHES)
            check(source.contains("Boolean.getBoolean(\"" + flag + "\")"), "plugin checks " + flag);
        check(Files.readString(Path.of("src/main/java/com/diskree/achievetodo/bootstrap/AchieveToDoBootstrap.java"))
            .contains("StartupModePolicy.MODE"), "bootstrap prints guidance early");
        check(Files.isRegularFile(Path.of("STARTING_THE_SERVER.md")), "operator note present");

        System.out.println("ATD_STARTUP_MODE_POLICY PASS cases=" + cases + "; default start fails closed");
    }

    private static Map<String, String> certified() {
        var map = new HashMap<String, String>();
        map.put(StartupModePolicy.MODE, "release");
        for (String flag : StartupModePolicy.CERTIFIED_SWITCHES) map.put(flag, "true");
        map.put(StartupModePolicy.BACAP_DIRECTORY, "/srv/world/datapacks");
        map.put(StartupModePolicy.PROGRESSION_WORLD, WORLD);
        map.put(StartupModePolicy.PROGRESSION_WORLD_KEY, "minecraft:overworld");
        map.put(StartupModePolicy.BRIDGE_SERVER, SERVER);
        return map;
    }

    private static java.util.function.Function<String, String> props(Map<String, String> map) {
        return map::get;
    }

    private static StartupModePolicy.Refusal refuse(Map<String, String> map, StartupModePolicy.Hints hints, String code) {
        try {
            StartupModePolicy.decide(props(map), hints);
        } catch (StartupModePolicy.Refusal refusal) {
            check(refusal.getMessage().equals(code), "refusal code " + code + " got " + refusal.getMessage());
            check(refusal.guidance().get(0).startsWith(code), "guidance starts with code");
            return refusal;
        }
        throw new AssertionError("Unsafe start accepted: " + map);
    }

    private static void check(boolean condition, String what) {
        cases++;
        if (!condition) throw new AssertionError(what);
    }
}
