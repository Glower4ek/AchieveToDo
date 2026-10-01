package com.diskree.achievetodo;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthorIdentityMetadataTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path PASS = ROOT.resolve("reference/phase_a_planning/pre26_2_author_identity_0_1_5_3");

    @Test void canonicalVersionAndAuthorMatchProcessedMetadata() throws Exception {
        var properties = new Properties();
        try (var input = Files.newInputStream(ROOT.resolve("gradle.properties"))) { properties.load(input); }
        assertEquals("Glower4ek", properties.getProperty("author"));
        assertEquals("com.diskree", properties.getProperty("javaNamespace"));
        assertEquals("0.1.5.4", properties.getProperty("modVersion"));
        var metadata = JsonParser.parseString(Files.readString(ROOT.resolve("build/resources/main/fabric.mod.json"))).getAsJsonObject();
        assertEquals("0.1.5.4", metadata.get("version").getAsString());
        assertEquals("[\"Glower4ek\"]", metadata.getAsJsonArray("authors").toString());
        assertEquals("https://github.com/Glower4ek/AchieveToDo", metadata.getAsJsonObject("contact").get("sources").getAsString());
        assertFalse(metadata.has("contributors"));
    }

    @Test void generatedClassRetainsTechnicalNamespace() throws Exception {
        assertEquals("com.diskree.achievetodo.BuildConfig", BuildConfig.class.getName());
        assertEquals("0.1.5.4", BuildConfig.MOD_VERSION);
        assertEquals("AchieveToDo", BuildConfig.MOD_NAME);
        assertFalse(Files.exists(ROOT.resolve("src/main/generated/java/com/Glower4ek")));
    }

    @Test void mitGrantIsByteForBytePreserved() throws Exception {
        String before = Files.readString(PASS.resolve("license_before.txt"));
        String after = Files.readString(ROOT.resolve("LICENSE"));
        assertEquals(before.replace("Copyright (c) 2024 diskree", "Copyright (c) 2026 Glower4ek"), after);
    }

    @Test void everyMixinAndEntrypointClassResolves() throws Exception {
        var metadata = JsonParser.parseString(Files.readString(ROOT.resolve("build/resources/main/fabric.mod.json"))).getAsJsonObject();
        for (var entry : metadata.getAsJsonObject("entrypoints").entrySet()) {
            for (var name : entry.getValue().getAsJsonArray()) {
                String resource = name.getAsString().replace('.', '/') + ".class";
                assertNotNull(getClass().getClassLoader().getResource(resource), resource);
            }
        }
        var mixins = JsonParser.parseString(Files.readString(ROOT.resolve("src/main/resources/achievetodo.mixins.json"))).getAsJsonObject();
        assertEquals("com.diskree.achievetodo.injection.mixin", mixins.get("package").getAsString());
        for (String side : new String[]{"mixins", "client", "server"}) {
            if (!mixins.has(side)) continue;
            for (var name : mixins.getAsJsonArray(side)) {
                String resource = (mixins.get("package").getAsString()+"."+name.getAsString()).replace('.', '/')+".class";
                assertNotNull(getClass().getClassLoader().getResource(resource), resource);
            }
        }
    }
}
