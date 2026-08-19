package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Shadow
    @Final
    private Fluid content;

    @Inject(
        method = "emptyContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/LiquidBlockContainer;placeLiquid(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFillFluid(
        @Nullable LivingEntity user,
        Level world,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Player player = user instanceof Player actualPlayer ? actualPlayer : null;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_WATER_BUCKET)
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "emptyContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEmptying(
        @Nullable LivingEntity user,
        Level world,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Player player = user instanceof Player actualPlayer ? actualPlayer : null;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            content == Fluids.WATER && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_WATER_BUCKET)
        ) {
            cir.setReturnValue(false);
        }
    }
}
