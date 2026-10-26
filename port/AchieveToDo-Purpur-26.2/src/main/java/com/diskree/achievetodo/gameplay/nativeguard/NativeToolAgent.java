package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Exact 2620 transaction boundaries, installed after the retained transaction/landmark/action pipelines. */
public final class NativeToolAgent {
    private static final ClassDesc SELF = ClassDesc.of(NativeToolAgent.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
    private static final MethodTypeDesc DENIED = MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, ConstantDescs.CD_String);
    private static final String MATERIAL_FIELD = "achievetodo$toolMaterial";
    private static final String[] MATERIALS = {"WOOD", "STONE", "COPPER", "IRON", "GOLD", "DIAMOND", "NETHERITE"};
    private static final String[] IDS = {"USE_WOODEN_TOOLS", "USE_STONE_TOOLS", "USE_COPPER_TOOLS", "USE_IRON_TOOLS", "USE_GOLDEN_TOOLS", "USE_DIAMOND_TOOLS", "USE_NETHERITE_TOOLS"};
    // Explicit canonical registry identities; neither tags, components nor suffix inference.
    static final String[][] ITEMS = {
        {"WOODEN_SWORD", "WOODEN_PICKAXE", "WOODEN_AXE", "WOODEN_SHOVEL", "WOODEN_HOE", "WOODEN_SPEAR"},
        {"STONE_SWORD", "STONE_PICKAXE", "STONE_AXE", "STONE_SHOVEL", "STONE_HOE", "STONE_SPEAR"},
        {"COPPER_SWORD", "COPPER_PICKAXE", "COPPER_AXE", "COPPER_SHOVEL", "COPPER_HOE", "COPPER_SPEAR"},
        {"IRON_SWORD", "IRON_PICKAXE", "IRON_AXE", "IRON_SHOVEL", "IRON_HOE", "IRON_SPEAR"},
        {"GOLDEN_SWORD", "GOLDEN_PICKAXE", "GOLDEN_AXE", "GOLDEN_SHOVEL", "GOLDEN_HOE", "GOLDEN_SPEAR"},
        {"DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_AXE", "DIAMOND_SHOVEL", "DIAMOND_HOE", "DIAMOND_SPEAR"},
        {"NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL", "NETHERITE_HOE", "NETHERITE_SPEAR"}
    };
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String, String> failures = new HashMap<>();
    private static volatile BiPredicate<Object, String> guard;
    private static Object owner;
    private static boolean started;

