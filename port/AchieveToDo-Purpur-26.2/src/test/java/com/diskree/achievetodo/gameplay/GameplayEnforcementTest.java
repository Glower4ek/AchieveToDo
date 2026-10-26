package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

/** API callback contracts; real native transactions are verified separately in the disposable server. */
public final class GameplayEnforcementTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("S4A check " + checks); checks++; }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String,Object[],Object> impl) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self,m,a) -> switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(self);
            case "equals" -> self == a[0];
            case "toString" -> type.getSimpleName();
            default -> impl.apply(m.getName(),a);
        });
    }
    public static void main(String[] args) {
        UUID id=UUID.randomUUID();
        Map<String,String> files=new HashMap<>();
        var repository=new ProgressionConfigRepository() {
            public Optional<String> read(String n) { return Optional.ofNullable(files.get(n)); }
            public void write(String n,String s) { files.put(n,s); }
        };
        files.put("custom.toml","version = 1\n[abilities]\nbreak_blocks = -2\nbreak_blocks_in_negative_y = -1\nsleep = 0\nget_into_boat = 7\n");
        var service=new ProgressionConfigService();
        for (String mode:List.of("easy","medium","hard","custom","chaos")) {
            var config=service.resolve(new WorldProgressionConfig(mode),777,repository,true);
            var req=config.definition().requirements();
            for (var ability:Set.of(AbilityType.BREAK_BLOCKS, AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y,
                    AbilityType.SLEEP, AbilityType.GET_INTO_BOAT)) {
                int threshold=req.getRequiredCount(ability);
                for(int count:new int[]{Integer.MIN_VALUE,-3,-2,-1,0,threshold-1,threshold,threshold+1,1152}) {
                    var state=PlayerProgressionState.create(id,count,req);
                    boolean expected=threshold==0 || threshold!=-1 && count>=threshold;
                    check(GameplayGate.allows(state,id,false,ability)==expected);
                    check(!GameplayGate.allows(state.withReady(false),id,false,ability));
                    check(!GameplayGate.allows(state,UUID.randomUUID(),false,ability));
                    check(GameplayGate.allows(state,id,true,ability));
                }
            }
            // Mining below zero requires both permissions, not an alternative threshold.
            for (int count:new int[]{0,6,24,32,38,42,44,1000}) {
                var state=PlayerProgressionState.create(id,count,req);
                boolean ordinary=GameplayGate.allows(state,id,false,AbilityType.BREAK_BLOCKS);
                check(GameplayGate.allowsBreak(state,id,false,0)==ordinary);
                check(GameplayGate.allowsBreak(state,id,false,-1)==(ordinary&&GameplayGate.allows(state,id,false,AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y)));
            }
            var reloaded=service.resolve(new WorldProgressionConfig(mode),777,repository,false);
            check(req.asMap().equals(reloaded.definition().requirements().asMap()));
        }
        // Exercise actual adapter callbacks with an evolving authority, including missing state and relock.
        int[] y={0};
        var block=proxy(Block.class,(n,a)->n.equals("getY")?y[0]:null);
        GameMode[] mode={GameMode.SURVIVAL};
        var player=proxy(Player.class,(n,a)->switch(n) { case "getUniqueId"->id; case "getGameMode"->mode[0]; default->null; });
        var boat=proxy(Boat.class,(n,a)->null);
        var cart=proxy(Minecart.class,(n,a)->null);
        var requirements=AbilityRequirements.normalize(Map.of(AbilityType.BREAK_BLOCKS,3,AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y,5,AbilityType.SLEEP,3,AbilityType.GET_INTO_BOAT,3));
        PlayerProgressionState[] current={null};
        var runtime=new BukkitGameplayRuntime(null,actor->current[0]);
        for(int count:new int[]{-99,2,3,5,2}) {
            current[0]=count==-99?null:PlayerProgressionState.create(id,count,requirements);
            for(int height:new int[]{-1,0}) {
                y[0]=height;
                boolean allowed=count>=3 && (height>=0||count>=5);
                var damage=new BlockDamageEvent(player,block,org.bukkit.block.BlockFace.UP,null,false);runtime.damage(damage);check(damage.isCancelled()!=allowed);
                var destroy=new BlockBreakEvent(block,player);runtime.destroy(destroy);check(destroy.isCancelled()!=allowed);
            }
            var bed=new PlayerBedEnterEvent(player,block,PlayerBedEnterEvent.BedEnterResult.OK,proxy(io.papermc.paper.block.bed.BedEnterAction.class,(n,a)->n.equals("canSleep")?io.papermc.paper.block.bed.BedRuleResult.ALLOWED:null));
            runtime.sleep(bed);check(bed.isCancelled()!=(count>=3));
            var enter=new VehicleEnterEvent(boat,player);runtime.enter(enter);check(enter.isCancelled()!=(count>=3));
            var mount=new EntityMountEvent(player,boat);runtime.mount(mount);check(mount.isCancelled()!=(count>=3));
            var unrelated=new VehicleEnterEvent(cart,player);runtime.enter(unrelated);check(!unrelated.isCancelled());
        }
        // Previously cancelled events must never be re-enabled by unlocked checks.
        current[0]=PlayerProgressionState.create(id,100,requirements);
        var cancelled=new BlockBreakEvent(block,player);cancelled.setCancelled(true);runtime.destroy(cancelled);check(cancelled.isCancelled());
        check(!GameplayGate.allows(null,id,false,AbilityType.SLEEP));
        System.out.println("S4A_GAMEPLAY_CONTRACT_PASS checks="+checks+" modes=EASY,MEDIUM,HARD,CUSTOM,CHAOS nativeRuntime=SEPARATE");
        ExpandedGameplayTest.main(args);
        MenuGameplayTest.main(args);
        FoodGameplayTest.main(args);
        NativeTransactionGameplayTest.main(args);
        NativeActionGameplayTest.main(args);
        NativeToolGameplayTest.main(args);
        NativeComplexGameplayTest.main(args);
        NativePortalGameplayTest.main(args);
        NativeMovementGameplayTest.main(args);
        ClientDependentGameplayTest.main(args);
        com.diskree.achievetodo.gameplay.nativeguard.NativeComplexContractTest.main(args);
        com.diskree.achievetodo.gameplay.nativeguard.NativeToolContractTest.main(args);
        com.diskree.achievetodo.gameplay.nativeguard.NativeActionContractTest.main(args);
    }
}
