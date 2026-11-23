package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W5-RED-1: a denied native use returns a consuming InteractionResult so vanilla does not continue
 * into the item fallback. Vanilla then fires the use criteria as if the use happened: ITEM_USED_ON_BLOCK and
 * DEFAULT_BLOCK_USE (ServerPlayerGameMode.useItemOn), ANY_BLOCK_USE (ServerGamePacketListenerImpl.handleUseItemOn)
 * and PLAYER_INTERACTED_WITH_ENTITY (handleInteract; 0.4.0, known issue 4 audit: a map put into an item frame or a
 * golden dandelion given to a tadpole inside a locked landmark granted BACAP criteria).
 * The canonical denial path marks the actor; the very next trigger of each of these kinds for that actor within a
 * short window is suppressed. PLACED_BLOCK, ALLAY_DROP_ITEM_ON_BLOCK and PLAYER_SHEARED_EQUIPMENT share trigger
 * classes with them and are never affected.
 */
public final class NativeCriterionAgent {
    /** Trigger class -> {trigger method descriptor, pinned input SHA-256, CriteriaTriggers fields it guards}. */
    record Target(String descriptor, String pin, List<String> fields) {}
    static final Map<String, Target> TARGETS = Map.of(
        "net/minecraft/advancements/triggers/ItemUsedOnLocationTrigger", new Target(
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemInstance;)V",
            "97d84fcb4d954cc18381ade94ee719421e5f1d65c4975abe0c2fc226ee8f633d", List.of("ITEM_USED_ON_BLOCK")),
        "net/minecraft/advancements/triggers/DefaultBlockInteractionTrigger", new Target(
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)V",
            "aee005aaa1b3ee87b88c99b8b09ae0dffa255a4877d25ca28c5188c90ce19d3b", List.of("DEFAULT_BLOCK_USE")),
        "net/minecraft/advancements/triggers/AnyBlockInteractionTrigger", new Target(
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemInstance;)V",
            "29b02850f42b4db7400458b8b2a0ec0651f36be2f602eb5e349a2282bbf63f04", List.of("ANY_BLOCK_USE")),
        "net/minecraft/advancements/triggers/PlayerInteractTrigger", new Target(
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)V",
            "345ad946c68a066cf90586f92aa254e3c4d0f134c26b611ba07dc2864bdcfe26", List.of("PLAYER_INTERACTED_WITH_ENTITY")));
    static final List<String> KINDS = List.of("ITEM_USED_ON_BLOCK", "DEFAULT_BLOCK_USE", "ANY_BLOCK_USE", "PLAYER_INTERACTED_WITH_ENTITY");
    static final String METHOD = "trigger";
    /** Guard -> trigger is a direct call chain on the server thread; the window only bounds stale marks. */
    static final long WINDOW_NANOS = 50_000_000L;
    /** Per actor, per kind: the time of the denial that may still suppress that kind once. */
    private static final ConcurrentHashMap<UUID, long[]> denied = new ConcurrentHashMap<>();
    private static final Set<String> transformed = ConcurrentHashMap.newKeySet();
    private static volatile String failure;

    public static void initialize(Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                Target target = name == null ? null : TARGETS.get(name);
                if (target == null) return null;
                try {
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!actual.equals(target.pin())) throw new IllegalStateException("Unpinned criterion input " + name + ": " + actual);
                    byte[] result = transformTrigger(bytes, target.descriptor());
                    transformed.add(name);
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

    static byte[] transformTrigger(byte[] bytes, String descriptor) {
        var cf = ClassFile.of();
        int[] sites = {0};
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (e instanceof MethodModel m && m.methodName().equalsString(METHOD)
                && m.methodType().equalsString(descriptor)) {
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
        if (actor == null) return;
        long now = System.nanoTime();
        long[] marks = new long[KINDS.size()];
        Arrays.fill(marks, now);
        denied.put(actor, marks);
    }

    public static boolean suppressed(Object trigger, Object player) {
        try {
            if (player == null) return false;
            Class<?> triggers = Class.forName("net.minecraft.advancements.triggers.CriteriaTriggers", true,
                trigger.getClass().getClassLoader());
            int kind = -1;
            for (int i = 0; i < KINDS.size() && kind < 0; i++) if (trigger == triggers.getField(KINDS.get(i)).get(null)) kind = i;
            if (kind < 0) return false;
            Object id = player.getClass().getMethod("getUUID").invoke(player);
            boolean suppress = id instanceof UUID uuid && consume(uuid, kind, System.nanoTime());
            if (suppress) System.out.println("ATD_CRITERION_SUPPRESSED " + KINDS.get(kind) + " actor=" + id);
            return suppress;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            return false; // Never block legitimate progression because of a lookup failure.
        }
    }

    /** One-shot per kind: a trigger of that kind clears its mark; only a fresh mark suppresses. */
    static boolean consume(UUID actor, int kind, long now) {
        boolean[] result = {false};
        denied.computeIfPresent(actor, (id, marks) -> {
            long at = marks[kind];
            marks[kind] = Long.MIN_VALUE;
            result[0] = at != Long.MIN_VALUE && now - at < WINDOW_NANOS;
            return Arrays.stream(marks).allMatch(m -> m == Long.MIN_VALUE) ? null : marks;
        });
        return result[0];
    }

    public static boolean installed() { return transformed.size() == TARGETS.size() && failure == null; }
    private NativeCriterionAgent() {}
}
