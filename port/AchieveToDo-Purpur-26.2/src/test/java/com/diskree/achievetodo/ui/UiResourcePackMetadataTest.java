package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.tests.TestSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UiResourcePackMetadataTest {

    private UiResourcePackMetadataTest() {
    }

    public static void run() {
        assertFileContains(Path.of("src/main/resource-pack/pack.mcmeta"), "\"pack_format\": 75");
        assertFileContains(Path.of("src/main/resource-pack/assets/achievetodo/font/ui_title.json"), "\"file\": \"achievetodo:font/ui_panel.png\"");
        TestSupport.assertTrue(
            Files.exists(Path.of("src/main/resource-pack/NOTICE.txt")),
            () -> "Resource-pack NOTICE.txt is missing"
        );
    }

    private static void assertFileContains(Path file, String expectedFragment) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            TestSupport.assertTrue(
                content.contains(expectedFragment),
                () -> file + " is missing expected fragment: " + expectedFragment
            );
        } catch (IOException exception) {
            throw new AssertionError("Unable to read resource-pack metadata file " + file, exception);
        }
    }
}
