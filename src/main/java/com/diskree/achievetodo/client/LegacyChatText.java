package com.diskree.achievetodo.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Converts old command text metadata without flattening localized components. */
public final class LegacyChatText {
    private LegacyChatText() {}

    public static String migrateCommand(String command) {
        int tellraw = command.indexOf("tellraw ");
        if (tellraw < 0 || command.stripLeading().startsWith("#")) return command;
        int start = command.indexOf(' ', tellraw + 8);
        if (start < 0) return command;
        String raw = command.substring(start + 1);
        try {
            JsonElement component = JsonParser.parseString(raw);
            return migrate(component) ? command.substring(0, start + 1) + component : command;
        } catch (RuntimeException ignored) { return command; }
    }

    private static boolean migrate(JsonElement value) {
        boolean changed = false;
        if (value.isJsonArray()) {
            for (var child : value.getAsJsonArray()) changed |= migrate(child);
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("hoverEvent") && !object.has("hover_event")) {
                JsonObject event = object.remove("hoverEvent").getAsJsonObject();
                if (event.has("contents") && "show_text".equals(event.get("action").getAsString())) event.add("value", event.remove("contents"));
                object.add("hover_event", event); changed = true;
            }
            if (object.has("clickEvent") && !object.has("click_event")) {
                JsonObject event = object.remove("clickEvent").getAsJsonObject();
                if (event.has("value")) {
                    String action = event.get("action").getAsString();
                    String key = switch (action) {
                        case "run_command", "suggest_command" -> "command";
                        case "open_url" -> "url";
                        case "change_page" -> "page";
                        default -> "value";
                    };
                    if (!key.equals("value")) event.add(key, event.remove("value"));
                }
                object.add("click_event", event); changed = true;
            }
            for (var child : object.entrySet()) changed |= migrate(child.getValue());
        }
        return changed;
    }
}
