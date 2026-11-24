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
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.Minecraft;
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
    private Map<com.diskree.achievetodo.ability.AbilityType, Integer> bridgeLayoutConfiguration;

    @Inject(method = "init", at = @At("TAIL"))
    private void rememberBridgeLayoutConfiguration(CallbackInfo ci) {
        bridgeLayoutConfiguration = com.diskree.achievetodo.client.bridge.BridgeClient.presentationReady()
            ? com.diskree.achievetodo.client.bridge.BridgePresentation.thresholds() : null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void refreshBridgeLayoutConfiguration(CallbackInfo ci) {
        if (!com.diskree.achievetodo.client.bridge.BridgeClient.presentationReady()) return;
        var current = com.diskree.achievetodo.client.bridge.BridgePresentation.thresholds();
        if (current.equals(bridgeLayoutConfiguration)) return;
        // A count-only delta updates frames/progress live. Changed thresholds also
        // change row order and cached tooltip widths/descriptions, requiring new widgets.
        AdvancementHolder selected = selectedTab == null ? null : selectedTab.getRootAdvancement();
        rebuildWidgets();
        if (selected != null && tabs.containsKey(selected) && minecraft.getConnection() != null) {
            minecraft.getConnection().getAdvancements().setSelectedTab(selected, true);
        }
    }

    @Unique
    private static final float MYSTIFIED_TAB_ALPHA = 0.3f;

    @Unique
    private final Identifier ADVANCEMENTS_TAB_MYSTIFIED_MASK_TEXTURE =
        AchieveToDoMod.getIdentifier("advancements_tab_mystified_mask");

    @Unique
    private boolean isMystifiedTab(@NotNull AdvancementTab tab) {
        Identifier advancementId = tab.getRootAdvancement().id();
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

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void purpurSyncNotice(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        // On a Purpur server the screen looks the same as in single player; only a pending sync is shown.
        if(com.diskree.achievetodo.client.bridge.BridgeClient.isPurpurBackend()
                && !com.diskree.achievetodo.client.bridge.BridgeClient.presentationReady()) {
            graphics.text(minecraft.font,net.minecraft.network.chat.Component.translatable("achievetodo.bridge.syncing"),6,4,0xFFFFC66D);
        }
    }

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Shadow
    private AdvancementTab selectedTab;

    /**
     * Since 26.3 the screen rebuilds every tab in {@code onAdvancementsUpdated} instead of adding roots one by one.
     * After the real roots are in place, each ATD tab whose root is not present yet gets its mystified placeholder.
     */
    @Inject(
        method = "onAdvancementsUpdated",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementTree;tasks()Ljava/lang/Iterable;"
        )
    )
    public void addLockedTabs(CallbackInfo ci) {
        if (minecraft == null) {
            return;
        }
        AdvancementsScreen advancementsScreen = (AdvancementsScreen) (Object) this;
        for (AdvancementsTabType advancementsTabType : AdvancementsTabType.values()) {
            boolean present = false;
            for (AdvancementHolder root : tabs.keySet()) {
                if (AdvancementsTabType.findByAdvancement(root.id()) == advancementsTabType) {
                    present = true;
                    break;
                }
            }
            if (present) {
                continue;
            }
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
            AdvancementWidget rootWidget = AdvancementWidget.createWidget(minecraft, placedAdvancement);
            if (rootWidget == null) {
                continue;
            }
            tabs.put(
                placedAdvancement.holder(),
                new AdvancementTab(
                    minecraft,
                    advancementsScreen,
                    advancementsTabType.getPosition(),
                    advancementsTabType.getOrder(),
                    rootWidget,
                    advancementDisplay.icon(),
                    advancementDisplay.title(),
                    AdvancementsTabType.backgroundOf(advancementDisplay)
                )
            );
        }
    }

    @WrapOperation(
        method = "onAdvancementsUpdated",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;create(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;ILnet/minecraft/advancements/AdvancementNode;)Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;"
        )
    )
    public AdvancementTab placeForeignTab(
        Minecraft client,
        AdvancementsScreen screen,
        int index,
        @NotNull AdvancementNode root,
        @NotNull Operation<AdvancementTab> original
    ) {
        AdvancementsTabType tab = AdvancementsTabType.findByAdvancement(root);
        if (tab != null) {
            // ATD tabs get their fixed slot from AdvancementTabMixin.
            return original.call(client, screen, index, root);
        }
        {
            // Vanilla's ordinal counts the reserved ATD tabs and can alias their slots.
            // Keep foreign ownership intact and allocate only a genuinely free native slot.
            DisplayInfo display = root.advancement().display().orElse(null);
            if (display != null) {
                for (AdvancementTabType position : AdvancementTabType.values()) {
                    for (int order = 0; order < position.getMax(); order++) {
                        boolean occupied = false;
                        for (AdvancementsTabType reserved : AdvancementsTabType.values()) {
                            if (overlapsNativeTab(position, order, reserved.getPosition(), reserved.getOrder())) occupied = true;
                        }
                        for (AdvancementTab existing : tabs.values()) {
                            if (overlapsNativeTab(position, order, existing.getType(), existing.getIndex())) occupied = true;
                        }
                        if (!occupied) {
                            AdvancementWidget rootWidget = AdvancementWidget.createWidget(client, root);
                            return rootWidget == null ? null : new AdvancementTab(client, screen,
                                position, order, rootWidget, display.icon(), display.title(),
                                AdvancementsTabType.backgroundOf(display));
                        }
                    }
                }
            }
            // Native capacity is finite; never overlap a reserved or existing tab.
            return null;
        }
    }

    @Unique
    private static boolean overlapsNativeTab(AdvancementTabType a, int ai, AdvancementTabType b, int bi) {
        return a.getX(ai) < b.getX(bi) + b.getWidth()
            && a.getX(ai) + a.getWidth() > b.getX(bi)
            && a.getY(ai) < b.getY(bi) + b.getHeight()
            && a.getY(ai) + a.getHeight() > b.getY(bi);
    }

    @WrapOperation(
        method = {"init", "onAdvancementsUpdated"},
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
            AdvancementsTabType.findByAdvancement(tab.getRootAdvancement().id()) == AdvancementsTabType.ABILITIES
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
