package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.JarFile;

/** Verifies actual pinned bytecode in both existing agent pipelines, without generating worlds. */
public final class NativeActionContractTest {
    public static void main(String[] args) {
        if (NativeActionAgent.denied(null, null, "USE_SHIELD") || NativeActionAgent.crystal(false, null))
            throw new AssertionError("Canonical no-player/false predicate");
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar == null || nativeJar.isBlank()) {
            System.out.println("S4H_NATIVE_TRANSFORM_CONTRACT NOT_RUN missing ACHIEVETODO_TEST_PURPUR_NATIVE"); return;
        }
        try (var jar = new JarFile(nativeJar)) {
            var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(nativeJar).toUri().toURL());
            try (var files = Files.walk(Path.of(nativeJar).getParent().getParent().getParent().resolve("libraries"))) {
                for (var file : files.filter(p -> p.toString().endsWith(".jar")).toList()) urls.add(file.toUri().toURL());
            }
            int checks = 0;
            try {
                NativeActionAgent.install(new Object(), (actor, id) -> false, NativeActionContractTest.class.getClassLoader());
                throw new AssertionError("Missing bootstrap agent accepted");
            } catch (IllegalStateException expected) { checks++; }
            try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), NativeActionContractTest.class.getClassLoader())) {
                var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
                for (String name : NativeActionSpecs.INPUTS.keySet()) {
                    byte[] raw = jar.getInputStream(jar.getJarEntry(name + ".class")).readAllBytes();
                    byte[] prior = name.endsWith("/Consumable") ? NativeTransactionAgent.transformNative(name, raw) : raw;
                    byte[] landmark = name.equals("net/minecraft/world/entity/player/Player") || NativeLandmarkActionSpecs.HASHES.containsKey(name)
                        ? NativeLandmarkAgent.transformNative(name, prior, loader) : prior;
                    int mode = 0;
                    for (byte[] input : List.of(prior, landmark)) {
                        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
                        if (!actual.equals(NativeActionSpecs.INPUTS.get(name).get(mode++))) throw new AssertionError("Pipeline pin " + name);
                        var output = NativeActionAgent.transformNative(name, input, loader);
                        if (!cf.verify(output).isEmpty()) throw new AssertionError(name + cf.verify(output));
                        checks += 2;
                    }
                    // Removing a selected method must refuse installation instead of silently losing its boundary.
                    var spec = NativeActionSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).findFirst().orElseThrow();
                    byte[] missing = cf.transformClass(cf.parse(prior), (cb, element) -> {
                        if (element instanceof MethodModel m && m.methodName().equalsString(spec.method())
                            && m.methodType().equalsString(spec.descriptor())) return;
                        cb.with(element);
                    });
                    try { NativeActionAgent.transformNative(name, missing, loader); throw new AssertionError("Missing descriptor accepted " + name); }
                    catch (IllegalStateException expected) { checks++; }
                }
            }
            // Exercise the actual startup transformer's pin-refusal path for the new boundary.
            var captured = new java.util.concurrent.atomic.AtomicReference<java.lang.instrument.ClassFileTransformer>();
            var instrumentation = (java.lang.instrument.Instrumentation) java.lang.reflect.Proxy.newProxyInstance(
                NativeActionContractTest.class.getClassLoader(), new Class<?>[]{java.lang.instrument.Instrumentation.class},
                (proxy, method, arguments) -> {
                    if (!method.getName().equals("addTransformer")) throw new AssertionError(method);
                    captured.set((java.lang.instrument.ClassFileTransformer) arguments[0]); return null;
                });
            NativeActionAgent.initialize(instrumentation);
            String spyglass = "net/minecraft/world/item/SpyglassItem";
            byte[] corrupt = jar.getInputStream(jar.getJarEntry(spyglass + ".class")).readAllBytes();
            corrupt[0] ^= 1;
            if (captured.get().transform(null, spyglass, null, null, corrupt) != null)
                throw new AssertionError("Wrong spyglass pin transformed");
            var failures = NativeActionAgent.class.getDeclaredField("failures"); failures.setAccessible(true);
            if (!String.valueOf(((Map<?, ?>) failures.get(null)).get(spyglass)).contains("Unpinned S4H input " + spyglass))
                throw new AssertionError("Wrong spyglass pin was not recorded for installation refusal");
            checks++;
            System.out.println("S4H_NATIVE_TRANSFORM_CONTRACT PASS checks=" + checks + " pipelines=default-off,prototype-on");
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private NativeActionContractTest() {}
}
