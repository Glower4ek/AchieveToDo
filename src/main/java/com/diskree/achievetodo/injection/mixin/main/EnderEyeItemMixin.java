package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEnderEye(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEnderEye(Level world, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_ENDER_EYE)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
