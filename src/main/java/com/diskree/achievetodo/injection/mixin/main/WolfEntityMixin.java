package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public class WolfEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;feed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setCollarColor(Lnet/minecraft/world/item/DyeColor;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setItemSlotAndDropWhenKilled(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/TamableAnimal;mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract4(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (player.getItemInHand(hand).is(Items.SHEARS) &&
            (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        )) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract5(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setOrderedToSit(Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract6(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE,
            ordinal = 2
        ),
        cancellable = true
    )
    public void lockInteract7(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolfEntity = (Wolf) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wolfEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
