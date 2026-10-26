package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/** S4J native transactions. Bootstrap code has no Bukkit or plugin dependencies. */
public final class NativeComplexAgent {
    private static final ClassDesc SELF = ClassDesc.of(NativeComplexAgent.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
    private static final MethodTypeDesc DENIED = MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, ConstantDescs.CD_String);
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String, String> failures = new HashMap<>();
    private static volatile BiPredicate<Object, String> guard;
    private static Object owner;
    private static boolean started;

    public static void initialize(Instrumentation instrumentation) {
        started = true;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!NativeComplexSpecs.INPUTS.containsKey(name)) return null;
                try {
                    verifyPin(name, bytes, Boolean.getBoolean("achievetodo.landmarks.exactPrototype") ? 1 : 0);
                    byte[] output = transformNative(name, bytes, loader);
                    synchronized (NativeComplexAgent.class) { transformed.add(name); }
                    System.out.println("ATD_COMPLEX_TRANSFORM " + name);
                    return output;
                } catch (Throwable failure) {
                    synchronized (NativeComplexAgent.class) { failures.put(name, failure.toString()); }
                    return null;
                }
            }
        });
    }

    static void verifyPin(String name, byte[] bytes, int pipeline) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(NativeComplexSpecs.INPUTS.get(name).get(pipeline)))
            throw new IllegalStateException("Unpinned S4J input " + name + ": " + actual);
    }

    public static synchronized void install(Object token, BiPredicate<Object, String> callback, ClassLoader loader) throws Exception {
        if (!started || NativeComplexAgent.class.getClassLoader() != null
            || Class.forName(NativeComplexAgent.class.getName(), false, loader) != NativeComplexAgent.class)
            throw new IllegalStateException("S4J requires pinned startup agent and bootstrap identity");
        if (owner != null) throw new IllegalStateException("S4J callbacks already owned");
        for (String name : NativeComplexSpecs.INPUTS.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(NativeComplexSpecs.INPUTS.keySet()))
            throw new IllegalStateException("S4J verification failed: " + failures + "; transformed=" + transformed);
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

    private static boolean exactItem(Object stack, String name) throws ReflectiveOperationException {
        return call(stack, "getItem") == field(type(stack, "world.item.Items"), name);
    }

    public static boolean restricted(boolean original, Object actor) {
        return original || denied(actor, null, "mine");
    }

    /** Resolve branch predicates first; consult the synchronous S3C callback only for canonical candidates. */
    public static boolean denied(Object actor, Object subject, String mode) {
        try {
            if (mode.startsWith("context:")) { actor = call(actor, "getPlayer"); mode = mode.substring(8); }
            if (mode.equals("projectile")) { actor = call(actor, "getOwner"); mode = "IGNITE_TNT"; }
            if (!isType(actor, "world.entity.player.Player")) return false;
            String ability = mode;
            switch (mode) {
                case "ignite":
                    return denied(actor, subject, "flint") || denied(actor, null, "IGNITE_TNT");
                case "flint":
                    if (!exactItem(subject, "FLINT_AND_STEEL")) return false;
                    ability = "USE_FLINT_AND_STEEL"; break;
                case "wolf":
                    if (!exactItem(subject, "SHEARS")) return false;
                    ability = "USE_SHEARS"; break;
                case "mine":
                    if (!isType(call(call(actor, "getMainHandItem"), "getItem"), "world.item.ShearsItem")) return false;
                    ability = "USE_SHEARS"; break;
                case "water":
                    if (field(subject, "content") != field(type(subject, "world.level.material.Fluids"), "WATER")) return false;
                    ability = "USE_WATER_BUCKET"; break;
                case "cauldron":
                    if (subject == field(type(actor, "core.cauldron.CauldronInteraction"), "DEFAULT")) return false;
                    ability = "USE_CAULDRON"; break;
                case "leash":
                    Class<?> leash = type(subject, "world.entity.Leashable");
                    boolean own = leash.isInstance(subject) && (boolean)call(subject, "isLeashed");
                    if (!own && ((List<?>)call(leash, "leashableLeashedTo", subject)).isEmpty()) return false;
                    ability = "USE_SHEARS"; break;
                case "equipment":
                    if (!shearableEquipment(actor, subject)) return false;
                    ability = "USE_SHEARS"; break;
                case "brush-tick": ability = "USE_BRUSH"; break;
                default: break;
            }
            var callback = guard;
            boolean denied = callback == null || callback.test(actor, ability);
            if (denied && mode.equals("brush-tick")) call(actor, "releaseUsingItem");
            return denied;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    private static boolean shearableEquipment(Object player, Object mob) throws ReflectiveOperationException {
        Object componentType = field(type(mob, "core.component.DataComponents"), "EQUIPPABLE");
        Object prevent = field(type(mob, "world.item.enchantment.EnchantmentEffectComponents"), "PREVENT_ARMOR_CHANGE");
        Class<?> helper = type(mob, "world.item.enchantment.EnchantmentHelper");
        for (Object slot : type(mob, "world.entity.EquipmentSlot").getEnumConstants()) {
            Object stack = call(mob, "getItemBySlot", slot);
            Object component = call(stack, "get", componentType);
            if (component != null && (boolean)call(component, "canBeSheared")
                && (!(boolean)call(helper, "has", stack, prevent) || (boolean)call(player, "isCreative"))) return true;
        }
        return false;
    }

    public static boolean cauldronFallback(Object player, Object level, Object stack, Object hit) {
        try {
            if ((boolean)call(player, "isSecondaryUseActive")
                && (!(boolean)call(call(player, "getMainHandItem"), "isEmpty") || !(boolean)call(call(player, "getOffhandItem"), "isEmpty"))) return false;
            Object block = call(call(level, "getBlockState", call(hit, "getBlockPos")), "getBlock");
            if (!isType(block, "world.level.block.AbstractCauldronBlock")) return false;
            Object behavior = call(field(block, "interactions"), "get", stack);
            return denied(player, behavior, "cauldron");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    public static boolean armadillo(Object entity, Object user, Object stack) {
        try {
            return !(boolean)call(entity, "isBaby") && !denied(user, null, "USE_BRUSH")
                && (boolean)call(entity, "brushOffScute", user, stack);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    public static Stream<?> eligible(Stream<?> original, Object level) {
        return original.filter(uuid -> {
            try { return !denied(call(level, "getPlayerByUUID", uuid), null, "UNLOCK_VAULT"); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
        });
    }

    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var specs = NativeComplexSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList();
        Map<NativeComplexSpecs.Spec, Integer> counts = new HashMap<>();
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (!(e instanceof MethodModel m)) { cb.with(e); return; }
            var selected = specs.stream().filter(s -> s.method().equals(m.methodName().stringValue()) && s.descriptor().equals(m.methodType().stringValue())).toList();
            if (selected.isEmpty()) { cb.with(e); return; }
            cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                @Override public void atStart(CodeBuilder b) {
                    for (var s : selected) if (s.anchor().equals("HEAD")) { emit(b, s); counts.merge(s, 1, Integer::sum); }
                }
                @Override public void accept(CodeBuilder b, CodeElement element) {
                    for (var s : selected) {
                        if (s.mode().equals("mine") && element instanceof ReturnInstruction r && r.opcode() == Opcode.IRETURN) {
                            b.aload(0).invokestatic(SELF, "restricted", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT));
                            counts.merge(s, 1, Integer::sum);
                        } else if (element instanceof InvokeInstruction i && s.anchor().equals("L" + i.owner().asInternalName() + ";" + i.name().stringValue() + i.type().stringValue())) {
                            int ordinal = counts.getOrDefault(s, 0); counts.merge(s, 1, Integer::sum);
                            if (s.mode().equals("armadillo")) {
                                b.invokestatic(SELF, "armadillo", MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT)); return;
                            }
                            if (s.mode().equals("vault-stream")) {
                                b.with(element).aload(1).invokestatic(SELF, "eligible", MethodTypeDesc.of(ClassDesc.of("java.util.stream.Stream"), ClassDesc.of("java.util.stream.Stream"), OBJECT)); return;
                            }
                            if (s.ordinal() < 0 || s.ordinal() == ordinal) emit(b, s);
                        }
                    }
                    b.with(element);
                }
            }));
        });
        for (var s : specs) if (counts.getOrDefault(s, 0) != s.sites())
            throw new IllegalStateException("S4J anchor count " + s.id() + "=" + counts.get(s) + "; expected=" + s.sites());
        return output;
    }

    private static void emit(CodeBuilder b, NativeComplexSpecs.Spec s) {
        var pass = b.newLabel();
        if (s.mode().equals("cauldron-fallback"))
            b.aload(1).aload(2).aload(3).aload(5).invokestatic(SELF, "cauldronFallback", MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT, OBJECT));
        else {
            b.aload(s.actor());
            if (s.subject() < 0) b.aconst_null(); else b.aload(s.subject());
            b.ldc(s.mode()).invokestatic(SELF, "denied", DENIED);
        }
        b.ifeq(pass);
        switch (s.result()) {
            case "void" -> b.return_();
            case "false" -> b.iconst_0().ireturn();
            case "state3" -> b.aload(3).areturn();
            default -> b.getstatic(ClassDesc.of("net.minecraft.world.InteractionResult"), s.result(),
                ClassDesc.of("net.minecraft.world.InteractionResult$" + switch (s.result()) {
                    case "PASS" -> "Pass";
                    case "FAIL" -> "Fail";
                    default -> "Success";
                })).areturn();
        }
        b.labelBinding(pass);
    }
    private NativeComplexAgent() {}
}
