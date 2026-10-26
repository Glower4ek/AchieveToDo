package com.diskree.achievetodo.certification;
import com.google.gson.*;import java.nio.file.*;import java.util.*;
/** Separate present-day TNT contract. Frozen projectile assertion remains historical and failed. */
public final class R4MulticlassedCurrentContract {
 public static List<JsonObject> cases(Path root)throws Exception {
  var result=new ArrayList<JsonObject>();
  for(var frozen:PhaseAMixedWeaponryMulticlassedCertification.cases(root)){
   var row=frozen.deepCopy();if(row.get("criterion").getAsString().equals("tnt")){
    row.addProperty("action","TNT");row.addProperty("selectedItem","minecraft:flint_and_steel");row.addProperty("requiredAbility","IGNITE_TNT");row.addProperty("nativeDirectEntity","minecraft:tnt");
    row.addProperty("boundary","ServerboundUseItemOnPacket.handle->TntBlock.useItemOn->PrimedTnt.explode->CriteriaTriggers.PLAYER_HURT_ENTITY");
    row.add("frozenConditions",JsonParser.parseString("{\"damage\":{\"type\":{\"tags\":[{\"id\":\"minecraft:is_explosion\",\"expected\":true}],\"direct_entity\":{\"entity_type\":\"minecraft:tnt\"}}}}").getAsJsonObject());
   }result.add(row);
  }if(result.size()!=17)throw new IllegalStateException("Current EXACT17 cardinality changed");return result;
 }
}
