package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Piglin.class)
public class PiglinEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/Piglin;level()Lnet/minecraft/world/level/Level;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Piglin piglinEntity = (Piglin) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, piglinEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
