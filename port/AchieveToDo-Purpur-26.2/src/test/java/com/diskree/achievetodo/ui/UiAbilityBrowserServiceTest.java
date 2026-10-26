package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.AbilityRequirements;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ProgressionConstants;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class UiAbilityBrowserServiceTest {

    public static void run() {
        UiAbilityBrowserService service = new UiAbilityBrowserService();
        unknownAbilityDoesNotExposeIdentity(service);
        relockReturnsToDemystifiedLocked(service);
        notReadyStateDoesNotFabricateScreen(service);
        routeTransitionResetsPage(service);
        singlePageRouteDoesNotExposeNextAction(service);
    }

    private static void unknownAbilityDoesNotExposeIdentity(UiAbilityBrowserService service) {
        PlayerProgressionState state = PlayerProgressionState.create(UUID.randomUUID(), 0, demoRequirements());
        UiScreenViewModel screen = service.build("en_us", state, UiRoute.initial()).orElseThrow();
        UiAbilityViewModel unknown = findAbility(screen, AbilityType.JUMP);
        assertEquals(UiAbilityState.UNKNOWN_LOCKED, unknown.state());
        assertFalse(unknown.revealsIdentity());
        assertEquals("???", unknown.title());
        assertFalse(unknown.lore().getFirst().contains("jump"));
    }

    private static void relockReturnsToDemystifiedLocked(UiAbilityBrowserService service) {
        AbilityRequirements requirements = demoRequirements();
        PlayerProgressionState unlocked = PlayerProgressionState.create(UUID.randomUUID(), 10, requirements).withDemystified(AbilityType.OPEN_CHEST);
        PlayerProgressionState relocked = unlocked.withObtainedAdvancementCount(0);
        UiScreenViewModel screen = service.build("en_us", relocked, new UiRoute(UiAbilityGroup.BLOCKS, 0)).orElseThrow();
        UiAbilityViewModel openChest = findAbility(screen, AbilityType.OPEN_CHEST);
        assertEquals(UiAbilityState.DEMYSTIFIED_LOCKED, openChest.state());
        assertTrue(openChest.revealsIdentity());
    }

    private static void notReadyStateDoesNotFabricateScreen(UiAbilityBrowserService service) {
        PlayerProgressionState state = PlayerProgressionState.unavailable(UUID.randomUUID(), demoRequirements());
        Optional<UiScreenViewModel> screen = service.build("en_us", state, UiRoute.initial());
        assertTrue(screen.isEmpty());
    }

    private static void routeTransitionResetsPage(UiAbilityBrowserService service) {
        UiRoute route = new UiRoute(UiAbilityGroup.TRADING, 2);
        UiRoute updated = service.apply(route, UiSlotAction.selectGroup(UiAbilityGroup.MAIN), 1);
        assertEquals(UiAbilityGroup.MAIN, updated.group());
        assertEquals(0, updated.page());
    }

    private static void singlePageRouteDoesNotExposeNextAction(UiAbilityBrowserService service) {
        PlayerProgressionState state = PlayerProgressionState.create(UUID.randomUUID(), 0, demoRequirements());
        UiScreenViewModel screen = service.build("en_us", state, new UiRoute(UiAbilityGroup.ACTIONS, 0)).orElseThrow();
        assertFalse(screen.actions().containsKey(UiAbilityBrowserService.SLOT_NEXT));
    }

    private static UiAbilityViewModel findAbility(UiScreenViewModel screen, AbilityType abilityType) {
        return screen.abilities().values().stream()
            .filter(viewModel -> viewModel.abilityType() == abilityType)
            .findFirst()
            .orElseThrow();
    }

    private static AbilityRequirements demoRequirements() {
        Map<AbilityType, Integer> values = new EnumMap<>(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            values.put(abilityType, ProgressionConstants.INITIALLY_UNLOCKED_FLAG);
        }
        values.put(AbilityType.JUMP, 3);
        values.put(AbilityType.OPEN_CHEST, 5);
        values.put(AbilityType.VISION, ProgressionConstants.INITIALLY_UNLOCKED_FLAG);
        values.put(AbilityType.THROW_EGG, ProgressionConstants.PERMANENTLY_LOCKED_FLAG);
        return AbilityRequirements.from(values);
    }
}
