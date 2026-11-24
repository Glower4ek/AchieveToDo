package com.diskree.achievetodo.ability;

import java.util.ArrayList;
import java.util.List;

public enum AbilitiesHierarchyLayerType {

    MAIN(1),
    ACTIONS(2),
    BLOCKS(2),
    UPGRADE(2),
    FOOD(4),
    TRADING(2),
    LANDMARK(2);

    private final int rowsCount;

    AbilitiesHierarchyLayerType(int rowsCount) {
        this.rowsCount = rowsCount;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    /**
     * Splits a category, in order, into {@link #getRowsCount()} rows whose lengths differ by at most one; the longer
     * rows come first. 0.4.0: the abandoned camp made LANDMARK odd (23), which the old even-only split refused.
     */
    public <T> List<List<T>> splitIntoRows(List<T> abilities) {
        int base = abilities.size() / rowsCount;
        int longer = abilities.size() % rowsCount;
        List<List<T>> rows = new ArrayList<>(rowsCount);
        int from = 0;
        for (int i = 0; i < rowsCount; i++) {
            int to = from + base + (i < longer ? 1 : 0);
            rows.add(new ArrayList<>(abilities.subList(from, to)));
            from = to;
        }
        return rows;
    }
}
