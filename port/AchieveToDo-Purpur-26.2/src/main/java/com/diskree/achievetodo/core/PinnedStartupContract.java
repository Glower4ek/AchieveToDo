package com.diskree.achievetodo.core;

import java.nio.file.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import org.bukkit.configuration.file.YamlConfiguration;

/** The certified deployment is one exact runtime and one agent/plugin artifact. */
public final class PinnedStartupContract {
    public static void runtime(int javaFeature, String minecraft, String brand, int build) {
        if (javaFeature != 25 || !"26.2".equals(minecraft) || !"Purpur".equals(brand) || build != 2620)
            throw new IllegalStateException("ATD_UNPINNED_RUNTIME: Java25/MC26.2/Purpur2620 required");
    }

    public static void installation(Path pluginJar) throws Exception {
        singleInstallation(pluginJar);
        // A plugin-local class or a mutable system property cannot stand in for premain.
        Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeAgentIdentity", false, null)
            .getMethod("verify", Path.class).invoke(null, pluginJar);
    }

    static void singleInstallation(Path pluginJar) throws Exception {
        long copies = 0;
        try (var paths = Files.list(pluginJar.toAbsolutePath().getParent())) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                try (var jar = new JarFile(path.toFile())) {
                    var entry = jar.getJarEntry("paper-plugin.yml");
                    if (entry == null) entry = jar.getJarEntry("plugin.yml");
                    if (entry == null) continue;
                    try (var reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                        var meta = new YamlConfiguration(); meta.load(reader);
                        if ("AchieveToDo".equalsIgnoreCase(meta.getString("name"))) copies++;
                    }
                }
            }
        }
        if (copies != 1) throw new IllegalStateException("ATD_DUPLICATE_INSTALLATION: copies=" + copies);
    }

    private PinnedStartupContract() {}
}
