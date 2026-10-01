package com.diskree.achievetodo.certification;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
/** Frozen OR group selects the native adult-piglin interaction branch. */
public final class PhaseAMixedPiglinDistractionCertification {
    public static final String FAMILY="MIXED_PIGLIN_DISTRACTION", SOURCE="PhaseAMixedPiglinDistractionGameTest";
    public static final String ADVANCEMENT="minecraft:nether/distract_piglin", CRITERION="distract_piglin_directly";
    public static final String BOUNDARY="ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->Piglin.mobInteract->PiglinAi.mobInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY";
    public static final Path CATALOG=Path.of("src/test/resources/phase_a_certification/mixed_piglin_distraction_case_catalog.json"), PERSISTENT=Path.of("src/test/resources/phase_a_certification/mixed_piglin_distraction_execution_evidence.json");
    public static final Map<String,String> FROZEN=Map.of(ADVANCEMENT,"450e53e69fb67aaf5f78cb092c66bb90131dddc36eed6960ffb2722026cb3004");
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAMixedPiglinDistractionCertification(){}
    public static void main(String[] args)throws Exception{Path root=Path.of(args[0]).toAbsolutePath();Files.writeString(root.resolve(CATALOG),generate(root));}
    public static void validate(Path root)throws Exception{require(generate(root).equals(Files.readString(root.resolve(CATALOG))),"Frozen piglin catalog changed");}
    public static String generate(Path root)throws Exception{
        Path archive=root.resolve("reference/phase_a_preservation/files/final/bacap.zip");require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"),"BACAP changed");
        JsonObject raw;try(var zip=new ZipFile(archive.toFile(),StandardCharsets.UTF_8);var in=zip.getInputStream(zip.getEntry("data/minecraft/advancement/nether/distract_piglin.json"))){byte[] bytes=in.readAllBytes();require(sha(bytes).equals(FROZEN.get(ADVANCEMENT)),"Frozen advancement changed");raw=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();}
        var criteria=raw.getAsJsonObject("criteria");var groups=raw.getAsJsonArray("requirements");require(criteria.size()==2&&groups.size()==1&&groups.get(0).getAsJsonArray().size()==2,"Frozen OR group changed");
        var criterion=criteria.getAsJsonObject(CRITERION);require(criterion.get("trigger").getAsString().equals("minecraft:player_interacted_with_entity"),"Wrong native branch");
        var conditions=criterion.getAsJsonObject("conditions");require(conditions.getAsJsonObject("item").get("items").getAsString().equals("#minecraft:piglin_loved"),"Wrong loved item selector");
        var entity=conditions.getAsJsonArray("entity").get(0).getAsJsonObject().getAsJsonObject("predicate");require(entity.get("type").getAsString().equals("minecraft:piglin")&&!entity.getAsJsonObject("flags").get("is_baby").getAsBoolean(),"Wrong adult predicate");
        var player=conditions.getAsJsonArray("player");require(player.size()==4,"Wrong exclusions");
        Set<String> armor=new TreeSet<>();for(var term:player){var inverted=term.getAsJsonObject();require(inverted.get("condition").getAsString().equals("minecraft:inverted"),"Not inverted");var equipment=inverted.getAsJsonObject("term").getAsJsonObject("predicate").getAsJsonObject("equipment");require(equipment.size()==1,"Wrong slot predicate");for(var e:equipment.entrySet()){var a=e.getValue().getAsJsonObject().getAsJsonArray("items");require(a.size()==1&&a.get(0).getAsString().equals("minecraft:golden_"+switch(e.getKey()){case "head"->"helmet";case "chest"->"chestplate";case "legs"->"leggings";case "feet"->"boots";default->throw new IllegalStateException("Unknown armor");}),"Wrong golden exclusion");armor.add(e.getKey());}}
        require(armor.equals(Set.of("head","chest","legs","feet")),"Missing slot exclusion");
        JsonObject catalog=new JsonObject(),source=new JsonObject(),row=new JsonObject();catalog.addProperty("snapshot","phase_a_mixed_piglin_distraction_case_catalog");catalog.addProperty("family",FAMILY);catalog.addProperty("source",SOURCE);catalog.addProperty("minecraftVersion","26.2");catalog.addProperty("compatibilityMarker","compat_26_2_r15");catalog.addProperty("criteriaCount",2);catalog.addProperty("requirementGroupCount",1);
        source.addProperty("advancementId",ADVANCEMENT);source.addProperty("sha256",FROZEN.get(ADVANCEMENT));source.add("requirements",groups.deepCopy());source.add("frozenCriteria",criteria.deepCopy());JsonArray sources=new JsonArray();sources.add(source);catalog.add("sources",sources);
        row.addProperty("advancementId",ADVANCEMENT);row.addProperty("criterion",CRITERION);row.addProperty("requirementGroup",0);row.add("alternatives",groups.get(0).deepCopy());row.add("frozenConditions",conditions.deepCopy());row.addProperty("selectedItem","minecraft:gold_ingot");row.addProperty("runtimeBoundary",BOUNDARY);JsonArray rows=new JsonArray();rows.add(row);catalog.add("cases",rows);return GSON.toJson(catalog)+"\n";
    }
    public static List<JsonObject> cases(Path root)throws Exception{validate(root);return List.of(JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases").get(0).getAsJsonObject());}
    public static String key(JsonObject row){return row.get("advancementId").getAsString()+"#"+row.get("criterion").getAsString();}
    public static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
}
