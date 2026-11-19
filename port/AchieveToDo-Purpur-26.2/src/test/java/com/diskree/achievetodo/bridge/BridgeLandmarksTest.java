package com.diskree.achievetodo.bridge;
import java.util.*;

public final class BridgeLandmarksTest {
    private static int checks;
    private static void check(boolean yes) { checks++;if(!yes)throw new AssertionError("landmark projection "+checks); }
    private static void rejects(Runnable task) { try { task.run(); } catch(IllegalArgumentException expected) { checks++;return; }throw new AssertionError("expected rejection"); }
    public static void main(String[] args) {
        var projection=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),BridgeProjectionTest.fixture());
        var ref=BridgeWire.reference(projection);var boxes=new ArrayList<BridgeLandmarks.Box>();
        for(int i=0;i<300;i++)boxes.add(new BridgeLandmarks.Box("interact_inside_village",projection.world(),i,-64,-i,i+8,319,8-i));
        var snapshot=new BridgeLandmarks.Snapshot(true,boxes);var pages=BridgeLandmarks.pages(ref,1,snapshot);
        check(pages.size()==3);
        var cache=new BridgeLandmarks.Cache();
        rejects(()->cache.accept(pages.get(1),projection));
        for(int n=0;n<pages.size();n++) {
            var page=pages.get(n);byte[] bytes=BridgeLandmarks.encode(page);
            check(bytes.length<=BridgeWire.MAX_BODY);check(BridgeLandmarks.decode(bytes).equals(page));
            for(int i=0;i<bytes.length;i++) { byte[] cut=Arrays.copyOf(bytes,i);rejects(()->BridgeLandmarks.decode(cut)); }
            rejects(()->BridgeLandmarks.decode(Arrays.copyOf(bytes,bytes.length+1)));
            check(cache.accept(page,projection)==(n==2));
            check(n==2 || cache.current(projection)==null);
            check(!cache.accept(page,projection));
        }
        check(cache.current(projection).equals(snapshot));
        var next=projection.change(1,projection.state());check(cache.current(next)==null);
        check(!cache.accept(pages.get(0),next));
        var retired=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),projection.state());
        check(!cache.accept(pages.get(0),retired));check(!cache.accept(pages.get(0),null));
        var disabled=BridgeLandmarks.pages(BridgeWire.reference(next),2,BridgeLandmarks.Snapshot.disabled()).getFirst();
        check(cache.accept(disabled,next));check(!cache.current(next).enabled());check(cache.current(next).boxes().isEmpty());
        check(cache.current(projection)==null);cache.clear();check(cache.current(next)==null);
        var invalidWorld=new BridgeLandmarks.Box("interact_inside_village",UUID.randomUUID(),0,0,0,0,0,0);
        rejects(()->cache.accept(new BridgeLandmarks.Page(ref,1,true,0,1,List.of(invalidWorld)),projection));cache.clear();
        var invalidAbility=new BridgeLandmarks.Box("interact_inside_fake",projection.world(),0,0,0,0,0,0);
        rejects(()->cache.accept(new BridgeLandmarks.Page(ref,1,true,0,1,List.of(invalidAbility)),projection));cache.clear();
        rejects(()->new BridgeLandmarks.Box("open_chest",projection.world(),0,0,0,0,0,0));
        rejects(()->new BridgeLandmarks.Box("interact_inside_village",projection.world(),1,0,0,0,0,0));
        rejects(()->new BridgeLandmarks.Page(ref,-1,true,0,1,List.of()));
        rejects(()->new BridgeLandmarks.Page(ref,1,false,0,1,boxes.subList(0,1)));
        rejects(()->new BridgeLandmarks.Page(ref,1,true,0,1,boxes));
        var outer=new BridgeWire.Frame(BridgeWire.LANDMARKS,BridgeWire.MAJOR,BridgeWire.MINOR,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),BridgeWire.FEATURES,BridgeWire.CATALOG,"test",BridgeLandmarks.encode(disabled));
        check(BridgeWire.decode(BridgeWire.encode(outer)).kind()==BridgeWire.LANDMARKS);
        System.out.println("BRIDGE_LANDMARKS_PASS checks="+checks);
    }
}
