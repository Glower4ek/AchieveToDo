package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import com.diskree.achievetodo.AchieveToDoMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpyglassItem.class)
public class SpyglassItemMixin {

    @Inject(
        method = "use",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockSpyglass(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SPYGLASS)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
