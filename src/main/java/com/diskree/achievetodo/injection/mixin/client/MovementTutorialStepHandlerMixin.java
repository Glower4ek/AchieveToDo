package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.injection.extension.client.MovementTutorialStepHandlerExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.tutorial.MovementTutorialStepInstance;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementTutorialStepInstance.class)
public class MovementTutorialStepHandlerMixin implements MovementTutorialStepHandlerExtension {

    @Unique
    private static final Component OPEN_ADVANCEMENTS_TITLE =
        AchieveToDoClient.translate("tutorial.open_advancements.title");

    @Unique
    private static final Component OPEN_ADVANCEMENTS_DESCRIPTION =
        AchieveToDoClient.translate(
            "tutorial.open_advancements.description",
            Tutorial.key("advancements")
        );

    @Unique
    private boolean isAdvancementsOpened;

    @Unique
    private TutorialToast openAdvancementsToast;

    @Override
    public void achievetodo$onAdvancementsOpened() {
        if (openAdvancementsToast != null) {
            openAdvancementsToast.hide();
            openAdvancementsToast = null;
            isAdvancementsOpened = true;
        }
    }

    @Shadow
    private int moveCompleted;

    @Shadow
    private int lookCompleted;

    @Shadow
    private int timeWaiting;

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/Tutorial;getMinecraft()Lnet/minecraft/client/Minecraft;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void scheduleUntilVisionAbilityUnlocked(CallbackInfo ci) {
        if (AchieveToDoClient.isAbilityLocked(AbilityType.VISION, true)) {
            ci.cancel();
        }
    }

    @ModifyConstant(
        method = "tick",
        constant = @Constant(intValue = 100)
    )
    private int showToastFaster(int constant) {
        return 50;
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/Tutorial;getMinecraft()Lnet/minecraft/client/Minecraft;"
        )
    )
    public Minecraft showOpenAdvancementsToast(
        Tutorial manager,
        @NotNull Operation<Minecraft> original
    ) {
        Minecraft client = original.call(manager);
        if (moveCompleted != -1 &&
            lookCompleted != -1 &&
            timeWaiting - lookCompleted >= 40 &&
            !isAdvancementsOpened &&
            openAdvancementsToast == null
        ) {
            openAdvancementsToast = new TutorialToast(
                client.font,
                TutorialToast.Icons.RECIPE_BOOK,
                OPEN_ADVANCEMENTS_TITLE,
                OPEN_ADVANCEMENTS_DESCRIPTION,
                false
            );
            client.gui.toastManager().addToast(openAdvancementsToast);
        }
        return client;
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/Tutorial;setStep(Lnet/minecraft/client/tutorial/TutorialSteps;)V"
        )
    )
    public void waitOpenAdvancementsCompletion(Tutorial manager, TutorialSteps step, Operation<Void> original) {
        if (isAdvancementsOpened) {
            original.call(manager, step);
        }
    }

    @Inject(
        method = "clear",
        at = @At(value = "HEAD")
    )
    public void hideOpenAdvancementsToast(CallbackInfo ci) {
        if (openAdvancementsToast != null) {
            openAdvancementsToast.hide();
            openAdvancementsToast = null;
        }
    }
}
