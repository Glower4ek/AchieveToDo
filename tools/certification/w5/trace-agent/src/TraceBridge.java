/**
 * Test-only bootstrap class called by packet-witness.jar and w4-snap.jar (base runtime plugins).
 * Replaces the TraceBridge from Codex's lost causal-bridge.jar; loaded via Boot-Class-Path of
 * trace-agent.jar. Lines go to stdout (server output.log); campaign.py reads ATD_PACKET_IN.
 * Must never throw: it runs inside netty handlers and Bukkit listeners.
 */
public final class TraceBridge {
    private TraceBridge() {}

    /** Inbound packet from a player connection. */
    public static void packet(Object packet, String player) {
        try {
            System.out.println("ATD_PACKET_IN player=" + player + " type=" + name(packet) + " " + packet);
        } catch (Throwable ignored) {
        }
    }

    /** Outbound packet of an observed type (screen, container, sound, stats, advancements). */
    public static void outbound(Object packet) {
        try {
            System.out.println("ATD_PACKET_OUT type=" + name(packet));
        } catch (Throwable ignored) {
        }
    }

    /** Named observer boundary event (inventory open/close, observer failures). */
    public static void boundary(String event, Object detail) {
        try {
            System.out.println("ATD_BOUNDARY " + event + " " + detail);
        } catch (Throwable ignored) {
        }
    }

    private static String name(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
