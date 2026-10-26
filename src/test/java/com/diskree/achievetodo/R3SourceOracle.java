package com.diskree.achievetodo;
import com.google.gson.*;import java.nio.file.*;import java.security.*;import java.util.*;
public final class R3SourceOracle {
 public static String expected(String historical,String path) throws Exception {
  String selected=System.getenv("ACHIEVETODO_SOURCE_ORACLE");
  if(selected==null)return historical;
  boolean r9=selected.equals("S5_R9_FABRIC_SOURCE_ORACLE_V1");
  if(!r9&&!selected.equals("S5_R3_FABRIC_SOURCE_ORACLE_V1"))throw new IllegalStateException("Wrong source oracle authority");
  byte[] raw=Files.readAllBytes(Path.of(System.getenv("ACHIEVETODO_SOURCE_ORACLE_FILE")));
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(r9?"7d57ee1bbd9ecb1b903741dd4335b8055f426278aa5910e89908a8207f986072":"162bcbcf7c7e2a9947fa642082dc9c6edd709bb914cda6ac4fb520afd42837f1"))throw new IllegalStateException("Oracle manifest tampered");
  var m=JsonParser.parseString(new String(raw,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
  for(var e:m.getAsJsonObject("productionSource").entrySet()){
   var b=Files.readAllBytes(Path.of(e.getKey()));
   if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)).equals(e.getValue().getAsString()))throw new IllegalStateException("Production source changed: "+e.getKey());
  }
  var pin=m.getAsJsonObject("pins").getAsJsonObject(path);
  if(pin==null||!historical.equals(pin.get("historicalSHA256").getAsString()))throw new IllegalStateException("Historical oracle differs");
  return pin.get("fabricSHA256").getAsString();
 }
}