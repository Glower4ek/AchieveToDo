package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Map;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;

public final class ProgressionsTest {

    private ProgressionsTest() {
    }

    public static void run() {
        Map<AbilityType, Integer> progression = Progressions.getEasyProgression();
        assertEquals(AbilityType.values().length, progression.size());
        assertEquals(0, progression.get(AbilityType.VISION));
        assertEquals(0, progression.get(AbilityType.JUMP));
        assertEquals(2, progression.get(AbilityType.EAT_SALMON));
        assertEquals(42, progression.get(AbilityType.GET_INTO_BOAT));

        progression = Progressions.getNormalProgression();
        assertEquals(AbilityType.values().length, progression.size());
        assertEquals(0, progression.get(AbilityType.VISION));
        assertEquals(4, progression.get(AbilityType.JUMP));
        assertEquals(44, progression.get(AbilityType.GET_INTO_BOAT));

        progression = Progressions.getHardProgression();
        assertEquals(AbilityType.values().length, progression.size());
        assertEquals(2, progression.get(AbilityType.VISION));
        assertEquals(8, progression.get(AbilityType.JUMP));
        assertEquals(46, progression.get(AbilityType.PUT_IN_BUNDLE));
    }
}
