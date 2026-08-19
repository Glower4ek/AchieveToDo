package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCandleBlock.class)
public class AbstractCandleBlockMixin {

    @Inject(
        method = "extinguish",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private static void lockExtinguish(
        @Nullable Player player,
        BlockState state,
        LevelAccessor worldAccess,
        BlockPos pos,
        CallbackInfo ci
    ) {
        if (worldAccess instanceof Level world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/AbstractCandleBlock;setLit(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockLit(
        Level world,
        BlockState state,
        BlockHitResult hit,
        @NotNull Projectile projectile,
        CallbackInfo ci
    ) {
        if (projectile.getOwner() instanceof Player player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos())
        ) {
            ci.cancel();
        }
    }
}
