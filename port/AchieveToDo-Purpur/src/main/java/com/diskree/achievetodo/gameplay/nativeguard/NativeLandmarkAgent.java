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
        Map.entry("net/minecraft/world/level/levelgen/structure/Structure", "36ec2555b84189d2a577e0ad94804f34677830e84f84e369b32d965504bd66d6"),
        Map.entry("net/minecraft/world/level/levelgen/structure/StructureStart", "4787740161609dcc3c746bc14d175685d0fb7df1ddb07bed7de05e5ac45923f1"),
        Map.entry("net/minecraft/server/level/WorldGenRegion", "3c02f5f98302777bb5df7deb3f76e4ecf38c19dd67ccc91250d0b62fd11359c1"),
        Map.entry("net/minecraft/world/level/Level", "61ee3deb88a01e2430b8d1a0fc669d184752c549d63c0ef69c0bab4ba326a9a3"),
        // 26.3: features are configured instances keyed by ResourceKey and placed by FeaturePlacer.
        Map.entry("net/minecraft/world/level/levelgen/placement/FeaturePlacer", "90fc159ad751e4c6d9d82dbe61fb602c515a12a11089fb7c8b59a583fd7bc0a7"),
        Map.entry("net/minecraft/world/entity/player/Player", "3296e4d38bda6d7eb5ced65fb5a967b06a5ede8588f45ecaf5900d63d89cd118")
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
        return targetAndAbilityDenied(actor,level,pos,ability,"ORDINARY");
    }
    public static boolean targetAndAbilityDenied(Object actor,Object level,Object pos,String ability,String reporting) {
        if(actor==null)return false;
        if(!Set.of("ORDINARY","COMBAT","SILENT","SHELF").contains(reporting))throw new IllegalArgumentException("Unknown denial reporter mode");
        var callback=guard;if(callback==null)return true;
        try{return callback.test(actor,new Object[]{level,pos,ability,reporting});}
        catch(RuntimeException | LinkageError unavailable){return true;}
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
                boolean feature = name.endsWith("/FeaturePlacer") && method.equals("place") && m.methodType().stringValue()
                    .equals("(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Z)Z");
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
                            && i.owner().asInternalName().equals("net/minecraft/world/level/levelgen/feature/Feature")
                            && i.typeSymbol().parameterCount() == 4) {
                            // Stack: feature, level, generator, random, origin; the placed feature (slot 1) carries the key.
                            sites[0]++; b.aload(1).invokestatic(CAPTURE, "placed",
                                MethodTypeDesc.of(ConstantDescs.CD_boolean, OBJECT, OBJECT, OBJECT, OBJECT, OBJECT, OBJECT)); return;
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
        int expected = name.endsWith("/StructureStart") ? 2 : name.endsWith("/FeaturePlacer") ? 2 : name.endsWith("/Player") ? 4
            : name.equals("net/minecraft/world/level/Level") ? 6 : 1;
        if (sites[0] != expected) throw new IllegalStateException("S4G native sites " + name + "=" + sites[0] + " expected=" + expected);
        return result;
    }
    /** Live health only; never installs or repairs a missing guard. */
    public static synchronized boolean installed() { return enabled && !failed && owner != null && guard != null && failures.isEmpty() && transformed.equals(HASHES.keySet()); }
    private NativeLandmarkAgent() {}
}
