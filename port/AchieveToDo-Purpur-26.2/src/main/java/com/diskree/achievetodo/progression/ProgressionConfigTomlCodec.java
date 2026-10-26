package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;
import java.util.*;

/** Conservative scalar TOML reader: unsupported syntax fails closed instead of dropping statements. */
public final class ProgressionConfigTomlCodec {
    public ParsedProgressionConfig parse(String content) {
        Objects.requireNonNull(content, "content");
        int version = 0;
        String table = "";
        Set<String> tables = new HashSet<>(), keys = new HashSet<>();
        Map<String, Integer> abilities = new LinkedHashMap<>();
        Set<AbilityType> recognized = EnumSet.noneOf(AbilityType.class);
        for (String raw : content.split("\\R")) {
            String line = statement(raw).trim();
            if (line.isEmpty()) continue;
            if (line.matches("\\[[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*\\]")) {
                table = line.substring(1, line.length() - 1);
                if (!tables.add(table) || table.startsWith("abilities.")) throw invalid(line);
                continue;
            }
            int split = line.indexOf('=');
            if (split < 1) throw invalid(line);
            String key = line.substring(0, split).trim();
            if (key.matches("\"[A-Za-z0-9_-]+\"|'[A-Za-z0-9_-]+'")) key = key.substring(1, key.length()-1);
            if (!key.matches("[A-Za-z0-9_-]+") || !keys.add(table + "." + key)) throw invalid(line);
            String value = line.substring(split + 1).trim();
            boolean integer = value.matches("[+-]?(0|[1-9][0-9]*(?:_[0-9]+)*)");
            boolean scalar = integer || value.equals("true") || value.equals("false") ||
                value.matches("\"(?:[^\"\\\\]|\\\\[\"\\\\btnfr])*\"|'[^']*'");
            if (!scalar) throw invalid(line);
            if (table.isEmpty() && key.equals("version")) {
                if (!integer) throw invalid(line);
                version = Integer.parseInt(value.replace("_", ""));
            }
            if (table.equals("abilities")) {
                var ability = AbilityType.findByName(key);
                if (ability.isPresent()) {
                    if (!integer || !recognized.add(ability.get())) throw invalid(line);
                    abilities.put(key, Integer.parseInt(value.replace("_", "")));
                }
            }
        }
        return new ParsedProgressionConfig(version, tables.contains("abilities"), abilities);
    }

    static String statement(String line) {
        char quote = 0;
        boolean escape = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escape) { escape = false; continue; }
            if (quote == '"' && c == '\\') { escape = true; continue; }
            if (quote != 0) { if (c == quote) quote = 0; }
            else if (c == '\'' || c == '"') quote = c;
            else if (c == '#') return line.substring(0, i);
        }
        if (quote != 0 || escape) throw invalid(line);
        return line;
    }
    private static IllegalArgumentException invalid(String line) {
        return new IllegalArgumentException("Invalid or unsupported progression TOML statement: " + line);
    }
    public String write(int version, Map<AbilityType, Integer> requirements) {
        StringBuilder text = new StringBuilder("version = " + version + "\n\n[abilities]\n");
        for (AbilityType ability : AbilityType.values()) {
            text.append(ability.getName()).append(" = ").append(Objects.requireNonNull(requirements.get(ability))).append('\n');
        }
        return text.toString();
    }
}
