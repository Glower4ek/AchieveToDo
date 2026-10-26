package com.diskree.achievetodo.certification;
import com.google.gson.JsonObject;import java.nio.file.Path;import java.util.*;
/** Adds the observed current BACAP sprint requirement, preserving every frozen assertion. */
public final class R4CurrentLocationContract {
 public static void validateReceipt(JsonObject receipt,JsonObject row,String run,String fingerprint) {
  PhaseALocationHolderSetWorldgenEvidenceValidation.validateReceipt(receipt,currentRow(row),run,fingerprint);
  verifyCurrentReceipt(receipt);
 }
 public static JsonObject currentRow(JsonObject row){var copy=row.deepCopy();if(copy.get("criterion").getAsString().equals("silent_but_deadly"))copy.getAsJsonObject("selectedPredicate").getAsJsonObject("flags").addProperty("is_sprinting",true);return copy;}
 public static List<JsonObject> cases(Path root)throws Exception{var out=new ArrayList<JsonObject>();for(var row:PhaseALocationHolderSetWorldgenCertification.cases(root))out.add(currentRow(row));return out;}
 public static void verifyCurrentReceipt(JsonObject receipt){
  if(receipt.get("criterion").getAsString().equals("silent_but_deadly")) {
   var context=receipt.getAsJsonObject("contextWitness");
   if(!context.get("is_sneaking").getAsBoolean()||!context.get("is_sprinting").getAsBoolean())throw new IllegalStateException("Current location requires sneaking AND sprinting");
  }
  if(receipt.get("advancementId").getAsString().equals("blazeandcave:nether/inception")){var context=receipt.getAsJsonObject("contextWitness");if(!context.get("currentAndFrozenBlockMembership").getAsBoolean()||!context.get("currentBlockTag").getAsString().equals("blazeandcave:non_underwater_blocks")||!context.get("frozenSelectedBlock").getAsString().equals("minecraft:air"))throw new IllegalStateException("Current/frozen non-underwater membership missing");}
 }
}
