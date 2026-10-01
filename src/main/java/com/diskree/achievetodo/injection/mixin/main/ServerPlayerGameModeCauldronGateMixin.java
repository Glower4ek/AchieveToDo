package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeCauldronGateMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void lockCauldronInteraction(
        ServerPlayer player,
        Level level,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        // Vanilla skips the block interaction for deliberate secondary item use.
        if (player.isSecondaryUseActive() &&
            (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
        ) {
            return;
        }
        var pos = hit.getBlockPos();
        var block = level.getBlockState(pos).getBlock();
        if (!(block instanceof AbstractCauldronBlock)) {
            return;
        }
        var interactions = ((AbstractCauldronBlockAccessor) block).achievetodo$getInteractions();
        if (interactions.get(stack) != CauldronInteraction.DEFAULT &&
            (AchieveToDoMod.isTargetInLockedLandmark(player, level, pos) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAULDRON))
        ) {
            // End the entire server interaction before trigger emission or item fallback.
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
