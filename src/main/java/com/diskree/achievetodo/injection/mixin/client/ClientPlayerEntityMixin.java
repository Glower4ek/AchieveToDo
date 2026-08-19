package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @ModifyReturnValue(
        method = "isShiftKeyDown",
        at = @At("RETURN")
    )
    public boolean lockSneaking(boolean original) {
        if (!original) {
            return false;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        return !player.onGround() || !AchieveToDoMod.isAbilityLocked(player, AbilityType.SNEAK);
    }
}
