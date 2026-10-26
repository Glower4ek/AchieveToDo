package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Canonical movement setters and server-owned pre-acceptance ascent provenance. */
public final class NativeMovementAgent {
    static final String ENTITY = "net/minecraft/world/entity/Entity";
    static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    static final String PLAYER = "net/minecraft/server/level/ServerPlayer";
    static final String LISTENER = "net/minecraft/server/network/ServerGamePacketListenerImpl";
    static final Map<String, List<String>> INPUTS = Map.of(
        ENTITY, List.of("e32f3460fbb1e7da30235a87de3a903a063e86e230374508ee071431c1be4b54", "babce1c86ae15cdfc0a0fbcc0cb692dc943a6d3ff932b7852b160a61adacde1f"),
        LIVING, List.of("b66a592f38cce1bfc228dca8f62ad9d6900a5775d7141f3ef1e9640f4c889ab1", "b66a592f38cce1bfc228dca8f62ad9d6900a5775d7141f3ef1e9640f4c889ab1"),
        PLAYER, List.of("91d67a5d076c0a7fb2ff319c3b71ab1cad578844586a0b2aec6f4824d5c91e77", "91d67a5d076c0a7fb2ff319c3b71ab1cad578844586a0b2aec6f4824d5c91e77"),
        LISTENER, List.of("e2aec4eed79bafc90c539dc4d35217f0e674dd690f0d61a290f7bca66e90e383", "e2aec4eed79bafc90c539dc4d35217f0e674dd690f0d61a290f7bca66e90e383"));
    private static final ClassDesc SELF = ClassDesc.of(NativeMovementAgent.class.getName());
    private static final MethodTypeDesc CHECK = MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_Object);
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String, String> failures = new HashMap<>();
    private static volatile BiPredicate<Object, String> guard;
    private static Object owner;
    private static boolean started;

    public static void initialize(Instrumentation instrumentation) {
        started = true;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!INPUTS.containsKey(name)) return null;
                try {
                    verifyPin(name, bytes, Boolean.getBoolean("achievetodo.landmarks.exactPrototype") ? 1 : 0);
                    byte[] result = transformNative(name, bytes, loader);
                    synchronized (NativeMovementAgent.class) { transformed.add(name); }
                    System.out.println("ATD_MOVEMENT_TRANSFORM " + name);
                    return result;
                } catch (Throwable failure) {
                    synchronized (NativeMovementAgent.class) { failures.put(name, failure.toString()); }
                    return null;
                }
            }
        });
    }
    static void verifyPin(String name, byte[] bytes, int pipeline) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(INPUTS.get(name).get(pipeline))) throw new IllegalStateException("Unpinned S4L input " + name + ": " + actual);
    }
    public static synchronized void install(Object token, BiPredicate<Object, String> callback, ClassLoader loader) throws Exception {
        if (!started || NativeMovementAgent.class.getClassLoader() != null
            || Class.forName(NativeMovementAgent.class.getName(), false, loader) != NativeMovementAgent.class)
            throw new IllegalStateException("S4L requires pinned startup agent and bootstrap identity");
        if (owner != null) throw new IllegalStateException("S4L callbacks already owned");
        for (String name : INPUTS.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(INPUTS.keySet())) throw new IllegalStateException("S4L verification failed: " + failures + "; transformed=" + transformed);
        owner = Objects.requireNonNull(token); guard = Objects.requireNonNull(callback);
    }
    public static synchronized void retire(Object token) { if (owner == token) { guard = null; owner = null; NativeMovementAscent.clear(); } }
    private static boolean player(Object actor) {
        for (Class<?> c = actor == null ? null : actor.getClass(); c != null; c = c.getSuperclass())
            if (c.getName().equals("net.minecraft.world.entity.player.Player")) return true;
        return false;
    }
    private static boolean flag(Object actor, String method) throws ReflectiveOperationException {
        return (boolean) actor.getClass().getMethod(method).invoke(actor);
    }
    private static boolean locked(Object actor, String ability) { var callback = guard; return callback == null || callback.test(actor, ability); }
    public static boolean jump(Object actor) {
        if (!player(actor)) return false;
        try { return !flag(actor, "isInWater") && locked(actor, "JUMP"); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { return true; }
    }
    public static boolean sneak(Object actor) {
        if (!player(actor)) return false;
        try { return flag(actor, "onGround") && locked(actor, "SNEAK"); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { return true; }
    }
    public static boolean sprint(Object actor) {
        if (!player(actor)) return false;
        try { return locked(actor, flag(actor, "isUnderWater") ? "SWIM" : "SPRINT"); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { return true; }
    }
    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        if (!INPUTS.containsKey(name)) throw new IllegalArgumentException("Unpinned owner " + name);
        int[] sites = {0};
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        byte[] result = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (!(e instanceof MethodModel m)) { cb.with(e); return; }
            String method = m.methodName().stringValue(), desc = m.methodType().stringValue();
            boolean sneak = name.equals(ENTITY) && method.equals("setShiftKeyDown") && desc.equals("(Z)V");
            boolean velocity = name.equals(ENTITY) && method.equals("setDeltaMovement") && desc.equals("(Lnet/minecraft/world/phys/Vec3;)V");
            boolean sprint = name.equals(LIVING) && method.equals("setSprinting") && desc.equals("(Z)V");
            boolean jump = name.equals(PLAYER) && method.equals("jumpFromGround") && desc.equals("()V");
            boolean livingJump = name.equals(LIVING) && method.equals("jumpFromGround") && desc.equals("()V");
            boolean packet = name.equals(LISTENER) && method.equals("handleMovePlayer") && desc.equals("(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V");
            boolean teleport = name.equals(LISTENER) && method.equals("internalTeleport")
                && desc.equals("(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V");
            if (!(sneak || velocity || sprint || jump || livingJump || packet || teleport)) { cb.with(e); return; }
            cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                @Override public void atStart(CodeBuilder b) {
                    if (teleport) {
                        sites[0]++;
                        b.aload(0).getfield(ClassDesc.ofInternalName(LISTENER), "player", ClassDesc.ofInternalName(PLAYER))
                            .invokestatic(SELF, "resetAscent", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object));
                        return;
                    }
                    if (velocity) {
                        sites[0]++;
                        b.aload(0).aload(1).invokestatic(SELF, "velocity", MethodTypeDesc.of(ConstantDescs.CD_void,
                            ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                        return;
                    }
                    if (!(sneak || sprint || jump)) return;
                    sites[0]++;
                    var pass = b.newLabel();
                    if (!jump) b.iload(1).ifeq(pass);
                    b.aload(0).invokestatic(SELF, jump ? "jump" : sneak ? "sneak" : "sprint", CHECK).ifeq(pass).return_().labelBinding(pass);
                }
                @Override public void accept(CodeBuilder b, CodeElement e) {
                    if (packet && e instanceof InvokeInstruction call && call.owner().asInternalName().equals(PLAYER)
                        && call.name().equalsString("onGround") && call.type().equalsString("()Z")) {
                        sites[0]++;
                        var pass = b.newLabel();
                        // Before the packet-controlled ground branch and before either position write.
                        b.dup().aload(1).invokestatic(SELF, "ascent", MethodTypeDesc.of(ConstantDescs.CD_boolean,
                            ConstantDescs.CD_Object, ConstantDescs.CD_Object)).ifeq(pass).pop().return_().labelBinding(pass);
                    }
                    if (e instanceof InvokeInstruction call && livingJump && call.owner().asInternalName().equals(LIVING)
                        && call.name().equalsString("getDeltaMovement") && call.type().equalsString("()Lnet/minecraft/world/phys/Vec3;")) {
                        sites[0]++;
                        // The receiver is already on the stack. Leave it for the native invocation on allow.
                        var pass = b.newLabel();
                        b.dup().invokestatic(SELF, "jump", CHECK).ifeq(pass).pop().return_().labelBinding(pass);
                    }
                    b.with(e);
                }
            }));
        });
        int expected = name.equals(PLAYER) ? 1 : 2;
        if (sites[0] != expected) throw new IllegalStateException("S4L exact sites " + name + "=" + sites[0] + " expected=" + expected);
        return result;
    }
    public static boolean ascent(Object actor, Object packet) {
        return NativeMovementAscent.reject(actor, packet, locked(actor, "JUMP"));
    }
    public static void velocity(Object actor, Object vector) {
        if (player(actor)) NativeMovementAscent.velocity(actor, vector);
    }
    public static void resetAscent(Object actor) { NativeMovementAscent.reset(actor); }
    private NativeMovementAgent() {}
}
