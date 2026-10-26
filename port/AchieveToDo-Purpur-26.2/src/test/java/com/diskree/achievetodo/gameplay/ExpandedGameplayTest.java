package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.*;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.*;

/** Independent boundary oracles plus real Paper event objects, without a simulated Minecraft result. */
public final class ExpandedGameplayTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("S4B check " + checks); checks++; }
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
        var selected=EnumSet.copyOf(GameplayGate.IMPLEMENTED);
        selected.removeAll(GameplayMenuAbilities.abilities());
        selected.removeAll(NativeFoodAbilities.abilities());
        selected.removeAll(NativeEquipmentAbilities.abilities());
        selected.removeAll(NativeActionBindings.ABILITIES);
        selected.removeAll(NativeToolBindings.ABILITIES);
        selected.removeAll(NativeComplexBindings.ABILITIES);
        selected.removeAll(NativePortalBindings.ABILITIES);
        selected.removeAll(NativeMovementBindings.ABILITIES);
        selected.removeAll(GameplayGate.CLIENT_DEPENDENT);
        selected.removeAll(Set.of(AbilityType.BREAK_BLOCKS,AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y,AbilityType.SLEEP,AbilityType.GET_INTO_BOAT));
        check(selected.size()==12);
        UUID id=UUID.randomUUID();
        Map<String,String> files=new HashMap<>();
        var repository=new ProgressionConfigRepository() {
            public Optional<String> read(String n) { return Optional.ofNullable(files.get(n)); }
            public void write(String n,String s) { files.put(n,s); }
        };
        int index=0;StringBuilder custom=new StringBuilder("version = 1\n[abilities]\n");
        for(var a:selected)custom.append(a.name().toLowerCase(Locale.ROOT)).append(" = ").append(new int[]{-2,-1,0,7}[index++%4]).append('\n');
        files.put("custom.toml",custom.toString());
        var service=new ProgressionConfigService();
        for(String mode:List.of("easy","medium","hard","custom","chaos")) {
            var req=service.resolve(new WorldProgressionConfig(mode),777,repository,true).definition().requirements();
            for(var a:selected) {
                int threshold=req.getRequiredCount(a);
                for(int count:new int[]{Integer.MIN_VALUE,-3,-2,-1,0,threshold-1,threshold,threshold+1,1152,threshold-1}) {
                    var state=PlayerProgressionState.create(id,count,req);
                    check(GameplayGate.allows(state,id,false,a)==(threshold==0 || threshold!=-1 && count>=threshold));
                    check(!GameplayGate.allows(null,id,false,a));
                    check(!GameplayGate.allows(state.withReady(false),id,false,a));
                    check(!GameplayGate.allows(state,UUID.randomUUID(),false,a));
                    check(GameplayGate.allows(null,id,true,a));
                }
            }
            check(req.asMap().equals(service.resolve(new WorldProgressionConfig(mode),777,repository,false).definition().requirements().asMap()));
        }
        var req=AbilityRequirements.normalize(selected.stream().collect(java.util.stream.Collectors.toMap(a->a,a->7)));
        PlayerProgressionState[] state={null};GameMode[] mode={GameMode.SURVIVAL};
        var player=proxy(Player.class,(n,a)->switch(n){case "getUniqueId"->id;case "getGameMode"->mode[0];default->null;});
        var listener=new BukkitExpandedGameplayListener(actor->state[0]);
        var runtime=new BukkitGameplayRuntime(null,actor->state[0]);
        for(int count:new int[]{-99,6,7,6,100}) {
            state[0]=count==-99?null:PlayerProgressionState.create(id,count,req);
            boolean allowed=count>=7;
            for(Class<? extends BlockData> dataType:List.<Class<? extends BlockData>>of(Door.class,TrapDoor.class,Gate.class)) {
                for(boolean open:new boolean[]{false,true}) {
                    var data=proxy(dataType,(n,a)->n.equals("isOpen")?open:null);
                    var block=proxy(Block.class,(n,a)->n.equals("getBlockData")?data:null);
                    for(var hand:List.of(EquipmentSlot.HAND,EquipmentSlot.OFF_HAND)) {
                        var e=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,null,block,BlockFace.UP,hand);
                        e.setUseItemInHand(Event.Result.DENY);listener.interact(e);
                        check((e.useInteractedBlock()==Event.Result.DENY)==(!open&&!allowed));
                        check(e.useItemInHand()==Event.Result.DENY);
                        var cancelled=new PlayerInteractEvent(player,Action.RIGHT_CLICK_BLOCK,null,block,BlockFace.UP,hand);
                        cancelled.setCancelled(true);listener.interact(cancelled);check(cancelled.isCancelled());
                        var left=new PlayerInteractEvent(player,Action.LEFT_CLICK_BLOCK,null,block,BlockFace.UP,hand);
                        listener.interact(left);check(left.useInteractedBlock()!=Event.Result.DENY);
                    }
                }
            }
            for(var type:List.of(Snowball.class,Egg.class,EnderPearl.class,WindCharge.class)) {
                var projectile=proxy(type,(n,a)->n.equals("getShooter")?player:null);
                var launch=new PlayerLaunchProjectileEvent(player,null,projectile);listener.throwItem(launch);check(launch.isCancelled()!=allowed);
                var generic=new ProjectileLaunchEvent(projectile);listener.launch(generic);check(generic.isCancelled()!=allowed);
                generic.setCancelled(true);listener.launch(generic);check(generic.isCancelled());
                var dispenser=proxy(type,(n,a)->null);var nonPlayer=new ProjectileLaunchEvent(dispenser);listener.launch(nonPlayer);check(!nonPlayer.isCancelled());
            }
            var other=new ProjectileLaunchEvent(proxy(Arrow.class,(n,a)->n.equals("getShooter")?player:null));listener.launch(other);check(!other.isCancelled());
            var breeze=new ProjectileLaunchEvent(proxy(BreezeWindCharge.class,(n,a)->n.equals("getShooter")?player:null));listener.launch(breeze);check(!breeze.isCancelled());
            for(var fishState:PlayerFishEvent.State.values()) {
                var e=new PlayerFishEvent(player,null,proxy(FishHook.class,(n,a)->null),EquipmentSlot.OFF_HAND,fishState);listener.fish(e);
                check(e.isCancelled()==(fishState==PlayerFishEvent.State.FISHING&&!allowed));
                e.setCancelled(true);listener.fish(e);check(e.isCancelled());
            }
            for(var type:List.of(Chest.class,Barrel.class,Furnace.class)) {
                var holder=proxy(type,(n,a)->n.equals("getType")?Material.FURNACE:null);
                var inventory=proxy(Inventory.class,(n,a)->n.equals("getHolder")?holder:null);
                var view=proxy(InventoryView.class,(n,a)->switch(n){case "getPlayer"->player;case "getTopInventory"->inventory;default->null;});
                var e=new InventoryOpenEvent(view);listener.open(e);check(e.isCancelled()!=allowed);
                e.setCancelled(true);listener.open(e);check(e.isCancelled());
            }
            var doubleChest=new DoubleChest(proxy(DoubleChestInventory.class,(n,a)->null));
            var doubleInventory=proxy(Inventory.class,(n,a)->n.equals("getHolder")?doubleChest:null);
            var doubleView=proxy(InventoryView.class,(n,a)->switch(n){case "getPlayer"->player;case "getTopInventory"->doubleInventory;default->null;});
            var doubleOpen=new InventoryOpenEvent(doubleView);listener.open(doubleOpen);check(doubleOpen.isCancelled()!=allowed);
            var cart=proxy(RideableMinecart.class,(n,a)->null);
            var mount=new EntityMountEvent(player,cart);runtime.mount(mount);check(mount.isCancelled()!=allowed);
            var enter=new VehicleEnterEvent(cart,player);runtime.enter(enter);check(enter.isCancelled()!=allowed);
            var storage=new VehicleEnterEvent(proxy(StorageMinecart.class,(n,a)->null),player);runtime.enter(storage);check(!storage.isCancelled());
        }
        check(BukkitExpandedGameplayListener.containerAbility(proxy(Furnace.class,(n,a)->n.equals("getType")?Material.SMOKER:null))==null);
        check(BukkitExpandedGameplayListener.containerAbility(proxy(InventoryHolder.class,(n,a)->null))==null);
        check(BukkitExpandedGameplayListener.containerAbility(proxy(StorageMinecart.class,(n,a)->null))==null);
        check(BukkitExpandedGameplayListener.containerAbility(null)==null);
        for(var bypass:List.of(GameMode.CREATIVE,GameMode.SPECTATOR)) {
            mode[0]=bypass;state[0]=null;
            var cast=new PlayerFishEvent(player,null,proxy(FishHook.class,(n,a)->null),EquipmentSlot.HAND,PlayerFishEvent.State.FISHING);listener.fish(cast);check(!cast.isCancelled());
        }
        // Listener ownership is explicit, so lifecycle close can remove both listener instances.
        runtime.close();
        System.out.println("S4B_GAMEPLAY_CONTRACT_PASS checks="+checks+" addedIds="+selected.size()+" nativeRuntime=SEPARATE");
    }
}
