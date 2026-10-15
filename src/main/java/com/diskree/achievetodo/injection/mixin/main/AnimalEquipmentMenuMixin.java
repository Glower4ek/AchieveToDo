package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.injection.extension.main.MountInventoryMenuExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMountInventoryMenu.class)
public abstract class AnimalEquipmentMenuMixin implements MountInventoryMenuExtension {
    @Shadow @Final protected LivingEntity mount;

    @Override public LivingEntity achievetodo$getMount() {
        return mount;
    }

    @ModifyReturnValue(method = "stillValid", at = @At("RETURN"))
    private boolean lockMountMenuValidity(boolean original, @Local(argsOnly = true) Player player) {
        if (!original) {
            return false;
        }
        var server = AchieveToDoMod.getServer();
        DimensionType dimension = DimensionType.findByWorld(mount.level().dimension());
        if (player instanceof ServerPlayer actor && server != null && dimension != null) {
            return !server.isTargetInLockedLandmark(actor, dimension, mount.getBoundingBox(), true);
        }
        return !AchieveToDoMod.isTargetInLockedLandmark(player, mount);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void lockMountQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractMountInventoryMenu menu = (AbstractMountInventoryMenu) (Object) this;
        if (index >= 0 && index < menu.slots.size() && menu.getSlot(index).hasItem() &&
            AchieveToDoMod.isTargetInLockedLandmark(player, mount)) {
            menu.sendAllDataToRemote();
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
