package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCow.class)
public class CowEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractCow cowEntity = (AbstractCow) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, cowEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
