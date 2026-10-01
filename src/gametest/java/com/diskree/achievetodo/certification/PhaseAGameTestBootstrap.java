package com.diskree.achievetodo.certification;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class PhaseAGameTestBootstrap implements ModInitializer {
    private static final String BACAP_ADVANCEMENTS_OBJECTIVE = "bac_advancements";

    @Override
    public void onInitialize() {
        try {
            PhaseARuntimeExecutionEvidence.resetRun(PhaseARuntimeExecutionEvidence.projectRoot());
            PhaseAContainerLootExecutionEvidence.resetRun(PhaseAContainerLootExecutionEvidence.projectRoot());
            PhaseAItemTagInventoryChangedExecutionEvidence.resetRun(PhaseAItemTagInventoryChangedExecutionEvidence.projectRoot());
            PhaseAItemTagPlacedBlockExecutionEvidence.resetRun(PhaseAItemTagPlacedBlockExecutionEvidence.projectRoot());
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.resetRun(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidence.projectRoot());
            PhaseAEnchantmentInventoryChangedExecutionEvidence.resetRun(PhaseAEnchantmentInventoryChangedExecutionEvidence.projectRoot());
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.resetRun(
                PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidence.projectRoot()
            );
            PhaseATrimPatternRecipeCraftedExecutionEvidence.resetRun(
                PhaseATrimPatternRecipeCraftedExecutionEvidence.projectRoot()
            );
            PhaseAItemTagItemUsedOnBlockExecutionEvidence.resetRun(
                PhaseAItemTagItemUsedOnBlockExecutionEvidence.projectRoot()
            );
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidence.resetRun(
                PhaseAItemTagPlayerInteractedWithEntityExecutionEvidence.projectRoot()
            );
            PhaseAShotCrossbowExecutionEvidence.resetRun(
                PhaseAShotCrossbowExecutionEvidence.projectRoot()
            );
            PhaseAUsingItemExecutionEvidence.resetRun(
                PhaseAUsingItemExecutionEvidence.projectRoot()
            );
            PhaseATameAnimalLlamaExecutionEvidence.beginRun(
                PhaseATameAnimalLlamaExecutionEvidence.projectRoot()
            );
            PhaseARecipeCraftedWaxOnExecutionEvidence.beginRun(
                PhaseARecipeCraftedWaxOnExecutionEvidence.projectRoot()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset runtime evidence run", e);
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Scoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(BACAP_ADVANCEMENTS_OBJECTIVE);
            if (objective == null) {
                objective = scoreboard.addObjective(
                    BACAP_ADVANCEMENTS_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    Component.literal(BACAP_ADVANCEMENTS_OBJECTIVE),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
                );
            }
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        });
    }
}
