package com.diskree.achievetodo.bridge;
import java.util.*;
public final class BridgeDenialIntentTest {
 private static int checks;
 private static void check(boolean value){if(!value)throw new AssertionError("denial check "+checks);checks++;}
 private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError("expected rejection");}
 public static void main(String[] args){
  var projection=new BridgeWire.Projection(UUID.randomUUID(),3,UUID.randomUUID(),BridgeProjectionTest.fixture());
  var reference=BridgeWire.reference(projection);
  for(String ability:projection.state().abilities().keySet()){
   var intent=new BridgeWire.DenialIntent(reference,ability);byte[] raw=BridgeWire.denialIntent(intent);
   check(BridgeWire.denialIntent(raw).equals(intent));check(raw.length<256);
   for(int length=0;length<raw.length;length++){byte[] cut=Arrays.copyOf(raw,length);rejects(()->BridgeWire.denialIntent(cut));}
   rejects(()->BridgeWire.denialIntent(Arrays.copyOf(raw,raw.length+1)));
   byte[] oversizedRef=raw.clone();oversizedRef[0]=1;oversizedRef[1]=0;rejects(()->BridgeWire.denialIntent(oversizedRef));
  }
  for(String bad:List.of("","JUMP","../jump","jump/extra","jump\n","a".repeat(65)))rejects(()->new BridgeWire.DenialIntent(reference,bad));
  rejects(()->BridgeWire.denialIntent(new byte[257]));
  var frame=new BridgeWire.Frame(BridgeWire.DENIAL_INTENT,BridgeWire.MAJOR,BridgeWire.MINOR,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),BridgeWire.FEATURES,BridgeWire.CATALOG,"test",BridgeWire.denialIntent(new BridgeWire.DenialIntent(reference,"jump")));
  check(Arrays.equals(BridgeWire.encode(frame),BridgeWire.encode(BridgeWire.decode(BridgeWire.encode(frame)))));
  check(BridgeWire.MINOR==5 && (BridgeWire.FEATURES&64)!=0);
  System.out.println("DENIAL_INTENT_CODEC_PASS checks="+checks+"; runtime/contextadmission=NOT_PROVEN");
 }
}
