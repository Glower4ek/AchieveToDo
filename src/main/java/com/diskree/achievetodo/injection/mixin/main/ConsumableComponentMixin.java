package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Consumable.class)
public class ConsumableComponentMixin {

    @Inject(
        method = "startConsuming",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/Consumable;consumeTicks()I",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFood(
        LivingEntity user,
        ItemStack stack,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (user instanceof Player player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEatFoodAbility(stack))
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
        method = "canConsume",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockOminousBottle(
        LivingEntity user,
        ItemStack stack,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (user instanceof Player player &&
            stack.is(Items.OMINOUS_BOTTLE) &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_OMINOUS_BOTTLE)
        ) {
            cir.setReturnValue(false);
        }
    }
}
