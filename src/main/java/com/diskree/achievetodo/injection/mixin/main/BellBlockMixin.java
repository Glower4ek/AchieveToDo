package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BellBlock.class)
public class BellBlockMixin {

    @Inject(
        method = "attemptToRing(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BellBlockEntity;onHit(Lnet/minecraft/core/Direction;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockRing(
        Entity ringer,
        Level world,
        BlockPos pos,
        Direction direction,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (ringer instanceof Player player && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(false);
        }
    }
}
