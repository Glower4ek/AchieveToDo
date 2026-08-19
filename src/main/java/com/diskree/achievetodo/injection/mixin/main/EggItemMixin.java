package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.level.Level;
import com.diskree.achievetodo.AchieveToDoMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EggItem.class)
public class EggItemMixin {

    @Inject(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
        ),
        cancellable = true
    )
    public void lockEgg(
        Level world,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.THROW_EGG)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
