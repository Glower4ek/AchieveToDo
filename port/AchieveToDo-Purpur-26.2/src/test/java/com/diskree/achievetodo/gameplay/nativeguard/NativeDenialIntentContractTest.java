package com.diskree.achievetodo.gameplay.nativeguard;
import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
/** Pinned opening branch and explicit silent query bytecode contracts; no runtime claim. */
public final class NativeDenialIntentContractTest {
 private static int checks;
 private static void check(boolean value){if(!value)throw new AssertionError("native denial "+checks);checks++;}
 public static void main(String[] args)throws Exception {
  String path=Objects.requireNonNull(System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE"));
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(path).toUri().toURL());
  try(var files=Files.walk(Path.of(path).getParent().getParent().getParent().resolve("libraries"))){for(var f:files.filter(f->f.toString().endsWith(".jar")).toList())urls.add(f.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),NativeDenialIntentContractTest.class.getClassLoader());var jar=new JarFile(path)){
   var cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
   for(String name:List.of("DoorBlock","TrapDoorBlock","FenceGateBlock")){
    String owner="net/minecraft/world/level/block/"+name;byte[] raw=jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes();
    check(cf.parse(raw).fields().stream().anyMatch(f->f.fieldName().equalsString("OPEN") && f.fieldType().equalsString("Lnet/minecraft/world/level/block/state/properties/BooleanProperty;") && f.flags().has(java.lang.reflect.AccessFlag.PUBLIC) && f.flags().has(java.lang.reflect.AccessFlag.STATIC)));
    byte[] output=NativeLandmarkAgent.transformNative(owner,raw,loader);check(cf.verify(output).isEmpty());
    var method=cf.parse(output).methods().stream().filter(m->m.methodName().equalsString("useWithoutItem")).findFirst().orElseThrow();
    check(method.code().orElseThrow().elementStream().filter(e->e instanceof InvokeInstruction i && i.name().equalsString("openingDenied")).count()==1);
   }
   byte[] actions=NativeLandmarkActions.class.getResourceAsStream("NativeLandmarkActions.class").readAllBytes();
   var model=cf.parse(actions);var query=model.methods().stream().filter(m->m.methodName().equalsString("entityQueryDenied")).findFirst().orElseThrow();
   check(query.code().orElseThrow().elementStream().anyMatch(e->e instanceof ConstantInstruction.LoadConstantInstruction c && c.constantValue().equals("SILENT")));
   var validity=model.methods().stream().filter(m->m.methodName().equalsString("entityReturnAllowed")).findFirst().orElseThrow();
   check(validity.code().orElseThrow().elementStream().anyMatch(e->e instanceof InvokeInstruction i && i.name().equalsString("entityQueryDenied")));
  }
  System.out.println("NATIVE_DENIAL_BRANCH_CONTRACT_PASS checks="+checks+"; runtime=NOT_PROVEN");
 }
}
