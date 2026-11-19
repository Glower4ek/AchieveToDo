package com.diskree.achievetodo.tracking;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public final class ProgressionCountResolver {

    public int resolvePersonalCount(String objectiveName, String holderEntry, ScoreAccess scoreAccess) {
        Objects.requireNonNull(objectiveName, "objectiveName");
        Objects.requireNonNull(holderEntry, "holderEntry");
        Objects.requireNonNull(scoreAccess, "scoreAccess");
        return scoreAccess.getScore(holderEntry, objectiveName).orElse(0);
    }

    public Optional<TeamProgressionSnapshot> resolveTeamProgress(
        String objectiveName,
        String teamName,
        Set<String> teamEntries,
        ScoreAccess scoreAccess
    ) {
        Objects.requireNonNull(objectiveName, "objectiveName");
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(teamEntries, "teamEntries");
        Objects.requireNonNull(scoreAccess, "scoreAccess");
        if (teamEntries.isEmpty()) {
            return Optional.empty();
        }
        int obtainedAdvancementCount = 0;
        for (String teamEntry : teamEntries) {
            obtainedAdvancementCount += scoreAccess.getScore(teamEntry, objectiveName).orElse(0);
        }
        return Optional.of(new TeamProgressionSnapshot(teamName, obtainedAdvancementCount, teamEntries));
    }

    public OptionalInt resolveObtainedAdvancementCount(
        DetectedAdvancementsObjective detectedObjective,
        String holderEntry,
        String teamName,
        Set<String> teamEntries,
        ScoreAccess scoreAccess
    ) {
        Objects.requireNonNull(detectedObjective, "detectedObjective");
        Objects.requireNonNull(holderEntry, "holderEntry");
        Objects.requireNonNull(scoreAccess, "scoreAccess");
        if (!detectedObjective.mode().isTeamsMode()) {
            return OptionalInt.of(resolvePersonalCount(detectedObjective.objectiveName(), holderEntry, scoreAccess));
        }
        if (teamName == null || teamEntries == null || teamEntries.isEmpty()) {
            return OptionalInt.of(0);
        }
        return resolveTeamProgress(detectedObjective.objectiveName(), teamName, teamEntries, scoreAccess)
            .map(snapshot -> OptionalInt.of(snapshot.obtainedAdvancementCount()))
            .orElseGet(OptionalInt::empty);
    }
}
