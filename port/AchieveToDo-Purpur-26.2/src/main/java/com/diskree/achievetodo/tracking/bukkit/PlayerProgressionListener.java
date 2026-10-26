package com.diskree.achievetodo.tracking.bukkit;

import java.util.UUID;

@FunctionalInterface
public interface PlayerProgressionListener {

    void onPlayerProgressionChanged(UUID playerId);
}
