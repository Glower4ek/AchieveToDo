package com.diskree.achievetodo.ability;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertSame;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class AbilityModelTest {

    private AbilityModelTest() {
    }

    public static void run() {
        assertEquals(153, AbilityType.values().length);
        assertEquals(22, LandmarkType.values().length);
        assertSame(AbilityType.VISION, AbilityType.findByName("vision").orElseThrow());
        assertSame(AbilityType.OPEN_ENCHANTING_TABLE, AbilityType.findByName("open_enchanting_table").orElseThrow());
        assertSame(AbilityType.INTERACT_INSIDE_DESERT_WELL, AbilityType.findByName("interact_inside_desert_well").orElseThrow());
        assertEquals("achievetodo:abilities/vision", AbilityType.VISION.getAdvancementId());
        assertEquals("achievetodo:abilities/open_enchanting_table", AbilityType.OPEN_ENCHANTING_TABLE.getAdvancementId());
        for (AbilityType abilityType : AbilityType.values()) {
            assertSame(
                abilityType,
                AbilityResolvers.findAbilityByAdvancementId(AbilityResolvers.findAdvancementId(abilityType)).orElseThrow()
            );
        }
        assertSame(LandmarkType.DESERT_WELL, LandmarkType.findByFeatureKey("minecraft:desert_well").orElseThrow());
        assertSame(LandmarkType.MONSTER_ROOM, LandmarkType.findByFeatureKey("monster_room").orElseThrow());
        assertSame(LandmarkType.SHIPWRECK, LandmarkType.findByStructureKey("minecraft:shipwreck_beached").orElseThrow());
        assertTrue(LandmarkType.SHIPWRECK.isStructure());
        assertFalse(LandmarkType.SHIPWRECK.isFeature());
    }
}
