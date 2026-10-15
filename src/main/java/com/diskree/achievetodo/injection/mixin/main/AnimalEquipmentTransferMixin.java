package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.injection.extension.main.MountInventoryMenuExtension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class AnimalEquipmentTransferMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void lockMountClick(int index, int button, ContainerInput input, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (menu instanceof MountInventoryMenuExtension extension && menu.isValidSlotIndex(index) &&
            AchieveToDoMod.isTargetInLockedLandmark(player, extension.achievetodo$getMount())) {
            menu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
