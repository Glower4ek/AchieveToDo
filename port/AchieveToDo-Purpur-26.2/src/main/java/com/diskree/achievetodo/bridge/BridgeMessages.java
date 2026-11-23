package com.diskree.achievetodo.bridge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * What a player sees when the bridge refuses or ends a session. Players without the client mod (or with an older
 * one) have no AchieveToDo translations, so those messages are plain text in English and Russian. Everything else is
 * a translation key of the client mod with an English fallback; the internal code stays visible for bug reports.
 */
public final class BridgeMessages {
    public static final String DOWNLOAD = "https://github.com/Glower4ek/AchieveToDo/releases";

    public static Component clientRequired(String version) {
        return Component.text("This server needs the AchieveToDo " + version + " client mod (Fabric).\n"
            + "Для игры на этом сервере нужен мод AchieveToDo " + version + " (Fabric).\n\n" + DOWNLOAD);
    }

    public static Component clientOutdated(String version, String clientVersion) {
        String yours = clientVersion == null ? "?" : clientVersion;
        return Component.text("This server needs AchieveToDo " + version + ", you have " + yours + ".\n"
            + "Нужен мод AchieveToDo " + version + ", у вас " + yours + ".\n\n" + DOWNLOAD);
    }

    public static Component serverStarting() {
        return Component.text("The server is still starting. Try again in a minute.\n"
            + "Сервер ещё запускается. Попробуйте через минуту.");
    }

    /** A session error of a player who has the right client mod. */
    public static Component syncError(String code) {
        return Component.translatable("achievetodo.disconnect.sync_error",
                "AchieveToDo lost sync with the server. Please reconnect.")
            .append(Component.text("\n\n" + code, NamedTextColor.GRAY));
    }

    private BridgeMessages() {}
}
