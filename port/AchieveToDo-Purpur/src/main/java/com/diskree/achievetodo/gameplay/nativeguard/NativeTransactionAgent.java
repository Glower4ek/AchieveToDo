package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Startup-only, pinned native transaction boundaries. No progression state is stored here. */
public final class NativeTransactionAgent {
    private static final ClassDesc SELF = ClassDesc.of(NativeTransactionAgent.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
    private static final MethodTypeDesc DENIED = MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT);
    private static final MethodTypeDesc ENDER_DENIED = MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT);
    private static final Map<String, String> HASHES = Map.ofEntries(
        // Pinned Purpur 2620 input classes. Generated from the verified server artifact.
        Map.entry("net/minecraft/world/item/component/Consumable", "d4b40af8dff544f98faced1cc0156bc7d681eb6f599b3b5a55bb2cf3c7fc4a7c"),
        Map.entry("net/minecraft/world/entity/LivingEntity", "cb99739aa5b5e23559df93354d10faffb6c938bb656ff38470bbbcf001c6c20a"),
        Map.entry("net/minecraft/world/inventory/ArmorSlot", "3cf48a436eac83317b3099ba7c3ae050572ebc85f8b06856471ea2897f261157"),
        Map.entry("net/minecraft/world/item/equipment/Equippable", "b5e2f9ba68319ed3d8e1a73f3c2f220091f8e71778d10c7f4e5feffbd7f1251c"),
        Map.entry("net/minecraft/world/item/BundleItem", "a3fd8086fadf6b10e7300479e78102bfe7e5008317af4cbee79b3e6c757f22d3"),
        Map.entry("net/minecraft/world/item/component/BundleContents$Mutable", "929eaec408bf875263d1741919bec6f9d3c2d6bed80310daa6dcb06374603f80"),
        Map.entry("net/minecraft/world/level/block/EnderChestBlock", "4834ad3b3230938f24456ba6939a96b4a73793a7aec6b77b12f0c11f85f65624"),
        Map.entry("net/minecraft/world/level/block/ShulkerBoxBlock", "91ff0b8b90a87f6bf59f13eec058dfc4ff06e94927b8de7fab9e3a6a59195940")
    );
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String, String> failures = new HashMap<>();
    private static volatile Map<String, BiPredicate<Object, Object>> callbacks = Map.of();
    private static Object owner;
    private static boolean started;

    public static void initialize(Instrumentation instrumentation) {
        started = true;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type,
                    ProtectionDomain domain, byte[] bytes) {
                if (!HASHES.containsKey(name)) return null;
                try {
                    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!HASHES.get(name).equals(hash)) throw new IllegalStateException("Unpinned native class " + name + ": " + hash);
                    byte[] result = transformNative(name, bytes);
                    synchronized (NativeTransactionAgent.class) { transformed.add(name); }
                    System.out.println("ATD_NATIVE_TRANSFORM " + name + " sha256=" + hash);
                    return result;
                } catch (Throwable failure) {
                    synchronized (NativeTransactionAgent.class) { failures.put(name, failure.toString()); }
                    return null; // Plugin verification refuses readiness; JVM transformer failures alone are not fatal.
                }
            }
        });
    }

    public static synchronized void install(Object token, Map<String, BiPredicate<Object, Object>> guards,
                                             ClassLoader nativeLoader) throws Exception {
        if (!started) throw new IllegalStateException("S4F requires -javaagent:<AchieveToDo plugin JAR> before -jar");
        if (owner != null) throw new IllegalStateException("Native guards already owned by a runtime");
        if (NativeTransactionAgent.class.getClassLoader() != null
            || Class.forName(NativeTransactionAgent.class.getName(), false, nativeLoader) != NativeTransactionAgent.class)
            throw new IllegalStateException("Native guard is not visible to the server classloader");
        for (String name : HASHES.keySet()) Class.forName(name.replace('/', '.'), true, nativeLoader);
        if (!failures.isEmpty() || !transformed.equals(HASHES.keySet()))
            throw new IllegalStateException("Native guard verification failed: " + failures + "; transformed=" + transformed);
        if (!guards.keySet().equals(Set.of("food", "equipment", "bundle", "ender", "shulker")))
            throw new IllegalArgumentException("Incomplete native guards");
        callbacks = Map.copyOf(guards);
        owner = token;
    }

    public static synchronized void retire(Object token) {
        if (owner == token) { callbacks = Map.of(); owner = null; }
    }

    public static boolean food(Object actor, Object stack) { return denied("food", actor, stack); }
    public static boolean equipment(Object actor, Object stack) { return denied("equipment", actor, stack); }
    public static boolean ender(Object actor, Object level, Object pos) {
        if (actor == null) return false;
        if (Boolean.getBoolean("achievetodo.landmarks.exactPrototype"))
            return NativeLandmarkAgent.targetAndAbilityDenied(actor, level, pos, "OPEN_ENDER_CHEST");
        return denied("ender", actor, null);
    }

    public static boolean armorPlace(Object slot, Object stack) {
        try {
            java.lang.reflect.Field field = null;
            for (Class<?> type = slot.getClass(); type != null; type = type.getSuperclass()) {
                try { field = type.getDeclaredField("owner"); break; }
                catch (NoSuchFieldException missing) { /* Native mount slots subclass ArmorSlot. */ }
            }
            if (field == null) throw new NoSuchFieldException("owner");
            field.setAccessible(true);
            Object actor = field.get(slot);
            if (!equipment(actor, stack)) return false;
            close(actor); return true;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    public static boolean armorPickup(boolean original, Object slot, Object actor) {
        if (!original) return false;
        if (!equipment(actor, invoke(slot, "getItem"))) return true;
        close(actor); return false;
    }

    public static boolean bundle(Object actor, Object unused) {
        if (!denied("bundle", actor, unused)) return false;
        close(actor); return true;
    }

    public static boolean bundleModification(boolean original, Object actor) {
        return original && !bundle(actor, null);
    }

    public static boolean shulker(boolean original, Object actor, Object entity) {
        if (!original) return false;
        if (!invoke(entity, "getAnimationStatus").toString().equals("CLOSED")) return true;
        return !denied("shulker", actor, entity);
    }

    private static Object invoke(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static void close(Object actor) {
        // Same synchronous closeContainer site as canonical ArmorSlot/Bundle Mixins.
        for (Class<?> type = actor.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("net.minecraft.server.level.ServerPlayer")) { invoke(actor, "closeContainer"); return; }
    }

    private static boolean denied(String contract, Object actor, Object stack) {
        if (!player(actor)) return false;
        var callback = callbacks.get(contract);
        // Startup, shutdown, reload gaps and failed authority reads remain closed.
        if (callback == null) return true;
        try { return callback.test(actor, stack); }
        catch (RuntimeException | LinkageError unavailable) { return true; }
    }

    private static boolean player(Object actor) {
        for (Class<?> type = actor == null ? null : actor.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("net.minecraft.world.entity.player.Player")) return true;
        return false;
    }

    static byte[] transformNative(String name, byte[] bytes) {
        var cf = ClassFile.of();
        int[] sites = {0};
        ClassTransform transform = (classBuilder, element) -> {
            if (!(element instanceof MethodModel method)) { classBuilder.with(element); return; }
            String m = method.methodName().stringValue();
            boolean food = name.endsWith("/Consumable") && m.equals("startConsuming");
            boolean living = name.endsWith("/LivingEntity") && (m.equals("isEquippableInSlot") || m.equals("canEquipWithDispenser"));
            boolean place = name.endsWith("/ArmorSlot") && m.equals("mayPlace");
            boolean pickup = name.endsWith("/ArmorSlot") && m.equals("mayPickup");
            boolean equip = name.endsWith("/Equippable") && m.equals("swapWithEquipmentSlot");
            boolean transfer = name.endsWith("/BundleContents$Mutable") && m.equals("tryTransfer");
            boolean bundle = name.endsWith("/BundleItem") && m.equals("overrideOtherStackedOnMe");
            boolean ender = name.endsWith("/EnderChestBlock") && m.equals("useWithoutItem");
            boolean shulker = name.endsWith("/ShulkerBoxBlock") && m.equals("useWithoutItem");
            if (!(food || living || place || pickup || equip || transfer || bundle || ender || shulker)) {
                classBuilder.with(element); return;
            }
            var code = new CodeTransform() {
                int ordinal;
                @Override public void atStart(CodeBuilder b) {
                    if (living || place || transfer) {
                        sites[0]++;
                        if (transfer) b.aload(2).aconst_null(); else b.aload(0).aload(1);
                        b.invokestatic(SELF, transfer ? "bundle" : place ? "armorPlace" : "equipment", DENIED)
                            .ifThen(deny -> deny.iconst_0().ireturn());
                    }
                }
                @Override public void accept(CodeBuilder b, CodeElement e) {
                    if (e instanceof InvokeInstruction call) {
                        if (food && call.name().equalsString("consumeTicks")
                            || equip && call.name().equalsString("level")
                            || ender && call.name().equalsString("setActiveChest")) {
                            sites[0]++;
                            b.aload(food ? 1 : equip ? 2 : 4);
                            if (food) b.aload(2);
                            else if (equip) b.aload(1);
                            else if (ender) b.aload(2).aload(3);
                            else b.aconst_null();
                            b.invokestatic(SELF, food ? "food" : equip ? "equipment" : "ender",
                                ender ? ENDER_DENIED : DENIED)
                                .ifThen(deny -> {
                                    if (ender) deny.pop2(); else deny.pop();
                                    deny.getstatic(ClassDesc.of("net.minecraft.world.InteractionResult"), "PASS",
                                        ClassDesc.of("net.minecraft.world.InteractionResult$Pass")).areturn();
                                });
                        }
                        b.with(e);
                        if (pickup && call.owner().asInternalName().equals("net/minecraft/world/inventory/Slot") && call.name().equalsString("mayPickup")) {
                            sites[0]++;
                            b.aload(0).aload(1).invokestatic(SELF, "armorPickup", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT, OBJECT));
                        }
                        if (bundle && call.name().equalsString("allowModification") && ordinal++ == 0) {
                            sites[0]++;
                            b.aload(5).invokestatic(SELF, "bundleModification", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT));
                        }
                        if (shulker && call.name().equalsString("canOpen")) {
                            sites[0]++;
                            b.aload(4).aload(7).invokestatic(SELF, "shulker", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT, OBJECT));
                        }
                        return;
                    }
                    b.with(e);
                }
            };
            classBuilder.transformMethod(method, MethodTransform.transformingCode(code));
        };
        byte[] result = cf.transformClass(cf.parse(bytes), transform);
        int expected = name.endsWith("/LivingEntity") || name.endsWith("/ArmorSlot") ? 2 : 1;
        if (sites[0] != expected) throw new IllegalStateException("Unexpected native sites: " + name + "=" + sites[0]);
        return result;
    }

    /** Live health only; never installs or repairs a missing guard. */
    public static synchronized boolean installed() { return started && owner != null && callbacks.keySet().equals(Set.of("food", "equipment", "bundle", "ender", "shulker")) && failures.isEmpty() && transformed.equals(HASHES.keySet()); }
    private NativeTransactionAgent() {}
}
