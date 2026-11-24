package com.diskree.achievetodo.ability;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 0.4.0 crash: the abilities tab split each category into rows of equal length and threw for LANDMARK once the abandoned
 * camp made it 23. Every category must now split, in order, into rows that differ by at most one, longer rows first.
 */
final class AbilityRowsLayoutTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void everyCategorySplitsWithoutLosingOrReorderingAbilities() {
        for (AbilitiesHierarchyLayerType category : AbilitiesHierarchyLayerType.values()) {
            List<AbilityType> abilities = Arrays.stream(AbilityType.values())
                .filter(a -> a.getHierarchyLayerType() == category).toList();
            List<List<AbilityType>> rows = category.splitIntoRows(abilities);
            assertEquals(category.getRowsCount(), rows.size(), category.name());
            List<AbilityType> joined = new ArrayList<>();
            rows.forEach(joined::addAll);
            assertEquals(abilities, joined, category.name());
            for (int i = 1; i < rows.size(); i++) {
                assertTrue(rows.get(i - 1).size() >= rows.get(i).size(), category.name());
                assertTrue(rows.getFirst().size() - rows.get(i).size() <= 1, category.name());
            }
            assertTrue(rows.stream().noneMatch(List::isEmpty), category.name());
        }
    }

    @Test
    void oddLandmarkCategoryGetsTheLongerRowFirst() {
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 23; i++) items.add(i);
        List<List<Integer>> rows = AbilitiesHierarchyLayerType.LANDMARK.splitIntoRows(items);
        assertEquals(12, rows.get(0).size());
        assertEquals(11, rows.get(1).size());
        assertEquals(List.of(12, 13), rows.get(1).subList(0, 2));
    }
}
