package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HappyGhast.class)
public class HappyGhastEntityMixin {
    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;doPlayerRide(Lnet/minecraft/world/entity/player/Player;)V"), cancellable = true)
    private void lockHappyGhastRide(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        HappyGhast target = (HappyGhast) (Object) this;
        if (target.getPassengers().size() < 4 && AchieveToDoMod.isTargetInLockedLandmark(player, target)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
