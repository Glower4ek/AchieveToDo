package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockFishingRod(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FISHING_ROD)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
