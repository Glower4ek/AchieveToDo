package com.diskree.achievetodo.gameplay.nativeguard;

import com.diskree.achievetodo.datapack.BacapPacks;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/** Executable contracts only. Real native reload/state conservation is a separate mandatory proof. */
public final class SourceReloadContractTest {
 private static int checks;
 private static void check(boolean condition){checks++;if(!condition)throw new AssertionError("source reload check "+checks);}
 @FunctionalInterface private interface Action {void run()throws Exception;}
 private static void rejects(Action action)throws Exception{try{action.run();}catch(Exception expected){checks++;return;}throw new AssertionError("Expected source rejection");}
 private static final class Candidate implements AutoCloseable {int closes;public void close(){closes++;}}
 private static Path required(String name){String value=System.getenv(name);if(value==null)throw new IllegalStateException("Required fixture "+name);return Path.of(value);}
 private static void mutatedZip(Path output,Map<String,byte[]> entries,String extra)throws Exception{
  try(var zip=new ZipOutputStream(Files.newOutputStream(output))){for(var e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}if(extra!=null){zip.putNextEntry(new ZipEntry(extra));zip.write("NEGATIVE UNIT CONTROL".getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}}
 }
 public static void main(String[] args)throws Exception {
  List<String> valid=List.of("vanilla","paper","AchieveToDo/embedded","file/bacap.zip","file/bacap_override.zip");
  BacapPacks.validateEnabledPackSelection(valid);check(true);
  var alternate=new ArrayList<>(valid);Collections.swap(alternate,0,1);BacapPacks.validateEnabledPackSelection(alternate);check(true); // Preserve relative-order-only contract.
  for(String id:valid){var missing=new ArrayList<>(valid);missing.remove(id);rejects(()->BacapPacks.validateEnabledPackSelection(missing));}
  var wrongOrder=new ArrayList<>(valid);Collections.swap(wrongOrder,3,4);rejects(()->BacapPacks.validateEnabledPackSelection(wrongOrder));
  var extra=new ArrayList<>(valid);extra.add("file/unapproved.zip");rejects(()->BacapPacks.validateEnabledPackSelection(extra));
  var duplicate=new ArrayList<>(valid);duplicate.add(valid.getFirst());rejects(()->BacapPacks.validateEnabledPackSelection(duplicate));
  var caller=new ArrayList<>(valid);var copy=NativeSourceReloadAgent.snapshot(caller);caller.clear();check(copy.equals(valid));rejects(()->((Collection<Object>)copy).add("mutated"));rejects(()->NativeSourceReloadAgent.snapshot(null));
  check(NativeSourceReloadAgent.rejection(new Object(),valid).isCompletedExceptionally());check(!NativeSourceReloadAgent.installed());
  var candidate=new Candidate();rejects(()->NativeSourceReloadAgent.beforeCommit(new Object(),valid,candidate));check(candidate.closes==1);
  Path profile=required("ACHIEVETODO_TEST_BACAP");var current=BacapPacks.validate(profile);
  try(var bacap=new ZipFile(profile.resolve("bacap.zip").toFile());var override=new ZipFile(profile.resolve("bacap_override.zip").toFile())){
   var borrowed=Map.of("bacap",bacap,"bacap_override",override);var opened=BacapPacks.validateOpenedArchives(borrowed);
   for(String id:BacapPacks.IDS)check(BacapPacks.contentDigest(current.get(id)).equals(BacapPacks.contentDigest(opened.get(id))));
   check(bacap.size()>0 && override.size()>0); // Validator did not close native-owned handles.
   rejects(()->BacapPacks.validateOpenedArchives(Map.of("bacap",bacap)));
   Path temp=Files.createTempDirectory("atd-source-reload-negative-");Path altered=temp.resolve("altered.zip");mutatedZip(altered,current.get("bacap"),"data/atd_negative/advancement/extra.json");
   try(var changed=new ZipFile(altered.toFile())){rejects(()->BacapPacks.validateOpenedArchives(Map.of("bacap",changed,"bacap_override",override)));}
   var markerChanged=new TreeMap<>(current.get("bacap"));markerChanged.put(BacapPacks.MARKER,"version=invalid\nsourceSha1=invalid\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));Path marker=temp.resolve("marker.zip");mutatedZip(marker,markerChanged,null);
   try(var changed=new ZipFile(marker.toFile())){rejects(()->BacapPacks.validateOpenedArchives(Map.of("bacap",changed,"bacap_override",override)));}
  }
  Path nativeJar=required("ACHIEVETODO_TEST_PURPUR_NATIVE");var urls=new ArrayList<URL>();urls.add(nativeJar.toUri().toURL());
  try(var paths=Files.walk(nativeJar.getParent().getParent().getParent().resolve("libraries"))){for(Path file:paths.filter(q->q.toString().endsWith(".jar")).toList())urls.add(file.toUri().toURL());}
  try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getSystemClassLoader());var jar=new ZipFile(nativeJar.toFile())){
   for(var pin:NativeSourceReloadAgent.INPUTS.entrySet()){
    byte[] input=jar.getInputStream(jar.getEntry(pin.getKey()+".class")).readAllBytes();NativeSourceReloadAgent.verifyPin(pin.getKey(),input);check(true);
    byte[] wrong=input.clone();wrong[wrong.length-1]^=1;rejects(()->NativeSourceReloadAgent.verifyPin(pin.getKey(),wrong));
   }
   byte[] input=jar.getInputStream(jar.getEntry(NativeSourceReloadAgent.SERVER+".class")).readAllBytes();byte[] output=NativeSourceReloadAgent.transformNative(NativeSourceReloadAgent.SERVER,input,loader);var model=ClassFile.of().parse(output);
   int entrySites=0,commitSites=0,oneArgSites=0;
   for(var method:model.methods())if(method.code().isPresent()){
    var calls=new ArrayList<InvokeInstruction>();for(CodeElement element:method.code().get())if(element instanceof InvokeInstruction call)calls.add(call);
    for(var call:calls)if(call.owner().asInternalName().equals("com/diskree/achievetodo/gameplay/nativeguard/NativeSourceReloadAgent")){
     if(call.name().equalsString("rejection")){check(method.methodName().equalsString("reloadResources") && method.methodType().equalsString(NativeSourceReloadAgent.RELOAD));entrySites++;}
     if(call.name().equalsString("beforeCommit")){check(method.methodName().equalsString("lambda$reloadResources$4") && method.methodType().equalsString(NativeSourceReloadAgent.COMMIT));check(calls.getFirst()==call);commitSites++;}
     if(method.methodName().equalsString("reloadResources") && method.methodType().equalsString("(Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;"))oneArgSites++;
    }
   }
   check(entrySites==1 && commitSites==1 && oneArgSites==0);
  }
  System.out.println("ATD_SOURCE_RELOAD_CONTRACT_PASS checks="+checks+" pinnedInputs=3 nativeSites=2 runtimeStateConservation=SEPARATE");
 }
}
