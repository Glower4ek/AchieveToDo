package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SulfurCube.class)
public class SulfurCubeEntityMixin {
    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    private void lockBabyFeeding(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, (SulfurCube) (Object) this)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"), cancellable = true)
    private void lockBodyReplacement(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        SulfurCube cube = (SulfurCube) (Object) this;
        // Adult/unprimed/tag checks precede this site. equipItem's remaining native
        // rejection is the same existing BODY item, before any drop or replacement.
        var body = cube.getItemBySlot(EquipmentSlot.BODY);
        if (!body.isEmpty() && player.getItemInHand(hand).is(body.getItem())) {
            return;
        }
        if (AchieveToDoMod.isTargetInLockedLandmark(player, cube)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    private void lockManualShearing(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS) ||
            AchieveToDoMod.isTargetInLockedLandmark(player, (SulfurCube) (Object) this)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;primeTime(Z)Z"), cancellable = true)
    private void lockManualIgnition(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Native adult/unprimed/canExplode/item/gamerule checks have all passed.
        if ((player.getItemInHand(hand).is(Items.FLINT_AND_STEEL) &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL)) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.IGNITE_TNT) ||
            AchieveToDoMod.isTargetInLockedLandmark(player, (SulfurCube) (Object) this)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
