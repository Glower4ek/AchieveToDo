package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.datapack.BacapPacks;
import com.diskree.achievetodo.progression.*;
import com.diskree.achievetodo.tracking.*;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import static com.diskree.achievetodo.tests.TestSupport.*;

/** Mock API contracts are deliberately distinct from real BACAP runtime evidence. */
public final class ProgressionFoundationTest {
    private static int checks;
    private static void check(boolean value) { checks++; assertTrue(value); }
    private static void fails(Runnable action) {
        boolean failed = false;
        try { action.run(); } catch (RuntimeException expected) { failed = true; }
        check(failed);
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> action) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> {
            return switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(self);
                case "equals" -> self == args[0];
                case "toString" -> type.getSimpleName();
                default -> action.apply(method.getName(), args);
            };
        });
    }

    public static void main(String[] args) throws Exception {
        TrackingCoreTest.run();
        scoreboardReconciliation();
        worldPersistence();
        syntheticIdempotence();
        contentIdentity();
        System.out.println("S3A progression contracts PASS checks=" + checks + " (plus existing focused TrackingCoreTest)");
    }

    private static void scoreboardReconciliation() {
        Map<String, Integer> scores = new HashMap<>();
        String[] displayed = {"bac_advancements"};
        boolean[] inTeam = {false};
        Team team = proxy(Team.class, (name, args) -> switch (name) {
            case "getName" -> "red";
            case "getEntries" -> Set.of("Alice", "Offline");
            default -> null;
        });
        Map<String, Objective> objectives = new HashMap<>();
        for (String id : List.of("bac_advancements", "bac_advancements_team")) {
            objectives.put(id, proxy(Objective.class, (name, args) -> switch (name) {
                case "getName" -> id;
                case "getScore" -> proxy(Score.class, (n, a) -> switch (n) {
                    case "isScoreSet" -> scores.containsKey(args[0] + "|" + id);
                    case "getScore" -> scores.getOrDefault(args[0] + "|" + id, 0);
                    default -> null;
                });
                default -> null;
            }));
        }
        Scoreboard board = proxy(Scoreboard.class, (name, args) -> switch (name) {
            case "getObjective" -> args[0] instanceof DisplaySlot ? (args[0] == DisplaySlot.SIDEBAR ? objectives.get(displayed[0]) : null) : objectives.get(args[0]);
            case "getEntryTeam" -> inTeam[0] ? team : null;
            default -> null;
        });
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Player a = proxy(Player.class, (name, args) -> name.equals("getUniqueId") ? alice : name.equals("getName") ? "Alice" : null);
        Player b = proxy(Player.class, (name, args) -> name.equals("getUniqueId") ? bob : name.equals("getName") ? "Bob" : null);
        var source = new BukkitBacapProgressionSource(() -> board, Logger.getAnonymousLogger(), (id, type) -> 0);
        source.refreshDetectedObjective();check(source.isReady());
        scores.put("Alice|bac_advancements", 7);
        source.refreshCount(a); source.refreshCount(b);
        check(source.getObtainedAdvancementCount(alice) == 7);
        check(source.getObtainedAdvancementCount(bob) == 0);
        // Duplicate notifications never add deltas. Grant/revoke/reset effects are read back.
        source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 7);
        scores.put("Alice|bac_advancements", 8);source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 8);
        scores.put("Alice|bac_advancements", 3);source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 3);
        scores.remove("Alice|bac_advancements");source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 0);
        displayed[0] = "bac_advancements_team";source.refreshDetectedObjective();inTeam[0] = true;
        scores.put("Alice|bac_advancements_team", 5);scores.put("Offline|bac_advancements_team", 11);
        source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 16);
        inTeam[0] = false;source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 0);
        inTeam[0] = true;source.refreshCount(a);source.removePlayer(alice);check(source.getObtainedAdvancementCount(alice) == 0);
        source.refreshCount(a);check(source.getObtainedAdvancementCount(alice) == 16);
        displayed[0] = "missing";source.refreshDetectedObjective();check(!source.isReady());
        source.clear();check(source.getObtainedAdvancementCount(alice) == 0 && !source.isReady());
    }

    private static void worldPersistence() throws Exception {
        UUID uuid = UUID.randomUUID();
        var identity = new ProgressionWorldIdentity(uuid, "minecraft:overworld");
        check(identity.matches(uuid, "minecraft:overworld"));
        check(!identity.matches(UUID.randomUUID(), "minecraft:overworld"));
        check(!identity.matches(uuid, "minecraft:the_nether"));
        fails(() -> identity.verifySaved(uuid.toString(), null));
        fails(() -> new ProgressionWorldIdentity(new UUID(0, 0), "minecraft:overworld"));
        String[] displayName = {"display-a"};
        World world = proxy(World.class, (name, args) -> switch (name) {
            case "getUID" -> uuid;
            case "getName" -> displayName[0];
            case "getKey" -> NamespacedKey.minecraft("overworld");
            default -> null;
        });
        Path file = Files.createTempDirectory("s3a-world").resolve("selection.properties");
        var store = new BukkitWorldProgressionConfigStore(file, world);
        check(store.load().isEmpty());
        store.save(new WorldProgressionConfig("normal"));
        byte[] persisted = Files.readAllBytes(file);
        displayName[0] = "renamed";
        check(new BukkitWorldProgressionConfigStore(file, world).load().orElseThrow().configName().equals("normal"));
        check(Arrays.equals(persisted, Files.readAllBytes(file)));
        Files.writeString(file, "worldUid=" + UUID.randomUUID() + "\nworldKey=minecraft:overworld\nconfigName=normal\n");
        fails(store::load);
        Files.writeString(file, "worldUid=" + uuid + "\nworldKey=minecraft:overworld\n");
        fails(store::load);
    }

    private static void syntheticIdempotence() {
        var key = new NamespacedKey("achievetodo", "abilities/jump");
        Set<String> awarded = new LinkedHashSet<>();
        int[] completions = {0}, writes = {0};
        Advancement advancement = proxy(Advancement.class, (name, args) -> switch (name) {
            case "getKey" -> key;
            case "getCriteria" -> List.of("demystified", "unlocked");
            default -> null;
        });
        AdvancementProgress progress = proxy(AdvancementProgress.class, (name, args) -> switch (name) {
            case "getAwardedCriteria" -> Set.copyOf(awarded);
            case "getRemainingCriteria" -> { var remaining = new LinkedHashSet<>(List.of("demystified", "unlocked"));remaining.removeAll(awarded);yield remaining; }
            case "isDone" -> awarded.size() == 2;
            case "awardCriteria" -> { boolean changed = awarded.add((String) args[0]); if (changed) { writes[0]++;if (awarded.size() == 2) completions[0]++; } yield changed; }
            case "revokeCriteria" -> { boolean changed = awarded.remove(args[0]);if (changed) writes[0]++;yield changed; }
            default -> null;
        });
        UUID id = UUID.randomUUID();
        Player player = proxy(Player.class, (name, args) -> name.equals("getAdvancementProgress") ? progress : name.equals("getUniqueId") ? id : null);
        var sync = new BukkitSyntheticAdvancementSynchronizer(k -> k.equals(key) ? advancement : null);
        var requirements = AbilityRequirements.normalize(Map.of(AbilityType.JUMP, 4));
        var locked = PlayerProgressionState.create(id, 3, requirements);
        sync.fullSync(player, locked);check(writes[0] == 0);
        sync.demystify(player, AbilityType.JUMP);check(completions[0] == 0 && awarded.size() == 1);
        var unlocked = locked.withObtainedAdvancementCount(4).withDemystified(AbilityType.JUMP);
        sync.applyUpdate(player, ProgressionStateSynchronizer.diff(locked, unlocked));check(completions[0] == 1);
        int firstWrites = writes[0];
        sync.fullSync(player, unlocked);sync.fullSync(player, unlocked);check(writes[0] == firstWrites && completions[0] == 1);
        var repeat = ProgressionStateSynchronizer.synchronize(unlocked, 4, true);
        check(repeat.unlockedAbilities().isEmpty() && repeat.relockedAbilities().isEmpty());
        sync.applyUpdate(player, repeat);check(writes[0] == firstWrites);
        sync.applyUpdate(player, ProgressionStateSynchronizer.diff(unlocked, locked));check(awarded.equals(Set.of("demystified")));
        sync.applyUpdate(player, ProgressionStateSynchronizer.diff(locked, unlocked));
        // Canonical relock/reunlock re-completes the synthetic message advancement; no item ledger.
        check(completions[0] == 2);
    }

    private static void contentIdentity() throws Exception {
        Path absent = Files.createTempDirectory("s3a-missing-bacap");
        boolean missing = false;
        try { BacapPacks.validate(absent); } catch (java.io.IOException expected) { missing = true; }
        check(missing);
        BacapPacks.beginDiscovery();check(!BacapPacks.discoveryReady());
        long generation = BacapPacks.generation();BacapPacks.completeDiscovery();check(BacapPacks.discoveryReady());
        BacapPacks.beginDiscovery();check(!BacapPacks.discoveryReady() && BacapPacks.generation() > generation);
        String external = System.getenv("ACHIEVETODO_TEST_BACAP");
        if (external == null || external.isBlank()) { System.out.println("S3A real-content automated fixtures NOT_RUN: ACHIEVETODO_TEST_BACAP unset");return; }
        var packs = BacapPacks.validate(Path.of(external));
        var definitions = BacapPacks.advancementDefinitions(packs);
        check(definitions.containsKey("blazeandcave:bacap/root"));
        check(definitions.get("blazeandcave:building/your_door_was_locked").getAsJsonObject("criteria").has("door"));
        String recount = new String(packs.get("bacap").get("data/blazeandcave/function/config/update_score.mcfunction"), java.nio.charset.StandardCharsets.UTF_8);
        check(recount.contains("advancements={minecraft:story/mine_stone=true}"));
        check(recount.contains("advancements={blazeandcave:bacap/root=true}"));
        check(!recount.contains("achievetodo:abilities/") && !recount.contains("minecraft:recipes/") && !recount.contains("advancements={blazeandcave:technical/"));
        var content = new TreeMap<>(packs.get("bacap"));
        String original = BacapPacks.contentDigest(content);
        content.put("data/blazeandcave/advancement/bacap/root.json", new byte[]{1});
        check(!original.equals(BacapPacks.contentDigest(content)));
        // Corrupt copies live only under test temp, never the operator's pack or repository assets.
        for (String pack : BacapPacks.IDS) Files.copy(Path.of(external).resolve(pack + ".zip"), absent.resolve(pack + ".zip"));
        Files.write(absent.resolve("bacap.zip"), new byte[]{1, 2, 3});
        boolean invalid = false;try { BacapPacks.validate(absent); } catch (java.io.IOException expected) { invalid = true; }check(invalid);
        System.out.println("S3A real-content fixtures PASS definitions=" + definitions.size());
    }
}
