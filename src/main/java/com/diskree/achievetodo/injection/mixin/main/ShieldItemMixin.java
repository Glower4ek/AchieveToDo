package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ShieldItemMixin {

    @Inject(
        method = "use",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockShield(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if ((Object) this instanceof ShieldItem &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHIELD)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
