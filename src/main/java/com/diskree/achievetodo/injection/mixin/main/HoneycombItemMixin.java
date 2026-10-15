package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoneycombItem.class)
public class HoneycombItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void lockManualWax(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        if (HoneycombItem.getWaxed(state).isEmpty()) {
            return;
        }
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            isLinkedTargetLocked(context.getPlayer(), world, pos, state)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Unique
    private static boolean isLinkedTargetLocked(Player player, Level world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState partner = world.getBlockState(other);
            if (partner.getBlock() instanceof DoorBlock && partner.getValue(DoorBlock.HALF) != state.getValue(DoorBlock.HALF) &&
                AchieveToDoMod.isTargetInLockedLandmark(player, world, other)) {
                return true;
            }
        }
        if (state.getBlock() instanceof CopperChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return AchieveToDoMod.isTargetInLockedLandmark(player, world, ChestBlock.getConnectedBlockPos(pos, state));
        }
        return false;
    }
}
