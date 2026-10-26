package com.diskree.achievetodo.bridge;

import java.util.*;

/** Executable narrow runner; does not label packet round-trip as real E2E. */
public final class BridgeContractTest {
    private static int checks;
    private static void check(boolean yes) { if (!yes) throw new AssertionError(); checks++; }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("expected rejection");
    }
    private static BridgeWire.Frame ack(BridgeHandshake h) {
        var f=h.hello();return new BridgeWire.Frame(BridgeWire.ACK,f.major(),f.minor(),f.server(),f.session(),f.challenge(),f.features(),f.catalog(),"test");
    }
    public static void main(String[] args) {
        UUID server=UUID.randomUUID();
        BridgeHandshake h=new BridgeHandshake(server,100,100);
        var a=ack(h);
        check(Arrays.equals(BridgeWire.encode(BridgeWire.decode(BridgeWire.encode(a))),BridgeWire.encode(a)));
        byte[] good=BridgeWire.encode(a);
        for (int n=0;n<good.length;n++) { byte[] cut=Arrays.copyOf(good,n); rejects(() -> BridgeWire.decode(cut)); }
        rejects(() -> BridgeWire.decode(Arrays.copyOf(good,good.length+1)));
        rejects(() -> BridgeWire.decode(new byte[BridgeWire.MAX_FRAME+1]));
        var bad=new BridgeWire.Frame(a.kind(),2,a.minor(),a.server(),a.session(),a.challenge(),a.features(),a.catalog(),a.build());
        rejects(() -> h.accept(bad,110));
        BridgeHandshake other=new BridgeHandshake(server,100,100);
        rejects(() -> other.accept(a,110));
        var old=new BridgeWire.Frame(a.kind(),1,2,a.server(),a.session(),a.challenge(),11,a.catalog(),a.build());
        rejects(()->h.accept(old,110));
        h.accept(a,110);check(h.state()==BridgeHandshake.State.ACKNOWLEDGED);
        rejects(() -> h.accept(a,111));
        other.close();rejects(() -> other.accept(ack(other),110));
        BridgeHandshake timeout=new BridgeHandshake(server,100,100);
        check(!timeout.expired(199));check(timeout.expired(200));rejects(() -> timeout.accept(ack(timeout),200));
        check(other.state()==BridgeHandshake.State.CLOSED);
        var gate=new BridgeResyncGate();var independent=new BridgeResyncGate();
        var projection=new BridgeWire.Projection(UUID.randomUUID(),0,UUID.randomUUID(),BridgeProjectionTest.fixture());
        UUID token=UUID.randomUUID();gate.begin(token,100,100);gate.snapshot(BridgeWire.reference(projection));
        var ready=new BridgeWire.Ticket(token,BridgeWire.reference(projection));
        gate.validate(ready,199);check(!gate.expired(199));check(gate.expired(200));
        rejects(() -> gate.validate(ready,200));
        rejects(() -> gate.begin(UUID.randomUUID(),110,100));
        rejects(() -> independent.validate(ready,110));
        rejects(() -> gate.validate(new BridgeWire.Ticket(UUID.randomUUID(),ready.reference()),110));
        check(!independent.pending());gate.clear();check(!gate.pending());rejects(() -> gate.validate(ready,110));
        gate.begin(UUID.randomUUID(),200,100);gate.snapshot(ready.reference());rejects(() -> gate.validate(ready,201));
        gate.clear();check(!gate.expired(1000));
        BridgeProjectionTest.main(args);
        BridgeAuthoritativeProjectionTest.main(args);
        BridgeLandmarksTest.main(args);
        System.out.println("BRIDGE_HANDSHAKE_PASS checks="+checks+"; E2E=NOT_PROVEN");
    }
}
