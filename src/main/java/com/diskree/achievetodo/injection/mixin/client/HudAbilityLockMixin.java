package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.gui.AbilityLockLines;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public class HudAbilityLockMixin {
    @WrapOperation(method = "extractOverlayMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void achievetodo$drawAbilityLines(GuiGraphicsExtractor graphics, Font font, Component message,
                                            int x, int y, int width, int color, Operation<Void> original) {
        if (!AbilityLockLines.isAbilityLock(message)) { original.call(graphics, font, message, x, y, width, color); return; }
        var lines = AbilityLockLines.split(message);
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lineWidth = font.width(line);
            original.call(graphics, font, line, -lineWidth / 2, y - (lines.size() - 1 - i) * (font.lineHeight + 2), lineWidth, color);
        }
    }
}
