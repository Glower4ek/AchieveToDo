package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.ability.*;
import com.diskree.achievetodo.progression.*;
import java.util.*;

public final class CanonicalMembershipParityTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("Membership oracle check " + checks); checks++; }
    private static PlayerProgressionState state(UUID id, int count, int threshold) {
        var requirements = new EnumMap<AbilityType, Integer>(AbilityType.class);
        for (var type : LandmarkType.values()) requirements.put(AbilityResolvers.findLandmarkAbility(type).orElseThrow(), threshold);
        return PlayerProgressionState.create(id, count, AbilityRequirements.normalize(requirements));
    }
    public static void run() {
        UUID id = UUID.randomUUID();
        var projection = new CanonicalLandmarkMembership();
        var oracle = new CanonicalMembershipOracle();
        // Hand-selected event sequence: flags, retained memberships, signed boundaries, equal counts and config changes.
        int[][] events = {{0,-1},{0,0},{-4,-2},{-2,-2},{-4,-2},{-4,-1},{100,-1},{100,0},{100,7},
            {6,7},{6,7},{6,0},{6,-1},{7,9},{9,9},{8,9},{Integer.MIN_VALUE,7},{Integer.MIN_VALUE,-2}};
        for (int[] event : events) {
            var s = state(id,event[0],event[1]); projection.observe(s); oracle.observe(s);
            check(projection.types(id).equals(oracle.types(id)));
        }
        var before = projection.types(id);
        projection.observe(state(id,100,7).withReady(false)); oracle.observe(state(id,100,7).withReady(false));
        check(projection.types(id).equals(before)); check(projection.types(id).equals(oracle.types(id)));
        projection.disconnect(id); oracle.disconnect(id);
        check(projection.types(id).isEmpty());
        // Stable persisted CHAOS table is a requirements input; projection must not regenerate it.
        Random random = new Random(0x54_47_01);
        for (int i=0;i<256;i++) {
            int threshold = new int[]{-20,-2,-1,0,1,7,100}[random.nextInt(7)];
            var s = state(id,random.nextInt(151)-30,threshold);
            projection.observe(s); oracle.observe(s); check(projection.types(id).equals(oracle.types(id)));
        }
        var world = new LandmarkWorld("group-a-world",DimensionType.OVERWORLD);
        var box = new BlockBox(0,0,0,1,1,1);
        var village = new LandmarkRecord(LandmarkType.VILLAGE,world,box,new LandmarkSource(LandmarkSourceKind.STRUCTURE,"village_plains",new ChunkCoordinate(0,0)));
        var mine = new LandmarkRecord(LandmarkType.MINESHAFT,world,box,new LandmarkSource(LandmarkSourceKind.STRUCTURE,"mineshaft",new ChunkCoordinate(0,0)));
        projection.clear(); projection.observe(state(id,0,7));
        var changed = PlayerProgressionState.create(id,0,AbilityRequirements.normalize(Map.of(AbilityType.INTERACT_INSIDE_VILLAGE,0,AbilityType.INTERACT_INSIDE_MINESHAFT,-1)));
        projection.observe(changed);
        check(projection.ordered(changed,id,false,List.of(village,mine)) == ExactLandmarkGate.Result.UNLOCKED);
        check(projection.ordered(changed,id,false,List.of(mine,village)) == ExactLandmarkGate.Result.LOCKED);
        check(projection.evaluate(changed,id,false,Set.of(village,mine)) ==
            (projection.iterationOrder().indexOf(LandmarkType.VILLAGE) < projection.iterationOrder().indexOf(LandmarkType.MINESHAFT)
                ? ExactLandmarkGate.Result.UNLOCKED : ExactLandmarkGate.Result.LOCKED));
        check(projection.evaluate(changed,id,false,Set.of(village)) == ExactLandmarkGate.Result.UNLOCKED);
        check(projection.evaluate(changed,id,false,Set.of(mine)) == ExactLandmarkGate.Result.LOCKED);
        check(projection.evaluate(changed.withReady(false),id,false,Set.of(mine)) == ExactLandmarkGate.Result.UNAVAILABLE);
        var queryOracle = new CanonicalMembershipOracle();
        queryOracle.observe(state(id,0,7)); queryOracle.addBox(village); queryOracle.addBox(mine);
        var order = queryOracle.order();
        LandmarkRecord first = order.indexOf(LandmarkType.VILLAGE)<order.indexOf(LandmarkType.MINESHAFT) ? village : mine;
        LandmarkRecord second = first==village ? mine : village;
        var firstFalse = PlayerProgressionState.create(id,0,AbilityRequirements.normalize(Map.of(
            AbilityResolvers.findLandmarkAbility(first.landmarkType()).orElseThrow(),0,
            AbilityResolvers.findLandmarkAbility(second.landmarkType()).orElseThrow(),-1)));
        queryOracle.observe(firstFalse); // Both memberships retained: special flags are skipped.
        check(!queryOracle.isTargetInLockedLandmark(new CanonicalMembershipOracle.ServerPlayer(id),DimensionType.OVERWORLD,
            new CanonicalMembershipOracle.AABB(0,0,0,1,1,1),true));
        check(queryOracle.isAbilityLocked(new CanonicalMembershipOracle.ServerPlayer(id),
            AbilityResolvers.findLandmarkAbility(second.landmarkType()).orElseThrow(),true));
        projection.clear(); projection.observe(state(id,0,7)); projection.observe(firstFalse);
        check(projection.ordered(firstFalse,id,false,List.of(first,second))==ExactLandmarkGate.Result.UNLOCKED);
        check(projection.iterationOrder().equals(queryOracle.order()));
        check(projection.evaluate(firstFalse,id,false,Set.of(first,second))==ExactLandmarkGate.Result.UNLOCKED);
        projection.disconnect(id); projection.observe(changed);
        check(projection.evaluate(changed,id,false,Set.of(mine)) == ExactLandmarkGate.Result.OUTSIDE);
        check(projection.evaluate(changed.withReady(false),id,false,Set.of(mine)) == ExactLandmarkGate.Result.OUTSIDE);
        check(!projection.contains(UUID.randomUUID(),LandmarkType.MINESHAFT));
        check(TargetBlockBox.fromAabb(new double[]{-1.2,0,1.1,0,1,2.0}).equals(new BlockBox(-2,0,1,-1,0,1)));
        check(TargetBlockBox.fromAabb(new double[]{0,0,0,0,0,0}).equals(new BlockBox(-1,-1,-1,0,0,0)));
        projection.clear(); check(projection.types(id).isEmpty());
        var loaded = new LoadedLandmarkMembership();
        var chunk = new LandmarkChunkKey(world,new ChunkCoordinate(0,0));
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,0))).isEmpty());
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,-1))).isEmpty());
        loaded.unload(chunk);
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,-1))).contains("village"));
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,0))).contains("village"));
        check(!loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,0),
            new LoadedLandmarkMembership.Source("new-feature",true,0))).contains("new-feature"));
        check(!loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("village",true,7),
            new LoadedLandmarkMembership.Source("new-feature",true,7))).contains("new-feature"));
        loaded.clear();
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("late-hull",false,7))).isEmpty());
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("late-hull",true,7))).contains("late-hull"));
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("first-hull-at-zero",false,7))).isEmpty());
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("first-hull-at-zero",true,0))).isEmpty());
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("first-hull-at-zero",true,7))).isEmpty());
        loaded.unloadWorld(world);
        check(loaded.publish(chunk,List.of(new LoadedLandmarkMembership.Source("late-hull",true,7))).contains("late-hull"));
        var otherChunk = new LandmarkChunkKey(new LandmarkWorld("other-group",DimensionType.OVERWORLD),chunk.chunk());
        check(loaded.publish(otherChunk,List.of(new LoadedLandmarkMembership.Source("late-hull",true,0))).isEmpty());
        var tools = PlayerProgressionState.create(id,139,AbilityRequirements.normalize(Map.of(AbilityType.USE_IRON_TOOLS,140)));
        check(ExactLandmarkGate.denied(ExactLandmarkGate.Result.UNLOCKED,tools,id,AbilityType.USE_IRON_TOOLS));
        check(ExactLandmarkGate.denied(ExactLandmarkGate.Result.LOCKED,tools.withObtainedAdvancementCount(140),id,AbilityType.USE_IRON_TOOLS));
        check(!ExactLandmarkGate.denied(ExactLandmarkGate.Result.UNLOCKED,tools.withObtainedAdvancementCount(140),id,AbilityType.USE_IRON_TOOLS));
        check(!ExactLandmarkGate.denied(ExactLandmarkGate.Result.OUTSIDE,tools,id,null));
        check(ExactLandmarkGate.denied(ExactLandmarkGate.Result.OUTSIDE,tools.withReady(false),id,AbilityType.USE_IRON_TOOLS));
        for (var result : List.of(ExactLandmarkGate.Result.OUTSIDE,ExactLandmarkGate.Result.UNLOCKED)) {
            check(ExactLandmarkGate.denied(result,tools.withReady(false),id,null));
            check(ExactLandmarkGate.denied(result,null,id,null));
            check(ExactLandmarkGate.denied(result,tools,UUID.randomUUID(),null));
        }
        check(!ExactLandmarkGate.denied(ExactLandmarkGate.Result.BYPASS,null,id,null));
        sharedMapLifecycleParity();
        System.out.println("S4N_CANONICAL_MEMBERSHIP_ORACLE_PASS checks="+checks+" firstMatch=SHARED_HASHMAP");
    }

    private static void sharedMapLifecycleParity() {
        var projection = new CanonicalLandmarkMembership();
        var oracle = new CanonicalMembershipOracle();
        var world = new LandmarkWorld("oracle-world", DimensionType.OVERWORLD);
        Set<LandmarkRecord> matches = new HashSet<>();
        for (var type : LandmarkType.values()) {
            var record = new LandmarkRecord(type, world, new BlockBox(0,0,0,1,1,1),
                new LandmarkSource(type.isFeature() ? LandmarkSourceKind.FEATURE : LandmarkSourceKind.STRUCTURE,
                    type.isFeature() ? type.getFeatureKey() : type.getStructureKeys().iterator().next(), new ChunkCoordinate(0,0)));
            matches.add(record); oracle.addBox(record);
        }
        UUID[] actors = {new UUID(0,1), new UUID(0,2), new UUID(0,3)};
        var random = new Random(0x54_4e_01);
        for (int i=0; i<2048; i++) {
            UUID actor = actors[random.nextInt(actors.length)];
            if (i % 17 == 0) { projection.disconnect(actor); oracle.disconnect(actor); }
            if (i % 257 == 0) {
                projection.clear(); oracle.playersByLockedLandmarkTypes.clear(); oracle.obtainedAdvancementsCountByPlayers.clear();
            }
            var requirements = new EnumMap<AbilityType,Integer>(AbilityType.class);
            for (var type : LandmarkType.values()) requirements.put(AbilityResolvers.findLandmarkAbility(type).orElseThrow(),
                new int[]{Integer.MIN_VALUE,-20,-2,-1,0,1,7,100}[random.nextInt(8)]);
            int count = i % 31 == 0 ? Integer.MIN_VALUE : random.nextInt(151)-30;
            var state = PlayerProgressionState.create(actor,count,AbilityRequirements.normalize(requirements));
            projection.observe(state); oracle.observe(state);
            check(projection.iterationOrder().equals(oracle.order()));
            for (UUID player : actors) check(projection.types(player).equals(oracle.types(player)));
            boolean expected = oracle.isTargetInLockedLandmark(new CanonicalMembershipOracle.ServerPlayer(actor),
                DimensionType.OVERWORLD,new CanonicalMembershipOracle.AABB(0,0,0,1,1,1),true);
            var actual = projection.evaluate(state,actor,false,matches);
            check(ExactLandmarkGate.denied(actual) == expected);
            check(actual != ExactLandmarkGate.Result.UNSUPPORTED_MEMBERSHIP);
            check(projection.evaluate(state,actor,true,matches) == ExactLandmarkGate.Result.BYPASS);
        }
    }
}
