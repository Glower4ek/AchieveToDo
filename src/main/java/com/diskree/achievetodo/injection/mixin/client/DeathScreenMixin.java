package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.gui.DesignCodePalette;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Shadow
    @Final
    private boolean hardcore;

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
        )
    )
    private @NotNull MutableComponent showObtainedCountInsteadScoreInHardcore(
        String key,
        Object[] args,
        @NotNull Operation<MutableComponent> original
    ) {
        if (hardcore && !AchieveToDoClient.isNotReady()) {
            return Component.translatable("key.advancements")
                .append(": ")
                .append(
                    Component.literal(String.valueOf(AchieveToDoClient.getObtainedAdvancementsCount()))
                        .withStyle(DesignCodePalette.TEXT_COLOR)
                );
        }
        return original.call(key, args);
    }
}
