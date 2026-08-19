package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TripWireBlock.class)
public class TripwireBlockMixin {

    @Inject(
        method = "playerWillDestroy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockShears(
        Level world,
        BlockPos blockPos,
        BlockState blockState,
        Player player,
        CallbackInfoReturnable<BlockState> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockPos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(blockState);
        }
    }
}
