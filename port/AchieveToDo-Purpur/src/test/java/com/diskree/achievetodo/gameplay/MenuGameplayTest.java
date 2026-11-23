package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

/** Shared numeric authority matrix and actual cancellable Paper menu events. Native effects are separate. */
public final class MenuGameplayTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("S4C menu check " + checks); checks++; }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String,Object[],Object> impl) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self,m,a) -> switch(m.getName()) {
            case "hashCode" -> System.identityHashCode(self);
            case "equals" -> self == a[0];
            case "toString" -> type.getSimpleName();
            default -> impl.apply(m.getName(),a);
        });
    }
    public static void main(String[] args) {
        var selected=GameplayMenuAbilities.abilities();
        check(selected.size()==26); check(GameplayGate.IMPLEMENTED.containsAll(selected));
        UUID id=UUID.randomUUID();
        Map<String,String> files=new HashMap<>();
        var repository=new ProgressionConfigRepository() {
            public Optional<String> read(String n) { return Optional.ofNullable(files.get(n)); }
            public void write(String n,String s) { files.put(n,s); }
        };
        StringBuilder custom=new StringBuilder("version = 1\n[abilities]\n"); int index=0;
        for(var ability:selected.stream().sorted().toList()) custom.append(ability.name().toLowerCase(Locale.ROOT))
            .append(" = ").append(new int[]{-2,-1,0,7}[index++%4]).append('\n');
        files.put("custom.toml",custom.toString());
        for(String mode:List.of("easy","medium","hard","custom","chaos")) {
            var service=new ProgressionConfigService();
            var req=service.resolve(new WorldProgressionConfig(mode),777,repository,true).definition().requirements();
            for(var ability:selected) {
                int threshold=req.getRequiredCount(ability);
                for(int count:new int[]{-3,-2,-1,0,threshold-1,threshold,threshold+1,threshold-1}) {
                    var state=PlayerProgressionState.create(id,count,req);
                    check(GameplayGate.allows(state,id,false,ability)==(threshold==0 || threshold!=-1 && count>=threshold));
                }
                var state=PlayerProgressionState.create(id,10000,req);
                check(!GameplayGate.allows(null,id,false,ability));
                check(!GameplayGate.allows(state.withReady(false),id,false,ability));
                check(!GameplayGate.allows(state,UUID.randomUUID(),false,ability));
                check(GameplayGate.allows(null,id,true,ability));
            }
            check(req.asMap().equals(service.resolve(new WorldProgressionConfig(mode),777,repository,false).definition().requirements().asMap()));
        }
        PlayerProgressionState[] state={null}; GameMode[] mode={GameMode.SURVIVAL};
        var player=proxy(Player.class,(n,a)->switch(n){case "getUniqueId"->id;case "getGameMode"->mode[0];default->null;});
        var listener=new BukkitExpandedGameplayListener(actor->state[0]);
        var requirements=AbilityRequirements.normalize(selected.stream().collect(java.util.stream.Collectors.toMap(a->a,a->7)));
        for(var entry:GameplayMenuAbilities.BLOCKS.entrySet()) {
            Material[] material={entry.getValue().material()};
            var block=proxy(Block.class,(n,a)->n.equals("getType")?material[0]:null);
            var world=proxy(World.class,(n,a)->n.equals("getBlockAt")?block:null);
            Location[] location={new Location(world,2,5,0)};
            var inventory=proxy(Inventory.class,(n,a)->switch(n){case "getType"->entry.getKey();case "getLocation"->location[0];default->null;});
            check(GameplayMenuAbilities.opening(inventory)==entry.getValue().ability());
            eventMatrix(listener,player,inventory,state,id,requirements,mode);
            material[0]=Material.DIRT; check(GameplayMenuAbilities.opening(inventory)==null);
            material[0]=entry.getValue().material(); location[0]=null; check(GameplayMenuAbilities.opening(inventory)==null);
            if(entry.getValue().ability()==AbilityType.OPEN_ANVIL) {
                location[0]=new Location(world,2,5,0);
                for(var variant:List.of(Material.CHIPPED_ANVIL,Material.DAMAGED_ANVIL)) {
                    material[0]=variant;check(GameplayMenuAbilities.opening(inventory)==AbilityType.OPEN_ANVIL);
                }
            }
        }
        for(var entry:GameplayMenuAbilities.PROFESSIONS.entrySet()) {
            Villager.Profession[] profession={entry.getKey()};
            var merchant=proxy(Villager.class,(n,a)->n.equals("getProfession")?profession[0]:null);
            check(GameplayMenuAbilities.merchant(merchant)==entry.getValue());
            var inventory=proxy(MerchantInventory.class,(n,a)->n.equals("getMerchant")?merchant:null);
            eventMatrix(listener,player,inventory,state,id,requirements,mode);
            // Profession is sampled on every opening, never cached at interaction time.
            profession[0]=Villager.Profession.NONE;check(GameplayMenuAbilities.opening(inventory)==null);
            profession[0]=Villager.Profession.NITWIT;check(GameplayMenuAbilities.opening(inventory)==null);
        }
        var trader=proxy(WanderingTrader.class,(n,a)->null);
        eventMatrix(listener,player,proxy(MerchantInventory.class,(n,a)->n.equals("getMerchant")?trader:null),state,id,requirements,mode);
        check(GameplayMenuAbilities.merchant(proxy(Merchant.class,(n,a)->null))==null);
        for(var type:List.of(InventoryType.CRAFTING,InventoryType.CHEST,InventoryType.ENDER_CHEST,InventoryType.SHULKER_BOX)) {
            check(GameplayMenuAbilities.opening(proxy(Inventory.class,(n,a)->n.equals("getType")?type:null))==null);
        }
        System.out.println("S4C_MENU_CONTRACT_PASS checks="+checks+" addedIds="+selected.size()+" nativeRuntime=SEPARATE");
    }
    private static void eventMatrix(BukkitExpandedGameplayListener listener, Player player, Inventory inventory,
            PlayerProgressionState[] state, UUID id, AbilityRequirements req, GameMode[] mode) {
        var view=proxy(InventoryView.class,(n,a)->switch(n){case "getPlayer"->player;case "getTopInventory"->inventory;default->null;});
        for(int count:new int[]{-99,6,7,6}) {
            mode[0]=GameMode.SURVIVAL;state[0]=count==-99?null:PlayerProgressionState.create(id,count,req);
            var event=new InventoryOpenEvent(view);listener.open(event);check(event.isCancelled()==(count<7));
            event.setCancelled(true);listener.open(event);check(event.isCancelled());
        }
        for(var bypass:List.of(GameMode.CREATIVE,GameMode.SPECTATOR)) {
            mode[0]=bypass;state[0]=null;
            var event=new InventoryOpenEvent(view);listener.open(event);check(!event.isCancelled());
        }
        mode[0]=GameMode.SURVIVAL;
    }
}
