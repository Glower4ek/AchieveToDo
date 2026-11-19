package com.diskree.achievetodo.tracking;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AdvancementsMode {

    DEFAULT("bac_advancements", false),
    FIRST_ADVANCEMENTS("bac_advfirst", false),
    TEAM_ADVANCEMENTS("bac_advancements_team", true),
    TOTAL_TEAM_ADVANCEMENTS("bac_advfirst_team_sum", true),
    TEAM_FIRST_ADVANCEMENTS("bac_advfirst_sum", true),
    FIRST_ADVANCEMENTS_IN_TEAM("bac_advfirst_team", true);

    private static final Map<String, AdvancementsMode> BY_OBJECTIVE_NAME = Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(
            mode -> mode.objectiveName.toLowerCase(Locale.ROOT),
            mode -> mode
        ));

    private final String objectiveName;
    private final boolean teamsMode;

    AdvancementsMode(String objectiveName, boolean teamsMode) {
        this.objectiveName = objectiveName;
        this.teamsMode = teamsMode;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public boolean isTeamsMode() {
        return teamsMode;
    }

    public static Optional<AdvancementsMode> findByObjectiveName(String objectiveName) {
        if (objectiveName == null || objectiveName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_OBJECTIVE_NAME.get(objectiveName.trim().toLowerCase(Locale.ROOT)));
    }
}
