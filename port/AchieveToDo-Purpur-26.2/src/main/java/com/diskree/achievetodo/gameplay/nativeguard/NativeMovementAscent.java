package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.reflect.*;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

/** Native collision/velocity provenance for locked ascent; no packet ground/input authority. */
final class NativeMovementAscent {
    private static final WeakHashMap<Object, Flight> flights = new WeakHashMap<>();
    private static final class Flight {
        WeakReference<Object> level;
        int tick;
        double ceiling, tickVelocity;
    }
    private static Object invoke(Object actor, String name) throws ReflectiveOperationException {
        return actor.getClass().getMethod(name).invoke(actor);
    }
    private static double number(Object actor, String name) throws ReflectiveOperationException {
        return ((Number) invoke(actor, name)).doubleValue();
    }
    private static boolean flag(Object actor, String name) throws ReflectiveOperationException {
        return (boolean) invoke(actor, name);
    }
    static synchronized void velocity(Object actor, Object vector) {
        // Only server players have a network ascent transaction. Integrated/client players are untouched.
        if (!actor.getClass().getName().equals("net.minecraft.server.level.ServerPlayer")) return;
        try {
            double vy = vector.getClass().getField("y").getDouble(vector);
            int tick = actor.getClass().getField("tickCount").getInt(actor);
            Object level = invoke(actor, "level");
            if (flag(actor, "isInWater") || flag(actor, "isInLava") || flag(actor, "onClimbable")
                || flag(actor, "isPassenger") || flag(actor, "isFallFlying")) {
                flights.remove(actor);
                return;
            }
            Flight f = flights.get(actor);
            if (f != null && (f.level.get() != level || tick < f.tick)) { flights.remove(actor); f = null; }
            if (vy <= 0) {
                if (f != null && supported(actor)) flights.remove(actor);
                return;
            }
            if (f == null) {
                f = new Flight(); f.level = new WeakReference<>(level); f.tick = tick; f.ceiling = number(actor, "getY");
                flights.put(actor, f);
            }
            if (f.tick != tick) { f.tick = tick; f.tickVelocity = 0; }
            if (vy > f.tickVelocity) { f.ceiling += vy - f.tickVelocity; f.tickVelocity = vy; }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Pinned native velocity provenance failed", failure);
        }
    }
    private static Object collide(Object actor, double dx, double dy, double dz) throws ReflectiveOperationException {
        Class<?> vector = invoke(actor, "getDeltaMovement").getClass();
        Class<?> entity = Class.forName("net.minecraft.world.entity.Entity", false, actor.getClass().getClassLoader());
        Method collide = entity.getDeclaredMethod("collide", vector); collide.setAccessible(true);
        return collide.invoke(actor, vector.getConstructor(double.class, double.class, double.class).newInstance(dx, dy, dz));
    }
    private static double y(Object vector) throws ReflectiveOperationException { return vector.getClass().getField("y").getDouble(vector); }
    private static boolean supported(Object actor) throws ReflectiveOperationException { return y(collide(actor, 0, -1.0E-5, 0)) >= -1.0E-7; }
    static boolean reject(Object actor, Object packet, boolean locked) {
        if (!locked) return false;
        try {
            if (!(boolean) packet.getClass().getMethod("hasPosition").invoke(packet)) return false;
            double x = number(actor, "getX"), y = number(actor, "getY"), z = number(actor, "getZ");
            Class<?> packetType = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket", false, actor.getClass().getClassLoader());
            double dy = (double) packetType.getMethod("getY", double.class).invoke(packet, y) - y;
            if (dy <= 1.0E-7) return false;
            if (flag(actor, "isInWater") || flag(actor, "isInLava") || flag(actor, "onClimbable")
                || flag(actor, "isPassenger") || flag(actor, "isFallFlying")) return false;
            synchronized (NativeMovementAscent.class) {
                Flight f = flights.get(actor);
                if (f != null && f.level.get() == invoke(actor, "level") && y + dy <= f.ceiling + 1.0E-7) return false;
            }
            double dx = (double) packetType.getMethod("getX", double.class).invoke(packet, x) - x;
            double dz = (double) packetType.getMethod("getZ", double.class).invoke(packet, z) - z;
            // A legal step is produced by native horizontal collision/step resolution without a jump.
            Object step = collide(actor, dx, -1.0E-5, dz);
            return dy > Math.max(0, y(step)) + 1.0E-7;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw new IllegalStateException("Pinned native ascent validation failed", failure);
        }
    }
    static synchronized void reset(Object actor) { flights.remove(actor); }
    static synchronized void clear() { flights.clear(); }
    private NativeMovementAscent() {}
}
