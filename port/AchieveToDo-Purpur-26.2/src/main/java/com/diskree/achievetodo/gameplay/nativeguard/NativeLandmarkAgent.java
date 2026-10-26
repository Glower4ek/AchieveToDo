package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;

/** Independent S4G opt-in prototype. It does not certify the 22 complete landmark abilities. */
public final class NativeLandmarkAgent {
    private static final ClassDesc SELF = ClassDesc.of(NativeLandmarkAgent.class.getName());
    private static final ClassDesc CAPTURE = ClassDesc.of(NativeLandmarkCapture.class.getName());
    private static final ClassDesc OBJECT = ConstantDescs.CD_Object;
    private static final Map<String, String> BASE_HASHES = Map.ofEntries(
        Map.entry("net/minecraft/world/level/levelgen/structure/Structure", "c4ea9bbb11c9ca72fe3de924f0e4c9f226a887373fbe8d93d7356251143ca2e4"),
        Map.entry("net/minecraft/world/level/levelgen/structure/StructureStart", "4787740161609dcc3c746bc14d175685d0fb7df1ddb07bed7de05e5ac45923f1"),
        Map.entry("net/minecraft/server/level/WorldGenRegion", "0d4dbbc135e79f295e4d94aa6dff687fbe0bb9797d1d9626258e3b8f2a0bdaeb"),
        Map.entry("net/minecraft/world/level/Level", "d571f8101512c6ca6a2aa05160a592d19630d126204c24e68c8502d3a69ffbc3"),
        Map.entry("net/minecraft/world/level/levelgen/feature/Feature", "918a7c20cd2acc836ade09dd31a98d1b591a70b4345706e047056026eddfbe44"),
        Map.entry("net/minecraft/world/entity/player/Player", "b0d7e891842135a1bc5dc5e245496dbd98e4578bfd66662737f3fa2b61229128")
    );
    private static final Map<String, String> HASHES;
    static {
        var hashes = new HashMap<>(BASE_HASHES);
        hashes.putAll(NativeLandmarkActionSpecs.HASHES);
        HASHES = Map.copyOf(hashes);
    }
    private static final Set<String> transformed = new HashSet<>();
    private static final Map<String, String> failures = new HashMap<>();
    private static final Map<List<Object>, Object[]> changed = new LinkedHashMap<>();
    private static boolean overflow;
    private static Object owner;
    private static volatile BiPredicate<Object, Object> guard;
    private static boolean enabled;
    private static volatile boolean failed;

