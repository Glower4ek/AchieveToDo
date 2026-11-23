package com.diskree.achievetodo.bridge;

import java.util.UUID;

/** Thread-confined server handshake, independent of Bukkit and client claims of gameplay rights. */
public final class BridgeHandshake {
    public enum State { NEGOTIATING, ACKNOWLEDGED, CLOSED }
    private final BridgeWire.Frame hello;
    private final long deadline;
    private State state = State.NEGOTIATING;

    /** Version used when none is given (unit fixtures). */
    public static final String FIXTURE_VERSION = "0.0.0-test";
    private final String version;

    public BridgeHandshake(UUID server, long now, long budget) {
        this(server,UUID.randomUUID(),now,budget,FIXTURE_VERSION);
    }
    public BridgeHandshake(UUID server,UUID session,long now,long budget) {
        this(server,session,now,budget,FIXTURE_VERSION);
    }
    /** @param version the plugin version; the client mod must be exactly this version */
    public BridgeHandshake(UUID server,UUID session,long now,long budget,String version) {
        this.version = java.util.Objects.requireNonNull(version);
        hello = new BridgeWire.Frame(BridgeWire.HELLO, BridgeWire.MAJOR, BridgeWire.MINOR,
                server, session, UUID.randomUUID(), BridgeWire.FEATURES,
                BridgeWire.CATALOG, "purpur-" + version);
        deadline = Math.addExact(now, budget);
    }
    public String version() { return version; }
    /** The client mod version an ACK announces ({@code universal-<version>}), or null when it announces none. */
    public static String clientVersion(BridgeWire.Frame ack) {
        String build = ack.build();
        return build != null && build.startsWith("universal-") ? build.substring("universal-".length()) : null;
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
                || ack.features() != BridgeWire.FEATURES || !ack.catalog().equals(BridgeWire.CATALOG)
                || (!version.equals(FIXTURE_VERSION) && !version.equals(clientVersion(ack))))
            throw new ClientVersionMismatch(clientVersion(ack));
        state = State.ACKNOWLEDGED;
    }
    public void close() { state = State.CLOSED; }

    /** The client speaks another protocol or is another AchieveToDo version than this plugin. */
    public static final class ClientVersionMismatch extends IllegalArgumentException {
        private final String clientVersion;
        ClientVersionMismatch(String clientVersion) { super("ATD_PROTOCOL_INCOMPATIBLE"); this.clientVersion = clientVersion; }
        /** The announced client mod version, or null when unknown. */
        public String clientVersion() { return clientVersion; }
    }
}
