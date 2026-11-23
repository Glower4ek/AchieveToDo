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

    public static boolean restricted(boolean original, Object actor) {
        return original || denied(actor, null, "mine");
    }
    public static boolean restricted(boolean original,Object actor,Object level,Object target) {
        if(original)return true;
        if(!player(actor))return false;
        try {
            var callback=guard;if(callback==null)return true;
            int targetY=(int)NativeLandmarkCapture.call(target,"getY");
            if(targetY<0 && callback.test(actor,"BASIC:BREAK_BLOCKS_IN_NEGATIVE_Y"))return true;
            if(callback.test(actor,"BASIC:BREAK_BLOCKS"))return true;
            return denied(actor,null,"mine");
        } catch(RuntimeException | LinkageError unavailable){return true;}
    }

    public static boolean permitted(boolean original, Object item, Object context) {
        return original && !denied(context, item, "context");
    }

    public static boolean denied(Object actor, Object subject, String mode) {
        try {
            String ability;
            if (mode.equals("context")) {
                // 26.3 block transforms: the tool is the item in the used hand; its material is its registry identity.
                Object context = actor; actor = call(context, "getPlayer");
                if (!player(actor)) return false;
                ability = identity(call(call(context, "getItemInHand"), "getItem"), false);
            } else {
                if (!player(actor)) return false;
                Object stack = switch (mode) {
                    case "mine" -> call(actor, "getMainHandItem");
                    case "attack", "post" -> call(actor, "getWeaponItem");
                    case "slot" -> slot(actor, subject);
                    default -> subject;
                };
                ability = identity(call(stack, "getItem"), Set.of("post", "slot", "spear", "kinetic").contains(mode));
            }
            if (ability == null) return false;
            var callback = guard;
            String reporting=mode.equals("kinetic")?"SILENT:"
                : Set.of("attack","post","slot","spear").contains(mode)?"COMBAT:":"";
            return callback == null || callback.test(actor, reporting+ability);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return true; }
    }

    private static boolean player(Object value) {
        for (Class<?> type = value == null ? null : value.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("net.minecraft.world.entity.player.Player")) return true;
        return false;
    }

    public static boolean combatTargetDenied(Object actor,Object target) {
        return Boolean.getBoolean("achievetodo.landmarks.exactPrototype") && NativeLandmarkActions.combatEntityDenied(actor,target);
    }
    private static void verifyCombatMethods(String name,byte[] bytes) {
        var parsed=ClassFile.of().parse(bytes);
        for(var site:NativeToolSpecs.COMBAT_SITES)if(site.owner().equals(name)) {
            var methods=parsed.methods().stream().filter(m->m.methodName().equalsString(site.method()) && m.methodType().equalsString(site.descriptor())).toList();
            if(methods.size()!=1 || methods.getFirst().code().isEmpty())throw new IllegalStateException("Missing combat method "+site);
            long returns=methods.getFirst().code().orElseThrow().elementStream().filter(e->e instanceof ReturnInstruction).count();
            if(returns!=site.nativeReturns())throw new IllegalStateException("Changed native combat returns "+site+"="+returns);
        }
    }
    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        verifyCombatMethods(name,bytes);

        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var specs = NativeToolSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList();
        Map<NativeToolSpecs.Spec, Integer> counts = new HashMap<>();
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
                            b.aload(0).aload(1).aload(2).invokestatic(SELF, "restricted", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT));
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
        return transformCombatScopes(name,output,loader);
    }

    private static byte[] transformCombatScopes(String name,byte[] bytes,ClassLoader loader) {
        var cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        var scope=ClassDesc.of(NativeToolCombatInvocation.class.getName());
        var owner=ClassDesc.ofInternalName(name);
        return cf.transformClass(cf.parse(bytes),(cb,e)->{
            if(!(e instanceof MethodModel m)){cb.with(e);return;}
            if(m.methodName().stringValue().startsWith("achievetodo$combatBody$"))throw new IllegalStateException("Duplicate combat transform "+name);
            var selected=NativeToolSpecs.COMBAT_SITES.stream().filter(s->s.owner().equals(name) && m.methodName().equalsString(s.method()) && m.methodType().equalsString(s.descriptor())).findFirst();
            if(selected.isEmpty() || selected.get().kind().equals("stab")){cb.with(e);return;}
            String kind=selected.get().kind();
            if(kind.equals("predicate")) {
                int[] returns={0};
                cb.transformMethod(m,MethodTransform.transformingCode((b,element)->{
                    if(element instanceof ReturnInstruction r && r.opcode()==Opcode.IRETURN){returns[0]++;b.aload(0).aload(1).invokestatic(scope,"scopedTargetAllowed",MethodTypeDesc.of(ConstantDescs.CD_boolean,ConstantDescs.CD_boolean,OBJECT,OBJECT));}
                    b.with(element);
                }));
                if(returns[0]!=1)throw new IllegalStateException("Scoped predicate return count "+returns[0]);return;
            }
            boolean piercing=kind.equals("piercing");String body="achievetodo$combatBody$"+m.methodName().stringValue();
            cb.withMethod(body,m.methodTypeSymbol(),ClassFile.ACC_PRIVATE|ClassFile.ACC_SYNTHETIC,mb->mb.with(m.code().orElseThrow()));
            cb.withMethod(m.methodName().stringValue(),m.methodTypeSymbol(),m.flags().flagsMask(),mb->{
                for(MethodElement element:m)if(!(element instanceof AccessFlags) && !(element instanceof CodeModel))mb.with(element);
                mb.withCode(b->{
                    int token=piercing?3:5,thrown=token+1;
                    b.aload(piercing?1:3).aload(piercing?2:4);
                    if(piercing)b.aconst_null();else b.aload(1);
                    b.ldc(piercing?1:0).invokestatic(scope,"open",MethodTypeDesc.of(scope,OBJECT,OBJECT,OBJECT,ConstantDescs.CD_boolean)).astore(token);
                    var start=b.newBoundLabel();var end=b.newLabel();var handler=b.newLabel();
                    b.aload(0).aload(1);
                    if(piercing)b.aload(2);else b.iload(2).aload(3).aload(4);
                    b.invokespecial(owner,body,m.methodTypeSymbol());
                    if(piercing)b.aload(token).invokestatic(scope,"normal",MethodTypeDesc.of(ConstantDescs.CD_void,scope));
                    b.labelBinding(end).aload(token).invokestatic(scope,"finish",MethodTypeDesc.of(ConstantDescs.CD_void,scope)).return_();
                    b.labelBinding(handler).astore(thrown).aload(token).aload(thrown).invokestatic(scope,"failed",MethodTypeDesc.of(ConstantDescs.CD_void,scope,ClassDesc.of("java.lang.Throwable")))
                        .aload(thrown).athrow().exceptionCatchAll(start,end,handler);
                });
            });
        });
    }

    private static void emit(CodeBuilder b, NativeToolSpecs.Spec s) {
        if(s.id().equals("stab")) {
            var targetAllowed=b.newLabel();
            b.aload(0).aload(2).invokestatic(SELF,"combatTargetDenied",MethodTypeDesc.of(ConstantDescs.CD_boolean,OBJECT,OBJECT))
                .ifeq(targetAllowed).iconst_0().ireturn().labelBinding(targetAllowed);
        }
        var pass = b.newLabel();
        b.aload(s.actor());
        if (s.subject() < 0) b.aconst_null(); else b.aload(s.subject());
        b.ldc(s.id().equals("kinetic-contact")?"kinetic":s.mode()).invokestatic(SELF, "denied", DENIED);
        b.ifeq(pass);
        switch (s.result()) {
            case "void" -> b.return_();
            case "false" -> b.iconst_0().ireturn();
            case "empty" -> b.invokestatic(ClassDesc.of("java.util.Optional"), "empty", MethodTypeDesc.of(ClassDesc.of("java.util.Optional"))).areturn();
            default -> b.getstatic(ClassDesc.of("net.minecraft.world.InteractionResult"), s.result(), ClassDesc.of("net.minecraft.world.InteractionResult$" + (s.result().equals("FAIL") ? "Fail" : "Pass"))).areturn();
        }
        b.labelBinding(pass);
    }
    /** Live health only; never installs or repairs a missing guard. */
    public static synchronized boolean installed() { return started && owner != null && guard != null && failures.isEmpty() && transformed.equals(NativeToolSpecs.INPUTS.keySet()); }
    private NativeToolAgent() {}
}
