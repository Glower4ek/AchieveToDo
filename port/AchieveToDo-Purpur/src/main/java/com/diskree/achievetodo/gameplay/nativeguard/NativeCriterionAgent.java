package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W5-RED-1: a denied native use returns a consuming InteractionResult so vanilla does not continue
 * into the item fallback. ServerPlayerGameMode then fires ITEM_USED_ON_BLOCK as if the use happened.
 * The canonical denial path marks the actor; the very next ITEM_USED_ON_BLOCK trigger for that actor
 * within a short window is suppressed. PLACED_BLOCK and ALLAY_DROP_ITEM_ON_BLOCK are never affected.
 */
public final class NativeCriterionAgent {
    static final String TRIGGER = "net/minecraft/advancements/triggers/ItemUsedOnLocationTrigger";
    static final String METHOD = "trigger";
    static final String DESCRIPTOR = "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemInstance;)V";
    static final String PIN = "97d84fcb4d954cc18381ade94ee719421e5f1d65c4975abe0c2fc226ee8f633d";
    /** Guard -> trigger is a direct call chain on the server thread; the window only bounds stale marks. */
    static final long WINDOW_NANOS = 50_000_000L;
    private static final ConcurrentHashMap<UUID, Long> denied = new ConcurrentHashMap<>();
    private static volatile boolean transformed;
    private static volatile String failure;

    public static void initialize(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!TRIGGER.equals(name)) return null;
                try {
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!actual.equals(PIN)) throw new IllegalStateException("Unpinned criterion input " + name + ": " + actual);
                    byte[] result = transformTrigger(bytes);
                    transformed = true;
                    System.out.println("ATD_CRITERION_TRANSFORM " + name + " sha256=" + actual);
                    return result;
                } catch (Throwable error) {
                    failure = error.toString();
                    System.out.println("ATD_CRITERION_TRANSFORM_FAILED " + failure);
                    return null;
                }
            }
        });
    }

    static byte[] transformTrigger(byte[] bytes) {
        var cf = ClassFile.of();
        int[] sites = {0};
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (e instanceof MethodModel m && m.methodName().equalsString(METHOD)
                && m.methodType().equalsString(DESCRIPTOR)) {
                cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                    @Override public void atStart(CodeBuilder b) {
                        var pass = b.newLabel();
                        b.aload(0).aload(1).invokestatic(ClassDesc.of(NativeCriterionAgent.class.getName()), "suppressed",
                            MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                        b.ifeq(pass).return_().labelBinding(pass);
                        sites[0]++;
                    }
                    @Override public void accept(CodeBuilder b, CodeElement element) { b.with(element); }
                }));
            } else cb.with(e);
        });
        if (sites[0] != 1) throw new IllegalStateException("Criterion anchor count=" + sites[0]);
        return output;
    }

    /** Called by the canonical denial effect for every effectful player denial. */
    public static void markDenied(UUID actor) {
        if (actor != null) denied.put(actor, System.nanoTime());
    }

    public static boolean suppressed(Object trigger, Object player) {
        try {
            Object itemUsed = Class.forName("net.minecraft.advancements.triggers.CriteriaTriggers", true,
                trigger.getClass().getClassLoader()).getField("ITEM_USED_ON_BLOCK").get(null);
            if (trigger != itemUsed || player == null) return false;
            Object id = player.getClass().getMethod("getUUID").invoke(player);
            boolean suppress = id instanceof UUID uuid && consume(uuid, System.nanoTime());
            if (suppress) System.out.println("ATD_CRITERION_SUPPRESSED ITEM_USED_ON_BLOCK actor=" + id);
            return suppress;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return false; // Never block legitimate progression because of a lookup failure.
        }
    }

    /** One-shot: any ITEM_USED_ON_BLOCK for the actor clears the mark; only a fresh mark suppresses. */
    static boolean consume(UUID actor, long now) {
        Long at = denied.remove(actor);
        return at != null && now - at < WINDOW_NANOS;
    }

    public static boolean installed() { return transformed && failure == null; }
    private NativeCriterionAgent() {}
}
