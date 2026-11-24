package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Since 26.3 axe stripping/scraping/wax-off, hoe tilling and shovel path flattening are data-driven
 * {@link BlockTransformer}s (the AxeItem, HoeItem and ShovelItem classes are gone). The lock runs once a transform has
 * found its new state and before any side effect (criterion, loot, item damage, block change, sound).
 */
@Mixin(BlockTransformer.class)
public class BlockTransformerMixin {

    @Inject(
        method = "transformBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/context/UseOnContext;getPlayer()Lnet/minecraft/world/entity/player/Player;",
            ordinal = 0,
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockBlockTransform(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(context.getItemInHand().getItem())) ||
            isLinkedTargetLocked(player, world, pos, world.getBlockState(pos))
        ) {
            cir.setReturnValue(InteractionResult.PASS);
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
