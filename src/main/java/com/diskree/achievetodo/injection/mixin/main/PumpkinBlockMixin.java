package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin {

    @Inject(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockShears(
        ItemStack stack,
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