    public static void initialize(Instrumentation instrumentation) {
        enabled = Boolean.getBoolean("achievetodo.landmarks.exactPrototype");
        if (!enabled) return;
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!HASHES.containsKey(name)) return null;
                try {
                    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                    if (!HASHES.get(name).equals(hash)) throw new IllegalStateException("Unpinned S4G native class " + name + ": " + hash);
                    byte[] result = transformNative(name, bytes, loader);
                    synchronized (NativeLandmarkAgent.class) { transformed.add(name); }
                    System.out.println("ATD_LANDMARK_TRANSFORM " + name + " sha256=" + hash);
                    return result;
                } catch (Throwable failure) {
                    failed = true;
                    synchronized (NativeLandmarkAgent.class) { failures.put(name, failure.toString()); }
                    return null;
                }
            }
        });
    }
    static boolean captureAvailable() { return !failed; }

    public static synchronized void install(Object token, BiPredicate<Object, Object> callback, ClassLoader loader) throws Exception {
        if (!enabled) throw new IllegalStateException("S4G prototype not enabled at premain");
        if (owner != null || NativeLandmarkAgent.class.getClassLoader() != null
            || Class.forName(NativeLandmarkAgent.class.getName(), false, loader) != NativeLandmarkAgent.class)
            throw new IllegalStateException("S4G ownership/bootstrap mismatch");
        for (String name : HASHES.keySet()) Class.forName(name.replace('/', '.'), true, loader);
        if (!failures.isEmpty() || !transformed.equals(HASHES.keySet())) throw new IllegalStateException("S4G transforms unavailable: " + failures);
        NativeLandmarkSupplementAgent.verifyInstalled(loader);
        owner = token; guard = callback;
    }
    public static synchronized void retire(Object token) {
        if (owner == token) { guard = null; owner = null; changed.clear(); overflow = false; }
    }
    public static boolean blockRestricted(boolean original, Object actor, Object level, Object pos) {
        if (original) return true;
        return targetDenied(actor, level, pos);
    }
    public static boolean targetDenied(Object actor, Object level, Object pos) {
        return targetAndAbilityDenied(actor, level, pos, null);
    }
    public static boolean targetAndAbilityDenied(Object actor, Object level, Object pos, String ability) {
        if (actor == null) return false;
        var callback = guard;
        if (callback == null) return true;
        try { return callback.test(actor, new Object[]{level, pos, ability}); }
        catch (RuntimeException | LinkageError unavailable) { return true; }
    }
    static synchronized void changed(Object level, int x, int z) {
        if (owner == null) return;
        if (changed.size() >= 4096) { overflow = true; return; }
        changed.put(List.of(level, x, z), new Object[]{level, x, z});
    }
    public static synchronized Object[] drain() {
        Object[] result = {overflow, List.copyOf(changed.values())};
        changed.clear(); overflow = false; return result;
    }

    static byte[] transformNative(String name, byte[] bytes, ClassLoader loader) {
        if (NativeLandmarkActionSpecs.HASHES.containsKey(name)) return NativeLandmarkActions.transform(name,bytes,loader);
        var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        int[] sites = {0};
        ClassTransform transform = new ClassTransform() {
            @Override public void atStart(ClassBuilder cb) {
                if (name.endsWith("/WorldGenRegion") || name.equals("net/minecraft/world/level/Level"))
                    cb.withField("atd$s4gCapture", OBJECT, ClassFile.ACC_PUBLIC);
            }
            @Override public void accept(ClassBuilder cb, ClassElement element) {
                if (!(element instanceof MethodModel m)) { cb.with(element); return; }
                String method = m.methodName().stringValue();
                boolean generate = name.endsWith("/Structure") && method.equals("generate");
                boolean place = name.endsWith("/StructureStart") && method.equals("placeInChunk");
                boolean block = name.endsWith("/WorldGenRegion") && method.equals("setBlock");
                boolean levelBlock = name.equals("net/minecraft/world/level/Level") && method.equals("setBlock")
                    && m.methodType().stringValue().equals("(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z");
                boolean feature = name.endsWith("/Feature") && method.equals("place") && m.methodTypeSymbol().parameterCount() == 5;
                boolean player = name.endsWith("/Player") && method.equals("blockActionRestricted");
                if (!(generate || place || block || levelBlock || feature || player)) { cb.with(element); return; }
                long returns = m.code().orElseThrow().elementStream().filter(e -> e instanceof ReturnInstruction).count();
                cb.transformMethod(m, MethodTransform.transformingCode(new CodeTransform() {
                    int ordinal;
                    Label start, end, handler;
                    @Override public void atStart(CodeBuilder b) {
                        if (place) { start = b.newBoundLabel(); end = b.newLabel(); handler = b.newLabel(); }
                    }
                    @Override public void accept(CodeBuilder b, CodeElement e) {
                        if (e instanceof ReturnInstruction) {
                            if (generate && ordinal == 0) {
                                sites[0]++; b.dup().aload(1).aload(2).invokestatic(CAPTURE, "generated", MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT, OBJECT, OBJECT));
                            }
                            if (block && ordinal == returns - 1) {
                                sites[0]++; b.aload(0).aload(1).invokestatic(CAPTURE, "block", MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT, OBJECT));
                            }
                            if (player) {
                                sites[0]++; b.aload(0).aload(1).aload(2).invokestatic(SELF, "blockRestricted", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT));
                            }
                            if (levelBlock) {
                                sites[0]++; b.aload(0).aload(1).invokestatic(CAPTURE, "levelBlock", MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_boolean, OBJECT, OBJECT));
                            }
                            ordinal++;
                        }
                        if (feature && e instanceof InvokeInstruction i && i.name().equalsString("place")
                            && i.typeSymbol().parameterCount() == 1) {
                            sites[0]++; b.invokestatic(CAPTURE, "feature", MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT)); return;
                        }
                        b.with(e);
                        if (place && e instanceof InvokeInstruction i) {
                            if (i.name().equalsString("getCenter")) {
                                sites[0]++; b.aload(0).aload(1).invokestatic(CAPTURE, "beginStructure", MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT, OBJECT));
                            }
                            if (i.name().equalsString("afterPlace")) {
                                sites[0]++; b.aload(0).aload(1).invokestatic(CAPTURE, "endStructure", MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT, OBJECT));
                            }
                        }
                    }
                    @Override public void atEnd(CodeBuilder b) {
                        if (place) {
                            b.labelBinding(end).labelBinding(handler).aload(1)
                                .invokestatic(CAPTURE, "abandon", MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT)).athrow()
                                .exceptionCatchAll(start, end, handler);
                        }
                    }
                }));
            }
        };
        byte[] result = cf.transformClass(cf.parse(bytes), transform);
        int expected = name.endsWith("/StructureStart") ? 2 : name.endsWith("/Player") ? 4 : name.equals("net/minecraft/world/level/Level") ? 6 : 1;
        if (sites[0] != expected) throw new IllegalStateException("S4G native sites " + name + "=" + sites[0] + " expected=" + expected);
        return result;
    }
    private NativeLandmarkAgent() {}
}
