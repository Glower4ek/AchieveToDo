package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.bridge.BridgeClient;
import com.diskree.achievetodo.client.bridge.PurpurMovementInput;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Shadow
    @Final
    protected Minecraft minecraft;

    // Use the same effective input for comparison, packet construction and lastSentInput.
    // Filtering only the packet argument loses held-key relock transitions.
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;"),
        require = 3, allow = 3)
    private Input achievetodo$purpurMovementInput(Input original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // Vehicle shift also carries native dismount input; keep that path untouched.
        boolean purpur = BridgeClient.isPurpurBackend() && !player.isPassenger();
        return PurpurMovementInput.forServer(original, purpur, player.onGround(),
            purpur && AchieveToDoMod.isAbilityLocked(player, AbilityType.SNEAK));
    }

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
