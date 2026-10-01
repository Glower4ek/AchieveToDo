package com.diskree.achievetodo.certification;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LocationTriggerProbe {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private static final Set<Identifier> TRACKED_ADVANCEMENTS = ConcurrentHashMap.newKeySet();

    private LocationTriggerProbe() {
    }

    public static void setTrackedAdvancements(Collection<Identifier> trackedAdvancements) {
        TRACKED_ADVANCEMENTS.clear();
        TRACKED_ADVANCEMENTS.addAll(trackedAdvancements);
    }

    public static void addTrackedAdvancements(Collection<Identifier> trackedAdvancements) {
        TRACKED_ADVANCEMENTS.addAll(trackedAdvancements);
    }

    public static boolean isTrackedAdvancement(Identifier advancementId) {
        return TRACKED_ADVANCEMENTS.contains(advancementId);
    }

    public static State reset(UUID playerId) {
        State state = new State();
        STATES.put(playerId, state);
        return state;
    }

    public static State get(UUID playerId) {
        return STATES.computeIfAbsent(playerId, ignored -> new State());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static final class State {
        private final Set<Identifier> registeredLocationAdvancements = ConcurrentHashMap.newKeySet();
        private volatile int anyPlayerTriggerCalls;
        private volatile int locationTriggerCalls;
        private volatile int tickTriggerCalls;
        private volatile int otherPlayerTriggerCalls;
        private volatile String lastPlayerTriggerType = "unknown";
        private volatile String recentTickTriggerTickCounts = "";
        private volatile String lastTriggerPosition = "unknown";
        private volatile String lastTriggerBiome = "unknown";
        private volatile String lastTriggerGameMode = "unknown";

        public boolean isLocationListenerRegistered(Identifier advancementId) {
            return registeredLocationAdvancements.contains(advancementId);
        }

        public Set<Identifier> getRegisteredLocationAdvancements() {
            return Collections.unmodifiableSet(registeredLocationAdvancements);
        }

        public int getLocationTriggerCalls() {
            return locationTriggerCalls;
        }

        public int getAnyPlayerTriggerCalls() {
            return anyPlayerTriggerCalls;
        }

        public int getTickTriggerCalls() {
            return tickTriggerCalls;
        }

        public int getOtherPlayerTriggerCalls() {
            return otherPlayerTriggerCalls;
        }

        public String getLastPlayerTriggerType() {
            return lastPlayerTriggerType;
        }

        public String getRecentTickTriggerTickCounts() {
            return recentTickTriggerTickCounts;
        }

        public String getLastTriggerPosition() {
            return lastTriggerPosition;
        }

        public String getLastTriggerBiome() {
            return lastTriggerBiome;
        }

        public String getLastTriggerGameMode() {
            return lastTriggerGameMode;
        }

        public void markLocationListenerRegistered(Identifier advancementId) {
            registeredLocationAdvancements.add(advancementId);
        }

        public void recordPlayerTriggerType(String triggerType, boolean isLocation, boolean isTick) {
            anyPlayerTriggerCalls++;
            lastPlayerTriggerType = triggerType;
            if (isLocation) {
                locationTriggerCalls++;
            } else if (isTick) {
                tickTriggerCalls++;
            } else {
                otherPlayerTriggerCalls++;
            }
        }

        public void recordTickTriggerTickCount(int tickCount) {
            recentTickTriggerTickCounts = appendRecent(recentTickTriggerTickCounts, Integer.toString(tickCount), 8);
        }

        public void recordLocationTrigger(ServerPlayer player) {
            lastTriggerPosition = formatPosition(player);
            lastTriggerBiome = formatBiome(player);
            lastTriggerGameMode = player.gameMode().getName();
        }

        public String describe() {
            return "registeredLocationAdvancements=" + registeredLocationAdvancements +
                ", anyPlayerTriggerCalls=" + anyPlayerTriggerCalls +
                ", locationTriggerCalls=" + locationTriggerCalls +
                ", tickTriggerCalls=" + tickTriggerCalls +
                ", otherPlayerTriggerCalls=" + otherPlayerTriggerCalls +
                ", lastPlayerTriggerType=" + lastPlayerTriggerType +
                ", recentTickTriggerTickCounts=" + recentTickTriggerTickCounts +
                ", lastTriggerPosition=" + lastTriggerPosition +
                ", lastTriggerBiome=" + lastTriggerBiome +
                ", lastTriggerGameMode=" + lastTriggerGameMode;
        }
    }

    private static String formatPosition(ServerPlayer player) {
        return player.position().x + "," + player.position().y + "," + player.position().z;
    }

    private static String formatBiome(ServerPlayer player) {
        var biome = player.level().getBiome(player.blockPosition());
        return biome.unwrapKey().map(Object::toString).orElse("unknown");
    }

    private static String appendRecent(String current, String next, int maxItems) {
        if (current.isEmpty()) {
            return next;
        }
        String[] parts = current.split(",");
        if (parts.length >= maxItems) {
            return String.join(",", java.util.Arrays.copyOfRange(parts, 1, parts.length)) + "," + next;
        }
        return current + "," + next;
    }
}
