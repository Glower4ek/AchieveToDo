package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.JarFile;

/** Verifies actual pinned bytecode in both existing agent pipelines, without generating worlds. */
public final class NativeComplexContractTest {
    public static void main(String[] args) {
        if (NativeComplexAgent.denied(null, null, "mine") || NativeComplexAgent.restricted(false, null))
            throw new AssertionError("Canonical no-player/false predicate");
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar == null || nativeJar.isBlank()) {
            System.out.println("S4J_NATIVE_TRANSFORM_CONTRACT NOT_RUN missing ACHIEVETODO_TEST_PURPUR_NATIVE"); return;
        }
        try (var jar = new JarFile(nativeJar)) {
            var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(nativeJar).toUri().toURL());
            try (var files = Files.walk(Path.of(nativeJar).getParent().getParent().getParent().resolve("libraries"))) {
                for (var file : files.filter(p -> p.toString().endsWith(".jar")).toList()) urls.add(file.toUri().toURL());
            }
            int checks = 0;
            try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), NativeComplexContractTest.class.getClassLoader())) {
                var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
                for (String name : NativeComplexSpecs.INPUTS.keySet()) {
                    byte[] raw = jar.getInputStream(jar.getJarEntry(name + ".class")).readAllBytes();
                    byte[] prior = name.equals("net/minecraft/world/entity/LivingEntity") ? NativeTransactionAgent.transformNative(name, raw) : raw;
                    byte[] landmark = name.equals("net/minecraft/world/entity/player/Player") || NativeLandmarkActionSpecs.HASHES.containsKey(name)
                        ? NativeLandmarkAgent.transformNative(name, prior, loader) : prior;
                    int mode = 0;
                    for (byte[] input : List.of(prior, landmark)) {
                        if (NativeActionSpecs.INPUTS.containsKey(name)) input = NativeActionAgent.transformNative(name, input, loader);
                        if (NativeToolSpecs.INPUTS.containsKey(name)) input = NativeToolAgent.transformNative(name, input, loader);
                        NativeComplexAgent.verifyPin(name, input, mode++);
                        var output = NativeComplexAgent.transformNative(name, input, loader);
                        if (!cf.verify(output).isEmpty()) throw new AssertionError(name + cf.verify(output));
                        checks += 2;
                    }
                    if (NativeActionSpecs.INPUTS.containsKey(name)) prior = NativeActionAgent.transformNative(name, prior, loader);
                    if (NativeToolSpecs.INPUTS.containsKey(name)) prior = NativeToolAgent.transformNative(name, prior, loader);
                    byte[] pinned = prior;
                    byte[] corrupt = pinned.clone(); corrupt[corrupt.length-1] ^= 1;
                    try { NativeComplexAgent.verifyPin(name, corrupt, 0); throw new AssertionError("Wrong pin accepted"); }
                    catch (IllegalStateException expected) { checks++; }
                    // Removing a selected method must refuse installation instead of silently losing its boundary.
                    for (var spec : NativeComplexSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList()) {
                    byte[] missing = cf.transformClass(cf.parse(pinned), (cb, element) -> {
                        if (element instanceof MethodModel m && m.methodName().equalsString(spec.method())
                            && m.methodType().equalsString(spec.descriptor())) return;
                        cb.with(element);
                    });
                    try { NativeComplexAgent.transformNative(name, missing, loader); throw new AssertionError("Missing descriptor accepted " + name); }
                    catch (IllegalStateException expected) { checks++; }
                    }
                }
            }
            System.out.println("S4J_NATIVE_TRANSFORM_CONTRACT PASS checks=" + checks + " pipelines=default-off,prototype-on");
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private NativeComplexContractTest() {}
}
