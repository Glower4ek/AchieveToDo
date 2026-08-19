package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;
import com.diskree.achievetodo.tracking.TrackedScoreType;
import com.diskree.achievetodo.tracking.TrackedStatisticsDataType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin {

    @Unique
    private static final Identifier ABILITY_MYSTIFIED_MASK_TEXTURE =
        AchieveToDoMod.getIdentifier("ability_mystified_mask");

    @Unique
    private TrackedScoreType trackedScoreType;

    @Unique
    private TrackedNearbyEntitiesType trackedNearbyEntitiesType;

    @Unique
    private TrackedStatisticsDataType trackedStatisticsDataType;

    @Unique
    private AbilityType abilityType;

    @Unique
    private boolean shouldRenderMystifiedMask() {
        if (progress == null || abilityType == null || !AchieveToDoClient.isAbilityLocked(abilityType, true)) {
            return false;
        }
        int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
            requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
        ) {
            return false;
        }
        CriterionProgress demystifiedCriterionProgress = progress.getCriterion(
            AbilityAdvancementsGenerator.DEMYSTIFIED_CRITERION
        );
        return demystifiedCriterionProgress != null && !demystifiedCriterionProgress.isDone();
    }

    @Shadow
    private @Nullable AdvancementProgress progress;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
            shift = At.Shift.BEFORE
        )
    )
    private void findAbility(
        AdvancementTab tab,
        Minecraft client,
        @NotNull AdvancementNode advancement,
        DisplayInfo display,
        CallbackInfo ci
    ) {
        Identifier advancementId = advancement.holder().id();
        trackedScoreType = TrackedScoreType.findByAdvancement(advancementId);
        if (trackedScoreType == null) {
            trackedNearbyEntitiesType = TrackedNearbyEntitiesType.findByAdvancement(advancementId);
            if (trackedNearbyEntitiesType == null) {
                trackedStatisticsDataType = TrackedStatisticsDataType.findByAdvancement(advancementId);
                if (trackedStatisticsDataType == null) {
                    abilityType = AbilityType.findByAdvancement(advancementId);
                }
            }
        }
    }

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/DisplayInfo;getDescription()Lnet/minecraft/network/chat/Component;"
        )
    )
    private Component appendSpecialFlagInfoToDescription(
        DisplayInfo displayInfo,
        @NotNull Operation<Component> original
    ) {
        MutableComponent originalText = original.call(displayInfo).copy();
        if (abilityType != null) {
            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
            boolean isInitiallyUnlocked = requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG;
            boolean isPermanentlyLocked = requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG;
            if (isInitiallyUnlocked || isPermanentlyLocked) {
                Component specialFlagInfo = AchieveToDoClient
                    .translate(isInitiallyUnlocked ? "ability.initially_unlocked" : "ability.permanently_locked")
                    .withStyle(ChatFormatting.ITALIC)
                    .withStyle(isInitiallyUnlocked ? ChatFormatting.GRAY : ChatFormatting.RED);
                originalText = originalText
                    .append(CommonComponents.NEW_LINE)
                    .append(CommonComponents.NEW_LINE)
                    .append(specialFlagInfo);
            }
        }
        return originalText;
    }

    @WrapOperation(
        method = "getMaxProgressWidth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementRequirements;size()I"
        )
    )
    public int setRequiredAdvancementsCount(AdvancementRequirements requirements, Operation<Integer> original) {
        if (trackedScoreType != null) {
            return trackedScoreType.getFinalValue();
        }
        if (trackedNearbyEntitiesType != null) {
            return trackedNearbyEntitiesType.getEntitiesCount();
        }
        if (trackedStatisticsDataType != null) {
            return trackedStatisticsDataType.getFinalValue();
        }
        if (abilityType != null) {
            return AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
        }
        return original.call(requirements);
    }

    @Inject(
        method = "getMaxProgressWidth",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void overrideProgressTextWidth(CallbackInfoReturnable<Integer> cir) {
        if (abilityType != null) {
            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
            ) {
                cir.setReturnValue(0);
                return;
            }
        }
        if (trackedScoreType != null && trackedScoreType.isPercentage() ||
            trackedStatisticsDataType != null && trackedStatisticsDataType.isPercentage()
        ) {
            cir.setReturnValue(8 + minecraft.font.width(Component.translatable("mco.upload.percent", 100)));
        }
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
        ),
        index = 1
    )
    private Identifier renderMystifiedMask(Identifier original) {
        return shouldRenderMystifiedMask() ? ABILITY_MYSTIFIED_MASK_TEXTURE : original;
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"
        )
    )
    private void hideAdvancementIconForMystifiedAbility(
        GuiGraphicsExtractor instance,
        ItemStack stack,
        int x,
        int y,
        Operation<Void> original
    ) {
        if (!shouldRenderMystifiedMask()) {
            original.call(instance, stack, x, y);
        }
    }

    @ModifyReturnValue(
        method = "isMouseOver",
        at = @At("RETURN")
    )
    private boolean hideTooltipForMystifiedAbility(boolean original) {
        return original && !shouldRenderMystifiedMask();
    }
}
