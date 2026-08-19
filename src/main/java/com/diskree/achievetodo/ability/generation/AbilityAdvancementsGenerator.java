package com.diskree.achievetodo.ability.generation;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.client.AchieveToDoClient;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AbilityAdvancementsGenerator extends FabricAdvancementProvider {

    public static final String DEMYSTIFIED_CRITERION = "demystified";
    public static final String UNLOCKED_CRITERION = "unlocked";

    public static final String ABILITY_PATH_PREFIX = "abilities/";

    public static final Block TAB_BACKGROUND = Blocks.PALE_MOSS_BLOCK;

    protected AbilityAdvancementsGenerator(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registryLookup
    ) {
        super(output, registryLookup);
    }

    public static @NotNull Identifier buildAdvancementId(@NotNull AbilityType ability) {
        return buildAdvancementId(ability.getName());
    }

    private static @NotNull Identifier buildAdvancementId(String suffix) {
        return AchieveToDoMod.getIdentifier(ABILITY_PATH_PREFIX + suffix);
    }

    @Override
    public void generateAdvancement(
        HolderLookup.Provider registryLookup,
        @NotNull Consumer<AdvancementHolder> consumer
    ) {
        AdvancementHolder rootAdvancement = Advancement.Builder
            .recipeAdvancement()
            .display(
                Items.BARRIER,
                Component.literal(BuildConfig.MOD_NAME),
                AchieveToDoClient.translate("description"),
                Identifier.withDefaultNamespace("block/" + BuiltInRegistries.BLOCK.getKey(TAB_BACKGROUND).getPath()),
                AdvancementType.TASK,
                false,
                false,
                false
            )
            .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
            .build(buildAdvancementId("root"));
        consumer.accept(rootAdvancement);

        for (AbilityType abilityType : AbilityType.values()) {
            Identifier advancementId = buildAdvancementId(abilityType);
            consumer.accept(Advancement.Builder
                .recipeAdvancement()
                .parent(rootAdvancement)
                .display(
                    abilityType.getIcon(),
                    abilityType.getTitle(),
                    abilityType.getDescription(),
                    null,
                    AdvancementType.TASK,
                    true,
                    false,
                    false
                )
                .rewards(AdvancementRewards.Builder.function(advancementId))
                .addCriterion(
                    DEMYSTIFIED_CRITERION,
                    CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())
                )
                .addCriterion(
                    UNLOCKED_CRITERION,
                    CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())
                )
                .build(advancementId)
            );
        }
    }
}
