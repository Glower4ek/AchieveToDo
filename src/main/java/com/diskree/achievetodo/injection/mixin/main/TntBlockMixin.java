package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public class TntBlockMixin {

    @Inject(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFlintAndSteel(
        @NotNull ItemStack stack,
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (stack.is(Items.FLINT_AND_STEEL) &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.IGNITE_TNT)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockIgnite(
        Level world,
        BlockState state,
        BlockHitResult hit,
        @NotNull Projectile projectile,
        CallbackInfo ci
    ) {
        if (projectile.getOwner() instanceof Player player) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, world, hit.getBlockPos()) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.IGNITE_TNT)
            ) {
                ci.cancel();
            }
        }
    }
}
