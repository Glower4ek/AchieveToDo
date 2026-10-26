package com.diskree.achievetodo.ui;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ResolvedAbilityState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiAbilityBrowserService {

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;
    private static final int[] TAB_SLOTS = {1, 2, 3, 4, 5, 6, 7};

    private final UiAbilityLayoutResolver layoutResolver;
    private final UiTextService textService;

    public UiAbilityBrowserService() {
        this(new UiAbilityLayoutResolver(), new UiTextService());
    }

    UiAbilityBrowserService(UiAbilityLayoutResolver layoutResolver, UiTextService textService) {
        this.layoutResolver = Objects.requireNonNull(layoutResolver, "layoutResolver");
        this.textService = Objects.requireNonNull(textService, "textService");
    }

    public Optional<UiScreenViewModel> build(String playerLocale, PlayerProgressionState state, UiRoute requestedRoute) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(requestedRoute, "requestedRoute");
        if (!state.isReady()) {
            return Optional.empty();
        }
        String locale = textService.normalizeLocale(playerLocale);
        UiAbilityLayout layout = layoutResolver.resolve(requestedRoute);
        Map<Integer, UiTabViewModel> tabs = buildTabs(locale, layout.route());
        Map<Integer, UiAbilityViewModel> abilities = buildAbilities(locale, state, layout);
        Map<Integer, UiSlotAction> actions = buildActions(layout.route(), layout.totalPages());
        String pageLine = textService.text(locale, "achievetodo.ui.page", layout.route().page() + 1, layout.totalPages());
        String footerLine = textService.text(locale, "achievetodo.ui.footer.obtained", state.getObtainedAdvancementCount());
        return Optional.of(new UiScreenViewModel(
            textService.resolve(locale, "achievetodo.ui.title"),
            layout.route(),
            layout.totalPages(),
            tabs,
            abilities,
            actions,
            footerLine,
            pageLine
        ));
    }

    public UiRoute apply(UiRoute currentRoute, UiSlotAction action, int totalPages) {
        Objects.requireNonNull(currentRoute, "currentRoute");
        Objects.requireNonNull(action, "action");
        return switch (action.kind()) {
            case SELECT_GROUP -> currentRoute.withGroup(action.group());
            case PREVIOUS_PAGE -> currentRoute.withPage(Math.max(0, currentRoute.page() - 1));
            case NEXT_PAGE -> currentRoute.withPage(Math.min(Math.max(0, totalPages - 1), currentRoute.page() + 1));
            case CLOSE -> currentRoute;
        };
    }

    public UiTextService textService() {
        return textService;
    }

    public UiAbilityLayoutResolver layoutResolver() {
        return layoutResolver;
    }

    private Map<Integer, UiTabViewModel> buildTabs(String locale, UiRoute route) {
        Map<Integer, UiTabViewModel> tabs = new LinkedHashMap<>();
        UiAbilityGroup[] groups = UiAbilityGroup.values();
        for (int index = 0; index < groups.length; index++) {
            UiAbilityGroup group = groups[index];
            boolean selected = group == route.group();
            List<String> lore = List.of(selected
                ? textService.resolve(locale, "achievetodo.ui.group.selected")
                : textService.resolve(locale, "achievetodo.ui.group.click"));
            tabs.put(TAB_SLOTS[index], new UiTabViewModel(
                group,
                UiIconKey.category(group),
                textService.resolve(locale, group.translationKey()),
                lore,
                selected
            ));
        }
        return tabs;
    }

    private Map<Integer, UiAbilityViewModel> buildAbilities(String locale, PlayerProgressionState state, UiAbilityLayout layout) {
        Map<Integer, UiAbilityViewModel> abilities = new LinkedHashMap<>();
        for (Map.Entry<Integer, AbilityType> entry : layout.slotToAbility().entrySet()) {
            AbilityType abilityType = entry.getValue();
            ResolvedAbilityState resolvedState = state.getAbilityState(abilityType);
            int requiredCount = state.getRequirements().getRequiredCount(abilityType);
            UiAbilityState uiState = UiAbilityStateResolver.resolve(resolvedState, requiredCount);
            abilities.put(entry.getKey(), createAbilityView(locale, state, abilityType, requiredCount, uiState));
        }
        return abilities;
    }

    private UiAbilityViewModel createAbilityView(
        String locale,
        PlayerProgressionState state,
        AbilityType abilityType,
        int requiredCount,
        UiAbilityState uiState
    ) {
        if (uiState == UiAbilityState.UNKNOWN_LOCKED) {
            return new UiAbilityViewModel(
                abilityType,
                uiState,
                UiIconKey.state("unknown"),
                textService.resolve(locale, "achievetodo.ui.unknown.name"),
                List.of(textService.resolve(locale, "achievetodo.ui.unknown.description")),
                false
            );
        }
        String nameKey = "achievetodo.ability." + abilityType.getName() + ".name";
        String descriptionKey = "achievetodo.ability." + abilityType.getName() + ".description";
        List<String> lore = new ArrayList<>();
        lore.add(textService.resolve(locale, descriptionKey));
        lore.add("");
        switch (uiState) {
            case DEMYSTIFIED_LOCKED -> {
                lore.add(textService.text(locale, "achievetodo.ui.progress", state.getObtainedAdvancementCount(), requiredCount));
                lore.add(textService.text(locale, "achievetodo.ui.remaining", Math.max(0, requiredCount - state.getObtainedAdvancementCount())));
                lore.add(textService.resolve(locale, "achievetodo.ui.state.locked"));
            }
            case UNLOCKED -> {
                lore.add(textService.text(locale, "achievetodo.ui.progress", state.getObtainedAdvancementCount(), requiredCount));
                lore.add(textService.resolve(locale, "achievetodo.ui.state.unlocked"));
            }
            case INITIALLY_UNLOCKED -> lore.add(textService.resolve(locale, "achievetodo.ui.state.default_unlocked"));
            case PERMANENTLY_LOCKED -> lore.add(textService.resolve(locale, "achievetodo.ui.state.permanently_locked"));
            default -> throw new IllegalStateException("Unhandled state " + uiState);
        }
        return new UiAbilityViewModel(
            abilityType,
            uiState,
            UiIconKey.ability(abilityType.getName()),
            textService.resolve(locale, nameKey),
            lore,
            true
        );
    }

    private Map<Integer, UiSlotAction> buildActions(UiRoute route, int totalPages) {
        Map<Integer, UiSlotAction> actions = new LinkedHashMap<>();
        UiAbilityGroup[] groups = UiAbilityGroup.values();
        for (int index = 0; index < groups.length; index++) {
            actions.put(TAB_SLOTS[index], UiSlotAction.selectGroup(groups[index]));
        }
        if (route.page() > 0) {
            actions.put(SLOT_PREVIOUS, UiSlotAction.previousPage());
        }
        actions.put(SLOT_CLOSE, UiSlotAction.close());
        if (route.page() + 1 < totalPages) {
            actions.put(SLOT_NEXT, UiSlotAction.nextPage());
        }
        return actions;
    }
}
