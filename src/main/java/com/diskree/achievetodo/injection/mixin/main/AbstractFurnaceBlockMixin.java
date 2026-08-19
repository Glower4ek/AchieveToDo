package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlock.class)
public class AbstractFurnaceBlockMixin {

    @Inject(
        method = "useWithoutItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/AbstractFurnaceBlock;openContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFurnace(
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        AbstractFurnaceBlock abstractFurnaceBlock = (AbstractFurnaceBlock) (Object) this;
        AbilityType abilityType = switch (abstractFurnaceBlock) {
            case FurnaceBlock ignored -> AbilityType.OPEN_FURNACE;
            case SmokerBlock ignored -> AbilityType.OPEN_SMOKER;
            case BlastFurnaceBlock ignored -> AbilityType.OPEN_BLAST_FURNACE;
            default -> null;
        };
        if (AchieveToDoMod.isAbilityLocked(player, abilityType)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
