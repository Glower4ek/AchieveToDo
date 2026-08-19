package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AmethystBlock.class)
public class AmethystBlockMixin {

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockAmethystSound(
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
