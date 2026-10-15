package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {
    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ShelfBlock;swapSingleItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/ShelfBlockEntity;ILnet/minecraft/world/entity/player/Inventory;)Z"), cancellable = true)
    private void lockSingleTransfer(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit,
                                    CallbackInfoReturnable<InteractionResult> cir,
                                    @Local ShelfBlockEntity shelf, @Local OptionalInt slot) {
        // Native BE, hand, hit slot and server-side branch checks have already passed.
        if (stack.isEmpty() && shelf.getItem(slot.getAsInt()).isEmpty()) {
            return;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_CHEST) ||
            AchieveToDoMod.isTargetInLockedLandmark(player, level, pos)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/ShelfBlock;swapHotbar(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Inventory;)Z"), cancellable = true)
    private void lockPoweredTransfer(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                     Player player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        ShelfBlock shelfBlock = (ShelfBlock) (Object) this;
        List<BlockPos> chain = shelfBlock.getAllBlocksConnectedTo(level, pos);
        List<BlockPos> affected = new ArrayList<>();
        boolean transfersItems = false;
        var inventory = player.getInventory();
        for (int index = 0; index < chain.size(); index++) {
            var blockEntity = level.getBlockEntity(chain.get(index));
            if (blockEntity == null) {
                continue; // Vanilla skips a missing BE without changing chain indices.
            }
            if (!(blockEntity instanceof ShelfBlockEntity shelf)) {
                return; // Preserve the native incompatible-BE error, without ATD feedback.
            }
            for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
                // This is vanilla's slot mapping, including its exact bounds.
                int inventorySlot = 9 - (chain.size() - index) * shelf.getContainerSize() + slot;
                if (inventorySlot >= 0 && inventorySlot <= inventory.getContainerSize()) {
                    if (!affected.contains(chain.get(index))) {
                        affected.add(chain.get(index));
                    }
                    transfersItems |= !inventory.getItem(inventorySlot).isEmpty() || !shelf.getItem(slot).isEmpty();
                }
            }
        }
        if (!transfersItems) {
            return; // Native no-transfer CONSUME: no new permission notification.
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_CHEST)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        for (BlockPos target : affected) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, level, target)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
        }
        // The native resolver is pure and the original swap runs in this same server call.
        // No inventory mutation has occurred anywhere in the chain before this point.
    }
}
