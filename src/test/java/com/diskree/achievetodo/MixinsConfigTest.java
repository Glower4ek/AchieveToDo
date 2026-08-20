package com.diskree.achievetodo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinsConfigTest {

    @Test
    void clientMixinsConfigIncludesGuiMixin() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("achievetodo.mixins.json")) {
            assertNotNull(stream, "achievetodo.mixins.json should be present on the test classpath");

            String config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(
                config.contains("\"client.GuiMixin\""),
                "achievetodo.mixins.json should include client.GuiMixin so Create World continuation runs after datapack validation"
            );
        }
    }
}
