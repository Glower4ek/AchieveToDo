package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.injection.extension.main.AdvancementProgressExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientAdvancements.class)
public class ClientAdvancementManagerMixin {

    @WrapOperation(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementProgress;update(Lnet/minecraft/advancements/AdvancementRequirements;)V"
        )
    )
    public void setAdvancementId(
        AdvancementProgress progress,
        AdvancementRequirements requirements,
        @NotNull Operation<Void> original,
        @Local AdvancementNode placedAdvancement
    ) {
        original.call(progress, requirements);
        if (progress instanceof AdvancementProgressExtension advancementProgressExtension) {
            advancementProgressExtension.achievetodo$setAdvancementId(placedAdvancement.holder().id());
        }
    }
}
