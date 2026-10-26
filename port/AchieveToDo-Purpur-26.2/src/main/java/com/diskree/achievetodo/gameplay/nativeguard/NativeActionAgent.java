package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Exact S4H transaction anchors, installed after the existing S4F and optional S4G transformers. */
public final class NativeActionAgent {
    private static final ClassDesc SELF = ClassDesc.of(NativeActionAgent.class.getName());
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
                if (!NativeActionSpecs.INPUTS.containsKey(name)) return null;
                try {
                    var pins = NativeActionSpecs.INPUTS.get(name);
                    String expected = Boolean.getBoolean("achievetodo.landmarks.exactPrototype") ? pins.get(1) : pins.get(0);
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!actual.equals(expected)) throw new IllegalStateException("Unpinned S4H input " + name + ": " + actual);
                    byte[] result = transformNative(name, bytes, loader);
                    synchronized (NativeActionAgent.class) { transformed.add(name); }
                    System.out.println("ATD_ACTION_TRANSFORM " + name + " sha256=" + actual);
                    return result;
                } catch (Throwable failure) {
                    synchronized (NativeActionAgent.class) { failures.put(name, failure.toString()); }
                    return null; // Installation refuses readiness when even one required boundary failed.
                }
            }
        });
    }

    public static synchronized void install(Object token, BiPredicate<Object, String> callback, ClassLoader loader) throws Exception {
        if (!started || NativeActionAgent.class.getClassLoader() != null
            || Class.forName(NativeActionAgent.class.getName(), false, loader) != NativeActionAgent.class)
            throw new IllegalStateException("S4H requires the pinned startup agent and bootstrap identity");
        if (owner != null) throw new IllegalStateException("S4H callbacks already owned");
        for (String name : NativeActionSpecs.INPUTS.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(NativeActionSpecs.INPUTS.keySet()))
            throw new IllegalStateException("S4H verification failed: " + failures + "; transformed=" + transformed);
        owner = token;
        guard = Objects.requireNonNull(callback);
    }

    public static synchronized void retire(Object token) {
        if (owner == token) { guard = null; owner = null; }
    }

    public static boolean denied(Object actor, Object subject, String mode) {
        if (!player(actor)) return false;
        try {
            String ability = mode;
            if (mode.equals("shield")) {
                if (!isType(subject, "net.minecraft.world.item.ShieldItem")) return false;
                ability = "USE_SHIELD";
            } else if (mode.equals("bottle")) {
                if (!exactItem(subject, "OMINOUS_BOTTLE")) return false;
                ability = "USE_OMINOUS_BOTTLE";
            } else if (mode.equals("attack")) {
                Object stack = actor.getClass().getMethod("getWeaponItem").invoke(actor);
                ability = exactItem(stack, "MACE") ? "ATTACK_WITH_MACE"
                    : exactItem(stack, "TRIDENT") ? "ATTACK_WITH_TRIDENT" : null;
                if (ability == null) return false;
            }
            var callback = guard;
            return callback == null || callback.test(actor, ability);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    private static boolean exactItem(Object stack, String item) throws ReflectiveOperationException {
        Object value = stack.getClass().getMethod("getItem").invoke(stack);
        return value == Class.forName("net.minecraft.world.item.Items", true, stack.getClass().getClassLoader()).getField(item).get(null);
    }

    public static boolean composter(Object actor, int level) {
        // Paper process() handles level 7 without addItem. Canonical restriction applies only to addItem.
        return level < 7 && denied(actor, null, "USE_COMPOSTER");
    }

    public static boolean crystal(boolean original, Object context) {
        if (!original) return false;
        try { return !denied(context.getClass().getMethod("getPlayer").invoke(context), null, "PLACE_END_CRYSTAL"); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    private static boolean player(Object actor) { return isType(actor, "net.minecraft.world.entity.player.Player"); }
    private static boolean isType(Object value, String name) {
        for (Class<?> type = value == null ? null : value.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals(name)) return true;
        return false;
    }

    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var specs = NativeActionSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList();
        Map<NativeActionSpecs.Spec, Integer> counts = new HashMap<>();
        var output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (!(e instanceof MethodModel m)) { cb.with(e); return; }
            var selected = specs.stream().filter(s -> s.method().equals(m.methodName().stringValue())
                && s.descriptor().equals(m.methodType().stringValue())).toList();
            if (selected.isEmpty()) { cb.with(e); return; }
            cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                @Override public void atStart(CodeBuilder b) {
                    for (var s : selected) if (s.anchor().equals("HEAD")) { emit(b, s, m, 0); counts.merge(s, 1, Integer::sum); }
                }
                @Override public void accept(CodeBuilder b, CodeElement element) {
                    if (element instanceof InvokeInstruction i) {
                        String key = "L" + i.owner().asInternalName() + ";" + i.name().stringValue() + i.type().stringValue();
                        for (var s : selected) if (s.anchor().equals(key) && !s.mode().equals("crystal")) {
                            emit(b, s, m, counts.getOrDefault(s, 0)); counts.merge(s, 1, Integer::sum);
                        }
                        b.with(element);
                        for (var s : selected) if (s.anchor().equals(key) && s.mode().equals("crystal")) {
                            b.aload(1).invokestatic(SELF, "crystal", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT));
                            counts.merge(s, 1, Integer::sum);
                        }
                        return;
                    }
                    b.with(element);
                }
            }));
        });
        for (var s : specs) if (counts.getOrDefault(s, 0) != s.sites())
            throw new IllegalStateException("S4H anchor count " + s.id() + "=" + counts.get(s) + "; expected=" + s.sites());
        return output;
    }

    private static void emit(CodeBuilder b, NativeActionSpecs.Spec s, MethodModel method, int ordinal) {
        var pass = b.newLabel();
        b.aload(s.actor());
        if (s.mode().equals("composter"))
            // Initial process uses local 8; Purpur's bulk loop passes its current level from local 13.
            b.iload(ordinal == 0 ? 8 : 13).invokestatic(SELF, "composter", MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, ConstantDescs.CD_int));
        else {
            if (s.subject() >= 0) b.aload(s.subject()); else b.aconst_null();
            b.ldc(s.mode()).invokestatic(SELF, "denied", DENIED);
        }
        b.ifeq(pass);
        if (s.result().equals("void")) b.return_();
        else if (s.result().equals("false")) b.iconst_0().ireturn();
        else b.getstatic(ClassDesc.of("net.minecraft.world.InteractionResult"), s.result(),
            ClassDesc.of("net.minecraft.world.InteractionResult$" + switch (s.result()) {
                case "PASS" -> "Pass";
                case "FAIL" -> "Fail";
                case "TRY_WITH_EMPTY_HAND" -> "TryEmptyHandInteraction";
                default -> "Success";
            })).areturn();
        b.labelBinding(pass);
    }
    private NativeActionAgent() {}
}
