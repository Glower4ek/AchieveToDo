package com.diskree.achievetodo.bridge;

import java.util.UUID;

/** Thread-confined server handshake, independent of Bukkit and client claims of gameplay rights. */
public final class BridgeHandshake {
    public enum State { NEGOTIATING, ACKNOWLEDGED, CLOSED }
    private final BridgeWire.Frame hello;
    private final long deadline;
    private State state = State.NEGOTIATING;

    public BridgeHandshake(UUID server, long now, long budget) {
        this(server,UUID.randomUUID(),now,budget);
    }
    public BridgeHandshake(UUID server,UUID session,long now,long budget) {
        hello = new BridgeWire.Frame(BridgeWire.HELLO, BridgeWire.MAJOR, BridgeWire.MINOR,
                server, session, UUID.randomUUID(), BridgeWire.FEATURES,
                BridgeWire.CATALOG, "purpur-0.2.1-rc.1");
        deadline = Math.addExact(now, budget);
    }
    public BridgeWire.Frame hello() { return hello; }
    public State state() { return state; }
    public boolean expired(long now) { return now - deadline >= 0; }
    public void accept(BridgeWire.Frame ack, long now) {
        if (state != State.NEGOTIATING || expired(now)) throw new IllegalArgumentException("ATD_INVALID_ACK");
        if (ack.kind() != BridgeWire.ACK || ack.body().length!=0 || !ack.server().equals(hello.server())
                || !ack.session().equals(hello.session()) || !ack.challenge().equals(hello.challenge()))
            throw new IllegalArgumentException("ATD_INVALID_ACK");
        if (ack.major() != BridgeWire.MAJOR || ack.minor() != BridgeWire.MINOR
                || ack.features() != BridgeWire.FEATURES || !ack.catalog().equals(BridgeWire.CATALOG))
            throw new IllegalArgumentException("ATD_PROTOCOL_INCOMPATIBLE");
        state = State.ACKNOWLEDGED;
    }
    public void close() { state = State.CLOSED; }
}
