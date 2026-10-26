package com.diskree.achievetodo.gameplay.nativeguard;
import java.lang.classfile.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
public final class NativePortalContractTest {
 public static void main(String[] args) throws Exception {
  int checks=0;
  for(int x:new int[]{-97,-96,0,96,97})for(int z:new int[]{-97,-96,0,96,97})for(int y:new int[]{74,75,76}){
   if(NativePortalAgent.central(x,y,z)!=(y==75&&Math.abs(x)<=96&&Math.abs(z)<=96))throw new AssertionError("central boundary");checks++;
  }
  String path=System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");if(path==null)throw new IllegalStateException("Native jar required for S4K contract");
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(path).toUri().toURL());
  try(var paths=Files.walk(Path.of(path).getParent().getParent().getParent().resolve("libraries"))){for(var p:paths.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),NativePortalContractTest.class.getClassLoader());var jar=new JarFile(path)){
   var cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
   String name=NativePortalAgent.ENTITY;byte[] raw=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();
   for(int mode=0;mode<2;mode++){
    byte[] b=mode==1?NativeLandmarkAgent.transformNative(name,raw,loader):raw;
    if(NativeActionSpecs.INPUTS.containsKey(name))b=NativeActionAgent.transformNative(name,b,loader);
    if(NativeToolSpecs.INPUTS.containsKey(name))b=NativeToolAgent.transformNative(name,b,loader);
    b=NativeComplexAgent.transformNative(name,b,loader);NativePortalAgent.verifyPin(name,b,mode);checks++;
    byte[] out=NativePortalAgent.transformNative(name,b,loader);if(!cf.verify(out).isEmpty())throw new AssertionError(cf.verify(out));checks++;
    byte[] corrupt=b.clone();corrupt[corrupt.length-1]^=1;
    try{NativePortalAgent.verifyPin(name,corrupt,mode);throw new AssertionError("wrong pin");}catch(IllegalStateException expected){checks++;}
    byte[] missing=cf.transformClass(cf.parse(b),(cb,e)->{if(e instanceof MethodModel m&&m.methodName().equalsString("setAsInsidePortal")&&m.methodType().equalsString(NativePortalAgent.DESCRIPTOR))return;cb.with(e);});
    try{NativePortalAgent.transformNative(name,missing,loader);throw new AssertionError("missing method");}catch(IllegalStateException expected){checks++;}
   }
  }
  try{NativePortalAgent.install(new Object(),(o,id)->false,NativePortalContractTest.class.getClassLoader());throw new AssertionError("missing startup");}catch(IllegalStateException expected){checks++;}
  System.out.println("S4K_NATIVE_CONTRACT_PASS checks="+checks+" pipelines=default-off,prototype-on");
 }
}
