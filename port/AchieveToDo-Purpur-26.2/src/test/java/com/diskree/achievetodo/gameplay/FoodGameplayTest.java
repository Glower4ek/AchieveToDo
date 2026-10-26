package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

public final class FoodGameplayTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("S4D food check " + checks); checks++; }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String,Object[],Object> impl) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self,m,a) -> switch(m.getName()) {
            case "hashCode" -> System.identityHashCode(self); case "equals" -> self == a[0];
            case "toString" -> type.getSimpleName(); default -> impl.apply(m.getName(),a);
        });
    }
    private static final class Stack extends ItemStack {
        @Override public boolean isEmpty() { return false; }
        @Override public ItemStack clone() { return this; }
    }
    public static void main(String[] args) {
        var selected=NativeFoodAbilities.abilities();check(selected.size()==40);check(GameplayGate.IMPLEMENTED.size()==131);
        UUID id=UUID.randomUUID();Map<String,String> files=new HashMap<>();
        var repository=new ProgressionConfigRepository() {
            public Optional<String> read(String n) { return Optional.ofNullable(files.get(n)); }
            public void write(String n,String s) { files.put(n,s); }
        };
        StringBuilder custom=new StringBuilder("version = 1\n[abilities]\n");int index=0;
        for(var ability:selected.stream().sorted().toList())custom.append(ability.name().toLowerCase(Locale.ROOT)).append(" = ")
            .append(new int[]{-2,-1,0,7}[index++%4]).append('\n');
        files.put("custom.toml",custom.toString());
        for(String config:List.of("easy","medium","hard","custom","chaos")) {
            var service=new ProgressionConfigService();var req=service.resolve(new WorldProgressionConfig(config),777,repository,true).definition().requirements();
            for(var ability:selected) {
                int t=req.getRequiredCount(ability);
                for(int count:new int[]{-3,-2,-1,0,t-1,t,t+1,t-1}) {
                    var state=PlayerProgressionState.create(id,count,req);
                    check(GameplayGate.allows(state,id,false,ability)==(t==0 || t!=-1 && count>=t));
                }
                var state=PlayerProgressionState.create(id,10000,req);
                check(!GameplayGate.allows(null,id,false,ability));check(!GameplayGate.allows(state.withReady(false),id,false,ability));
                check(!GameplayGate.allows(state,UUID.randomUUID(),false,ability));check(GameplayGate.allows(null,id,true,ability));
            }
        }
        // Equality is deliberately insufficient, just as in canonical Fabric's FOOD reference comparison.
        Object original=new String("food");Object equal=new String("food");Object[] component={original};
        var resolver=new NativeFoodAbilities(Map.of(original,AbilityType.EAT_SALMON),item->component[0]);
        var stack=new Stack();check(resolver.apply(stack)==AbilityType.EAT_SALMON);component[0]=equal;check(resolver.apply(stack)==null);
        component[0]=null;check(resolver.apply(stack)==null);check(resolver.apply(null)==null);

        PlayerProgressionState[] state={null};GameMode[] mode={GameMode.SURVIVAL};ItemStack[] active={stack};
        var inventory=proxy(PlayerInventory.class,(n,a)->n.equals("getItem")?active[0]:null);
        var player=proxy(Player.class,(n,a)->switch(n){case "getUniqueId"->id;case "getGameMode"->mode[0];case "getInventory"->inventory;default->null;});
        var block=proxy(Block.class,(n,a)->null);
        AbilityType[] ability={null};
        var listener=new BukkitFoodGameplayListener(actor->state[0],item->ability[0]);
        var req=AbilityRequirements.normalize(selected.stream().collect(java.util.stream.Collectors.toMap(a->a,a->7)));
        for(var food:selected) {
            ability[0]=food;
            for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND)) {
                for(int count:new int[]{-99,6,7,6}) {
                    state[0]=count==-99?null:PlayerProgressionState.create(id,count,req);
                    for(var action:List.of(Action.RIGHT_CLICK_AIR,Action.RIGHT_CLICK_BLOCK)) {
                        var event=new PlayerInteractEvent(player,action,stack,action==Action.RIGHT_CLICK_BLOCK?block:null,BlockFace.UP,hand);
                        var before=event.useInteractedBlock();listener.begin(event);
                        check((event.useItemInHand()==Event.Result.DENY)==(count<7 && !NativeFoodAbilities.plantable(food)));check(event.useInteractedBlock()==before);
                        event.setUseItemInHand(Event.Result.DENY);listener.begin(event);check(event.useItemInHand()==Event.Result.DENY);
                    }
                    var finish=new PlayerItemConsumeEvent(player,stack,hand);listener.finish(finish);check(finish.isCancelled()==(count<7));
                    finish.setCancelled(true);listener.finish(finish);check(finish.isCancelled());
                }
            }
            for(var bypass:List.of(GameMode.CREATIVE,GameMode.SPECTATOR)) {
                mode[0]=bypass;state[0]=null;
                var finish=new PlayerItemConsumeEvent(player,stack,EquipmentSlot.HAND);listener.finish(finish);check(!finish.isCancelled());
                var begin=new PlayerInteractEvent(player,Action.RIGHT_CLICK_AIR,stack,null,BlockFace.UP,EquipmentSlot.HAND);listener.begin(begin);check(begin.useItemInHand()!=Event.Result.DENY);
            }
            mode[0]=GameMode.SURVIVAL;
        }
        state[0]=null;ability[0]=null;
        var neighbour=new PlayerItemConsumeEvent(player,stack,EquipmentSlot.HAND);listener.finish(neighbour);check(!neighbour.isCancelled());
        ability[0]=AbilityType.EAT_SALMON;
        var left=new PlayerInteractEvent(player,Action.LEFT_CLICK_BLOCK,stack,block,BlockFace.UP,EquipmentSlot.HAND);listener.begin(left);check(left.useItemInHand()!=Event.Result.DENY);
        var failing=new BukkitFoodGameplayListener(actor->state[0],item->{throw new IllegalStateException("unavailable native contract");});
        var failure=new PlayerItemConsumeEvent(player,stack,EquipmentSlot.HAND);failing.finish(failure);check(failure.isCancelled());
        // A plugin replacing an unlocked active food with a locked food cannot bypass completion.
        var replacement=new Stack();
        var mixed=new BukkitFoodGameplayListener(actor->PlayerProgressionState.create(id,7,
            AbilityRequirements.normalize(Map.of(AbilityType.EAT_SALMON,7,AbilityType.EAT_COD,8))),
            item->item==replacement?AbilityType.EAT_COD:AbilityType.EAT_SALMON);
        var changed=new PlayerItemConsumeEvent(player,replacement,EquipmentSlot.HAND);mixed.finish(changed);check(changed.isCancelled());
        active[0]=replacement;changed=new PlayerItemConsumeEvent(player,stack,EquipmentSlot.HAND);mixed.finish(changed);check(changed.isCancelled());
        System.out.println("S4F_FOOD_CONTRACT_PASS checks="+checks+" foodIds="+selected.size()+" nativeRuntime=SEPARATE");
    }
}
