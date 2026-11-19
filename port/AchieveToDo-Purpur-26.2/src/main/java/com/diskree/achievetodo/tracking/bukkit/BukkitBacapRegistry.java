package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.datapack.BacapPacks;
import com.diskree.achievetodo.datapack.EmbeddedDatapack;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/** Validates loaded definitions; never awards criteria or invokes BACAP functions. */
public final class BukkitBacapRegistry {
    private BukkitBacapRegistry() {}

    static void validate(JavaPlugin plugin) throws IOException {
        if (!BacapPacks.discoveryReady()) throw new IOException("BACAP discovery is unavailable");
        if (!java.nio.file.Files.isSameFile(BacapPacks.directory(), plugin.getServer().getLevelDirectory().resolve("datapacks"))) {
            throw new IOException("BACAP_DIRECTORY_MISMATCH: configured content must be installed in this server level's datapacks directory");
        }
        var packs = BacapPacks.validate(BacapPacks.directory());
        var enabled = plugin.getServer().getDatapackManager().getEnabledPacks().stream().map(pack -> pack.getName()).toList();
        BacapPacks.validateEnabledPackSelection(enabled);
        int definitions=validateDefinitions(packs,key -> plugin.getServer().getAdvancement(NamespacedKey.fromString(key)));
        plugin.getLogger().info("BACAP_ADVANCEMENT_DATA_READY definitions=" + definitions + " enabled=" + enabled);
    }

    /** Validates the staged manager, before it can replace current player advancement definitions. */
    public static void validateCandidateAdvancements(Object manager, java.util.Map<String,java.util.Map<String,byte[]>> packs) throws IOException {
        try {
            var actual=new java.util.HashMap<String,org.bukkit.advancement.Advancement>();
            var holders=(java.util.Collection<?>)manager.getClass().getMethod("getAllAdvancements").invoke(manager);
            for(Object holder:holders) {
                var view=(org.bukkit.advancement.Advancement)holder.getClass().getMethod("toBukkit").invoke(holder);
                if(actual.put(view.getKey().toString(),view)!=null)throw new IOException("Duplicate staged advancement ID");
            }
            validateDefinitions(packs,actual::get);
        } catch(ReflectiveOperationException failure) {throw new IOException("Pinned staged advancement contract unavailable",failure);}
    }
    private static int validateDefinitions(java.util.Map<String,java.util.Map<String,byte[]>> packs,
            java.util.function.Function<String,org.bukkit.advancement.Advancement> resolve) throws IOException {
        var definitions = new java.util.LinkedHashMap<>(BacapPacks.advancementDefinitions(packs));
        EmbeddedDatapack.renderResources().forEach((name, json) -> {
            if (name.contains("/advancement/") && name.endsWith(".json")) {
                String key = "achievetodo:" + name.substring(name.indexOf("/advancement/") + 13, name.length() - 5);
                definitions.put(key, JsonParser.parseString(json).getAsJsonObject());
            }
        });
        for (var entry : definitions.entrySet()) {
            var advancement = resolve.apply(entry.getKey());
            JsonObject definition = entry.getValue();
            if (advancement == null || !new HashSet<>(advancement.getCriteria()).equals(definition.getAsJsonObject("criteria").keySet())) {
                throw new IOException("BACAP_ADVANCEMENT_DATA_MISSING_OR_INCOMPATIBLE: " + entry.getKey());
            }
            Set<Set<String>> expected = new HashSet<>();
            if (definition.has("requirements")) {
                for (var group : definition.getAsJsonArray("requirements")) {
                    Set<String> names = new HashSet<>();
                    group.getAsJsonArray().forEach(value -> names.add(value.getAsString()));
                    expected.add(names);
                }
            } else definition.getAsJsonObject("criteria").keySet().forEach(name -> expected.add(Set.of(name)));
            Set<Set<String>> actual = new HashSet<>();
            advancement.getRequirements().getRequirements().forEach(group -> actual.add(Set.copyOf(group.getRequiredCriteria())));
            if (!expected.equals(actual)) throw new IOException("BACAP_REQUIREMENTS_MISMATCH: " + entry.getKey());
        }
        return definitions.size();
    }
}
