package com.diskree.achievetodo.client.gui;

import com.diskree.achievetodo.server.Constants;

/** Discovery controls identity only; unlocking remains server authoritative. */
public final class AbilityPresentationState {
    public static boolean mystified(int threshold, boolean unlocked, boolean demystified) {
        return threshold != Constants.Progression.INITIALLY_UNLOCKED_FLAG
            && threshold != Constants.Progression.PERMANENTLY_LOCKED_FLAG
            && !unlocked && !demystified;
    }

    private AbilityPresentationState() {}
}
