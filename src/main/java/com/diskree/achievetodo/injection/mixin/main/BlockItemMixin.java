package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(
        method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void lockBlockPlace(@NotNull BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
