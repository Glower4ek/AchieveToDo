package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpeleothemBlock.class)
public class PointedDripstoneBlockMixin {

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockBreak(
        Level world,
        net.minecraft.world.level.block.state.BlockState state,
        BlockHitResult hit,
        @NotNull Projectile projectile,
        CallbackInfo ci
    ) {
        if (!(((Object) this) instanceof PointedDripstoneBlock)) {
            return;
        }
        if (projectile.getOwner() instanceof Player player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos())
        ) {
            ci.cancel();
        }
    }
}
