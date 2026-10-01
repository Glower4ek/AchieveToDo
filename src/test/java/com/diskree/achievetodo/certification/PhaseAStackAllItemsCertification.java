package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;

/** Frozen completion-group complement of the immutable accepted item-tag slice. */
public final class PhaseAStackAllItemsCertification {
    public static final String FAMILY="RUNTIME_PARTIAL_INVENTORY_BACKLOG", SOURCE="PhaseAStackAllItemsInventoryBacklogGameTest", ADVANCEMENT="blazeandcave:challenges/stack_all_the_items";
    public static final String PICKUP="ItemEntity.playerTouch->Inventory.add->InventoryChangeTrigger";
    public static final Path CATALOG=Path.of("src/test/resources/phase_a_certification/stack_all_items_inventory_backlog_case_catalog.json"), PERSISTENT=Path.of("src/test/resources/phase_a_certification/stack_all_items_inventory_backlog_execution_evidence.json");
    public static final Map<String,String> FROZEN=Map.of(ADVANCEMENT,"f0d365374ebe9393830946a295d3005b78798001d496c39a78414481a9b0775c");
    public static final Path PRIOR=Path.of("src/test/resources/phase_a_certification/item_tag_inventory_changed_execution_evidence.json");
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAStackAllItemsCertification() {}
    public static void main(String[] args)throws Exception { Path root=Path.of(args[0]).toAbsolutePath();Files.writeString(root.resolve(CATALOG),generate(root)); }
    public static void validate(Path root)throws Exception { require(generate(root).equals(Files.readString(root.resolve(CATALOG))),"Frozen backlog catalog mismatch"); }
    public static String generate(Path root)throws Exception {
        Path archive=root.resolve("reference/phase_a_preservation/files/final/bacap.zip");require(sha(Files.readAllBytes(archive)).equals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"),"Frozen BACAP changed");
        String priorHash=sha(Files.readAllBytes(root.resolve(PRIOR)));require(priorHash.equals("b7378167a2496e561689ab5eb920d495c92e2eccc655ce1ced03cfaad7f4f651"),"Accepted smithing-template evidence changed");
        var prior=PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(root).get(ADVANCEMENT);require(prior!=null&&prior.greenCriteria().equals(Set.of("smithing_template")),"Accepted prior group coverage changed");
        byte[] bytes;try(var zip=new ZipFile(archive.toFile(),StandardCharsets.UTF_8)){bytes=zip.getInputStream(zip.getEntry("data/blazeandcave/advancement/challenges/stack_all_the_items.json")).readAllBytes();}
        require(sha(bytes).equals(FROZEN.get(ADVANCEMENT)),"Frozen stack advancement changed");var raw=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();var criteria=raw.getAsJsonObject("criteria");
        JsonArray groups=raw.getAsJsonArray("requirements");if(groups==null){groups=new JsonArray();for(String key:criteria.keySet()){JsonArray group=new JsonArray();group.add(key);groups.add(group);}}
        require(criteria.size()==320&&groups.size()==320,"Frozen completion scope changed");JsonArray cases=new JsonArray(),acceptedGroups=new JsonArray();
        for(int i=0;i<groups.size();i++){
            var alternatives=groups.get(i).getAsJsonArray();boolean accepted=false;for(var alternative:alternatives)accepted|=prior.greenCriteria().contains(alternative.getAsString());if(accepted){acceptedGroups.add(i);continue;}
            require(alternatives.size()==1,"Unexpected frozen OR group; derive a valid alternative before extending this catalog");String key=alternatives.get(0).getAsString();var criterion=criteria.getAsJsonObject(key);require(criterion.get("trigger").getAsString().equals("minecraft:inventory_changed"),"Wrong native trigger");
            var conditions=criterion.getAsJsonObject("conditions");require(conditions.keySet().equals(Set.of("items"))&&conditions.getAsJsonArray("items").size()==1,"Unhandled inventory context");var item=conditions.getAsJsonArray("items").get(0).getAsJsonObject();require(item.keySet().equals(Set.of("items"))||item.keySet().equals(Set.of("items","count")),"Unhandled item predicate");
            require(item.get("items").isJsonArray()&&item.getAsJsonArray("items").size()==1,"Backlog must retain only literal item groups");String selected=item.getAsJsonArray("items").get(0).getAsString();int count=item.has("count")?item.get("count").getAsInt():1;require(count==1||count==16||count==64,"Unhandled frozen count");
            var row=new JsonObject();row.addProperty("advancementId",ADVANCEMENT);row.addProperty("criterion",key);row.addProperty("requirementGroup",i);row.add("alternatives",alternatives.deepCopy());row.addProperty("trigger","minecraft:inventory_changed");row.addProperty("boundary",PICKUP);row.addProperty("selectedItem",selected);row.addProperty("requiredCount",count);row.add("frozenConditions",conditions.deepCopy());cases.add(row);
        }
        require(cases.size()==319&&acceptedGroups.size()==1,"Remaining group complement changed");var catalog=new JsonObject();catalog.addProperty("snapshot","phase_a_stack_all_items_inventory_backlog_case_catalog");catalog.addProperty("family",FAMILY);catalog.addProperty("source",SOURCE);catalog.addProperty("minecraftVersion","26.2");catalog.addProperty("compatibilityMarker","compat_26_2_r15");catalog.addProperty("criteriaCount",cases.size());catalog.addProperty("requirementGroupCount",cases.size());catalog.addProperty("frozenCriteriaCount",criteria.size());catalog.addProperty("frozenRequirementGroupCount",groups.size());
        var accepted=new JsonObject();accepted.addProperty("artifact",PRIOR.toString().replace('\\','/'));accepted.addProperty("sha256",priorHash);accepted.addProperty("criterion","smithing_template");accepted.add("requirementGroups",acceptedGroups);catalog.add("immutableAcceptedSlice",accepted);
        var source=new JsonObject();source.addProperty("advancementId",ADVANCEMENT);source.addProperty("sha256",FROZEN.get(ADVANCEMENT));source.add("requirements",groups.deepCopy());source.add("frozenCriteria",criteria.deepCopy());var sources=new JsonArray();sources.add(source);catalog.add("sources",sources);catalog.add("cases",cases);return GSON.toJson(catalog)+"\n";
    }
    public static List<JsonObject> cases(Path root)throws Exception { validate(root);var rows=new ArrayList<JsonObject>();for(var e:JsonParser.parseString(Files.readString(root.resolve(CATALOG))).getAsJsonObject().getAsJsonArray("cases"))rows.add(e.getAsJsonObject());return rows; }
    public static String key(JsonObject row){return row.get("advancementId").getAsString()+"#"+row.get("criterion").getAsString();}
    public static String sha(byte[] bytes)throws Exception { return PhaseAMixedPerfectRunCertification.sha(bytes); }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
