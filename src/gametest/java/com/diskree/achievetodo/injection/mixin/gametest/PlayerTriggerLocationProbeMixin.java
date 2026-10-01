package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.certification.LocationTriggerProbe;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTrigger.class)
public class PlayerTriggerLocationProbeMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void achievetodo$probeLocationTrigger(ServerPlayer player, CallbackInfo ci) {
        boolean isLocation = (Object) this == CriteriaTriggers.LOCATION;
        boolean isTick = (Object) this == CriteriaTriggers.TICK;
        LocationTriggerProbe.State state = LocationTriggerProbe.get(player.getUUID());
        state.recordPlayerTriggerType(((Object) this).getClass().getName(), isLocation, isTick);
        if (isTick) {
            state.recordTickTriggerTickCount(player.tickCount);
        }
        if (!isLocation) {
            return;
        }
        state.recordLocationTrigger(player);
    }
}
