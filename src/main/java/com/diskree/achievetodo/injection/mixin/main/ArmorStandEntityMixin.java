package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public class ArmorStandEntityMixin {

    @Inject(
        method = "swapItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        cancellable = true
    )
    public void lockArmorStandEquip(
        Player player,
        EquipmentSlot slot,
        ItemStack stack,
        InteractionHand hand,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ArmorStand armorStandEntity = (ArmorStand) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, armorStandEntity)) {
            cir.setReturnValue(false);
        }
    }
}
