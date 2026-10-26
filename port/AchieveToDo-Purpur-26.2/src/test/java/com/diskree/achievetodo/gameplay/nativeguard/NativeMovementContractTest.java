package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Checks real pinned bytecode in both existing pipelines, not native gameplay certification. */
public final class NativeMovementContractTest {
    public static void main(String[] args) throws Exception {
        int checks = 0;
        String path = Objects.requireNonNull(System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE"), "Native jar required");
        var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(path).toUri().toURL());
        try (var paths = Files.walk(Path.of(path).getParent().getParent().getParent().resolve("libraries"))) {
            for (var p : paths.filter(x -> x.toString().endsWith(".jar")).toList()) urls.add(p.toUri().toURL());
        }
        try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), NativeMovementContractTest.class.getClassLoader()); var jar = new JarFile(path)) {
            var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
            for (String name : NativeMovementAgent.INPUTS.keySet()) for (int mode = 0; mode < 2; mode++) {
                byte[] b = jar.getInputStream(jar.getJarEntry(name + ".class")).readAllBytes();
                if (name.equals(NativeMovementAgent.LIVING)) b = NativeTransactionAgent.transformNative(name, b);
                if (mode == 1 && NativeLandmarkActionSpecs.HASHES.containsKey(name)) b = NativeLandmarkAgent.transformNative(name, b, loader);
                if (NativeActionSpecs.INPUTS.containsKey(name)) b = NativeActionAgent.transformNative(name, b, loader);
                if (NativeToolSpecs.INPUTS.containsKey(name)) b = NativeToolAgent.transformNative(name, b, loader);
                if (NativeComplexSpecs.INPUTS.containsKey(name)) b = NativeComplexAgent.transformNative(name, b, loader);
                if (NativePortalAgent.INPUTS.containsKey(name)) b = NativePortalAgent.transformNative(name, b, loader);
                NativeMovementAgent.verifyPin(name, b, mode); checks++;
                byte[] out = NativeMovementAgent.transformNative(name, b, loader);
                if (!cf.verify(out).isEmpty()) throw new AssertionError(cf.verify(out)); checks++;
                int expected = name.equals(NativeMovementAgent.PLAYER) ? 1 : 2, found = 0;
                for (var m : cf.parse(out).methods()) if (m.code().isPresent()) for (var e : m.code().get())
                    if (e instanceof InvokeInstruction call && call.owner().asInternalName().endsWith("/NativeMovementAgent")) found++;
                if (found != expected) throw new AssertionError("Exact inserted calls " + name + ":" + found); checks++;
                if (name.equals(NativeMovementAgent.LISTENER)) {
                    var movement = cf.parse(out).methods().stream().filter(m -> m.methodName().equalsString("handleMovePlayer")).findFirst().orElseThrow();
                    var calls = new ArrayList<String>();
                    for (var e : movement.code().orElseThrow()) if (e instanceof InvokeInstruction call
                        && (call.owner().asInternalName().equals(NativeMovementAgent.PLAYER)
                            || call.owner().asInternalName().endsWith("/NativeMovementAgent"))) calls.add(call.name().stringValue());
                    int preflight = calls.indexOf("ascent");
                    if (preflight < 0 || preflight >= calls.indexOf("onGround") || preflight >= calls.indexOf("move")
                        // Earlier absSnapTo is the separate passenger rotation branch, not on-foot position acceptance.
                        || preflight >= calls.lastIndexOf("absSnapTo")) throw new AssertionError("Ascent must precede the untrusted ground branch and both on-foot position writes");
                    checks++;
                    if (calls.contains("jump")) throw new AssertionError("Packet rejection must not depend on entering the native jump callback");
                    checks++;
                    var teleport = cf.parse(out).methods().stream().filter(m -> m.methodName().equalsString("internalTeleport")
                        && m.methodType().equalsString("(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V")).findFirst().orElseThrow();
                    String first = null;
                    for (var e : teleport.code().orElseThrow()) if (e instanceof InvokeInstruction call) { first = call.name().stringValue(); break; }
                    if (!"resetAscent".equals(first)) throw new AssertionError("Teleport must invalidate ascent provenance before native mutation");
                    checks++;
                }
                byte[] corrupt = b.clone(); corrupt[corrupt.length - 1] ^= 1;
                try { NativeMovementAgent.verifyPin(name, corrupt, mode); throw new AssertionError("wrong pin"); }
                catch (IllegalStateException expectedFailure) { checks++; }
                byte[] missing = cf.transformClass(cf.parse(b), (cb, e) -> {
                    if (e instanceof MethodModel m && (name.equals(NativeMovementAgent.ENTITY) && m.methodName().equalsString("setShiftKeyDown")
                        || name.equals(NativeMovementAgent.LIVING) && m.methodName().equalsString("setSprinting")
                        || name.equals(NativeMovementAgent.PLAYER) && m.methodName().equalsString("jumpFromGround")
                        || name.equals(NativeMovementAgent.LISTENER) && m.methodName().equalsString("handleMovePlayer"))) return;
                    cb.with(e);
                });
                try { NativeMovementAgent.transformNative(name, missing, loader); throw new AssertionError("missing native method"); }
                catch (IllegalStateException expectedFailure) { checks++; }
            }
        }
        try { NativeMovementAgent.install(new Object(), (o, id) -> false, NativeMovementContractTest.class.getClassLoader()); throw new AssertionError("missing agent"); }
        catch (IllegalStateException expected) { checks++; }
        if (NativeMovementAgent.jump(new Object()) || NativeMovementAgent.sneak(new Object()) || NativeMovementAgent.sprint(new Object())) throw new AssertionError("non-player"); checks++;
        System.out.println("S4L_NATIVE_CONTRACT_PASS checks=" + checks + " pipelines=default-off,prototype-on nativeGameplay=SEPARATE");
    }
}
