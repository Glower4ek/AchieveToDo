package com.diskree.achievetodo.client;

import com.diskree.achievetodo.client.gui.AbilityPresentationState;
import com.diskree.achievetodo.client.gui.AdvancementsTabType;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityPresentationStateTest {
    @Test void discoveryAndUnlockAreIndependent() {
        assertTrue(AbilityPresentationState.mystified(11, false, false));
        assertFalse(AbilityPresentationState.mystified(11, false, true));
        assertFalse(AbilityPresentationState.mystified(11, true, false));
        assertFalse(AbilityPresentationState.mystified(11, true, true));
    }

    @Test void specialFlagsKeepOriginalVisibleIdentity() {
        for (int flag : new int[]{0, -1}) {
            assertFalse(AbilityPresentationState.mystified(flag, false, false));
            assertFalse(AbilityPresentationState.mystified(flag, false, true));
        }
    }

    @Test void thresholdsDoNotChangeDiscoveryRules() {
        for (int threshold : new int[]{1, 11, 319, 1152, -2, Integer.MIN_VALUE}) {
            assertTrue(AbilityPresentationState.mystified(threshold, false, false));
            assertFalse(AbilityPresentationState.mystified(threshold, false, true));
        }
    }

    @Test void unknownNamespaceMustNotClaimKnownTabSlots() {
        assertNull(AdvancementsTabType.findByAdvancement(Identifier.parse("other:abilities/root")));
        assertEquals(AdvancementsTabType.ADVENTURE,
            AdvancementsTabType.findByAdvancement(Identifier.parse("minecraft:adventure/root")));
        assertNull(AdvancementsTabType.findByAdvancement(Identifier.parse("blazeandcave:unknown/root")));
        assertEquals(AdvancementsTabType.ABILITIES,
            AdvancementsTabType.findByAdvancement(Identifier.parse("achievetodo:abilities/root")));
        for (AdvancementsTabType tab : AdvancementsTabType.values()) {
            assertEquals(tab, AdvancementsTabType.findByAdvancement(
                Identifier.parse("blazeandcave:" + tab.getName() + "/root")));
        }
    }
}
