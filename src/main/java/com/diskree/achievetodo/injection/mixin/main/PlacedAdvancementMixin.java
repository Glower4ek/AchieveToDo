package com.diskree.achievetodo.injection.mixin.main;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.Identifier;

@Mixin(AdvancementNode.class)
public abstract class PlacedAdvancementMixin {

    @Unique
    private static final Map<String, List<String>> customChildrenOrderMap = new HashMap<>();

    static {
        customChildrenOrderMap.put("blazeandcave:statistics/root", List.of(
            "blazeandcave:statistics/out_for_a_stroll",
            "blazeandcave:statistics/natural_sprinter",
            "blazeandcave:statistics/sneaky_snitch",
            "blazeandcave:statistics/laps_in_the_pool",
            "blazeandcave:statistics/luxury_cruise",
            "blazeandcave:technical/big_cheater",
            "blazeandcave:statistics/minecart_rider",
            "blazeandcave:statistics/pig_training",
            "blazeandcave:statistics/taking_it_in_stride",
            "blazeandcave:statistics/horse_training",
            "blazeandcave:statistics/take_to_the_skies"
        ));
        customChildrenOrderMap.put("blazeandcave:technical/you_are_a_big_cheater", List.of(
            "blazeandcave:statistics/the_first_night",
            "blazeandcave:statistics/spring_in_your_step",
            "blazeandcave:statistics/om_nom_nom",
            "blazeandcave:statistics/mob_hunter",
            "blazeandcave:statistics/level_up",
            "blazeandcave:statistics/loot_em",
            "blazeandcave:statistics/the_parrots_and_the_bats",
            "blazeandcave:statistics/novice_enchanter",
            "blazeandcave:statistics/the_haggler"
        ));
        customChildrenOrderMap.put("blazeandcave:challenges/root", List.of(
            "blazeandcave:challenges/nuclear_fusion",
            "blazeandcave:challenges/ad_astra",
            "blazeandcave:challenges/all_the_blocks",
            "blazeandcave:challenges/constellation",
            "blazeandcave:challenges/ultimate_enchanter",
            "blazeandcave:challenges/i_am_loot",
            "blazeandcave:challenges/telescopic",
            "blazeandcave:challenges/were_in_the_endgame_now"
        ));
    }

    @Unique
    private List<AdvancementNode> sortedChildren = null;

    @Shadow
    @Final
    public Set<AdvancementNode> children;

    @Shadow
    @Final
    private AdvancementHolder holder;

    @Shadow
    public abstract AdvancementNode root();

    @Inject(
        method = "children",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sortChildren(CallbackInfoReturnable<Iterable<AdvancementNode>> cir) {
        if (children.size() <= 1) {
            return;
        }
        if (sortedChildren == null) {
            sortedChildren = new ArrayList<>(children);
            Identifier advancementId = holder.id();
            if (customChildrenOrderMap.containsKey(advancementId.toString())) {
                List<String> customChildrenIds = customChildrenOrderMap.get(advancementId.toString());
                sortedChildren.sort(Comparator.comparingInt(child ->
                    customChildrenIds.indexOf(child.holder().id().toString())
                ));
            } else {
                sortedChildren.sort(Comparator.comparing(advancement -> advancement.holder().id()));
            }
        }
        cir.setReturnValue(sortedChildren);
    }
}
