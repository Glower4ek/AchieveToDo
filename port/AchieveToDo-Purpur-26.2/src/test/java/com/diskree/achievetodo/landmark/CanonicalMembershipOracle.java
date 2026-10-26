package com.diskree.achievetodo.landmark;
import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.progression.*;
import java.util.*;
/** Original canonical method body. Only networking and native types are stubbed; see source receipt. */
final class CanonicalMembershipOracle {
    @interface NotNull {}
    record ServerPlayer(UUID getUUID) {}
    record AABB(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {}
    record BoundingBox(BlockBox box) { boolean intersects(BoundingBox other) { return box.intersects(other.box); } }
    record DimensionalBlockBox(DimensionType dimensionType,BoundingBox blockBox) {}
    static class Utils { static BoundingBox toBlockBox(AABB box) { return new BoundingBox(TargetBlockBox.fromAabb(new double[]{box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ})); } }
    void notifyLockedLandmark(ServerPlayer p,DimensionType dimension,AABB box) {}
    boolean isAbilityLocked(ServerPlayer p,AbilityType type,boolean checkOnly) {
        if(!ready)return true;
        int required=abilitiesConfiguration.get(type),count=obtainedAdvancementsCountByPlayers.getOrDefault(p.getUUID(),Integer.MIN_VALUE);
        return required!=0 && (required==-1 || count<required);
    }
    void addBox(LandmarkRecord record) {
        landmarksByChunks.computeIfAbsent(0,k->new HashMap<>()).computeIfAbsent(record.landmarkType(),k->new HashSet<>())
            .add(new DimensionalBlockBox(record.world().dimensionType(),new BoundingBox(record.blockBox())));
    }
    List<LandmarkType> order() { return new ArrayList<>(playersByLockedLandmarkTypes.keySet()); }
    static class Constants { static class Progression { static final int INITIALLY_UNLOCKED_FLAG=0, PERMANENTLY_LOCKED_FLAG=-1; } }
    static class ServerPlayNetworking { static void send(Object p,Object payload) {} }
    record SyncObtainedAdvancementsCountPayload(int n) {}
    record LandmarkTypesUnlockedPayload(Set<LandmarkType> types) {}
    record LandmarksLockedStatusChangedPayload(Map<LandmarkType,Set<DimensionalBlockBox>> boxes,boolean locked) {}
    final Map<UUID,Integer> obtainedAdvancementsCountByPlayers=new HashMap<>();
    final Map<AbilityType,Integer> abilitiesConfiguration=new EnumMap<>(AbilityType.class);
    final Map<Integer,Map<LandmarkType,Set<DimensionalBlockBox>>> landmarksByChunks=new HashMap<>();
    final Map<LandmarkType,Set<UUID>> playersByLockedLandmarkTypes=new HashMap<>();
    boolean ready=true;
    boolean isNotReady() { return !ready; }
    void setAbilityLocked(ServerPlayer p,AbilityType a,boolean locked) {}
    void observe(PlayerProgressionState state) {
        ready=state.isReady();
        for (var type:AbilityType.values()) abilitiesConfiguration.put(type,state.getRequirements().getRequiredCount(type));
        setObtainedCount(new ServerPlayer(state.getPlayerId()),state.getObtainedAdvancementCount());
    }
    Set<LandmarkType> types(UUID id) {
        var result=EnumSet.noneOf(LandmarkType.class);
        playersByLockedLandmarkTypes.forEach((type,players)->{ if(players.contains(id)) result.add(type); });
        return result;
    }
    void disconnect(UUID id) {
        obtainedAdvancementsCountByPlayers.remove(id);
        for(var players:playersByLockedLandmarkTypes.values()) players.remove(id);
    }
    public void setObtainedCount(@NotNull ServerPlayer player, int obtainedCount) {
        if (isNotReady()) {
            return;
        }
        UUID playerUuid = player.getUUID();
        int oldCount = obtainedAdvancementsCountByPlayers.getOrDefault(playerUuid, Integer.MIN_VALUE);
        obtainedAdvancementsCountByPlayers.put(playerUuid, obtainedCount);
        Set<LandmarkType> unlockedLandmarkTypes = null;
        Map<LandmarkType, Set<DimensionalBlockBox>> lockedLandmarks = null;

        for (AbilityType abilityType : AbilityType.values()) {
            int requiredCount = abilitiesConfiguration.get(abilityType);
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
            ) {
                continue;
            }
            boolean isLock;
            if (obtainedCount >= requiredCount && oldCount < requiredCount) {
                isLock = false;
            } else if (oldCount == Integer.MIN_VALUE || obtainedCount < requiredCount && oldCount >= requiredCount) {
                isLock = true;
            } else {
                continue;
            }
            setAbilityLocked(player, abilityType, isLock);
            LandmarkType landmarkType = abilityType.getLandmarkType();
            if (landmarkType != null) {
                if (isLock) {
                    playersByLockedLandmarkTypes
                        .computeIfAbsent(landmarkType, k -> new HashSet<>())
                        .add(playerUuid);
                    for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                        Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                        if (dimensionalBlockBoxes != null) {
                            if (lockedLandmarks == null) {
                                lockedLandmarks = new HashMap<>();
                            }
                            lockedLandmarks.put(landmarkType, dimensionalBlockBoxes);
                        }
                    }
                } else {
                    Set<UUID> players = playersByLockedLandmarkTypes.get(landmarkType);
                    if (players != null && players.remove(playerUuid) && players.isEmpty()) {
                        playersByLockedLandmarkTypes.remove(landmarkType);
                    }
                    if (unlockedLandmarkTypes == null) {
                        unlockedLandmarkTypes = new HashSet<>();
                    }
                    unlockedLandmarkTypes.add(landmarkType);
                }
            }
        }
        ServerPlayNetworking.send(player, new SyncObtainedAdvancementsCountPayload(obtainedCount));
        if (unlockedLandmarkTypes != null) {
            ServerPlayNetworking.send(player, new LandmarkTypesUnlockedPayload(unlockedLandmarkTypes));
        }
        if (lockedLandmarks != null) {
            ServerPlayNetworking.send(player, new LandmarksLockedStatusChangedPayload(lockedLandmarks, true));
        }
    }

    public boolean isTargetInLockedLandmark(
        @NotNull ServerPlayer actor,
        @NotNull DimensionType targetDimensionType,
        @NotNull AABB targetBox,
        boolean checkOnly
    ) {
        BoundingBox targetBlockBox = Utils.toBlockBox(targetBox);
        for (var entry : playersByLockedLandmarkTypes.entrySet()) {
            if (entry.getValue().contains(actor.getUUID())) {
                LandmarkType landmarkType = entry.getKey();
                for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                    if (dimensionalBlockBoxes != null) {
                        for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                            if (dimensionalBlockBox.dimensionType() == targetDimensionType &&
                                dimensionalBlockBox.blockBox().intersects(targetBlockBox)
                            ) {
                                AbilityType abilityType = AbilityResolvers.findLandmarkAbility(landmarkType).orElse(null);
                                if (abilityType != null) {
                                    boolean isAbilityLocked = isAbilityLocked(actor, abilityType, true);
                                    if (isAbilityLocked && !checkOnly) {
                                        notifyLockedLandmark(actor, targetDimensionType, targetBox);
                                    }
                                    return isAbilityLocked;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

}
