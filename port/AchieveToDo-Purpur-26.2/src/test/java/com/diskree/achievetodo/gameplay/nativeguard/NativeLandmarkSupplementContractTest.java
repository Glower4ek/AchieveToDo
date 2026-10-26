package com.diskree.achievetodo.gameplay.nativeguard;

import java.lang.classfile.*;
import java.lang.classfile.instruction.FieldInstruction;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.JarFile;

/** Exact post-pipeline pins, verifier and lost-boundary refusal. Actual gameplay has separate receipts. */
public final class NativeLandmarkSupplementContractTest {
    public static final class Tag {}
    public static final class TaggedStack {
        public boolean is(Object item) { throw new AssertionError("Tag used as an item identity"); }
        public boolean is(Tag tag) { return true; }
    }
    public static final class Baby {
        public boolean isBaby() { return true; }
        public boolean brushOffScute(Object user,Object stack) { throw new AssertionError("Baby brushed"); }
    }
    public static final class BlockedCollision {
        public boolean noCollision(Object entity,Object box) { return false; }
    }
    public static class ArmorOwner { private final Object owner = new Object(); }
    public static final class MountArmorSlot extends ArmorOwner {}
    public static final class Stack {
        private final boolean empty;
        Stack(boolean empty) { this.empty=empty; }
        public boolean isEmpty() { return empty; }
    }
    public static final class Shelf {
        public Stack getItem(int slot) { return new Stack(true); }
    }
    @SuppressWarnings("unchecked")
    private static boolean owns(Class<?> type, String field, String name) throws ReflectiveOperationException {
        var f = type.getDeclaredField(field); f.setAccessible(true);
        return ((Map<String,?>)f.get(null)).containsKey(name);
    }
    static byte[] prior(String name, byte[] raw, ClassLoader loader) throws Exception {
        byte[] input = raw;
        if (owns(NativeTransactionAgent.class,"HASHES",name)) input = NativeTransactionAgent.transformNative(name,input);
        if (owns(NativeLandmarkAgent.class,"HASHES",name)) input = NativeLandmarkAgent.transformNative(name,input,loader);
        if (NativeActionSpecs.INPUTS.containsKey(name)) input = NativeActionAgent.transformNative(name,input,loader);
        if (NativeToolSpecs.INPUTS.containsKey(name)) input = NativeToolAgent.transformNative(name,input,loader);
        if (NativeComplexSpecs.INPUTS.containsKey(name)) input = NativeComplexAgent.transformNative(name,input,loader);
        if (NativePortalAgent.INPUTS.containsKey(name)) input = NativePortalAgent.transformNative(name,input,loader);
        if (NativeMovementAgent.INPUTS.containsKey(name)) input = NativeMovementAgent.transformNative(name,input,loader);
        return input;
    }
    public static void main(String[] args) throws Exception {
        String nativeJar = System.getenv("ACHIEVETODO_TEST_PURPUR_NATIVE");
        if (nativeJar == null || nativeJar.isBlank()) throw new IllegalStateException("S4N requires ACHIEVETODO_TEST_PURPUR_NATIVE");
        boolean printPins = "1".equals(System.getenv("S4N_PRINT_PINS"));
        int checks = 0;
        if(!NativeLandmarkOperations.hasTag(new TaggedStack(),new Tag()))throw new AssertionError("Exact tag overload lost");checks++;
        var immutableLeashes=List.of(new Object());
        if(!((List<?>)NativeLandmarkOperations.filterLeashes(immutableLeashes,new Object(),null,null)).isEmpty())throw new AssertionError("Locked immutable leash result retained");checks++;
        if(immutableLeashes.size()!=1)throw new AssertionError("Native leash collection mutated");checks++;
        if(NativeLandmarkOperations.filterLeashes(immutableLeashes,null,null,null)!=immutableLeashes)throw new AssertionError("Allowed leash result replaced");checks++;
        var emptyLeashes=List.of();
        if(NativeLandmarkOperations.filterLeashes(emptyLeashes,new Object(),null,null)!=emptyLeashes)throw new AssertionError("Empty leash branch changed");checks++;
        try (var jar = new JarFile(nativeJar)) {
            var urls = new ArrayList<java.net.URL>(); urls.add(Path.of(nativeJar).toUri().toURL());
            try (var files = Files.walk(Path.of(nativeJar).getParent().getParent().getParent().resolve("libraries"))) {
                for (var file : files.filter(p -> p.toString().endsWith(".jar")).toList()) urls.add(file.toUri().toURL());
            }
            try (var loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),NativeLandmarkSupplementContractTest.class.getClassLoader())) {
                var cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
                for (String name : new TreeSet<>(NativeLandmarkSupplementSpecs.INPUTS.keySet())) {
                    byte[] raw; try (var in = jar.getInputStream(jar.getJarEntry(name+".class"))) { raw = in.readAllBytes(); }
                    byte[] input = prior(name,raw,loader);
                    if (printPins) {
                        System.out.println("S4N_INPUT_PIN " + name + " " + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input)));
                        continue;
                    }
                    NativeLandmarkSupplementAgent.verifyPin(name,input); checks++;
                    byte[] output = NativeLandmarkActions.transform(name,input,loader,NativeLandmarkSupplementSpecs.SPECS);
                    var errors = cf.verify(output);
                    if (!errors.isEmpty()) throw new AssertionError(name+errors); checks++;
                    byte[] wrong = input.clone(); wrong[wrong.length-1] ^= 1;
                    try { NativeLandmarkSupplementAgent.verifyPin(name,wrong); throw new AssertionError("Wrong pin accepted"); }
                    catch (IllegalStateException expected) { checks++; }
                    for (var spec : NativeLandmarkSupplementSpecs.SPECS.stream().filter(s -> s.owner().equals(name)).toList()) {
                        byte[] missing = cf.transformClass(cf.parse(input),(cb,e) -> {
                            if (e instanceof MethodModel m && m.methodName().equalsString(spec.method()) && m.methodType().equalsString(spec.descriptor())) return;
                            cb.with(e);
                        });
                        try {
                            NativeLandmarkActions.transform(name,missing,loader,NativeLandmarkSupplementSpecs.SPECS);
                            throw new AssertionError("Missing method accepted " + spec.id());
                        } catch (IllegalStateException expected) { checks++; }
                        if (spec.anchor().startsWith("FIELD:")) {
                            byte[] missingField = cf.transformClass(cf.parse(input),(cb,e) -> {
                                if (e instanceof MethodModel m && m.methodName().equalsString(spec.method()) && m.methodType().equalsString(spec.descriptor())) {
                                    cb.transformMethod(m,MethodTransform.transformingCode((b,element) -> {
                                        if (element instanceof FieldInstruction f && f.name().equalsString("stewEffects"))
                                            b.fieldAccess(f.opcode(),f.owner().asSymbol(),"atd$absentStew",f.typeSymbol());
                                        else b.with(element);
                                    }));
                                } else cb.with(e);
                            });
                            try { NativeLandmarkActions.transform(name,missingField,loader,NativeLandmarkSupplementSpecs.SPECS);
                                throw new AssertionError("Missing FIELD anchor accepted"); }
                            catch (IllegalStateException expected) { checks++; }
                        }
                    }
                    System.out.println("S4N_SUPPLEMENT_VERIFIED " + name);
                }
            }
        }
        if (!printPins) {
            var landmarkCallback=NativeLandmarkAgent.class.getDeclaredField("guard");landmarkCallback.setAccessible(true);
            Object previousCallback=landmarkCallback.get(null);int[] queries={0};
            try {
                landmarkCallback.set(null,(java.util.function.BiPredicate<Object,Object>)(actor,target)->{
                    queries[0]++;if (!((Object[])target)[2].equals("OPEN_CHEST")) throw new AssertionError("Wrong shelf compound authority");return false;
                });
                if (NativeLandmarkOperations.shelfSingleDenied(new Object(),new Object(),new Object(),new Stack(false),new Shelf(),OptionalInt.of(0))||queries[0]!=1)
                    throw new AssertionError("Unlocked shelf denied or wrong callback");checks++;
                if (NativeLandmarkOperations.shelfSingleDenied(new Object(),new Object(),new Object(),new Stack(true),new Shelf(),OptionalInt.of(0))||queries[0]!=1)
                    throw new AssertionError("Empty shelf queried permission");checks++;
                landmarkCallback.set(null,(java.util.function.BiPredicate<Object,Object>)(actor,target)->true);
                if (!NativeLandmarkOperations.shelfSingleDenied(new Object(),new Object(),new Object(),new Stack(false),new Shelf(),OptionalInt.of(0)))
                    throw new AssertionError("Locked shelf allowed");checks++;
            } finally { landmarkCallback.set(null,previousCallback); }
            if (NativeTransactionAgent.armorPlace(new MountArmorSlot(),new Object())) throw new AssertionError("Nonplayer mount armor restricted");checks++;
            if (NativeLandmarkOperations.contextEmpty(List.of(new Object()),null)) throw new AssertionError("Occupied crystal branch changed");checks++;
            if (NativeLandmarkOperations.boxEmpty(List.of(new Object()),null,null)) throw new AssertionError("Occupied placement branch changed");checks++;
            if (!NativeLandmarkOperations.offersEmpty(List.of(),null,null)) throw new AssertionError("Empty offers changed");checks++;
            if (NativeLandmarkOperations.shulkerAllowed(false,null,null,null,null)) throw new AssertionError("Blocked shulker changed");checks++;
            if (NativeLandmarkOperations.boatCollision(new BlockedCollision(),null,null,null)) throw new AssertionError("Blocked boat changed");checks++;
            if (NativeLandmarkOperations.brushArmadillo(new Baby(),null,null,null)) throw new AssertionError("Baby brush changed");checks++;
            for (var alias : NativeLandmarkSupplementSpecs.ALIASES.entrySet()) {
                String candidateMode = switch(alias.getKey()) {
                    case "AxeItemMixin:87", "AxeItemMixin:92" -> "axeCandidate";
                    case "HoneycombItemMixin:44", "HoneycombItemMixin:49" -> "wax";
                    case "EquippableComponentMixin:37" -> "targetEntity";
                    default -> null;
                };
                if (candidateMode != null) {
                    if (NativeLandmarkSupplementSpecs.SPECS.stream().noneMatch(s -> s.id().equals(alias.getValue()) && s.mode().equals(candidateMode)))
                        throw new AssertionError("Missing conditional shared target boundary " + alias);
                    checks++;continue;
                }
                if (alias.getKey().equals("AnimalEquipmentMenuMixin:38")) {
                    if (!alias.getValue().equals("AnimalEquipmentMenuMixin:36") || NativeLandmarkSupplementSpecs.SPECS.stream()
                        .noneMatch(s -> s.id().equals(alias.getValue()) && s.mode().equals("mountValidity") && s.anchor().equals("RETURN")))
                        throw new AssertionError("Missing shared mount validity boundary");
                    checks++;continue;
                }
                var retained = NativeLandmarkActionSpecs.SPECS.stream().filter(s -> s.id().equals(alias.getValue())).findFirst().orElseThrow();
                if (!alias.getKey().equals("ShovelItemMixin:48") || !retained.owner().equals("net/minecraft/world/item/ShovelItem")
                    || !retained.anchor().equals("Lnet/minecraft/world/level/Level;isClientSide()Z") || !retained.mode().equals("context")
                    || !retained.result().equals("PASS") || NativeLandmarkSupplementSpecs.SPECS.stream().anyMatch(s -> s.id().equals(alias.getKey())))
                    throw new AssertionError("Invalid/duplicate shared shovel boundary");
                checks++;
            }
            try { NativeLandmarkSupplementAgent.verifyInstalled(NativeLandmarkSupplementContractTest.class.getClassLoader());
                throw new AssertionError("Missing startup agent accepted"); }
            catch (IllegalStateException expected) { checks++; }
            if (NativeLandmarkActions.playerBlockDenied(null,null,null)) throw new AssertionError("Null actor denied"); checks++;
            for (String mode : List.of("passengerBoat","passengerMinecart","projectileEntity")) {
                if (NativeLandmarkActions.entityReturnAllowed(false,null,null,mode)) throw new AssertionError("False predicate changed " + mode);
                checks++;
            }
            if (!NativeLandmarkActions.entityReturnAllowed(true,new Object(),null,"passengerBoat"))
                throw new AssertionError("Nonplayer passenger denied"); checks++;
            var transformer = new java.util.concurrent.atomic.AtomicReference<java.lang.instrument.ClassFileTransformer>();
            var installs = new java.util.concurrent.atomic.AtomicInteger();
            var instrumentation = (java.lang.instrument.Instrumentation) java.lang.reflect.Proxy.newProxyInstance(
                NativeLandmarkSupplementContractTest.class.getClassLoader(),new Class<?>[]{java.lang.instrument.Instrumentation.class},
                (proxy,method,arguments) -> {
                    if (!method.getName().equals("addTransformer")) throw new AssertionError(method);
                    installs.incrementAndGet(); transformer.set((java.lang.instrument.ClassFileTransformer)arguments[0]);return null;
                });
            String property = "achievetodo.landmarks.exactPrototype", previous = System.getProperty(property);
            try {
                System.clearProperty(property);NativeLandmarkSupplementAgent.initialize(instrumentation);
                if (installs.get()!=0) throw new AssertionError("Default-off transformer registered"); checks++;
                System.setProperty(property,"true");NativeLandmarkSupplementAgent.initialize(instrumentation);
                if (installs.get()!=1) throw new AssertionError("Opt-in transformer registration"); checks++;
                try { NativeLandmarkSupplementAgent.initialize(instrumentation);throw new AssertionError("Duplicate transformer accepted"); }
                catch (IllegalStateException expected) { checks++; }
                if (transformer.get().transform(null,"unrelated/Class",null,null,new byte[]{0})!=null)
                    throw new AssertionError("Unrelated class modified"); checks++;
                String name = NativeLandmarkSupplementSpecs.INPUTS.keySet().iterator().next();
                if (transformer.get().transform(null,name,null,null,new byte[]{0})!=null) throw new AssertionError("Wrong pin transformed");
                var failures = NativeLandmarkSupplementAgent.class.getDeclaredField("failures");failures.setAccessible(true);
                if (!String.valueOf(((Map<?,?>)failures.get(null)).get(name)).contains("Unpinned S4N landmark input"))
                    throw new AssertionError("Wrong pin not retained for startup refusal"); checks++;
            } finally {
                if (previous==null) System.clearProperty(property);else System.setProperty(property,previous);
            }
        }
        System.out.println("S4N_SUPPLEMENT_CONTRACT " + (printPins ? "PIN_GENERATION_ONLY" : "PASS") + " checks=" + checks
            + " callsites=" + NativeLandmarkSupplementSpecs.SPECS.size() + " aliases=" + NativeLandmarkSupplementSpecs.ALIASES.size() + " gameplayCertification=SEPARATE");
    }
}
