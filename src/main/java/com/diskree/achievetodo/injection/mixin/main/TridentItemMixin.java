package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V"
        ),
        cancellable = true
    )
    public void lockTrident(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.ATTACK_WITH_TRIDENT)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
