package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;

/** Last opt-in transformer. Uses the existing landmark callback and its retirement lifecycle. */
public final class NativeLandmarkSupplementAgent {
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String,String> failures = new HashMap<>();
    private static boolean started;

    public static synchronized void initialize(Instrumentation instrumentation) {
        if (!Boolean.getBoolean("achievetodo.landmarks.exactPrototype")) return;
        if (started) throw new IllegalStateException("S4N supplemental transformer already initialized");
        started = true;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!NativeLandmarkSupplementSpecs.INPUTS.containsKey(name)) return null;
                try {
                    verifyPin(name,bytes);
                    byte[] output = NativeLandmarkActions.transform(name,bytes,loader,NativeLandmarkSupplementSpecs.SPECS);
                    synchronized (NativeLandmarkSupplementAgent.class) { transformed.add(name); }
                    // Purpur redirects System.out before Bukkit.server is assigned. Keep receipts in
                    // memory until startup verification; logging here can itself abort an early transform.
                    return output;
                } catch (Throwable failure) {
                    var trace = new java.io.StringWriter();
                    failure.printStackTrace(new java.io.PrintWriter(trace));
                    synchronized (NativeLandmarkSupplementAgent.class) { failures.put(name,trace.toString()); }
                    return null;
                }
            }
        });
    }

    static void verifyPin(String name, byte[] bytes) throws NoSuchAlgorithmException {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(NativeLandmarkSupplementSpecs.INPUTS.get(name)))
            throw new IllegalStateException("Unpinned S4N landmark input " + name + ": " + actual);
    }

    static synchronized void verifyInstalled(ClassLoader loader) throws ClassNotFoundException {
        if (!started || NativeLandmarkSupplementAgent.class.getClassLoader() != null
            || Class.forName(NativeLandmarkSupplementAgent.class.getName(),false,loader) != NativeLandmarkSupplementAgent.class)
            throw new IllegalStateException("S4N requires pinned bootstrap supplemental transformer");
        for (String name : NativeLandmarkSupplementSpecs.INPUTS.keySet()) Class.forName(name.replace('/','.'),true,loader);
        if (!failures.isEmpty() || !transformed.equals(NativeLandmarkSupplementSpecs.INPUTS.keySet()))
            throw new IllegalStateException("S4N landmark transforms unavailable: " + failures + "; transformed=" + transformed);
    }

    private NativeLandmarkSupplementAgent() {}
}
