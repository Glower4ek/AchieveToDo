package com.diskree.achievetodo.bridge;
import java.util.*;
public final class BridgeProjectionTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("bridge check "+checks);}
    private static void rejects(Runnable action){try{action.run();}catch(IllegalArgumentException e){checks++;return;}throw new AssertionError("expected rejection");}
    public static BridgeWire.State fixture(){
        Map<String,BridgeWire.Ability> rows=new TreeMap<>();
        for(String id:IDS.split("\n"))rows.put(id,new BridgeWire.Ability(4,false));
        rows.put("vision",new BridgeWire.Ability(0,true));rows.put("jump",new BridgeWire.Ability(-2,true));rows.put("open_chest",new BridgeWire.Ability(Integer.MIN_VALUE,true));
        UUID world=UUID.fromString("11111111-1111-4111-8111-111111111111");
        return new BridgeWire.State(world,"minecraft:overworld",UUID.randomUUID(),"test",1,"CUSTOM","custom","bac_advancements",3,
            Map.of("overworld",new BridgeWire.Binding(world,"minecraft:overworld"),"nether",new BridgeWire.Binding(UUID.randomUUID(),"minecraft:the_nether")),rows);
    }
    public static void main(String[] args){
        var state=fixture();var p=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),state);
        check(BridgeWire.snapshot(BridgeWire.snapshot(p)).equals(p));
        check(p.state().abilities().size()==153 && p.state().abilities().get("open_chest").threshold()==Integer.MIN_VALUE);
        byte[] bytes=BridgeWire.snapshot(p);check(bytes.length<BridgeWire.MAX_BODY);
        for(int i=0;i<bytes.length;i++){byte[] cut=Arrays.copyOf(bytes,i);rejects(()->BridgeWire.snapshot(cut));}
        byte[] corrupt=bytes.clone();corrupt[corrupt.length-1]^=1;rejects(()->BridgeWire.snapshot(corrupt));
        byte[] raw=BridgeWire.projection(p),duplicate=raw.clone();
        int idOffset=new String(raw,java.nio.charset.StandardCharsets.ISO_8859_1).indexOf("eat_honey");
        System.arraycopy("eat_apple".getBytes(java.nio.charset.StandardCharsets.UTF_8),0,duplicate,idOffset,9);
        rejects(()->BridgeWire.projection(duplicate));
        byte[] invalidBoolean=raw.clone();invalidBoolean[invalidBoolean.length-1]=2;rejects(()->BridgeWire.projection(invalidBoolean));
        byte[] productReady=raw.clone();productReady[0]=7;rejects(()->BridgeWire.projection(productReady));
        rejects(()->BridgeWire.snapshot(Arrays.copyOf(bytes,bytes.length+1)));
        var rows=new TreeMap<>(state.abilities());rows.remove("jump");
        rejects(()->new BridgeWire.State(state.world(),state.worldKey(),state.config(),state.group(),1,"CUSTOM","custom",state.objective(),3,state.worlds(),rows));
        rows.put("unknown",new BridgeWire.Ability(0,true));rejects(()->new BridgeWire.State(state.world(),state.worldKey(),state.config(),state.group(),1,"CUSTOM","custom",state.objective(),3,state.worlds(),rows));
        try{p.state().abilities().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){checks++;}
        var cache=new BridgeProjectionCache();var other=new BridgeProjectionCache();cache.replace(p);cache.admit(BridgeWire.reference(p));other.replace(p);
        var changed=new BridgeWire.State(state.world(),state.worldKey(),state.config(),state.group(),2,"HARD","hard",state.objective(),7,state.worlds(),state.abilities());
        var p1=p.change(1,changed);var delta=new BridgeWire.Patch(p.epoch(),0,1,changed,BridgeWire.digest(p1));
        check(BridgeWire.patch(BridgeWire.patch(delta)).equals(delta));check(cache.apply(delta)==BridgeProjectionCache.Result.APPLIED);
        check(cache.current().state().count()==7 && cache.current().state().generation()==2 && cache.current().state().difficulty().equals("HARD"));
        check(cache.apply(delta)==BridgeProjectionCache.Result.DUPLICATE);check(other.current().equals(p) && !other.admitted());
        rejects(()->cache.apply(new BridgeWire.Patch(p.epoch(),0,1,changed,"0".repeat(64))));
        check(cache.apply(new BridgeWire.Patch(UUID.randomUUID(),1,2,changed,"0".repeat(64)))==BridgeProjectionCache.Result.STALE);
        check(cache.apply(new BridgeWire.Patch(p.epoch(),2,3,changed,"0".repeat(64)))==BridgeProjectionCache.Result.RESYNC);
        UUID token=UUID.randomUUID();cache.beginResync(token,100,100);
        var full=new BridgeWire.Projection(p.epoch(),1,UUID.randomUUID(),changed);var msg=new BridgeWire.ResyncSnapshot(token,full);
        check(BridgeWire.resyncSnapshot(BridgeWire.resyncSnapshot(msg)).equals(msg));
        check(cache.stageResync(new BridgeWire.ResyncSnapshot(UUID.randomUUID(),full),110)==BridgeProjectionCache.Result.STALE);
        check(cache.stageResync(msg,110)==BridgeProjectionCache.Result.APPLIED);check(cache.current().equals(p1));
        check(cache.stageResync(msg,111)==BridgeProjectionCache.Result.DUPLICATE);
        var ticket=new BridgeWire.Ticket(token,BridgeWire.reference(full));check(BridgeWire.ticket(BridgeWire.ticket(ticket)).equals(ticket));
        UUID initialToken=token;rejects(()->cache.commitResync(new BridgeWire.Ticket(initialToken,BridgeWire.reference(p)),112));
        check(cache.commitResync(ticket,113)==BridgeProjectionCache.Result.APPLIED && cache.admitted());
        check(cache.commitResync(ticket,114)==BridgeProjectionCache.Result.STALE);
        var destination=state.worlds().get("nether");
        var transition=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),changed.at(destination.uuid(),destination.key()));
        token=UUID.randomUUID();var tm=new BridgeWire.ResyncSnapshot(token,transition);
        check(BridgeWire.suspension(BridgeWire.suspend(tm)).transition().equals(tm));
        check(BridgeWire.suspension(BridgeWire.suspend(ticket)).control().equals(ticket));
        rejects(()->BridgeWire.suspension(new byte[]{2}));
        rejects(()->BridgeWire.suspension(new byte[]{0}));
        rejects(()->BridgeWire.suspension(Arrays.copyOf(BridgeWire.suspend(tm),BridgeWire.suspend(tm).length+1)));
        check(cache.stageTransition(tm,200,100)==BridgeProjectionCache.Result.APPLIED);check(cache.current().equals(full) && !cache.admitted());
        rejects(()->cache.stageResync(new BridgeWire.ResyncSnapshot(cache.request(),full),210));
        check(cache.commitResync(new BridgeWire.Ticket(token,BridgeWire.reference(transition)),211)==BridgeProjectionCache.Result.APPLIED);
        check(cache.current().world().equals(destination.uuid()) && !cache.current().epoch().equals(full.epoch()));
        check(cache.apply(delta)==BridgeProjectionCache.Result.STALE);
        rejects(()->cache.stageTransition(new BridgeWire.ResyncSnapshot(UUID.randomUUID(),p),220,100));
        cache.beginResync(UUID.randomUUID(),300,10);check(cache.expired(310));rejects(()->cache.stageResync(new BridgeWire.ResyncSnapshot(cache.request(),transition),310));
        cache.clear();check(cache.current()==null && !cache.admitted() && !cache.resyncPending());check(other.current().equals(p));
        var frame=new BridgeWire.Frame(BridgeWire.SNAPSHOT,BridgeWire.MAJOR,BridgeWire.MINOR,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),BridgeWire.FEATURES,BridgeWire.CATALOG,"test",bytes);
        check(BridgeWire.decode(BridgeWire.encode(frame)).body().length==bytes.length);
        check(BridgeWire.encode(frame).length<BridgeWire.MAX_FRAME);
        for(int i=0;i<160;i++){byte[] cut=Arrays.copyOf(BridgeWire.encode(frame),i);rejects(()->BridgeWire.decode(cut));}
        byte[] utf=BridgeWire.encode(frame);utf[67]=(byte)255;rejects(()->BridgeWire.decode(utf));
        System.out.println("BRIDGE13_PROJECTION_PASS checks="+checks+" snapshotBytes="+bytes.length+" ids=153 productGameplayReady=false");
    }
    private static final String IDS="""
        attack_with_mace
        attack_with_trident
        break_blocks
        break_blocks_in_negative_y
        charge_respawn_anchor
        eat_apple
        eat_baked_potato
        eat_beef
        eat_beetroot
        eat_beetroot_soup
        eat_bread
        eat_carrot
        eat_chicken
        eat_chorus_fruit
        eat_cod
        eat_cooked_beef
        eat_cooked_chicken
        eat_cooked_cod
        eat_cooked_mutton
        eat_cooked_porkchop
        eat_cooked_rabbit
        eat_cooked_salmon
        eat_cookie
        eat_dried_kelp
        eat_enchanted_golden_apple
        eat_glow_berries
        eat_golden_apple
        eat_golden_carrot
        eat_honey
        eat_melon_slice
        eat_mushroom_stew
        eat_mutton
        eat_poisonous_potato
        eat_porkchop
        eat_potato
        eat_pufferfish
        eat_pumpkin_pie
        eat_rabbit
        eat_rabbit_stew
        eat_rotten_flesh
        eat_salmon
        eat_spider_eye
        eat_suspicious_stew
        eat_sweet_berries
        eat_tropical_fish
        enter_end
        enter_nether
        equip_chainmail_armor
        equip_copper_armor
        equip_diamond_armor
        equip_elytra
        equip_golden_armor
        equip_iron_armor
        equip_leather_armor
        equip_netherite_armor
        equip_turtle_helmet
        get_into_boat
        get_into_minecart
        glide_with_fireworks
        ignite_tnt
        interact_inside_ancient_city
        interact_inside_bastion_remnant
        interact_inside_buried_treasure
        interact_inside_desert_pyramid
        interact_inside_desert_well
        interact_inside_end_city
        interact_inside_fortress
        interact_inside_igloo
        interact_inside_jungle_pyramid
        interact_inside_mansion
        interact_inside_mineshaft
        interact_inside_monster_room
        interact_inside_monument
        interact_inside_ocean_ruin
        interact_inside_pillager_outpost
        interact_inside_ruined_portal
        interact_inside_shipwreck
        interact_inside_stronghold
        interact_inside_swamp_hut
        interact_inside_trail_ruins
        interact_inside_trial_chambers
        interact_inside_village
        jump
        open_anvil
        open_barrel
        open_beacon
        open_blast_furnace
        open_brewing_stand
        open_cartography_table
        open_chest
        open_crafting_table
        open_door
        open_enchanting_table
        open_ender_chest
        open_fence_gate
        open_furnace
        open_grindstone
        open_inventory
        open_loom
        open_shulker_box
        open_smithing_table
        open_smoker
        open_stonecutter
        open_trapdoor
        place_end_crystal
        put_in_bundle
        shoot_bow
        shoot_crossbow
        sleep
        sneak
        sprint
        swim
        teleport_outer_islands
        throw_egg
        throw_ender_pearl
        throw_snowball
        throw_wind_charge
        trade_with_armorer
        trade_with_butcher
        trade_with_cartographer
        trade_with_cleric
        trade_with_farmer
        trade_with_fisherman
        trade_with_fletcher
        trade_with_leatherworker
        trade_with_librarian
        trade_with_mason
        trade_with_shepherd
        trade_with_toolsmith
        trade_with_wandering_trader
        trade_with_weaponsmith
        unlock_vault
        use_brush
        use_campfire
        use_cauldron
        use_composter
        use_copper_tools
        use_diamond_tools
        use_ender_eye
        use_fishing_rod
        use_flint_and_steel
        use_golden_tools
        use_iron_tools
        use_jukebox
        use_netherite_tools
        use_ominous_bottle
        use_shears
        use_shield
        use_spyglass
        use_stone_tools
        use_water_bucket
        use_wooden_tools
        vision
        """.strip();
}
