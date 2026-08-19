package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/context/UseOnContext;getItemInHand()Lnet/minecraft/world/item/ItemStack;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockShears(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
