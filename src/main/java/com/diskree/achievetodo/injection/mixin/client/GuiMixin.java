package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.injection.extension.client.CreateWorldScreenExtension;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    private Screen screen;

    @Inject(
        method = "setScreen",
        at = @At("HEAD"),
        cancellable = true
    )
    private void achievetodo$resumePendingCreate(Screen nextScreen, CallbackInfo ci) {
        if (!(nextScreen instanceof CreateWorldScreen createWorldScreen)
            || !(nextScreen instanceof CreateWorldScreenExtension createWorldScreenExtension)
            || !createWorldScreenExtension.achievetodo$getCreateContinuationGate().hasPendingUserCreate()) {
            return;
        }

        if (createWorldScreenExtension.achievetodo$getCreateContinuationGate().shouldResumeWhenCreateScreenReopens()
            && (this.screen == createWorldScreen || this.screen instanceof GenericMessageScreen)) {
            createWorldScreen.onCreate();
            ci.cancel();
            return;
        }

        createWorldScreenExtension.achievetodo$getCreateContinuationGate().clear();
        createWorldScreenExtension.achievetodo$setWaitingDatapack(false);
    }
}
