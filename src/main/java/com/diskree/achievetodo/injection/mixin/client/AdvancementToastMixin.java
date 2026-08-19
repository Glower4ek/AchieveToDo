package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Unique
    private static final Identifier ABILITY_UNLOCKED_NOTIFICATION_BACKGROUND_TEXTURE =
        AchieveToDoMod.getIdentifier("ability_unlocked_notification_background");

    @Unique
    private static final int ABILITY_UNLOCKED_NOTIFICATION_TITLE_COLOR = CommonColors.BLACK;

    @Unique
    private static final int ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR = 0x725e3c;

    @Unique
    private @Nullable AbilityType abilityType;

    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    private void findAbility(AdvancementHolder advancement, CallbackInfo ci) {
        abilityType = AbilityType.findByAdvancement(advancement);
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            ordinal = 0
        ),
        index = 1
    )
    private Identifier setCustomBackgroundTextureForAbilityUnlockedNotification(Identifier original) {
        return abilityType != null ? ABILITY_UNLOCKED_NOTIFICATION_BACKGROUND_TEXTURE : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 0
        ),
        index = 1
    )
    private Component setCustomTitleForAbilityUnlockedNotificationInSingleLineMode(Component original) {
        return abilityType != null ? abilityType.getUnlockToastType().getToastTitle() : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 1
        ),
        index = 1
    )
    private Component setCustomTitleForAbilityUnlockedNotificationInMultiLineMode(Component original) {
        return abilityType != null ? abilityType.getUnlockToastType().getToastTitle() : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 0
        ),
        index = 4
    )
    private int setCustomTitleColorForAbilityUnlockedNotificationInSingleLineMode(int original) {
        return abilityType != null
            ? (original & 0xFF000000) | ABILITY_UNLOCKED_NOTIFICATION_TITLE_COLOR
            : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 1
        ),
        index = 4
    )
    private int setCustomTitleColorForAbilityUnlockedNotificationInMultiLineMode(int original) {
        return abilityType != null
            ? (original & 0xFF000000) | ABILITY_UNLOCKED_NOTIFICATION_TITLE_COLOR
            : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 0
        ),
        index = 4
    )
    private int setCustomSubtitleColorForAbilityUnlockedNotificationInSingleLineMode(int original) {
        return abilityType != null
            ? (original & 0xFF000000) | ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR
            : original;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            ordinal = 1
        ),
        index = 4
    )
    private int setCustomSubtitleColorForAbilityUnlockedNotificationInMultiLineMode(int original) {
        return abilityType != null
            ? (original & 0xFF000000) | ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR
            : original;
    }

    @Inject(
        method = "getSoundEvent",
        at = @At("HEAD"),
        cancellable = true
    )
    private void playAbilityUnlockedSound(CallbackInfoReturnable<SoundEvent> cir) {
        if (abilityType != null) {
            cir.setReturnValue(SoundEvents.PLAYER_LEVELUP);
        }
    }
}
