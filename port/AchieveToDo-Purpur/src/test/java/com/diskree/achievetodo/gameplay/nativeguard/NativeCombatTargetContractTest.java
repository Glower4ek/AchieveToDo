package com.diskree.achievetodo.gameplay.nativeguard;
import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
public final class NativeCombatTargetContractTest {
 private static int checks;
 private static void check(boolean value){if(!value)throw new AssertionError("combat target "+checks);checks++;}
 public static void main(String[] args)throws Exception {
  String path=Objects.requireNonNull(System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE"),"Mandatory pinned native input");
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(path).toUri().toURL());
  try(var files=Files.walk(Path.of(path).getParent().getParent().getParent().resolve("libraries"))){for(var f:files.filter(f->f.toString().endsWith(".jar")).toList())urls.add(f.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),NativeCombatTargetContractTest.class.getClassLoader());var jar=new JarFile(path)){
   var cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
   for(var site:NativeToolSpecs.COMBAT_SITES){
    byte[] raw=jar.getInputStream(jar.getJarEntry(site.owner()+".class")).readAllBytes();
    byte[] input=raw;
    if(site.owner().endsWith("/Player"))input=NativeActionAgent.transformNative(site.owner(),raw,loader);
    byte[] transformed=NativeToolAgent.transformNative(site.owner(),input,loader);check(cf.verify(transformed).isEmpty());
    var model=cf.parse(transformed);var method=model.methods().stream().filter(m->m.methodName().equalsString(site.method()) && m.methodType().equalsString(site.descriptor())).findFirst().orElseThrow();
    var calls=method.code().orElseThrow().elementStream().filter(e->e instanceof InvokeInstruction).map(e->((InvokeInstruction)e).name().stringValue()).toList();
    if(site.kind().equals("stab"))check(calls.indexOf("combatTargetDenied")>=0 && calls.indexOf("combatTargetDenied")<calls.indexOf("denied"));
    else if(site.kind().equals("predicate"))check(Collections.frequency(calls,"scopedTargetAllowed")==1);
    else{
     check(calls.contains("open") && calls.contains("finish") && calls.contains("failed"));
     check(calls.contains("normal")==site.kind().equals("piercing"));
     check(model.methods().stream().anyMatch(m->m.methodName().equalsString("achievetodo$combatBody$"+site.method()) && m.flags().has(java.lang.reflect.AccessFlag.PRIVATE) && m.flags().has(java.lang.reflect.AccessFlag.SYNTHETIC)));
     check(method.code().orElseThrow().elementStream().anyMatch(e->e instanceof ExceptionCatch));
    }
    byte[] missing=cf.transformClass(cf.parse(input),(cb,e)->{if(e instanceof MethodModel m && m.methodName().equalsString(site.method()) && m.methodType().equalsString(site.descriptor()))return;cb.with(e);});
    try{NativeToolAgent.transformNative(site.owner(),missing,loader);throw new AssertionError("Missing combat boundary accepted");}catch(IllegalStateException expected){checks++;}
   }
  }
  System.out.println("COMBAT_TARGET_BYTECODE_PASS checks="+checks+"; real-runtime=NOT_PROVEN");
 }
}
