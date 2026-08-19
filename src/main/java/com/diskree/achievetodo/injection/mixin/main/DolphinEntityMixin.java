package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dolphin.class)
public class DolphinEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Dolphin dolphinEntity = (Dolphin) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, dolphinEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
