package com.diskree.achievetodo.certification;
import com.google.gson.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.zip.ZipFile;
public final class PhaseAMixedPerfectRunCertification{
    public static final String FAMILY="MIXED_PERFECT_RUN",SOURCE="PhaseAMixedPerfectRunGameTest",ADVANCEMENT="blazeandcave:challenges/the_perfect_run",PREREQUISITE="blazeandcave:technical/spawn_perfect_one";
    public static final Path CATALOG=Path.of("src/test/resources/phase_a_certification/mixed_perfect_run_case_catalog.json"),PERSISTENT=Path.of("src/test/resources/phase_a_certification/mixed_perfect_run_execution_evidence.json");
    public static final Map<String,String> FROZEN=Map.of(ADVANCEMENT,"66accc2c4b0011b9932c7c3abe7ee6be0e5a38c792789ea69ec85c70eeb04142");
    private static final Map<String,String> CONTRACTS=Map.of(
        "data/blazeandcave/advancement/technical/spawn_perfect_one.json","94f70edb7d73fb65e6c7d6edfdfabd1606046c0f54d2873a5d51fc1e8255bad2",
        "data/blazeandcave/advancement/technical/spawn_perfect_all.json","b33b12c282c0f26bb856745907f402a0258b8787bd6e04220861d991e3b29fc0",
        "data/blazeandcave/function/perfect_run_start.mcfunction","7e2424169e21323341e6c052772b3f028456f7ac8b9e3d6633ae6e7d0bdbaf89",
        "data/blazeandcave/function/perfect_run_fail.mcfunction","5cccb85da0a89107a2005093e55fee5ff749d38f4917e594db4571bade95a6d3",
        "data/blazeandcave/function/one_second_timer.mcfunction","ced64a7d466e5751a065eebc720ab6b7a74f92041bf2200335661948d7fef43e");
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAMixedPerfectRunCertification(){}
    public static void main(String[] args)throws Exception{Path root=Path.of(args[0]).toAbsolutePath();Files.writeString(root.resolve(CATALOG),generate(root));}
    public static void validate(Path root)throws Exception{require(generate(root).equals(Files.readString(root.resolve(CATALOG))),"Frozen perfect-run catalog changed");}
    public static String generate(Path root)throws Exception{
        Path archive=root.resolve("reference/phase_a_preservation/files/final/bacap.zip");require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"),"BACAP changed");
        JsonObject raw,contracts=new JsonObject();try(var zip=new ZipFile(archive.toFile(),StandardCharsets.UTF_8)){
            byte[] bytes=zip.getInputStream(zip.getEntry("data/blazeandcave/advancement/challenges/the_perfect_run.json")).readAllBytes();require(sha(bytes).equals(FROZEN.get(ADVANCEMENT)),"Frozen advancement changed");raw=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:new TreeMap<>(CONTRACTS).entrySet()){try(var in=zip.getInputStream(zip.getEntry(e.getKey()))){bytes=in.readAllBytes();}require(sha(bytes).equals(e.getValue()),"Frozen support contract changed");JsonObject contract=new JsonObject();contract.addProperty("sha256",e.getValue());if(e.getKey().endsWith(".json"))contract.add("frozenJson",JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)));contracts.add(e.getKey(),contract);}
        }
        var criteria=raw.getAsJsonObject("criteria");require(criteria.keySet().equals(Set.of("dragon","wither","raid"))&&!raw.has("requirements"),"Expected three implicit AND groups");
        JsonObject catalog=new JsonObject(),source=new JsonObject();catalog.addProperty("snapshot","phase_a_mixed_perfect_run_case_catalog");catalog.addProperty("family",FAMILY);catalog.addProperty("source",SOURCE);catalog.addProperty("minecraftVersion","26.2");catalog.addProperty("compatibilityMarker","compat_26_2_r15");catalog.addProperty("criteriaCount",3);catalog.addProperty("requirementGroupCount",3);catalog.add("supportContracts",contracts);
        catalog.addProperty("completionPolicy","One joined finite SURVIVAL player completes all three native criteria while native spawn_perfect_one is live and before the frozen thirty-second failure window, with all damage counters zero. Frozen criteria require spawn_perfect_one rather than spawn_perfect_all; proof must not claim the stronger display text.");
        var one=contracts.getAsJsonObject("data/blazeandcave/advancement/technical/spawn_perfect_one.json").getAsJsonObject("frozenJson");require(one.getAsJsonArray("requirements").size()==1&&one.getAsJsonArray("requirements").get(0).getAsJsonArray().size()==3,"Prerequisite OR semantics changed");
        require(one.getAsJsonObject("criteria").getAsJsonObject("wither").get("trigger").getAsString().equals("minecraft:summoned_entity"),"Wrong native prerequisite branch");
        JsonArray groups=new JsonArray(),cases=new JsonArray();int index=0;for(String key:List.of("dragon","wither","raid")){
            var c=criteria.getAsJsonObject(key);String trigger=c.get("trigger").getAsString();require(trigger.equals(key.equals("raid")?"minecraft:hero_of_the_village":"minecraft:player_killed_entity"),"Wrong native boundary");var conditions=c.getAsJsonObject("conditions");require(conditions.getAsJsonObject("player").getAsJsonObject("type_specific").getAsJsonObject("advancements").get(PREREQUISITE).getAsBoolean(),"Prerequisite missing");
            if(!key.equals("raid"))require(conditions.getAsJsonObject("entity").get("type").getAsString().equals(key.equals("dragon")?"ender_dragon":"wither"),"Wrong victim");
            JsonArray group=new JsonArray();group.add(key);groups.add(group);JsonObject row=new JsonObject();row.addProperty("advancementId",ADVANCEMENT);row.addProperty("criterion",key);row.addProperty("requirementGroup",index++);row.addProperty("trigger",trigger);row.addProperty("boundary",key.equals("raid")?"native Raider.die->Raid hero bookkeeping->normal Raid.tick VICTORY->CriteriaTriggers.RAID_WIN":"ServerboundAttackPacket.handle->Player.attack->LivingEntity.hurtServer/die->CriteriaTriggers.PLAYER_KILLED_ENTITY");row.add("alternatives",group.deepCopy());row.add("frozenConditions",conditions.deepCopy());cases.add(row);
        }
        source.addProperty("advancementId",ADVANCEMENT);source.addProperty("sha256",FROZEN.get(ADVANCEMENT));source.add("requirements",groups);source.add("frozenCriteria",criteria.deepCopy());JsonArray sources=new JsonArray();sources.add(source);catalog.add("sources",sources);catalog.add("cases",cases);return GSON.toJson(catalog)+"\n";
    }
    public static List<JsonObject> cases(Path root)throws Exception{validate(root);var result=new ArrayList<JsonObject>();for(var row:JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases"))result.add(row.getAsJsonObject());return result;}
    public static String key(JsonObject row){return row.get("advancementId").getAsString()+"#"+row.get("criterion").getAsString();}
    public static String sha(byte[] bytes)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
}
