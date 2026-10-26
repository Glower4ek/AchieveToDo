package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class UiAbilityLayoutResolverTest {

    public static void run() {
        UiAbilityLayoutResolver resolver = new UiAbilityLayoutResolver();
        everyAbilityAppearsExactlyOnce(resolver);
        groupOrderIsDeterministic(resolver);
        paginationNormalizesOutOfRangePage(resolver);
    }

    private static void everyAbilityAppearsExactlyOnce(UiAbilityLayoutResolver resolver) {
        Set<AbilityType> seen = new HashSet<>();
        for (UiAbilityGroup group : UiAbilityGroup.values()) {
            int page = 0;
            while (true) {
                UiAbilityLayout layout = resolver.resolve(new UiRoute(group, page));
                for (AbilityType abilityType : layout.slotToAbility().values()) {
                    assertTrue(seen.add(abilityType), () -> "Duplicate ability in UI layout: " + abilityType);
                }
                if (page + 1 >= layout.totalPages()) {
                    break;
                }
                page++;
            }
        }
        assertEquals(AbilityType.values().length, seen.size());
    }

    private static void groupOrderIsDeterministic(UiAbilityLayoutResolver resolver) {
        List<AbilityType> main = resolver.abilitiesByGroup().get(UiAbilityGroup.MAIN);
        assertEquals(AbilityType.VISION, main.getFirst());
        assertTrue(main.indexOf(AbilityType.JUMP) > main.indexOf(AbilityType.VISION));
        assertTrue(main.indexOf(AbilityType.BREAK_BLOCKS) > main.indexOf(AbilityType.OPEN_INVENTORY));
        assertTrue(main.indexOf(AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y) > main.indexOf(AbilityType.BREAK_BLOCKS));
        assertTrue(main.indexOf(AbilityType.PUT_IN_BUNDLE) > main.indexOf(AbilityType.GET_INTO_BOAT));
    }

    private static void paginationNormalizesOutOfRangePage(UiAbilityLayoutResolver resolver) {
        UiAbilityLayout trading = resolver.resolve(new UiRoute(UiAbilityGroup.TRADING, 99));
        assertFalse(trading.slotToAbility().isEmpty());
        assertEquals(trading.totalPages() - 1, trading.route().page());
    }
}
