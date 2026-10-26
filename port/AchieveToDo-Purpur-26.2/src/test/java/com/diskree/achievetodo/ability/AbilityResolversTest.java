package com.diskree.achievetodo.ability;

import static com.diskree.achievetodo.tests.TestSupport.assertSame;

public final class AbilityResolversTest {

    private AbilityResolversTest() {
    }

    public static void run() {
        assertSame(AbilityType.OPEN_CHEST, AbilityResolvers.findBlockAbility("chest").orElseThrow());
        assertSame(AbilityType.THROW_ENDER_PEARL, AbilityResolvers.findItemAbility("ender_pearl").orElseThrow());
        assertSame(AbilityType.EAT_SALMON, AbilityResolvers.findFoodAbility("salmon").orElseThrow());
        assertSame(AbilityType.USE_IRON_TOOLS, AbilityResolvers.findToolMaterialAbility("iron").orElseThrow());
        assertSame(
            AbilityType.EQUIP_TURTLE_HELMET,
            AbilityResolvers.findEquipmentAbility(EquipmentDescriptor.ofItem("turtle_helmet")).orElseThrow()
        );
        assertSame(
            AbilityType.EQUIP_DIAMOND_ARMOR,
            AbilityResolvers.findEquipmentAbility(EquipmentDescriptor.ofMaterial("diamond")).orElseThrow()
        );
        assertSame(AbilityType.TRADE_WITH_LIBRARIAN, AbilityResolvers.findTradeAbility("librarian").orElseThrow());
        assertSame(AbilityType.TRADE_WITH_WANDERING_TRADER, AbilityResolvers.findTradeAbility("wandering_trader").orElseThrow());
        assertSame(
            AbilityType.INTERACT_INSIDE_DESERT_WELL,
            AbilityResolvers.findLandmarkAbility(LandmarkType.DESERT_WELL).orElseThrow()
        );
    }
}
