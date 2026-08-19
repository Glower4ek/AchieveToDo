package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.injection.extension.main.AdvancementProgressExtension;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementTrackerMixin {

    @Inject(
        method = "startProgress",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementProgress;update(Lnet/minecraft/advancements/AdvancementRequirements;)V",
            shift = At.Shift.AFTER
        )
    )
    public void setAdvancementId(
        AdvancementHolder advancement,
        AdvancementProgress progress,
        CallbackInfo ci
    ) {
        if (progress instanceof AdvancementProgressExtension advancementProgressExtension) {
            advancementProgressExtension.achievetodo$setAdvancementId(advancement.id());
        }
    }
}
