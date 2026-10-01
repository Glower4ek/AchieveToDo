package com.diskree.achievetodo.certification;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
public final class PhaseAMixedWeaponryMulticlassedCertification{
    public static final String FAMILY="MIXED_WEAPONRY_MULTICLASSED",SOURCE="PhaseAMixedWeaponryMulticlassedGameTest",ADVANCEMENT="blazeandcave:weaponry/multiclassed";
    public static final Path CATALOG=Path.of("src/test/resources/phase_a_certification/mixed_weaponry_multiclassed_case_catalog.json"),PERSISTENT=Path.of("src/test/resources/phase_a_certification/mixed_weaponry_multiclassed_execution_evidence.json");
    public static final Map<String,String> FROZEN=Map.of(ADVANCEMENT,"14f649c0cbd49eb848ce7bd1c6ecfae9afa3c79413fc69c8ea1b29e2d09fe0a5");
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAMixedWeaponryMulticlassedCertification(){}
    public static void main(String[] args)throws Exception{Path root=Path.of(args[0]).toAbsolutePath();Files.writeString(root.resolve(CATALOG),generate(root));}
    public static void validate(Path root)throws Exception{require(generate(root).equals(Files.readString(root.resolve(CATALOG))),"Frozen multiclassed catalog changed");}
    public static String generate(Path root)throws Exception{
        Path archive=root.resolve("reference/phase_a_preservation/files/final/bacap.zip");require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"),"BACAP changed");
        JsonObject raw;try(var zip=new ZipFile(archive.toFile(),StandardCharsets.UTF_8);var in=zip.getInputStream(zip.getEntry("data/blazeandcave/advancement/weaponry/multiclassed.json"))){byte[] bytes=in.readAllBytes();require(sha(bytes).equals(FROZEN.get(ADVANCEMENT)),"Frozen source changed");raw=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();}
        var criteria=raw.getAsJsonObject("criteria");var groups=raw.getAsJsonArray("requirements");require(criteria.size()==18&&groups.size()==17,"Frozen cardinality changed");
        JsonObject catalog=new JsonObject(),source=new JsonObject();catalog.addProperty("snapshot","phase_a_mixed_weaponry_multiclassed_case_catalog");catalog.addProperty("family",FAMILY);catalog.addProperty("source",SOURCE);catalog.addProperty("minecraftVersion","26.2");catalog.addProperty("compatibilityMarker","compat_26_2_r15");catalog.addProperty("criteriaCount",18);catalog.addProperty("requirementGroupCount",17);
        source.addProperty("advancementId",ADVANCEMENT);source.addProperty("sha256",FROZEN.get(ADVANCEMENT));source.add("requirements",groups.deepCopy());source.add("frozenCriteria",criteria.deepCopy());JsonArray sources=new JsonArray();sources.add(source);catalog.add("sources",sources);
        JsonArray cases=new JsonArray();
        for(int i=0;i<groups.size();i++){
            var alternatives=groups.get(i).getAsJsonArray();String key=alternatives.get(0).getAsString();var criterion=criteria.getAsJsonObject(key);String trigger=criterion.get("trigger").getAsString();
            String action=switch(key){case "axe","shovel","pickaxe","hoe","sword","trident_melee","mace"->"MELEE";case "bow","tnt"->"BOW";case "crossbow","firework_rocket"->"CROSSBOW";case "fishing_rod"->"FISHING";case "snowball","egg","splash_potion","lingering_potion","wind_charge"->"THROW";default->throw new IllegalStateException("Unknown criterion "+key);};
            require(trigger.equals(key.equals("crossbow")?"minecraft:shot_crossbow":key.equals("fishing_rod")?"minecraft:fishing_rod_hooked":"minecraft:player_hurt_entity"),"Unexpected trigger");
            String item=switch(key){case "axe","shovel","pickaxe","hoe","sword"->"minecraft:wooden_"+key;case "trident_melee"->"minecraft:trident";case "tnt"->"minecraft:bow";case "firework_rocket"->"minecraft:crossbow";default->"minecraft:"+key;};
            JsonObject row=new JsonObject();row.addProperty("advancementId",ADVANCEMENT);row.addProperty("criterion",key);row.addProperty("requirementGroup",i);row.addProperty("trigger",trigger);row.addProperty("action",action);row.addProperty("selectedItem",item);row.addProperty("selectedEntity",key.equals("snowball")?"minecraft:blaze":"minecraft:ravager");row.addProperty("boundary",action.equals("MELEE")?"ServerboundAttackPacket.handle->Player.attack->LivingEntity.hurtServer->CriteriaTriggers.PLAYER_HURT_ENTITY":action.equals("FISHING")?"ServerboundUseItemPacket.handle->FishingRodItem.use->FishingHook.retrieve->CriteriaTriggers.FISHING_ROD_HOOKED":trigger.equals("minecraft:shot_crossbow")?"ServerboundUseItemPacket.handle->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW":"ServerboundUseItemPacket.handle->native projectile collision->LivingEntity.hurtServer->CriteriaTriggers.PLAYER_HURT_ENTITY");
            row.addProperty("requiredAbility",switch(key){case "axe","shovel","pickaxe","hoe","sword"->"USE_WOODEN_TOOLS";case "trident_melee"->"ATTACK_WITH_TRIDENT";case "mace"->"ATTACK_WITH_MACE";case "bow","tnt"->"SHOOT_BOW";case "crossbow","firework_rocket"->"SHOOT_CROSSBOW";case "fishing_rod"->"USE_FISHING_ROD";case "snowball"->"THROW_SNOWBALL";case "egg"->"THROW_EGG";case "wind_charge"->"THROW_WIND_CHARGE";default->"NONE";});
            row.add("alternatives",alternatives.deepCopy());var conditions=criterion.getAsJsonObject("conditions");row.add("frozenConditions",conditions.deepCopy());
            if(conditions.has("damage")){
                var damage=conditions.getAsJsonObject("damage").getAsJsonObject("type");if(damage.has("direct_entity")){String direct=damage.getAsJsonObject("direct_entity").get("type").getAsString();row.addProperty("nativeDirectEntity",direct.equals("minecraft:potion")?"minecraft:splash_potion":direct);if(direct.equals("minecraft:potion"))row.addProperty("acceptedProductionEntityMapping","potion->splash_potion");}
            }
            cases.add(row);
        }
        catalog.add("cases",cases);return GSON.toJson(catalog)+"\n";
    }
    public static List<JsonObject> cases(Path root)throws Exception{validate(root);List<JsonObject> result=new ArrayList<>();for(var e:JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases"))result.add(e.getAsJsonObject());return result;}
    public static String key(JsonObject row){return row.get("advancementId").getAsString()+"#"+row.get("criterion").getAsString();}
    public static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException(why);}
}
