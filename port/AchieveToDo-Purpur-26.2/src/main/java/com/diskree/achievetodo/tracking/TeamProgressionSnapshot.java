package com.diskree.achievetodo.tracking;

import java.util.Objects;
import java.util.Set;

public record TeamProgressionSnapshot(String teamName, int obtainedAdvancementCount, Set<String> memberEntries) {

    public TeamProgressionSnapshot {
        teamName = Objects.requireNonNull(teamName, "teamName");
        memberEntries = Set.copyOf(memberEntries);
    }
}
