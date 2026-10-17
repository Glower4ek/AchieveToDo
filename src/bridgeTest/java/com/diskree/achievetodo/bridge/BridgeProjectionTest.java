package com.diskree.achievetodo.bridge;

import java.util.*;

/** S1 V04/V06-V10/V14/V15/V20/V22, narrowed to explicit diagnostic projection. */
public final class BridgeProjectionTest {
    private static int checks;
    private static void check(boolean v) { if (!v) throw new AssertionError();checks++; }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++;return; }
        throw new AssertionError("expected rejection");
    }
    public static void main(String[] args) {
        UUID epoch=UUID.fromString("11111111-1111-4111-8111-111111111111");
        var p=new BridgeWire.Projection(epoch,0,UUID.randomUUID(),UUID.randomUUID(),"minecraft:overworld",UUID.randomUUID(),"disposable","vision",false);
        check(BridgeWire.projection(BridgeWire.projection(p)).equals(p));
        check(BridgeWire.snapshot(BridgeWire.snapshot(p)).equals(p));
        byte[] snapshot=BridgeWire.snapshot(p);
        for (int n=0;n<snapshot.length;n++) { byte[] cut=Arrays.copyOf(snapshot,n);rejects(() -> BridgeWire.snapshot(cut)); }
        byte[] corrupt=snapshot.clone();corrupt[corrupt.length-1]^=1;rejects(() -> BridgeWire.snapshot(corrupt));
        rejects(() -> BridgeWire.snapshot(Arrays.copyOf(snapshot,snapshot.length+1)));
        var cache=new BridgeProjectionCache();var other=new BridgeProjectionCache();
        cache.replace(p);check(!cache.admitted());cache.admit(BridgeWire.reference(p));
        rejects(() -> cache.admit(BridgeWire.reference(p)));
        var p1=p.change(1,true);var delta=new BridgeWire.Patch(epoch,0,1,true,BridgeWire.digest(p1));
        check(BridgeWire.patch(BridgeWire.patch(delta)).equals(delta));
        check(cache.apply(delta)==BridgeProjectionCache.Result.APPLIED);check(cache.current().equals(p1));
        check(cache.apply(delta)==BridgeProjectionCache.Result.DUPLICATE);
        rejects(() -> cache.apply(new BridgeWire.Patch(epoch,0,1,false,"0".repeat(64))));
        var p2=p1.change(2,false);check(cache.apply(new BridgeWire.Patch(epoch,1,2,false,BridgeWire.digest(p2)))==BridgeProjectionCache.Result.APPLIED);
        check(cache.apply(delta)==BridgeProjectionCache.Result.STALE);
        check(cache.apply(new BridgeWire.Patch(UUID.randomUUID(),2,3,true,"0".repeat(64)))==BridgeProjectionCache.Result.STALE);
        rejects(() -> cache.replace(p));
        check(cache.apply(new BridgeWire.Patch(epoch,3,4,true,"0".repeat(64)))==BridgeProjectionCache.Result.RESYNC);
        check(!cache.admitted());check(cache.current().equals(p2));
        cache.clear();cache.replace(p2);cache.admit(BridgeWire.reference(p2));check(cache.admitted());
        other.replace(p);check(other.current().revision()==0);check(!other.admitted());
        cache.clear();check(cache.current()==null && !cache.admitted());check(other.current().equals(p));
        rejects(() -> new BridgeWire.Patch(epoch,Long.MAX_VALUE,Long.MIN_VALUE,true,""));
        // Independently corrupt the header string's UTF-8 and unsigned length.
        var hello=new BridgeWire.Frame(BridgeWire.HELLO,1,2,epoch,epoch,epoch,11,BridgeWire.CATALOG,"test");
        byte[] bytes=BridgeWire.encode(hello);bytes[67]=(byte)0xff;final byte[] invalidUtf=bytes;rejects(() -> BridgeWire.decode(invalidUtf));
        bytes=BridgeWire.encode(hello);bytes[65]=0x7f;final byte[] huge=bytes;rejects(() -> BridgeWire.decode(huge));
        // Play recovery keeps the last published state until a complete matching receipt.
        cache.replace(p2);cache.admit(BridgeWire.reference(p2));
        UUID token=UUID.randomUUID();cache.beginResync(token,100,100);
        check(!cache.admitted());check(!cache.expired(199));check(cache.expired(200));
        var full=new BridgeWire.Projection(epoch,4,UUID.randomUUID(),p.world(),p.worldKey(),p.config(),p.group(),p.ability(),true);
        var message=new BridgeWire.ResyncSnapshot(token,full);
        check(BridgeWire.resyncSnapshot(BridgeWire.resyncSnapshot(message)).equals(message));
        check(cache.stageResync(new BridgeWire.ResyncSnapshot(UUID.randomUUID(),full),110)==BridgeProjectionCache.Result.STALE);
        check(cache.stageResync(message,110)==BridgeProjectionCache.Result.APPLIED);
        check(cache.current().equals(p2));check(!cache.admitted());
        check(cache.stageResync(message,111)==BridgeProjectionCache.Result.DUPLICATE);
        check(cache.stageResync(new BridgeWire.ResyncSnapshot(token,p),111)==BridgeProjectionCache.Result.STALE);
        var ticket=new BridgeWire.Ticket(token,BridgeWire.reference(full));
        check(BridgeWire.ticket(BridgeWire.ticket(ticket)).equals(ticket));
        rejects(() -> BridgeWire.ticket(Arrays.copyOf(BridgeWire.ticket(ticket),BridgeWire.ticket(ticket).length+1)));
        rejects(() -> cache.commitResync(new BridgeWire.Ticket(token,BridgeWire.reference(p2)),112));
        check(cache.commitResync(ticket,112)==BridgeProjectionCache.Result.APPLIED);
        check(cache.current().equals(full) && cache.admitted() && !cache.resyncPending());
        check(cache.commitResync(ticket,113)==BridgeProjectionCache.Result.STALE);
        check(cache.stageResync(message,114)==BridgeProjectionCache.Result.STALE);
        cache.beginResync(UUID.randomUUID(),200,100);
        check(cache.commitResync(ticket,201)==BridgeProjectionCache.Result.STALE);
        var wrongWorld=new BridgeWire.Projection(epoch,5,UUID.randomUUID(),UUID.randomUUID(),p.worldKey(),p.config(),p.group(),p.ability(),true);
        rejects(() -> cache.stageResync(new BridgeWire.ResyncSnapshot(cache.request(),wrongWorld),210));
        rejects(() -> cache.stageResync(new BridgeWire.ResyncSnapshot(cache.request(),full),300));
        check(other.current().equals(p));cache.clear();check(!cache.resyncPending() && cache.current()==null);
        check(cache.commitResync(ticket,301)==BridgeProjectionCache.Result.STALE);
        System.out.println("BRIDGE_PROJECTION_PASS checks="+checks+" rewardWriters=NONE; E2E=NOT_PROVEN");
    }
}
