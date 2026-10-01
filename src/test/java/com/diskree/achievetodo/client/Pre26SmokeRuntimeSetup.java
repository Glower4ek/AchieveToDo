package com.diskree.achievetodo.client;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class Pre26SmokeRuntimeSetup {
    public static void main(String[] args) throws Exception {
        Path runtime = Path.of(args[0]);
        Files.createDirectories(runtime.resolve("world/datapacks"));
        ExternalPackCompatibility.copyForWorld(Path.of("reference/phase_a_preservation/files/final/bacap.zip"), runtime.resolve("world/datapacks/bacap.zip"), ExternalPack.BACAP);
        Path fixture = runtime.resolve("world/datapacks/pre26_smoke");
        Files.createDirectories(fixture.resolve("data/achievetodo-test/advancement"));
        try (var input = net.minecraft.advancements.Advancement.class.getResourceAsStream("/data/minecraft/advancement/story/mine_stone.json")) {
            if (input == null) throw new IllegalStateException("Missing actual Minecraft 26.2 vanilla fixture");
            var definition = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            Files.writeString(fixture.resolve("data/achievetodo-test/advancement/vanilla_task.json"), definition.toString());
        }
        try (var input = net.minecraft.advancements.Advancement.class.getResourceAsStream("/resourcepacks/bacap_override/pack.mcmeta")) {
            if (input == null) throw new IllegalStateException("Missing vanilla pack format");
            var pack = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            Files.writeString(fixture.resolve("pack.mcmeta"), pack.toString());
        }
        Files.writeString(runtime.resolve("server.properties"), "level-name=world\ngamemode=survival\ninitial-enabled-packs=vanilla,achievetodo,fabric-convention-tags-v2,fabric-gametest-api-v1,file/bacap.zip,file/pre26_smoke,achievetodo:bacap_override\ninitial-disabled-packs=\n");
        Files.writeString(runtime.resolve("eula.txt"), "eula=true\n");
    }
}
