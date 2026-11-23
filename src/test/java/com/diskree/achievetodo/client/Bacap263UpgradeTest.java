package com.diskree.achievetodo.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 26.2 -> 26.3 upgrade makes every advancement of BACAP 1.21 (the last 26.2-format BACAP, kept as a frozen
 * fixture) parse with the 26.3 advancement codec, and leaves 26.3 JSON unchanged.
 */
public class Bacap263UpgradeTest {
    private static HolderLookup.Provider registries;
    private static final java.util.Set<String> CONTENT_CHANGES = java.util.Set.of(
        "data/blazeandcave/advancement/adventure/all_around_the_world.json",
        // Needs Terralith loot tables, which only exist when the Terralith datapack is loaded.
        "data/blazeandcave/advancement/technical/open_loot_chest.json");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createReloadableLookup(VanillaRegistries.createWorldLookup());
    }

    @Test
    void every26_2BacapAdvancementParsesAfterUpgrade() throws Exception {
        List<String> failures = new ArrayList<>();
        int parsed = 0, contentChanges = 0;
        try (ZipFile zip = new ZipFile(Path.of("src/test/resources/phase_b_certification/bacap_1_21_pinned.zip").toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("data/") || !entry.getName().contains("/advancement/") || !entry.getName().endsWith(".json")) continue;
                String json;
                try (InputStream input = zip.getInputStream(entry)) { json = new String(input.readAllBytes(), StandardCharsets.UTF_8); }
                JsonElement upgraded = Bacap263Upgrade.upgrade(JsonParser.parseString(json));
                var result = Advancement.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), upgraded);
                if (result.error().isPresent()) {
                    // Content change, not syntax: 26.3 replaced map_color filled maps with dedicated map items.
                    if (CONTENT_CHANGES.contains(entry.getName())) contentChanges++;
                    else failures.add(entry.getName() + ": " + result.error().get().message());
                } else parsed++;
            }
        }
        failures.stream().limit(40).forEach(System.out::println);
        System.out.println("BACAP_263_UPGRADE parsed=" + parsed + " contentChanges=" + contentChanges + " failed=" + failures.size());
        assertTrue(parsed > 1000);
        assertEquals(CONTENT_CHANGES.size(), contentChanges);
        assertEquals(0, failures.size(), failures.isEmpty() ? "" : failures.getFirst());
    }

    @Test
    void upgradeIsIdempotentOn26_3Json() {
        String modern = "{\"criteria\":{\"c\":{\"trigger\":\"minecraft:summoned_entity\",\"conditions\":{\"entity\":"
            + "{\"type\":\"minecraft:entity_properties\",\"entity\":\"this\",\"predicate\":{\"entity_type\":\"minecraft:allay\"}}}}}}";
        JsonElement element = JsonParser.parseString(modern);
        assertEquals(element, Bacap263Upgrade.upgrade(element));
        assertEquals(element, Bacap263Upgrade.upgrade(Bacap263Upgrade.upgrade(element)));
    }
}
