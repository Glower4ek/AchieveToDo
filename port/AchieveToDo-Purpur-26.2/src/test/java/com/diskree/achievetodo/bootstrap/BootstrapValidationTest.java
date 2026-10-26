package com.diskree.achievetodo.bootstrap;

import com.diskree.achievetodo.core.AchieveToDoPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class BootstrapValidationTest {

    private BootstrapValidationTest() {
    }

    public static void run() {
        assertResourceExists("/datapack/pack.mcmeta");
        assertResourceExists("/paper-plugin.yml");
        assertPaperPluginDescriptor();
    }

    private static void assertResourceExists(String path) {
        assertTrue(
            AchieveToDoBootstrap.class.getResource(path) != null,
            () -> "Required bootstrap resource is missing: " + path
        );
    }

    private static void assertPaperPluginDescriptor() {
        String descriptor = readResource("/paper-plugin.yml");
        assertTrue(
            descriptor.contains("main: " + AchieveToDoPlugin.class.getName()),
            () -> "paper-plugin.yml is missing the expected main class"
        );
        assertTrue(
            descriptor.contains("bootstrapper: " + AchieveToDoBootstrap.class.getName()),
            () -> "paper-plugin.yml is missing the expected bootstrapper class"
        );
        assertTrue(
            descriptor.contains("api-version: \"26.2\""),
            () -> "paper-plugin.yml is missing the expected api-version"
        );
    }

    private static String readResource(String path) {
        try (InputStream inputStream = Objects.requireNonNull(
            AchieveToDoBootstrap.class.getResourceAsStream(path),
            "Missing resource " + path
        )) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Failed to read resource " + path, exception);
        }
    }
}
