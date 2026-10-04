package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.certification.PhaseAGameTestBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** B10 never resets closed Phase A evidence or fabricates a missing native objective. */
@Mixin(PhaseAGameTestBootstrap.class)
public abstract class B10HistoricalBootstrapIsolationMixin {
    @Inject(method = "onInitialize", at = @At("HEAD"), cancellable = true)
    private void b10Only(CallbackInfo ci) {
        if (System.getProperty("achievetodo.b10.mode") != null) ci.cancel();
    }
}