    public static void initialize(Instrumentation instrumentation) {
        started = true;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!NativeToolSpecs.INPUTS.containsKey(name)) return null;
                try {
                    verifyPin(name, bytes, Boolean.getBoolean("achievetodo.landmarks.exactPrototype") ? 1 : 0);
                    byte[] output = transformNative(name, bytes, loader);
                    synchronized (NativeToolAgent.class) { transformed.add(name); }
                    System.out.println("ATD_TOOL_TRANSFORM " + name);
                    return output;
                } catch (Throwable failure) {
                    synchronized (NativeToolAgent.class) { failures.put(name, failure.toString()); }
                    return null;
                }
            }
        });
    }

    static void verifyPin(String name, byte[] bytes, int pipeline) throws Exception {
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!actual.equals(NativeToolSpecs.INPUTS.get(name).get(pipeline)))
            throw new IllegalStateException("Unpinned S4I input " + name + ": " + actual);
    }

    public static synchronized void install(Object token, BiPredicate<Object, String> callback, ClassLoader loader) throws Exception {
        if (!started || NativeToolAgent.class.getClassLoader() != null
            || Class.forName(NativeToolAgent.class.getName(), false, loader) != NativeToolAgent.class)
            throw new IllegalStateException("S4I requires pinned startup agent and bootstrap identity");
        if (owner != null) throw new IllegalStateException("S4I callbacks already owned");
        for (String name : NativeToolSpecs.INPUTS.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(NativeToolSpecs.INPUTS.keySet()))
            throw new IllegalStateException("S4I verification failed: " + failures + "; transformed=" + transformed);
        owner = Objects.requireNonNull(token);
        guard = Objects.requireNonNull(callback);
    }

    public static synchronized void retire(Object token) {
        if (owner == token) { guard = null; owner = null; }
    }

    private static Object call(Object value, String name) throws ReflectiveOperationException {
        return value.getClass().getMethod(name).invoke(value);
    }

    private static Object slot(Object actor, Object slot) throws ReflectiveOperationException {
        return actor.getClass().getMethod("getItemBySlot", slot.getClass()).invoke(actor, slot);
    }

    static String identity(Object item, boolean spearOnly) throws ReflectiveOperationException {
        Class<?> items = Class.forName("net.minecraft.world.item.Items", true, item.getClass().getClassLoader());
        for (int i = 0; i < ITEMS.length; i++)
            for (int j = spearOnly ? 5 : 0; j < ITEMS[i].length; j++)
                if (item == items.getField(ITEMS[i][j]).get(null)) return IDS[i];
        return null;
    }

    private static String material(Object item) throws ReflectiveOperationException {
        Class<?> base = item.getClass();
        while (base != null && !Set.of("net.minecraft.world.item.AxeItem", "net.minecraft.world.item.ShovelItem", "net.minecraft.world.item.HoeItem").contains(base.getName())) base = base.getSuperclass();
        if (base == null) return null;
        Field field = base.getDeclaredField(MATERIAL_FIELD); field.setAccessible(true);
        Object value = field.get(item);
        if (value == null) return null;
        for (int i = 0; i < MATERIALS.length; i++)
            if (value == value.getClass().getField(MATERIALS[i]).get(null)) return IDS[i];
        return null; // Equal record values are not canonical singleton identity.
    }

    public static boolean restricted(boolean original, Object actor) {
        return original || denied(actor, null, "mine");
    }

    public static boolean permitted(boolean original, Object item, Object context) {
        return original && !denied(context, item, "context");
    }

    public static boolean denied(Object actor, Object subject, String mode) {
        try {
            String ability;
            if (mode.equals("context")) {
                Object context = actor; actor = call(context, "getPlayer");
                if (!player(actor)) return false;
                ability = material(subject);
            } else {
                if (!player(actor)) return false;
                Object stack = switch (mode) {
                    case "mine" -> call(actor, "getMainHandItem");
                    case "attack", "post" -> call(actor, "getWeaponItem");
                    case "slot" -> slot(actor, subject);
                    default -> subject;
                };
                ability = identity(call(stack, "getItem"), Set.of("post", "slot", "spear").contains(mode));
            }
            if (ability == null) return false;
            var callback = guard;
            return callback == null || callback.test(actor, ability);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    /** Pure canonical candidate lookups precede the Purpur evaluator's sounds and particles. */
    public static boolean axeDenied(Object item, Object actor, Object state) {
        if (!player(actor)) return false;
        try {
            Class<?> type = Class.forName("net.minecraft.world.item.AxeItem", false, item.getClass().getClassLoader());
            Field strip = type.getDeclaredField("STRIPPABLES"); strip.setAccessible(true);
            Object block = call(state, "getBlock");
            boolean candidate = ((Map<?, ?>) strip.get(null)).containsKey(block);
            Class<?> weather = Class.forName("net.minecraft.world.level.block.WeatheringCopper", false, type.getClassLoader());
            Class<?> blockState = Class.forName("net.minecraft.world.level.block.state.BlockState", false, type.getClassLoader());
            candidate |= ((Optional<?>) weather.getMethod("getPrevious", blockState).invoke(null, state)).isPresent();
            Class<?> honeycomb = Class.forName("net.minecraft.world.item.HoneycombItem", false, type.getClassLoader());
            Object wax = ((java.util.function.Supplier<?>) honeycomb.getField("WAX_OFF_BY_BLOCK").get(null)).get();
            candidate |= ((Map<?, ?>) wax).containsKey(block);
            if (!candidate) return false;
            String ability = material(item);
            var callback = guard;
            return ability != null && (callback == null || callback.test(actor, ability));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    private static boolean player(Object value) {
        for (Class<?> type = value == null ? null : value.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("net.minecraft.world.entity.player.Player")) return true;
        return false;
    }

    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var specs = NativeToolSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList();
        Map<NativeToolSpecs.Spec, Integer> counts = new HashMap<>();
        boolean capture = specs.stream().anyMatch(s -> s.mode().equals("capture"));
        byte[] output = cf.transformClass(cf.parse(bytes), (cb, e) -> {
            if (capture && e instanceof FieldModel f && f.fieldName().equalsString(MATERIAL_FIELD))
                throw new IllegalStateException("Duplicate tool transformation " + name);
            if (!(e instanceof MethodModel m)) { cb.with(e); return; }
            var selected = specs.stream().filter(s -> s.method().equals(m.methodName().stringValue()) && s.descriptor().equals(m.methodType().stringValue())).toList();
            if (selected.isEmpty()) { cb.with(e); return; }
            cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                @Override public void atStart(CodeBuilder b) {
                    for (var s : selected) if (s.anchor().equals("HEAD")) { emit(b, s); counts.merge(s, 1, Integer::sum); }
                }
                @Override public void accept(CodeBuilder b, CodeElement element) {
                    for (var s : selected) {
                        if (s.mode().equals("capture") && element instanceof ReturnInstruction r && r.opcode() == Opcode.RETURN) {
                            b.aload(0).aload(1).putfield(ClassDesc.ofInternalName(name), MATERIAL_FIELD, OBJECT);
                            counts.merge(s, 1, Integer::sum);
                        } else if (s.mode().equals("mine") && element instanceof ReturnInstruction r && r.opcode() == Opcode.IRETURN) {
                            b.aload(0).invokestatic(SELF, "restricted", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT));
                            counts.merge(s, 1, Integer::sum);
                        } else if (element instanceof InvokeInstruction i && s.anchor().equals("L" + i.owner().asInternalName() + ";" + i.name().stringValue() + i.type().stringValue())) {
                            int ordinal = counts.getOrDefault(s, 0); counts.merge(s, 1, Integer::sum);
                            if (s.ordinal() < 0 || s.ordinal() == ordinal) emit(b, s);
                        }
                    }
                    b.with(element);
                }
            }));
        });
        for (var s : specs) if (counts.getOrDefault(s, 0) != s.sites())
            throw new IllegalStateException("S4I anchor count " + s.id() + "=" + counts.get(s) + "; expected=" + s.sites());
        // Add the constructor-captured field once, without changing Item components or registry identity.
        if (capture) output = cf.transformClass(cf.parse(output), ClassTransform.endHandler(cb ->
            cb.withField(MATERIAL_FIELD, OBJECT, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)));
        return output;
    }

    private static void emit(CodeBuilder b, NativeToolSpecs.Spec s) {
        var pass = b.newLabel();
        if (s.mode().equals("axe"))
            b.aload(0).aload(3).aload(4).invokestatic(SELF, "axeDenied", MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT));
        else {
            b.aload(s.actor());
            if (s.subject() < 0) b.aconst_null(); else b.aload(s.subject());
            b.ldc(s.mode()).invokestatic(SELF, "denied", DENIED);
        }
        b.ifeq(pass);
        switch (s.result()) {
            case "void" -> b.return_();
            case "false" -> b.iconst_0().ireturn();
            case "empty" -> b.invokestatic(ClassDesc.of("java.util.Optional"), "empty", MethodTypeDesc.of(ClassDesc.of("java.util.Optional"))).areturn();
            default -> b.getstatic(ClassDesc.of("net.minecraft.world.InteractionResult"), s.result(), ClassDesc.of("net.minecraft.world.InteractionResult$" + (s.result().equals("FAIL") ? "Fail" : "Pass"))).areturn();
        }
        b.labelBinding(pass);
    }
    private NativeToolAgent() {}
}
