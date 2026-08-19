package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;useExperimentalMovement(Lnet/minecraft/world/level/Level;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockMinecartPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
