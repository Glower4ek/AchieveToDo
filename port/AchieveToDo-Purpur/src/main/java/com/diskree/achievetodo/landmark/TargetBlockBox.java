package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.BlockBox;

/** Canonical Utils.toBlockBox: floor(min), ceil(max)-1, followed by BoundingBox endpoint normalization. */
public final class TargetBlockBox {
    public static BlockBox fromAabb(double[] box) {
        if (box.length != 6) throw new IllegalArgumentException("AABB requires six coordinates");
        int[] p = new int[6];
        for (int i = 0; i < 6; i++) {
            if (!Double.isFinite(box[i]) || box[i] < Integer.MIN_VALUE + 1d || box[i] > Integer.MAX_VALUE - 1d)
                throw new IllegalArgumentException("Invalid target AABB");
            p[i] = i < 3 ? (int) Math.floor(box[i]) : (int) Math.ceil(box[i]) - 1;
        }
        return new BlockBox(Math.min(p[0], p[3]), Math.min(p[1], p[4]), Math.min(p[2], p[5]),
            Math.max(p[0], p[3]), Math.max(p[1], p[4]), Math.max(p[2], p[5]));
    }
    private TargetBlockBox() {}
}
