package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.client.AchieveToDoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ErrorScreen extends AlertScreen {

    public ErrorScreen(Screen parent, String messageKey) {
        super(
            () -> Minecraft.getInstance().setScreenAndShow(parent),
            AchieveToDoClient.translate("error.title")
                .withStyle(ChatFormatting.RED),
            AchieveToDoClient.translate(messageKey)
        );
    }
}
