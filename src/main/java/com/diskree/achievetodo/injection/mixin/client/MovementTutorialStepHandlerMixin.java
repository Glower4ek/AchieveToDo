package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.client.AdvancementsTutorialProgress;
import com.diskree.achievetodo.injection.extension.client.MovementTutorialStepHandlerExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.tutorial.MovementTutorialStepInstance;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
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
    private AdvancementsTutorialProgress advancementsProgress;

    @Unique
    private AdvancementsTutorialProgress achievetodo$progress() {
        if (advancementsProgress == null) {
            try {
                advancementsProgress = new AdvancementsTutorialProgress(FabricLoader.getInstance().getConfigDir()
                    .resolve("achievetodo/early-advancements-tutorial-completion"));
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read advancements tutorial completion", exception);
            }
        }
        return advancementsProgress;
    }

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Tutorial tutorial;

    @Override
    public void achievetodo$onAdvancementsOpened() {
        try {
            achievetodo$progress().opened(moveCompleted != -1 && lookCompleted != -1,
                () -> tutorial.setStep(tutorial.isSurvival() ? TutorialSteps.FIND_TREE : TutorialSteps.NONE));
        } catch (IOException exception) {
            AchieveToDoMod.logger.error("Cannot save early advancements tutorial completion", exception);
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
            achievetodo$progress().mayShowToast()
        ) {
            TutorialToast toast = new TutorialToast(
                client.font,
                TutorialToast.Icons.RECIPE_BOOK,
                OPEN_ADVANCEMENTS_TITLE,
                OPEN_ADVANCEMENTS_DESCRIPTION,
                false
            );
            achievetodo$progress().showToast(() -> client.gui.toastManager().addToast(toast), toast::hide);
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
        if (step == TutorialSteps.NONE) {
            original.call(manager, step);
        } else {
            achievetodo$progress().advanceIfReady(true, () -> original.call(manager, step));
        }
    }

    @Inject(
        method = "clear",
        at = @At(value = "HEAD")
    )
    public void hideOpenAdvancementsToast(CallbackInfo ci) {
        if (advancementsProgress != null) {
            advancementsProgress.clear();
        }
    }
}
