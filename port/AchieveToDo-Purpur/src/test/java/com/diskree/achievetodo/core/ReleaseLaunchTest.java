package com.diskree.achievetodo.core;

import java.nio.file.*;
import java.util.*;

/** The release launch needs only -javaagent and must end in exactly the certified property set. */
public final class ReleaseLaunchTest {
    private static final UUID OVER = UUID.fromString("3f2b8a4e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
    private static final UUID NETHER = UUID.fromString("4f2b8a4e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
    private static final UUID END = UUID.fromString("5f2b8a4e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
    private static int cases;

    public static void main(String[] args) throws Exception {
        // 1. Agent defaults: no mode -> the five switches and release; nothing else is touched.
        var props = new HashMap<String, String>();
        check("release".equals(ReleaseLaunchDefaults.apply(props::get, props::put)), "release applied");
        check("release".equals(props.get(ReleaseLaunchDefaults.MODE)), "mode set");
        check(ReleaseLaunchDefaults.nativeMode("release") && ReleaseLaunchDefaults.nativeMode("certification")
            && !ReleaseLaunchDefaults.nativeMode("diagnostic") && !ReleaseLaunchDefaults.nativeMode(null), "native modes");
        for (String s : ReleaseLaunchDefaults.SWITCHES) check("true".equals(props.get(s)), s + " set");
        check(props.size() == ReleaseLaunchDefaults.SWITCHES.size() + 2, "only mode, switches and profile set: " + props);
        check(ReleaseLaunchDefaults.active(props::get), "active");

        // 2. Explicit launch (the rc.1 certified command) is left completely alone.
        var explicit = new HashMap<String, String>(Map.of("achievetodo.gameplay.mode", "certification"));
        check(ReleaseLaunchDefaults.apply(explicit::get, explicit::put) == null && explicit.size() == 1, "explicit untouched");
        check(!ReleaseLaunchDefaults.active(explicit::get), "explicit is not the release profile");
        var diagnostic = new HashMap<String, String>(Map.of("achievetodo.gameplay.mode", "diagnostic"));
        check(ReleaseLaunchDefaults.apply(diagnostic::get, diagnostic::put) == null && diagnostic.size() == 1, "diagnostic untouched");

        // 3. A contradicting explicit switch is never overridden: no mode, named conflict.
        for (String s : ReleaseLaunchDefaults.SWITCHES) {
            for (String bad : List.of("false", "yes", "")) {
                var conflict = new HashMap<String, String>(Map.of(s, bad));
                String result = ReleaseLaunchDefaults.apply(conflict::get, conflict::put);
                check(result.equals("refused:" + s + "=" + bad), "conflict " + s + "=" + bad);
                check(!conflict.containsKey(ReleaseLaunchDefaults.MODE) && conflict.get(s).equals(bad), "no override of " + s);
            }
            var agreeing = new HashMap<String, String>(Map.of(s, "TRUE"));
            check("release".equals(ReleaseLaunchDefaults.apply(agreeing::get, agreeing::put)) && agreeing.get(s).equals("TRUE"), "agreeing kept");
        }

        Path root = Files.createTempDirectory("atd-release-launch");
        try {
            // 4. BACAP directory from server.properties level-name (default "world"); explicit wins.
            Path server = Files.createDirectories(root.resolve("server"));
            var p = release();
            new ReleaseLaunchProfile(p::get, p::put).deriveBacapDirectory(server);
            check(Path.of(p.get(ReleaseLaunchProfile.BACAP_DIRECTORY)).equals(server.resolve("world/datapacks").toAbsolutePath().normalize()), "default level");
            Files.writeString(server.resolve("server.properties"), "motd=x\nlevel-name=survival\n");
            p = release();
            new ReleaseLaunchProfile(p::get, p::put).deriveBacapDirectory(server);
            check(p.get(ReleaseLaunchProfile.BACAP_DIRECTORY).endsWith("survival" + java.io.File.separator + "datapacks"), "level-name used");
            check(p.get(ReleaseLaunchProfile.DERIVED).equals(ReleaseLaunchProfile.BACAP_DIRECTORY), "derived recorded");
            var given = release(); given.put(ReleaseLaunchProfile.BACAP_DIRECTORY, "/explicit");
            new ReleaseLaunchProfile(given::get, given::put).deriveBacapDirectory(server);
            check(given.get(ReleaseLaunchProfile.BACAP_DIRECTORY).equals("/explicit") && !given.containsKey(ReleaseLaunchProfile.DERIVED), "explicit bacap wins");

            // 5. Fresh install: server id and world group written once; all per-world values derived.
            Path data = root.resolve("plugins/AchieveToDo");
            Path world = server.resolve("survival");
            var worlds = worlds(world);
            var profile = new ReleaseLaunchProfile(p::get, p::put);
            profile.deriveInstallation(data, worlds);
            var all = new HashMap<String, String>(p);
            check(StartupModePolicy.decide(all::get, null).mode() == GameplayAdmission.Mode.RELEASE, "release admission after derivation");
            check(p.get(ReleaseLaunchProfile.PROGRESSION_WORLD).equals(OVER.toString()), "progression world");
            check(p.get(ReleaseLaunchProfile.PROGRESSION_WORLD_KEY).equals("minecraft:overworld"), "progression key");
            check("true".equals(p.get(ReleaseLaunchProfile.FRESH_CONFIG)), "fresh config on first start");
            String serverId = p.get(ReleaseLaunchProfile.BRIDGE_SERVER);
            check(!UUID.fromString(serverId).equals(new UUID(0, 0)), "server id generated");
            var group = com.diskree.achievetodo.progression.ProgressionWorldGroup.load(data.resolve("progression/world-group.properties"));
            check(group.groupId().equals("main") && group.worlds().size() == 3, "world group bound");
            check(group.worlds().get("nether").uuid().equals(NETHER) && group.worlds().get("end").uuid().equals(END), "dimensions by key");
            String groupText = Files.readString(data.resolve("progression/world-group.properties"));

            // 6. Second start: same server id, same group (never rewritten), no freshConfig.
            Files.writeString(data.resolve("progression/world-selection.properties"), "configName=normal\n");
            var second = secondStart(server);
            new ReleaseLaunchProfile(second::get, second::put).deriveInstallation(data, worlds);
            check(second.get(ReleaseLaunchProfile.BRIDGE_SERVER).equals(serverId), "server id stable");
            check(!second.containsKey(ReleaseLaunchProfile.FRESH_CONFIG), "no fresh config later");
            check(Files.readString(data.resolve("progression/world-group.properties")).equals(groupText), "group not rewritten");

            // 7. A swapped world folder is not silently rebound: the derived world is the bound one, which the
            //    unchanged runtime check (WORLD_UNBOUND) then rejects because it is not loaded.
            var swapped = List.of(new ReleaseLaunchProfile.LoadedWorld("overworld", UUID.randomUUID(), "minecraft:overworld", world));
            var third = secondStart(server);
            new ReleaseLaunchProfile(third::get, third::put).deriveInstallation(data, swapped);
            check(third.get(ReleaseLaunchProfile.PROGRESSION_WORLD).equals(OVER.toString()), "binding comes from the file, not the loaded world");

            // 8. Refusals.
            Files.delete(data.resolve("progression/world-group.properties"));
            refuse(() -> { var r = secondStart(server); new ReleaseLaunchProfile(r::get, r::put).deriveInstallation(data, worlds); }, "ATD_LAUNCH_WORLD_GROUP_MISSING");
            Files.writeString(data.resolve("server-identity.properties"), "serverId=00000000-0000-0000-0000-000000000000\n");
            refuse(() -> { var r = secondStart(server); new ReleaseLaunchProfile(r::get, r::put).deriveInstallation(data, worlds); }, "ATD_LAUNCH_SERVER_IDENTITY");
            refuse(() -> { var r = secondStart(server); r.put(ReleaseLaunchProfile.BACAP_DIRECTORY, root.resolve("elsewhere").toString());
                new ReleaseLaunchProfile(r::get, r::put).deriveInstallation(root.resolve("other"), worlds); }, "ATD_LAUNCH_BACAP_DIRECTORY");
            refuse(() -> { var r = release(); new ReleaseLaunchProfile(r::get, r::put).deriveInstallation(root.resolve("nowhere"),
                List.of(new ReleaseLaunchProfile.LoadedWorld("nether", OVER, "minecraft:overworld", world))); }, "ATD_LAUNCH_WORLD_GROUP_UNBOUND");
            // Explicit values are kept even when files exist.
            var explicitIds = secondStart(server);
            explicitIds.put(ReleaseLaunchProfile.BRIDGE_SERVER, "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
            explicitIds.put(ReleaseLaunchProfile.PROGRESSION_WORLD, OVER.toString());
            explicitIds.put(ReleaseLaunchProfile.PROGRESSION_WORLD_KEY, "minecraft:overworld");
            Path freshData = root.resolve("plugins2/AchieveToDo");
            new ReleaseLaunchProfile(explicitIds::get, explicitIds::put).deriveInstallation(freshData, worlds);
            check(explicitIds.get(ReleaseLaunchProfile.BRIDGE_SERVER).equals("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"), "explicit server id wins");
            check(!Files.exists(freshData.resolve("server-identity.properties")), "no identity file when explicit");
        } finally {
            try (var walk = Files.walk(root)) { walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
        }

        // 8b. 26.2 layout: dimension folders live under <level>/dimensions/<ns>/<path>; datapacks at the level root.
        Path level = Path.of("srv", "survival").toAbsolutePath();
        check(ReleaseLaunchProfile.levelRoot(level.resolve("dimensions/minecraft/overworld")).equals(level), "26.2 overworld folder");
        check(ReleaseLaunchProfile.levelRoot(level.resolve("dimensions/minecraft/the_nether")).equals(level), "26.2 nether folder");
        check(ReleaseLaunchProfile.levelRoot(level).equals(level), "flat layout folder");

        // 9. Wiring: the jar's premain applies the defaults and then the unchanged certified bootstrap.
        String build = Files.readString(Path.of("build.gradle.kts"));
        check(build.contains("manifest.attributes[\"Premain-Class\"] = \"com.diskree.achievetodo.core.ReleaseLaunchPremain\""), "premain wired");
        String premain = Files.readString(Path.of("src/main/java/com/diskree/achievetodo/core/ReleaseLaunchPremain.java"));
        check(premain.indexOf("ReleaseLaunchDefaults.apply") < premain.indexOf("NativeTransactionPremain.premain"), "defaults before agents");
        String plugin = Files.readString(Path.of("src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java"));
        check(plugin.indexOf("ReleaseLaunchDefaults.active") > plugin.indexOf("PinnedStartupContract.installation")
            && plugin.indexOf("ReleaseLaunchDefaults.active") < plugin.indexOf("var startup = StartupModePolicy.decide"), "derivation after identity, before admission");
        check(Files.readString(Path.of("src/main/java/com/diskree/achievetodo/bootstrap/AchieveToDoBootstrap.java"))
            .contains("deriveBacapDirectory"), "bootstrap derives the BACAP directory");

        System.out.println("ATD_RELEASE_LAUNCH PASS cases=" + cases + "; -javaagent alone yields the certified property set");
    }

    private static HashMap<String, String> release() {
        var map = new HashMap<String, String>();
        ReleaseLaunchDefaults.apply(map::get, map::put);
        return map;
    }

    private static HashMap<String, String> secondStart(Path server) {
        var map = release();
        new ReleaseLaunchProfile(map::get, map::put).deriveBacapDirectory(server);
        return map;
    }

    private static List<ReleaseLaunchProfile.LoadedWorld> worlds(Path folder) {
        return List.of(
            new ReleaseLaunchProfile.LoadedWorld("overworld", OVER, "minecraft:overworld", folder.resolve("dimensions/minecraft/overworld")),
            new ReleaseLaunchProfile.LoadedWorld("nether", NETHER, "minecraft:the_nether", folder.resolve("dimensions/minecraft/the_nether")),
            new ReleaseLaunchProfile.LoadedWorld("end", END, "minecraft:the_end", folder.resolve("dimensions/minecraft/the_end")));
    }

    interface Step { void run() throws Exception; }

    private static void refuse(Step step, String code) throws Exception {
        try { step.run(); }
        catch (StartupModePolicy.Refusal refusal) {
            check(refusal.getMessage().equals(code), "code " + code + " got " + refusal.getMessage());
            check(refusal.guidance().get(0).startsWith(code), "guidance starts with " + code);
            return;
        }
        throw new AssertionError("accepted, expected " + code);
    }

    private static void check(boolean condition, String what) {
        cases++;
        if (!condition) throw new AssertionError(what);
    }
}
