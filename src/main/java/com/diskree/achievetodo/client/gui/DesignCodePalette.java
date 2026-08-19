package com.diskree.achievetodo.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;

import java.util.Locale;

public class DesignCodePalette {

    public static final ChatFormatting TEXT_COLOR = ChatFormatting.YELLOW;

    public static final String TEXT_COLOR_NAME = TEXT_COLOR.name().toLowerCase(Locale.ROOT);

    public static final int IN_WORLD_RGB = DyeColor.YELLOW.getTextureDiffuseColor();
}

