package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.client.AchieveToDoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum AdvancementsTabType {

    ABILITIES,
    BACAP,
    STATISTICS,

    BUILDING,
    FARMING,
    HUSBANDRY,
    BIOMES,
    ADVENTURE,
    MONSTERS,
    WEAPONRY,

    MINING,
    REDSTONE,
    ENCHANTING,
    NETHER,
    POTION,
    END,
    CHALLENGES;

    public @NotNull Identifier getMystifiedTabId() {
        return AchieveToDoMod.getIdentifier("locked_tab_" + getName() + "/root");
    }

    public @NotNull Component getMystifiedTabTooltipText() {
        return AchieveToDoClient.translate("advancements_tab_mystified_tooltip." + getName())
            .withStyle(ChatFormatting.ITALIC)
            .withStyle(ChatFormatting.GRAY);
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Background texture of a tab. 26.2 drew a tab whose display has no background with the intentional missing
     * texture ({@code minecraft:}); 26.3's AdvancementTab takes the texture path directly, so the fallback lives here.
     */
    public static @NotNull Identifier backgroundOf(@NotNull DisplayInfo display) {
        return display.background()
            .map(ClientAsset.ResourceTexture::texturePath)
            .orElse(Identifier.withDefaultNamespace(""));
    }

    public static @Nullable AdvancementsTabType findByAdvancement(@NotNull AdvancementNode advancement) {
        return findByAdvancement(advancement.holder().id());
    }

    public static @Nullable AdvancementsTabType findByAdvancement(@NotNull Identifier advancementId) {
        if (!advancementId.getNamespace().equals("achievetodo")
            && !advancementId.getNamespace().equals("blazeandcave")
            && !advancementId.getNamespace().equals("minecraft")) {
            return null;
        }
        String[] pathSlices = advancementId.getPath().split("/");
        if (pathSlices.length == 2) {
            for (AdvancementsTabType tabType : values()) {
                if (tabType.name().equalsIgnoreCase(pathSlices[0])) {
                    return tabType;
                }
            }
        }
        return null;
    }

    @Environment(EnvType.CLIENT)
    public AdvancementTabType getPosition() {
        if (this == ABILITIES || this == BACAP || this == STATISTICS) {
            return AdvancementTabType.LEFT;
        }
        if (this == BUILDING ||
            this == FARMING ||
            this == HUSBANDRY ||
            this == BIOMES ||
            this == ADVENTURE ||
            this == MONSTERS ||
            this == WEAPONRY
        ) {
            return AdvancementTabType.ABOVE;
        }
        return AdvancementTabType.BELOW;
    }

    @Environment(EnvType.CLIENT)
    public int getOrder() {
        int order = 0;
        AdvancementTabType position = getPosition();
        for (AdvancementsTabType tab : AdvancementsTabType.values()) {
            if (tab.getPosition() == position) {
                if (tab == this) {
                    break;
                }
                order++;
            }
        }
        return order;
    }
}
