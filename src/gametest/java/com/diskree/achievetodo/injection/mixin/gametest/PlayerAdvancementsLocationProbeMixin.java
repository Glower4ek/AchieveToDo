package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.certification.LocationTriggerProbe;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsLocationProbeMixin {

    @Shadow
    @Final
    private ServerPlayer player;

    @Inject(method = "registerListeners(Lnet/minecraft/advancements/AdvancementHolder;)V", at = @At("RETURN"))
    private void achievetodo$probeLocationListenerRegistration(AdvancementHolder advancement, CallbackInfo ci) {
        if (LocationTriggerProbe.isTrackedAdvancement(advancement.id())) {
            LocationTriggerProbe.get(player.getUUID()).markLocationListenerRegistered(advancement.id());
        }
    }
}
