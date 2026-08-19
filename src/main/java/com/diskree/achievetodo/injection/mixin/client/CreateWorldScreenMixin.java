package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.client.InternalPack;
import com.diskree.achievetodo.client.gui.ExternalPackDownloader;
import com.diskree.achievetodo.client.gui.WorldCreationTab;
import com.diskree.achievetodo.injection.extension.client.CreateWorldScreenExtension;
import com.diskree.achievetodo.injection.extension.client.WorldCreatorExtension;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;

@Mixin(value = CreateWorldScreen.class, priority = 500)
public abstract class CreateWorldScreenMixin extends Screen implements CreateWorldScreenExtension {

    @Unique
    private boolean isWaitingDatapack;

    @Unique
    private WorldCreationTab worldCreationTab;

    protected CreateWorldScreenMixin(Component title) {
        super(title);
    }

    @Override
    public boolean achievetodo$isWaitingDatapack() {
        return isWaitingDatapack;
    }

    @Override
    public void achievetodo$setWaitingDatapack(boolean isWaitingDatapack) {
        this.isWaitingDatapack = isWaitingDatapack;
    }

    @Shadow
    @Final
    WorldCreationUiState uiState;

    @Shadow
    private @Nullable PackRepository tempDataPackRepository;

    @Shadow
    @Final
    private TabManager tabManager;

    @Shadow
    public abstract void onCreate();

    @Shadow
    protected abstract @Nullable Path getOrCreateTempDataPackDir();

