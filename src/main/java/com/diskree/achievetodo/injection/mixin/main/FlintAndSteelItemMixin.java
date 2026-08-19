package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFlintAndSteel(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.USE_FLINT_AND_STEEL)
        ) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
