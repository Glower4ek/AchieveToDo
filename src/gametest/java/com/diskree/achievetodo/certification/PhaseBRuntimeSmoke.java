package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.ability.AbilityType;
import com.google.gson.*;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** B10-only runtime receipts; every external copy comes from compiled production. */
public final class PhaseBRuntimeSmoke implements CustomTestMethodInvoker {
    private static JsonObject plan;
    private static JsonObject installed;
    private static List<String> selected;
    private static Path output() { return Path.of(System.getProperty("achievetodo.b10.output")); }
    private static String hash(byte[] bytes, String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
    }
    public static void write(String name, JsonObject value) {
        try { Files.writeString(output().resolve(name), new GsonBuilder().setPrettyPrinting().create().toJson(value) + "\n"); }
        catch (Exception e) { throw new IllegalStateException("B10 receipt failed", e); }
    }
    public static void install(LevelStorageSource.LevelStorageAccess storage) {
        try {
            plan = JsonParser.parseString(Files.readString(Path.of(System.getProperty("achievetodo.b10.plan")))).getAsJsonObject();
            require(plan.get("mode").getAsString().equals(System.getProperty("achievetodo.b10.mode")), "mode mismatch");
            Path datapacks = storage.getLevelPath(LevelResource.DATAPACK_DIR);
            Files.createDirectories(datapacks);
            installed = new JsonObject();
            installed.addProperty("mode", plan.get("mode").getAsString());
            installed.addProperty("worldDatapacks", datapacks.toAbsolutePath().toString());
            installed.addProperty("pid", ProcessHandle.current().pid());
            JsonArray copies = new JsonArray();
            for (JsonElement element : plan.getAsJsonArray("external")) {
                JsonObject input = element.getAsJsonObject();
                ExternalPack pack = ExternalPack.valueOf(input.get("enum").getAsString());
                Path source = Path.of(input.get("source").getAsString());
                byte[] raw = Files.readAllBytes(source);
                require(hash(raw, "SHA-1").equals(pack.getSha1()), "source pin mismatch " + pack);
                require(hash(raw, "SHA-256").equals(input.get("sourceSha256").getAsString()), "source SHA256 mismatch " + pack);
                Path target = datapacks.resolve(pack.getFileName());
                ExternalPackCompatibility.copyForWorld(source, target, pack);
                require(ExternalPackCompatibility.isCurrentWorldPack(target, pack), "world freshness failed " + pack);
                JsonObject copy = input.deepCopy();
                copy.addProperty("worldCopySha256", hash(Files.readAllBytes(target), "SHA-256"));
                copy.addProperty("isCurrentWorldPack", true);
                try (ZipFile zip = new ZipFile(target.toFile())) {
                    var entry = zip.getEntry("achievetodo_compatibility/compat_26_2.properties");
                    if (entry != null) {
                        Properties properties = new Properties(); properties.load(zip.getInputStream(entry));
                        JsonObject marker = new JsonObject();
                        for (String key : properties.stringPropertyNames()) marker.addProperty(key, properties.getProperty(key));
                        copy.add("marker", marker);
                        require("compat_26_2_r19".equals(properties.getProperty("version")), "revision mismatch");
                        require(pack.getSha1().equals(properties.getProperty("sourceSha1")), "actual source mismatch");
                        require("cbc432be35d5525001872c430877541cfa1fcad6".equals(properties.getProperty("rootOverrideSha1")), "root digest mismatch");
                    } else require(Arrays.equals(raw, Files.readAllBytes(target)), "unmarked transformed pack");
                }
                copies.add(copy);
            }
            installed.add("externalCopies", copies);
            write("installed.json", installed);
        } catch (Exception e) { throw new IllegalStateException("B10 exact pack installation failed", e); }
    }
    public static Collection<String> select(PackRepository repository) {
        selected = new ArrayList<>();
        // The base mod pack contains always-present ATD resources. Optional builtins are explicit.
        // Fabric's required base packs are also selected by normal world creation.
        // Experimental vanilla packs and every optional builtin stay unselected.
        for (String id : List.of("vanilla", "achievetodo", "fabric-convention-tags-v2", "fabric-gametest-api-v1")) {
            require(repository.isAvailable(id), "required base pack unavailable " + id);
            selected.add(id);
        }
        for (JsonElement id : plan.getAsJsonArray("productionPackIds")) selected.add(id.getAsString());
        for (String id : selected) require(repository.isAvailable(id), "pack unavailable " + id + " available=" + repository.getAvailableIds());
        installed.add("availableIds", new Gson().toJsonTree(repository.getAvailableIds()));
        installed.add("requestedSelectedIds", new Gson().toJsonTree(selected));
        write("installed.json", installed);
        return selected;
    }
    @GameTest(maxTicks = 600)
    public void productionSmoke(GameTestHelper helper) {
        require(System.getProperty("achievetodo.b10.mode") != null, "B10 property required");
        helper.runAfterDelay(10, () -> {
            JsonObject result = new JsonObject();
            try {
                var server = helper.getLevel().getServer();
                var ids = server.getAdvancements().getAllAdvancements().stream().map(a -> a.id().toString()).sorted().toList();
                result.add("liveAdvancementIds", new Gson().toJsonTree(ids));
                result.addProperty("liveAdvancementCount", ids.size());
                result.add("selectedPackIds", new Gson().toJsonTree(server.getPackRepository().getSelectedIds()));
                require(new ArrayList<>(server.getPackRepository().getSelectedIds()).equals(selected), "actual selection drift");
                validateAdvancementSet(ids, plan.getAsJsonObject("advancementSetContract"), result);
                List<String> functions = new ArrayList<>();
                server.getFunctions().getFunctionNames().forEach(id -> functions.add(id.toString()));
                require(!functions.isEmpty(), "function library empty");
                require(server.getFunctions().get(net.minecraft.resources.Identifier.parse("bacap_rewards:advancement_made_macro")).isPresent(), "native macro not loaded");
                result.addProperty("liveFunctionCount", functions.size());
                JsonArray objectives = new JsonArray();
                for (String name : List.of("bac_advancements", "bac_advfirst", "bac_advancements_team", "bac_advfirst_team_sum", "bac_advfirst_sum", "bac_advfirst_team")) {
                    require(server.getScoreboard().getObjective(name) != null, "native objective missing " + name); objectives.add(name);
                }
                result.add("nativeObjectivesPresent", objectives);
                result.addProperty("packLoadDiagnostic", "PASS");
                result.addProperty("deepSmokeCompleted", false);
                write("live.json", result);
                helper.succeed();
            } catch (Throwable failure) {
                result.addProperty("failure", failure.toString()); write("live.json", result);
                helper.fail("B10: " + failure);
            }
        });
    }
    @GameTest(maxTicks = 600)
    public void mainDeepSmoke(GameTestHelper helper) {
        require("MAIN".equals(System.getProperty("achievetodo.b10.mode")), "D1 MAIN only");
        helper.runAfterDelay(10, () -> {
            JsonObject result = new JsonObject();
            try {
                var server = helper.getLevel().getServer();
                var ids = server.getAdvancements().getAllAdvancements().stream().map(a -> a.id().toString()).sorted().toList();
                result.addProperty("liveAdvancementCount", ids.size());
                result.add("selectedPackIds", new Gson().toJsonTree(server.getPackRepository().getSelectedIds()));
                require(new ArrayList<>(server.getPackRepository().getSelectedIds()).equals(selected), "actual selection drift");
                validateAdvancementSet(ids, plan.getAsJsonObject("advancementSetContract"), result);
                List<String> functions = new ArrayList<>();
                server.getFunctions().getFunctionNames().forEach(id -> functions.add(id.toString()));
                require(!functions.isEmpty(), "function library empty");
                require(server.getFunctions().get(net.minecraft.resources.Identifier.parse("bacap_rewards:advancement_made_macro")).isPresent(), "native macro not loaded");
                result.addProperty("liveFunctionCount", functions.size());
                for (String name : List.of("bac_advancements", "bac_advfirst", "bac_advancements_team", "bac_advfirst_team_sum", "bac_advfirst_sum", "bac_advfirst_team"))
                    require(server.getScoreboard().getObjective(name) != null, "native objective missing " + name);
                result.addProperty("packLoadDiagnostic", "PASS");
                B10MainDeepWitness.start(helper, plan.getAsJsonObject("deepContract"), result);
            } catch (Throwable failure) {
                result.addProperty("failure", failure.toString()); write("deep.json", result);
                helper.fail("B10-D1: " + failure);
            }
        });
    }
    public static net.minecraft.world.level.WorldDataConfiguration companionProductionOrder(net.minecraft.world.level.WorldDataConfiguration configuration) {
        if (plan.get("companionContract").isJsonNull()) return configuration;
        var enabled = configuration.dataPacks().getEnabled();
        require(enabled.size() == selected.size() && new HashSet<>(enabled).equals(new HashSet<>(selected)), "GameTest bootstrap changed selected pack set");
        installed.add("fabricGameTestBootstrapOrder", new Gson().toJsonTree(enabled));
        installed.addProperty("productionOrderRestoredBeforeResourceLoad", true);
        write("installed.json", installed);
        return new net.minecraft.world.level.WorldDataConfiguration(
            new net.minecraft.world.level.DataPackConfig(List.copyOf(selected), configuration.dataPacks().getDisabled()), configuration.enabledFeatures());
    }
    @GameTest(maxTicks = 600)
    public void companionSmoke(GameTestHelper helper) {
        require(List.of("HARDCORE", "TERRALITH", "AMPLIFIED_NETHER", "NULLSCAPE").contains(System.getProperty("achievetodo.b10.mode")), "D2 companion only");
        helper.runAfterDelay(10, () -> {
            JsonObject result = new JsonObject();
            try {
                var server = helper.getLevel().getServer();
                var ids = server.getAdvancements().getAllAdvancements().stream().map(a -> a.id().toString()).sorted().toList();
                result.addProperty("liveAdvancementCount", ids.size());
                result.addProperty("mode", System.getProperty("achievetodo.b10.mode"));
                result.add("selectedPackIds", new Gson().toJsonTree(server.getPackRepository().getSelectedIds()));
                require(new ArrayList<>(server.getPackRepository().getSelectedIds()).equals(selected), "actual selection drift");
                validateAdvancementSet(ids, plan.getAsJsonObject("advancementSetContract"), result);
                List<String> functions = new ArrayList<>();
                server.getFunctions().getFunctionNames().forEach(id -> functions.add(id.toString()));
                require(!functions.isEmpty(), "function library empty");
                result.addProperty("liveFunctionCount", functions.size());
                for (String name : List.of("bac_advancements", "bac_advfirst", "bac_advancements_team", "bac_advfirst_team_sum", "bac_advfirst_sum", "bac_advfirst_team"))
                    require(server.getScoreboard().getObjective(name) != null, "native objective missing " + name);
                result.addProperty("packLoadDiagnostic", "PASS");
                B10CompanionRuntimeWitness.start(helper, plan.getAsJsonObject("companionContract"), result);
            } catch (Throwable failure) {
                result.addProperty("failure", failure.toString()); write("companion.json", result);
                helper.fail("B10-D2: " + failure);
            }
        });
    }
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    private static SortedSet<String> expectedIds(JsonObject contract, String field, String digest) throws Exception {
        JsonArray values = contract.getAsJsonArray(field);
        SortedSet<String> ids = new TreeSet<>();
        for (JsonElement value : values) require(ids.add(value.getAsString()), "duplicate expected ID in " + field);
        require(idSetHash(ids).equals(contract.get(digest).getAsString()), "expected ID digest mismatch " + field);
        return ids;
    }
    private static String idSetHash(Collection<String> ids) throws Exception {
        return hash((String.join("\n", new TreeSet<>(ids)) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8), "SHA-256");
    }
    private static SortedSet<String> difference(Collection<String> left, Collection<String> right) {
        SortedSet<String> result = new TreeSet<>(left); result.removeAll(right); return result;
    }
    private static void validateAdvancementSet(List<String> liveIds, JsonObject contract, JsonObject result) throws Exception {
        SortedSet<String> core = expectedIds(contract, "b8CoreIds", "b8CoreIdsSha256");
        SortedSet<String> atd = expectedIds(contract, "atdAbilityIds", "atdAbilityIdsSha256");
        SortedSet<String> expected = expectedIds(contract, "expectedFullIds", "expectedFullIdsSha256");
        Set<String> overlap = new TreeSet<>(core); overlap.retainAll(atd);
        require(overlap.isEmpty(), "B8 core intersects ATD production set");
        SortedSet<String> union = new TreeSet<>(core); union.addAll(atd);
        require(expected.equals(union), "full expected set is not the exact union");
        require(core.size() == contract.get("b8CoreExpectedCount").getAsInt(), "core contract count mismatch");
        require(atd.size() == contract.get("atdAbilityExpectedCount").getAsInt() && atd.size() == 152, "ATD contract count mismatch");
        SortedSet<String> compiledAbilities = new TreeSet<>();
        for (AbilityType ability : AbilityType.values()) compiledAbilities.add("achievetodo:abilities/" + ability.getName());
        require(AbilityType.values().length == 151 && compiledAbilities.size() == 151, "compiled AbilityType count mismatch");
        compiledAbilities.add("achievetodo:abilities/root");
        require(atd.equals(compiledAbilities), "ATD expected set differs from compiled AbilityType plus root");
        SortedSet<String> live = new TreeSet<>(liveIds);
        SortedSet<String> missingCore = difference(core, live);
        SortedSet<String> missingAtd = difference(atd, live);
        SortedSet<String> unexpected = difference(live, expected);
        int duplicateCount = liveIds.size() - live.size();
        boolean exact = live.equals(expected) && duplicateCount == 0;
        result.addProperty("b8CoreExpectedCount", core.size());
        result.addProperty("atdAbilityExpectedCount", atd.size());
        result.addProperty("expectedFullLiveCount", expected.size());
        result.addProperty("b8CoreIdsSha256", idSetHash(core));
        result.addProperty("atdAbilityIdsSha256", idSetHash(atd));
        result.addProperty("expectedFullIdsSha256", idSetHash(expected));
        result.addProperty("liveIdsSha256", idSetHash(live));
        result.add("missingB8CoreIds", new Gson().toJsonTree(missingCore));
        result.add("missingAtdAbilityIds", new Gson().toJsonTree(missingAtd));
        result.add("unexpectedLiveIds", new Gson().toJsonTree(unexpected));
        result.addProperty("duplicateIds", duplicateCount);
        result.addProperty("exactSetMatch", exact);
        result.addProperty("compiledAbilityTypeCount", AbilityType.values().length);
        require(expected.size() == contract.get("expectedFullLiveCount").getAsInt(), "full contract count mismatch");
        require(exact, "live advancement set mismatch: missing core=" + missingCore + ", missing ATD=" + missingAtd + ", unexpected=" + unexpected + ", duplicates=" + duplicateCount);
    }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR); method.invoke(this, helper);
    }
}
