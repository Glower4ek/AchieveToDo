package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockBowShoot(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.SHOOT_BOW)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
