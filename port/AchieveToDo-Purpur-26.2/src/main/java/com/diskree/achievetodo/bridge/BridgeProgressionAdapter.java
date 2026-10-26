package com.diskree.achievetodo.bridge;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import java.util.*;

/** Domain-to-network adapter only. All numeric eligibility uses the existing progression resolver. */
public final class BridgeProgressionAdapter {
    public static BridgeWire.State project(BukkitProgressionRuntime.Presentation capture) {
        var config=capture.config();
        var resolved=PlayerProgressionState.create(capture.player(),capture.count(),config.definition().requirements());
        Map<String,BridgeWire.Ability> rows=new TreeMap<>();
        for(var ability:AbilityType.values())rows.put(ability.getName(),new BridgeWire.Ability(config.definition().requirements().getRequiredCount(ability),resolved.getAbilityState(ability).isUnlocked()));
        Map<String,BridgeWire.Binding> bindings=new TreeMap<>();
        capture.group().worlds().forEach((role,id)->bindings.put(role,new BridgeWire.Binding(id.uuid(),id.key())));
        return new BridgeWire.State(capture.world().uuid(),capture.world().key(),capture.group().configId(),capture.group().groupId(),capture.generation(),
            config.definition().builtInMode()==null?"CUSTOM":config.definition().builtInMode().name(),config.worldSelection().configName(),capture.objective(),capture.count(),bindings,rows);
    }
    private BridgeProgressionAdapter() {}
}
