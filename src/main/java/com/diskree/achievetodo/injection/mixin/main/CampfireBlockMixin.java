package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public class CampfireBlockMixin {

    @Inject(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;placeFood(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockCampfire(
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
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAMPFIRE)
        ) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(
        method = "onProjectileHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockCampfire(
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
