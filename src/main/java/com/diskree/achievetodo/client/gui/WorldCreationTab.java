package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.ability.ProgressionModeType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.injection.extension.client.WorldCreatorExtension;
import com.diskree.achievetodo.server.Constants;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SwitchGrid;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WorldCreationTab extends GridLayoutTab {

    private static final Identifier CONTAINER_BACKGROUND_TEXTURE =
        Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");

    private CycleButton<ProgressionConfig> configSelector;

    private SwitchGrid rewardsSection;
    private SwitchGrid customGenerationSection;

    private GridLayout rewardsContainer;
    private GridLayout customGenerationContainer;

    public WorldCreationTab(CreateWorldScreen screen) {
        super(Component.literal(BuildConfig.MOD_NAME));
        if (screen == null || screen.minecraft == null) {
            return;
        }
        WorldCreationUiState worldCreator = screen.getUiState();
        WorldCreatorExtension worldCreatorExtension = (WorldCreatorExtension) worldCreator;

        layout.defaultCellSetting().alignHorizontallyCenter();

        GridLayout.RowHelper rootContainer = layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);

        List<ProgressionConfig> progressionConfigs = new ArrayList<>();
        ProgressionConfig defaultProgressionConfig = null;
        for (ProgressionModeType progressionModeType : ProgressionModeType.values()) {
            ProgressionConfig progressionConfig = ProgressionConfig.fromProgressionMode(progressionModeType);
            progressionConfigs.add(progressionConfig);
            if (progressionModeType == ProgressionModeType.getDefaultMode()) {
                defaultProgressionConfig = progressionConfig;
            }
        }
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(BuildConfig.MOD_ID);
        if (Files.exists(configDir)) {
            try (Stream<Path> stream = Files.list(configDir)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(Constants.FileExtension.TOML))
                    .map(path -> StringUtils.removeEnd(path.getFileName().toString(), Constants.FileExtension.TOML))
                    .filter(fileName -> !fileName.startsWith(ProgressionModeType.CHAOS.getName() + "_") &&
                        ProgressionModeType.findByName(fileName) == null
                    )
                    .forEach(fileName -> progressionConfigs.add(ProgressionConfig.fromCustomName(fileName)));
            } catch (IOException ignored) {
            }
        }

        configSelector = CycleButton
            .builder(ProgressionConfig::getDisplayedText, defaultProgressionConfig)
            .withValues(progressionConfigs)
            .create(
                0, 0, 150, 20,
                Component.translatable("options.difficulty"),
                (button, progressionConfig) -> worldCreatorExtension.achievetodo$setConfigName(progressionConfig.getConfigName())
            );
        configSelector.setValue(defaultProgressionConfig);
        for (ProgressionConfig progressionConfig : progressionConfigs) {
            if (progressionConfig.getConfigName().equals(worldCreatorExtension.achievetodo$getConfigName())) {
                configSelector.setValue(progressionConfig);
                break;
            }
        }
        updateConfigSelectorTooltip();
        rootContainer.addChild(configSelector, layout.newCellSettings().paddingTop(2));

        CycleButton<Boolean> cooperativeModeButton = CycleButton
            .onOffBuilder(false)
            .withTooltip(value ->
                Tooltip.create(AchieveToDoClient.translate("world_creation_tab.cooperative_mode.tooltip"))
            )
            .create(
                0, 0, 150, 20,
                AchieveToDoClient.translate("world_creation_tab.cooperative_mode"),
                (button, value) -> worldCreatorExtension.achievetodo$setCooperativeModeEnabled(value)
            );
        cooperativeModeButton.setValue(worldCreatorExtension.achievetodo$isCooperativeModeEnabled());
        rootContainer.addChild(cooperativeModeButton, layout.newCellSettings().paddingTop(2));

        GridLayout.RowHelper rewardsTitleContainer = new GridLayout().createRowHelper(1);
        rewardsTitleContainer.addChild(new StringWidget(
            AchieveToDoClient.translate("world_creation_tab.rewards.title")
                .withStyle(DesignCodePalette.TEXT_COLOR),
            screen.minecraft.font
        ));
        SwitchGrid.Builder rewardsSectionBuilder = SwitchGrid.builder(130);
        rewardsSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.rewards.items"),
            worldCreatorExtension::achievetodo$isItemRewardsEnabled,
            worldCreatorExtension::achievetodo$setItemRewardsEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.rewards.items.tooltip"));
        rewardsSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.rewards.experience"),
            worldCreatorExtension::achievetodo$isExperienceRewardsEnabled,
            worldCreatorExtension::achievetodo$setExperienceRewardsEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.rewards.experience.tooltip"));
        rewardsSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.rewards.trophy"),
            worldCreatorExtension::achievetodo$isTrophyRewardsEnabled,
            worldCreatorExtension::achievetodo$setTrophyRewardsEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.rewards.trophy.tooltip"));
        rewardsContainer = new GridLayout();
        rewardsContainer.addChild(rewardsTitleContainer.getGrid(), 0, 0, layout.newCellSettings());
        rewardsSection = rewardsSectionBuilder.build();
        rewardsContainer.addChild(rewardsSection.layout(), 0, 0, layout.newCellSettings().paddingTop(14));
        rootContainer.addChild(rewardsContainer, 1, layout.newCellSettings().paddingTop(14));

        GridLayout.RowHelper customGenerationTitleContainer = new GridLayout().createRowHelper(1);
        customGenerationTitleContainer.addChild(new StringWidget(
            AchieveToDoClient.translate("world_creation_tab.generation.title")
                .withStyle(DesignCodePalette.TEXT_COLOR),
            screen.minecraft.font
        ));
        SwitchGrid.Builder customGenerationSectionBuilder = SwitchGrid.builder(130);
        customGenerationSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.generation.overworld"),
            worldCreatorExtension::achievetodo$isTerralithEnabled,
            worldCreatorExtension::achievetodo$setTerralithEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.generation.overworld.tooltip"));
        customGenerationSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.generation.nether"),
            worldCreatorExtension::achievetodo$isAmplifiedNetherEnabled,
            worldCreatorExtension::achievetodo$setAmplifiedNetherEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.generation.nether.tooltip"));
        customGenerationSectionBuilder.addSwitch(
            AchieveToDoClient.translate("world_creation_tab.generation.end"),
            worldCreatorExtension::achievetodo$isNullscapeEnabled,
            worldCreatorExtension::achievetodo$setNullscapeEnabled
        ).withInfo(AchieveToDoClient.translate("world_creation_tab.generation.end.tooltip"));
        customGenerationContainer = new GridLayout();
        customGenerationContainer.addChild(customGenerationTitleContainer.getGrid(), 0, 0, layout.newCellSettings());
        customGenerationSection = customGenerationSectionBuilder.build();
        customGenerationContainer.addChild(customGenerationSection.layout(), 0, 0, layout.newCellSettings().paddingTop(14));
        rootContainer.addChild(customGenerationContainer, 1, layout.newCellSettings().paddingTop(14));

        worldCreator.addListener(creator -> {
            rewardsSection.refreshStates();
            customGenerationSection.refreshStates();
            updateConfigSelectorTooltip();
        });
        layout.arrangeElements();
    }

    public void render(GuiGraphicsExtractor context) {
        renderContainerBackground(context, rewardsContainer);
        renderContainerBackground(context, customGenerationContainer);
    }

    private void updateConfigSelectorTooltip() {
        configSelector.setTooltip(Tooltip.create(configSelector.getValue().getTooltipText()));
    }

    private void renderContainerBackground(GuiGraphicsExtractor context, GridLayout container) {
        if (container == null) {
            return;
        }
        int padding = 9;
        int lineHeight = 2;
        int textureSize = 32;
        int x = container.getX() - padding;
        int y = container.getY() - padding;
        int width = container.getWidth() + padding * 2;
        int height = container.getHeight() + padding * 2;
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        context.blit(
            pipeline,
            Screen.HEADER_SEPARATOR,
            x,
            y - lineHeight,
            0.0f,
            0.0f,
            width,
            lineHeight,
            textureSize,
            lineHeight
        );
        context.blit(
            pipeline,
            CONTAINER_BACKGROUND_TEXTURE,
            x,
            y,
            0.0f,
            0.0f,
            width,
            height,
            textureSize,
            textureSize
        );
        context.blit(
            pipeline,
            Screen.FOOTER_SEPARATOR,
            x,
            y + height,
            0.0f,
            0.0f,
            width,
            lineHeight,
            textureSize,
            lineHeight
        );
    }

    private record ProgressionConfig(ProgressionModeType builtInMode, String customName) {

        public static @NotNull WorldCreationTab.ProgressionConfig fromProgressionMode(ProgressionModeType type) {
            return new ProgressionConfig(type, null);
        }

        public static @NotNull WorldCreationTab.ProgressionConfig fromCustomName(String customName) {
            return new ProgressionConfig(null, customName);
        }

        public Component getDisplayedText() {
            return builtInMode != null ? builtInMode.getDisplayedText() : Component.literal(customName);
        }

        public Component getTooltipText() {
            return builtInMode != null ? builtInMode.getTooltipText()
                : AchieveToDoClient.translate("world_creation_tab.progression.custom.tooltip");
        }

        public String getConfigName() {
            return builtInMode != null ? builtInMode.getName() : customName;
        }
    }
}
