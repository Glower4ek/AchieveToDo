package com.diskree.achievetodo.tracking;

import java.util.OptionalInt;

@FunctionalInterface
public interface ScoreAccess {

    OptionalInt getScore(String holderEntry, String objectiveName);
}
