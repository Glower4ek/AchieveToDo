package com.diskree.achievetodo.certification;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;
import static com.diskree.achievetodo.certification.Final19StaticContext.require;

/** Artifact-local validator, independent of the native executor. */
public final class Final19PreservedWorldgenEvidence {
    public static final String FAMILY="WORLDGEN_HOLDERSET_CONTEXT";
    public static final Path TEMP=Path.of("build/tmp/final19_implementation/worldgen_temp.json");
    public static final Path RUN=Path.of("build/tmp/final19_implementation/worldgen_run.json");
    public static final Path PERSISTENT=Final19StaticContext.RESOURCES.resolve("worldgen_holderset_context_evidence.json");
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Path root(){return Path.of(System.getProperty("achievetodo.phaseA.projectRoot","")).toAbsolutePath();}
    public static JsonObject read(Path path)throws Exception{return JsonParser.parseString(Files.readString(path)).getAsJsonObject();}
    public static void write(Path path,JsonObject json)throws Exception{Files.createDirectories(path.getParent());Files.writeString(path,GSON.toJson(json)+"\n");}
    public static List<JsonObject> rows(Path root,boolean exact)throws Exception{
        var catalog=read(root.resolve(Final19StaticContext.RESOURCES).resolve("worldgen_holderset_context_catalog.json"));List<JsonObject> rows=new ArrayList<>();
        for(var value:catalog.getAsJsonArray("cases")){var row=value.getAsJsonObject();if(row.get("retainedHistoricalProof").getAsBoolean())continue;if(exact||Set.of("plains","snowy_plains","pale_garden","nether_wastes","the_end").contains(row.get("criterion").getAsString()))rows.add(row);}return rows;
    }
    public static String fingerprint(Path root)throws Exception{
        StringBuilder value=new StringBuilder();
        for(String name:List.of("reference/phase_a_preservation/files/final/bacap.zip","src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java","src/test/resources/final19_certification/worldgen_holderset_context_catalog.json","tools/final19/java/com/diskree/achievetodo/certification/Final19StaticContext.java","tools/final19/java/com/diskree/achievetodo/certification/Final19WorldgenEvidence.java","tools/final19/gametest/com/diskree/achievetodo/certification/Final19WorldgenGameTest.java","src/test/resources/phase_a_certification/runtime_test_matrix.json","src/main/resources/fabric.mod.json"))value.append(name).append(':').append(Final19PreservedSemanticBridge.originalInputSha(root,name)).append('\n');
        return Final19StaticContext.sha(value.toString().getBytes(StandardCharsets.UTF_8));
    }
    public static String begin(Path root,boolean exact)throws Exception{
        require(!Files.exists(root.resolve(PERSISTENT)),"Family already persistent");
        {
            // The authoritative JUnit gate already resolved every typed selector. Do not rebuild
            // VanillaRegistries on the live server tick; verify its gate and current source bytes.
            var staticGate=read(root.resolve("build/tmp/final19_implementation/worldgen_static.json"));
            require(staticGate.get("criteriaCount").getAsInt()==37&&staticGate.get("requirementGroupCount").getAsInt()==37,"Missing authoritative typed static gate");
            require(Final19StaticContext.inputFingerprint(root,FAMILY).equals(staticGate.get("inputFingerprint").getAsString()),"Stale typed static gate");
            var catalog=read(root.resolve(Final19StaticContext.RESOURCES).resolve("worldgen_holderset_context_catalog.json"));
            try(var sourceZip=new ZipFile(root.resolve("reference/phase_a_preservation/files/final/bacap.zip").toFile())) {
                for(var s:catalog.getAsJsonArray("sources")){var source=s.getAsJsonObject();byte[] bytes;try(var in=sourceZip.getInputStream(sourceZip.getEntry(source.get("sourcePath").getAsString()))){bytes=in.readAllBytes();}require(Final19StaticContext.sha(bytes).equals(source.get("frozenSha256").getAsString()),"Frozen source drift");Final19StaticContext.validateExact(source,Final19StaticContext.convert(new String(bytes,StandardCharsets.UTF_8)));}
            }
            try(var zip=new ZipFile(root.resolve("build/run/gameTest/world/datapacks/bacap.zip").toFile())) {
                var marker=new Properties();try(var in=zip.getInputStream(zip.getEntry("achievetodo_compatibility/compat_26_2.properties"))){marker.load(in);}
                require("compat_26_2_r15".equals(marker.getProperty("version"))&&"equipment.body".equals(marker.getProperty("llamaCarpetNbtMapping")),"Stale runtime copy marker");
                for(var s:catalog.getAsJsonArray("sources")){var source=s.getAsJsonObject();JsonObject runtime;try(var in=zip.getInputStream(zip.getEntry(source.get("sourcePath").getAsString()))){runtime=JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}Final19StaticContext.validateExact(source,runtime);}
            }
        }
        String id=UUID.randomUUID().toString();JsonObject artifact=new JsonObject();artifact.addProperty("family",FAMILY);artifact.addProperty("runId",id);artifact.addProperty("exact",exact);artifact.addProperty("fingerprint",fingerprint(root));artifact.add("entries",new JsonArray());write(root.resolve(TEMP),artifact);
        JsonObject state=artifact.deepCopy();state.remove("entries");state.addProperty("runtimeCopySha256",Final19StaticContext.sha(Files.readAllBytes(root.resolve("build/run/gameTest/world/datapacks/bacap.zip"))));state.addProperty("expectedEntries",rows(root,exact).size());state.addProperty("stage","RUNNING");write(root.resolve(RUN),state);return id;
    }
    public static void append(Path root,JsonObject observed)throws Exception{var artifact=read(root.resolve(TEMP));require(artifact.get("runId").equals(observed.get("runId")),"Wrong run");artifact.getAsJsonArray("entries").add(observed);write(root.resolve(TEMP),artifact);}
    public static JsonObject validate(Path root,Path path,boolean exact)throws Exception{
        var artifact=read(root.resolve(path));require(FAMILY.equals(artifact.get("family").getAsString()),"Wrong family");require(artifact.get("exact").getAsBoolean()==exact,"Wrong run mode");require(fingerprint(root).equals(artifact.get("fingerprint").getAsString()),"Stale evidence fingerprint");
        var expected=new TreeMap<String,JsonObject>();for(var row:rows(root,exact))expected.put(row.get("criterion").getAsString(),row);
        var observed=new TreeSet<String>();Set<String> players=new HashSet<>();
        for(var value:artifact.getAsJsonArray("entries")){
            var receipt=value.getAsJsonObject();String criterion=receipt.get("criterion").getAsString();var row=expected.get(criterion);require(row!=null&&observed.add(criterion),"Wrong/duplicate criterion");
            require(receipt.get("runId").equals(artifact.get("runId")),"Wrong receipt run");require(players.add(receipt.get("playerUuid").getAsString()),"Reused player receipt");
            require(receipt.get("advancementId").equals(row.get("advancementId"))&&receipt.get("requirementGroup").equals(row.get("requirementGroup")),"Wrong group");
            require(receipt.get("observedBiome").equals(row.get("selectedBiome"))&&receipt.get("observedDimension").equals(row.get("selectedDimension")),"Wrong live biome/dimension");
            require("minecraft:note_block".equals(receipt.get("observedBlock").getAsString())&&"minecraft:default_block_use".equals(receipt.get("trigger").getAsString()),"Wrong default-use semantics");
            require(!receipt.get("criterionBefore").getAsBoolean()&&receipt.get("criterionAfter").getAsBoolean(),"Missing false-to-true transition");
            require(receipt.get("noteAfter").getAsInt()==(receipt.get("noteBefore").getAsInt()+1)%25,"No native tuning");
            for(String key:List.of("joined","connectionRegistered","clientLoaded","finiteMaterials","emptyHand","groupSatisfied","wrongBiomeNegative","wrongBlockNegative","nativeUseConsumed","noDirectTrigger","noManualAward"))require(receipt.get(key).getAsBoolean(),"Missing observed gate "+key);
            require("SURVIVAL".equals(receipt.get("gameMode").getAsString()),"Not SURVIVAL");
            var cleanup=receipt.getAsJsonObject("cleanup");for(String key:List.of("playerRemoved","connectionRemoved","channelSettled","biomeRestored","blocksRestored"))require(cleanup.get(key).getAsBoolean(),"Cleanup debt "+key);
        }
        require(observed.equals(expected.keySet()),"Missing exact groups");
        if(path.equals(TEMP)){var state=read(root.resolve(RUN));require(state.get("runId").equals(artifact.get("runId"))&&state.get("fingerprint").equals(artifact.get("fingerprint")),"TEMP/run-state drift");require(state.get("expectedEntries").getAsInt()==observed.size(),"Wrong selected count");}
        return artifact;
    }
    public static void complete(Path root,boolean exact)throws Exception{validate(root,TEMP,exact);var state=read(root.resolve(RUN));state.addProperty("stage",exact?"TEMP_PROMOTABLE":"TEMP_DIAGNOSTIC");write(root.resolve(RUN),state);}
}
