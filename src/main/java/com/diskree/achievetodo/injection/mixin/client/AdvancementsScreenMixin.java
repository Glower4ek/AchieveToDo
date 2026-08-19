package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    @Unique
    private static final float MYSTIFIED_TAB_ALPHA = 0.3f;

    @Unique
    private final Identifier ADVANCEMENTS_TAB_MYSTIFIED_MASK_TEXTURE =
        AchieveToDoMod.getIdentifier("advancements_tab_mystified_mask");

    @Unique
    private boolean isMystifiedTab(@NotNull AdvancementTab tab) {
        Identifier advancementId = tab.getRootNode().holder().id();
        for (AdvancementsTabType advancementsTabType : AdvancementsTabType.values()) {
            if (advancementsTabType != null && advancementsTabType.getMystifiedTabId().equals(advancementId)) {
                return true;
            }
        }
        return false;
    }

    public AdvancementsScreenMixin() {
        super(null);
    }

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Shadow
    private AdvancementTab selectedTab;

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;clear()V",
            shift = At.Shift.AFTER
        )
    )
    public void addLockedTabs(CallbackInfo ci) {
        if (minecraft == null) {
            return;
        }
        AdvancementsScreen advancementsScreen = (AdvancementsScreen) (Object) this;
        for (AdvancementsTabType advancementsTabType : AdvancementsTabType.values()) {
            DisplayInfo advancementDisplay = new DisplayInfo(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.BARRIER)),
                advancementsTabType.getMystifiedTabTooltipText(),
                Component.empty(),
                Optional.empty(),
                AdvancementType.TASK,
                false,
                false,
                false
            );
            AdvancementNode placedAdvancement = new AdvancementNode(
                Advancement.Builder
                    .recipeAdvancement()
                    .display(advancementDisplay)
                    .build(advancementsTabType.getMystifiedTabId()),
                null
            );
            tabs.put(
                placedAdvancement.holder(),
                new AdvancementTab(
                    minecraft,
                    advancementsScreen,
                    advancementsTabType.getPosition(),
                    advancementsTabType.getOrder(),
                    placedAdvancement,
                    advancementDisplay
                )
            );
        }
    }

    @Inject(
        method = "onAddAdvancementRoot",
        at = @At(value = "HEAD")
    )
    public void removeLockedTab(@NotNull AdvancementNode root, CallbackInfo ci) {
        AdvancementsTabType tab = AdvancementsTabType.findByAdvancement(root);
        AdvancementHolder lockedRoot = null;
        for (AdvancementHolder advancementEntry : tabs.keySet()) {
            if (tab != null && tab.getMystifiedTabId().equals(advancementEntry.id())) {
                lockedRoot = advancementEntry;
                break;
            }
        }
        if (lockedRoot != null) {
            tabs.remove(lockedRoot);
        }
    }

    @WrapOperation(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;values()Ljava/util/Collection;"
        )
    )
    public Collection<AdvancementTab> setAbilitiesTabOpenedByDefault(
        Map<AdvancementHolder, AdvancementTab> tabs,
        @NotNull Operation<Collection<AdvancementTab>> original
    ) {
        Collection<AdvancementTab> allTabs = original.call(tabs);
        Collection<AdvancementTab> abilityTabs = allTabs.stream().filter(tab ->
            AdvancementsTabType.findByAdvancement(tab.getRootNode()) == AdvancementsTabType.ABILITIES
        ).toList();
        return abilityTabs.isEmpty() ? allTabs : abilityTabs;
    }

    @WrapOperation(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;isMouseOver(IIDD)Z"
        )
    )
    public boolean disallowClickOnLockedTab(
        @NotNull AdvancementTab tab,
        int screenX,
        int screenY,
        double mouseX,
        double mouseY,
        Operation<Boolean> original
    ) {
        return !isMystifiedTab(tab) && original.call(tab, screenX, screenY, mouseX, mouseY);
    }

    @WrapOperation(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractTab(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIZ)V"
        )
    )
    public void renderLockedTab(
        AdvancementTab tab,
        GuiGraphicsExtractor context,
        int x,
        int y,
        int mouseX,
        int mouseY,
        boolean selected,
        @NotNull Operation<Void> original
    ) {
        if (!isMystifiedTab(tab)) {
            original.call(tab, context, x, y, mouseX, mouseY, selected);
            return;
        }
        int tabX = x + tab.getType().getX(tab.getIndex());
        int tabY = y + tab.getType().getY(tab.getIndex());
        renderMystifiedTabBackground(context, tab, tabX, tabY, selected);
        if (!selected &&
            mouseX > tabX &&
            mouseY > tabY &&
            mouseX < tabX + tab.getType().getWidth() &&
            mouseY < tabY + tab.getType().getHeight()
        ) {
            context.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND);
        }
        int maskX = x + tab.getType().getX(tab.getIndex());
        int maskY = y + tab.getType().getY(tab.getIndex());
        switch (tab.getType()) {
            case AdvancementTabType.ABOVE:
                maskX += 6;
                maskY += 9;
                break;
            case AdvancementTabType.BELOW:
                maskX += 6;
                maskY += 6;
                break;
            case AdvancementTabType.LEFT:
                maskX += 10;
                maskY += 6;
                break;
            case AdvancementTabType.RIGHT:
                maskX += 6;
                maskY += 5;
        }
        context.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            ADVANCEMENTS_TAB_MYSTIFIED_MASK_TEXTURE,
            maskX,
            maskY,
            16,
            16,
            MYSTIFIED_TAB_ALPHA
        );
    }

    @WrapOperation(
        method = "extractWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;extractIcon(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
        )
    )
    public void hideLockedTabPlaceholderIcon(
        AdvancementTab tab,
        GuiGraphicsExtractor context,
        int x,
        int y,
        @NotNull Operation<Void> original
    ) {
        if (!isMystifiedTab(tab)) {
            original.call(tab, context, x, y);
        }
    }

    @Unique
    private void renderMystifiedTabBackground(
        @NotNull GuiGraphicsExtractor context,
        @NotNull AdvancementTab tab,
        int x,
        int y,
        boolean selected
    ) {
        context.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            getTabSpriteId(tab.getType(), tab.getIndex(), selected),
            x,
            y,
            tab.getType().getWidth(),
            tab.getType().getHeight(),
            MYSTIFIED_TAB_ALPHA
        );
    }

    @Unique
    private Identifier getTabSpriteId(@NotNull AdvancementTabType type, int index, boolean selected) {
        String variant;
        if (index == 0) {
            variant = type == AdvancementTabType.LEFT || type == AdvancementTabType.RIGHT ? "top" : "left";
        } else if (index == type.getMax() - 1) {
            variant = type == AdvancementTabType.LEFT || type == AdvancementTabType.RIGHT ? "bottom" : "right";
        } else {
            variant = "middle";
        }
        String selectedSuffix = selected ? "_selected" : "";
        return switch (type) {
            case ABOVE -> Identifier.withDefaultNamespace("advancements/tab_above_" + variant + selectedSuffix);
            case BELOW -> Identifier.withDefaultNamespace("advancements/tab_below_" + variant + selectedSuffix);
            case LEFT -> Identifier.withDefaultNamespace("advancements/tab_left_" + variant + selectedSuffix);
            case RIGHT -> Identifier.withDefaultNamespace("advancements/tab_right_" + variant + selectedSuffix);
        };
    }
}
