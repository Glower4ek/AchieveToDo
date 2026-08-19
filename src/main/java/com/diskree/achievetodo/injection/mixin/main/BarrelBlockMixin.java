package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrelBlock.class)
public class BarrelBlockMixin {

    @Inject(
        method = "useWithoutItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockBarrel(
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_BARREL)
        ) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
