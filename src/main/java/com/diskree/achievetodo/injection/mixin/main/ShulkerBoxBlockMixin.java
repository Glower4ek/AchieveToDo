package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {

    @WrapOperation(
        method = "useWithoutItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/ShulkerBoxBlock;canOpen(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;)Z"
        )
    )
    private boolean lockShulkerBox(
        BlockState state,
        Level world,
        BlockPos pos,
        @NotNull ShulkerBoxBlockEntity entity,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) Player player
    ) {
        if (!original.call(state, world, pos, entity)) {
            return false;
        }
        if (entity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
            return true;
        }
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_SHULKER_BOX)
        ) {
            return false;
        }
        return true;
    }
}
