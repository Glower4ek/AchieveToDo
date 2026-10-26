package com.diskree.achievetodo.ui;

public record UiSlotAction(UiSlotActionKind kind, UiAbilityGroup group) {

    public static UiSlotAction selectGroup(UiAbilityGroup group) {
        return new UiSlotAction(UiSlotActionKind.SELECT_GROUP, group);
    }

    public static UiSlotAction previousPage() {
        return new UiSlotAction(UiSlotActionKind.PREVIOUS_PAGE, null);
    }

    public static UiSlotAction nextPage() {
        return new UiSlotAction(UiSlotActionKind.NEXT_PAGE, null);
    }

    public static UiSlotAction close() {
        return new UiSlotAction(UiSlotActionKind.CLOSE, null);
    }
}
