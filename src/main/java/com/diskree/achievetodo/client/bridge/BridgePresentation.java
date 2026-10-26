package com.diskree.achievetodo.client.bridge;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.bridge.BridgeWire;
import java.util.*;

public final class BridgePresentation {
    private static Map<AbilityType,Integer> thresholds=Map.of();
    private static Map<String,BridgeWire.Ability> last;
    private static final List<List<AbilityType>> rows=new ArrayList<>();
    public static void clear() {last=null;thresholds=Map.of();rows.clear();}
    public static Map<AbilityType,Integer> thresholds() {
        var p=BridgeClient.presentation();if(p==null) {last=null;thresholds=Map.of();rows.clear();return thresholds;}
        if(last==null || !last.equals(p.state().abilities())) {
            Map<AbilityType,Integer> map=new EnumMap<>(AbilityType.class);
            for(var a:AbilityType.values()) {var value=p.state().abilities().get(a.getName());if(value==null)throw new IllegalArgumentException("client catalog mismatch");map.put(a,value.threshold());}
            thresholds=Map.copyOf(map);last=p.state().abilities();rows.clear();
        }
        return thresholds;
    }
    public static List<List<AbilityType>> rows() {thresholds();return rows;}
    public static boolean locked(AbilityType ability) {
        var p=BridgeClient.presentation();return !BridgeClient.presentationReady() || p==null || !p.state().abilities().get(ability.getName()).unlocked();
    }
    private BridgePresentation() {}
}
