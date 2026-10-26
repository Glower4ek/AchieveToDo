package com.diskree.achievetodo.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;

public final class UiTextServiceTest {

    public static void run() {
        parsesFlatJsonEscapes();
        localeFallbackUsesEnglish();
    }

    private static void parsesFlatJsonEscapes() {
        Map<String, String> parsed = UiTextService.parseFlatJson("""
            {
              "a": "hello",
              "b": "line\\nnext",
              "c": "quote \\\"x\\\""
            }
            """);
        assertEquals("hello", parsed.get("a"));
        assertEquals("line\nnext", parsed.get("b"));
        assertEquals("quote \"x\"", parsed.get("c"));
    }

    private static void localeFallbackUsesEnglish() {
        Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
        catalogs.put("en_us", Map.of("key", "Hello"));
        catalogs.put("ru_ru", Map.of("key", "Privet"));
        UiTextService service = new UiTextService(catalogs);
        assertEquals("Privet", service.resolve("ru_ru", "key"));
        assertEquals("Hello", service.resolve("de_de", "key"));
    }
}
