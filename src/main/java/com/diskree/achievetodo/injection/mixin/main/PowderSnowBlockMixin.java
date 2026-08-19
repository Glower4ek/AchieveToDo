package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(
        method = "pickupBlock",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockDrainFluid(
        @Nullable LivingEntity user,
        LevelAccessor worldAccess,
        BlockPos pos,
        BlockState state,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        Player player = user instanceof Player actualPlayer ? actualPlayer : null;
        if (worldAccess instanceof Level world && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
