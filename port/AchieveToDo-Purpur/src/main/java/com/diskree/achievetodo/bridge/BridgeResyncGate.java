package com.diskree.achievetodo.bridge;

import java.util.UUID;

/** Bounded per-session play resync receipt. No native phase transition. */
public final class BridgeResyncGate {
    private UUID request;
    private BridgeWire.Reference expected;
    private long deadline;
    public boolean pending() { return request!=null; }
    public UUID request() { return request; }
    public void begin(UUID token,long now,long budget) {
        if (pending() || token==null || budget<=0) throw new IllegalArgumentException("ATD_RESYNC_IN_FLIGHT");
        request=token;deadline=now+budget;
    }
    public void snapshot(BridgeWire.Reference reference) {
        if (!pending()) throw new IllegalArgumentException("no pending resync");expected=reference;
    }
    public boolean expired(long now) { return pending() && now-deadline>=0; }
    public void validate(BridgeWire.Ticket ticket,long now) {
        if (expired(now)) throw new IllegalArgumentException("ATD_RESYNC_TIMEOUT");
        if (!pending() || !request.equals(ticket.request()) || expected==null || !expected.equals(ticket.reference()))
            throw new IllegalArgumentException("ATD_INVALID_RESYNC_READY");
    }
    public void clear() { request=null;expected=null;deadline=0; }
}
