package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** S4K native transactions. Bootstrap code has no Bukkit or plugin dependencies. */
public final class NativePortalAgent {
    static final String ENTITY = "net/minecraft/world/entity/Entity";
    static final String DESCRIPTOR = "(Lnet/minecraft/world/level/block/Portal;Lnet/minecraft/core/BlockPos;)V";
    static final Map<String, List<String>> INPUTS = Map.of(ENTITY, List.of("baae8bcbb94c57025be9e90ea53c25d734fdcc8a524ccc6bc9707eaba9690ed2", "5949c2d33ff1ada771fec80484cc9a90bc92d9b6aaff776adcca38101a9660bf"));
    private static final ClassDesc SELF = ClassDesc.of(NativePortalAgent.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
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
                    byte[] output = transformNative(name, bytes, loader);
                    synchronized (NativePortalAgent.class) { transformed.add(name); }
                    System.out.println("ATD_PORTAL_TRANSFORM " + name);
                    return output;
                } catch (Throwable failure) {
                    synchronized (NativePortalAgent.class) { failures.put(name, failure.toString()); }
                    return null;
                }
            }
        });
    }

    static void verifyPin(String name, byte[] bytes, int pipeline) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(INPUTS.get(name).get(pipeline)))
            throw new IllegalStateException("Unpinned S4K input " + name + ": " + actual);
    }

    public static synchronized void install(Object token, BiPredicate<Object, String> callback, ClassLoader loader) throws Exception {
        if (!started || NativePortalAgent.class.getClassLoader() != null
            || Class.forName(NativePortalAgent.class.getName(), false, loader) != NativePortalAgent.class)
            throw new IllegalStateException("S4K requires pinned startup agent and bootstrap identity");
        if (owner != null) throw new IllegalStateException("S4K callbacks already owned");
        for (String name : INPUTS.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(INPUTS.keySet()))
            throw new IllegalStateException("S4K verification failed: " + failures + "; transformed=" + transformed);
        owner = Objects.requireNonNull(token);
        guard = Objects.requireNonNull(callback);
    }

    public static synchronized void retire(Object token) {
        if (owner == token) { guard = null; owner = null; }
    }

    private static boolean isType(Object value, String name) {
        for (Class<?> c = value == null ? null : value.getClass(); c != null; c = c.getSuperclass())
            if (c.getName().equals("net.minecraft." + name)) return true;
        return false;
    }

    private static Class<?> type(Object value, String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft." + name, true, value.getClass().getClassLoader());
    }

    private static Object field(Object value, String name) throws ReflectiveOperationException {
        Class<?> c = value instanceof Class<?> t ? t : value.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(value instanceof Class<?> ? null : value); }
            catch (NoSuchFieldException missing) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object call(Object value, String name, Object... args) throws ReflectiveOperationException {
        Class<?> c = value instanceof Class<?> t ? t : value.getClass();
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            boolean match = true;
            for (int i = 0; i < args.length; i++)
                if (args[i] != null && !m.getParameterTypes()[i].isInstance(args[i])) { match = false; break; }
            if (match) return m.invoke(value instanceof Class<?> ? null : value, args);
        }
        throw new NoSuchMethodException(c.getName() + "." + name);
    }

    /** Mirrors canonical EntityMixin.lockPortal in order. No destination prediction or graph-wide veto. */
    public static boolean denied(Object entity, Object portal, Object pos) {
        try {
            Object pearl = isType(entity, "world.entity.projectile.throwableitemprojectile.ThrownEnderpearl") ? entity : null;
            Object actor = pearl == null ? entity : call(pearl, "getOwner");
            if (actor == null) return false;
            Object level = call(pearl == null ? actor : pearl, "level");
            Object dimension = call(level, "dimension");
            String ability = null;
            if (portal != null) {
                Class<?> actual = portal.getClass();
                if (actual == type(entity, "world.level.block.NetherPortalBlock")) ability = "ENTER_NETHER";
                else if (actual == type(entity, "world.level.block.EndPortalBlock")) ability = "ENTER_END";
                else if (actual == type(entity, "world.level.block.EndGatewayBlock")) ability = "TELEPORT_OUTER_ISLANDS";
            }
            if (dimension == field(type(entity, "world.level.Level"), "NETHER") && "ENTER_NETHER".equals(ability)) return false;
            if (dimension == field(type(entity, "world.level.Level"), "END")) {
                if ("ENTER_END".equals(ability)) return false;
                if ("TELEPORT_OUTER_ISLANDS".equals(ability) && !central((int)call(pos,"getX"),(int)call(pos,"getY"),(int)call(pos,"getZ"))) return false;
            }
            if (isType(actor, "world.entity.player.Player")) {
                boolean landmark = Boolean.getBoolean("achievetodo.landmarks.exactPrototype") && NativeLandmarkAgent.targetDenied(actor, level, pos);
                if (landmark || locked(actor, ability)) {
                    if (pearl != null) call(pearl, "remove", field(type(entity,"world.entity.Entity$RemovalReason"), "DISCARDED"));
                    return true;
                }
            }
            if (pearl != null || !(boolean)call(actor,"isVehicle")) return false;
            Object controller = call(actor,"getControllingPassenger");
            if (isType(controller,"world.entity.player.Player") && locked(controller,ability)) return true;
            // Native getPassengers() is an immutable snapshot; stopRiding may replace the live list.
            for (Object passenger : (List<?>)call(actor,"getPassengers"))
                if (isType(passenger,"world.entity.player.Player") && locked(passenger,ability)) call(passenger,"stopRiding");
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }
    static boolean central(int x, int y, int z) { return y == 75 && x >= -96 && x <= 96 && z >= -96 && z <= 96; }
    private static boolean locked(Object actor, String ability) {
        if (ability == null) return false;
        var callback = guard;
        return callback == null || callback.test(actor, ability);
    }
    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        if (!ENTITY.equals(name)) throw new IllegalArgumentException("Unpinned owner " + name);
        int[] count = {0};
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (e instanceof MethodModel m && m.methodName().equalsString("setAsInsidePortal") && m.methodType().equalsString(DESCRIPTOR)) {
                count[0]++;
                cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                    @Override public void accept(CodeBuilder b, CodeElement e) { b.with(e); }
                    @Override public void atStart(CodeBuilder b) {
                        var pass = b.newLabel();
                        b.aload(0).aload(1).aload(2).invokestatic(SELF,"denied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT,OBJECT))
                            .ifeq(pass).return_().labelBinding(pass);
                    }
                }));
            } else cb.with(e);
        });
        if (count[0] != 1) throw new IllegalStateException("S4K exact method count="+count[0]);
        return output;
    }
    private NativePortalAgent() {}
}
