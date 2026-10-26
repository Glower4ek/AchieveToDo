package com.diskree.achievetodo.ui.bukkit;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

final class BukkitUiTitleFactory {

    private static final Key TITLE_FONT = Key.key("achievetodo", "ui_title");
    private static final String TITLE_GLYPH = "\uE000";

    Component createTitle(String fallbackTitle, UiPresentationMode presentationMode) {
        if (presentationMode == UiPresentationMode.FALLBACK) {
            return Component.text(fallbackTitle);
        }
        return Component.text(TITLE_GLYPH).font(TITLE_FONT);
    }
}
