package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CopperGolemStatueBlock.class, WeatheringCopperGolemStatueBlock.class})
public class CopperGolemStatueBlockMixin {
    @Inject(method = "useItemOn", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CopperGolemStatueBlock;updatePose(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/WeatheringCopperGolemStatueBlock;updatePose(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/CopperGolemStatueBlockEntity;removeStatue(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/animal/golem/CopperGolem;")
    }, cancellable = true)
    private void lockStatueInteraction(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        // Native branching has established the exact action, including BE and age eligibility.
        // A repeated getBlockEntity lookup here could recreate a missing BE or remove a stale one.
        boolean reanimate = stack.is(ItemTags.AXES);
        // Creation's placement/carving rules remain on their actual routes; reanimation uses an axe.
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            (reanimate && AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(stack.getItem())))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
