package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockBrush(
        @NotNull UseOnContext context,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local Player player
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH)
        ) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(
        method = "onUseTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BrushItem;getUseDuration(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)I",
            shift = At.Shift.BEFORE
        )
    )
    public void lockBrush(
        Level world,
        LivingEntity user,
        ItemStack stack,
        int remainingUseTicks,
        CallbackInfo ci,
        @Local Player player,
        @Local @NotNull BlockHitResult blockHitResult
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockHitResult.getBlockPos()) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH)
        ) {
            user.releaseUsingItem();
        }
    }
}
