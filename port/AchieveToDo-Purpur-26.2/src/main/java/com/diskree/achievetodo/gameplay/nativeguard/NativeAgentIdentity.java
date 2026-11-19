package com.diskree.achievetodo.gameplay.nativeguard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Bootstrap-owned identity of the actual startup agent, independent of plugin classloaders. */
public final class NativeAgentIdentity {
    private static String digest;

    public static synchronized void register(String value) {
        if (digest != null) throw new IllegalStateException("ATD_DUPLICATE_AGENT");
        if (value == null || !value.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("agent digest");
        digest = value;
    }

    public static synchronized void verify(Path pluginJar) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(pluginJar)));
        if (digest == null || !digest.equals(actual))
            throw new IllegalStateException("ATD_AGENT_PLUGIN_JAR_MISMATCH");
    }

    private NativeAgentIdentity() {}
}