    @Shadow
    protected abstract @Nullable Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration settings);

    @Shadow
    protected abstract void tryApplyNewDataPacks(
        PackRepository dataPackManager,
        boolean warnForExperimentsIfApplicable,
        Consumer<WorldDataConfiguration> consumer
    );

    @ModifyArgs(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/MenuTabBar$Builder;"
        )
    )
    private void addTab(@NotNull Args args) {
        CreateWorldScreen createWorldScreen = (CreateWorldScreen) (Object) this;
        Tab[] originalTabs = args.get(0);
        Tab[] newTabs = new Tab[originalTabs.length + 1];
        if (originalTabs.length >= 0) {
            System.arraycopy(originalTabs, 0, newTabs, 0, originalTabs.length);
        }
        newTabs[originalTabs.length] = worldCreationTab = new WorldCreationTab(createWorldScreen);
        args.set(0, newTabs);
    }

    @Inject(
        method = "createFromExisting(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;Ljava/nio/file/Path;)Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;",
        at = @At(value = "TAIL")
    )
    private static void parseWorldOptionsOnRecreate(
        Minecraft client,
        Runnable callback,
        @NotNull LevelSettings levelInfo,
        WorldCreationContext generatorOptionsHolder,
        Path dataPackTempDir,
        CallbackInfoReturnable<CreateWorldScreen> cir,
        @Local @NotNull CreateWorldScreen createWorldScreen
    ) {
        WorldDataConfiguration dataConfiguration = levelInfo.dataConfiguration();
        if (dataConfiguration != null) {
            DataPackConfig dataPackSettings = dataConfiguration.dataPacks();
            if (dataPackSettings != null) {
                List<String> enabledPacks = dataPackSettings.getEnabled();
                if (enabledPacks != null) {
                    WorldCreationUiState worldCreator = createWorldScreen.getUiState();
                    if (worldCreator instanceof WorldCreatorExtension worldCreatorExtension) {
                        LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(levelInfo);
                        worldCreatorExtension.achievetodo$setConfigName(
                            levelInfoExtension.achievetodo$getConfigName()
                        );

                        worldCreatorExtension.achievetodo$setTerralithEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_TERRALITH.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setAmplifiedNetherEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_AMPLIFIED_NETHER.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setNullscapeEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_NULLSCAPE.getDatapackName())
                        );

                        worldCreatorExtension.achievetodo$setItemRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_ITEM.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setExperienceRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_EXPERIENCE.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setTrophyRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_TROPHY.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setCooperativeModeEnabled(
                            enabledPacks.contains(InternalPack.BACAP_COOPERATIVE_MODE.getDatapackName())
                        );
                    }
                }
            }
        }
    }

    @Inject(
        method = "onCreate",
        at = @At("HEAD"),
        cancellable = true
    )
    private void prepareDatapacks(CallbackInfo ci) {
        CreateWorldScreen createWorldScreen = (CreateWorldScreen) (Object) this;
        WorldCreatorExtension worldCreatorExtension = (WorldCreatorExtension) uiState;
        Minecraft client = createWorldScreen.minecraft;
        if (client == null) {
            ci.cancel();
            return;
        }

        boolean isHardcoreEnabled = uiState.isHardcore();
        boolean isTerralithEnabled = worldCreatorExtension.achievetodo$isTerralithEnabled();
        boolean isAmplifiedNetherEnabled = worldCreatorExtension.achievetodo$isAmplifiedNetherEnabled();
        boolean isNullscapeEnabled = worldCreatorExtension.achievetodo$isNullscapeEnabled();

        Path globalPacksDirectory = new File(client.gameDirectory, "datapacks").toPath();
        List<ExternalPack> requiredPacks = new ArrayList<>();
        requiredPacks.add(ExternalPack.BACAP);
        if (isHardcoreEnabled) {
            requiredPacks.add(ExternalPack.BACAP_HARDCORE);
        }
        if (isTerralithEnabled) {
            requiredPacks.add(ExternalPack.TERRALITH);
            requiredPacks.add(ExternalPack.BACAP_TERRALITH);
        }
        if (isAmplifiedNetherEnabled) {
            requiredPacks.add(ExternalPack.AMPLIFIED_NETHER);
            requiredPacks.add(ExternalPack.BACAP_AMPLIFIED_NETHER);
        }
        if (isNullscapeEnabled) {
            requiredPacks.add(ExternalPack.NULLSCAPE);
            requiredPacks.add(ExternalPack.BACAP_NULLSCAPE);
        }
        for (ExternalPack requiredPack : requiredPacks) {
            Path globalPack = globalPacksDirectory.resolve(requiredPack.getFileName());
            if (Files.exists(globalPack)
                && (requiredPack.getSha1().equalsIgnoreCase(com.diskree.achievetodo.client.Utils.calculateSHA1(globalPack))
                || ExternalPackCompatibility.isCompatibleWorldCopy(globalPack, requiredPack))) {
                continue;
            }
            client.setScreenAndShow(new ExternalPackDownloader(createWorldScreen, requiredPack, isFileDownloaded -> {
                if (isFileDownloaded) {
                    onCreate();
                }
            }, false));
            ci.cancel();
            return;
        }

        if (!isWaitingDatapack) {
            Path worldPacksTempDirectory = getOrCreateTempDataPackDir();
            if (worldPacksTempDirectory == null) {
                ci.cancel();
                return;
            }
            try {
                for (ExternalPack pack : requiredPacks) {
                    Path globalPack = globalPacksDirectory.resolve(pack.getFileName());
                    Path worldPack = worldPacksTempDirectory.resolve(globalPack.getFileName());
                    ExternalPackCompatibility.copyForWorld(globalPack, worldPack, pack);
                }
            } catch (IOException ignored) {
            }

            if (tempDataPackRepository != null) {
                tempDataPackRepository.reload();
            }
            getDataPackSelectionSettings(uiState.getSettings().dataConfiguration());
            if (tempDataPackRepository != null) {
                for (ExternalPack externalPack : ExternalPack.values()) {
                    tempDataPackRepository.removePack(externalPack.getDatapackName());
                }
                for (InternalPack internalPack : InternalPack.values()) {
                    tempDataPackRepository.removePack(internalPack.getDatapackName());
                }

                tempDataPackRepository.addPack(ExternalPack.BACAP.getDatapackName());
                tempDataPackRepository.addPack(InternalPack.BACAP_OVERRIDE.getDatapackName());
                if (isHardcoreEnabled) {
                    tempDataPackRepository.addPack(ExternalPack.BACAP_HARDCORE.getDatapackName());
                    tempDataPackRepository.addPack(InternalPack.BACAP_HARDCORE_OVERRIDE.getDatapackName());
                }
                if (isTerralithEnabled) {
                    tempDataPackRepository.addPack(ExternalPack.TERRALITH.getDatapackName());
                    tempDataPackRepository.addPack(ExternalPack.BACAP_TERRALITH.getDatapackName());
                    tempDataPackRepository.addPack(InternalPack.BACAP_TERRALITH_OVERRIDE.getDatapackName());
                }
                if (isAmplifiedNetherEnabled) {
                    tempDataPackRepository.addPack(ExternalPack.AMPLIFIED_NETHER.getDatapackName());
                    tempDataPackRepository.addPack(ExternalPack.BACAP_AMPLIFIED_NETHER.getDatapackName());
                    tempDataPackRepository.addPack(InternalPack.BACAP_AMPLIFIED_NETHER_OVERRIDE.getDatapackName());
                }
                if (isNullscapeEnabled) {
                    tempDataPackRepository.addPack(ExternalPack.NULLSCAPE.getDatapackName());
                    tempDataPackRepository.addPack(ExternalPack.BACAP_NULLSCAPE.getDatapackName());
                    tempDataPackRepository.addPack(InternalPack.BACAP_NULLSCAPE_OVERRIDE.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isItemRewardsEnabled()) {
                    tempDataPackRepository.addPack(InternalPack.BACAP_REWARDS_ITEM.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isExperienceRewardsEnabled()) {
                    tempDataPackRepository.addPack(InternalPack.BACAP_REWARDS_EXPERIENCE.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isTrophyRewardsEnabled()) {
                    tempDataPackRepository.addPack(InternalPack.BACAP_REWARDS_TROPHY.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isCooperativeModeEnabled()) {
                    tempDataPackRepository.addPack(InternalPack.BACAP_COOPERATIVE_MODE.getDatapackName());
                }

                isWaitingDatapack = true;
                tryApplyNewDataPacks(tempDataPackRepository, false, (dataConfiguration) -> client.setScreenAndShow(createWorldScreen));

                ci.cancel();
            }
        }
    }

    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void renderWorldCreationTab(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (worldCreationTab != null && tabManager.getCurrentTab() == worldCreationTab) {
            worldCreationTab.render(context);
        }
    }

    @ModifyReturnValue(
        method = "createLevelSettings",
        at = @At(
            value = "RETURN",
            ordinal = 1
        )
    )
    private LevelSettings setConfigName(LevelSettings levelInfo) {
        if (uiState instanceof WorldCreatorExtension worldCreatorExtension) {
            LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(levelInfo);
            levelInfoExtension.achievetodo$setConfigName(worldCreatorExtension.achievetodo$getConfigName());
        }
        return levelInfo;
    }
}
