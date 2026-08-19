package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.ability.AbilitiesHierarchyLayerType;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {

    @Unique
    private final Map<AbilityType, AdvancementWidget> pendingAbilityWidgets = new HashMap<>();

    @Shadow
    protected abstract void addWidget(AdvancementWidget widget, AdvancementHolder advancement);

    @Shadow
    @Final
    private AdvancementNode rootNode;

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementWidget> widgets;

    @Inject(
        method = "create",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void setCustomTabsLayout(
        Minecraft client,
        AdvancementsScreen screen,
        int index,
        @NotNull AdvancementNode root,
        CallbackInfoReturnable<AdvancementTab> cir
    ) {
        DisplayInfo advancementDisplay = root.advancement().display().orElse(null);
        if (advancementDisplay == null) {
            cir.setReturnValue(null);
            return;
        }
        AdvancementsTabType tab = AdvancementsTabType.findByAdvancement(root);
        if (tab == null) {
            return;
        }
        if (tab == AdvancementsTabType.ABILITIES) {
            int childrenCount = 0;
            for (AbilitiesHierarchyLayerType layerType : AbilitiesHierarchyLayerType.values()) {
                childrenCount += layerType.getRowsCount();
            }
            advancementDisplay.setLocation(advancementDisplay.getX(), (float) (childrenCount / 2));
        }
        cir.setReturnValue(new AdvancementTab(
            client,
            screen,
            tab.getPosition(),
            tab.getOrder(),
            root,
            advancementDisplay
        ));
    }

    @WrapOperation(
        method = "addAdvancement",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;Lnet/minecraft/client/Minecraft;Lnet/minecraft/advancements/AdvancementNode;Lnet/minecraft/advancements/DisplayInfo;)Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;"
        )
    )
    public AdvancementWidget restructureAbilitiesAdvancements(
        AdvancementTab tab,
        Minecraft client,
        @NotNull AdvancementNode advancement,
        DisplayInfo display,
        @NotNull Operation<AdvancementWidget> original
    ) {
        AbilityType abilityTypeToAdd = null;
        boolean isFirstInRow = false;
        boolean shouldSkipVanillaBehavior = false;
        if (AdvancementsTabType.findByAdvancement(rootNode) == AdvancementsTabType.ABILITIES) {
            abilityTypeToAdd = AbilityType.findByAdvancement(advancement);
            if (abilityTypeToAdd != null) {
                List<List<AbilityType>> rows = AchieveToDoClient.getAbilityRows();
                if (rows != null) {
                    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                        List<AbilityType> row = rows.get(rowIndex);
                        if (abilityTypeToAdd == row.getFirst()) {
                            isFirstInRow = true;
                            display.setLocation(display.getX(), rowIndex);
                            break;
                        }
                    }
                    if (!isFirstInRow) {
                        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                            List<AbilityType> row = rows.get(rowIndex);
                            int columnIndex = row.indexOf(abilityTypeToAdd);
                            if (columnIndex != -1) {
                                display.setLocation(columnIndex + 1, rowIndex);
                                break;
                            }
                        }
                        shouldSkipVanillaBehavior = true;
                    }
                }
            }
        }
        AdvancementWidget advancementWidget = original.call(tab, client, advancement, display);
        if (abilityTypeToAdd != null) {
            if (isFirstInRow || shouldSkipVanillaBehavior) {
                pendingAbilityWidgets.put(abilityTypeToAdd, advancementWidget);
            }
            if (pendingAbilityWidgets.size() == AbilityType.values().length) {
                List<List<AbilityType>> rows = AchieveToDoClient.getAbilityRows();

                for (List<AbilityType> row : rows) {
                    for (int columnIndex = 1; columnIndex < row.size(); columnIndex++) {
                        AbilityType abilityType = row.get(columnIndex);
                        AbilityType previousAbilityType = row.get(columnIndex - 1);
                        AdvancementWidget currentWidget = pendingAbilityWidgets.get(abilityType);
                        if (currentWidget == null) {
                            for (AdvancementHolder widgetAdvancement : widgets.keySet()) {
                                if (AbilityType.findByAdvancement(widgetAdvancement) == abilityType) {
                                    currentWidget = widgets.get(widgetAdvancement);
                                }
                            }
                        }
                        if (currentWidget == null) {
                            continue;
                        }
                        AdvancementWidget parentWidget = pendingAbilityWidgets.get(previousAbilityType);
                        if (parentWidget == null) {
                            for (AdvancementHolder widgetAdvancement : widgets.keySet()) {
                                if (AbilityType.findByAdvancement(widgetAdvancement) == previousAbilityType) {
                                    parentWidget = widgets.get(widgetAdvancement);
                                }
                            }
                        }
                        if (parentWidget == null) {
                            continue;
                        }
                        currentWidget.parent = parentWidget;
                        parentWidget.addChild(currentWidget);
                        addWidget(currentWidget, currentWidget.advancementNode.holder());
                    }
                }
            }
        }
        return shouldSkipVanillaBehavior ? null : advancementWidget;
    }

    @Inject(
        method = "addWidget",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void skipNullWidget(AdvancementWidget widget, @NotNull AdvancementHolder advancement, CallbackInfo ci) {
        if (widget == null) {
            ci.cancel();
        }
    }
}
