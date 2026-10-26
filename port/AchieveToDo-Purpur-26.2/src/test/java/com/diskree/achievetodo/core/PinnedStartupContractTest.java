package com.diskree.achievetodo.core;

import com.diskree.achievetodo.gameplay.nativeguard.NativeAgentIdentity;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.*;

public final class PinnedStartupContractTest {
    public static void main(String[] args) throws Exception {
        PinnedStartupContract.runtime(25, "26.2", "Purpur", 2620);
        reject(() -> PinnedStartupContract.runtime(21, "26.2", "Purpur", 2620));
        reject(() -> PinnedStartupContract.runtime(26, "26.2", "Purpur", 2620));
        reject(() -> PinnedStartupContract.runtime(25, "26.1", "Purpur", 2620));
        reject(() -> PinnedStartupContract.runtime(25, "26.2", "Paper", 2620));
        reject(() -> PinnedStartupContract.runtime(25, "26.2", "Purpur", 2621));
        Path folder = Files.createTempDirectory("s5-startup-identity");
        Path jar = folder.resolve("product.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("paper-plugin.yml"));
            out.write("name: AchieveToDo\nversion: evidence-only\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.closeEntry();
        }
        PinnedStartupContract.singleInstallation(jar);
        reject(() -> NativeAgentIdentity.verify(jar));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        NativeAgentIdentity.register(digest);
        NativeAgentIdentity.verify(jar);
        reject(() -> NativeAgentIdentity.register(digest));
        Path changed = folder.resolve("changed.bin"); Files.writeString(changed, "different artifact");
        reject(() -> NativeAgentIdentity.verify(changed));
        Files.copy(jar, folder.resolve("duplicate.jar"));
        reject(() -> PinnedStartupContract.singleInstallation(jar));
        System.out.println("S5_PINNED_STARTUP_CONTRACT PASS cases=12; native startup is separate");
    }
    private interface Action { void run() throws Exception; }
    private static void reject(Action action) throws Exception {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("Unsafe startup accepted");
    }
}
