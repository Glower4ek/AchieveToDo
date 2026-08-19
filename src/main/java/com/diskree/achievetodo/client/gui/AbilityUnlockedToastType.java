package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.client.AchieveToDoClient;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum AbilityUnlockedToastType {

    ACTION,
    ITEM,
    FOOD,
    TOOL,
    WEAPON,
    EQUIPMENT,
    BLOCK,
    TRADING,
    PORTAL,
    LANDMARK;

    public @NotNull Component getToastTitle() {
        return AchieveToDoClient.translate("ability_unlocked_toast." + getName())
            .append("!");
    }

    private @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
