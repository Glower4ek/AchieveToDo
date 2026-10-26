package com.diskree.achievetodo.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class UiTextService {

    private static final String DEFAULT_LOCALE = "en_us";
    private final Map<String, Map<String, String>> catalogs;

    public UiTextService() {
        this(loadCatalogs(UiTextService.class));
    }

    UiTextService(Map<String, Map<String, String>> catalogs) {
        this.catalogs = Map.copyOf(catalogs);
    }

    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return catalogs.containsKey(normalized) ? normalized : DEFAULT_LOCALE;
    }

    public String text(String locale, String key, Object... args) {
        String pattern = resolve(locale, key);
        if (args.length == 0) {
            return pattern;
        }
        return pattern.formatted(args);
    }

    public String resolve(String locale, String key) {
        Objects.requireNonNull(key, "key");
        String normalizedLocale = normalizeLocale(locale);
        Map<String, String> selected = catalogs.getOrDefault(normalizedLocale, Map.of());
        String value = selected.get(key);
        if (value != null) {
            return value;
        }
        return catalogs.getOrDefault(DEFAULT_LOCALE, Map.of()).getOrDefault(key, key);
    }

    private static Map<String, Map<String, String>> loadCatalogs(Class<?> anchor) {
        Map<String, Map<String, String>> loaded = new LinkedHashMap<>();
        for (String locale : new String[]{DEFAULT_LOCALE, "ru_ru"}) {
            Map<String, String> original = readFlatJson(anchor, "/ui/lang/original/" + locale + ".json");
            Map<String, String> overlay = readFlatJson(anchor, "/ui/lang/" + locale + ".json");
            Map<String, String> merged = new LinkedHashMap<>(original);
            merged.putAll(overlay);
            loaded.put(locale, Collections.unmodifiableMap(merged));
        }
        return Collections.unmodifiableMap(loaded);
    }

    private static Map<String, String> readFlatJson(Class<?> anchor, String path) {
        try (InputStream inputStream = anchor.getResourceAsStream(path)) {
            if (inputStream == null) {
                return Map.of();
            }
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return parseFlatJson(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read UI text catalog " + path, exception);
        }
    }

    static Map<String, String> parseFlatJson(String content) {
        Map<String, String> parsed = new LinkedHashMap<>();
        int index = skipWhitespace(content, 0);
        if (index >= content.length() || content.charAt(index) != '{') {
            throw new IllegalArgumentException("Expected JSON object");
        }
        index++;
        while (true) {
            index = skipWhitespace(content, index);
            if (index >= content.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON object");
            }
            if (content.charAt(index) == '}') {
                return Collections.unmodifiableMap(parsed);
            }
            JsonStringResult key = readJsonString(content, index);
            index = skipWhitespace(content, key.nextIndex());
            if (index >= content.length() || content.charAt(index) != ':') {
                throw new IllegalArgumentException("Expected ':' after JSON key");
            }
            JsonStringResult value = readJsonString(content, skipWhitespace(content, index + 1));
            parsed.put(key.value(), value.value());
            index = skipWhitespace(content, value.nextIndex());
            if (index < content.length() && content.charAt(index) == ',') {
                index++;
                continue;
            }
            if (index < content.length() && content.charAt(index) == '}') {
                return Collections.unmodifiableMap(parsed);
            }
            throw new IllegalArgumentException("Expected ',' or '}' in JSON object");
        }
    }

    private static int skipWhitespace(String content, int index) {
        while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
            index++;
        }
        return index;
    }

    private static JsonStringResult readJsonString(String content, int index) {
        if (index >= content.length() || content.charAt(index) != '"') {
            throw new IllegalArgumentException("Expected JSON string");
        }
        StringBuilder value = new StringBuilder();
        int cursor = index + 1;
        while (cursor < content.length()) {
            char current = content.charAt(cursor++);
            if (current == '"') {
                return new JsonStringResult(value.toString(), cursor);
            }
            if (current != '\\') {
                value.append(current);
                continue;
            }
            if (cursor >= content.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON escape");
            }
            char escaped = content.charAt(cursor++);
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (cursor + 4 > content.length()) {
                        throw new IllegalArgumentException("Incomplete unicode escape");
                    }
                    String codePoint = content.substring(cursor, cursor + 4);
                    value.append((char) Integer.parseInt(codePoint, 16));
                    cursor += 4;
                }
                default -> throw new IllegalArgumentException("Unsupported JSON escape: \\" + escaped);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private record JsonStringResult(String value, int nextIndex) {
    }
}
