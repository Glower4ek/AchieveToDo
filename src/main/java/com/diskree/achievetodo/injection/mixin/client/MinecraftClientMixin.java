package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.injection.extension.client.CreateWorldScreenExtension;
import com.diskree.achievetodo.injection.extension.client.MovementTutorialStepHandlerExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public @Nullable LocalPlayer player;

    @Inject(
        method = "setScreenAndShow",
        at = @At("HEAD"),
        cancellable = true
    )
    public void setScreenInject(Screen screen, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (screen instanceof AdvancementsScreen) {
            if (AchieveToDoClient.isNotReady()) {
                if (player != null) {
                    player.sendOverlayMessage(
                        AchieveToDoClient.translate("error.not_ready_yet")
                            .withStyle(ChatFormatting.RED)
                    );
                }
                ci.cancel();
            } else if (client.getTutorial().instance instanceof MovementTutorialStepHandlerExtension movementTutorialStepHandlerExtension) {
                movementTutorialStepHandlerExtension.achievetodo$onAdvancementsOpened();
            }
        }
    }

    @WrapOperation(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z",
            ordinal = 4
        )
    )
    public boolean lockInventory(KeyMapping keyBinding, @NotNull Operation<Boolean> original) {
        return original.call(keyBinding) && !AchieveToDoClient.isAbilityLocked(AbilityType.OPEN_INVENTORY);
    }
}
