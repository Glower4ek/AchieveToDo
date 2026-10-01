package com.diskree.achievetodo.certification;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Reconcile retained proof bytes with exactly the authorized smoke additions. */
final class Pre26SmokeAcceptedSources {
    static String sha(Path root, String path) throws Exception {
        byte[] bytes = Files.readAllBytes(root.resolve(path));
        if (path.equals("src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java")) {
            String text = new String(bytes, StandardCharsets.UTF_8).replace("compat_26_2_r16", "compat_26_2_r15")
                .replace("            convertedLine = LegacyItemText.migrateCommand(convertedLine);\n", "");
            bytes = text.getBytes(StandardCharsets.UTF_8);
            assertEquals("1bea3ba9caf22963d1ef26de69b19534a4b16d1eab85a39c5e1fc17750227a31", Final19StaticContext.sha(bytes));
        } else if (path.equals("src/main/resources/fabric.mod.json")) {
            String text = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(text.contains("\"fabric-api\": \">=${apiVersion}+${minMinecraftVersion}\""));
            bytes = text.replace("\"fabric-api\": \">=${apiVersion}+${minMinecraftVersion}\"\r\n", "\"fabric-api\": \"*\"\n").getBytes(StandardCharsets.UTF_8);
            assertEquals("408c0aa567715089febc57a84500ffcb6d3a8e5f39313654d6980795ad39ca88", Final19StaticContext.sha(bytes));
        } else if (path.equals("src/main/resources/achievetodo.mixins.json")) {
            var current = com.google.gson.JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            var restored = new com.google.gson.JsonArray();
            for (var item : current.getAsJsonArray("mixins")) {
                restored.add(item);
                if (item.getAsString().equals("main.ChunkRegionMixin")) restored.add("main.CommandManagerMixin");
            }
            current.add("mixins", restored);
            assertTrue(current.getAsJsonArray("mixins").remove(new com.google.gson.JsonPrimitive("main.CommandFunctionMixin")));
            assertTrue(current.getAsJsonArray("client").remove(new com.google.gson.JsonPrimitive("client.HudAbilityLockMixin")));
            bytes = Files.readAllBytes(root.resolve("reference/publication/fixtures/historical_0_1_5_achievetodo.mixins.json"));
            assertEquals(com.google.gson.JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)), current);
            assertEquals("56fcb41d75273382681cbeede45882a876de68babcd6580ae63d5c20c0ac211f", Final19StaticContext.sha(bytes));
        }
        return Final19StaticContext.sha(bytes);
    }

    static boolean retainedCopy(Path path) throws Exception {
        try (var zip = new ZipFile(path.toFile())) {
            var entry = zip.getEntry("achievetodo_compatibility/compat_26_2.properties");
            if (entry == null) return false;
            var marker = new Properties();try (var input=zip.getInputStream(entry)){marker.load(input);}
            return "compat_26_2_r15".equals(marker.getProperty("version"))
                && "bacap.zip".equals(marker.getProperty("fileName"))
                && "45b8bb0076bbf5b92fde7dc9590c6686937abbc0".equals(marker.getProperty("sourceSha1"))
                && "51c2bebe15a2225e11f157753edf2d94c8520b67".equals(marker.getProperty("rootOverrideSha1"))
                && "equipment.body".equals(marker.getProperty("llamaCarpetNbtMapping"))
                && "snake_case".equals(marker.getProperty("raiderPredicateKeys"));
        }
    }
}
