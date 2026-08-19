package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private static final VisionOverlay VISION_OVERLAY = new VisionOverlay();

    @Unique
    private float blackOverlayAlpha = 0.0f;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void tryTakeScreenshotIfNeeded();

    @Inject(
        method = "tick",
        at = @At("TAIL")
    )
    private void updateVisionOverlay(CallbackInfo ci) {
        if (AchieveToDoClient.isAbilityLocked(AbilityType.VISION)) {
            blackOverlayAlpha = 1.0f;
        } else if (blackOverlayAlpha > 0.0f) {
            blackOverlayAlpha = Math.max(0.0f, blackOverlayAlpha - 0.02f);
            if (blackOverlayAlpha == 0.0f) {
                tryTakeScreenshotIfNeeded();
            }
        }

        if (blackOverlayAlpha > 0.0f) {
            VISION_OVERLAY.setAlpha(blackOverlayAlpha);
            if (minecraft.gui.overlay() == null || minecraft.gui.overlay() == VISION_OVERLAY) {
                minecraft.gui.setOverlay(VISION_OVERLAY);
            }
        } else if (minecraft.gui.overlay() == VISION_OVERLAY) {
            minecraft.gui.setOverlay(null);
        }
    }

    @Inject(
        method = "tryTakeScreenshotIfNeeded()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void scheduleWorldIconUpdateUntilVisionAbilityUnlocked(CallbackInfo ci) {
        if (blackOverlayAlpha != 0.0f) {
            ci.cancel();
        }
    }

    @Unique
    private static final class VisionOverlay extends Overlay {

        private float alpha;

        private void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        @Override
        public boolean isPausing() {
            return false;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int alphaChannel = (int) (alpha * 255.0f) << 24;
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), alphaChannel);
        }
    }
}
