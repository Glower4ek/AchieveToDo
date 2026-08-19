package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public class FireChargeItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/FireChargeItem;playSound(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFireCharge(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
