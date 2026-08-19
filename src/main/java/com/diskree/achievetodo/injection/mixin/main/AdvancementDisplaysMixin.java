package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;

@Mixin(AdvancementVisibilityEvaluator.class)
public class AdvancementDisplaysMixin {

    @ModifyConstant(
        method = "evaluateVisiblityForUnfinishedNode(Lit/unimi/dsi/fastutil/Stack;)Z",
        constant = @Constant(intValue = 2)
    )
    private static int forceShowAllAdvancements(
        int displayDepth,
        @Local(argsOnly = true) Stack<?> statuses
    ) {
        return ((ObjectArrayList<?>) statuses).size() - 1;
    }

    @WrapOperation(
        method = "evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Lit/unimi/dsi/fastutil/Stack;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"
        )
    )
    private static boolean wrapTestOperation(
        Predicate<AdvancementNode> donePredicate,
        Object advancement,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) @NotNull AdvancementNode placedAdvancement
    ) {
        if (!placedAdvancement.advancement().isRoot() &&
            AdvancementsTabType.findByAdvancement(placedAdvancement.root()) != AdvancementsTabType.CHALLENGES
        ) {
            DisplayInfo display = placedAdvancement.advancement().display().orElse(null);
            if (display != null && !display.isHidden()) {
                return true;
            }
        }
        return original.call(donePredicate, advancement);
    }
}
