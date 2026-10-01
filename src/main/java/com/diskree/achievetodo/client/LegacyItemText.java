package com.diskree.achievetodo.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import java.util.regex.Pattern;

/** Migrates legacy JSON-in-SNBT item text at component assignments only. */
public final class LegacyItemText {
    private static final Pattern ASSIGNMENT = Pattern.compile("(?<=[\\[,])(?:minecraft:)?(custom_name|item_name|lore)=");
    private LegacyItemText() {}

    public static String migrateCommand(String command) {
        if (command.stripLeading().startsWith("#")) return command;
        var matcher = ASSIGNMENT.matcher(command);
        var result = new StringBuilder();
        int copied = 0;
        while (matcher.find()) {
            if (!isItemComponentAssignment(command, matcher.start())) continue;
            int start = matcher.end();
            int end = valueEnd(command, start);
            if (end <= start) continue;
            String raw = command.substring(start, end);
            try {
                Tag value = TagParser.create(NbtOps.INSTANCE).parseFully(raw);
                Tag converted = migrate(value, matcher.group(1).equals("lore"));
                if (!converted.equals(value)) {
                    result.append(command, copied, start).append(converted);
                    copied = end;
                }
            } catch (Exception ignored) {
                // Invalid command data remains the command parser's responsibility.
            }
        }
        return copied == 0 ? command : result.append(command, copied, command.length()).toString();
    }

    private static Tag migrate(Tag value, boolean lore) {
        if (lore && value instanceof net.minecraft.nbt.ListTag list) {
            var result = new net.minecraft.nbt.ListTag();
            for (Tag line : list) result.add(migrate(line, false));
            return result;
        }
        if (!(value instanceof StringTag string)) return value;
        try {
            JsonElement component = JsonParser.parseString(string.value());
            for (int depth = 0; depth < 3 && component.isJsonPrimitive() && component.getAsJsonPrimitive().isString(); depth++) {
                String nested = component.getAsString();
                if (!nested.stripLeading().startsWith("{") && !nested.stripLeading().startsWith("[") && !nested.stripLeading().startsWith("\"")) return StringTag.valueOf(nested);
                component = JsonParser.parseString(nested);
            }
            if (!isComponent(component)) return value;
            return JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, component);
        } catch (RuntimeException ignored) { return value; }
    }

    private static boolean isComponent(JsonElement element) {
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            return object.has("text") || object.has("translate") || object.has("selector") || object.has("score") || object.has("nbt") || object.has("keybind");
        }
        return element.isJsonArray() && !element.getAsJsonArray().isEmpty()
            && element.getAsJsonArray().asList().stream().anyMatch(LegacyItemText::isComponent);
    }

    private static boolean isItemComponentAssignment(String text, int end) {
        char quote = 0; boolean escaped = false; int square = 0, compound = 0;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == '[') square++;
            else if (c == ']') square--;
            else if (c == '{') compound++;
            else if (c == '}') compound--;
        }
        return quote == 0 && square == 1 && compound == 0;
    }

    private static int valueEnd(String text, int start) {
        char quote = 0; boolean escaped = false; int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') { if (depth == 0) return i; depth--; }
            else if (c == ',' && depth == 0) return i;
        }
        return text.length();
    }
}
