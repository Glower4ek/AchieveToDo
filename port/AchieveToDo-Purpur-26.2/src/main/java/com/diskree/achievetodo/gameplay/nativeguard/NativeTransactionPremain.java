package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.jar.*;

/** Bootstrap visibility for the guard only; never expose plugin/Bukkit classes to bootstrap loading. */
public final class NativeTransactionPremain {
    private static boolean launched;
    public static synchronized void premain(String arguments, Instrumentation instrumentation) throws Exception {
        if (launched) throw new IllegalStateException("ATD_DUPLICATE_AGENT");
        launched = true;
        Path source = Path.of(NativeTransactionPremain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path support = Files.createTempFile("atd-native-2620-", ".jar");
        support.toFile().deleteOnExit();
        int count = 0;
        try (var input = new JarFile(source.toFile()); var output = new JarOutputStream(Files.newOutputStream(support))) {
            var entries = input.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!(entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeAgentIdentity")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeTransactionAgent")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeAction")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeTool")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeComplex")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativePortal")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeMovement")
                    || entry.getName().startsWith("com/diskree/achievetodo/gameplay/nativeguard/NativeLandmark"))
                    || !entry.getName().endsWith(".class")) continue;
                output.putNextEntry(new JarEntry(entry.getName()));
                try (var stream = input.getInputStream(entry)) { stream.transferTo(output); }
                output.closeEntry(); count++;
            }
        }
        if (count < 2) throw new IllegalStateException("Incomplete native guard support JAR");
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(support.toFile()));
        String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeAgentIdentity", true, null)
            .getMethod("register", String.class).invoke(null, digest);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeTransactionAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeActionAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeToolAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeComplexAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativePortalAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeMovementAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeLandmarkSupplementAgent", true, null)
            .getMethod("initialize", Instrumentation.class).invoke(null, instrumentation);
        System.out.println("ATD_NATIVE_BOOTSTRAP classes=" + count);
    }
    private NativeTransactionPremain() {}
}
