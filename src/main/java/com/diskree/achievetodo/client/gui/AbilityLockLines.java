package com.diskree.achievetodo.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AbilityLockLines {
    private AbilityLockLines() {}

    public static boolean isAbilityLock(Component message) {
        return message.getContents() instanceof TranslatableContents contents
            && contents.getKey().startsWith("achievetodo.ability.")
            && contents.getKey().endsWith(".locked_message");
    }

    public static List<MutableComponent> split(Component message) {
        var lines = new ArrayList<MutableComponent>();
        lines.add(Component.empty());
        message.visit((Style style, String text) -> {
            String[] parts = text.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) lines.add(Component.empty());
                lines.getLast().append(Component.literal(parts[i]).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return lines;
    }
}
