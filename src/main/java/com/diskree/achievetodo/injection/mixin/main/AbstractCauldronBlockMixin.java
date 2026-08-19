package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlockMixin {

    @Shadow
    @Final
    protected CauldronInteraction.Dispatcher interactions;

    @WrapOperation(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/cauldron/CauldronInteraction;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    public InteractionResult lockCauldron(
        CauldronInteraction behavior,
        BlockState blockState,
        Level world,
        BlockPos blockPos,
        Player player,
        InteractionHand hand,
        ItemStack stack,
        Operation<InteractionResult> original
    ) {
        if (behavior != CauldronInteraction.DEFAULT) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockPos) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAULDRON)
            ) {
                return InteractionResult.SUCCESS;
            }
        }
        return original.call(behavior, blockState, world, blockPos, player, hand, stack);
    }
}
