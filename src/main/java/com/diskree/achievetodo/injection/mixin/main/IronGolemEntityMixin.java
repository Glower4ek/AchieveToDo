package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public class IronGolemEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/golem/IronGolem;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockLandmarkTarget(
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local float currentHealth
    ) {
        IronGolem ironGolemEntity = (IronGolem) (Object) this;
        if (currentHealth < ironGolemEntity.getMaxHealth() &&
            AchieveToDoMod.isTargetInLockedLandmark(player, ironGolemEntity)
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
