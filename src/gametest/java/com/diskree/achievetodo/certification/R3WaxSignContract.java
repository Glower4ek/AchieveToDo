package com.diskree.achievetodo.certification;
import com.google.gson.*;import java.nio.file.*;
/** S5_R3_WAX_SIGN_CONTRACT_V1: object identity is observed; actual consumption is mandatory. */
public final class R3WaxSignContract {
 public static void validate(JsonObject r){
  require(!r.get("criterionBefore").getAsBoolean()&&r.get("criterionAfter").getAsBoolean(),"criterion transition");
  var p=r.getAsJsonObject("actionProof");require(!p.get("waxedBefore").getAsBoolean()&&p.get("waxedAfter").getAsBoolean(),"wax transition");
  require(r.get("heldCountBefore").getAsInt()==1&&r.get("heldCountAfter").getAsInt()==0,"held consumption");
  require(r.get("interactionStackCountBefore").getAsInt()==1&&r.get("interactionStackCountAfter").getAsInt()==0&&r.get("interactionStackConsumed").getAsBoolean(),"interaction consumption");
  require(r.get("sameStackReference").isJsonPrimitive()&&r.getAsJsonPrimitive("sameStackReference").isBoolean(),"identity observation");
  int slot=r.get("selectedHandSlot").getAsInt();require(slot>=0&&slot<9&&slot==r.get("selectedHandSlotAfter").getAsInt(),"hand slot binding");
  require(r.get("joined").getAsBoolean()&&r.get("connectionRegistered").getAsBoolean()&&r.get("clientLoaded").getAsBoolean()&&r.get("normalScheduler").getAsBoolean(),"joined native player");
  require(r.get("gameMode").getAsString().equals("SURVIVAL")&&!r.get("infiniteMaterials").getAsBoolean(),"finite survival");
  require(r.get("interactionConsumesAction").getAsBoolean()&&r.get("hand").getAsString().equals("MAIN_HAND")&&r.get("boundary").getAsString().equals("ServerPlayerGameMode.useItemOn"),"real native action");
 }
 static void require(boolean c,String m){if(!c)throw new IllegalStateException("R3 wax contract: "+m);}
 public static void main(String[] args)throws Exception{
  var doc=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
  JsonObject r=doc.getAsJsonArray("entries").get(0).getAsJsonObject();validate(r);
  var results=new JsonArray();
  for(String field:new String[]{"heldCountAfter","interactionStackCountAfter","interactionStackConsumed","criterionAfter","selectedHandSlotAfter","infiniteMaterials","joined"}){
   var copy=r.deepCopy();if(field.equals("heldCountAfter")||field.equals("interactionStackCountAfter"))copy.addProperty(field,1);
   else if(field.equals("selectedHandSlotAfter"))copy.addProperty(field,(r.get(field).getAsInt()+1)%9);
   else copy.addProperty(field,field.equals("infiniteMaterials"));
   var out=new JsonObject();out.addProperty("field",field);boolean rejected=false;
   try{validate(copy);}catch(IllegalStateException expected){rejected=true;out.addProperty("error",expected.getMessage());}
   require(rejected,"negative not rejected: "+field);out.addProperty("rejected",rejected);results.add(out);
  }
  Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(results));
 }
}