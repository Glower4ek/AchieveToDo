package com.diskree.achievetodo.bridge;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import java.util.*;

public final class BridgeAuthoritativeProjectionTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("projection adapter check "+checks);}
    public static void main(String[] args){
        UUID player=UUID.randomUUID(),world=UUID.randomUUID(),config=UUID.randomUUID();
        var identity=new ProgressionWorldIdentity(world,"minecraft:overworld");var group=new ProgressionWorldGroup("group",config,Map.of("overworld",identity));
        Map<String,String> files=new HashMap<>();
        var repository=new ProgressionConfigRepository(){public Optional<String> read(String name){return Optional.ofNullable(files.get(name));}public void write(String name,String value){files.put(name,value);}};
        var service=new ProgressionConfigService();
        for(String mode:List.of("easy","medium","hard","chaos","custom")) {
            files.put("custom.toml","version = 1\n[abilities]\njump = -2\nvision = -1\nopen_chest = -2147483648\n");
            var resolved=service.resolve(new WorldProgressionConfig(mode),777,repository,true);
            if(mode.equals("chaos")) {
                String saved=files.get("chaos_777.toml").replaceFirst("jump = [-0-9]+","jump = 913");files.put("chaos_777.toml",saved);
                resolved=service.resolve(new WorldProgressionConfig(mode),777,repository,false);check(resolved.definition().requirements().getRequiredCount(AbilityType.JUMP)==913);
            }
            for(int count:new int[]{Integer.MIN_VALUE,-3,-2,-1,0,3,4,1152}) {
                var capture=new BukkitProgressionRuntime.Presentation(player,identity,group,12,resolved,"bac_advancements",count);
                var state=BridgeProgressionAdapter.project(capture);var authority=PlayerProgressionState.create(player,count,resolved.definition().requirements());
                check(state.count()==count && state.config().equals(config) && state.generation()==12 && state.abilities().size()==153);
                for(var ability:AbilityType.values()) {
                    var row=state.abilities().get(ability.getName());check(row.threshold()==resolved.definition().requirements().getRequiredCount(ability));check(row.unlocked()==authority.getAbilityState(ability).isUnlocked());
                }
                var p=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),state);check(BridgeWire.snapshot(BridgeWire.snapshot(p)).equals(p));
            }
        }
        System.out.println("S3C_AUTHORITY_ADAPTER_PASS checks="+checks+" derivedByExistingResolver=true");
    }
}
