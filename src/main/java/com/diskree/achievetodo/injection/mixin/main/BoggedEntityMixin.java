package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bogged.class)
public class BoggedEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/skeleton/Bogged;level()Lnet/minecraft/world/level/Level;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockShears(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Bogged boggedEntity = (Bogged) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, boggedEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
